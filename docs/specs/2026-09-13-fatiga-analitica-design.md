# Fatiga capturada y analítica muscular — diseño (Slice 2)

> Segundo slice de una pareja. Depende del Slice 1
> (`2026-09-13-auto-progresion-design.md`) — no arranca antes de que el motor
> automático + `ProgresionAutomatica` estén en `main`. Este slice agrega la
> captura de fatiga por serie, extiende el motor para consumirla, y suma el
> análisis por grupo muscular (avisos y pie chart).

## Contexto y objetivo

Con el Slice 1 en producción, la app ya sugiere ajustes de peso/reps basados solo
en "cumpliste el objetivo o no". La señal es útil pero pobre: no distingue entre
"cumpliste con margen" y "cumpliste al fallo". Este slice suma esa distinción con
feedback subjetivo por serie (chips rápidos + RIR opcional) y aprovecha los datos
que ya existen en el catálogo (`musculoObjetivo` + `musculosSecundarios`, 15 valores
fijos) para dos cosas:

1. **Avisos** de exceso de trabajo por grupo muscular (en editor, al arrancar
   sesión, y dinámicamente durante ella si el toggle está activo).
2. **Pie chart** de reparto muscular en el resumen de la sesión terminada y una
   vista agregada por semana/mes/todo en `StatsFragment`.

El slice es **opt-in**: sin el toggle activo la app se comporta exactamente como
después del Slice 1. Con el toggle activo, aparecen los chips y el algoritmo se
refina.

## Decisiones del usuario (2026-09-13)

| Decisión | Elección |
|---|---|
| Formato de captura | Chips (Fácil / Justo / Duro / Al fallo) + expansión opcional a RIR numérico |
| Activación | Toggle global único en `AjustesActivity` (bloque reservado en Slice 1) |
| Aviso "muchos ejercicios de X grupo" | En editor + al arrancar sesión + dinámicamente durante sesión (esta última solo con toggle activo) |
| Ponderación primario vs secundarios | Primario 1.0, los secundarios comparten 0.5 (se divide entre `n_secundarios`) |
| Ubicación del pie chart | Post-sesión Y en `StatsFragment` con selector Semana/Mes/Todo |
| Migración de sesiones viejas | Ninguna. `SerieSesion.rir = -1` (no capturado), el motor lo tolera |
| Encargado de la integración de código | **big pickle** vía OpenCode |

## División de trabajo

**Claude:**
1. Este spec.
2. `docs/plans/2026-09-13-fatiga-analitica.md` con las tareas listas para big pickle.

**big pickle:**
1. Todos los cambios de modelo, motor (extensión de `ProgresionAutomatica`), UI y
   layouts.
2. Nueva `PieChartView` casera (sin dependencias, misma familia que la
   `BarChartView` ya existente).
3. Verificación en dispositivo por el checklist del plan.
4. Commit final + release del APK cuando el usuario apruebe.

## Alcance en un vistazo

### Dentro
- Nuevo campo `SerieSesion.rir` (`int`, default `-1`, primitivo).
- Nuevo campo `Usuario.seguimientoFatiga` (`boolean`, default `false`, primitivo).
- Nueva utilidad estática `util/AnalisisMuscular.java` con:
  - `calcularVolumenPorGrupo(Sesion, Map<String, Ejercicio>)` → `Map<String, Double>`.
  - Sobrecarga por rango: `calcularVolumenPorGrupo(List<Sesion>, LocalDate desde, LocalDate hasta, Map<String, Ejercicio>)`.
  - `ejerciciosPorGrupo(Rutina, Map<String, Ejercicio>)` → `Map<String, Integer>` (solo primario).
  - Constante `LIMITE_POR_GRUPO = 4`.
- Nueva `ui/PieChartView.java` (extiende `View`, `onDraw` con arcos).
- Chips por serie + expansor RIR en `EjercicioSesionPagerAdapter`.
- Toggle "Seguimiento de fatiga avanzado" en `AjustesActivity` (bloque reservado).
- Banner en `EditRoutineActivity` (encima del banner de sugerencias del Slice 1)
  cuando algún grupo supera `LIMITE_POR_GRUPO`.
- Card al arrancar sesión (`ActiveSessionActivity`) con la misma lógica.
- Detección dinámica durante sesión (3 series consecutivas del mismo ejercicio
  con `rir <= 0` → snackbar informativo). **Solo si `seguimientoFatiga == true`.**
