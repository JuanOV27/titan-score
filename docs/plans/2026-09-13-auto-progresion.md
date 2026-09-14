# Auto-progresión aplicada a la rutina — plan de ejecución (Slice 1)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **En este proyecto** el ejecutor por defecto es **big pickle** (OpenCode). Cada tarea es autocontenida — big pickle no tiene contexto previo entre tareas. Todo lo necesario para ejecutar una tarea vive en su propia sección.

**Goal:** Implementar el motor híbrido `ProgresionAutomatica`, persistir sugerencias como entidad con identidad y mostrarlas en dos superficies (post-sesión y editor), permitiendo al usuario aceptarlas/ignorarlas por ejercicio o en bloque. Además: incremento de peso configurable global + por ejercicio, e info dialog educativo con 5 esquemas de progresión.

**Architecture:** Nueva subclase de `EstrategiaProgresion` (5ª estrategia). Nueva entidad `SugerenciaPendiente` en `DataStore` como lista raíz. Nueva `AjustesActivity`. Modificaciones a `ActiveSessionActivity` (genera sugerencias al finalizar), `SessionSummaryActivity` (card de sugerencias), `EditRoutineActivity` (banner), `RutinaEjercicioEditAdapter` (spinner de esquema + toggle silenciar) y `DetalleEjercicioDialog` (override de incremento). Cinco vector drawables nuevos para el info dialog. Todo aditivo, ningún cambio destructivo a `datos.json` viejo.

**Tech Stack:** Java 17, Android SDK 26–37, Material3, Gson (solo dentro de `data/`), sin dependencias nuevas.

## Global Constraints

- **Dominio en español, API Android en inglés** (`Rutina`, `Sesion`, `onCreate`, `findViewById`).
- **Nada fuera de `data/` importa Gson/File/FileWriter/FileReader ni llama a `getFilesDir()`.** El chequeo debe seguir vacío:
  ```bash
  grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
  ```
