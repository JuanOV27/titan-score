# Editar personalizados, reordenar y compartir rutinas — diseño

> Feedback de pruebas en dos teléfonos reales con la versión nueva: además del bug del
> `RecyclerView` que se cortaba al editar una rutina (ya arreglado y commiteado aparte,
> `55e2216`), se pidieron tres funciones nuevas sobre la pantalla de editar rutina.

## Alcance

1. Editar nombre y grupo muscular de un ejercicio **personalizado** (creado por el usuario),
   de forma general — el cambio se refleja en todas las rutinas que lo usan, porque es el
   mismo ejercicio de catálogo, no una copia por rutina.
2. Reordenar los ejercicios dentro de una rutina con arrastrar y soltar.
3. Exportar una rutina individual a un archivo para compartirla (WhatsApp, correo, etc.) e
   importarla en otro teléfono, sin reemplazar los datos existentes de quien la recibe.

## 1. Marcar ejercicios personalizados

`Ejercicio` gana un campo nuevo:

```java
private boolean personalizado;   // primitivo: datos.json viejo lo lee en false, sin migración
```

Se pone en `true` únicamente en `EjercicioPicker.mostrarPersonalizado(...)`, al crear uno
nuevo. Reemplaza la necesidad de adivinar por el prefijo del id (`ex_` vs `ex1..ex20`), que es
frágil y no está pensado como un contrato explícito.

## 2. Editar nombre/grupo muscular

En `item_ejercicio_rutina_edit.xml`, junto al nombre del ejercicio, aparece un ícono de lápiz
**solo si `ejercicio.isPersonalizado()`** — los ejercicios del catálogo semilla (`ex1..ex20`)
quedan de solo lectura, para no arriesgar romper el catálogo base compartido por todas las
rutinas.

Tocar el lápiz reutiliza el mismo `dialog_custom_exercise.xml` que ya existe para crear un
ejercicio, mostrado con los valores actuales precargados y un texto breve aclarando que el
cambio aplicará a todas las rutinas donde se use ese ejercicio. Al guardar, se muta el mismo
objeto `Ejercicio` en `dataStore.getEjercicios()` (no se crea uno nuevo) y se refresca esa
fila con `notifyItemChanged`.

## 3. Reordenar con arrastrar y soltar

`ItemTouchHelper` con un `SimpleCallback` de arrastre vertical (`UP | DOWN`, sin swipe),
adjunto al `RecyclerView` de `EditRoutineActivity`. Se agrega un ícono de "manija" (☰) a la
izquierda de cada tarjeta en `item_ejercicio_rutina_edit.xml`; el arrastre se dispara **solo**
tocando esa manija (`setOnTouchListener` con `ACTION_DOWN` → `itemTouchHelper.startDrag(...)`),
no la tarjeta completa — así no compite con los campos de texto y el spinner ya interactivos
dentro de la tarjeta.

Nuevo método en el modelo, siguiendo el patrón ya usado por `agregarEjercicio`/
`quitarEjercicio`:

```java
public void moverEjercicio(int desde, int hasta) {
    ejercicios.add(hasta, ejercicios.remove(desde));
}
```

En `onMove` del callback: `rutina.moverEjercicio(origen, destino)` +
`adapter.notifyItemMoved(origen, destino)`.

## 4. Exportar/importar una rutina

Mismo mecanismo que ya usa "Exportar datos"/"Importar datos" en `MainActivity`
(`FileProvider` + `Intent.ACTION_SEND` para compartir, `ACTION_OPEN_DOCUMENT` para elegir un
archivo) — pero **sin reemplazar nada**, a diferencia de "Importar datos". Se agrega, no se
sustituye.

**Formato del archivo compartido** — nuevo modelo `RutinaCompartida` (no hereda de
`EntidadIdentificable`, es un contenedor sin identidad propia, igual que `DataStore`):

```java
public class RutinaCompartida {
    private Rutina rutina;
    private List<Ejercicio> ejercicios;   // solo los que esa rutina referencia
}
```

Se incluyen los `Ejercicio` completos (catálogo y personalizados) que la rutina usa, para que
quien la reciba no necesite tener ya esos mismos ejercicios.

**Exportar** — botón nuevo en cada tarjeta de `item_rutina.xml` (junto al de editar):
`DataManager.exportarRutinaComoArchivo(Rutina)` arma el `RutinaCompartida` recorriendo
`rutina.getEjercicios()` y buscando cada uno en `dataStore.buscarEjercicio(id)`, lo escribe a
`cacheDir/exportaciones/rutina_<nombre>.json`, y se comparte con el mismo flujo de
`ACTION_SEND` que ya existe.

**Importar** — opción nueva en el menú principal, "Importar rutina" (junto a "Importar
datos"). `DataManager.leerRutinaCompartidaDesde(InputStream)` deserializa el archivo.
`DataManager.importarRutina(RutinaCompartida)`:

- Por cada `Ejercicio` del paquete: si ya existe uno con el **mismo id** en el catálogo local
  (el caso típico: ambos teléfonos tienen `ex1..ex20` sembrados igual), se reutiliza tal cual.
  Si no existe (el caso típico: un personalizado del otro usuario), se crea uno nuevo con
  `dataManager.newId("ex")`, `personalizado=true`, y se recuerda el mapeo id-viejo→id-nuevo.
- Se reconstruye la `Rutina` con un id nuevo (`dataManager.newId("r")`), remapeando el
  `ejercicioId` de cada `RutinaEjercicio` importado según el mapeo del paso anterior.
- Se agrega a `dataStore.getRutinas()` (nunca reemplaza) y se guarda.
- Antes de guardar, un diálogo simple: "¿Agregar la rutina '<nombre>' con N ejercicios?".

Coincidir por id (no por nombre) es intencional y suficiente: los 20 ejercicios semilla son
idénticos en cualquier instalación nueva (mismo `seedEjercicios()`), y un personalizado usa un
id aleatorio por instalación que casi nunca va a coincidir por accidente — así que "mismo id"
ya distingue correctamente los dos casos sin necesitar comparación difusa por nombre.

## Fuera de alcance

- No se versiona ni se resuelve conflicto si el usuario importa la misma rutina dos veces —
  simplemente se agrega de nuevo como una rutina adicional (con otro id). No se pidió
  deduplicar.
- No se comparten sesiones, historial ni configuración de progresión asociada a otras
  rutinas — solo la rutina elegida y los ejercicios que referencia.
- La edición de nombre/grupo muscular no aplica a los ejercicios semilla (`ex1..ex20`); si se
  quiere eso más adelante, es una decisión aparte (afectaría a todos los usuarios del mismo
  catálogo base).

## Verificación

- Editar nombre/grupo de un personalizado usado en 2+ rutinas → confirmar que el cambio se ve
  en ambas.
- Arrastrar ejercicios dentro de una rutina con 5+ ejercicios (la que ya sirvió para probar el
  bug del scroll) → confirmar que el orden persiste después de guardar y reabrir.
- Exportar una rutina con un ejercicio personalizado → importar en un estado limpio (o
  revisando que no colisione con ids existentes) → confirmar que aparece completa, con el
  personalizado creado de nuevo y NINGUNA rutina ni ejercicio existente alterado.
- Respaldo de `datos.json` antes de probar en el teléfono real, como siempre.