- Pie chart en `SessionSummaryActivity` (debajo del resumen).
- Nueva sección "Distribución muscular" en `StatsFragment` con selector
  Semana/Mes/Todo y `PieChartView` agregado.
- Refinamiento en `ProgresionAutomatica`:
  - Sesión con `rir >= 3` en la última serie completada → tratar como MARGEN.
  - Sesión con `rir == 0` y reps cumplidas justo → tratar como FALLIDO.
  - Sin `rir` (`== -1`) → comportamiento del Slice 1 puro (solo señal de reps).

### Fuera (ver "Fuera de alcance")
- Ver Slice 1 (spec `2026-09-13-auto-progresion-design.md`).
- Configurabilidad de la ponderación secundaria (0.5 fijo).
- Configurabilidad del umbral (4 ejercicios/grupo, fijo).
- Colores/orden del pie chart configurables.
- Análisis comparativo entre rutinas.
- Toggle de fatiga por rutina o por sesión (solo global).

## Modelo de datos

Aditivo. Cumple trampa #3 de [CLAUDE.md](../../CLAUDE.md).

### Campos nuevos

- `SerieSesion.rir` — `int`, default `-1`. Escala:
  - `-1` = no capturado (retro-compat).
  - `0` = "Al fallo o con ayuda" (equivalente RPE 10).
  - `1` = "Duro" (RIR ~1, RPE 9).
  - `2` = "Justo" (RIR ~2, RPE 8).
  - `3` = (reservado, usado si el usuario expande y elige "3").
  - `4` = "Fácil" (RIR ~4, RPE ~6).
  - `5` = (reservado, usado si el usuario expande y elige "5").
  
  Los chips visibles mapean así: Fácil → 4, Justo → 2, Duro → 1, Al fallo → 0.
  El expansor RIR (opcional) permite elegir 0/1/2/3/4/5 directamente y **pisa** el
  chip.

- `Usuario.seguimientoFatiga` — `boolean`, default `false`.

### Migración

- `CATALOGO_VERSION_ACTUAL` **no se toca**.
- Sesiones viejas se leen con `rir = -1` en todas sus series.
  `ProgresionAutomatica` trata `rir == -1` como "no hay señal", y usa solo reps
  (comportamiento del Slice 1 puro para esas sesiones).

## Motor: refinamiento de `ProgresionAutomatica`

Sin cambios en la tabla básica del Slice 1. Solo se enriquece el vocabulario:
antes de clasificar una sesión pasada como MARGEN/JUSTO/FALLIDO, mirar el RIR de
la última serie completada:

```java
SerieSesion ultima = sesionPasada.getUltimaSerieCompletada();
int rir = ultima != null ? ultima.getRir() : -1;

if (rir >= 3) {
    return MARGEN;  // aunque las reps solo llegaran al objetivo
}
if (rir == 0 && repsCumplenJusto(sesionPasada, config)) {
    return FALLIDO;  // ya está al límite, no subir
}
// si no, seguir con la clasificación por reps del Slice 1
```

El refinamiento vive **dentro** de `ProgresionAutomatica.clasificarSesion(...)`
(nuevo método privado). Las otras 3 estrategias (Lineal, Greyskull, Doble) **no
se modifican** — el RIR es exclusivo del esquema Automático.

Sin `rir` en las series (todas `== -1`), la lógica cae al comportamiento del
Slice 1 puro. La compatibilidad hacia atrás es completa.

## Captura de fatiga en UI

### Chips post-serie en `EjercicioSesionPagerAdapter`

Debajo de cada fila de serie **completada**, si `usuario.isSeguimientoFatiga()`:

```
Serie 1  [40 kg] × [8]  ✓ completada
         [ Fácil ] [ Justo ] [ Duro ] [ Al fallo ]     [+ RIR exacto]
```

- Cuatro `Chip` seleccionables, mutuamente excluyentes.
- Un tap guarda inmediato en `SerieSesion.rir` con el valor traducido y llama
  a `guardarProgreso()` (mismo patrón que ya usa el pager).
- `[+ RIR exacto]` expande un pequeño picker numérico (0-5). Si se usa, guarda
  el valor **exacto** en `rir` y desmarca el chip (o marca el chip que más se
  aproxime — decidir en el plan).
- Si `usuario.isSeguimientoFatiga()` es `false`, todo este bloque queda `View.GONE`
  y no ocupa espacio.