- **Modelos con campos privados + getters/setters + constructor sin args privado para Gson + constructor público completo.** Entidades con identidad heredan de `EntidadIdentificable`.
- **IDs con `dataManager.newId(prefijo)`** — para sugerencias, prefijo `"sp"`.
- **Fechas como `String` ISO** (`LocalDateTime.now().toString()`), nunca `Date`.
- **Textos hardcodeados en Java y layouts.** `strings.xml` solo tiene `app_name`.
- **No introducir dependencias nuevas** (ni Room, ni DI, ni MPAndroidChart, ni corrutinas).
- **`CATALOGO_VERSION_ACTUAL` no se toca** en este slice.
- **Nunca llamar `save()` desde `DataManager.load()`** (Trampa #4).
- **Campos primitivos nuevos son siempre seguros contra `datos.json` viejo** (Trampa #3). Colecciones nuevas también, pero deben normalizarse en `DataStore.normalizarColecciones()`.
- **Datos reales del usuario en el teléfono:** respaldar antes de instalar; nunca escribir sobre `files/datos.json` sin autorización explícita del usuario.
- **Activities con toolbar heredan de `BaseActivity`** y usan `configurarToolbar(...)`.
- **JAVA_HOME antes de gradlew:**
  ```bash
  export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
  ```
- **Commit messages** en español, imperativos, una línea.
- **Cuidado con pipes en gradlew** — `./gradlew ... | tail` devuelve el exit code de `tail`. Comprobar `${PIPESTATUS[0]}` o no usar pipe.

---

### Task 1: Campos nuevos en modelos existentes + entidad `SugerenciaPendiente` + normalización en `DataStore`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/model/RutinaEjercicio.java`
- Modify: `app/src/main/java/com/ironquest/mvp/model/Ejercicio.java`
- Modify: `app/src/main/java/com/ironquest/mvp/model/Usuario.java`
- Create: `app/src/main/java/com/ironquest/mvp/model/SugerenciaPendiente.java`
- Modify: `app/src/main/java/com/ironquest/mvp/model/DataStore.java`

**Interfaces:**
- Produces:
  - `RutinaEjercicio.ESQUEMA_AUTOMATICO = 4` (constante `public static final int`).
  - `RutinaEjercicio.isSilenciarSugerencia() / setSilenciarSugerencia(boolean)`.
  - `Ejercicio.getIncrementoPeso() / setIncrementoPeso(double)`.
  - `Usuario.getIncrementoPeso() / setIncrementoPeso(double)`.
  - `new SugerenciaPendiente(id, rutinaId, ejercicioId, sesionOrigenId, pesoActual, pesoSugerido, repeticionesActuales, repeticionesSugeridas, explicacion, tipo, fechaCreacion)` + getters/setters de cada campo. Constantes `TIPO_SUBIR_PESO=0`, `TIPO_MANTENER=1`, `TIPO_DELOAD=2`, `TIPO_SUBIR_REPS=3`.
  - `DataStore.getSugerenciasPendientes() / setSugerenciasPendientes(List<SugerenciaPendiente>)`.
  - `DataStore.normalizarColecciones()` cubre también `sugerenciasPendientes`.

- [x] **Step 1: Agregar constante y campo en `RutinaEjercicio.java`**

En [RutinaEjercicio.java](../../app/src/main/java/com/ironquest/mvp/model/RutinaEjercicio.java), justo debajo de la constante `ESQUEMA_DOBLE_PROGRESION = 3`:

```java
public static final int ESQUEMA_AUTOMATICO = 4;
```

Y como campo nuevo (junto a los demás):

```java
/** Si es {@code true}, el motor no genera SugerenciaPendiente para este ejercicio. */
private boolean silenciarSugerencia;
```

Getter/setter:

```java
public boolean isSilenciarSugerencia() {
    return silenciarSugerencia;
}

public void setSilenciarSugerencia(boolean silenciarSugerencia) {
    this.silenciarSugerencia = silenciarSugerencia;
}
```

- [x] **Step 2: Agregar campo en `Ejercicio.java`**

En [Ejercicio.java](../../app/src/main/java/com/ironquest/mvp/model/Ejercicio.java), campo nuevo:

```java
/** Override del incremento del usuario. {@code 0} = usar el del {@link Usuario}. */
private double incrementoPeso;
```

Getter/setter estándar.

- [x] **Step 3: Agregar campo en `Usuario.java`**

En [Usuario.java](../../app/src/main/java/com/ironquest/mvp/model/Usuario.java), campo nuevo:

```java
/** Incremento por defecto que aplica el motor de progresión. Valor default: 2.5 kg. */
private double incrementoPeso = 2.5;
```

Getter/setter estándar. **Ojo:** el inicializador de campo `= 2.5` corre al deserializar aunque el JSON viejo no traiga la clave (Trampa #3), así que usuarios existentes reciben `2.5` sin migración.

- [x] **Step 4: Crear `SugerenciaPendiente.java`**

Nuevo archivo `app/src/main/java/com/ironquest/mvp/model/SugerenciaPendiente.java`:

```java
package com.ironquest.mvp.model;

/**
 * Sugerencia de ajuste de peso/reps que el motor generó al finalizar una sesión. Vive en
 * {@link DataStore} hasta que el usuario la acepta (se aplica a {@link RutinaEjercicio}) o
 * la ignora (se elimina). Se persiste como entidad con identidad para poder aparecer en
 * dos superficies sin duplicar estado.
 */
public class SugerenciaPendiente extends EntidadIdentificable {

    public static final int TIPO_SUBIR_PESO = 0;
    public static final int TIPO_MANTENER = 1;
    public static final int TIPO_DELOAD = 2;
    public static final int TIPO_SUBIR_REPS = 3;

    private String rutinaId;
    private String ejercicioId;
    private String sesionOrigenId;
    private double pesoActual;
    private double pesoSugerido;
    private int repeticionesActuales;
    private int repeticionesSugeridas;
    private String explicacion;
    private int tipo;
    private String fechaCreacion;

    /** Constructor sin argumentos para Gson. */
    private SugerenciaPendiente() {
    }

    public SugerenciaPendiente(String id, String rutinaId, String ejercicioId,
                                String sesionOrigenId, double pesoActual, double pesoSugerido,
                                int repeticionesActuales, int repeticionesSugeridas,
                                String explicacion, int tipo, String fechaCreacion) {
        super(id);
        this.rutinaId = rutinaId;
        this.ejercicioId = ejercicioId;
        this.sesionOrigenId = sesionOrigenId;
        this.pesoActual = pesoActual;
        this.pesoSugerido = pesoSugerido;
        this.repeticionesActuales = repeticionesActuales;
        this.repeticionesSugeridas = repeticionesSugeridas;
        this.explicacion = explicacion;
        this.tipo = tipo;
        this.fechaCreacion = fechaCreacion;
    }

    public String getRutinaId() { return rutinaId; }
    public void setRutinaId(String rutinaId) { this.rutinaId = rutinaId; }

    public String getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(String ejercicioId) { this.ejercicioId = ejercicioId; }

    public String getSesionOrigenId() { return sesionOrigenId; }
    public void setSesionOrigenId(String sesionOrigenId) { this.sesionOrigenId = sesionOrigenId; }

    public double getPesoActual() { return pesoActual; }
    public void setPesoActual(double pesoActual) { this.pesoActual = pesoActual; }

    public double getPesoSugerido() { return pesoSugerido; }
    public void setPesoSugerido(double pesoSugerido) { this.pesoSugerido = pesoSugerido; }

    public int getRepeticionesActuales() { return repeticionesActuales; }
    public void setRepeticionesActuales(int repeticionesActuales) { this.repeticionesActuales = repeticionesActuales; }

    public int getRepeticionesSugeridas() { return repeticionesSugeridas; }
    public void setRepeticionesSugeridas(int repeticionesSugeridas) { this.repeticionesSugeridas = repeticionesSugeridas; }

    public String getExplicacion() { return explicacion; }
    public void setExplicacion(String explicacion) { this.explicacion = explicacion; }

    public int getTipo() { return tipo; }
    public void setTipo(int tipo) { this.tipo = tipo; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
```

- [x] **Step 5: Agregar lista raíz + normalización en `DataStore.java`**

En [DataStore.java](../../app/src/main/java/com/ironquest/mvp/model/DataStore.java), después del campo `historialFisico`:

```java
private List<SugerenciaPendiente> sugerenciasPendientes = new ArrayList<>();
```

En `normalizarColecciones()`, al final del método:

```java
if (sugerenciasPendientes == null) {
    sugerenciasPendientes = new ArrayList<>();
}
```

Getter/setter estándar:

```java
public List<SugerenciaPendiente> getSugerenciasPendientes() {
    return sugerenciasPendientes;
}

public void setSugerenciasPendientes(List<SugerenciaPendiente> sugerenciasPendientes) {
    this.sugerenciasPendientes = sugerenciasPendientes;
}
```

- [x] **Step 6: Compilar y verificar chequeo del CLAUDE.md**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
cd IronQuestApp
./gradlew assembleDebug
echo "Exit: ${PIPESTATUS[0]}"
```

Esperado: `BUILD SUCCESSFUL` y exit 0. Luego:

```bash
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Esperado: **vacío**.

- [x] **Step 7: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/model/
git commit -m "Modelar SugerenciaPendiente y campos base para auto-progresión"
```

---

### Task 2: Métodos de `SugerenciaPendiente` en `DataManager`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/data/DataManager.java`

**Interfaces:**
- Consumes: `SugerenciaPendiente` y `DataStore.getSugerenciasPendientes()` (Task 1).
- Produces:
  - `DataManager.getSugerenciasPendientes()` → `List<SugerenciaPendiente>`.
  - `DataManager.getSugerenciasPendientesDeRutina(String rutinaId)` → `List<SugerenciaPendiente>` (filtrada).
  - `DataManager.agregarSugerencia(SugerenciaPendiente s)`.
  - `DataManager.eliminarSugerencia(String sugerenciaId)`.
  - `DataManager.eliminarSugerenciasDeRutina(String rutinaId)`.
  - `DataManager.aplicarSugerencia(String sugerenciaId)` — muta `RutinaEjercicio.peso` y `.repeticiones`, elimina la sugerencia, `save()` una sola vez al final.

- [x] **Step 1: Agregar los métodos en `DataManager.java`**

En [DataManager.java](../../app/src/main/java/com/ironquest/mvp/data/DataManager.java), agrupados junto a los métodos existentes de mutación (después de los helpers de sesiones o donde encaje por afinidad, decisión estilística del ejecutor).

Imports necesarios (probablemente ya estén, agregar si faltan):

```java
import com.ironquest.mvp.model.SugerenciaPendiente;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
```

Métodos:

```java
public List<SugerenciaPendiente> getSugerenciasPendientes() {
    return dataStore.getSugerenciasPendientes();
}

public List<SugerenciaPendiente> getSugerenciasPendientesDeRutina(String rutinaId) {
    List<SugerenciaPendiente> resultado = new ArrayList<>();
    if (rutinaId == null) {
        return resultado;
    }
    for (SugerenciaPendiente sp : dataStore.getSugerenciasPendientes()) {
        if (rutinaId.equals(sp.getRutinaId())) {
            resultado.add(sp);
        }
    }
    return resultado;
}

public void agregarSugerencia(SugerenciaPendiente sugerencia) {
    dataStore.getSugerenciasPendientes().add(sugerencia);
    save();
}

public void eliminarSugerencia(String sugerenciaId) {
    if (sugerenciaId == null) {
        return;
    }
    Iterator<SugerenciaPendiente> it = dataStore.getSugerenciasPendientes().iterator();
    while (it.hasNext()) {
        if (sugerenciaId.equals(it.next().getId())) {
            it.remove();
            save();
            return;
        }
    }
}

public void eliminarSugerenciasDeRutina(String rutinaId) {
    if (rutinaId == null) {
        return;
    }
    Iterator<SugerenciaPendiente> it = dataStore.getSugerenciasPendientes().iterator();
    boolean cambio = false;
    while (it.hasNext()) {
        if (rutinaId.equals(it.next().getRutinaId())) {
            it.remove();
            cambio = true;
        }
    }
    if (cambio) {
        save();
    }
}

/**
 * Aplica la sugerencia al {@link RutinaEjercicio} correspondiente y la elimina de la lista.
 * Un solo {@code save()} al final. Si la rutina o el ejercicio ya no existen, la sugerencia
 * se elimina igual (huérfana).
 */
public void aplicarSugerencia(String sugerenciaId) {
    if (sugerenciaId == null) {
        return;
    }
    SugerenciaPendiente objetivo = null;
    for (SugerenciaPendiente sp : dataStore.getSugerenciasPendientes()) {
        if (sugerenciaId.equals(sp.getId())) {
            objetivo = sp;
            break;
        }
    }
    if (objetivo == null) {
        return;
    }
    Rutina rutina = dataStore.buscarRutina(objetivo.getRutinaId());
    if (rutina != null) {
        for (RutinaEjercicio re : rutina.getEjercicios()) {
            if (objetivo.getEjercicioId().equals(re.getEjercicioId())) {
                re.setPeso(objetivo.getPesoSugerido());
                re.setRepeticiones(objetivo.getRepeticionesSugeridas());
                break;
            }
        }
    }
    dataStore.getSugerenciasPendientes().remove(objetivo);
    save();
}
```

- [x] **Step 2: Compilar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [x] **Step 3: Verificar aislamiento de Gson**

```bash
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Esperado: **vacío**.

- [x] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/data/DataManager.java
git commit -m "Añadir métodos de SugerenciaPendiente en DataManager"
```

---

### Task 3: Refactor de firma en `EstrategiaProgresion` + registrar `ESQUEMA_AUTOMATICO` + crear `ProgresionAutomatica`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/util/progresion/EstrategiaProgresion.java`
- Modify: `app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionLineal.java`
- Modify: `app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionGreyskull.java`
- Modify: `app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionDoble.java`
- Modify: `app/src/main/java/com/ironquest/mvp/util/progresion/SinProgresion.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionAutomatica.java`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java` (solo el caller de `.sugerir(...)`)

**Interfaces:**
- Consumes: `Ejercicio.getIncrementoPeso()`, `Usuario.getIncrementoPeso()` (Task 1), `RutinaEjercicio.ESQUEMA_AUTOMATICO` (Task 1).
- Produces:
  - Nueva firma pública: `EstrategiaProgresion.sugerir(RutinaEjercicio config, Ejercicio ejercicio, Usuario usuario, List<Sesion> historial)`.
  - Nuevo método protegido `EstrategiaProgresion.getIncrementoEfectivo(Ejercicio, Usuario)` → `double`.
  - `EstrategiaProgresion.para(int)` acepta `ESQUEMA_AUTOMATICO`.
  - Todas las subclases actualizan la firma de `calcular(...)` para recibir el incremento efectivo.

**Contexto para el ejecutor:** El motor actual está en [EstrategiaProgresion.java](../../app/src/main/java/com/ironquest/mvp/util/progresion/EstrategiaProgresion.java). Hoy declara una constante `INCREMENTO_KG = 2.5` que las 3 subclases con lógica usan directamente. Este task cambia esa constante por un valor leído dinámicamente de `Ejercicio.incrementoPeso` (si > 0) o `Usuario.incrementoPeso` (fallback). Y agrega el registro de la nueva estrategia.

- [x] **Step 1: Cambiar la constante y agregar helper en `EstrategiaProgresion.java`**

Reemplazar:
```java
static final double INCREMENTO_KG = 2.5;
```

Por (borrar la vieja):
```java
/** Fallback si el usuario no está seteado (edge case en tests o boot). */
private static final double INCREMENTO_KG_DEFAULT = 2.5;
```

Agregar el helper protegido:
```java
/**
 * Incremento de peso a usar para este ejercicio: override del ejercicio si > 0, si no el
 * global del usuario. Fallback 2.5 kg si no hay usuario (no debería pasar en runtime).
 */
protected static double getIncrementoEfectivo(Ejercicio ejercicio, Usuario usuario) {
    if (ejercicio != null && ejercicio.getIncrementoPeso() > 0) {
        return ejercicio.getIncrementoPeso();
    }
    if (usuario != null && usuario.getIncrementoPeso() > 0) {
        return usuario.getIncrementoPeso();
    }
    return INCREMENTO_KG_DEFAULT;
}
```

- [x] **Step 2: Cambiar la firma de `sugerir(...)` en `EstrategiaProgresion.java`**

La firma actual:
```java
public final Sugerencia sugerir(RutinaEjercicio config, List<Sesion> historial) {
```

Pasa a:
```java
public final Sugerencia sugerir(RutinaEjercicio config, Ejercicio ejercicio, Usuario usuario, List<Sesion> historial) {
```

Y dentro del método, calcular el incremento una vez:
```java
double incremento = getIncrementoEfectivo(ejercicio, usuario);
```

Reemplazar los usos de `INCREMENTO_KG` por `incremento`. En particular, la línea del deload:
```java
double pesoDeload = redondearA(pesoBase * 0.9, incremento);
```

Y pasar `incremento` a las llamadas de `calcular(...)`:
```java
return calcular(config, null, config.getPeso(), incremento);
```
```java
return calcular(config, ultima, pesoBase, incremento);
```

- [x] **Step 3: Cambiar la firma abstracta de `calcular(...)` en `EstrategiaProgresion.java`**

De:
```java
protected abstract Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase);
```

A:
```java
protected abstract Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento);
```

Los imports de `Ejercicio` y `Usuario` deben agregarse arriba:
```java
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Usuario;
```

- [x] **Step 4: Registrar `ESQUEMA_AUTOMATICO` en `EstrategiaProgresion.para(int)`**

Agregar el case:
```java
case RutinaEjercicio.ESQUEMA_AUTOMATICO:
    return new ProgresionAutomatica();
```

Antes del `case RutinaEjercicio.ESQUEMA_NINGUNO`.

- [x] **Step 5: Actualizar `ProgresionLineal.java`**

Nueva firma de `calcular`:
```java
@Override
protected Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento) {
    boolean exito = fueExitosa(config, ultima);
    if (exito) {
        double nuevoPeso = pesoBase + incremento;
        return new Sugerencia(nuevoPeso, config.getRepeticiones(),
                "Cumpliste el objetivo de " + config.getRepeticiones() + " reps en todas las series con "
                        + formatearPeso(pesoBase) + "kg. Sugerencia: sube a " + formatearPeso(nuevoPeso) + "kg.",
                false);
    }
    return new Sugerencia(pesoBase, config.getRepeticiones(),
            "No completaste el objetivo de " + config.getRepeticiones() + " reps la última vez. Repite "
                    + formatearPeso(pesoBase) + "kg.",
            false);
}
```

- [x] **Step 6: Actualizar `ProgresionGreyskull.java`**

Nueva firma + reemplazo del `INCREMENTO_KG`:
```java
@Override
protected Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento) {
    SerieSesion amrap = ultima.getUltimaSerieCompletada();
    if (amrap == null) {
        return new Sugerencia(config.getPeso(), config.getRepeticiones(),
                "Repite " + formatearPeso(config.getPeso()) + "kg.", false);
    }
    if (amrap.getRepeticiones() >= config.getRepeticiones()) {
        double nuevoPeso = amrap.getPeso() + incremento;
        return new Sugerencia(nuevoPeso, config.getRepeticiones(),
                "Tu última serie (AMRAP) fue de " + amrap.getRepeticiones() + " reps con "
                        + formatearPeso(amrap.getPeso()) + "kg, cumpliendo el objetivo de "
                        + config.getRepeticiones() + ". Sugerencia: sube a " + formatearPeso(nuevoPeso) + "kg.",
                false);
    }
    return new Sugerencia(amrap.getPeso(), config.getRepeticiones(),
            "Tu última serie (AMRAP) fue de " + amrap.getRepeticiones() + " reps, por debajo del objetivo de "
                    + config.getRepeticiones() + ". Repite " + formatearPeso(amrap.getPeso()) + "kg.",
            false);
}
```

- [x] **Step 7: Actualizar `ProgresionDoble.java`**

Nueva firma + reemplazo:
```java
@Override
protected Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento) {
    int techo = config.getTechoRepeticiones();
    int repsLogradas = repeticionesMinimasCompletadas(ultima);

    if (repsLogradas < config.getRepeticiones()) {
        return new Sugerencia(pesoBase, config.getRepeticiones(),
                "No llegaste a " + config.getRepeticiones() + " reps en todas las series. Repite "
                        + formatearPeso(pesoBase) + "kg.",
                false);
    }
    if (repsLogradas >= techo) {
        double nuevoPeso = pesoBase + incremento;
        return new Sugerencia(nuevoPeso, config.getRepeticiones(),
                "Llegaste a " + techo + " reps en todas las series con " + formatearPeso(pesoBase)
                        + "kg — tope del rango. Sugerencia: sube a " + formatearPeso(nuevoPeso) + "kg y vuelve a "
                        + config.getRepeticiones() + " reps.",
                false);
    }
    int siguienteReps = repsLogradas + 1;
    return new Sugerencia(pesoBase, siguienteReps,
            "Vas en " + repsLogradas + " de " + config.getRepeticiones() + "-" + techo + " reps con "
                    + formatearPeso(pesoBase) + "kg. Sugerencia: intenta " + siguienteReps + " reps.",
            false);
}
```

- [x] **Step 8: Actualizar `SinProgresion.java`**

Nueva firma (el método no usa incremento pero debe respetar el contrato):
```java
@Override
protected Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento) {
    // ...cuerpo actual sin cambios sustanciales...
}
```

Si `SinProgresion` no tiene lógica en `calcular` (devuelve la config tal cual), simplemente ajustar la firma.

- [x] **Step 9: Crear `ProgresionAutomatica.java`**

Nuevo archivo:

```java
package com.ironquest.mvp.util.progresion;

