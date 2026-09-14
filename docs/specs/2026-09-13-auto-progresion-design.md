# Auto-progresión aplicada a la rutina — diseño (Slice 1)

> Primer slice de una pareja. Este slice pone el motor híbrido, la persistencia
> de sugerencias y la UI de aceptación. El segundo slice (fatiga + analítica,
> `2026-09-13-fatiga-analitica-design.md`) lo extiende consumiendo RIR y añadiendo
> el pie chart.

## Contexto y objetivo

La app ya tiene un motor de progresión completo en
[util/progresion/](../../app/src/main/java/com/ironquest/mvp/util/progresion/): la
estrategia polimórfica abstracta con tres subclases reales (`ProgresionLineal`,
`ProgresionGreyskull`, `ProgresionDoble`) más `SinProgresion` como objeto nulo,
detección de estancamiento (3 sesiones sin cumplir → deload 90 %), y consumo desde
[ActiveSessionActivity.java:123](../../app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java)
al arrancar cada sesión. La sugerencia se muestra dentro del pager pero **nunca
se persiste**: si el usuario abre el editor de la rutina, sigue viendo el peso
original.

Este slice cierra ese hueco y agrega tres cosas relacionadas:

1. Una **quinta estrategia** (`ProgresionAutomatica`) con una regla híbrida por
   historial pensada para principiantes.
2. **Persistencia** de la sugerencia como entidad con identidad, para que aparezca
   en el resumen post-sesión y en el editor, y para que aceptarla actualice
   `RutinaEjercicio` de verdad.
3. **Configurabilidad del incremento** de peso, porque el gimnasio del usuario
   (y los del contexto universitario) frecuentemente no tiene discos de 2.5 kg.

Slice acotado a peso y reps del ejercicio. No toca estructura de rutina, orden
de ejercicios, ni analítica agregada — eso es Slice 2 o slices futuros.

## Decisiones del usuario (2026-09-13)

| Decisión | Elección |
|---|---|
| Superficies donde aparece la sugerencia | Resumen post-sesión Y banner en editor de rutina |
| Alcance del motor | Todos los ejercicios, con esquema "Automático" default para nuevos + toggle silenciar por ejercicio |
| Regla del "Automático" | Híbrida sobre las últimas 3 sesiones: margen → subir peso, justo → mantener y sugerir +1 rep, 1 fallo → mantener, 2+ fallos → deload |
| UX de aceptación | Por ejercicio (Aceptar / Ignorar individual) + "Aceptar todas" arriba |
| Peso corporal | Progresa por reps: `+2.5 kg` se traduce a `+1 rep`, deload en reps con piso 3 |
| Incremento de peso | Global en `Usuario` (chips 1 / 2.5 / 5 kg) + override opcional en `Ejercicio` |
| Convención de peso registrado | Total en las manos (ver tabla en "Convención de registro") |
| Sugerencias educativas | Info dialog con 5 tarjetas + vector drawables mínimos (esquemas) |
| Retro-compatibilidad de esquemas viejos | Ejercicios con `esquemaProgresion == 0` (Ninguno) **conservan** "Ninguno"; el nuevo default "Automático" solo aplica a ejercicios creados/editados a partir de este slice |
| Migración de sesiones viejas | Ninguna. Sesiones anteriores siguen tal cual, la sugerencia solo mira las 3 más recientes disponibles |
| Encargado de la integración de código | **big pickle** vía OpenCode (Claude solo prepara este spec y el plan.md correspondiente) |

## División de trabajo

**Claude (esta sesión y la siguiente):**
1. Este spec.
2. `docs/plans/2026-09-13-auto-progresion.md` con las tareas de implementación bite-sized,
   listas para que big pickle las ejecute sin decisiones abiertas.

**big pickle (después, con el plan.md):**
1. Todos los cambios de modelo, motor, `DataManager`, activities, adapters, layouts,
   drawables y `AjustesActivity`.
2. Verificación en dispositivo siguiendo el checklist del plan.
3. Commit final + release del APK cuando el usuario apruebe.

## Alcance en un vistazo

### Dentro
- Nueva estrategia `ProgresionAutomatica` (5ª subclase de `EstrategiaProgresion`).
- Nueva entidad `SugerenciaPendiente` en `model/` + lista raíz en `DataStore`.
- Nuevos campos: `RutinaEjercicio.silenciarSugerencia`, `Ejercicio.incrementoPeso`,
  `Usuario.incrementoPeso`.
- Nueva constante `RutinaEjercicio.ESQUEMA_AUTOMATICO = 4`.
- Nueva `AjustesActivity` (accesible desde HomeFragment o menú overflow, decidido
  por big pickle en el plan) con selector de incremento y tarjeta de convención.