- Series no completadas no muestran chips (no tiene sentido reportar fatiga de
  algo que no hiciste).

Layout nuevo: `layout/view_fatiga_chips.xml` (inflable como componente). Se
incluye en `layout/item_serie_sesion.xml` con `<include>` para poder ocultarlo
como bloque.

### Toggle en `AjustesActivity`

En el bloque reservado por el Slice 1, un `SwitchMaterial` "Seguimiento de fatiga
avanzado" con texto explicativo corto de 2 líneas ("Marca cómo te sentiste al
terminar cada serie. La app usa esta info para afinar sus sugerencias.").

Guarda inmediato en `Usuario.seguimientoFatiga`. No requiere reiniciar la app —
la próxima vez que el pager infle una fila de serie, la lógica lee la bandera
actual.

## Avisos por grupo muscular

### Banner en `EditRoutineActivity`

Encima del banner de sugerencias del Slice 1. Se calcula al vuelo con
`AnalisisMuscular.ejerciciosPorGrupo(rutina, catalogoPorId)` cada vez que se
agrega/quita un ejercicio o al abrir la Activity.

Muestra el grupo con más ejercicios si supera 4:

```
Aviso: 5 ejercicios de "Pectorales" en esta rutina.
Se recomienda máximo 4 por sesión.        [ Entendido ]
```

"Entendido" oculta el banner hasta que cambie el conteo. No modifica la rutina.

### Card al arrancar sesión

En `ActiveSessionActivity.onCreate`, antes de inflar el pager. Si algún grupo
supera 4, `MaterialCardView` breve con la lista de grupos afectados y sus
conteos, más botón "Empezar de todos modos". No hay opción "Cancelar" — es
solo advertencia, no bloqueo.

### Detección dinámica durante sesión

**Solo si `usuario.isSeguimientoFatiga() == true`**. En
`EjercicioSesionPagerAdapter`, después de guardar el RIR de una serie:

```java
int racha = contarSeriesAlFalloConsecutivas(ejercicioSesion, 3);
if (racha >= 3) {
    mostrarSnackbar("Llevas 3 series al fallo — considera parar aquí.");
}
```

Se cuenta hacia atrás desde la serie recién marcada. Solo aparece la primera vez
en la racha (usar un flag por `EjercicioSesion` para no mostrarlo de nuevo al
guardar la siguiente serie del mismo ejercicio en la misma sesión).

## Pie chart

### `PieChartView` (nueva View casera)

Extiende `View`. Recibe `Map<String, Double>` (grupo muscular → volumen ponderado)
via setter `setDatos(Map<String, Double>)`. En `onDraw`:

1. Calcular suma total; ignorar si es 0.
2. Para cada entrada, calcular ángulo proporcional y `canvas.drawArc(...)` con
   `Paint#setStyle(Style.FILL)` y color asignado.
3. Anti-alias siempre `true`.
4. `onMeasure` respeta `MeasureSpec` con mínimo 200dp, máximo el ancho disponible,
   mantiene proporción 1:1.

Leyenda: no dibujada por la View, sino en el layout aparte (`RecyclerView` o
`LinearLayout` dinámico bajo el pie), para que sea seleccionable/copiable. Cada
fila: cuadradito de color + nombre del grupo + porcentaje.

Layout contenedor: `layout/view_pie_chart_con_leyenda.xml`.

### Colores para los 15 grupos musculares

Paleta hardcoded en `PieChartView` (constante `COLORES`), derivada del acento
naranja del tema pero con suficiente contraste entre grupos adyacentes. 15
colores fijos, uno por cada valor de `Ejercicio.MUSCULOS_OBJETIVO`. Big pickle
elige los hex exactos en el plan, respetando:

- Buen contraste entre valores adyacentes en el orden alfabético.
- Legibles sobre fondo Material Day y Night.
- Familia visual coherente (evitar violeta chillón junto a verde neón).

### En `SessionSummaryActivity`

Debajo del resumen actual + card de sugerencias (Slice 1). Se calcula con
`AnalisisMuscular.calcularVolumenPorGrupo(sesion, catalogoPorId)` una sola vez
en `onCreate`.

### En `StatsFragment`

Nueva sección "Distribución muscular" con:

- `ChipGroup` selector: "Semana / Mes / Todo" (single-select, default "Semana").
- `PieChartView` + leyenda.
- Al cambiar el chip, recalcular con
  `AnalisisMuscular.calcularVolumenPorGrupo(sesiones, desde, hasta, catalogoPorId)`
  usando el rango correspondiente:
  - Semana: `desde = hoy - 7 días`, `hasta = hoy`.
  - Mes: `desde = hoy - 30 días`, `hasta = hoy`.
  - Todo: `desde = null`, `hasta = null` (o `LocalDate.MIN` / `LocalDate.MAX`).

Layout `layout/fragment_stats.xml` extiende con la sección al final.

## Ponderación (recordatorio)

Se aplica en `AnalisisMuscular`:

```java
for (SerieSesion serie : ejercicioSesion.getSeries()) {
    if (!serie.isCompletada()) continue;
    double vol = serie.calcularVolumen();
    acumular(volumenPorGrupo, ejercicio.getMusculoObjetivo(), vol);
    List<String> sec = ejercicio.getMusculosSecundarios();
    if (!sec.isEmpty()) {
        double porCadaSecundario = 0.5 * vol / sec.size();
        for (String m : sec) {
            acumular(volumenPorGrupo, m, porCadaSecundario);
        }
    }
}
```

Presupuesto por serie: primario 100 % + secundarios 50 % total = 150 %.

Ejercicios personalizados sin `musculoObjetivo` (o con muscular vacío): se
agrupan bajo `"Otros"` para que no crasheen. El color de "Otros" es gris neutro
fuera de los 15 principales.

## Testing y verificación

Igual que el Slice 1: sin tests unitarios, verificación manual en dispositivo por
el checklist del plan. Puntos críticos:

1. Compilación + chequeo del CLAUDE.md.
2. **Respaldo de `datos.json`.**
3. Sesiones viejas (Slice 1) siguen abriéndose sin crash y sin chips (por
   `seguimientoFatiga == false` por default).
4. Prender toggle → chips aparecen inmediato en próxima serie completada.
5. Chip "Duro" guarda `rir = 1`. Expandir RIR y poner "3" → guarda `rir = 3` y
   sobreescribe el chip.
6. Refinamiento del motor: sesión 3×8 justo con RIR "Fácil" en todas → sugerencia
   sube peso. 3×8 justo con RIR "Al fallo" → sugerencia mantiene.
7. Banner en editor con 5+ ejercicios del mismo grupo → aparece. Quitar uno →
   desaparece.
8. Card al arrancar sesión con rutina "sobrecargada" → aparece con lista de
   grupos.
9. Detección dinámica: con toggle activo, 3 series consecutivas del mismo
   ejercicio con "Al fallo" → snackbar. Con toggle apagado → nada.
10. Pie chart post-sesión: al menos 3 grupos, porcentajes suman 100 % (aprox por
    redondeo), colores estables.
11. Pie chart en Stats: cambiar Semana/Mes/Todo actualiza sin recrear el
    Fragment.
12. Ponderación correcta con caso conocido (5×5 peso muerto → total 750 =
    500 primario + 5×50 secundarios).
13. Restaurar `datos.json` real.

## Fuera de alcance

- Todo lo del Slice 1.
- Configurabilidad de la ponderación primario/secundario (0.5 total fijo).
- Configurabilidad del umbral `LIMITE_POR_GRUPO` (fijo en 4).
- Colores/orden del pie chart configurables por el usuario.
- Análisis comparativo entre rutinas.
- Toggle de fatiga por rutina o por sesión (solo global).
- Consumo de RIR por las estrategias Lineal/Greyskull/Doble (solo Automática).
- Detección dinámica sin toggle (sin `rir` no hay señal fiable).
- Export/import del historial en JSON (idea del usuario, candidata a slice
  futuro independiente).
- Generación de rutinas asistida por IA (§5.2).

## Riesgos aceptados

- Chips + RIR expandible: más UI que RIR puro, pero menos preciso que un slider.
  Compromiso deliberado — decisión del usuario.
- Pie chart en "Todo" puede volverse lento con miles de sesiones. Iteración
  lineal simple; si aparece el problema real, se optimiza en su momento.
- Ejercicios personalizados sin `musculoObjetivo` caen en "Otros" — grupo
  comodín, no distorsiona el resto.
- Sin tests unitarios: verificación en dispositivo es la red de seguridad.
- Colores del pie chart hardcoded: si el usuario los odia, es cambio de código.