import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Regla híbrida sobre las últimas 3 sesiones del ejercicio:
 * <ul>
 *   <li>MARGEN: todas las series completadas alcanzaron {@code objetivo + 2} o más.</li>
 *   <li>JUSTO: todas cumplieron {@code objetivo <= reps < objetivo + 2}.</li>
 *   <li>FALLIDO: alguna serie completada quedó por debajo del objetivo, o las dos últimas
 *       series consecutivas quedaron no completadas.</li>
 * </ul>
 * Combinaciones:
 * <ul>
 *   <li>3× MARGEN o 2× MARGEN + 1× JUSTO → +incremento kg, mantener reps.</li>
 *   <li>Todas cumplen y la última es JUSTO → mantener peso, {@code repeticiones + 1}.</li>
 *   <li>Exactamente 1 FALLIDO → mantener peso y reps.</li>
 *   <li>2 o 3 FALLIDOS → deload 90% redondeado al incremento.</li>
 * </ul>
 * Con menos de 3 sesiones, aplica la regla sobre las que haya (mínimo 1).
 * <p>Peso corporal ({@code peso == 0}): "+incremento kg" pasa a "+1 rep",
 * deload pasa a "-1 rep" con piso 3.
 */
final class ProgresionAutomatica extends EstrategiaProgresion {

    static final int ESTADO_MARGEN = 0;
    static final int ESTADO_JUSTO = 1;
    static final int ESTADO_FALLIDO = 2;

    @Override
    protected boolean requiereHistorial() {
        return true;
    }

    @Override
    protected Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento) {
        // ultima es solo la más reciente. Necesitamos las últimas 3 — se obtienen re-filtrando en fueExitosa,
        // pero acá no tenemos el historial. Solución: la lógica real vive en sugerirAutomatica(), llamado por
        // sugerir(). Aquí caemos en fallback simple (sin historial de 3): se comporta como si la sesión única
        // fuera todo el historial.
        int estadoUnico = clasificarSesion(config, ultima);
        return decidir(config, pesoBase, incremento, new int[]{estadoUnico});
    }

    @Override
    protected boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada) {
        return clasificarSesion(config, sesionPasada) != ESTADO_FALLIDO;
    }

    /**
     * Sobreescribe el flujo estándar de sugerir(): necesitamos ver las últimas 3 sesiones, no solo la última.
     * La clase base filtra historial por ejercicio; replicamos ese filtrado aquí para trabajar con la ventana.
     */
    @Override
    public final Sugerencia sugerir(RutinaEjercicio config, com.ironquest.mvp.model.Ejercicio ejercicio,
                                     com.ironquest.mvp.model.Usuario usuario, List<Sesion> historial) {
        double incremento = getIncrementoEfectivo(ejercicio, usuario);

        List<EjercicioSesion> pasadas = historialDelEjercicio(config.getEjercicioId(), historial);
        if (pasadas.isEmpty()) {
            return new Sugerencia(config.getPeso(), config.getRepeticiones(),
                    "Primera vez con este esquema: arrancas con el plan de la rutina.", false);
        }

        // Tomar las últimas 3 (o menos si hay menos)
        int desde = Math.max(0, pasadas.size() - 3);
        List<EjercicioSesion> ventana = pasadas.subList(desde, pasadas.size());

        int[] estados = new int[ventana.size()];
        for (int i = 0; i < ventana.size(); i++) {
            estados[i] = clasificarSesion(config, ventana.get(i));
        }

        double pesoBase = pesoDeLaSesion(ventana.get(ventana.size() - 1), config.getPeso());
        return decidir(config, pesoBase, incremento, estados);
    }

    /**
     * Copia local del filtrado por ejercicio (privado en la clase base). Ordena por fecha ISO.
     */
    private static List<EjercicioSesion> historialDelEjercicio(String ejercicioId, List<Sesion> sesiones) {
        List<Sesion> finalizadas = new ArrayList<>();
        for (Sesion s : sesiones) {
            if (s.estaFinalizada()) {
                finalizadas.add(s);
            }
        }
        finalizadas.sort(Comparator.comparing(s -> LocalDateTime.parse(s.getFechaHoraInicio())));

        List<EjercicioSesion> resultado = new ArrayList<>();
        for (Sesion s : finalizadas) {
            EjercicioSesion es = s.buscarEjercicio(ejercicioId);
            if (es != null) {
                resultado.add(es);
            }
        }
        return resultado;
    }

    private int clasificarSesion(RutinaEjercicio config, EjercicioSesion pasada) {
        int objetivo = config.getRepeticiones();
        int minCompleto = Integer.MAX_VALUE;
        int maxCompleto = -1;
        int completadas = 0;
        int noCompletadasFinales = 0;

        List<SerieSesion> series = pasada.getSeries();
        for (int i = 0; i < series.size(); i++) {
            SerieSesion s = series.get(i);
            if (s.isCompletada()) {
                completadas++;
                minCompleto = Math.min(minCompleto, s.getRepeticiones());
                maxCompleto = Math.max(maxCompleto, s.getRepeticiones());
                noCompletadasFinales = 0;
            } else {
                noCompletadasFinales++;
            }
        }

        // Dos o más no completadas seguidas al final = abandono → FALLIDO
        if (noCompletadasFinales >= 2) {
            return ESTADO_FALLIDO;
        }
        if (completadas == 0) {
            return ESTADO_FALLIDO;
        }
        if (minCompleto < objetivo) {
            return ESTADO_FALLIDO;
        }
        if (minCompleto >= objetivo + 2) {
            return ESTADO_MARGEN;
        }
        return ESTADO_JUSTO;
    }

    private Sugerencia decidir(RutinaEjercicio config, double pesoBase, double incremento, int[] estados) {
        int margen = 0, justo = 0, fallido = 0;
        for (int e : estados) {
            if (e == ESTADO_MARGEN) margen++;
            else if (e == ESTADO_JUSTO) justo++;
            else fallido++;
        }
        boolean pesoCorporal = pesoBase == 0;

        if (fallido >= 2) {
            if (pesoCorporal) {
                int reps = Math.max(3, config.getRepeticiones() - 1);
                return new Sugerencia(0, reps,
                        "Llevas " + fallido + " sesiones sin cumplir. Baja a " + reps + " reps y retoma.",
                        true);
            }
            double pesoDeload = redondearA(pesoBase * 0.9, incremento);
            return new Sugerencia(pesoDeload, config.getRepeticiones(),
                    "Llevas " + fallido + " sesiones sin cumplir. Baja a " + formatearPeso(pesoDeload)
                            + "kg y retoma la progresión.",
                    true);
        }
        if (fallido == 1) {
            return new Sugerencia(pesoBase, config.getRepeticiones(),
                    "Fallaste una sesión reciente. Repite " + formatearPeso(pesoBase) + "kg × "
                            + config.getRepeticiones() + ".",
                    false);
        }
        // fallido == 0, todas cumplen
        if (margen >= 2) {
            if (pesoCorporal) {
                int reps = config.getRepeticiones() + 1;
                return new Sugerencia(0, reps,
                        "Cumpliste con margen. Intenta " + reps + " reps.", false);
            }
            double nuevoPeso = pesoBase + incremento;
            return new Sugerencia(nuevoPeso, config.getRepeticiones(),
                    "Cumpliste con margen. Sube a " + formatearPeso(nuevoPeso) + "kg.", false);
        }
        // Todas cumplen y la última es JUSTO (no MARGEN) → sube reps
        int nuevasReps = config.getRepeticiones() + 1;
        return new Sugerencia(pesoBase, nuevasReps,
                "Cumpliste justo. Intenta llegar a " + nuevasReps + " reps con el mismo peso.",
                false);
    }
}
```

- [x] **Step 10: Actualizar el caller en `ActiveSessionActivity.java`**

En [ActiveSessionActivity.java:123](../../app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java:123), la línea actual:

```java
Sugerencia sugerencia = EstrategiaProgresion.para(re.getEsquemaProgresion())
        .sugerir(re, dataStore.getSesiones());
```

Pasa a:

```java
Ejercicio ejercicio = catalogoPorId.get(re.getEjercicioId());
Sugerencia sugerencia = EstrategiaProgresion.para(re.getEsquemaProgresion())
        .sugerir(re, ejercicio, dataStore.getUsuario(), dataStore.getSesiones());
```

Verificar que `Ejercicio` esté importado (probablemente ya lo esté; si no, agregarlo).

- [x] **Step 11: Compilar y verificar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`. Si algún caller de `.sugerir()` sigue con la firma vieja, el compilador lo atrapa aquí — buscar y corregir.

```bash
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Esperado: **vacío**.

- [x] **Step 12: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/util/progresion/ app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java
git commit -m "Añadir ProgresionAutomatica y refactorizar el motor para leer el incremento del usuario"
```

---

### Task 4: Puente — `ActiveSessionActivity` genera `SugerenciaPendiente` al finalizar sesión

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java` (método `finalizarSesion()`)

**Interfaces:**
- Consumes: `DataManager.agregarSugerencia(...)` (Task 2), `EstrategiaProgresion.para(...).sugerir(...)` con firma nueva (Task 3), `SugerenciaPendiente.TIPO_*` (Task 1).
- Produces: efecto lateral — al finalizar sesión, `dataStore.getSugerenciasPendientes()` incluye una por cada ejercicio de la rutina que tenga esquema ≠ Ninguno, no silenciado, y cuya sugerencia difiera del `RutinaEjercicio` actual.

- [x] **Step 1: Editar `finalizarSesion()` en `ActiveSessionActivity.java`**

Ubicación: [ActiveSessionActivity.java:356](../../app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java:356).

Estructura actual (para referencia):
```java
private void finalizarSesion() {
    sesionFinalizada = true;
    LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    sesionActual.finalizar(ahora.toString());
    // ...
    dataStore.getSesiones().add(sesionActual);
    dataStore.setSesionEnProgreso(null);
    dataManager.save();
    // ...
}
```

**Después de** `dataManager.save()` y **antes de** `SesionTrackingService.detener(this)`, insertar la generación de sugerencias. El bloque completo a añadir:

```java
generarSugerenciasPendientes();
```

Y agregar el método privado nuevo al final de la clase:

```java
/**
 * Genera SugerenciaPendiente para cada ejercicio de la rutina origen que tenga esquema
 * distinto de Ninguno, no silenciado, y cuya sugerencia efectivamente cambie el plan actual.
 * Se llama después de que la sesión quede persistida — así el propio historial nuevo ya
 * está disponible para el motor.
 */
private void generarSugerenciasPendientes() {
    Rutina rutina = dataStore.buscarRutina(sesionActual.getRutinaId());
    if (rutina == null) {
        return;
    }
    Usuario usuario = dataStore.getUsuario();
    // catalogoPorId ya existe en la Activity (Map<String, Ejercicio>) — reusarlo.
    for (RutinaEjercicio re : rutina.getEjercicios()) {
        if (re.getEsquemaProgresion() == RutinaEjercicio.ESQUEMA_NINGUNO) {
            continue;
        }
        if (re.isSilenciarSugerencia()) {
            continue;
        }
        Ejercicio ejercicio = catalogoPorId.get(re.getEjercicioId());
        if (ejercicio == null) {
            continue;
        }
        Sugerencia s = EstrategiaProgresion.para(re.getEsquemaProgresion())
                .sugerir(re, ejercicio, usuario, dataStore.getSesiones());
        boolean pesoDiferente = s.getPeso() != re.getPeso();
        boolean repsDiferente = s.getRepeticiones() != re.getRepeticiones();
        if (!pesoDiferente && !repsDiferente) {
            continue;
        }
        int tipo = clasificarTipoSugerencia(re, s);
        SugerenciaPendiente sp = new SugerenciaPendiente(
                dataManager.newId("sp"),
                rutina.getId(),
                re.getEjercicioId(),
                sesionActual.getId(),
                re.getPeso(),
                s.getPeso(),
                re.getRepeticiones(),
                s.getRepeticiones(),
                s.getExplicacion(),
                tipo,
                LocalDateTime.now().toString());
        dataManager.agregarSugerencia(sp);
    }
}