- Nuevo `InfoProgresionDialog` con 5 tarjetas educativas + 5 vector drawables.
- Card de sugerencias en `SessionSummaryActivity` + banner en `EditRoutineActivity`.
- Selector de esquema y toggle "Silenciar sugerencias" en la fila de edición del ejercicio.
- Selector de incremento por ejercicio en `DetalleEjercicioDialog`.
- Firma nueva de `EstrategiaProgresion.sugerir(RutinaEjercicio, Ejercicio, Usuario, List<Sesion>)`
  para poder leer el incremento correcto sin variables globales mutables.

### Fuera (ver "Fuera de alcance")
- Captura de fatiga por serie, refinamiento del algoritmo con RIR, pie chart, avisos
  por grupo muscular, `Usuario.seguimientoFatiga`, `SerieSesion.rir` → **Slice 2**.
- Sync remoto vía Firebase, export/import a JSON, undo tras aceptar, notificaciones,
  periodización, sugerencias estructurales → futuros o descartados.

## Modelo de datos

Cambios **aditivos** en `datos.json`. Cumplen la trampa #3 de
[CLAUDE.md](../../CLAUDE.md): primitivos siempre seguros; colecciones nuevas seguras con
inicializador de campo y con normalización explícita para el caso "clave presente pero null".

### Nueva entidad `SugerenciaPendiente extends EntidadIdentificable`

Campos privados con getter/setter, constructor sin args privado para Gson, constructor
completo público. Prefijo de id `"sp"`. Campos:

| Campo | Tipo | Notas |
|---|---|---|
| `rutinaId` | `String` | FK lógica a `Rutina.id` |
| `ejercicioId` | `String` | FK lógica a `Ejercicio.id` — combinado con `rutinaId` identifica la fila afectada |
| `sesionOrigenId` | `String` | Qué sesión disparó la sugerencia (para auditoría corta) |
| `pesoActual` | `double` | Fotografía del peso antes de aceptar (no depender de la rutina, que puede haber cambiado a mano) |
| `pesoSugerido` | `double` | Peso que quedaría al aceptar (igual a `pesoActual` cuando el cambio es solo en reps) |
| `repeticionesActuales` | `int` | Reps antes de aceptar |
| `repeticionesSugeridas` | `int` | Reps al aceptar |
| `explicacion` | `String` | Texto listo para pintar en UI |
| `tipo` | `int` | Constantes: `TIPO_SUBIR_PESO=0`, `TIPO_MANTENER=1`, `TIPO_DELOAD=2`, `TIPO_SUBIR_REPS=3` |
| `fechaCreacion` | `String` | ISO, `LocalDateTime.now().toString()` |

### Nueva lista raíz en `DataStore`

```java
private List<SugerenciaPendiente> sugerenciasPendientes = new ArrayList<>();
```

Y en `normalizarColecciones()`:

```java
if (sugerenciasPendientes == null) sugerenciasPendientes = new ArrayList<>();
```

### Campos nuevos en entidades existentes

- `RutinaEjercicio.silenciarSugerencia` (`boolean`, default `false`, primitivo).
- `RutinaEjercicio.ESQUEMA_AUTOMATICO = 4` (constante nueva, `ESQUEMA_NINGUNO = 0`
  no se toca).
- `Ejercicio.incrementoPeso` (`double`, default `0` = usa el del usuario).
- `Usuario.incrementoPeso` (`double`, default `2.5`).

### `DataManager` — nuevos métodos

- `getSugerenciasPendientes()`, `getSugerenciasPendientesDeRutina(String rutinaId)`.
- `agregarSugerencia(SugerenciaPendiente s)` — persiste y guarda.
- `eliminarSugerencia(String id)` — quita de la lista y guarda.
- `aplicarSugerencia(String id)` — busca la sugerencia, aplica los cambios al
  `RutinaEjercicio` correspondiente (`peso` y `repeticiones`), elimina la sugerencia,
  guarda una sola vez al final.

Todas las mutaciones pasan por `DataManager` — nada fuera de `data/` importa Gson ni
toca `datos.json`. El chequeo del CLAUDE.md sigue dando vacío.

### Migración

- `CATALOGO_VERSION_ACTUAL` **no se toca** (no es cambio de catálogo).
- Nada en `DataManager.load()` genera sugerencias ni cambia esquemas — cumple trampa #4.

## Motor híbrido (`ProgresionAutomatica`)

Se registra en `EstrategiaProgresion.para(int)`:

```java
case RutinaEjercicio.ESQUEMA_AUTOMATICO:
    return new ProgresionAutomatica();
```

