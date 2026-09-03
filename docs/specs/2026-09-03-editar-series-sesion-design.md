# Eliminar serie y editar peso/reps por teclado — diseño

> Feedback de un amigo del usuario que probó el APK (no del profesor). Dos huecos de UX en la
> sesión activa, ambos en `view_serie_sesion_row.xml` / `ActiveSessionActivity`.

## Problema 1 — no se puede eliminar una serie

Cada fila de serie tiene flechas +/- para peso y repeticiones, y un botón para marcarla
completada, pero ninguna forma de quitarla. Sí existe "+ Agregar serie" (agrega al final), sin
su contraparte. Un toque de más al agregar deja una serie fantasma sin forma de corregirla
desde la app.

**Solución — long-press sobre "Serie N":** mantener presionado el texto elimina esa serie. Se
eligió long-press en vez de un ícono visible para no agregar un sexto control a una fila ya
apretada (numero + 2 flechas de peso + 2 de reps + completada). Sin diálogo de confirmación
—es una sola serie, de bajo riesgo—, pero sí:

- Vibración corta (mismo patrón que ya usa `vibrarConfirmacion()`).
- Snackbar "Serie eliminada" con acción "Deshacer", porque el long-press no es tan discreto
  como un botón (puede dispararse sin querer al hacer scroll) y no hay confirmación previa.

Las series restantes se renumeran (Serie 1, Serie 2... sin huecos). El deshacer reinserta la
serie en su posición original, no al final — el orden importa para
`EjercicioSesion.getUltimaSerieCompletada()`, que el motor de progresión Greyskull usa como
referencia AMRAP en la próxima sesión.

## Problema 2 — ajustar peso/reps a fuerza de tocar flechas es lento

Las flechas +/- suman de a 2.5kg o 1 repetición. Para un cambio grande (ajustar el plan de
golpe a un peso muy distinto), hay que tocar muchas veces.

**Solución — tocar el número para escribirlo:** `text_peso` y `text_reps` pasan de `TextView` a
`EditText` (teclado numérico, selección automática del texto al enfocar para escribir directo
sin borrar). Las flechas se quedan sin cambios, para los ajustes cortos que ya sirven bien.
Mismo patrón de `EditText` + `SimpleTextWatcher` que ya usa `RutinaEjercicioEditAdapter` en
Editar rutina — sin componente nuevo.

## Cambios de modelo

`EjercicioSesion` gana tres métodos:

```java
public void quitarSerie(SerieSesion serie) { series.remove(serie); renumerar(); }
public void insertarSerie(int posicion, SerieSesion serie) {
    series.add(Math.min(posicion, series.size()), serie);
    renumerar();
}
private void renumerar() { /* series.get(i).setNumero(i + 1) para cada i */ }
```

## Fuera de alcance

- El botón "eliminar ejercicio" (todo el bloque) no cambia — ya tiene confirmación propia y
  pierde más trabajo, así que sí la necesita.
- No se toca la discoverability de "cambiar/quitar ejercicio durante la sesión" (tema
  independiente, no priorizado en esta rebanada).

## Verificación

`./gradlew assembleDebug` → respaldo de `datos.json` real → instalar → en una sesión: agregar
una serie de más y borrarla con long-press (confirmar que renumera y que el Snackbar deshace
bien) → escribir un peso y unas repeticiones por teclado en dos series distintas → rotar la
pantalla (la Activity ya tiene `configChanges` para esto) → finalizar la sesión → comparar
`datos.json` antes/después: mismas rutinas y sesiones previas, la sesión nueva con los valores
exactos que se escribieron.