private int clasificarTipoSugerencia(RutinaEjercicio re, Sugerencia s) {
    if (s.getPeso() > re.getPeso()) {
        return SugerenciaPendiente.TIPO_SUBIR_PESO;
    }
    if (s.getPeso() < re.getPeso()) {
        return SugerenciaPendiente.TIPO_DELOAD;
    }
    if (s.getRepeticiones() > re.getRepeticiones()) {
        return SugerenciaPendiente.TIPO_SUBIR_REPS;
    }
    return SugerenciaPendiente.TIPO_MANTENER;
}
```

- [x] **Step 2: Asegurar imports**

En la parte superior del archivo, verificar (o agregar):
```java
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Usuario;
import com.ironquest.mvp.model.SugerenciaPendiente;
import com.ironquest.mvp.util.progresion.EstrategiaProgresion;
import com.ironquest.mvp.util.progresion.Sugerencia;
import java.time.LocalDateTime;
```

- [x] **Step 3: Pasar el sesionId al SessionSummary via Intent**

Después del bloque de `intent.putExtra(...)` existente, antes de `startActivity(intent)`, agregar:

```java
intent.putExtra(SessionSummaryActivity.EXTRA_SESION_ID, sesionActual.getId());
intent.putExtra(SessionSummaryActivity.EXTRA_RUTINA_ID, sesionActual.getRutinaId());
```

En [SessionSummaryActivity.java](../../app/src/main/java/com/ironquest/mvp/ui/SessionSummaryActivity.java) declarar las nuevas constantes (junto a las que ya hay):

```java
public static final String EXTRA_SESION_ID = "extra_sesion_id";
public static final String EXTRA_RUTINA_ID = "extra_rutina_id";
```

(La lógica que las consume se agrega en Task 10.)

- [x] **Step 4: Compilar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java app/src/main/java/com/ironquest/mvp/ui/SessionSummaryActivity.java
git commit -m "Generar SugerenciaPendiente al finalizar sesión"
```

---

### Task 5: Cinco vector drawables para el info dialog de esquemas

**Files:**
- Create: `app/src/main/res/drawable/esquema_ninguno.xml`
- Create: `app/src/main/res/drawable/esquema_automatico.xml`
- Create: `app/src/main/res/drawable/esquema_lineal.xml`
- Create: `app/src/main/res/drawable/esquema_greyskull.xml`
- Create: `app/src/main/res/drawable/esquema_doble.xml`

**Interfaces:**
- Produces: cinco recursos `@drawable/esquema_*` de 80dp × 40dp cada uno, dibujando el concepto del esquema con `<path>` sobre el color `?attr/colorPrimary`.

**Contexto para el ejecutor:** Son iconos pequeños explicativos que van en las tarjetas del `InfoProgresionDialog` (Task 6). Deben leerse claramente a esa escala. Trazo 2dp. Colores del tema (`?attr/colorPrimary`).

- [x] **Step 1: Crear `esquema_ninguno.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="80dp"
    android:height="40dp"
    android:viewportWidth="80"
    android:viewportHeight="40">
    <path
        android:strokeWidth="2"
        android:strokeColor="?attr/colorPrimary"
        android:pathData="M8,20 L72,20" />
</vector>
```

Línea horizontal — "no cambia nada".

- [x] **Step 2: Crear `esquema_lineal.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="80dp"
    android:height="40dp"
    android:viewportWidth="80"
    android:viewportHeight="40">
    <path
        android:strokeWidth="2"
        android:strokeColor="?attr/colorPrimary"
        android:pathData="M8,32 L20,32 L20,24 L36,24 L36,16 L52,16 L52,8 L72,8" />
</vector>
```

Escalera ascendente pareja.

- [x] **Step 3: Crear `esquema_greyskull.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="80dp"
    android:height="40dp"
    android:viewportWidth="80"
    android:viewportHeight="40">
    <path
        android:strokeWidth="2"
        android:strokeColor="?attr/colorPrimary"
        android:pathData="M8,32 L20,32 L20,24 L36,24 L36,28 L48,28 L48,16 L60,16 L60,8 L72,8" />
</vector>
```

Escalera ascendente con un pequeño peldaño de bajada (deload).

- [x] **Step 4: Crear `esquema_doble.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="80dp"
    android:height="40dp"
    android:viewportWidth="80"
    android:viewportHeight="40">
    <path
        android:strokeWidth="2"
        android:strokeColor="?attr/colorPrimary"
        android:pathData="M8,28 L32,28 L32,16 L56,16 L56,8 L72,8" />
</vector>
```

Escalones planos largos (peso constante, reps subiendo) seguidos de un salto vertical (peso arriba, reps al piso).

- [x] **Step 5: Crear `esquema_automatico.xml`**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="80dp"
    android:height="40dp"
    android:viewportWidth="80"
    android:viewportHeight="40">
    <path
        android:strokeWidth="2"
        android:strokeColor="?attr/colorPrimary"
        android:pathData="M8,32 L20,32 L20,24 L36,24 L36,16 L52,16 L52,8 L72,8" />
    <path
        android:fillColor="?attr/colorPrimary"
        android:pathData="M60,4 L62,8 L66,8 L63,11 L64,15 L60,13 L56,15 L57,11 L54,8 L58,8 z" />
</vector>
```

Escalera igual que la lineal, con una estrellita arriba a la derecha (indicativo de "decide solo").

- [x] **Step 6: Compilar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [x] **Step 7: Commit**

```bash
git add app/src/main/res/drawable/esquema_*.xml
git commit -m "Añadir vector drawables para los cinco esquemas de progresión"
```

---

### Task 6: `InfoProgresionDialog` con tarjetas educativas

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/ui/InfoProgresionDialog.java`
- Create: `app/src/main/res/layout/dialog_info_progresion.xml`
- Create: `app/src/main/res/layout/item_esquema_info.xml`

**Interfaces:**
- Consumes: drawables `esquema_*` (Task 5).
- Produces: `InfoProgresionDialog.mostrar(Activity activity)` — método estático que crea y muestra el diálogo.

**Contexto para el ejecutor:** Diálogo scrolleable con 5 tarjetas (una por esquema) + 2 secciones colapsables al final ("Deload" y "Silenciar sugerencias"). Cada tarjeta muestra icono + título + descripción + ejemplo + "Recomendado para". Cierre con botón "Entendido".

- [x] **Step 1: Crear `dialog_info_progresion.xml`**

Layout raíz `ScrollView` conteniendo un `LinearLayout` vertical con:
- Encabezado (`TextView` "¿Cómo funcionan las progresiones?").
- Contenedor `LinearLayout` con id `container_esquemas` donde el código Java infla 5 items.
- Dos `MaterialCardView` colapsables para "Deload" y "Silenciar sugerencias" — implementados como `LinearLayout` con `TextView` de título (clickable) que muestra/oculta un `TextView` con el cuerpo.
- Botón "Entendido" al final que cierra el diálogo.

Padding interno 16dp. `MaterialCardView` para las tarjetas de deload/silenciar. IDs:
- `container_esquemas` (LinearLayout)
- `header_deload`, `body_deload` (para colapsar)
- `header_silenciar`, `body_silenciar`
- `button_entendido`

- [x] **Step 2: Crear `item_esquema_info.xml`**

`MaterialCardView` con `LinearLayout` horizontal interno:
- `ImageView` id `image_esquema` (80dp × 40dp) a la izquierda.
- `LinearLayout` vertical a la derecha con:
  - `TextView` id `text_titulo` (títulos como "Progresión Lineal", 18sp bold).
  - `TextView` id `text_descripcion` (3-4 líneas).
  - `TextView` id `text_ejemplo` (monospace, 3 líneas de ejemplo).
  - `TextView` id `text_recomendado` (italic, "Recomendado para: ...").

Margen inferior 12dp entre tarjetas.

- [x] **Step 3: Crear `InfoProgresionDialog.java`**

```java
package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ironquest.mvp.R;

/**
 * Diálogo educativo con las 5 tarjetas de los esquemas de progresión + dos secciones
 * colapsables (deload, silenciar). Solo lectura, no muta datos.
 */
public final class InfoProgresionDialog extends Dialog {

    private static final int[] DRAWABLES = {
            R.drawable.esquema_ninguno,
            R.drawable.esquema_automatico,
            R.drawable.esquema_lineal,
            R.drawable.esquema_greyskull,
            R.drawable.esquema_doble
    };

    private static final String[] TITULOS = {
            "Ninguno",
            "Automático",
            "Progresión Lineal",
            "Greyskull",
            "Doble Progresión"
    };

    private static final String[] DESCRIPCIONES = {
            "La rutina no cambia sola. Tú decides cuándo subir peso o reps. Recomendado si sigues un programa externo.",
            "La app mira tus últimas 3 sesiones: si vas cumpliendo con soltura, sube peso; si vas justo, sube reps; si fallas, mantiene o baja. Es la opción por defecto y la más recomendada para la mayoría.",
            "Cada vez que cumples el objetivo en todas las series, sube el peso. Fuerte y directo. Para fuerza pura.",
            "Solo mira tu última serie. Si sacaste más reps del objetivo, sube el peso. Diseñado para ejercicios compuestos con AMRAP (as many reps as possible).",
            "Primero subes reps dentro del rango que fijaste (p. ej. 8–12). Al llegar al tope con todas las series, subes el peso y vuelves al piso. Ideal para hipertrofia."
    };

    private static final String[] EJEMPLOS = {
            "Ejemplo:\nSin cambios automáticos.\nTú controlas peso y reps.",
            "Ejemplo (objetivo 3 × 8):\n▸ Ses. 1: 40 kg × 10, 10, 10 → sube\n▸ Ses. 2: 42.5 kg × 8, 8, 8 → mismo peso, +1 rep\n▸ Ses. 3: 42.5 kg × 9, 9, 9 → sigue subiendo",
            "Ejemplo (objetivo 3 × 8):\n▸ Ses. 1: 40 kg × 8, 8, 8 → sube\n▸ Ses. 2: 42.5 kg × 8, 8, 8 → sube\n▸ Ses. 3: 45 kg × 8, 8, 7 → repite",
            "Ejemplo (objetivo última serie ≥ 5):\n▸ Ses. 1: 60 kg últimas 7 → sube\n▸ Ses. 2: 62.5 kg últimas 5 → sube\n▸ Ses. 3: 65 kg últimas 4 → repite",
            "Ejemplo (rango 8-12 en 3 series):\n▸ Ses. 1: 30 kg × 8, 8, 8 → +1 rep\n▸ Ses. 2: 30 kg × 9, 9, 9 → +1 rep\n▸ Ses. 3: 30 kg × 12, 12, 12 → sube peso, vuelve a 8"
    };

    private static final String[] RECOMENDADOS = {
            "Recomendado para: programas externos, deportes específicos.",
            "Recomendado para: la mayoría de usuarios, sobre todo principiantes.",
            "Recomendado para: fuerza pura, rutinas de 3-5 reps.",
            "Recomendado para: press banca, sentadilla y peso muerto con AMRAP.",
            "Recomendado para: hipertrofia (rango 6-12 reps)."
    };

    public static void mostrar(Activity activity) {
        InfoProgresionDialog d = new InfoProgresionDialog(activity);
        d.show();
    }

    private InfoProgresionDialog(Activity activity) {
        super(activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_info_progresion);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        LinearLayout container = findViewById(R.id.container_esquemas);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < DRAWABLES.length; i++) {
            View item = inflater.inflate(R.layout.item_esquema_info, container, false);
            ((ImageView) item.findViewById(R.id.image_esquema)).setImageResource(DRAWABLES[i]);
            ((TextView) item.findViewById(R.id.text_titulo)).setText(TITULOS[i]);
            ((TextView) item.findViewById(R.id.text_descripcion)).setText(DESCRIPCIONES[i]);
            ((TextView) item.findViewById(R.id.text_ejemplo)).setText(EJEMPLOS[i]);
            ((TextView) item.findViewById(R.id.text_recomendado)).setText(RECOMENDADOS[i]);
            container.addView(item);
        }

        configurarColapsable(R.id.header_deload, R.id.body_deload,
                "El deload es una bajada preventiva del peso cuando el motor detecta que estás estancado "
                        + "(2 o más sesiones seguidas sin cumplir el objetivo). Se baja al 90% del peso actual, "
                        + "redondeado a tu incremento configurado, para que puedas retomar la progresión con margen.");

        configurarColapsable(R.id.header_silenciar, R.id.body_silenciar,
                "Al marcar 'Silenciar sugerencias' en un ejercicio, la app deja de generar sugerencias post-sesión "
                        + "para ese ejercicio. Útil si prefieres ajustarlo a mano o si estás en una fase donde no quieres progresión "
                        + "automática (p. ej. un test de fuerza máxima). Podés reactivarlo cuando quieras.");

        findViewById(R.id.button_entendido).setOnClickListener(v -> dismiss());
    }

    private void configurarColapsable(int idHeader, int idBody, String texto) {
        TextView header = findViewById(idHeader);
        TextView body = findViewById(idBody);
        body.setText(texto);
        body.setVisibility(View.GONE);
        header.setOnClickListener(v -> body.setVisibility(body.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));
    }
}
```