Reutiliza `sugerir()` de la clase base (flujo compartido: filtrar historial, detectar
estancamiento, fallback "primera vez"). Aporta:

### Vocabulario por sesión pasada

Se clasifica cada `EjercicioSesion` del historial contra el `RutinaEjercicio` de referencia:

- **MARGEN** — todas las series completadas tienen `reps >= objetivo + 2`.
- **JUSTO** — todas las series completadas cumplen `objetivo <= reps < objetivo + 2`.
- **FALLIDO** — al menos una serie completada con `reps < objetivo`, o las **últimas dos**
  series consecutivas fueron marcadas como no completadas (abandono).

### Regla sobre las últimas 3 sesiones

| Patrón | Sugerencia | `tipo` |
|---|---|---|
| 3× MARGEN, o 2× MARGEN + 1× JUSTO | `+incremento` kg, mantener reps objetivo | `TIPO_SUBIR_PESO` |
| Todas cumplen y la última es JUSTO (no MARGEN) | Mantener peso, `repeticiones + 1` | `TIPO_SUBIR_REPS` |
| Exactamente 1 FALLIDO | Mantener peso y reps | `TIPO_MANTENER` |
| 2 o 3 FALLIDOS | `redondearA(peso * 0.9, incremento)` | `TIPO_DELOAD` |

Con menos de 3 sesiones registradas, la regla se aplica sobre las que haya (mínimo 1).
Con 0 sesiones, la clase base ya devuelve la sugerencia "primera vez" — no se toca.

### Casos borde

- **Peso corporal (`peso == 0`)** — la regla opera sobre `repeticiones`. "+incremento kg"
  se reemplaza por "+1 rep". Deload se reemplaza por `−1 rep` con **piso mínimo 3 reps**.
- **Silenciado (`silenciarSugerencia == true`)** — `ProgresionAutomatica.sugerir()` sigue
  devolviendo una `Sugerencia` (para que la sesión activa pueda seguir usando peso/reps
  actuales al pre-llenar las series), pero
  `ActiveSessionActivity` **no genera `SugerenciaPendiente`** al finalizar. La lógica de
  "no persistir" vive en la Activity, no en la estrategia — mantiene la estrategia limpia.
- **Incremento efectivo** — la clase base lee `Ejercicio.incrementoPeso` si es `> 0`;
  si no, cae al `Usuario.incrementoPeso`. Cambio de firma:
  `sugerir(RutinaEjercicio config, Ejercicio ejercicio, Usuario usuario, List<Sesion> historial)`.
  Se elimina la constante `INCREMENTO_KG` y todos los `+ INCREMENTO_KG` en subclases se
  reemplazan por el incremento efectivo pasado como parámetro auxiliar (protegido, `getIncrementoEfectivo(...)`
  en la clase base).

### Generación de `SugerenciaPendiente`

En `ActiveSessionActivity.finalizarSesion()` (o el método que ya cierra la sesión),
**después** de que la sesión quede finalizada y guardada, iterar los ejercicios de la
rutina origen:

```java
for (RutinaEjercicio re : rutina.getEjercicios()) {
    if (re.getEsquemaProgresion() == ESQUEMA_NINGUNO) continue;
    if (re.isSilenciarSugerencia()) continue;
    Ejercicio ej = catalogoPorId.get(re.getEjercicioId());
    if (ej == null) continue;
    Sugerencia s = EstrategiaProgresion.para(re.getEsquemaProgresion())
        .sugerir(re, ej, usuario, dataStore.getSesiones());
    boolean cambio = s.getPeso() != re.getPeso() || s.getRepeticiones() != re.getRepeticiones();
    if (!cambio) continue;
    SugerenciaPendiente sp = new SugerenciaPendiente(
        dataManager.newId("sp"), rutina.getId(), re.getEjercicioId(),
        sesionActual.getId(), re.getPeso(), s.getPeso(),
        re.getRepeticiones(), s.getRepeticiones(),
        s.getExplicacion(), tipoDe(s, re), LocalDateTime.now().toString());
    dataManager.agregarSugerencia(sp);
}
```

`tipoDe(...)` es una utilidad local que clasifica según qué cambió (peso subió → SUBIR_PESO;
peso bajó → DELOAD; peso igual y reps subió → SUBIR_REPS; ninguno cambió no debería
llegar aquí por el `continue`).

## Convención de registro de peso

Se adopta una convención unificada, visible en tooltip de `AjustesActivity` (tarjeta
"Cómo registro el peso"):

