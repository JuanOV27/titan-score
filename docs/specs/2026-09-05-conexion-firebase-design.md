# Conexión con Firebase — diseño

> El profesor pidió ver una conexión con Firebase desde Android Studio, sin más detalle. El
> usuario define el alcance real: Authentication + los datos de perfil (peso, medidas, etc.),
> un checkbox obligatorio de términos y condiciones al registrarse, y un mecanismo para forzar
> o sugerir la actualización de la app desde un valor remoto.

## Alcance

- **Firebase Authentication** reemplaza el login/registro local (`PasswordUtil` + hash en
  `datos.json`) por cuentas reales en la nube (email/contraseña).
- **Firestore** guarda una copia del perfil (`Usuario` + `historialFisico`) como respaldo,
  asociada al usuario autenticado.
- **Términos y condiciones**: checkbox obligatorio al registrarse/vincular cuenta.
- **Verificación de versión**: un documento remoto decide si se avisa o se bloquea el uso de
  una versión vieja de la app.

Fuera de alcance, explícitamente: `rutinas`, `sesiones` y `ejercicios` siguen 100% locales en
`datos.json`, sin ningún cambio. XP, avatar y el resto del plan de slices no se tocan aquí.

## Arquitectura: Firestore como respaldo, no como fuente viva

La UI sigue leyendo siempre de `dataStore.getHistorialFisico()` / `getUsuario()`, exactamente
igual que hoy — **cero cambios** en `PhysicalHistoryActivity`, `PerfilFisicoUtil`,
`PhysicalProfileActivity`. Firestore solo recibe un push asíncrono "fire-and-forget" cuando algo
cambia localmente, y se lee una sola vez (al iniciar sesión) para reponer el perfil si el
teléfono está vacío (reinstalación o equipo nuevo).

Se eligió así — en vez de listeners en tiempo real sobre Firestore — porque el `CLAUDE.md` del
proyecto ya establece "sin ViewModel, sin corrutinas": un patrón de listeners obligaría a
reescribir esas pantallas a un modelo async, contradiciendo esa simplicidad deliberada.

**Invariante que se extiende:** hoy `CLAUDE.md` exige que nada fuera de `data/` toque `Gson`,
`File` ni `getFilesDir()`. Este diseño agrega la misma regla para Firebase: nada fuera de
`data/` importa `FirebaseAuth` ni `FirebaseFirestore` directamente. Tres clases nuevas, todas
en `data/`:

- `data/AuthManager.java` — único punto que toca `FirebaseAuth` (`registrar`, `iniciarSesion`,
  `usuarioActual`).
- `data/PerfilSync.java` — único punto que toca Firestore para el perfil: push completo
  (perfil + historial físico) y pull único post-login.
- `data/VersionChecker.java` — único punto que toca Firestore para `config/version`.

## Configuración inicial (consola Firebase)

Ya verificado en la consola (proyecto `titan-score`):

- App Android `com.ironquest.mvp` registrada. ✅
- Authentication → Email/contraseña habilitado. ✅
- Firestore creado, reglas de seguridad publicadas (ver más abajo). ✅

Pendiente, antes de poder compilar:

- Descargar `google-services.json` y colocarlo en `app/google-services.json`.
- Crear el documento `config/version` con los valores iniciales (sección de verificación de
  versión, más abajo).

Del lado del código: agregar `classpath 'com.google.gms:google-services'` en el `build.gradle`
raíz; aplicar el plugin + agregar Firebase BoM + `firebase-auth` + `firebase-firestore` en
`app/build.gradle`.

## Modelo de datos

`Usuario` (local, en `datos.json`) gana 3 campos y pierde 2:

```java
private String firebaseUid;              // null = cuenta local vieja, aún no vinculada
private boolean aceptoTerminos;
private String fechaAceptacionTerminos;
```

Se eliminan `passwordHash` y `coincideHash()` — Firebase valida la contraseña, ya no hace
falta guardar un hash local. `util/PasswordUtil.java` queda sin ningún llamador y se borra
completo.

Firestore:

```
usuarios/{uid}
  username, edad, fechaRegistro, aceptoTerminos, fechaAceptacionTerminos

usuarios/{uid}/historialFisico/{mismoIdLocal}   ← un documento por cada RegistroFisico
  fecha, alturaCm, pesoKg, imc, pechoCm, cinturaCm, caderaCm, brazoCm, piernaCm, tipoCuerpo

config/version   (documento único, lectura pública sin autenticación)
  minVersionCode, latestVersionCode, latestVersionName, notas, urlDescarga
```

