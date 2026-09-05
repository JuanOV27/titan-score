# Conexión con Firebase — Plan de implementación

> **Para el ejecutor:** usar superpowers:subagent-driven-development (recomendado) o
> superpowers:executing-plans para ejecutar tarea por tarea. Pasos con checkbox (`- [ ]`).

**Objetivo:** reemplazar el login local por Firebase Authentication, respaldar el perfil
(`Usuario` + `historialFisico`) en Firestore, exigir aceptar términos y condiciones al
registrarse, y avisar/bloquear cuando haya una versión nueva obligatoria.

**Arquitectura:** tres clases nuevas en `data/` (`AuthManager`, `PerfilSync`,
`VersionChecker`) son el único código que toca `FirebaseAuth`/`Firestore`, igual que
`DataManager` es hoy el único que toca `Gson`/archivos. La UI sigue leyendo siempre de
`datos.json` local (`dataStore.getUsuario()`/`getHistorialFisico()`); Firestore es un
respaldo "write-behind" (push completo tras cada cambio), nunca una fuente que la UI
escuche en vivo. Detalle completo del razonamiento en
`docs/specs/2026-09-05-conexion-firebase-design.md`.

**Tech Stack:** Firebase Authentication (email/contraseña), Cloud Firestore, Firebase BoM
`34.18.0`, plugin `com.google.gms.google-services` `4.5.0`.

## Global Constraints

- Nada fuera de `data/` importa `FirebaseAuth` ni `FirebaseFirestore` (extiende la
  invariante ya existente de `CLAUDE.md` sobre Gson/File).
- Texto de UI hardcodeado en Java/XML — nada nuevo en `strings.xml`.
- Sin ViewModel, sin corrutinas, sin Room, sin DI — cualquier callback usa
  `OnCompleteListener`/`OnSuccessListener` planos de las Task de Firebase.
- **Adaptación al método de verificación de este proyecto:** no hay JUnit/Espresso en el
  repo — el proyecto verifica cada rebanada compilando, instalando en el teléfono real y
  navegando la UI a mano (`CLAUDE.md`, sección "Metodología"). Cada tarea de este plan
  termina en una verificación de compilación; **el commit real ocurre solo en la última
  tarea de cada rebanada** (Tarea 9 y Tarea 13), después de verificar en el dispositivo —
  igual que las 20 rebanadas anteriores de este proyecto, documentadas en su historial de
  git. Esto reemplaza el "commit por tarea" genérico de esta skill.
- Antes de instalar sobre el teléfono con la cuenta real, respaldar `datos.json` y mostrar
  el cambio exacto — regla no negociable ya en `CLAUDE.md`.

---

## Rebanada 1 — Firebase Authentication + perfil/historial en Firestore + términos

### Tarea 1: Configuración de Gradle, manifiesto y `google-services.json`

**Files:**
- Modify: `build.gradle` (raíz)
- Modify: `app/build.gradle`
- Modify: `app/src/main/AndroidManifest.xml:1-8`
- Create (por el usuario, no por código): `app/google-services.json`

**Interfaces:**
- Produce: dependencias `firebase-auth` y `firebase-firestore` disponibles para las
  Tareas 3, 4, 6, 7, 8, 11. `BuildConfig.VERSION_CODE` disponible para la Tarea 12.

- [ ] **Paso 1: Obtener y colocar `app/google-services.json` (bloqueante)**

Verificado el 2026-09-05: este archivo **no existe todavía** en el proyecto ni en
`~/Descargas`. Es un prerrequisito que bloquea el resto de esta tarea — sin él, el build
falla con `File google-services.json is missing`.

```bash
ls "app/google-services.json" 2>&1
```
Si no existe: en Firebase Console → Configuración del proyecto → General → la app Android
`com.ironquest.mvp` → botón "Descargar google-services.json" (proyecto `titan-score`, ya
registrado). Colocarlo en `app/google-services.json` (mismo nivel que `app/build.gradle`,
NO dentro de `app/src/`).

- [ ] **Paso 2: Agregar `google-services.json` a `.gitignore`**

Es una credencial de proyecto (identifica la app ante Firebase), no código fuente — no
debe subirse al repositorio. Agregar esta línea a `.gitignore`:

```
app/google-services.json
```

- [ ] **Paso 3: Agregar el plugin al `build.gradle` raíz**

Reemplazar el contenido completo de `build.gradle` (raíz) por:

```groovy
plugins {
    id 'com.android.application' version '9.3.2' apply false
    id 'com.google.gms.google-services' version '4.5.0' apply false
}
```

- [ ] **Paso 4: Aplicar el plugin y agregar las dependencias en `app/build.gradle`**

Reemplazar el contenido completo de `app/build.gradle` por:

```groovy
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}

android {
    namespace 'com.ironquest.mvp'
    compileSdk 37

    defaultConfig {
        applicationId "com.ironquest.mvp"
        minSdk 26
        targetSdk 37
        versionCode 5
        versionName "0.4.0"
    }

    buildTypes {
        release {
            minifyEnabled false
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig true
    }
}

dependencies {
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.code.gson:gson:2.11.0'

    implementation platform('com.google.firebase:firebase-bom:34.18.0')
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-firestore'
}
```

`buildFeatures { buildConfig true }` es obligatorio: desde AGP 8.0 ese valor por defecto es
`false`, y sin él `BuildConfig.VERSION_CODE` (que se usa en la Tarea 12) no compila.

- [ ] **Paso 5: Agregar permisos de red al manifiesto**

En `app/src/main/AndroidManifest.xml`, agregar estas dos líneas junto a los
`<uses-permission>` ya existentes (líneas 4-7):

