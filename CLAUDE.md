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
model/    POJOs con campos públicos, sin getters/setters, sin constructores (salvo Ejercicio)
data/     DataManager — singleton, ÚNICO punto que toca Gson y el sistema de archivos
util/     Lógica pura y estática: cálculos, clasificaciones, hashing
ui/       Activities, Fragments, adapters, Views personalizadas, diálogos
service/  SesionTrackingService — servicio en primer plano de la sesión activa
```

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
- Modelos con **campos públicos**, sin encapsulación. Es una decisión consciente del proyecto.
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
Hay que llamar a `setTitle("...")` **después**:

```java
setSupportActionBar(toolbar);
setTitle("Mi físico");   // sin esto muestra "IronQuest"
```

### 2. Rotación destruye el estado en memoria

Cualquier Activity que mantenga estado de formulario en memoria —sobre todo si infla vistas
repetidas que comparten el mismo `@id`, donde la restauración automática de `View` no es
confiable— necesita:

```xml
android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"
```

Ya aplicado en `ActiveSessionActivity` y `PhysicalProfileActivity`.

### 3. Gson y los campos nuevos en `datos.json` viejo

**El comportamiento depende de si la clase declara un constructor explícito**, y en este
proyecto está repartido:

| Comportamiento de Gson | Clases |
|---|---|
| Usa el constructor implícito sin argumentos → **los inicializadores de campo SÍ corren** | `DataStore`, `Usuario`, `RegistroFisico` |
| No hay constructor sin argumentos → usa `UnsafeAllocator` → **los inicializadores se SALTAN** | `Sesion`, `Rutina`, `Ejercicio`, `EjercicioSesion`, `RutinaEjercicio`, `SerieSesion` |

Al agregar un campo a una clase **de la segunda fila** (la mayoría), un `datos.json` viejo que
no lo trae lo deja en el valor por defecto de la JVM, **no en el inicializador**:

- Campos **primitivos** (`int`, `double`, `boolean`) → 0 / 0.0 / false. Seguros.
- Campos de **tipo referencia** (`List`, `Integer`, `String`) → **`null`**, aunque estén
  escritos como `= new ArrayList<>()`. Hay que comprobar `null` explícitamente al leerlos.

`Sesion.ejercicios` es el ejemplo vivo: está declarado `= new ArrayList<>()` y ese
inicializador **nunca corre al deserializar**. Hoy funciona solo porque toda sesión ya
guardada trae el arreglo en el JSON.

Regla práctica: **para un campo nuevo, preferir un tipo primitivo.** Si tiene que ser una
colección, normalizarla a no-`null` en el constructor de `DataManager`, no confiar en el
inicializador.

Comprobar a qué grupo pertenece una clase:

```bash
cd app/src/main/java/com/ironquest/mvp/model
for f in *.java; do n="${f%.java}"; grep -qE "public +$n *\(" "$f" \
  && echo "$n: inicializadores SALTADOS" || echo "$n: inicializadores OK"; done
```

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

```bash
./gradlew assembleDebug          # APK en app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

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