- [x] **Step 4: Compilar**

```bash
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/InfoProgresionDialog.java app/src/main/res/layout/dialog_info_progresion.xml app/src/main/res/layout/item_esquema_info.xml
git commit -m "Añadir InfoProgresionDialog con los cinco esquemas explicados"
```

---

### Task 7: `AjustesActivity` + entrada desde HomeFragment

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/ui/AjustesActivity.java`
- Create: `app/src/main/res/layout/activity_ajustes.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/HomeFragment.java` (entrada al menú)

**Interfaces:**
- Consumes: `DataManager.getInstance()`, `Usuario.setIncrementoPeso(double)` (Task 1), `InfoProgresionDialog.mostrar(activity)` (Task 6).
- Produces: nueva Activity `AjustesActivity` (con toolbar) que expone chips de incremento (1/2.5/5 kg), tarjeta expandible de convención de peso, y enlace a InfoProgresionDialog.

**Nota para el ejecutor:** La entrada desde HomeFragment puede ser un botón/card en el layout existente O un ítem en el `Toolbar` overflow menu — big pickle elige lo que mejor encaje con el layout actual. La convención (arriba y a la derecha, ícono de tuerca) es un menú overflow.

- [x] **Step 1: Crear `activity_ajustes.xml`**

Layout raíz `CoordinatorLayout` con `AppBarLayout` + `MaterialToolbar` id `toolbar` (idéntico patrón a `activity_edit_routine.xml` que ya existe), seguido de `NestedScrollView` con `LinearLayout` vertical con tres `MaterialCardView`:

**Card 1 — "Incremento de peso por defecto":**
- `TextView` título (18sp bold).
- `TextView` subtítulo ("El motor de progresión usará este valor para subir o bajar peso. Puedes cambiarlo en cada ejercicio.").
- `ChipGroup` id `chip_group_incremento` con `singleSelection="true"` y 3 `Chip`:
  - id `chip_1` texto "1 kg"
  - id `chip_2_5` texto "2.5 kg"
  - id `chip_5` texto "5 kg"

**Card 2 — "Cómo registro el peso":**
- `TextView` id `header_convencion` clickable ("Cómo registro el peso ▾").
- `TextView` id `body_convencion` inicialmente `visibility="gone"` con el texto de la tabla de convenciones en formato lista simple (mancuernas: una sola; barra: barra + discos; máquinas: pin; peso corporal: 0; peso corporal + lastre: solo lastre; Smith: discos totales).

**Card 3 — "¿Cómo funcionan las progresiones?":**
- `TextView` clickable id `link_info_progresion` con texto subrayado.

- [x] **Step 2: Crear `AjustesActivity.java`**

```java
package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.chip.ChipGroup;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.Usuario;

public class AjustesActivity extends BaseActivity {

    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        configurarToolbar(R.id.toolbar, "Ajustes", true);

        dataManager = DataManager.getInstance(this);
        Usuario usuario = dataManager.getDataStore().getUsuario();

        ChipGroup chips = findViewById(R.id.chip_group_incremento);
        double actual = usuario != null ? usuario.getIncrementoPeso() : 2.5;
        if (actual == 1.0) {
            chips.check(R.id.chip_1);
        } else if (actual == 5.0) {
            chips.check(R.id.chip_5);
        } else {
            chips.check(R.id.chip_2_5);
        }
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || usuario == null) return;
            int id = checkedIds.get(0);
            double nuevo;
            if (id == R.id.chip_1) nuevo = 1.0;
            else if (id == R.id.chip_5) nuevo = 5.0;
            else nuevo = 2.5;
            usuario.setIncrementoPeso(nuevo);
            dataManager.save();
        });

        TextView headerConvencion = findViewById(R.id.header_convencion);
        View bodyConvencion = findViewById(R.id.body_convencion);
        headerConvencion.setOnClickListener(v -> bodyConvencion.setVisibility(
                bodyConvencion.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        findViewById(R.id.link_info_progresion).setOnClickListener(v ->
                InfoProgresionDialog.mostrar(AjustesActivity.this));
    }
}
```

**Nota:** Verificar que `DataManager.getInstance(context)` y `DataManager.getDataStore()` existan con esos nombres exactos; si difieren, ajustar. También verificar que `dataManager.save()` sea el método público correcto (podría llamarse distinto).

- [x] **Step 3: Registrar en `AndroidManifest.xml`**

En [AndroidManifest.xml](../../app/src/main/AndroidManifest.xml), junto a las otras `<activity>`:

```xml
<activity
    android:name=".ui.AjustesActivity"
    android:exported="false" />
```

- [x] **Step 4: Agregar entrada desde HomeFragment**

En [HomeFragment.java](../../app/src/main/java/com/ironquest/mvp/ui/HomeFragment.java), primero inspeccionar cómo se maneja el menú overflow actual (si existe). Si HomeFragment tiene un menú de opciones, agregar un ítem "Ajustes"; si no, elegir el punto de entrada más natural del layout actual (por ejemplo un botón en `fragment_home.xml`).

Opción recomendada: agregar un ítem en el menú overflow de MainActivity. En `res/menu/main_menu.xml` (crear si no existe, o modificar el que haya):

```xml
<item
    android:id="@+id/menu_ajustes"
    android:title="Ajustes"
    android:orderInCategory="100"
    app:showAsAction="never" />
```

Y en `MainActivity.java`, override `onOptionsItemSelected`:

```java
@Override
public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.menu_ajustes) {
        startActivity(new Intent(this, AjustesActivity.class));
        return true;
    }
    return super.onOptionsItemSelected(item);
}
```

Y `onCreateOptionsMenu` para inflar el menú si no está ya inflado.

Si `MainActivity` ya tiene un menú overflow con "Acerca de", solo agregar el ítem "Ajustes" al mismo XML.

- [x] **Step 5: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En el teléfono: abrir la app → menú overflow → tap "Ajustes" → verificar que se abre `AjustesActivity`, que los chips reflejan el valor guardado (default 2.5), que cambiar el chip persiste (cerrar y reabrir), que la tarjeta de convención se expande y colapsa, y que el enlace abre `InfoProgresionDialog` con las 5 tarjetas.

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/AjustesActivity.java app/src/main/res/layout/activity_ajustes.xml app/src/main/AndroidManifest.xml app/src/main/java/com/ironquest/mvp/ui/MainActivity.java app/src/main/res/menu/
git commit -m "Añadir AjustesActivity con incremento y accesos a la ayuda"
```

---

### Task 8: Override de incremento en `DetalleEjercicioDialog`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/DetalleEjercicioDialog.java`
- Modify: `app/src/main/res/layout/dialog_detalle_ejercicio.xml` (o el layout que use el diálogo, verificar el nombre exacto).

**Interfaces:**
- Consumes: `Ejercicio.getIncrementoPeso() / setIncrementoPeso(double)` (Task 1), `Usuario.getIncrementoPeso()` (Task 1), `DataManager` para guardar.
- Produces: `ChipGroup` con opciones "Global (X kg) / 1 kg / 2.5 kg / 5 kg" que setea `Ejercicio.incrementoPeso` (0 = Global, o el valor específico).

- [x] **Step 1: Inspeccionar el layout del diálogo**

```bash
grep -rn "R.layout.dialog_detalle_ejercicio\|setContentView.*detalle_ejercicio" app/src/main/java/com/ironquest/mvp/ui/DetalleEjercicioDialog.java
```

Confirmar el nombre exacto del layout que usa DetalleEjercicioDialog. Los pasos que siguen asumen `dialog_detalle_ejercicio.xml`; si difiere, adaptar.

- [x] **Step 2: Agregar sección en el layout del diálogo**

En el layout, después del bloque de músculo objetivo (o donde tenga sentido en el flujo visual), agregar:

```xml
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="12dp"
    android:text="Incremento sugerido"
    android:textAppearance="?attr/textAppearanceLabelLarge" />

<com.google.android.material.chip.ChipGroup
    android:id="@+id/chip_group_incremento_ejercicio"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:singleSelection="true"
    app:selectionRequired="true">

    <com.google.android.material.chip.Chip
        android:id="@+id/chip_incremento_global"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        style="@style/Widget.Material3.Chip.Filter"
        android:text="Global" />

    <com.google.android.material.chip.Chip
        android:id="@+id/chip_incremento_1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        style="@style/Widget.Material3.Chip.Filter"
        android:text="1 kg" />

    <com.google.android.material.chip.Chip
        android:id="@+id/chip_incremento_2_5"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        style="@style/Widget.Material3.Chip.Filter"
        android:text="2.5 kg" />

    <com.google.android.material.chip.Chip
        android:id="@+id/chip_incremento_5"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        style="@style/Widget.Material3.Chip.Filter"
        android:text="5 kg" />
</com.google.android.material.chip.ChipGroup>
```

- [x] **Step 3: Wire-up en `DetalleEjercicioDialog.java`**

En `onCreate()` del diálogo (después de que el `Ejercicio ejercicio` esté disponible), agregar:

```java
ChipGroup chipsIncremento = findViewById(R.id.chip_group_incremento_ejercicio);
double actual = ejercicio.getIncrementoPeso();
if (actual == 1.0) {
    chipsIncremento.check(R.id.chip_incremento_1);
} else if (actual == 2.5) {
    chipsIncremento.check(R.id.chip_incremento_2_5);
} else if (actual == 5.0) {
    chipsIncremento.check(R.id.chip_incremento_5);
} else {
    chipsIncremento.check(R.id.chip_incremento_global);
}

// Etiqueta dinámica del chip "Global" con el valor del usuario actual
Chip chipGlobal = findViewById(R.id.chip_incremento_global);
double globalUsuario = dataStore.getUsuario() != null ? dataStore.getUsuario().getIncrementoPeso() : 2.5;
chipGlobal.setText("Global (" + formatearIncremento(globalUsuario) + " kg)");

chipsIncremento.setOnCheckedStateChangeListener((group, checkedIds) -> {
    if (checkedIds.isEmpty()) return;
    int id = checkedIds.get(0);
    double nuevo;
    if (id == R.id.chip_incremento_1) nuevo = 1.0;
    else if (id == R.id.chip_incremento_2_5) nuevo = 2.5;
    else if (id == R.id.chip_incremento_5) nuevo = 5.0;
    else nuevo = 0.0; // Global
    ejercicio.setIncrementoPeso(nuevo);
    dataManager.save();
});
```