```xml
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

El proyecto no tenía ningún permiso de red — era 100% local hasta ahora. Sin
`INTERNET`, todas las llamadas a Firebase fallan en tiempo de ejecución.

- [ ] **Paso 6: Verificar que compila**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Si falla con `File google-services.json is missing`, volver
al Paso 1.

---

### Tarea 2: Modelo `Usuario` + borrar `PasswordUtil`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/model/Usuario.java`
- Delete: `app/src/main/java/com/ironquest/mvp/util/PasswordUtil.java`

**Interfaces:**
- Produce: `Usuario.getFirebaseUid()/setFirebaseUid(String)`,
  `Usuario.isAceptoTerminos()/setAceptoTerminos(boolean)`,
  `Usuario.getFechaAceptacionTerminos()/setFechaAceptacionTerminos(String)` — usados por
  las Tareas 4 y 6.

- [ ] **Paso 1: Reemplazar el contenido completo de `Usuario.java`**

```java
package com.ironquest.mvp.model;

/** La cuenta, vinculada a Firebase Authentication mediante {@code firebaseUid}. */
public class Usuario extends EntidadIdentificable {

    private String username;
    private String email;
    private int edad;
    private String fechaRegistro;
    private String firebaseUid;
    private boolean aceptoTerminos;
    private String fechaAceptacionTerminos;

    /** Público: el registro construye el usuario vacío y lo va llenando campo a campo. */
    public Usuario() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public boolean isAceptoTerminos() {
        return aceptoTerminos;
    }

    public void setAceptoTerminos(boolean aceptoTerminos) {
        this.aceptoTerminos = aceptoTerminos;
    }

    public String getFechaAceptacionTerminos() {
        return fechaAceptacionTerminos;
    }

    public void setFechaAceptacionTerminos(String fechaAceptacionTerminos) {
        this.fechaAceptacionTerminos = fechaAceptacionTerminos;
    }

    @Override
    public String toString() {
        return username != null ? username : super.toString();
    }
}
```

Campos eliminados respecto a la versión anterior: `passwordHash`, `sesionActiva`, y el
método `coincideHash(String)`. Firebase valida la contraseña; el gate de sesión pasa a
`AuthManager.usuarioActual()` (Tarea 7).

- [ ] **Paso 2: Borrar `PasswordUtil.java`**

```bash
rm "app/src/main/java/com/ironquest/mvp/util/PasswordUtil.java"
```

- [ ] **Paso 3: Confirmar que no queda ningún uso**

```bash
grep -rn "PasswordUtil\|passwordHash\|coincideHash\|sesionActiva\|isSesionActiva" app/src/main/java
```
Expected: sin resultados (los usos en `AuthActivity.java` y `MainActivity.java` se
resuelven en las Tareas 6 y 7 — es normal que compile roto entre esta tarea y esa).

- [ ] **Paso 4: Compilar el módulo `model` de forma aislada no aplica en Gradle** — pasar
directamente a la Tarea 3; la compilación completa se confirma al cierre de la Tarea 9.

---

### Tarea 3: `data/AuthManager.java`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/data/AuthManager.java`

**Interfaces:**
- Consume: `com.google.firebase.auth.FirebaseAuth` (de la Tarea 1).
- Produce: `AuthManager.getInstance()`, `.usuarioActual(): FirebaseUser`,
  `.registrar(String email, String password, AuthManager.Callback callback)`,
  `.iniciarSesion(String email, String password, AuthManager.Callback callback)`,
  interfaz `AuthManager.Callback { void onExito(String uid); void onError(String mensaje); }`
  — consumidos por las Tareas 6 y 7.

- [ ] **Paso 1: Crear el archivo completo**

```java
package com.ironquest.mvp.data;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

/** Único punto que toca {@link FirebaseAuth} en todo el proyecto. */
public class AuthManager {

    /** Resultado de un registro o inicio de sesión. */
    public interface Callback {
        void onExito(String uid);
        void onError(String mensaje);
    }

    private static AuthManager instance;

    private final FirebaseAuth firebaseAuth;

    private AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    @Nullable
    public FirebaseUser usuarioActual() {
        return firebaseAuth.getCurrentUser();
    }

    public void registrar(String email, String password, Callback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(resultado -> callback.onExito(resultado.getUser().getUid()))
                .addOnFailureListener(e -> callback.onError(traducirError(e)));
    }

    public void iniciarSesion(String email, String password, Callback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(resultado -> callback.onExito(resultado.getUser().getUid()))
                .addOnFailureListener(e -> callback.onError(traducirError(e)));
    }

    private static String traducirError(Exception excepcion) {
        if (excepcion instanceof FirebaseAuthUserCollisionException) {
            return "Ese correo ya tiene una cuenta";
        }
        if (excepcion instanceof FirebaseAuthInvalidCredentialsException) {
            return "Correo o contraseña incorrectos";
        }
        if (excepcion instanceof FirebaseAuthInvalidUserException) {
            return "No existe una cuenta con ese correo";
        }
        if (excepcion instanceof FirebaseAuthWeakPasswordException) {
            return "La contraseña es demasiado débil";
        }
        if (excepcion instanceof FirebaseNetworkException) {
            return "Sin conexión a internet";
        }
        return excepcion.getMessage() != null ? excepcion.getMessage() : "No se pudo completar la operación";
    }
}
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

---

### Tarea 4: `data/PerfilSync.java`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/data/PerfilSync.java`

