# Rebanada: migración de ejercicios legacy (`ex1..ex20`) — contexto de sesión

Fecha: 2026-09-11. Siguiente paso natural: lectura como resumen de traspaso (equivalente a un
`/compact` por archivo).

## Objetivo

Antes de esta rebanada se completaron y commitéaron las tres rebanadas de la **Slice UI-Ejercicios**:

- `bb8573c` Enriquecer las filas de ejercicios en editar rutina: badge de músculo objetivo, GIF
  inline con toggle e info expandible.
- `1eff95e` Agregar búsqueda por nombre al selector de ejercicios con filtro combinado (chip AND texto).
- `25d4296` Refactorizar la sesión activa a ViewPager2 con cards por ejercicio, GIF con toggle e info
  expandible (`EjercicioSesionPagerAdapter`, `PASO_PESO` eliminado).

Estas tres están verificadas en el TECNO KJ5 y la rama `main` está **3 commits delante de origin**;
el commit del bloque de migración de esta rebanada sigue sin hacerse.

El objetivo de esta sesión es la **migración/limpieza de los ejercicios legacy** (`ex1..ex20`, sin
GIF ni ficha técnica): impedir que se elijan, avisar por fila y en un banner, bloquear el guardado
hasta resolver cada rutina (reemplazar por un curado o marcarlo como "creado por el usuario"), y
hacerlo sin dañar los datos reales del dispositivo.

## Contexto del proyecto (reglas vigentes)

- Root del proyecto: `/home/jdov/Documentos/titan score/IronQuestApp/`; reglas en `CLAUDE.md`.
- **Nunca escribir sobre `files/datos.json` real sin presentar el cambio exacto y aprobar.**
- Respaldo antes de pruebas con riesgo; la aprobación es específica de cada respaldo.
- Ojo con pipes a `run-as`: `cat archivo_inexistente | adb shell "run-as ... cat > files/datos.json"`
  **trunca el archivo a vacío** aunque el `cat` falle.
- Sin Room/DI/Navigation Component/ViewModel/corrutinas; sin Gson fuera de `data/`.
- Compilar con `JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr`
  y comprobar `${PIPESTATUS[0]}` (el pipe a `tail` enmascara el código de Gradle).
- Nombre interno IronQuest (paquete `com.ironquest.mvp`), nombre oficial Titan Score.

## Datos reales del dispositivo (TECNO KJ5)

- 131 ejercicios = 80 curados `gv_*` (con `gifAsset`), 20 seeds legacy `ex1..ex20` (sin gif,
  `personalizado=false`), ~31 personalizados `ex_*`.
- El catálogo curado (`app/src/main/assets/catalogo.json`) **no tiene nombres idénticos** a los
  legacy → el equivalente no es automático: hay que elegirlo manualmente en el selector.
- Un ejercicio cuenta como "pendiente de migración" si `ejercicio == null ||
  (!ejercicio.isPersonalizado() && !ejercicio.tieneFichaTecnica())`. Lo personalizado ya equivale a
  "marcado como creado por el usuario", por eso no pasa por el asistente.

### Respaldo vigente

- `/tmp/opencode/datos_respaldo_1789183950.json` — 147.803 bytes, md5
  `ab5c8663c700bc4e325891087fa06e69`. Estado limpio del dispositivo en esta sesión.

### Cómo leer/escribir `datos.json` en este build (operativa descubierta)

`run-as` **lee** pero **no escribe** en este terminal (producción, sin `adb root`): a escribir da
`Permission denied` (SELinux). La vía limpia de restauración es la **importación in-app**:
`Entrenar → menú ⋮ (680,144) → Importar datos → picker → archivo en `/sdcard/Download/...` → Reemplazar`
(`MainActivity.onArchivoSeleccionado` → `reemplazarTodo()` → migración aditiva → `save()`). Un
respaldo del estado limpio reimportado deja el archivo con md5 BYTE-idéntico (comprobado).

## Decisiones del flujo de migración (aprobadas por el usuario)

1. **Bloquear Guardar** en `EditRoutineActivity` mientras queden pendientes en esa rutina
   (gate + toast + abre el asistente; no guarda).
2. El reemplazo usa el **selector pre-filtrado por músculo** con **búsqueda prellenada**.
3. El selector (en general) muestra **solo ejercicios curados con ficha/GIF**; legacy y
   personalizados no aparecen.

## Implementación (archivos de la rebanada, sin commitear)

- `ui/EjercicioPicker.java`: overload
  `mostrar(activity, dataManager, dataStore, musculoInicial, textoInicial, listener)`; el viejo
  delega con `null`s.
- `ui/EjercicioPickerDialog.java`: `aplicarFiltro()` solo deja pasar `tieneFichaTecnica()`; chips
  derivados del set curado; (`musculoInicial`, `textoInicial`) preseleccionan chip y texto.
- `res/layout/activity_edit_routine.xml`: banner `card_migracion_ejercicios` +
  `text_migracion_descripcion` + `button_migrar_ejercicios`.