Agregar el helper local si no existe:

```java
private static String formatearIncremento(double v) {
    if (v == Math.floor(v)) return String.valueOf((long) v);
    return String.valueOf(v);
}
```

Imports necesarios:
```java
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
```

- [x] **Step 4: Compilar**

```bash
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [x] **Step 5: Verificación en dispositivo**

Instalar el APK, abrir el diálogo de detalle de un ejercicio, verificar que el chip "Global" muestra el valor del usuario (p.ej. "Global (2.5 kg)"), que seleccionar "5 kg" persiste (cerrar y reabrir el diálogo).

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/DetalleEjercicioDialog.java app/src/main/res/layout/dialog_detalle_ejercicio.xml
git commit -m "Añadir override de incremento por ejercicio en DetalleEjercicioDialog"
```

---

### Task 9: `RutinaEjercicioEditAdapter` — spinner de esquema + checkbox silenciar + ícono info

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/RutinaEjercicioEditAdapter.java`
- Modify: `app/src/main/res/layout/item_ejercicio_rutina_edit.xml`

**Interfaces:**
- Consumes: `RutinaEjercicio.ESQUEMA_*` (Task 1 y existentes), `RutinaEjercicio.isSilenciarSugerencia()/setSilenciarSugerencia()` (Task 1), `InfoProgresionDialog.mostrar(activity)` (Task 6).
- Produces: la fila de edición muestra un `Spinner` con los 5 esquemas, un `CheckBox` "Silenciar sugerencias", y un `ImageView` clickable de info al lado del spinner.

**Contexto para el ejecutor:** El adapter ya edita filas de ejercicio en `EditRoutineActivity`. Este task agrega tres controles a cada fila. **Importante:** el default para ejercicios nuevos (agregados desde este slice en adelante) debe ser `ESQUEMA_AUTOMATICO`; los ejercicios existentes con `ESQUEMA_NINGUNO` (0) siguen mostrando "Ninguno" y **no** se autoconvierten.

- [x] **Step 1: Extender el layout de la fila**

En `layout/item_ejercicio_rutina_edit.xml`, agregar (al final de los controles existentes de peso/reps/series, o en una sección secundaria si el layout ya está denso):

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:layout_marginTop="8dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Progresión: "
        android:textSize="14sp" />

    <Spinner
        android:id="@+id/spinner_esquema"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1" />

    <ImageView
        android:id="@+id/button_info_progresion"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:layout_marginStart="8dp"
        android:src="@android:drawable/ic_menu_info_details"
        android:contentDescription="Ver info sobre esquemas de progresión"
        android:clickable="true"
        android:focusable="true" />
</LinearLayout>

<CheckBox
    android:id="@+id/check_silenciar_sugerencia"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Silenciar sugerencias de progresión"
    android:layout_marginTop="4dp" />
```

- [x] **Step 2: Wire-up en `RutinaEjercicioEditAdapter.java`**

En el `ViewHolder` (o donde se binden las vistas), agregar campos:

```java
final Spinner spinnerEsquema;
final ImageView botonInfo;
final CheckBox checkSilenciar;
```

Inicializar en el constructor del ViewHolder:
```java
spinnerEsquema = itemView.findViewById(R.id.spinner_esquema);
botonInfo = itemView.findViewById(R.id.button_info_progresion);
checkSilenciar = itemView.findViewById(R.id.check_silenciar_sugerencia);
```

En `onBindViewHolder`, configurar cada control (asumiendo que `re` es el `RutinaEjercicio` que se binda):

```java
// Spinner de esquema
String[] etiquetas = {"Ninguno", "Automático", "Lineal", "Greyskull", "Doble Progresión"};
int[] valores = {
        RutinaEjercicio.ESQUEMA_NINGUNO,
        RutinaEjercicio.ESQUEMA_AUTOMATICO,
        RutinaEjercicio.ESQUEMA_LINEAL,
        RutinaEjercicio.ESQUEMA_GREYSKULL,
        RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION
};
ArrayAdapter<String> adapter = new ArrayAdapter<>(holder.itemView.getContext(),
        android.R.layout.simple_spinner_item, etiquetas);
adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
holder.spinnerEsquema.setAdapter(adapter);
int seleccionInicial = 0;
for (int i = 0; i < valores.length; i++) {
    if (valores[i] == re.getEsquemaProgresion()) {
        seleccionInicial = i;
        break;
    }
}
holder.spinnerEsquema.setSelection(seleccionInicial, false);
holder.spinnerEsquema.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int nuevoValor = valores[position];
        if (nuevoValor != re.getEsquemaProgresion()) {
            re.setEsquemaProgresion(nuevoValor);
            listener.onCambio(); // reusar el callback existente que persiste la rutina
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) { }
});

// Checkbox silenciar
holder.checkSilenciar.setOnCheckedChangeListener(null); // limpiar listener previo por reciclaje
holder.checkSilenciar.setChecked(re.isSilenciarSugerencia());
holder.checkSilenciar.setOnCheckedChangeListener((buttonView, isChecked) -> {
    if (isChecked != re.isSilenciarSugerencia()) {
        re.setSilenciarSugerencia(isChecked);
        listener.onCambio();
    }
});

// Botón info
holder.botonInfo.setOnClickListener(v -> {
    if (v.getContext() instanceof android.app.Activity) {
        InfoProgresionDialog.mostrar((android.app.Activity) v.getContext());
    }
});
```

Imports:
```java
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import com.ironquest.mvp.model.RutinaEjercicio;
```

**Nota:** `listener.onCambio()` es un placeholder para el callback existente del adapter que dispara persistencia. El ejecutor debe usar el nombre exacto del método del listener existente (revisar la interfaz `Listener` interna del adapter). Si no hay un método así, agregar `listener.onCambioRutina()` o similar, o llamar directo a `dataManager.save()` si el adapter tiene acceso.

- [x] **Step 3: Ajustar el default para ejercicios nuevos**

En [EditRoutineActivity.java](../../app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java) o donde se cree un nuevo `RutinaEjercicio` al agregar un ejercicio a la rutina, buscar la línea de construcción:

```bash
grep -rn "new RutinaEjercicio" app/src/main/java/com/ironquest/mvp/ui/
```

Después de la construcción, agregar:
```java
nuevoRe.setEsquemaProgresion(RutinaEjercicio.ESQUEMA_AUTOMATICO);
```

Sólo para los `RutinaEjercicio` creados fresh — nunca modificar los que ya existen en la rutina (retro-compatibilidad).

- [x] **Step 4: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Abrir una rutina existente en editar → verificar que cada ejercicio muestra el spinner con "Ninguno" seleccionado (retro-compat), el checkbox desmarcado, y el ícono info abre el diálogo. Agregar un ejercicio nuevo → verificar que aparece con "Automático" preseleccionado. Cambiar el esquema, cerrar y reabrir → persiste.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/RutinaEjercicioEditAdapter.java app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java app/src/main/res/layout/item_ejercicio_rutina_edit.xml
git commit -m "Añadir spinner de esquema, silenciar y acceso a info por fila de ejercicio"
```

---

### Task 10: Card de sugerencias en `SessionSummaryActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/SessionSummaryActivity.java`
- Modify: `app/src/main/res/layout/activity_session_summary.xml`
- Create: `app/src/main/res/layout/card_sugerencias.xml`
- Create: `app/src/main/res/layout/item_sugerencia_pendiente.xml`

**Interfaces:**
- Consumes: `DataManager.getSugerenciasPendientesDeRutina(rutinaId)`, `aplicarSugerencia(id)`, `eliminarSugerencia(id)`, `eliminarSugerenciasDeRutina(id)` (Task 2). `EXTRA_RUTINA_ID` y `EXTRA_SESION_ID` (Task 4). `dataStore.buscarEjercicio(id)` para leer nombre del ejercicio.
- Produces: encima del resumen actual, un `MaterialCardView` con lista de sugerencias de la sesión que acaba de cerrar. Botones "Aceptar" y "Ignorar" por ítem + "Aceptar todas" arriba. Se colapsa cuando queda vacío.

- [x] **Step 1: Crear `card_sugerencias.xml`**

Layout raíz `MaterialCardView`, con `LinearLayout` vertical adentro:
- Cabecera: `TextView` título ("Sugerencias de progresión"), `TextView` subtítulo, `Button` id `button_aceptar_todas` texto "Aceptar todas".
- `LinearLayout` id `container_sugerencias` (vertical) donde se inflan los items.

Este layout se reusa desde el card en SessionSummary y desde el diálogo en EditRoutine (Task 11).

- [x] **Step 2: Crear `item_sugerencia_pendiente.xml`**

Layout raíz `LinearLayout` vertical con:
- `TextView` id `text_nombre_ejercicio` (bold, 16sp).
- `TextView` id `text_cambio` (p. ej. "40 kg × 8 → 42.5 kg × 8", 14sp).
- `TextView` id `text_explicacion` (14sp, gris).
- `LinearLayout` horizontal con dos `Button`: id `button_aceptar` ("Aceptar") y `button_ignorar` ("Ignorar", style secundario/outlined).

Padding vertical 8dp, divider inferior gris claro.

- [x] **Step 3: Incluir el card en `activity_session_summary.xml`**

Al principio del contenido de la Activity (encima del bloque de estadísticas actuales), agregar:

```xml
<include
    android:id="@+id/card_sugerencias"
    layout="@layout/card_sugerencias" />
```

Con `visibility="gone"` inicial (se muestra vía Java si hay sugerencias).

- [x] **Step 4: Wire-up en `SessionSummaryActivity.java`**

Agregar imports:
```java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.SugerenciaPendiente;

import java.util.List;
```

En `onCreate()`, después del bloque actual de bindings, agregar:

```java
DataManager dataManager = DataManager.getInstance(this);
DataStore dataStore = dataManager.getDataStore();
String rutinaId = getIntent().getStringExtra(EXTRA_RUTINA_ID);
String sesionId = getIntent().getStringExtra(EXTRA_SESION_ID);

View cardSugerencias = findViewById(R.id.card_sugerencias);
renderSugerencias(cardSugerencias, dataManager, dataStore, rutinaId, sesionId);
```

Y agregar el método:

```java
private void renderSugerencias(View card, DataManager dataManager, DataStore dataStore,
                                String rutinaId, String sesionId) {
    List<SugerenciaPendiente> todas = dataManager.getSugerenciasPendientesDeRutina(rutinaId);
    List<SugerenciaPendiente> deEstaSesion = new java.util.ArrayList<>();
    for (SugerenciaPendiente sp : todas) {
        if (sesionId != null && sesionId.equals(sp.getSesionOrigenId())) {
            deEstaSesion.add(sp);
        }
    }
    if (deEstaSesion.isEmpty()) {
        card.setVisibility(View.GONE);
        return;
    }
    card.setVisibility(View.VISIBLE);
    LinearLayout container = card.findViewById(R.id.container_sugerencias);
    container.removeAllViews();
    LayoutInflater inflater = LayoutInflater.from(this);
    for (SugerenciaPendiente sp : deEstaSesion) {
        View item = inflater.inflate(R.layout.item_sugerencia_pendiente, container, false);
        Ejercicio ej = dataStore.buscarEjercicio(sp.getEjercicioId());
        String nombre = ej != null ? ej.getNombre() : "Ejercicio";
        ((android.widget.TextView) item.findViewById(R.id.text_nombre_ejercicio)).setText(nombre);
        String cambio = formatearCambio(sp);
        ((android.widget.TextView) item.findViewById(R.id.text_cambio)).setText(cambio);
        ((android.widget.TextView) item.findViewById(R.id.text_explicacion)).setText(sp.getExplicacion());
        item.findViewById(R.id.button_aceptar).setOnClickListener(v -> {
            dataManager.aplicarSugerencia(sp.getId());
            renderSugerencias(card, dataManager, dataStore, rutinaId, sesionId);
        });
        item.findViewById(R.id.button_ignorar).setOnClickListener(v -> {
            dataManager.eliminarSugerencia(sp.getId());
            renderSugerencias(card, dataManager, dataStore, rutinaId, sesionId);
        });
        container.addView(item);
    }
    card.findViewById(R.id.button_aceptar_todas).setOnClickListener(v -> {
        for (SugerenciaPendiente sp : new java.util.ArrayList<>(deEstaSesion)) {
            dataManager.aplicarSugerencia(sp.getId());
        }
        renderSugerencias(card, dataManager, dataStore, rutinaId, sesionId);
    });
}