**Interfaces:**
- Consume: `Usuario` (Tarea 2), `RegistroFisico` (existente,
  `getId/setId/getFecha/setFecha/getAlturaCm/setAlturaCm/getPesoKg/setPesoKg/getImc/setImc/
  getPechoCm/setPechoCm/getCinturaCm/setCinturaCm/getCaderaCm/setCaderaCm/getBrazoCm/
  setBrazoCm/getPiernaCm/setPiernaCm/getTipoCuerpo/setTipoCuerpo`).
- Produce: `PerfilSync.getInstance()`,
  `.pushCompleto(String uid, Usuario usuario, List<RegistroFisico> historial)`,
  `.pullUnaVez(String uid, PerfilSync.CallbackPull callback)`, interfaz
  `PerfilSync.CallbackPull { void onListo(Usuario, List<RegistroFisico>); void onError(String); }`
  — consumidos por las Tareas 6 y 8.

- [ ] **Paso 1: Crear el archivo completo**

```java
package com.ironquest.mvp.data;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.model.Usuario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Único punto que toca Firestore para el perfil del usuario. Respaldo "write-behind": cada
 * push reescribe TODO el estado actual (no hay sincronización incremental ni bandera de
 * "ya migré"), así un intento fallido simplemente se corrige solo en el próximo push.
 */
public class PerfilSync {

    public interface CallbackPull {
        void onListo(Usuario usuario, List<RegistroFisico> historial);
        void onError(String mensaje);
    }

    private static PerfilSync instance;

    private final FirebaseFirestore firestore;

    private PerfilSync() {
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized PerfilSync getInstance() {
        if (instance == null) {
            instance = new PerfilSync();
        }
        return instance;
    }

    public void pushCompleto(String uid, Usuario usuario, List<RegistroFisico> historial) {
        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection("usuarios").document(uid), mapaDeUsuario(usuario));
        for (RegistroFisico registro : historial) {
            batch.set(firestore.collection("usuarios").document(uid)
                    .collection("historialFisico").document(registro.getId()), mapaDeRegistro(registro));
        }
        batch.commit();
    }

    /** Lectura única, pensada para reponer el perfil en un teléfono nuevo o reinstalado. */
    public void pullUnaVez(String uid, CallbackPull callback) {
        firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documento -> {
                    if (!documento.exists() || documento.getData() == null) {
                        callback.onError("No hay perfil guardado en la nube todavía");
                        return;
                    }
                    Usuario usuario = usuarioDesdeMapa(uid, documento.getData());
                    firestore.collection("usuarios").document(uid).collection("historialFisico").get()
                            .addOnSuccessListener(coleccion -> {
                                List<RegistroFisico> historial = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : coleccion) {
                                    historial.add(registroDesdeMapa(doc.getId(), doc.getData()));
                                }
                                callback.onListo(usuario, historial);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private static Map<String, Object> mapaDeUsuario(Usuario usuario) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("username", usuario.getUsername());
        mapa.put("edad", usuario.getEdad());
        mapa.put("fechaRegistro", usuario.getFechaRegistro());
        mapa.put("aceptoTerminos", usuario.isAceptoTerminos());
        mapa.put("fechaAceptacionTerminos", usuario.getFechaAceptacionTerminos());
        return mapa;
    }

    private static Usuario usuarioDesdeMapa(String uid, Map<String, Object> mapa) {
        Usuario usuario = new Usuario();
        usuario.setFirebaseUid(uid);
        usuario.setUsername((String) mapa.get("username"));
        usuario.setEdad(entero(mapa.get("edad")));
        usuario.setFechaRegistro((String) mapa.get("fechaRegistro"));
        Object acepto = mapa.get("aceptoTerminos");
        usuario.setAceptoTerminos(acepto instanceof Boolean && (Boolean) acepto);
        usuario.setFechaAceptacionTerminos((String) mapa.get("fechaAceptacionTerminos"));
        return usuario;
    }

    private static Map<String, Object> mapaDeRegistro(RegistroFisico registro) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("fecha", registro.getFecha());
        mapa.put("alturaCm", registro.getAlturaCm());
        mapa.put("pesoKg", registro.getPesoKg());
        mapa.put("imc", registro.getImc());
        mapa.put("pechoCm", registro.getPechoCm());
        mapa.put("cinturaCm", registro.getCinturaCm());
        mapa.put("caderaCm", registro.getCaderaCm());
        mapa.put("brazoCm", registro.getBrazoCm());
        mapa.put("piernaCm", registro.getPiernaCm());
        mapa.put("tipoCuerpo", registro.getTipoCuerpo());
        return mapa;
    }

    private static RegistroFisico registroDesdeMapa(String id, Map<String, Object> mapa) {
        RegistroFisico registro = new RegistroFisico();
        registro.setId(id);
        registro.setFecha((String) mapa.get("fecha"));
        registro.setAlturaCm(numero(mapa.get("alturaCm")));
        registro.setPesoKg(numero(mapa.get("pesoKg")));
        registro.setImc(numero(mapa.get("imc")));
        registro.setPechoCm(numero(mapa.get("pechoCm")));
        registro.setCinturaCm(numero(mapa.get("cinturaCm")));
        registro.setCaderaCm(numero(mapa.get("caderaCm")));
        registro.setBrazoCm(numero(mapa.get("brazoCm")));
        registro.setPiernaCm(numero(mapa.get("piernaCm")));
        registro.setTipoCuerpo((String) mapa.get("tipoCuerpo"));
        return registro;
    }

    private static double numero(Object valor) {
        return valor instanceof Number ? ((Number) valor).doubleValue() : 0.0;
    }

    private static int entero(Object valor) {
        return valor instanceof Number ? ((Number) valor).intValue() : 0;
    }
}
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

---

### Tarea 5: Layout `activity_auth.xml`

**Files:**
- Modify: `app/src/main/res/layout/activity_auth.xml`

**Interfaces:**
- Produce: ids `text_auth_subtitulo`, `layout_input_password`, `layout_terminos`,
  `check_terminos`, `text_ver_terminos` — usados por la Tarea 6.

- [ ] **Paso 1: Reemplazar el contenido completo del archivo**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center_horizontal"
        android:padding="24dp">

        <View
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_weight="1" />

        <TextView
            android:id="@+id/text_auth_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="Crea tu cuenta"
            android:textStyle="bold"
            android:textSize="24sp" />

        <TextView
            android:id="@+id/text_auth_subtitulo"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:gravity="center"
            android:text="@string/app_name"
            android:textSize="14sp" />

        <LinearLayout
            android:id="@+id/layout_campos_registro"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="24dp"
                android:hint="Nombre de usuario">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_username"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="text" />

            </com.google.android.material.textfield.TextInputLayout>

        </LinearLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:hint="Correo electrónico">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/edit_email"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textEmailAddress" />

        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/layout_input_password"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:hint="Contraseña"
            app:passwordToggleEnabled="true">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/edit_password"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="textPassword" />

        </com.google.android.material.textfield.TextInputLayout>

        <LinearLayout
            android:id="@+id/layout_campo_edad"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical">

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="16dp"
                android:hint="Edad">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_edad"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number" />

            </com.google.android.material.textfield.TextInputLayout>

        </LinearLayout>

        <LinearLayout
            android:id="@+id/layout_terminos"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:orientation="vertical">

            <CheckBox
                android:id="@+id/check_terminos"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Acepto los Términos y Condiciones y autorizo el uso de mis datos" />

            <TextView
                android:id="@+id/text_ver_terminos"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:padding="4dp"
                android:text="Ver términos y condiciones"
                android:textColor="?attr/colorPrimary"
                android:textSize="13sp" />

        </LinearLayout>

        <com.google.android.material.button.MaterialButton
            android:id="@+id/button_auth_submit"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:text="Registrarme" />

        <TextView
            android:id="@+id/text_toggle_auth_mode"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:padding="8dp"
            android:text="¿Ya tienes cuenta? Inicia sesión"
            android:textColor="?attr/colorPrimary"
            android:textSize="14sp"
            android:visibility="gone" />

        <View
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_weight="2" />

    </LinearLayout>

</ScrollView>
```

