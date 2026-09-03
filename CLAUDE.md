# Titan Score — guía para Claude

App Android de trazabilidad de entrenamiento con gamificación. Nombre oficial **Titan Score**;
el paquete y los identificadores internos todavía usan **IronQuest** como nombre de trabajo.

Contexto del proyecto: `docs/propuesta_app_gimnasio_gamificado.md` define el alcance por fases.
La regla que ordena todo el alcance está en su §5.1: *toda funcionalidad debe servir al ciclo
"registro de entrenamiento → progreso del personaje"*. Lo que no aporta a ese ciclo queda fuera
(§5.2: nutrición, red social, wearables, generación de rutinas con IA).

## Stack y arquitectura

- **Java**, Android Studio, Gradle. `minSdk 26`, `compileSdk`/`targetSdk 37`.
- **Material3** (`Theme.Material3.DayNight.NoActionBar`), acento naranja `#FF6B35`.
- **Sin backend.** Persistencia en un único `datos.json` (Gson) en `context.getFilesDir()`.
- **Deliberadamente simple:** sin Room, sin inyección de dependencias, sin Navigation Component,
  sin ViewModel, sin corrutinas. No introducir ninguna de estas sin pedirlo explícitamente.
- Dependencias: material, appcompat, recyclerview, cardview, coordinatorlayout,
  constraintlayout, gson. **No agregar dependencias nuevas sin justificarlo.**

### Capas

```
model/    Entidades encapsuladas: campos privados, getters/setters, constructores
data/     DataManager — singleton, ÚNICO punto que toca Gson y el sistema de archivos
util/     Lógica pura y estática: cálculos, clasificaciones, hashing
ui/       Activities, Fragments, adapters, Views personalizadas, diálogos
service/  SesionTrackingService — servicio en primer plano de la sesión activa
```

Mapa completo de los cuatro pilares de POO (qué clase, qué línea) en
`docs/diseno_orientado_a_objetos.md`. Diseño del refactor en
`docs/specs/2026-09-02-refactor-poo-design.md`.

**Invariante crítica:** nada fuera de `data/` importa `Gson`, `File`, `FileReader`,
`FileWriter` ni llama a `getFilesDir()`. Esto es lo que hará viable la migración a Firebase
más adelante. Verificable con:

```bash
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Debe devolver vacío.

## Convenciones de código

- **Dominio en español** (`Rutina`, `Sesion`, `calcularRacha`, `volumenReal`), **API de Android
  en inglés** (`onCreate`, `findViewById`, `LayoutInflater`).
- Modelos con **campos privados** + getters/setters + constructores. Requisito académico
  (Móviles 2): el código debe presentar estructura de POO explícita. Las cinco entidades con
  identidad propia (`Ejercicio`, `Rutina`, `Sesion`, `RegistroFisico`, `Usuario`) heredan de
  `EntidadIdentificable` (campo `id` común). El motor de progresión usa polimorfismo real:
  `util/progresion/EstrategiaProgresion` (abstracta) + 4 subclases, en vez de un `switch` sobre
  constantes. Las Activities con toolbar heredan de `ui/BaseActivity`. Detalle completo con
  ejemplos de código en `docs/diseno_orientado_a_objetos.md`.
- IDs generados con `DataManager.newId(prefijo)` → `"pref_a1b2c3d4"`.
- Fechas como `String` ISO (`LocalDate.now().toString()` / `LocalDateTime`), nunca `Date`.
- Texto de UI **hardcodeado** en layouts y Java. `strings.xml` solo tiene `app_name`. No
  extraer a recursos salvo que se pida.
- Antes de crear una utilidad, **buscar si ya existe**. Ya hay: `EstadisticasUtil`
  (`calcularRachaDias`), `PerfilFisicoUtil` (IMC, ICC, ICEst, V-taper, recomendaciones),
  `PasswordUtil` (SHA-256), `BarChartView` (gráficas de barras dibujadas a mano),
  `EjercicioPicker`, `SimpleTextWatcher`, `RestTimerDialog`.

## Trampas conocidas (todas costaron un bug real)

### 1. `setSupportActionBar()` pisa el título del XML

Sobrescribe el `android:title` del `MaterialToolbar` con el `android:label` del manifiesto.
Hay que llamar a `setTitle("...")` **después**. Ya no hay que recordarlo a mano: las Activities
con toolbar heredan de `BaseActivity` y llaman a `configurarToolbar(...)`, que hace los dos
pasos en el orden correcto:

```java
configurarToolbar(R.id.toolbar, "Mi físico", true);   // true = flecha atrás que cierra la Activity
```

Si el título depende de datos que aún no cargaron, pasar `null` y llamar a `setTitle(...)` más
tarde en el mismo `onCreate` — sigue siendo seguro porque `setSupportActionBar()` ya corrió.

### 2. Rotación destruye el estado en memoria

Cualquier Activity que mantenga estado de formulario en memoria —sobre todo si infla vistas
repetidas que comparten el mismo `@id`, donde la restauración automática de `View` no es
confiable— necesita:

```xml
android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
```

Ya aplicado en `ActiveSessionActivity` y `PhysicalProfileActivity`.

### 3. Gson y los campos nuevos en `datos.json` viejo

Desde el refactor a POO, **las 9 clases de `model/` declaran constructor sin argumentos**
(privado en las que antes no lo tenían, para que solo Gson lo use). Gson lo detecta y **los
inicializadores de campo SÍ corren** al deserializar, en las 9 — ya no hay dos grupos con
comportamiento distinto como antes.

Sigue aplicando la regla de fondo, ahora sin excepciones:

- Campos **primitivos** (`int`, `double`, `boolean`) → siempre seguros, con o sin
  inicializador, porque el valor por defecto de la JVM y el inicializador explícito coinciden
  si no se escribe uno distinto.
- Campos de **tipo referencia** (`List`, `Integer`, `String`) → si el `datos.json` viejo no
  trae la clave, el inicializador de campo corre y deja el valor por defecto declarado (p. ej.
  `new ArrayList<>()`, no `null`). Si la clave está presente pero con `null` explícito (un
  respaldo importado manualmente, por ejemplo), el inicializador **no** se usa — Gson asigna
  `null` igual. `DataStore.normalizarColecciones()` cubre ese caso para las colecciones de
  nivel raíz; se llama desde `DataManager.load()` y `leerDataStoreDesde()`.

Regla práctica: **para un campo nuevo, seguir prefiriendo un tipo primitivo** cuando el dato
lo permita — sigue siendo la opción más simple. Para una colección nueva, el inicializador de
campo ya es confiable contra `datos.json` viejo; solo hay que normalizar explícitamente si el
campo puede llegar en `null` por una vía distinta a "clave ausente" (importación, por ejemplo).

### 4. `DataManager.load()` no puede llamar a `save()`

`load()` corre durante la construcción, **antes** de que `this.dataStore` esté asignado.
Llamar a `save()` desde dentro serializaría `null` y **dejaría el `datos.json` vacío**.
Cualquier migración o relleno de datos va en el **constructor, después de
`dataStore = load();`**, y normalmente también al final de `reemplazarTodo()` para que un
respaldo importado reciba el mismo tratamiento.

### 5. Migraciones del catálogo de ejercicios

`load()` solo siembra el catálogo cuando `ejercicios.isEmpty()`. Una instalación existente
**nunca** recibe ejercicios nuevos por esa vía. Además, los IDs `ex1..ex20` están
referenciados por `RutinaEjercicio.ejercicioId` y `EjercicioSesion.ejercicioId` en datos
reales. Toda migración del catálogo debe ser **aditiva y con marcador de versión**, nunca
destructiva.

### 6. `PendingIntent` con `FLAG_UPDATE_CURRENT` no cambia el componente destino

Si se cambia a qué Activity apunta una notificación de `SesionTrackingService`, hay que
**subir también el request code**, o el sistema reutiliza el intent viejo.

## Datos reales del usuario — regla no negociable

El teléfono conectado tiene **datos reales de entrenamiento** en
`/data/data/com.ironquest.mvp/files/datos.json`.

- **Respaldar antes de cualquier prueba con riesgo.**
- **Nunca escribir sobre ese archivo sin presentar el cambio exacto y obtener aprobación
  explícita.** La aprobación es **específica**: autorizar "probar y restaurar desde el
  respaldo X" no autoriza restaurar desde otro archivo, por bienintencionado que sea.
- Nunca guardar datos de prueba fabricados en el historial real.
- Ojo con los pipes encadenados: `cat archivo_inexistente | adb shell "run-as ... cat > files/datos.json"`
  **trunca el archivo a vacío** aunque el `cat` falle. Ya ocurrió una vez.

Lectura segura:

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json
```