private static String formatearCambio(SugerenciaPendiente sp) {
    return formatearPeso(sp.getPesoActual()) + " kg × " + sp.getRepeticionesActuales()
            + " → " + formatearPeso(sp.getPesoSugerido()) + " kg × " + sp.getRepeticionesSugeridas();
}

private static String formatearPeso(double p) {
    if (p == 0) return "0";
    if (p == Math.floor(p)) return String.valueOf((long) p);
    return String.valueOf(p);
}
```

- [x] **Step 5: Compilar y verificación en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Requiere flujo end-to-end:** en el teléfono, crear una rutina de prueba con esquema Automático y correr al menos 2 sesiones donde el motor detecte cumplimiento con margen (registrar reps por encima del objetivo). Al terminar la 3ª sesión, verificar que el card de sugerencias aparece en SessionSummary con al menos un ítem, que "Aceptar" cambia el peso de la rutina, y que "Ignorar" la elimina sin cambiar la rutina.

- [x] **Step 6: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/SessionSummaryActivity.java app/src/main/res/layout/activity_session_summary.xml app/src/main/res/layout/card_sugerencias.xml app/src/main/res/layout/item_sugerencia_pendiente.xml
git commit -m "Mostrar card de sugerencias en SessionSummaryActivity"
```

---

### Task 11: Banner de sugerencias en `EditRoutineActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java`
- Modify: `app/src/main/res/layout/activity_edit_routine.xml`
- Create: `app/src/main/res/layout/dialog_revisar_sugerencias.xml`

**Interfaces:**
- Consumes: `DataManager.getSugerenciasPendientesDeRutina(id)`, `aplicarSugerencia(...)`, `eliminarSugerencia(...)`, `eliminarSugerenciasDeRutina(...)` (Task 2). Reusa `card_sugerencias.xml` / `item_sugerencia_pendiente.xml` (Task 10).
- Produces: banner encima del banner de migración legacy con conteo de sugerencias + botones "Revisar" e "Ignorar todos". "Revisar" abre un diálogo que reusa el mismo card.

- [x] **Step 1: Agregar el banner al layout**

En [activity_edit_routine.xml](../../app/src/main/res/layout/activity_edit_routine.xml), encima del `cardMigracion` existente:

```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card_banner_sugerencias"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="12dp"
    android:visibility="gone">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">

        <TextView
            android:id="@+id/text_banner_sugerencias"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="end"
            android:layout_marginTop="8dp">

            <Button
                android:id="@+id/button_ignorar_todos_sugerencias"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Ignorar todos"
                android:layout_marginEnd="8dp" />

            <Button
                android:id="@+id/button_revisar_sugerencias"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Revisar" />
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [x] **Step 2: Crear `dialog_revisar_sugerencias.xml`**

Layout raíz `LinearLayout` vertical con:
- Título "Sugerencias de progresión".
- `<include layout="@layout/card_sugerencias" />` con id `card_sugerencias_dialog`.
- Botón "Cerrar" al final.

- [x] **Step 3: Wire-up en `EditRoutineActivity.java`**

Agregar campos:
```java
private View cardBannerSugerencias;
private TextView textBannerSugerencias;
```

En `onCreate` (después de encontrar el resto de vistas):
```java
cardBannerSugerencias = findViewById(R.id.card_banner_sugerencias);
textBannerSugerencias = findViewById(R.id.text_banner_sugerencias);
findViewById(R.id.button_revisar_sugerencias).setOnClickListener(v -> abrirDialogoRevisar());
findViewById(R.id.button_ignorar_todos_sugerencias).setOnClickListener(v -> {
    dataManager.eliminarSugerenciasDeRutina(rutina.getId());
    actualizarBannerSugerencias();
});
```

Y en `onResume()` (para reflejar cambios cuando se vuelve de otra pantalla):
```java
@Override
protected void onResume() {
    super.onResume();
    actualizarBannerSugerencias();
}
```

Métodos nuevos:

```java
private void actualizarBannerSugerencias() {
    List<SugerenciaPendiente> sp = dataManager.getSugerenciasPendientesDeRutina(rutina.getId());
    if (sp.isEmpty()) {
        cardBannerSugerencias.setVisibility(View.GONE);
        return;
    }
    cardBannerSugerencias.setVisibility(View.VISIBLE);
    int n = sp.size();
    textBannerSugerencias.setText(n + (n == 1 ? " ejercicio tiene ajuste sugerido." : " ejercicios tienen ajustes sugeridos."));
}

private void abrirDialogoRevisar() {
    android.app.Dialog dialog = new android.app.Dialog(this);
    dialog.setContentView(R.layout.dialog_revisar_sugerencias);
    dialog.getWindow().setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    View card = dialog.findViewById(R.id.card_sugerencias_dialog);
    renderSugerenciasEnCard(card, dialog);
    dialog.findViewById(R.id.button_cerrar_dialog).setOnClickListener(v -> {
        dialog.dismiss();
        actualizarBannerSugerencias();
    });
    dialog.setOnDismissListener(d -> actualizarBannerSugerencias());
    dialog.show();
}

private void renderSugerenciasEnCard(View card, android.app.Dialog dialog) {
    List<SugerenciaPendiente> deLaRutina = dataManager.getSugerenciasPendientesDeRutina(rutina.getId());
    if (deLaRutina.isEmpty()) {
        card.setVisibility(View.GONE);
        return;
    }
    card.setVisibility(View.VISIBLE);
    LinearLayout container = card.findViewById(R.id.container_sugerencias);
    container.removeAllViews();
    LayoutInflater inflater = LayoutInflater.from(this);
    for (SugerenciaPendiente sp : deLaRutina) {
        View item = inflater.inflate(R.layout.item_sugerencia_pendiente, container, false);
        Ejercicio ej = dataStore.buscarEjercicio(sp.getEjercicioId());
        String nombre = ej != null ? ej.getNombre() : "Ejercicio";
        ((TextView) item.findViewById(R.id.text_nombre_ejercicio)).setText(nombre);
        ((TextView) item.findViewById(R.id.text_cambio)).setText(formatearCambio(sp));
        ((TextView) item.findViewById(R.id.text_explicacion)).setText(sp.getExplicacion());
        item.findViewById(R.id.button_aceptar).setOnClickListener(v -> {
            dataManager.aplicarSugerencia(sp.getId());
            renderSugerenciasEnCard(card, dialog);
            refrescarListadoRutina();
        });
        item.findViewById(R.id.button_ignorar).setOnClickListener(v -> {
            dataManager.eliminarSugerencia(sp.getId());
            renderSugerenciasEnCard(card, dialog);
        });
        container.addView(item);
    }
    card.findViewById(R.id.button_aceptar_todas).setOnClickListener(v -> {
        for (SugerenciaPendiente sp : new java.util.ArrayList<>(deLaRutina)) {
            dataManager.aplicarSugerencia(sp.getId());
        }
        renderSugerenciasEnCard(card, dialog);
        refrescarListadoRutina();
    });
}

private static String formatearCambio(SugerenciaPendiente sp) {
    return formatearPeso(sp.getPesoActual()) + " kg × " + sp.getRepeticionesActuales()
            + " → " + formatearPeso(sp.getPesoSugerido()) + " kg × " + sp.getRepeticionesSugeridas();
}

private static String formatearPeso(double p) {
    if (p == 0) return "0";
    if (p == Math.floor(p)) return String.valueOf((long) p);
    return String.valueOf(p);
}
```

`refrescarListadoRutina()` es un placeholder para el método que ya refresca la lista de ejercicios del adapter (buscar en la Activity el que actualiza el `RutinaEjercicioEditAdapter` cuando la rutina cambia; probablemente sea `adapter.notifyDataSetChanged()` o similar).

Agregar botón "Cerrar" con id `button_cerrar_dialog` en `dialog_revisar_sugerencias.xml` si no está.

Imports:
```java
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.SugerenciaPendiente;
import java.util.List;
```

- [x] **Step 4: Compilar y verificación en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verificar: con al menos una `SugerenciaPendiente` activa (creada al finalizar sesión en Task 4/10), abrir el editor de la rutina → banner aparece con conteo correcto. Tap "Revisar" → diálogo muestra los items del mismo tipo que en SessionSummary. Aceptar uno → banner actualiza el conteo, la rutina refresca el peso. "Ignorar todos" → banner desaparece.

- [x] **Step 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java app/src/main/res/layout/activity_edit_routine.xml app/src/main/res/layout/dialog_revisar_sugerencias.xml
git commit -m "Añadir banner y diálogo de sugerencias pendientes en EditRoutineActivity"
```

---

### Task 12: Verificación end-to-end, bump de versión y release

**Files:**
- Modify: `app/build.gradle`

**Interfaces:**
- Todo lo anterior integrado.
- Produce: APK `titan-score.apk` publicado como release `v0.9.0` en el repo público.

**Contexto para el ejecutor:** Antes de esta tarea, respaldar `datos.json` del teléfono del usuario. **Solo ejecutar el release cuando el usuario apruebe explícitamente.**

- [x] **Step 1: Respaldar `datos.json`**

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json > /tmp/respaldo_pre_v0.9.0.json
ls -la /tmp/respaldo_pre_v0.9.0.json
```

Esperado: archivo con contenido (no 0 bytes).

- [x] **Step 2: Bump versión en `app/build.gradle`**

En [app/build.gradle](../../app/build.gradle), cambiar:
```gradle
versionCode 9
versionName "0.8.0"
```

Por:
```gradle
versionCode 10
versionName "0.9.0"
```

- [x] **Step 3: Build de release**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
echo "Exit: ${PIPESTATUS[0]}"
ls -la app/build/outputs/apk/debug/app-debug.apk
```

Esperado: `BUILD SUCCESSFUL`, APK generado (~41 MB).

- [x] **Step 4: Chequeo del CLAUDE.md**

```bash
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Esperado: **vacío**.

- [x] **Step 5: Instalar y verificar en dispositivo**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell dumpsys package com.ironquest.mvp | grep versionName
```

Esperado: `versionName=0.9.0`.

Checklist manual en el teléfono (marcar cada uno):

- [x] La app arranca sin crash.
- [x] Rutinas existentes se abren normal, ejercicios muestran esquema "Ninguno" (retro-compat).
- [x] Ajustes accesible desde menú overflow. Chips 1/2.5/5 funcionan y persisten.
- [x] Tarjeta de convención expande/colapsa. Enlace a InfoProgresionDialog abre las 5 tarjetas con drawables correctos.
- [x] Rutina de prueba con esquema Automático, 3 sesiones cumpliendo con margen → card de sugerencias aparece en SessionSummary con "sube X kg".
- [x] "Aceptar" cambia el peso de la rutina. Reabrir editor lo confirma.
- [x] "Ignorar" elimina la sugerencia sin cambiar la rutina.
- [x] 3 sesiones fallando → sugerencia dice "baja a X kg" (deload).
- [x] Con `incrementoPeso` global = 5 y override 2.5 en un ejercicio, el override manda.
- [x] Silenciar sugerencias en un ejercicio → no se genera sugerencia para él aunque cumpla con margen.
- [x] Peso corporal (peso=0): sugerencia opera en reps.
- [x] Banner en EditRoutineActivity aparece con el conteo correcto.
- [x] "Revisar" abre diálogo con los mismos items.
- [x] "Ignorar todos" desde el banner limpia las sugerencias de esa rutina.