- [ ] **Paso 2: Verificar que el layout infla sin error**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL` (fallará igual que en la Tarea 2 hasta que la Tarea 6 actualice
`AuthActivity.java` — si el único error mencionado es sobre `AuthActivity.java`, el layout
en sí está bien).

---

### Tarea 6: Reescribir `AuthActivity.java`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/AuthActivity.java`

**Interfaces:**
- Consume: `AuthManager` (Tarea 3), `PerfilSync` (Tarea 4), ids de la Tarea 5,
  `Usuario` (Tarea 2).
- Produce: nada que otras tareas consuman directamente (es punto de entrada de UI).

- [ ] **Paso 1: Reemplazar el contenido completo del archivo**

```java
package com.ironquest.mvp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.AuthManager;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.data.PerfilSync;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.model.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AuthActivity extends AppCompatActivity {

    private enum Modo { REGISTRO, LOGIN, VINCULAR }

    private static final String TERMINOS_TEXTO =
            "Al usar Titan Score, aceptas que la app guarde tu nombre de usuario, correo "
            + "electrónico, edad y las medidas corporales que registres (peso, altura, pecho, "
            + "cintura, cadera, brazo, pierna) asociadas a tu cuenta, con el fin de mostrarte tu "
            + "progreso físico a lo largo del tiempo.\n\n"
            + "Estos datos se almacenan de dos formas: localmente en tu teléfono, y como "
            + "respaldo en Firebase (Google), un servicio en la nube, para que no los pierdas si "
            + "cambias de equipo o reinstalas la app.\n\n"
            + "Tus rutinas, ejercicios y sesiones de entrenamiento se guardan únicamente en tu "
            + "teléfono y no se suben a ningún servidor.\n\n"
            + "No compartimos tus datos con terceros. Puedes dejar de usar la app en cualquier "
            + "momento; los datos seguirán guardados hasta que los elimines manualmente.";

    private DataManager dataManager;
    private DataStore dataStore;
    private AuthManager authManager;
    private Modo modo;
    private boolean esVinculacion;

    private TextView textTitulo;
    private TextView textAuthSubtitulo;
    private View layoutCamposRegistro;
    private View layoutCampoEdad;
    private View layoutTerminos;
    private TextInputLayout layoutInputPassword;
    private TextInputEditText editUsername;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextInputEditText editEdad;
    private CheckBox checkTerminos;
    private TextView textVerTerminos;
    private MaterialButton buttonSubmit;
    private TextView textToggleModo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();
        authManager = AuthManager.getInstance();

        textTitulo = findViewById(R.id.text_auth_title);
        textAuthSubtitulo = findViewById(R.id.text_auth_subtitulo);
        layoutCamposRegistro = findViewById(R.id.layout_campos_registro);
        layoutCampoEdad = findViewById(R.id.layout_campo_edad);
        layoutTerminos = findViewById(R.id.layout_terminos);
        layoutInputPassword = findViewById(R.id.layout_input_password);
        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        editEdad = findViewById(R.id.edit_edad);
        checkTerminos = findViewById(R.id.check_terminos);
        textVerTerminos = findViewById(R.id.text_ver_terminos);
        buttonSubmit = findViewById(R.id.button_auth_submit);
        textToggleModo = findViewById(R.id.text_toggle_auth_mode);

        Usuario usuarioLocal = dataStore.getUsuario();
        esVinculacion = usuarioLocal != null
                && (usuarioLocal.getFirebaseUid() == null || usuarioLocal.getFirebaseUid().isEmpty());
        modo = esVinculacion ? Modo.VINCULAR : (usuarioLocal == null ? Modo.REGISTRO : Modo.LOGIN);

        textToggleModo.setOnClickListener(v -> {
            modo = (modo == Modo.REGISTRO) ? Modo.LOGIN : Modo.REGISTRO;
            actualizarModo();
        });
        textVerTerminos.setOnClickListener(v -> mostrarTerminos());
        buttonSubmit.setOnClickListener(v -> enviar());

        actualizarModo();
    }

    private void actualizarModo() {
        layoutCamposRegistro.setVisibility(modo == Modo.REGISTRO ? View.VISIBLE : View.GONE);
        layoutCampoEdad.setVisibility(modo == Modo.REGISTRO ? View.VISIBLE : View.GONE);
        layoutTerminos.setVisibility(modo == Modo.LOGIN ? View.GONE : View.VISIBLE);
        textToggleModo.setVisibility(esVinculacion ? View.GONE : View.VISIBLE);

        editEmail.setEnabled(modo != Modo.VINCULAR);
        if (modo == Modo.VINCULAR) {
            editEmail.setText(dataStore.getUsuario().getEmail());
            layoutInputPassword.setHint("Crea una contraseña para la nube");
        } else {
            layoutInputPassword.setHint("Contraseña");
        }

        if (modo == Modo.REGISTRO) {
            textTitulo.setText("Crea tu cuenta");
            textAuthSubtitulo.setText(R.string.app_name);
            buttonSubmit.setText("Registrarme");
            textToggleModo.setText("¿Ya tienes cuenta? Inicia sesión");
        } else if (modo == Modo.LOGIN) {
            textTitulo.setText("Inicia sesión");
            textAuthSubtitulo.setText(R.string.app_name);
            buttonSubmit.setText("Iniciar sesión");
            textToggleModo.setText("¿No tienes cuenta? Regístrate");
        } else {
            textTitulo.setText("Vincula tu cuenta");
            textAuthSubtitulo.setText("Ya tienes datos guardados en este teléfono. Crea una "
                    + "contraseña para respaldarlos en la nube y no perderlos.");
            buttonSubmit.setText("Vincular");
        }
    }

    private void mostrarTerminos() {
        new AlertDialog.Builder(this)
                .setTitle("Términos y condiciones")
                .setMessage(TERMINOS_TEXTO)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void enviar() {
        if (modo != Modo.LOGIN && !checkTerminos.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones para continuar", Toast.LENGTH_SHORT).show();
            return;
        }
        if (modo == Modo.REGISTRO) {
            registrarNuevo();
        } else if (modo == Modo.VINCULAR) {
            vincularExistente();
        } else {
            iniciarSesion();
        }
    }

    private void registrarNuevo() {
        String username = textoDe(editUsername);
        String email = textoDe(editEmail);
        String password = contrasenaDe(editPassword);
        String edadTexto = textoDe(editEdad);

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || edadTexto.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }
        int edad;
        try {
            edad = Integer.parseInt(edadTexto);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ingresa una edad válida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (edad < 10 || edad > 100) {
            Toast.makeText(this, "Ingresa una edad válida", Toast.LENGTH_SHORT).show();
            return;
        }

        int edadFinal = edad;
        buttonSubmit.setEnabled(false);
        authManager.registrar(email, password, new AuthManager.Callback() {
            @Override
            public void onExito(String uid) {
                Usuario usuario = new Usuario();
                usuario.setId(dataManager.newId("user"));
                usuario.setUsername(username);
                usuario.setEmail(email);
                usuario.setEdad(edadFinal);
                usuario.setFechaRegistro(LocalDate.now().toString());
                usuario.setFirebaseUid(uid);
                usuario.setAceptoTerminos(true);
                usuario.setFechaAceptacionTerminos(LocalDateTime.now().toString());

                dataStore.setUsuario(usuario);
                dataManager.save();
                PerfilSync.getInstance().pushCompleto(uid, usuario, dataStore.getHistorialFisico());

                continuarDespuesDeAuth();
            }

            @Override
            public void onError(String mensaje) {
                buttonSubmit.setEnabled(true);
                Toast.makeText(AuthActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void vincularExistente() {
        String password = contrasenaDe(editPassword);
        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario usuario = dataStore.getUsuario();
        buttonSubmit.setEnabled(false);
        authManager.registrar(usuario.getEmail(), password, new AuthManager.Callback() {
            @Override
            public void onExito(String uid) {
                usuario.setFirebaseUid(uid);
                usuario.setAceptoTerminos(true);
                usuario.setFechaAceptacionTerminos(LocalDateTime.now().toString());
                dataManager.save();
                PerfilSync.getInstance().pushCompleto(uid, usuario, dataStore.getHistorialFisico());

                continuarDespuesDeAuth();
            }

            @Override
            public void onError(String mensaje) {
                buttonSubmit.setEnabled(true);
                Toast.makeText(AuthActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void iniciarSesion() {
        String email = textoDe(editEmail);
        String password = contrasenaDe(editPassword);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSubmit.setEnabled(false);
        authManager.iniciarSesion(email, password, new AuthManager.Callback() {
            @Override
            public void onExito(String uid) {
                if (dataStore.getUsuario() != null) {
                    continuarDespuesDeAuth();
                    return;
                }
                PerfilSync.getInstance().pullUnaVez(uid, new PerfilSync.CallbackPull() {
                    @Override
                    public void onListo(Usuario usuarioDescargado, List<RegistroFisico> historial) {
                        dataStore.setUsuario(usuarioDescargado);
                        dataStore.getHistorialFisico().addAll(historial);
                        dataManager.save();
                        continuarDespuesDeAuth();
                    }

                    @Override
                    public void onError(String mensaje) {
                        Usuario usuarioMinimo = new Usuario();
                        usuarioMinimo.setId(dataManager.newId("user"));
                        usuarioMinimo.setEmail(email);
                        usuarioMinimo.setUsername(email);
                        usuarioMinimo.setFechaRegistro(LocalDate.now().toString());
                        usuarioMinimo.setFirebaseUid(uid);
                        usuarioMinimo.setAceptoTerminos(true);
                        usuarioMinimo.setFechaAceptacionTerminos(LocalDateTime.now().toString());
                        dataStore.setUsuario(usuarioMinimo);
                        dataManager.save();
                        continuarDespuesDeAuth();
                    }
                });
            }

            @Override
            public void onError(String mensaje) {
                buttonSubmit.setEnabled(true);
                Toast.makeText(AuthActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void continuarDespuesDeAuth() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SUGERIR_FISICO, dataStore.getHistorialFisico().isEmpty());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String textoDe(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }

    private String contrasenaDe(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString();
    }
}
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

---

### Tarea 7: Gate de `MainActivity` basado en Firebase

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/MainActivity.java:1-61`