- `res/layout/item_ejercicio_rutina_edit.xml`: `text_aviso_migracion` ("Sin técnica — reemplázalo
  en la migración o márcalo como tuyo", color `cumplimiento_bajo`).
- `ui/RutinaEjercicioEditAdapter.java`: bind del aviso por pendiente.
- `ui/MigracionEjerciciosDialog.java` (nuevo): asistente "Ejercicio N de M"; botones Reemplazar /
  Marcarlo como creado por mí / Ahora no; `palabraClave()`, `abortar()`, `RoutineStep`, `deshacer()`.
- `res/layout/dialog_migracion_ejercicio.xml` (nuevo).
- `ui/EditRoutineActivity.java`: `contarPendientesMigracion()`, `actualizarBannerMigracion()`,
  `abrirMigracion()` (con `OnDismissListener → refrescarTrasMigracion()`), gate en `guardarRutina()`,
  banner actualizado en `onQuitar`.

### Bug encontrado y fix: fugas desde la migración en memoria

La migración muta objetos `Ejercicio` compartidos (vía `RutinaEjercicio.setEjercicioId` y
`setPersonalizado`). Un **cancelar posterior arrastra esos cambios al guardado** de otra pantalla
(misma trampa que renombrar un personalizado, pero con alcance global). Se detectó como drift real:

- "push" fila 1 quedó apuntando a `gv_0025` y `ex19` quedó `personalizado=true` (hash
  `04ff8ed2...`).

**Mejor detección:** en memoria la verificación parecía OK (hashes idénticos tras "Cancelar"),
pero un guardado posterior de otra rutina revelaba la fuga. **Fix aplicado y verificado:**
`abortar()` en "Ahora no"/back deshace la pasada (`RoutineStep.ejercicioIdOriginal` +
`personalizadoOriginal` + `deshacer()`); la terminación normal (`onTerminada → dismiss`) no deshace.

### Pre-fill con degradado elegante

`palabraClave()` prellena la primera palabra del nombre legacy **solo si existe en algún nombre del
catálogo curado**; si no ("Elevaciones laterales" vs "Elevación lateral en polea") devuelve `null`:
búsqueda vacía + chip de músculo pre-seleccionado. Un prefill que no empareja nada dejaba "No hay
ejercicios para este filtro" (callejón sin salida). No se tocó el trade-off documentado del picker
(sin folding de acentos a propósito).

## Verificaciones en el TECNO KJ5

1. **Selector curado-only:** no aparece "Press banca"; sí "Press de banca con barra".
2. **Pre-filtro:** al reemplazar "Press militar" abre con chip Hombros + texto "press".
3. **Banner + avisos por fila:** "4 ejercicios provienen de versiones anteriores…" + aviso en fila.
4. **Gate:** Guardar → toast + abre el asistente "Ejercicio 1 de 4 / Press banca".
5. **Abort-rollback:** reemplazar 2 pasos + "Ahora no" → fila restaurada ("Press banca"), banner a 4;
   guardar "prueba" → **md5 idéntico al respaldo** (no fuga).
6. **E2E real aprobado y restaurado:** migrar los 4 de "push" (`gv_0025` de banca, `gv_0091` press
   militar sentado, `gv_0289` press de banca con mancuernas, `gv_0178` elevación lateral en polea) +
   Guardar real → `push` referencia los 4 curados + `ex_b497814c`; los legacy quedaron intactos
   (`personalizado=false`); tras la prueba se restauró el respaldo → **md5 `ab5c8663` exacto**.

## Pendientes

- Commit del bloque de migración (pendiente de confirmación del usuario). Verificar con
  `git status`/`git diff` antes; el estilo del repo es un commit = una rebanada.
- Si se quiere cerrar la sesión: fin de la rebanada de migración; opcional probar en dispositivo una
  segunda rutina con pendientes (p. ej. "Pull" que tiene ejercicios con `ejercicioId` legacy).
- Está disponible el respaldo `/tmp/opencode/datos_respaldo_1789183950.json` por si hiciera falta
  restaurar otra vez; recordar que la aprobación del usuario es específica de cada respaldo.

## Archivos relevantes resumen (por prioridad de tocar)

- `app/src/main/java/com/ironquest/mvp/ui/MigracionEjerciciosDialog.java` — asistente + rollback.
- `app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java` — gate, banner, wiring.
- `app/src/main/java/com/ironquest/mvp/ui/EjercicioPickerDialog.java` / `EjercicioPicker.java`.
- `app/src/main/java/com/ironquest/mvp/ui/RutinaEjercicioEditAdapter.java`.
- `app/src/main/res/layout/activity_edit_routine.xml` / `item_ejercicio_rutina_edit.xml` /
  `dialog_migracion_ejercicio.xml`.
- Commit previo ya hecho: `ActiveSessionActivity.java` + `EjercicioSesionPagerAdapter.java` (Rebanada C).
- `/tmp/opencode/datos_respaldo_1789183950.json` (respaldo vigente).