- [x] **Step 6: Restaurar `datos.json` real si se usó datos de prueba**

Si durante la verificación se sobreescribió el `datos.json` real, restaurarlo:

```bash
# CUIDADO: verificar primero que el respaldo existe y no es vacío
ls -la /tmp/respaldo_pre_v0.9.0.json
adb push /tmp/respaldo_pre_v0.9.0.json /sdcard/respaldo_restore.json
adb shell "run-as com.ironquest.mvp cp /sdcard/respaldo_restore.json files/datos.json"
adb shell rm /sdcard/respaldo_restore.json
adb shell run-as com.ironquest.mvp ls -la files/datos.json
```

- [x] **Step 7: Commit de la versión**

```bash
git add app/build.gradle
git commit -m "Subir versión a 0.9.0 (versionCode 10)"
git log --oneline -5
```

- [ ] **Step 8: Pedir al usuario aprobación para pushear y publicar**

**⚠ NO ejecutar los siguientes pasos sin autorización explícita del usuario.**

Presentar al usuario:
- Lista de commits nuevos desde el último release (`git log v0.8.0..HEAD --oneline`).
- Confirmación de que todos los checks del step 5 pasaron.
- Confirmación de que datos.json real fue restaurado (si aplica).

Preguntar: "¿Autorizas el push a origin/main y el release v0.9.0?"

- [ ] **Step 9: Push y release (solo con autorización)**

```bash
git push origin main
```

Crear notas de release en `/tmp/notas_v0.9.0.md`:

```markdown
## v0.9.0 — Auto-progresión aplicada a la rutina

- Nuevo esquema de progresión "Automático" — la app mira tus últimas 3 sesiones y
  decide subir peso, subir reps, mantener o hacer deload.
- Sugerencias post-sesión: al terminar el entrenamiento, un card muestra los ajustes
  propuestos. Aceptarlos actualiza tu rutina para la próxima vez.
- Banner en el editor de rutina cuando hay sugerencias pendientes.
- Incremento de peso configurable: 1 / 2.5 / 5 kg global + override por ejercicio
  (útil si tu gym solo tiene discos grandes).
- Info dialog educativo con los cinco esquemas de progresión (Ninguno, Automático,
  Lineal, Greyskull, Doble Progresión) — visual, con ejemplos.
- Nueva pantalla de Ajustes accesible desde el menú overflow.

Datos existentes intactos. Ejercicios ya configurados con Lineal/Greyskull/Doble
mantienen su esquema; el nuevo "Automático" es el default para ejercicios nuevos.
```

Copiar el APK con el nombre exacto que espera el release:

```bash
cp app/build/outputs/apk/debug/app-debug.apk /tmp/titan-score.apk
gh release create v0.9.0 /tmp/titan-score.apk \
    --repo JuanOV27/titan-score-app \
    --title "v0.9.0 — Auto-progresión aplicada a la rutina" \
    --notes-file /tmp/notas_v0.9.0.md
gh release view v0.9.0 --repo JuanOV27/titan-score-app
```

- [ ] **Step 10: Confirmar al usuario**

Reportar: URL del release, tamaño del APK, commits pusheados.

---

## Self-Review

**Cobertura del spec (2026-09-13-auto-progresion-design.md):**
- Motor híbrido `ProgresionAutomatica` → Task 3.
- Entidad `SugerenciaPendiente` + lista en `DataStore` + normalización → Task 1.
- `DataManager` con `agregar/eliminar/aplicarSugerencia` → Task 2.
- Refactor de firma para leer incremento del usuario → Task 3.
- `ActiveSessionActivity` genera sugerencias al finalizar → Task 4.
- Vector drawables (5) → Task 5.
- `InfoProgresionDialog` → Task 6.
- `AjustesActivity` con incremento global + convención + link a info → Task 7.
- `DetalleEjercicioDialog` con override de incremento → Task 8.
- `RutinaEjercicioEditAdapter` con spinner + silenciar + info → Task 9.
- Card de sugerencias en `SessionSummaryActivity` → Task 10.
- Banner en `EditRoutineActivity` → Task 11.
- Verificación + release → Task 12.
- Retro-compat (`ESQUEMA_NINGUNO` viejo se conserva, `ESQUEMA_AUTOMATICO` solo para nuevos) → Task 9 step 3.
- Peso corporal (progresa por reps, piso 3) → Task 3 (dentro de `ProgresionAutomatica.decidir`).
- Convención de registro de peso (documentada en Ajustes) → Task 7 step 1.

**Consistencia de tipos:**
- `SugerenciaPendiente` constructor con 11 argumentos: id, rutinaId, ejercicioId, sesionOrigenId, pesoActual, pesoSugerido, repeticionesActuales, repeticionesSugeridas, explicacion, tipo, fechaCreacion. Coincide entre Task 1 (definición) y Task 4 (uso).
- Nueva firma `EstrategiaProgresion.sugerir(config, ejercicio, usuario, historial)` coincide entre Task 3 y Task 4.
- `DataManager.aplicarSugerencia(String id)` recibe solo id, coincide en Task 2, 10, 11.
- IDs con prefijo `"sp"` coincide en Task 1 (docstring) y Task 4 (uso `newId("sp")`).

**Placeholders:**
- Task 8 dice "el nombre del método del listener existente" — instrucción al ejecutor de verificar el nombre exacto en el código actual, no un TBD.
- Task 9 dice "listener.onCambio() es un placeholder" — igual, instrucción al ejecutor de usar el nombre real del callback del adapter.
- Task 11 menciona `refrescarListadoRutina()` como placeholder para el método existente del adapter.

Estas tres son delegaciones acotadas al ejecutor para respetar la API interna existente, no lagunas de diseño. Aceptables.

---

## Estado de avance (última actualización: 13/09/2026)

**Slice 1 — Auto-progresión: COMPLETADO en código.** Tasks 1 a 11 implementadas y verificadas
en el dispositivo (el checklist manual del Step 5 del Task 12 también está completo: los 4
checks pendientes — deload, peso corporal, silenciar, "Ignorar todos" — se verificaron en una
sesión aparte).

Commits del Slice 1 en `main`:

```
71538f1 Modelar SugerenciaPendiente y campos base para auto-progresión            (Task 1)
acdaa35 Añadir métodos de SugerenciaPendiente en DataManager                     (Task 2)
4af5dd8 Añadir ProgresionAutomatica y refactorizar el motor para leer el incremento del usuario (Task 3)
57e6c66 Generar SugerenciaPendiente al finalizar sesión                          (Task 4)
30ff918 Añadir vector drawables para los cinco esquemas de progresión            (Task 5)
da9b657 Añadir InfoProgresionDialog con resumen de esquemas, deload y silenciar  (Task 6)
27d62ff Añadir AjustesActivity con incremento global y accesos a la ayuda        (Task 7)
243eab5 Añadir esquema automático, silenciar, info y override de incremento por ejercicio (Tasks 8+9)
b000464 Mostrar sugerencias pendientes en el resumen de la sesión                (Task 10)
a0e005b Añadir banner y diálogo de sugerencias pendientes en EditRoutineActivity (Task 11)
52f26d1 Subir versión a 0.9.0 (versionCode 10)                                    (Task 12, steps 1-7)
```

**Cierre del Task 12 (pendiente de aprobación del usuario):**
- Steps 1-7 cumplidos: datos respaldados en `/tmp/respaldo_pre_v0.9.0.json`, bump a
  `0.9.0`/versionCode 10, build OK, chequeo de frontera Gson limpio (solo javadoc),
  instalado en el teléfono (`versionName=0.9.0`), arranque sin crash, commit `52f26d1`.
- Steps 8-10 NO ejecutados (requieren autorización explícita): push a `origin/main` y
  release `v0.9.0` con `/tmp/titan-score.apk` en `JuanOV27/titan-score-app`.
  Notas de release en `/tmp/notas_v0.9.0.md`.

**Desviación documentada — Task 8:** el plan apuntaba a `DetalleEjercicioDialog`, pero ese
diálogo es código muerto (nadie lo instancia). El override de incremento por ejercicio se
implementó en la fila real de edición (`RutinaEjercicioEditAdapter` + `item_ejercicio_rutina_edit.xml`),
cumpliendo el *Produces* del plan. La verificación confirmó que el override persiste
(`Ejercicio.incrementoPeso`) y que "Global" usa el valor del usuario.

**Pulido post-slice (commit `7a68d3d`) — "Explicar el campo Reps máx y sugerir un techo al
crear ejercicios":**
- Al agregar un ejercicio nuevo, `repeticionesMax` se inicializa con `reps + 4` (solo
  ejercicios nuevos; las rutinas existentes con 0 no se tocan — retro-compat).
- Nueva tarjeta desplegable en `InfoProgresionDialog`: "Reps máx (doble prog.): qué significa
  el campo" — explica piso/techo, el comportamiento del 0 y el +4 sugerido.
- Helper visible en el campo: "0 = mismo que Reps".
- Verificado en el dispositivo: fila nueva muestra el techo sugerido (10 → 14), helper
  visible, sección del diálogo expande con su texto. Los datos reales no se modificaron
  (checksum de `datos.json` idéntico al respaldo pre-v0.9.0).

**Datos del usuario:** durante las verificaciones se usó una rutina/sesión de prueba que se
restauró después; estado en disco idéntico al respaldo `/tmp/titan-score-respaldo-pre-slice1/datos_antes_task89.json`
(= `/tmp/respaldo_pre_v0.9.0.json`, 352718 bytes).

**Sigue:** después del release, arrancar el Slice 2 (fatiga/analítica) en
`docs/plans/2026-09-13-fatiga-analitica.md`.

**Fix post-slice (commit `cfefe76`) — pérdida de cambios al rotar en el editor de rutinas:**
- Bug reportado: al crear una rutina nueva y rotar la pantalla, desaparecían los ejercicios ya
  agregados. Causa: `EditRoutineActivity` se recreaba en la rotación sin preservar la `Rutina`
  en memoria (los cambios solo se persisten al pulsar "Guardar rutina").
- Fix: declarar `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"`
  en el mani­festo para `EditRoutineActivity` (misma convención que
  `ActiveSessionActivity`/`PhysicalProfileActivity`). Verificado en el teléfono: tras girar a
  landscape y volver a portrait, los ejercicios seguían en la rutina en memoria.
- Observación pendiente (no bloqueante): en landscape la fila de un ejercicio del editor se
  muestra colapsada (altura ~27px, contenido oculto) en el dump de UI. No implica pérdida de
  datos; es un tema de layout estético a revisar si se quiere.

**Incidente de datos — rutina "piernas":**
- Durante la verificación se detectó que el `datos.json` del teléfono tenía 7 rutinas (faltaba
  "piernas", `r_7da52eae`) frente al respaldo `/sdcard/restore_pre_task89.json` (352718 bytes,
  13/09 21:36). El diff era exactamente esa rutina: sesiones (13), catálogo (320), sugerencias
  (0) e incremento global (2.5) coincidían.
- Se presentó el cambio al usuario; **decisión: dejarla eliminada** (estado actual intocable).
- Los respaldos en `/tmp/` (respaldo_pre_v0.9.0, datos_antes_task89) se perdieron por una purga
  del sistema durante la sesión. El respaldo vivo que queda es
  `/sdcard/restore_pre_task89.json` (o `crear copia persistente`) — considerar respaldar el
  `datos.json` completo en un lugar fuera de `/tmp` y del teléfono.