**Interfaces:**
- Consume: `AuthManager.usuarioActual()` (Tarea 3), `Usuario.getFirebaseUid()` (Tarea 2).

- [ ] **Paso 1: Agregar imports**

En `app/src/main/java/com/ironquest/mvp/ui/MainActivity.java`, agregar junto a los imports
existentes (después de la línea 20, `import com.ironquest.mvp.model.Usuario;`):

```java
import com.google.firebase.auth.FirebaseUser;
import com.ironquest.mvp.data.AuthManager;
```

- [ ] **Paso 2: Reemplazar el gate de sesión**

Reemplazar (líneas 55-61):

```java
        dataManager = DataManager.getInstance(this);
        Usuario usuario = dataManager.getDataStore().getUsuario();
        if (usuario == null || !usuario.isSesionActiva()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
```

por:

```java
        dataManager = DataManager.getInstance(this);
        Usuario usuario = dataManager.getDataStore().getUsuario();
        FirebaseUser firebaseUser = AuthManager.getInstance().usuarioActual();
        boolean sesionValida = usuario != null && firebaseUser != null
                && firebaseUser.getUid().equals(usuario.getFirebaseUid());
        if (!sesionValida) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

---

### Tarea 8: Push desde `PhysicalProfileActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/PhysicalProfileActivity.java:1-24,133-157`

**Interfaces:**
- Consume: `PerfilSync.pushCompleto(...)` (Tarea 4).

- [ ] **Paso 1: Agregar imports**

Agregar junto a los imports existentes (después de la línea 17,
`import com.ironquest.mvp.util.PerfilFisicoUtil;`):

```java
import com.ironquest.mvp.data.PerfilSync;
import com.ironquest.mvp.model.Usuario;
```

- [ ] **Paso 2: Disparar el push tras guardar**

Reemplazar (líneas 152-153):

```java
        dataStore.getHistorialFisico().add(registro);
        dataManager.save();