## Compilar, verificar, publicar

**No hay JDK en el `PATH`.** Hay que apuntar `JAVA_HOME` al JBR que trae Android Studio, o
Gradle falla con `JAVA_HOME is not set and no 'java' command could be found`:

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Cuidado al encadenar con `&&`: `./gradlew ... | tail` devuelve el código de salida de `tail`,
no el de Gradle, y un build fallido puede parecer exitoso. Comprobar `${PIPESTATUS[0]}` o no
usar pipe.

Verificación en dispositivo físico vía ADB:

```bash
adb shell uiautomator dump && adb shell cat /sdcard/window_dump.xml   # estado de la UI
adb shell dumpsys package com.ironquest.mvp | grep version            # versión instalada
adb shell dumpsys power | grep mWakefulness                           # ¿pantalla dormida?
```

Si la pantalla está dormida, `am start` y la interacción con la UI fallan de forma confusa:
despertarla con `input keyevent KEYCODE_WAKEUP` seguido de `input keyevent 82`.

Las Activities con `exported="false"` **rechazan** `adb shell am start -n` con
`SecurityException`. Es correcto, no es un bug: navegar por la UI en vez de lanzarlas directo.

### Publicación

Dos repositorios:

- **`titan-score`** (privado) — este código fuente.
- **`titan-score-app`** (público) — página de descargas. Su `app.js` consulta la API de
  GitHub Releases en tiempo de ejecución, así que **publicar una versión nueva no requiere
  ningún cambio de código en ese repo**: solo un Release nuevo con el APK adjunto.

```bash
# subir versionCode y versionName en app/build.gradle, compilar, verificar en el teléfono, commit
gh release create vX.Y.Z ruta/al/titan-score.apk \
  --repo JuanOV27/titan-score-app --title "..." --notes-file notas.md
```

El asset debe llamarse `titan-score.apk`.

## Metodología de trabajo

- **Una sesión = una rebanada de trabajo = un commit**, verificado en el teléfono antes de
  commitear. El historial de git es el documento de traspaso entre sesiones.
- **No arrastrar una sesión más allá de una compactación de contexto.** Al ver el primer
  `/compact`, cerrar la rebanada en curso y empezar sesión nueva.
- Usar subagentes de exploración para las preguntas de "¿dónde está X?", en vez de gastar el
  contexto de la sesión principal leyendo archivos.
- Mensajes de commit en español, imperativos, en una línea (ver `git log`).