Reglas de seguridad (ya publicadas):

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /usuarios/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
      match /historialFisico/{regId} {
        allow read, write: if request.auth != null && request.auth.uid == uid;
      }
    }
    match /config/{doc} {
      allow read: if true;
      allow write: if false;   // solo se edita a mano desde la consola
    }
  }
}
```

## Flujo de autenticación

`AuthActivity` pasa de comparar hashes locales a llamar a `AuthManager` (Firebase, async):

- **Registro:** valida campos + checkbox de términos marcado → `authManager.registrar(email,
  password, callback)`. En éxito: crea el `Usuario` local con el `firebaseUid` devuelto,
  `aceptoTerminos=true` + fecha, guarda, dispara `PerfilSync.pushCompleto(...)`.
- **Login:** `authManager.iniciarSesion(email, password, callback)`. En éxito: si el
  `datos.json` local no tiene perfil (teléfono nuevo/reinstalación), hace
  `PerfilSync.pullUnaVez(...)` para reponer `Usuario` + `historialFisico` desde Firestore.
- Errores de Firebase (`FirebaseAuthException` y variantes) se traducen a Toasts en español ya
  existentes en el patrón del formulario ("correo ya registrado", "contraseña incorrecta",
  "sin conexión", etc.).

El gate de `MainActivity` deja de mirar `usuario.isSesionActiva()` (campo que también queda
muerto y se borra) y pasa a comparar `AuthManager.usuarioActual()` contra el `firebaseUid`
guardado localmente.

`PerfilSync.pushCompleto(...)` no se dispara solo desde `AuthActivity`: también se agrega una
línea en `PhysicalProfileActivity`, justo después de su `dataManager.save()` existente al
registrar una medida nueva — es el mismo mecanismo de "reescribir todo el estado actual" descrito
en la sección de migración, no un caso aparte.

## Migración de la cuenta local que ya existe en el teléfono

El teléfono real tiene ahora mismo un `Usuario` con `passwordHash` (sin `firebaseUid`) y un
`historialFisico` real. Se detecta con `dataStore.getUsuario() != null &&
usuario.getFirebaseUid() == null`.

En ese caso, `AuthActivity` muestra un tercer modo, **"Vincula tu cuenta"**, en vez de los
modos normales de registro/login:

- Email pre-cargado y de solo lectura (el que ya tenía).
- Un solo campo nuevo: contraseña. La `passwordHash` local es SHA-256 de un solo sentido y no
  se puede reutilizar en Firebase — se explica en pantalla que hay que definir una contraseña
  nueva para la nube.
- El checkbox de términos aplica también aquí (primer contacto con la nube de esta cuenta).

Al confirmar, `authManager.registrar(email_existente, password_nueva, callback)` crea la
cuenta en Firebase (primera vez que ese correo existe ahí). En éxito, el `firebaseUid` se
**adjunta** al `Usuario` local ya existente — nunca se crea uno nuevo, nunca se tocan
`username`/`edad`/`fechaRegistro` — y se dispara el mismo `PerfilSync.pushCompleto(...)` que
sube perfil **y todo el historial físico acumulado hasta hoy**.

**Robustez del push, sin complejidad extra:** en vez de llevar una bandera de "ya migré" o un
mecanismo incremental, cada vez que cambia el perfil o el historial (incluida esta migración,
y también cada medida nueva que se registre después) se reescribe **todo** el estado actual en
un solo `WriteBatch` de Firestore. Si un intento falla por falta de red, el siguiente disparo
(el próximo guardado, o el próximo login) simplemente vuelve a mandar el estado completo tal
como está — no hay parches parciales que puedan quedar desalineados. Con el volumen de datos de
una persona (unas pocas decenas de registros físicos como mucho) esto es trivial para Firestore.

**Qué no toca esta migración:** rutinas, sesiones, ejercicios — 100% locales, sin cambios. El
`datos.json` real solo gana los 3 campos nuevos en `Usuario`; nunca se borra ni se sobrescribe
el resto.

**Prueba en el dispositivo real:** antes de instalar y ejercitar este flujo sobre la cuenta
real, se respalda `datos.json`, se muestra el cambio exacto, y se pide aprobación específica —
igual que en toda rebanada anterior que tocó datos reales.

## Términos y condiciones

- Checkbox nuevo en `activity_auth.xml`, visible solo en modo registro/vinculación: "Acepto los
  Términos y Condiciones y autorizo el uso de mis datos". Junto a él, un texto tipo enlace "Ver
  términos y condiciones".
- El enlace abre un `AlertDialog` con el texto completo, hardcodeado en Java (sin
  `strings.xml`, siguiendo la convención ya establecida del proyecto). Explica en lenguaje
  llano qué datos se piden (usuario, edad, medidas corporales/peso), que se guardan en Firebase
  asociados a la cuenta, y que rutinas/sesiones de entrenamiento siguen solo en el teléfono.
- Si el checkbox no está marcado, el submit corta con un Toast antes de llamar a Firebase —
  mismo patrón que las demás validaciones del formulario.
- Fuera de alcance a propósito: no se versiona el texto ni se fuerza re-aceptación si cambia
  más adelante. Se registra una sola vez, en el alta.

## Verificación de versión

Documento único `config/version`, leído sin autenticación desde el arranque.

Se dispara desde **ambos** puntos de entrada reales — `MainActivity` (launcher) y
`AuthActivity` (a la que `MainActivity` redirige sin sesión) — cada una con su propia lectura
async al principio de `onCreate`, protegida con `isFinishing()/isDestroyed()` antes de mostrar
cualquier diálogo. Se duplica a propósito: evita que el resultado llegue después de que
`MainActivity` ya redirigió y el diálogo intente mostrarse sobre una Activity ya cerrada. El
resto de las pantallas no necesita chequeo propio — solo se llega a ellas pasando por una de
estas dos.

No bloquea el arranque: el resto de `onCreate` sigue de inmediato; sin red, o si Firestore no
responde, se interpreta como "nada que avisar", nunca se bloquea la app por no poder verificar.

- `versionCode instalado < minVersionCode` → diálogo **no cancelable**, un solo botón
  "Actualizar" → `Intent(ACTION_VIEW, urlDescarga)`.
- `minVersionCode ≤ versionCode instalado < latestVersionCode` → diálogo descartable,
  "Actualizar" / "Ahora no".
- Si ya está al día → no se muestra nada.

Valores iniciales del documento (no bloquean nada de lo ya instalado — versionCode actual = 5,
version 0.4.0):

```
minVersionCode: 5
latestVersionCode: 5
latestVersionName: "0.4.0"
notas: "Estás al día."
urlDescarga: "https://juanov27.github.io/titan-score-app/"
```

Cuando se publique la versión que incluya esta conexión con Firebase (versionCode 6), subir
`latestVersionCode` a 6 en la consola es un paso manual más del proceso de publicación que ya
documenta `CLAUDE.md`.

## Fuera de alcance

- Rutinas, sesiones, ejercicios: sin cambios, 100% locales.
- Sincronización en tiempo real / multi-dispositivo simultáneo (listeners de Firestore).
- Recuperación de contraseña ("olvidé mi contraseña") — no se pidió, se puede agregar después
  con `sendPasswordResetEmail` sin rediseñar nada de esto.
- Logout explícito — hoy tampoco existe; sigue sin existir. `AuthManager` expone lo necesario
  para agregarlo después sin fricción.
- Versionar o forzar re-aceptación de términos.

## Commits

1. **"Conectar Firebase: Authentication + perfil/historial en Firestore + términos y
   condiciones"** — configuración de Gradle, `AuthManager`, `PerfilSync`, cambios de modelo,
   los tres modos de `AuthActivity`, gate de `MainActivity`, checkbox + diálogo de términos,
   borrado de `PasswordUtil`. Se agrupa en un solo commit porque la migración de la cuenta real
   no tiene sentido sin subir también su historial — separarlos dejaría un estado a medias.
2. **"Agregar verificación de versión obligatoria/opcional vía Firestore"** — `VersionChecker`,
   enganches en `MainActivity`/`AuthActivity`, documento `config/version`. Independiente del
   commit 1, no depende de Auth ni de Firestore de perfil.

## Verificación en dispositivo

Para el commit 1: `./gradlew assembleDebug` → respaldo de `datos.json` real → instalar →
confirmar que aparece el modo "Vincula tu cuenta" (no "Crea tu cuenta") → completar con una
contraseña nueva → confirmar en la consola de Firebase que aparece el usuario en Authentication
y el documento `usuarios/{uid}` con el historial físico completo → cerrar la app, reinstalar
(simulando equipo nuevo) → iniciar sesión → confirmar que el perfil y el historial se
recuperan desde Firestore → `datos.json` conserva rutinas/sesiones intactas.

Para el commit 2: cambiar a mano `minVersionCode` a un número mayor al instalado → abrir la
app → confirmar el diálogo no cancelable → restaurar el valor original en la consola.