```

por:

```java
        dataStore.getHistorialFisico().add(registro);
        dataManager.save();

        Usuario usuarioActivo = dataStore.getUsuario();
        if (usuarioActivo != null && usuarioActivo.getFirebaseUid() != null) {
            PerfilSync.getInstance().pushCompleto(
                    usuarioActivo.getFirebaseUid(), usuarioActivo, dataStore.getHistorialFisico());
        }
```

- [ ] **Paso 3: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

---

### Tarea 9: Build completo, verificación en el dispositivo real y commit

**Files:** ninguno (solo build, ADB y git).

- [ ] **Paso 1: Respaldar `datos.json` real**

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json > \
  "/tmp/claude-1000/-home-jdov-Documentos-titan-score/b8c695f0-e7b0-48b1-bfbd-c5b4163060d1/scratchpad/datos_respaldo_pre_firebase.json"
```
Expected: el archivo se copia sin quedar vacío (confirmar con `wc -l` que tiene contenido).
**No continuar si este paso falla o el archivo queda vacío.**

- [ ] **Paso 2: Compilar e instalar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: `BUILD SUCCESSFUL` y `Success` en la instalación.

- [ ] **Paso 3: Verificar el modo "Vincula tu cuenta"**

Abrir la app. Expected: pantalla con título "Vincula tu cuenta", correo pre-cargado y de
solo lectura, un solo campo de contraseña, y el checkbox de términos visible.

- [ ] **Paso 4: Completar la vinculación (con aprobación explícita antes de este paso,
  ya que toca la cuenta real)**

Marcar el checkbox, ingresar una contraseña nueva de al menos 6 caracteres, tocar
"Vincular". Expected: navega a `MainActivity` sin errores.

- [ ] **Paso 5: Confirmar en Firebase Console**

Verificar en `console.firebase.google.com/u/1/project/titan-score/authentication/users`
que aparece el usuario nuevo, y en Firestore (`.../firestore/.../data`) que existe
`usuarios/{uid}` con los campos del perfil y una subcolección `historialFisico` con la
misma cantidad de documentos que el `datos.json` real respaldado en el Paso 1.

