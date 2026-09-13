# Ampliación del catálogo de ejercicios por equipo del gimnasio — diseño

> Extiende Slice 5 (✅ completada, ver `~/.claude/plans/velvet-bubbling-wall.md`), que dejó
> `app/src/main/assets/catalogo.json` con 80 ejercicios curados del dataset
> [hasaneyldrm/exercises-dataset](https://github.com/hasaneyldrm/exercises-dataset). Esta vez
> se amplía con ejercicios filtrados por las máquinas y elementos reales del gimnasio del
> usuario, en vez de un muestreo balanceado por grupo muscular.

## Contexto y objetivo

El catálogo actual (80 ejercicios) documenta muy pocos por grupo muscular. El usuario listó
las máquinas/estaciones concretas de su gimnasio (bancos de press con inclinaciones, polea
dual, predicador, fondos, polea alta/baja, Smith, remo T, pec deck, aductores/abductores,
prensa, hack, rack de sentadilla, cuádriceps, femoral, hip thrust, pantorrillas sentado,
V-squat, y pesos libres en general). El filtrado contra las 1324 entradas del mismo dataset ya
integrado (excluyendo las 80 ya usadas) produjo 684 candidatos, documentados en
[`docs/curacion/2026-09-13-candidatos-gimnasio.md`](../curacion/2026-09-13-candidatos-gimnasio.md).

Este documento fija el diseño para pasar de esos 684 candidatos a una extensión real del
catálogo de la app.

## Decisiones del usuario (2026-09-13)

| Decisión | Elección |
|---|---|
| Volumen | Curado, ~150-200 ejercicios nuevos (no los 684 completos) |
| Presupuesto de tamaño | +15-20 MB de APK (vs. +70 MB si se usaran los 684) |
| V-squat machine (0 resultados en el dataset) | Se omite, no se fuerza un sustituto |
| Hip thrust (solo 1 variante con banda, sin barra/máquina) | Se omite, no se fuerza un sustituto |
| Checkpoint de revisión | El usuario aprueba la shortlist final (nombres) antes de que se descarguen assets |
| Cambio de estrategia de ejecución | Claude prepara los datos; **OpenCode (modelo "big pickle") hace la integración de código**, para minimizar consumo de tokens de Claude |

## División de trabajo

**Claude (esta sesión o una siguiente, antes de involucrar a big pickle):**
1. Selecciona ~150-200 ejercicios de los 684 candidatos aplicando el recorte de la sección
   siguiente.
2. Presenta la shortlist (nombres en inglés, agrupados por estación) para aprobación.
3. Traduce y mapea cada campo (ver "Pipeline de datos y mapeos").
4. Descarga GIF + imagen de cada ejercicio aprobado.
5. Extiende `app/src/main/assets/catalogo.json` de forma **aditiva** — nunca reemplaza ni
   reordena las 80 entradas existentes, solo agrega objetos nuevos al array.
6. Commit propio del dato (Checkpoint A).

**Big pickle (después, con un plan.md autocontenido):**
1. Un solo cambio de código: bump de `CATALOGO_VERSION_ACTUAL` en `DataManager.java`.
2. Compilar, instalar, verificar en dispositivo.
3. Commit del cambio de código (Checkpoint B).

Esta división existe porque los chips de músculo, las tarjetas con GIF/badge y el buscador ya
leen dinámicamente lo que haya en `catalogo.json` (Slice UI-Ejercicios, ya completada) — si los
ejercicios nuevos reusan los mismos 15 valores de `musculoObjetivo` ya fijados en Slice 5, no
hace falta ningún cambio de UI. El trabajo de código que le queda a big pickle es mínimo a
propósito.

## Criterio de recorte (684 → ~150-200)

Por estación (ver conteos completos en `docs/curacion/2026-09-13-candidatos-gimnasio.md`):

- Estaciones ya pequeñas (Remo T=2, Pec deck=3, Leg extension=1, Femoral acostado=2, Hip
  thrust=1 [se omite, ver arriba], Pantorrillas sentado=3, Hack squat=4, Prensa=9,
  Aductores/abductores=10): se incluyen **completas**, ya están por debajo de cualquier cupo
  razonable.
- Estaciones medianas (Press banca mancuernas=18, Polea alta=15, Polea baja=22, Banco
  predicador=26, Fondos=28, Rack sentadilla=27): se recortan duplicados obvios — variantes
  "(back pov)"/"(side pov)" (mismo ejercicio, distinto ángulo de cámara), "v. 2"/"v. 3" cuando
  ya hay una versión estándar, y variantes de agarre redundantes cuando ya hay 2-3 cubiertas
  (ej. preacher curl: estándar + martillo + inversa es suficiente, no hacen falta las 9
  variantes de agarre/postura). Cupo orientativo: 10-15 por estación.
- Estaciones grandes (Polea dual tren superior=137, Máquina Smith=48, Pesos libres resto=374):
  cupo más estricto. Polea dual: priorizar un ejercicio representativo por patrón de movimiento
  (jalón, remo, curl, extensión, elevación lateral/frontal, crunch) en vez de todas las
  variantes de agarre/ángulo. Smith: los movimientos principales que ya se nombraron
  explícitamente (sentadilla, press banca, press militar, remo, peso muerto rumano, hack squat,
  leg press) — no todas las 48. Pesos libres: solo movimientos con nombre estándar reconocible
  (curl, press, remo, peso muerto, zancada, elevación) — se descartan variantes sobre
  `exercise ball`/`bosu` y duplicados de cámara.

Este criterio es una guía, no una fórmula exacta — el resultado final es la shortlist que el
usuario aprueba antes de la descarga de assets.

## Pipeline de datos y mapeos

Mismo esquema que las 80 entradas existentes de `catalogo.json` (`model/Ejercicio.java`, sin
campos nuevos):

| Campo | Origen | Regla |
|---|---|---|
| `id` | `"gv_" + dataset.id` | Ya verificado sin colisión con `ex1..ex20`, personalizados, ni las 80 curadas |
| `nombre` | `dataset.name` (solo inglés) | Traducción manual a término estándar de gimnasio en español, mismo criterio que las 80 existentes ("Press banca", "Sentadilla") |
| `grupoMuscular` | `dataset.body_part` | Mapeo ya fijado en Slice 5: chest→Pecho, back→Espalda, upper/lower legs→Pierna, shoulders→Hombro, upper/lower arms→Brazo, waist→Abdomen |
| `equipo` | `dataset.equipment` | Diccionario ya usado en las 80: Barra (barbell/ez barbell/olympic barbell), Polea (cable), Peso corporal (body weight), Máquina de palanca (leverage machine), Mancuerna (dumbbell), Banda elástica (band/resistance band), Con peso adicional (weighted). **Nuevo en esta ampliación:** Máquina Smith (smith machine), Máquina de deslizamiento (sled machine) |
| `musculoObjetivo` | `dataset.target` | Tabla de 15 valores ya fijada en Slice 5, sin cambios (Pectorales, Dorsales, Trapecios, Espalda alta, Zona lumbar, Cuádriceps, Isquiotibiales, Glúteos, Aductores/Abductores, Pantorrillas, Hombros, Bíceps, Tríceps, Antebrazos, Abdomen) |
| `musculosSecundarios` | `dataset.secondary_muscles` | Diccionario inglés→español ya usado en las 80 (Hombros, Isquiotibiales, Tríceps, Bíceps, Antebrazos, Glúteos, Cuádriceps, Oblicuos, Core, etc.), extendido con los términos nuevos que aparezcan en la selección final |
| `instrucciones` | `dataset.instruction_steps.es` | Directo, sin traducir — el dataset ya trae español |
| `imgAsset` | `"img/gv_" + id + ".jpg"` | Descarga de `raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/{dataset.image}` |
| `gifAsset` | `"gifs/gv_" + id + ".gif"` | Descarga de `raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/{dataset.gif_url}` |
| `personalizado` | — | `false`, igual que las 80 curadas |

**Verificación obligatoria post-generación** (misma regla que Slice 5): cada `musculoObjetivo`
generado debe existir literalmente en `Ejercicio.MUSCULOS_OBJETIVO` — un typo no rompe el
build, solo deja ese ejercicio invisible bajo cualquier chip (bug silencioso).

## Cambio de código (big pickle)

Un solo archivo, un solo cambio: en `data/DataManager.java`,

```java
private static final int CATALOGO_VERSION_ACTUAL = 1;   // → 2
```

`migrarCatalogoDataset()` ya es aditiva e idempotente (código de Slice 5, sin tocar):

```java
private void migrarCatalogoDataset() {
    if (dataStore.getCatalogoVersion() >= CATALOGO_VERSION_ACTUAL) return;
    // ... RETAG_SEED (no aplica aquí, ya se aplicó en Slice 5) ...
    // lee catalogo.json, agrega cada candidato cuyo id no exista ya en dataStore
    dataStore.setCatalogoVersion(CATALOGO_VERSION_ACTUAL);
    save();
}
```

Subir la versión hace que esta función corra una vez más en cada instalación existente: los 80
ids viejos ya están en `dataStore` y se saltan, los ids nuevos (`gv_XXXX` que no existían) se
agregan. No hace falta migración nueva, no hay campos nuevos en `Ejercicio.java`, no se toca
`RETAG_SEED` (era solo para `ex1..ex20`).

El riesgo de que `limpiarDatosDeUsuario()` (logout) o `reemplazarTodo()` (importar respaldo) no
vuelvan a disparar la migración **ya está resuelto** desde Slice 5: ambos resetean
`catalogoVersion` a 0 antes de llamar a `migrarCatalogoDataset()`. Nada que tocar ahí.

**Aviso para big pickle:** el repo tiene cambios sin commitear en este momento
(`ui/MigracionEjerciciosDialog.java`, cambios en `EjercicioPicker`/`EditRoutineActivity`/
`RutinaEjercicioEditAdapter`, layouts) — trabajo de otra slice en curso. No tocarlos ni asumir
que están terminados. No hay solape de archivos: `DataManager.java` no aparece en ese diff.

## Checkpoints, commits y verificación

Mismo patrón que Slice 5 (el dato es caro de regenerar → commit propio, separado del código):

1. **Checkpoint A (Claude):** `catalogo.json` extendido + `assets/img/*` + `assets/gifs/*`
   nuevos. Commit propio.
2. **Checkpoint B (big pickle):** bump de versión + build + verificación + commit.

Verificación de Checkpoint B:

- `./gradlew assembleDebug` compila limpio (con `JAVA_HOME` al JBR, ver `CLAUDE.md`).
- Tamaño de APK sube ~15-20 MB — si se dispara mucho más, revisar antes de continuar.
- Picker: conteo de ejercicios = 80 + X nuevos; los chips siguen siendo los mismos 15 valores;
  un ejercicio nuevo por estación aparece bajo el chip correcto y su GIF carga.
- `adb shell run-as com.ironquest.mvp cat files/datos.json` → `catalogoVersion: 2`, el conteo
  de ejercicios sube exactamente en X (si sube distinto, hay colisión de id — investigar antes
  de seguir).
- Logout → login con otra cuenta → catálogo sigue completo.
- Rutinas y sesiones previas intactas (migración aditiva, nunca destructiva).

## Fuera de alcance

- V-squat machine e hip thrust con barra/máquina — no existen en el dataset, no se agregan.
- Cambios de UI — no se necesitan (ver "División de trabajo").
- El diálogo de migración de ejercicios legacy en curso (`MigracionEjerciciosDialog`) — slice
  separada, no se toca desde este trabajo.