| Tipo de ejercicio | Qué se registra en `peso` |
|---|---|
| Barra (press banca, sentadilla, peso muerto...) | Barra **+** discos totales |
| Mancuernas | Peso de **una sola** mancuerna |
| Máquinas de rack | Lo que marca el pin del stack |
| Peso corporal sin lastre | `0` |
| Peso corporal con lastre | Peso del lastre añadido |
| Smith machine | Discos totales, ignorando el peso mecánico de la máquina |

La app **no modela** tipos de barra ni distingue mancuernas de barras. Si el usuario
alterna la barra de 20 kg con la de 15 kg, registra pesos distintos y el motor lo trata
como sesiones más ligeras o más pesadas — correcto físicamente.

## UI — superficies nuevas y modificadas

### Nueva `AjustesActivity`

Accesible desde HomeFragment (botón en menú overflow o card de acceso rápido, decisión
del plan). Tres bloques verticales, todos en `MaterialCardView`:

1. **Incremento de peso por defecto** — título + subtítulo explicativo + `ChipGroup`
   (single-select) con "1 kg / 2.5 kg / 5 kg". Guarda inmediato en `Usuario.incrementoPeso`.
2. **Cómo registro el peso** — tarjeta con título + botón/tap para expandir la tabla
   de convenciones. Solo texto.
3. **Enlace "¿Cómo funcionan las progresiones?"** — abre `InfoProgresionDialog`.

Hereda de `BaseActivity` y usa `configurarToolbar(R.id.toolbar, "Ajustes", true)`.
Registra en `AndroidManifest.xml` con `exported="false"`.

### `SessionSummaryActivity` — card de sugerencias

Encima del resumen actual, si `dataManager.getSugerenciasPendientesDeRutina(rutinaId)`
tiene items **y** todos son de la sesión que acaba de cerrar. `MaterialCardView` acento
naranja con:

- Cabecera: "Sugerencias de progresión" + explicación breve + botón "Aceptar todas".
- Lista de items (RecyclerView interno o LinearLayout dinámico): nombre del ejercicio,
  "peso × reps → peso × reps", explicación, botones "Aceptar" y "Ignorar".
- Aceptar → `dataManager.aplicarSugerencia(id)`, actualizar el card.
- Ignorar → `dataManager.eliminarSugerencia(id)`, actualizar el card.
- Al quedar vacío, el card se colapsa (`View.GONE`).

Layout: `layout/card_sugerencias.xml` + `layout/item_sugerencia_pendiente.xml`.

### `EditRoutineActivity` — banner de sugerencias pendientes

Encima del banner de migración legacy (ya existente), otro banner del mismo estilo cuando
`getSugerenciasPendientesDeRutina(rutina.getId())` no está vacío:

```
N ejercicios tienen ajustes sugeridos.   [ Revisar ] [ Ignorar todos ]
```

"Revisar" abre un diálogo que reusa el mismo `layout/card_sugerencias.xml` en modo modal.
"Ignorar todos" elimina las sugerencias de esa rutina.

### `RutinaEjercicioEditAdapter` — nuevos controles por fila

- Spinner de esquema: "Ninguno / Automático / Lineal / Greyskull / Doble". Automático
  es el nuevo default para ejercicios que el usuario cree o agregue desde este slice
  en adelante. Los ejercicios existentes con `esquemaProgresion == 0` siguen mostrando
  "Ninguno" (no se autoconvierten).
- Checkbox "Silenciar sugerencias para este ejercicio" → `silenciarSugerencia`.
- Ícono ⓘ (info) al lado del spinner → abre `InfoProgresionDialog`.

Layout `layout/item_ejercicio_rutina_edit.xml` se extiende con estos controles al final
de la fila (colapsables en una sección secundaria si el diseño existente ya está denso).

### `DetalleEjercicioDialog` — override de incremento

Debajo del bloque de músculo objetivo, un `ChipGroup` single-select:
"Global (X kg) / 1 kg / 2.5 kg / 5 kg". La opción "Global" escribe `0` en
`Ejercicio.incrementoPeso`; las otras escriben `1.0` / `2.5` / `5.0`.

### `InfoProgresionDialog`

`Dialog` con `ScrollView` vertical y una tarjeta por esquema (5 tarjetas). Cada tarjeta:

- Vector drawable pequeño (~80dp × 40dp).
- Título del esquema.
- Descripción de 2-3 líneas.
- Ejemplo numérico de 3 sesiones (peso × reps por sesión + resultado).
- Línea "Recomendado para: ...".

Al final, dos secciones colapsables:
- "Deload: por qué la app a veces sugiere bajar"
- "Silenciar sugerencias"

**Copy exacto** de cada tarjeta lo aterriza el plan.md — aquí solo el borrador de
los primeros párrafos:

- **Ninguno** — "La rutina no cambia sola. Tú decides cuándo subir peso o reps.
  Recomendado si sigues un programa externo."
- **Automático** — "La app mira tus últimas 3 sesiones: si vas cumpliendo con
  soltura, sube peso; si vas justo, sube reps; si fallas, mantiene o baja. Es la
  opción por defecto y la más recomendada para la mayoría."
- **Lineal** — "Cada vez que cumples el objetivo en todas las series, sube el peso.
  Fuerte y directo. Para fuerza pura."
- **Greyskull** — "Solo mira tu última serie. Si sacaste más reps del objetivo,
  sube el peso. Diseñado para ejercicios compuestos con AMRAP."
- **Doble Progresión** — "Primero subes reps dentro del rango que fijaste
  (p. ej. 8–12). Al llegar al tope con todas las series, subes el peso y vuelves
  al piso. Ideal para hipertrofia."

### Vector drawables

Cinco archivos en `res/drawable/`, dibujados con `<path>` puro:

- `esquema_ninguno.xml` — línea horizontal simple.
- `esquema_automatico.xml` — escalera ascendente con un puntito/estrella.
- `esquema_lineal.xml` — escalera ascendente pareja.
- `esquema_greyskull.xml` — escalera ascendente con un peldaño de bajada.
- `esquema_doble.xml` — escalones planos (peso constante, reps subiendo) seguidos
  de un salto vertical (peso arriba, reps al piso).

Color de trazo: `?attr/colorPrimary` (acento naranja del tema). Ancho de trazo 2dp.

## Testing y verificación

Sin tests unitarios. Verificación manual en dispositivo, protocolo completo en el
plan.md. Puntos críticos que big pickle **debe** cubrir antes de commitear:

1. Compilación sin warnings nuevos.
2. Chequeo del CLAUDE.md sigue devolviendo vacío
   (`grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" ...`).
3. **Respaldo de `datos.json` del teléfono antes de instalar el APK.**
4. Retro-compatibilidad: al abrir una rutina existente los ejercicios se ven
   con "Ninguno" (no "Automático") y sin crash.
5. Flujo end-to-end: crear rutina de prueba con esquema Automático, correr 3
   sesiones cumpliendo con margen, verificar sugerencia post-sesión y su aplicación
   con "Aceptar".
6. Deload: 3 sesiones fallando → sugerencia dice "peso × 0.9" redondeado al
   incremento.
7. Override: `Usuario.incrementoPeso = 5`, override 2.5 en un ejercicio → cada
   uno usa su incremento.
8. Silenciar: marcar un ejercicio, correr sesión cumpliendo → no se genera
   `SugerenciaPendiente` para ese ejercicio.
9. Peso corporal: 3×8 dominadas cumplidas → sugerencia "3×9"; fallidas → "3×7"
   respetando el piso 3.
10. Doble ruta consistente: aceptar en el editor borra también del `SessionSummary`
    de esa sesión.
11. Restaurar el `datos.json` real.

## Fuera de alcance

- Captura de fatiga por serie, RIR, refinamiento con RIR, pie chart, avisos por
  grupo muscular, seguimiento de fatiga como toggle → **Slice 2**.
- Sync remoto de sugerencias vía Firebase.
- Export/import del historial en JSON con prompt para IA externa (idea del usuario,
  candidata a slice futuro; queda apuntada para no perderse).
- Generación de rutinas asistida por IA (§5.2 de la propuesta la excluye).
- Historial de sugerencias aceptadas/ignoradas.
- Deshacer una sugerencia aceptada.
- Configurabilidad de umbrales del algoritmo (ventana, %, definición de "margen").
- Notificaciones push por sugerencias pendientes.
- Sugerencias que reorganizan la rutina (cambiar orden, sustituir, agregar/quitar).
- Periodización planificada (mesociclos, deloads programados).
- Múltiples "perfiles de gimnasio" (uno de casa vs. uno de la U).
- Editor visual de la ponderación primario/secundario (Slice 2, no configurable).

## Riesgos aceptados

- Usuarios con `esquemaProgresion == 0` migrados desde v0.8.0 no reciben sugerencias
  hasta editar la rutina y elegir Automático. Retro-compatibilidad > migración masiva.
- Cambiar el `incrementoPeso` global no recalcula sugerencias pendientes: se
  recalculan en la próxima sesión.
- Sin tests unitarios: cualquier bug del motor solo se detecta en dispositivo.
- `INCREMENTO_KG` deja de ser constante `static final`; el compilador atrapa los
  callers desactualizados al cambiar la firma de `sugerir()`.