- [ ] **Paso 6: Confirmar que `datos.json` no perdió nada**

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json
```
Expected: mismas rutinas/sesiones que el respaldo del Paso 1, más los 3 campos nuevos de
`Usuario` poblados.

- [ ] **Paso 7: Commit**

```bash
git add build.gradle app/build.gradle app/src/main/AndroidManifest.xml .gitignore \
  app/src/main/java/com/ironquest/mvp/model/Usuario.java \
  app/src/main/java/com/ironquest/mvp/data/AuthManager.java \
  app/src/main/java/com/ironquest/mvp/data/PerfilSync.java \
  app/src/main/res/layout/activity_auth.xml \
  app/src/main/java/com/ironquest/mvp/ui/AuthActivity.java \
  app/src/main/java/com/ironquest/mvp/ui/MainActivity.java \
  app/src/main/java/com/ironquest/mvp/ui/PhysicalProfileActivity.java \
  app/src/main/java/com/ironquest/mvp/util/PasswordUtil.java
git commit -m "Conectar Firebase: Authentication + perfil/historial en Firestore + términos y condiciones"
```

Nombrar explícitamente `PasswordUtil.java` en el `git add` aunque ya no exista en disco
(se borró en la Tarea 2) alcanza para que git registre la eliminación — no hace falta
`git rm` aparte. `app/google-services.json` queda fuera del commit porque el Paso 2 de la
Tarea 1 ya lo agregó a `.gitignore`.

---

## Rebanada 2 — Verificación de versión obligatoria/opcional

### Tarea 10: Crear el documento `config/version` en Firestore

**Files:** ninguno (acción de consola).

- [ ] **Paso 1: Crear el documento con los valores iniciales**

En Firestore Console → colección `config` → documento `version`, con estos campos y
tipos exactos:

```
minVersionCode       (number) = 5
latestVersionCode    (number) = 5
latestVersionName    (string) = "0.4.0"
notas                (string) = "Estás al día."
urlDescarga          (string) = "https://juanov27.github.io/titan-score-app/"
```

Estos valores no bloquean nada de lo ya instalado (versionCode actual = 5). Al publicar la
versión que incluya esta conexión con Firebase, subir `latestVersionCode` a 6 es un paso
manual aparte, de la sección de publicación de `CLAUDE.md`.

- [ ] **Paso 2: Confirmar lectura pública**

Sin sesión iniciada en la app (o directamente desde la consola, pestaña Reglas → "Zona de
pruebas de reglas"), confirmar que una lectura de `config/version` sin autenticación es
permitida por las reglas ya publicadas (`allow read: if true;`).

---

### Tarea 11: `data/VersionChecker.java`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/data/VersionChecker.java`

**Interfaces:**
- Produce: `VersionChecker.getInstance()`,
  `.verificar(int versionCodeInstalado, VersionChecker.Callback callback)`, clase
  `VersionChecker.Resultado { boolean bloqueoDuro; boolean avisoDisponible;
  String latestVersionName; String notas; String urlDescarga; }`, interfaz
  `VersionChecker.Callback { void onResultado(Resultado resultado); }` — consumidos por
  la Tarea 12.

- [ ] **Paso 1: Crear el archivo completo**

```java
package com.ironquest.mvp.data;

import com.google.firebase.firestore.FirebaseFirestore;

/** Único punto que toca Firestore para el documento público config/version. */
public class VersionChecker {

    /** Resultado de comparar la versión instalada contra config/version. */
    public static class Resultado {
        public final boolean bloqueoDuro;
        public final boolean avisoDisponible;
        public final String latestVersionName;
        public final String notas;
        public final String urlDescarga;

        Resultado(boolean bloqueoDuro, boolean avisoDisponible, String latestVersionName,
                  String notas, String urlDescarga) {
            this.bloqueoDuro = bloqueoDuro;
            this.avisoDisponible = avisoDisponible;
            this.latestVersionName = latestVersionName;
            this.notas = notas;
            this.urlDescarga = urlDescarga;
        }
    }

    public interface Callback {
        void onResultado(Resultado resultado);
    }

    private static VersionChecker instance;

    private final FirebaseFirestore firestore;

    private VersionChecker() {
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized VersionChecker getInstance() {
        if (instance == null) {
            instance = new VersionChecker();
        }
        return instance;
    }

    /** Sin red o sin documento: no llama al callback — no hay nada que avisar. */
    public void verificar(int versionCodeInstalado, Callback callback) {
        firestore.collection("config").document("version").get()
                .addOnSuccessListener(documento -> {
                    if (!documento.exists()) {
                        return;
                    }
                    Long minVersionCode = documento.getLong("minVersionCode");
                    Long latestVersionCode = documento.getLong("latestVersionCode");
                    if (minVersionCode == null || latestVersionCode == null) {
                        return;
                    }
                    boolean bloqueoDuro = versionCodeInstalado < minVersionCode;
                    boolean avisoDisponible = !bloqueoDuro && versionCodeInstalado < latestVersionCode;
                    if (!bloqueoDuro && !avisoDisponible) {
                        return;
                    }
                    callback.onResultado(new Resultado(
                            bloqueoDuro,
                            avisoDisponible,
                            documento.getString("latestVersionName"),
                            documento.getString("notas"),
                            documento.getString("urlDescarga")));
                });
    }
}
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

---

### Tarea 12: Enganchar el chequeo en `MainActivity` y `AuthActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/MainActivity.java`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/AuthActivity.java`

**Interfaces:**
- Consume: `VersionChecker.getInstance().verificar(...)` (Tarea 11),
  `BuildConfig.VERSION_CODE` (Tarea 1).

- [ ] **Paso 1: `MainActivity` — agregar imports**

Agregar junto a los imports existentes:

```java
import com.ironquest.mvp.BuildConfig;
import com.ironquest.mvp.data.VersionChecker;
```

