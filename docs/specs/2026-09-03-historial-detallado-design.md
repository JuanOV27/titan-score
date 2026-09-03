# Historial detallado — diseño

> Feedback de un amigo del usuario probando el APK: el historial solo muestra tarjetas cortas
> (rutina, fecha, duración, cumplimiento%) sin poder ver qué se hizo realmente — necesita saber
> el último peso/reps de cada ejercicio para decidir si subir peso, mantenerlo, o subir reps.

## Solución

Las cards se quedan cortas tal como están. Un **tap** las expande in-place (no se navega a
otra pantalla) mostrando, por cada ejercicio de esa sesión, su nombre y cada serie:
`Serie 1: 40 kg × 10 reps`. Un segundo tap colapsa. Se eligió tap normal en vez de long-press
—que sí se usa para eliminar una serie en la sesión activa— porque aquí es una acción de "ver
más", no destructiva, y tap es más descubrible con un solo dedo (consistente con la lección de
la rebanada anterior: los íconos sin etiqueta ya causaron un problema de descubribilidad).

Sin cambios de modelo: `Sesion.getEjercicios()` → `EjercicioSesion.getSeries()` ya tiene todo
el dato (peso, repeticiones, completada). Es una vista nueva sobre datos existentes.

**Series no completadas:** se marcan en el texto ("Serie 3: 20 kg × 8 reps — no la hiciste") en
vez de mostrarse igual que las que sí se hicieron. Es información necesaria para la decisión que
motivó el pedido: si la última vez fallaste una serie, eso importa para decidir si repetir el
peso o subirlo.

**Indicador visual:** una flecha pequeña (▾ colapsado / ▴ expandido, mismo drawable rotado 180°)
junto al nombre de la rutina, para que se note que la card es expandible sin agregarle texto
que la vuelva menos "corta".

## Implementación

- `item_sesion_historial.xml`: el nombre de rutina y la flecha pasan a una fila horizontal;
  se agrega `container_detalle_sesion` (LinearLayout vertical, `visibility="gone"` por defecto)
  debajo de las 4 líneas existentes.
- `view_ejercicio_historial_detalle.xml` (nuevo, chico): nombre del ejercicio en negrita + un
  `TextView` con una línea por serie.
- `HistorialAdapter`: gana un `Map<String, Ejercicio> catalogoPorId` (mismo patrón que
  `RutinaEjercicioEditAdapter`/`PhysicalHistoryAdapter`) para resolver nombres, y un
  `Set<String> expandidas` con los ids de sesión abiertos — sobrevive el reciclaje de vistas del
  RecyclerView. `onBindViewHolder` decide mostrar/ocultar y poblar el detalle según ese set;
  siempre limpia el contenedor antes de poblar, para no arrastrar contenido de una vista
  reciclada. El tap en la card alterna la membresía en el set y llama
  `notifyItemChanged(position)`.
- `HistoryActivity`: arma el `catalogoPorId` desde `dataStore.getEjercicios()` (mismo patrón que
  ya existe en `ActiveSessionActivity`/`EditRoutineActivity`) y lo pasa al adapter.

## Fuera de alcance

- No se toca el orden ni el filtrado de la lista de sesiones.
- No hay edición desde el historial — es de solo lectura, a diferencia de la sesión activa.

## Verificación

`./gradlew assembleDebug` → respaldo de `datos.json` → instalar → abrir Historial → tap en una
sesión con varios ejercicios → confirmar que expande con el detalle correcto (comparado a mano
contra el `datos.json` real) y que una serie no completada se distingue → tap de nuevo → colapsa
→ expandir dos sesiones distintas y hacer scroll para confirmar que no se mezclan por reciclaje
de vistas → `datos.json` sin cambios (es una vista de solo lectura).