- [ ] **Paso 2: `MainActivity` — disparar el chequeo al principio de `onCreate`**

Insertar como primera línea dentro de `onCreate`, antes de
`dataManager = DataManager.getInstance(this);`:

```java
        VersionChecker.getInstance().verificar(BuildConfig.VERSION_CODE, resultado -> {
            if (!isFinishing() && !isDestroyed()) {
                mostrarDialogoVersion(resultado);
            }
        });

```

- [ ] **Paso 3: `MainActivity` — agregar el método del diálogo**

Agregar como método privado nuevo, después de `onCreate`:

```java
    private void mostrarDialogoVersion(VersionChecker.Resultado resultado) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(resultado.bloqueoDuro ? "Actualización requerida" : "Nueva versión disponible")
                .setMessage((resultado.bloqueoDuro
                        ? "Necesitas actualizar Titan Score para seguir usándolo."
                        : "Hay una nueva versión de Titan Score disponible (" + resultado.latestVersionName + ").")
                        + (resultado.notas != null && !resultado.notas.isEmpty() ? "\n\n" + resultado.notas : ""))
                .setPositiveButton("Actualizar", (dialog, which) ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(resultado.urlDescarga))))
                .setCancelable(!resultado.bloqueoDuro);
        if (!resultado.bloqueoDuro) {
            builder.setNegativeButton("Ahora no", null);
        }
        builder.show();
    }
```

`AlertDialog`, `Intent` y `Uri` ya están importados en `MainActivity.java` (líneas 3, 4, 12).

- [ ] **Paso 4: `AuthActivity` — agregar imports**

```java
import android.net.Uri;

import com.ironquest.mvp.BuildConfig;
import com.ironquest.mvp.data.VersionChecker;
```

- [ ] **Paso 5: `AuthActivity` — disparar el chequeo al principio de `onCreate`**

Insertar como primera línea dentro de `onCreate`, antes de `setContentView(...)`:

```java
        VersionChecker.getInstance().verificar(BuildConfig.VERSION_CODE, resultado -> {
            if (!isFinishing() && !isDestroyed()) {
                mostrarDialogoVersion(resultado);
            }
        });

```

- [ ] **Paso 6: `AuthActivity` — agregar el mismo método del diálogo**

```java
    private void mostrarDialogoVersion(VersionChecker.Resultado resultado) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(resultado.bloqueoDuro ? "Actualización requerida" : "Nueva versión disponible")
                .setMessage((resultado.bloqueoDuro
                        ? "Necesitas actualizar Titan Score para seguir usándolo."
                        : "Hay una nueva versión de Titan Score disponible (" + resultado.latestVersionName + ").")
                        + (resultado.notas != null && !resultado.notas.isEmpty() ? "\n\n" + resultado.notas : ""))
                .setPositiveButton("Actualizar", (dialog, which) ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(resultado.urlDescarga))))
                .setCancelable(!resultado.bloqueoDuro);
        if (!resultado.bloqueoDuro) {
            builder.setNegativeButton("Ahora no", null);
        }
        builder.show();
    }
```

`AlertDialog` ya está importado en `AuthActivity.java` (Tarea 6). `Intent` también.

- [ ] **Paso 7: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

---

### Tarea 13: Build completo, verificación en el dispositivo y commit

**Files:** ninguno (solo build, ADB, consola y git).

- [ ] **Paso 1: Compilar e instalar**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: `BUILD SUCCESSFUL` y `Success`.

- [ ] **Paso 2: Probar el aviso "soft" (descartable)**

En la consola, cambiar `latestVersionCode` a `6` (dejando `minVersionCode` en `5`). Abrir
la app. Expected: diálogo "Nueva versión disponible" con botones "Actualizar"/"Ahora no",
descartable tocando afuera o con "Ahora no".

- [ ] **Paso 3: Probar el bloqueo duro**

Cambiar `minVersionCode` a `6`. Abrir la app. Expected: diálogo "Actualización requerida",
sin botón de cancelar, no se cierra tocando afuera ni con el botón atrás; "Actualizar" abre
`https://juanov27.github.io/titan-score-app/` en el navegador.

- [ ] **Paso 4: Restaurar los valores originales en la consola**

Volver `minVersionCode` y `latestVersionCode` a `5`, para no dejar la app bloqueada para
uso real después de la prueba.

- [ ] **Paso 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/data/VersionChecker.java \
  app/src/main/java/com/ironquest/mvp/ui/MainActivity.java \
  app/src/main/java/com/ironquest/mvp/ui/AuthActivity.java
git commit -m "Agregar verificación de versión obligatoria/opcional vía Firestore"
```

---

## Auto-revisión

**Cobertura del spec:** Configuración inicial → Tarea 1. Modelo de datos → Tareas 2, 4, 10.
Flujo de Auth → Tareas 3, 6, 7. Migración de cuenta local → Tarea 6 (`vincularExistente`) +
Tarea 9. Términos y condiciones → Tareas 5, 6. Verificación de versión → Tareas 10, 11, 12,
13. Los dos commits del spec se respetan exactamente (Tarea 9 y Tarea 13).

**Placeholders:** ninguno — cada paso de código tiene el archivo completo o el fragmento
exacto a reemplazar, sin "TODO" ni "manejar errores" genérico.

**Consistencia de tipos:** `AuthManager.Callback`, `PerfilSync.CallbackPull` y
`VersionChecker.Callback`/`Resultado` se usan con la misma forma en la Tarea donde se
definen y en las que los consumen (6, 7, 8, 12). `Usuario.getFirebaseUid()` se usa igual en
las Tareas 6, 7 y 8.
