# Sistema de metas — plan de ejecución

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **En este proyecto** el ejecutor por defecto es **big pickle** (OpenCode). Cada tarea es autocontenida — big pickle no tiene contexto previo entre tareas. Todo lo necesario para ejecutar una tarea vive en su propia sección.

**Goal:** Reemplazar el `MetasFragment` stub con un sistema completo de metas de entrenamiento y composición corporal (4 tipos), con validación de plausibilidad basada en IMC/ICC, motor de evaluación automática al guardar registro/sesión, y recomendador de ejercicios híbrido (tabla curada + filtro por rutinas del usuario) que sugiere plan (volumen semanal + series/reps + esquema + peso inicial).

**Architecture:** Nueva entidad `Meta` en `DataStore.metas`, motor polimórfico `EvaluadorMeta` con 4 subclases (una por tipo), fachada `ValidadorMeta` con reglas basadas en `PerfilFisicoUtil`, y `RecomendadorEjercicios` que combina un `Map<String, List<String>>` hardcoded (nombres verificados contra `catalogo.json`) con conteo dinámico de ejercicios en las rutinas del usuario. Cinco diálogos nuevos, banner en editor de rutina, notificación push única al pasar de <80% a ≥80% de progreso, celebración automática al cumplir.

**Tech Stack:** Java 17, Android SDK 26–37, Material3, sin dependencias nuevas.

## Global Constraints

- **Dominio en español, API Android en inglés.**
- **Nada fuera de `data/` importa Gson/File/FileWriter/FileReader ni llama a `getFilesDir()`.** Chequeo:
  ```bash
  grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
  ```
  Debe seguir vacío.
- **Campos primitivos nuevos siempre seguros contra `datos.json` viejo** (Trampa #3). Colecciones nuevas también, pero deben normalizarse en `DataStore.normalizarColecciones()`.
- **Textos hardcodeados en Java y layouts.** `strings.xml` solo tiene `app_name`.
- **No introducir dependencias nuevas.**
- **`CATALOGO_VERSION_ACTUAL` no se toca.**
- **Nunca llamar `save()` desde `DataManager.load()`** (Trampa #4).
- **Entidades con identidad** heredan de `EntidadIdentificable`. IDs con `dataManager.newId(prefijo)`. Prefijo para `Meta` es `"m"`.
- **Fechas como `String` ISO** (`LocalDateTime.now().toString()`), nunca `Date`.
- **Activities con toolbar heredan de `BaseActivity`** y usan `configurarToolbar(...)`.
- **Modelos con campos privados** + getters/setters + constructor sin args privado para Gson + constructor público completo.
- **El código debe presentar estructura POO explícita** (requisito académico Móviles 2). El motor de metas usa polimorfismo real (`EvaluadorMeta` abstracta + 4 subclases) igual que `EstrategiaProgresion`.
- **JAVA_HOME antes de gradlew:**
  ```bash
  export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
  ```
- **Datos reales del usuario en el teléfono:** respaldar antes de instalar; nunca escribir sobre `files/datos.json` sin autorización explícita del usuario.
- **Commit messages** en español, imperativos, una línea.
- **Cuidado con pipes en gradlew** — `./gradlew ... | tail` devuelve exit de `tail`. Comprobar `${PIPESTATUS[0]}` o no usar pipe.

---

### Task 1: Modelo base — `Meta`, campo `Usuario.diasEntrenoSemana`, lista `DataStore.metas` + normalización

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/model/Meta.java`
- Modify: `app/src/main/java/com/ironquest/mvp/model/Usuario.java`
- Modify: `app/src/main/java/com/ironquest/mvp/model/DataStore.java`

**Interfaces:**
- Produces:
  - `Meta` entidad con constantes `TIPO_AUMENTAR_MEDIDA=0`, `TIPO_REDUCIR_MEDIDA=1`, `TIPO_PESO_CORPORAL=2`, `TIPO_PR_EJERCICIO=3`, `ESTADO_ACTIVA=0`, `ESTADO_CUMPLIDA=1`, `ESTADO_DESCARTADA=2`. Getters/setters de todos los campos listados abajo.
  - `Usuario.getDiasEntrenoSemana()` / `setDiasEntrenoSemana(int)`. Default `3`.
  - `DataStore.getMetas()` / `setMetas(List<Meta>)`.
  - `DataStore.normalizarColecciones()` cubre `metas`.

- [ ] **Step 1: Crear `Meta.java`**

```java
package com.ironquest.mvp.model;

/**
 * Meta de entrenamiento o composición corporal. Cuatro tipos: aumentar medida muscular,
 * reducir cintura/cadera, alcanzar peso corporal, PR en ejercicio. Se evalúa automáticamente
 * cada vez que el usuario guarda un RegistroFisico o finaliza una Sesion.
 */
public class Meta extends EntidadIdentificable {

    public static final int TIPO_AUMENTAR_MEDIDA = 0;
    public static final int TIPO_REDUCIR_MEDIDA  = 1;
    public static final int TIPO_PESO_CORPORAL   = 2;
    public static final int TIPO_PR_EJERCICIO    = 3;

    public static final int ESTADO_ACTIVA     = 0;
    public static final int ESTADO_CUMPLIDA   = 1;
    public static final int ESTADO_DESCARTADA = 2;

    private int tipo;
    private String titulo;
    private int estado;

    /** Nombre del campo en RegistroFisico ("brazoCm", "cinturaCm", ...). Solo tipos de medida. */
    private String medidaTipo;

    /** Solo para PR: referencia al Ejercicio principal. */
    private String ejercicioId;
    private double pesoObjetivoKg;
    private int repsObjetivo;

    /** Fotografía del valor al momento de crear la meta. */
    private double valorInicial;

    /** Objetivo numérico. Para tipos de medida y peso; ignorado para PR (que usa peso+reps). */
    private double valorObjetivo;

    private String fechaCreacion;
    private String fechaObjetivo;
    private String fechaCumplida;
    private String fechaDescartada;

    /** True si el usuario aceptó una advertencia del validador al crear. */
    private boolean advertenciaAceptada;

    /** True una vez que la notificación push de "≥80% de progreso" ya se disparó. No se resetea. */
    private boolean notificadaCerca;

    /** Constructor sin argumentos para Gson. */
    private Meta() {
    }

    public Meta(String id, int tipo, String titulo, String fechaCreacion) {
        super(id);
        this.tipo = tipo;
        this.titulo = titulo;
        this.estado = ESTADO_ACTIVA;
        this.fechaCreacion = fechaCreacion;
    }

    public int getTipo() { return tipo; }
    public void setTipo(int tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }

    public String getMedidaTipo() { return medidaTipo; }
    public void setMedidaTipo(String medidaTipo) { this.medidaTipo = medidaTipo; }

    public String getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(String ejercicioId) { this.ejercicioId = ejercicioId; }

    public double getPesoObjetivoKg() { return pesoObjetivoKg; }
    public void setPesoObjetivoKg(double pesoObjetivoKg) { this.pesoObjetivoKg = pesoObjetivoKg; }

    public int getRepsObjetivo() { return repsObjetivo; }
    public void setRepsObjetivo(int repsObjetivo) { this.repsObjetivo = repsObjetivo; }

    public double getValorInicial() { return valorInicial; }
    public void setValorInicial(double valorInicial) { this.valorInicial = valorInicial; }

    public double getValorObjetivo() { return valorObjetivo; }
    public void setValorObjetivo(double valorObjetivo) { this.valorObjetivo = valorObjetivo; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaObjetivo() { return fechaObjetivo; }
    public void setFechaObjetivo(String fechaObjetivo) { this.fechaObjetivo = fechaObjetivo; }

    public String getFechaCumplida() { return fechaCumplida; }
    public void setFechaCumplida(String fechaCumplida) { this.fechaCumplida = fechaCumplida; }

    public String getFechaDescartada() { return fechaDescartada; }
    public void setFechaDescartada(String fechaDescartada) { this.fechaDescartada = fechaDescartada; }

    public boolean isAdvertenciaAceptada() { return advertenciaAceptada; }
    public void setAdvertenciaAceptada(boolean advertenciaAceptada) { this.advertenciaAceptada = advertenciaAceptada; }

    public boolean isNotificadaCerca() { return notificadaCerca; }
    public void setNotificadaCerca(boolean notificadaCerca) { this.notificadaCerca = notificadaCerca; }
}
```

- [ ] **Step 2: Agregar campo `diasEntrenoSemana` en `Usuario.java`**

Agregar el campo (junto a los otros, después de `seguimientoFatiga`):

```java
/** Cuántos días a la semana entrena. Alimenta al recomendador para calcular
 *  volumen semanal esperado. Editable en Ajustes. Default 3. */
private int diasEntrenoSemana = 3;
```

Getter/setter estándar:

```java
public int getDiasEntrenoSemana() {
    return diasEntrenoSemana;
}

public void setDiasEntrenoSemana(int diasEntrenoSemana) {
    this.diasEntrenoSemana = diasEntrenoSemana;
}
```

- [ ] **Step 3: Agregar lista `metas` en `DataStore.java`**

Después del campo `historialFisico`:

```java
private List<Meta> metas = new ArrayList<>();
```

Getter/setter:

```java
public List<Meta> getMetas() {
    return metas;
}

public void setMetas(List<Meta> metas) {
    this.metas = metas;
}
```

En `normalizarColecciones()`, al final del método:

```java
if (metas == null) {
    metas = new ArrayList<>();
}
```

- [ ] **Step 4: Compilar y verificar invariante Gson**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
cd IronQuestApp
./gradlew assembleDebug
echo "Exit: ${PIPESTATUS[0]}"
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Esperado: `BUILD SUCCESSFUL` y grep vacío.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/model/
git commit -m "Modelar Meta y campos base del sistema de metas"
```

---

### Task 2: Métodos de `Meta` en `DataManager`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/data/DataManager.java`

**Interfaces:**
- Consumes: `Meta` y `DataStore.getMetas()` (Task 1).
- Produces:
  - `DataManager.getMetasActivas()` → `List<Meta>` (nueva ArrayList filtrada).
  - `DataManager.getMetasHistorial()` → `List<Meta>` con CUMPLIDA o DESCARTADA, ordenadas por fecha descendente.
  - `DataManager.agregarMeta(Meta)` → `boolean` (`false` si ya hay 3 activas).
  - `DataManager.descartarMeta(String metaId)` → marca DESCARTADA + fecha + `save()`.

- [ ] **Step 1: Agregar imports en `DataManager.java`**

Asegurar que estos imports estén (agregar los que falten):

```java
import com.ironquest.mvp.model.Meta;
import java.time.LocalDateTime;
import java.util.Comparator;
```

- [ ] **Step 2: Agregar métodos CRUD de metas**

Al final de la clase `DataManager`, antes de `private DataStore load() {`:

```java
public java.util.List<Meta> getMetasActivas() {
    java.util.List<Meta> resultado = new java.util.ArrayList<>();
    for (Meta m : dataStore.getMetas()) {
        if (m.getEstado() == Meta.ESTADO_ACTIVA) {
            resultado.add(m);
        }
    }
    return resultado;
}

public java.util.List<Meta> getMetasHistorial() {
    java.util.List<Meta> resultado = new java.util.ArrayList<>();
    for (Meta m : dataStore.getMetas()) {
        if (m.getEstado() == Meta.ESTADO_CUMPLIDA || m.getEstado() == Meta.ESTADO_DESCARTADA) {
            resultado.add(m);
        }
    }
    // Ordenar por fecha de cierre descendente (más reciente arriba). Fallback: fecha creación.
    resultado.sort((a, b) -> {
        String fechaA = a.getFechaCumplida() != null ? a.getFechaCumplida()
                : a.getFechaDescartada() != null ? a.getFechaDescartada()
                : a.getFechaCreacion();
        String fechaB = b.getFechaCumplida() != null ? b.getFechaCumplida()
                : b.getFechaDescartada() != null ? b.getFechaDescartada()
                : b.getFechaCreacion();
        return fechaB.compareTo(fechaA);
    });
    return resultado;
}

/** Agrega una meta si no se supera el límite de 3 activas. Devuelve false si se rechaza. */
public boolean agregarMeta(Meta meta) {
    if (getMetasActivas().size() >= 3) {
        return false;
    }
    dataStore.getMetas().add(meta);
    save();
    return true;
}

public void descartarMeta(String metaId) {
    if (metaId == null) return;
    for (Meta m : dataStore.getMetas()) {
        if (metaId.equals(m.getId()) && m.getEstado() == Meta.ESTADO_ACTIVA) {
            m.setEstado(Meta.ESTADO_DESCARTADA);
            m.setFechaDescartada(java.time.LocalDateTime.now().toString());
            save();
            return;
        }
    }
}
```

**Nota:** `evaluarMetas()` se agrega en la Task 3 (necesita `EvaluadorMeta`).

- [ ] **Step 3: Compilar y verificar**

```bash
./gradlew assembleDebug
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Esperado: `BUILD SUCCESSFUL` + grep vacío.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/data/DataManager.java
git commit -m "Añadir CRUD de Meta en DataManager con límite de 3 activas"
```

---

### Task 3: Motor de evaluación polimórfico + `evaluarMetas()` en `DataManager`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/EvaluadorMeta.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/EvalMedidaMuscular.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/EvalReducirMedida.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/EvalPesoCorporal.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/EvalPrEjercicio.java`
- Modify: `app/src/main/java/com/ironquest/mvp/data/DataManager.java`

**Interfaces:**
- Consumes: `Meta` (Task 1), `DataManager.save()` (existente).
- Produces:
  - `EvaluadorMeta.para(Meta)` → subclase correcta.
  - `EvaluadorMeta.calcularProgreso(Meta, DataStore)` → `double` en [0.0, 1.0].
  - `EvaluadorMeta.cumplida(Meta, DataStore)` → `boolean`.
  - `EvaluadorMeta.textoProgreso(Meta, DataStore)` → `String` para pintar en card.
  - `EvaluadorMeta.valorActual(Meta, DataStore)` → `double` (protected, usado en el motor y por los recordatorios).
  - `EvaluadorMeta.ultimoRegistroRelevante(Meta, DataStore)` → `String` fecha ISO o `null` — usado para el recordatorio pasivo "sin actualizar hace X días".
  - `DataManager.evaluarMetas()` → `List<Meta>` con las recién cumplidas.

- [ ] **Step 1: Crear `EvaluadorMeta.java` (abstracta)**

```java
package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;

/**
 * Contrato de evaluación de una Meta contra el estado actual del DataStore. Polimórfico por
 * tipo de meta (una subclase por cada constante TIPO_* de Meta). Sin estado propio; cada
 * llamada recibe la meta y el dataStore.
 */
public abstract class EvaluadorMeta {

    /** Instancia la subclase correcta según el tipo de meta. */
    public static EvaluadorMeta para(Meta meta) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA: return new EvalMedidaMuscular();
            case Meta.TIPO_REDUCIR_MEDIDA:  return new EvalReducirMedida();
            case Meta.TIPO_PESO_CORPORAL:   return new EvalPesoCorporal();
            case Meta.TIPO_PR_EJERCICIO:    return new EvalPrEjercicio();
            default:
                throw new IllegalArgumentException("Tipo de meta desconocido: " + meta.getTipo());
        }
    }

    /** Progreso 0.0 a 1.0 (clamped). */
    public abstract double calcularProgreso(Meta meta, DataStore ds);

    /** Si el estado actual del DataStore alcanza o supera el objetivo. */
    public abstract boolean cumplida(Meta meta, DataStore ds);

    /** Texto legible: "32 → 40 cm (20 %)" o "45 kg × 6 → objetivo 50 kg × 8". */
    public abstract String textoProgreso(Meta meta, DataStore ds);

    /** Valor "actual" que compara contra objetivo. Devuelve 0.0 si no hay dato. */
    protected abstract double valorActual(Meta meta, DataStore ds);

    /**
     * Fecha ISO del último dato relevante (último RegistroFisico con la medida, o última
     * sesión con el ejercicio de la meta). Devuelve null si no hay ninguno. La UI usa esto
     * para el banner "actualiza tu {medida}" cuando pasó > 14 días.
     */
    public abstract String ultimoRegistroRelevante(Meta meta, DataStore ds);

    // ---- helpers compartidos ----

    protected static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    protected static String formatearPorcentaje(double frac) {
        return ((int) Math.round(frac * 100)) + " %";
    }

    protected static String formatearNumero(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}
```

- [ ] **Step 2: Crear `EvalMedidaMuscular.java`**

```java
package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;

import java.util.List;

/** Meta AUMENTAR_MEDIDA (brazo/pierna/pecho). */
final class EvalMedidaMuscular extends EvaluadorMeta {

    @Override
    public double calcularProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        double inicial = meta.getValorInicial();
        double objetivo = meta.getValorObjetivo();
        if (objetivo <= inicial) return actual >= objetivo ? 1.0 : 0.0;
        return clamp01((actual - inicial) / (objetivo - inicial));
    }

    @Override
    public boolean cumplida(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return actual > 0 && actual >= meta.getValorObjetivo();
    }

    @Override
    public String textoProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return formatearNumero(actual) + " → " + formatearNumero(meta.getValorObjetivo())
                + " cm (" + formatearPorcentaje(calcularProgreso(meta, ds)) + ")";
    }

    @Override
    protected double valorActual(Meta meta, DataStore ds) {
        RegistroFisico ultimo = ultimoRegistroConMedida(ds, meta.getMedidaTipo());
        return ultimo != null ? leerMedida(ultimo, meta.getMedidaTipo()) : 0.0;
    }

    @Override
    public String ultimoRegistroRelevante(Meta meta, DataStore ds) {
        RegistroFisico r = ultimoRegistroConMedida(ds, meta.getMedidaTipo());
        return r != null ? r.getFecha() : null;
    }

    private static RegistroFisico ultimoRegistroConMedida(DataStore ds, String medidaTipo) {
        List<RegistroFisico> lista = ds.getHistorialFisico();
        for (int i = lista.size() - 1; i >= 0; i--) {
            RegistroFisico r = lista.get(i);
            if (leerMedida(r, medidaTipo) > 0) return r;
        }
        return null;
    }

    static double leerMedida(RegistroFisico r, String medidaTipo) {
        if (medidaTipo == null) return 0.0;
        switch (medidaTipo) {
            case "brazoCm":   return r.getBrazoCm();
            case "piernaCm":  return r.getPiernaCm();
            case "pechoCm":   return r.getPechoCm();
            case "cinturaCm": return r.getCinturaCm();
            case "caderaCm":  return r.getCaderaCm();
            default: return 0.0;
        }
    }
}
```

- [ ] **Step 3: Crear `EvalReducirMedida.java`**

```java
package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;

import java.util.List;

/** Meta REDUCIR_MEDIDA (cintura/cadera). Progreso invertido: bajar es avanzar. */
final class EvalReducirMedida extends EvaluadorMeta {

    @Override
    public double calcularProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        double inicial = meta.getValorInicial();
        double objetivo = meta.getValorObjetivo();
        if (inicial <= objetivo || actual <= 0) return actual > 0 && actual <= objetivo ? 1.0 : 0.0;
        return clamp01((inicial - actual) / (inicial - objetivo));
    }

    @Override
    public boolean cumplida(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return actual > 0 && actual <= meta.getValorObjetivo();
    }

    @Override
    public String textoProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return formatearNumero(actual) + " → " + formatearNumero(meta.getValorObjetivo())
                + " cm (" + formatearPorcentaje(calcularProgreso(meta, ds)) + ")";
    }

    @Override
    protected double valorActual(Meta meta, DataStore ds) {
        RegistroFisico ultimo = ultimoRegistroConMedida(ds, meta.getMedidaTipo());
        return ultimo != null ? EvalMedidaMuscular.leerMedida(ultimo, meta.getMedidaTipo()) : 0.0;
    }

    @Override
    public String ultimoRegistroRelevante(Meta meta, DataStore ds) {
        RegistroFisico r = ultimoRegistroConMedida(ds, meta.getMedidaTipo());
        return r != null ? r.getFecha() : null;
    }

    private static RegistroFisico ultimoRegistroConMedida(DataStore ds, String medidaTipo) {
        List<RegistroFisico> lista = ds.getHistorialFisico();
        for (int i = lista.size() - 1; i >= 0; i--) {
            RegistroFisico r = lista.get(i);
            if (EvalMedidaMuscular.leerMedida(r, medidaTipo) > 0) return r;
        }
        return null;
    }
}
```

- [ ] **Step 4: Crear `EvalPesoCorporal.java`**

```java
package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;

import java.util.List;

/** Meta PESO_CORPORAL. Dirección se detecta por (objetivo - inicial). */
final class EvalPesoCorporal extends EvaluadorMeta {

    @Override
    public double calcularProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        double inicial = meta.getValorInicial();
        double objetivo = meta.getValorObjetivo();
        if (actual <= 0) return 0.0;
        double denom = objetivo - inicial;
        if (denom == 0) return actual == objetivo ? 1.0 : 0.0;
        return clamp01((actual - inicial) / denom);
    }

    @Override
    public boolean cumplida(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        if (actual <= 0) return false;
        boolean subir = meta.getValorObjetivo() > meta.getValorInicial();
        return subir ? actual >= meta.getValorObjetivo() : actual <= meta.getValorObjetivo();
    }

    @Override
    public String textoProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return formatearNumero(actual) + " → " + formatearNumero(meta.getValorObjetivo())
                + " kg (" + formatearPorcentaje(calcularProgreso(meta, ds)) + ")";
    }

    @Override
    protected double valorActual(Meta meta, DataStore ds) {
        RegistroFisico r = ultimoConPeso(ds);
        return r != null ? r.getPesoKg() : 0.0;
    }

    @Override
    public String ultimoRegistroRelevante(Meta meta, DataStore ds) {
        RegistroFisico r = ultimoConPeso(ds);
        return r != null ? r.getFecha() : null;
    }

    private static RegistroFisico ultimoConPeso(DataStore ds) {
        List<RegistroFisico> lista = ds.getHistorialFisico();
        for (int i = lista.size() - 1; i >= 0; i--) {
            if (lista.get(i).getPesoKg() > 0) return lista.get(i);
        }
        return null;
    }
}
```

- [ ] **Step 5: Crear `EvalPrEjercicio.java`**

```java
package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;

import java.util.List;

/**
 * Meta PR_EJERCICIO. Cumplida cuando alguna serie completada del historial iguala o supera
 * simultáneamente peso y reps objetivo. Progreso = mejor score actual / score objetivo,
 * donde score = peso × reps.
 */
final class EvalPrEjercicio extends EvaluadorMeta {

    @Override
    public double calcularProgreso(Meta meta, DataStore ds) {
        double scoreObjetivo = meta.getPesoObjetivoKg() * meta.getRepsObjetivo();
        if (scoreObjetivo <= 0) return 0.0;
        double mejorActual = mejorScore(meta, ds);
        return clamp01(mejorActual / scoreObjetivo);
    }

    @Override
    public boolean cumplida(Meta meta, DataStore ds) {
        for (Sesion s : ds.getSesiones()) {
            if (!s.estaFinalizada()) continue;
            EjercicioSesion es = s.buscarEjercicio(meta.getEjercicioId());
            if (es == null) continue;
            for (SerieSesion serie : es.getSeries()) {
                if (!serie.isCompletada()) continue;
                if (serie.getPeso() >= meta.getPesoObjetivoKg()
                        && serie.getRepeticiones() >= meta.getRepsObjetivo()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String textoProgreso(Meta meta, DataStore ds) {
        double mejor = mejorScore(meta, ds);
        double scoreObj = meta.getPesoObjetivoKg() * meta.getRepsObjetivo();
        return "Mejor actual: " + formatearNumero(mejor)
                + " · Objetivo: " + formatearNumero(scoreObj)
                + " (" + formatearPorcentaje(calcularProgreso(meta, ds)) + ")";
    }

    @Override
    protected double valorActual(Meta meta, DataStore ds) {
        return mejorScore(meta, ds);
    }

    @Override
    public String ultimoRegistroRelevante(Meta meta, DataStore ds) {
        List<Sesion> sesiones = ds.getSesiones();
        for (int i = sesiones.size() - 1; i >= 0; i--) {
            Sesion s = sesiones.get(i);
            if (!s.estaFinalizada()) continue;
            if (s.buscarEjercicio(meta.getEjercicioId()) != null) {
                return s.getFechaHoraInicio();
            }
        }
        return null;
    }

    private static double mejorScore(Meta meta, DataStore ds) {
        double mejor = 0.0;
        for (Sesion s : ds.getSesiones()) {
            if (!s.estaFinalizada()) continue;
            EjercicioSesion es = s.buscarEjercicio(meta.getEjercicioId());
            if (es == null) continue;
            for (SerieSesion serie : es.getSeries()) {
                if (!serie.isCompletada()) continue;
                double score = serie.getPeso() * serie.getRepeticiones();
                if (score > mejor) mejor = score;
            }
        }
        return mejor;
    }
}
```

- [ ] **Step 6: Agregar `evaluarMetas()` en `DataManager.java`**

Después del método `descartarMeta` de la Task 2:

```java
/**
 * Recorre metas activas: marca cumplidas y devuelve las recién cumplidas (para que la UI
 * muestre celebración). También dispara notificación push si alguna cruzó el 80% de progreso
 * por primera vez. Un solo save() al final si hubo cambios.
 */
public java.util.List<Meta> evaluarMetas() {
    java.util.List<Meta> recienCumplidas = new java.util.ArrayList<>();
    boolean cambio = false;
    for (Meta m : dataStore.getMetas()) {
        if (m.getEstado() != Meta.ESTADO_ACTIVA) continue;
        com.ironquest.mvp.util.metas.EvaluadorMeta ev =
                com.ironquest.mvp.util.metas.EvaluadorMeta.para(m);
        if (ev.cumplida(m, dataStore)) {
            m.setEstado(Meta.ESTADO_CUMPLIDA);
            m.setFechaCumplida(java.time.LocalDateTime.now().toString());
            recienCumplidas.add(m);
            cambio = true;
        } else if (!m.isNotificadaCerca() && ev.calcularProgreso(m, dataStore) >= 0.80) {
            m.setNotificadaCerca(true);
            com.ironquest.mvp.service.NotificacionMetasHelper.notificarCerca(appContext, m);
            cambio = true;
        }
    }
    if (cambio) save();
    return recienCumplidas;
}
```

**Nota:** `NotificacionMetasHelper.notificarCerca(...)` se crea en la Task 12. Este método referencia el símbolo antes de que exista → compila con error hasta Task 12. **Es aceptable si big pickle sigue el orden secuencial**; alternativamente puede stubearse temporalmente:

```java
// Stub temporal hasta Task 12 — reemplazar por la llamada real:
// com.ironquest.mvp.service.NotificacionMetasHelper.notificarCerca(appContext, m);
```

Recomendación: **saltarse la línea de notificación en esta task** (comentarla) y descomentarla en la Task 12 cuando el helper exista.

- [ ] **Step 7: Compilar**

```bash
./gradlew assembleDebug
```

Si aparece error por `NotificacionMetasHelper`, comentar temporalmente esa línea.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/util/metas/ app/src/main/java/com/ironquest/mvp/data/DataManager.java
git commit -m "Añadir motor polimórfico de evaluación de metas y hook en DataManager"
```

---

### Task 4: Validador de metas + `Advertencia`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/Advertencia.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/ValidadorMeta.java`

**Interfaces:**
- Consumes: `Meta` (Task 1), `EvaluadorMeta.valorActual` (Task 3 — accesible internamente porque el validador está en el mismo paquete), `PerfilFisicoUtil.calcularImc()` y `calcularIcc()` existentes.
- Produces:
  - `Advertencia` valor inmutable con `texto` y constantes de severidad (aunque solo se usa una severidad por ahora, dejar la clase preparada).
  - `ValidadorMeta.validar(Meta, DataStore)` → `Advertencia` o `null`.

- [ ] **Step 1: Crear `Advertencia.java`**

```java
package com.ironquest.mvp.util.metas;

/** Advertencia opcional que devuelve ValidadorMeta al crear una Meta. Inmutable. */
public final class Advertencia {

    public static final int SEVERIDAD_INFO = 0;
    public static final int SEVERIDAD_ATENCION = 1;

    private final String texto;
    private final int severidad;

    public Advertencia(String texto, int severidad) {
        this.texto = texto;
        this.severidad = severidad;
    }

    public String getTexto() { return texto; }
    public int getSeveridad() { return severidad; }
}
```

- [ ] **Step 2: Crear `ValidadorMeta.java`**

```java
package com.ironquest.mvp.util.metas;

import androidx.annotation.Nullable;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.Usuario;
import com.ironquest.mvp.util.PerfilFisicoUtil;

import java.util.List;

/**
 * Valida al crear una Meta. Nunca bloquea, solo advierte. El diálogo de crear muestra el
 * texto y el usuario decide entre Ajustar y Crear igual.
 */
public final class ValidadorMeta {

    private static final double DELTA_AGRESIVO_MEDIDA_AUMENTAR = 0.20; // >20% del valor actual
    private static final double DELTA_AGRESIVO_MEDIDA_REDUCIR  = 0.15;
    private static final double DELTA_AGRESIVO_PESO            = 0.15;
    private static final double DELTA_AGRESIVO_PR              = 0.20;
    private static final double IMC_MIN_SANO                   = 18.5;
    private static final double IMC_MAX_SANO                   = 30.0;
    private static final double ICC_NEUTRO_MIN                 = 0.72;

    private ValidadorMeta() {}

    public static @Nullable Advertencia validar(Meta meta, DataStore ds) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA: return validarAumentarMedida(meta, ds);
            case Meta.TIPO_REDUCIR_MEDIDA:  return validarReducirMedida(meta, ds);
            case Meta.TIPO_PESO_CORPORAL:   return validarPesoCorporal(meta, ds);
            case Meta.TIPO_PR_EJERCICIO:    return validarPrEjercicio(meta, ds);
            default: return null;
        }
    }

    private static Advertencia validarAumentarMedida(Meta meta, DataStore ds) {
        double actual = leerMedidaActual(ds, meta.getMedidaTipo());
        if (actual <= 0) return null;
        double delta = (meta.getValorObjetivo() - actual) / actual;
        if (delta > DELTA_AGRESIVO_MEDIDA_AUMENTAR) {
            int porc = (int) Math.round(delta * 100);
            return new Advertencia(
                    "Aumentar tu " + nombreLegibleMedida(meta.getMedidaTipo())
                            + " de " + formatCm(actual) + " a " + formatCm(meta.getValorObjetivo())
                            + " cm es un salto de " + porc + " %. Un aumento realista suele ser de "
                            + "1-2 cm por mes con entrenamiento constante.",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        return null;
    }

    private static Advertencia validarReducirMedida(Meta meta, DataStore ds) {
        double actual = leerMedidaActual(ds, meta.getMedidaTipo());
        if (actual <= 0) return null;

        // Regla ICC: si la meta es cintura y llevaría a ICC < 0.72
        if ("cinturaCm".equals(meta.getMedidaTipo())) {
            RegistroFisico ultimo = ultimoRegistro(ds);
            if (ultimo != null && ultimo.getCaderaCm() > 0) {
                double iccPropuesto = meta.getValorObjetivo() / ultimo.getCaderaCm();
                if (iccPropuesto < ICC_NEUTRO_MIN) {
                    return new Advertencia(
                            "Ese objetivo te llevaría a un índice cintura-cadera muy bajo ("
                                    + String.format("%.2f", iccPropuesto)
                                    + "). Considera un valor más moderado.",
                            Advertencia.SEVERIDAD_ATENCION);
                }
            }
        }

        // Regla delta relativo
        double delta = (actual - meta.getValorObjetivo()) / actual;
        if (delta > DELTA_AGRESIVO_MEDIDA_REDUCIR) {
            int porc = (int) Math.round(delta * 100);
            return new Advertencia(
                    "Una reducción de " + porc + " % de tu " + nombreLegibleMedida(meta.getMedidaTipo())
                            + " es agresiva. Considera dividirla en metas más pequeñas.",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        return null;
    }

    private static Advertencia validarPesoCorporal(Meta meta, DataStore ds) {
        RegistroFisico ultimo = ultimoRegistro(ds);
        double actual = ultimo != null ? ultimo.getPesoKg() : 0;
        double altura = ultimo != null ? ultimo.getAlturaCm() : 0;
        if (actual <= 0 || altura <= 0) return null;

        double imcObj = PerfilFisicoUtil.calcularImc(meta.getValorObjetivo(), altura);
        if (imcObj < IMC_MIN_SANO) {
            return new Advertencia(
                    "Ese peso te dejaría en bajo peso (IMC " + String.format("%.1f", imcObj)
                            + "). Es riesgoso para tu salud.",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        if (imcObj > IMC_MAX_SANO) {
            return new Advertencia(
                    "Ese peso te dejaría en rango de obesidad (IMC "
                            + String.format("%.1f", imcObj) + ").",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        double delta = Math.abs(meta.getValorObjetivo() - actual) / actual;
        if (delta > DELTA_AGRESIVO_PESO) {
            int porc = (int) Math.round(delta * 100);
            return new Advertencia(
                    "Un cambio de " + porc + " % de tu peso corporal es agresivo. "
                            + "Considera dividirlo en metas más pequeñas.",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        return null;
    }

    private static Advertencia validarPrEjercicio(Meta meta, DataStore ds) {
        double mejorPeso = mejorPesoRegistrado(ds, meta.getEjercicioId(), meta.getRepsObjetivo());
        if (mejorPeso <= 0) {
            return new Advertencia(
                    "No tenemos registros previos de este ejercicio. La meta se crea, pero no "
                            + "podremos comparar contra un PR anterior.",
                    Advertencia.SEVERIDAD_INFO);
        }
        double delta = (meta.getPesoObjetivoKg() - mejorPeso) / mejorPeso;
        if (delta > DELTA_AGRESIVO_PR) {
            int porc = (int) Math.round(delta * 100);
            return new Advertencia(
                    "Subir " + formatKg(mejorPeso) + " → " + formatKg(meta.getPesoObjetivoKg())
                            + " kg es un salto de " + porc + " %. Considera dividirlo (ej. primero "
                            + formatKg(mejorPeso + 5) + " kg × " + meta.getRepsObjetivo() + ").",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        return null;
    }

    // ---- helpers ----

    private static double leerMedidaActual(DataStore ds, String medidaTipo) {
        RegistroFisico r = ultimoRegistro(ds);
        if (r == null || medidaTipo == null) return 0;
        switch (medidaTipo) {
            case "brazoCm":   return r.getBrazoCm();
            case "piernaCm":  return r.getPiernaCm();
            case "pechoCm":   return r.getPechoCm();
            case "cinturaCm": return r.getCinturaCm();
            case "caderaCm":  return r.getCaderaCm();
            default: return 0;
        }
    }

    private static RegistroFisico ultimoRegistro(DataStore ds) {
        List<RegistroFisico> h = ds.getHistorialFisico();
        return h.isEmpty() ? null : h.get(h.size() - 1);
    }

    /** Mejor peso alcanzado con al menos `repsMinimas` reps completadas. */
    private static double mejorPesoRegistrado(DataStore ds, String ejercicioId, int repsMinimas) {
        double mejor = 0;
        for (Sesion s : ds.getSesiones()) {
            if (!s.estaFinalizada()) continue;
            EjercicioSesion es = s.buscarEjercicio(ejercicioId);
            if (es == null) continue;
            for (SerieSesion serie : es.getSeries()) {
                if (!serie.isCompletada()) continue;
                if (serie.getRepeticiones() >= repsMinimas && serie.getPeso() > mejor) {
                    mejor = serie.getPeso();
                }
            }
        }
        return mejor;
    }

    private static String nombreLegibleMedida(String medidaTipo) {
        if (medidaTipo == null) return "medida";
        switch (medidaTipo) {
            case "brazoCm":   return "brazo";
            case "piernaCm":  return "pierna";
            case "pechoCm":   return "pecho";
            case "cinturaCm": return "cintura";
            case "caderaCm":  return "cadera";
            default: return "medida";
        }
    }

    private static String formatCm(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }

    private static String formatKg(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }
}
```

- [ ] **Step 3: Compilar**

```bash
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/util/metas/Advertencia.java app/src/main/java/com/ironquest/mvp/util/metas/ValidadorMeta.java
git commit -m "Añadir ValidadorMeta con reglas basadas en IMC, ICC y deltas relativos"
```

---

### Task 5: Recomendador de ejercicios + `PlanRecomendado` + `Recomendacion` + `ParamsProgresion`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/ParamsProgresion.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/Recomendacion.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/PlanRecomendado.java`
- Create: `app/src/main/java/com/ironquest/mvp/util/metas/RecomendadorEjercicios.java`

**Interfaces:**
- Consumes: `Meta`, `DataStore`, `Ejercicio`, `Rutina`, `RutinaEjercicio`, `Sesion`, `EjercicioSesion`, `SerieSesion`, `Usuario.getDiasEntrenoSemana()` (Task 1), `RutinaEjercicio.ESQUEMA_*` (existentes).
- Produces:
  - `RecomendadorEjercicios.recomendar(Meta, DataStore)` → `PlanRecomendado`.
  - `RecomendadorEjercicios.paramsPara(Meta)` → `ParamsProgresion`.
  - `RecomendadorEjercicios.pesoInicialSugerido(String ejercicioId, java.util.List<Sesion>)` → `double`.
  - `PlanRecomendado` valor con `textoVolumen`, `volumenSemanalActual`, `volumenObjetivoMin/Max`, `seriesSugeridas`, `repsSugeridas`, `repsSugeridasMax`, `esquemaProgresionSugerido`, `ejercicios` (List<Recomendacion>), `textoCoberturaCompleta` (nullable).
  - `Recomendacion` valor con `ejercicio` (Ejercicio), `etiqueta` (String), `prioridad` (int).

- [ ] **Step 1: Crear `ParamsProgresion.java`**

```java
package com.ironquest.mvp.util.metas;

/** Parámetros de entrenamiento sugeridos según el tipo de meta. Valor inmutable. */
public final class ParamsProgresion {
    public final int series;
    public final int repsMin;
    public final int repsMax;         // si == repsMin, no aplica rango
    public final int esquemaProgresion;

    public ParamsProgresion(int series, int repsMin, int repsMax, int esquemaProgresion) {
        this.series = series;
        this.repsMin = repsMin;
        this.repsMax = repsMax;
        this.esquemaProgresion = esquemaProgresion;
    }
}
```

- [ ] **Step 2: Crear `Recomendacion.java`**

```java
package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.Ejercicio;

/** Un ejercicio candidato con etiqueta y prioridad para el PlanSugeridoDialog. Inmutable. */
public final class Recomendacion {
    private final Ejercicio ejercicio;
    private final String etiqueta;
    private final int prioridad;   // 0 = falta en rutinas, 1 = en 1 rutina, 2 = en 2+ rutinas

    public Recomendacion(Ejercicio ejercicio, String etiqueta, int prioridad) {
        this.ejercicio = ejercicio;
        this.etiqueta = etiqueta;
        this.prioridad = prioridad;
    }

    public Ejercicio getEjercicio() { return ejercicio; }
    public String getEtiqueta() { return etiqueta; }
    public int getPrioridad() { return prioridad; }
}
```

- [ ] **Step 3: Crear `PlanRecomendado.java`**

```java
package com.ironquest.mvp.util.metas;

import java.util.List;

/** Salida del RecomendadorEjercicios. Valor inmutable. */
public final class PlanRecomendado {

    private final String textoVolumen;
    private final int volumenSemanalActual;
    private final int volumenObjetivoMin;
    private final int volumenObjetivoMax;
    private final int seriesSugeridas;
    private final int repsSugeridas;
    private final int repsSugeridasMax;
    private final int esquemaProgresionSugerido;
    private final List<Recomendacion> ejercicios;
    private final String textoCoberturaCompleta;   // null si hay al menos un ejercicio faltante

    public PlanRecomendado(String textoVolumen, int volumenSemanalActual,
                            int volumenObjetivoMin, int volumenObjetivoMax,
                            int seriesSugeridas, int repsSugeridas, int repsSugeridasMax,
                            int esquemaProgresionSugerido,
                            List<Recomendacion> ejercicios, String textoCoberturaCompleta) {
        this.textoVolumen = textoVolumen;
        this.volumenSemanalActual = volumenSemanalActual;
        this.volumenObjetivoMin = volumenObjetivoMin;
        this.volumenObjetivoMax = volumenObjetivoMax;
        this.seriesSugeridas = seriesSugeridas;
        this.repsSugeridas = repsSugeridas;
        this.repsSugeridasMax = repsSugeridasMax;
        this.esquemaProgresionSugerido = esquemaProgresionSugerido;
        this.ejercicios = ejercicios;
        this.textoCoberturaCompleta = textoCoberturaCompleta;
    }

    public String getTextoVolumen() { return textoVolumen; }
    public int getVolumenSemanalActual() { return volumenSemanalActual; }
    public int getVolumenObjetivoMin() { return volumenObjetivoMin; }
    public int getVolumenObjetivoMax() { return volumenObjetivoMax; }
    public int getSeriesSugeridas() { return seriesSugeridas; }
    public int getRepsSugeridas() { return repsSugeridas; }
    public int getRepsSugeridasMax() { return repsSugeridasMax; }
    public int getEsquemaProgresionSugerido() { return esquemaProgresionSugerido; }
    public List<Recomendacion> getEjercicios() { return ejercicios; }
    public String getTextoCoberturaCompleta() { return textoCoberturaCompleta; }

    public boolean tieneEjerciciosSugeridos() {
        return ejercicios != null && !ejercicios.isEmpty();
    }

    public boolean tieneAlgunoFaltante() {
        if (ejercicios == null) return false;
        for (Recomendacion r : ejercicios) if (r.getPrioridad() == 0) return true;
        return false;
    }
}
```

- [ ] **Step 4: Crear `RecomendadorEjercicios.java`** (archivo grande, cópialo tal cual)

```java
package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recomendador híbrido: tabla curada de ejercicios por tipo de meta + filtro dinámico por
 * lo que el usuario ya tiene en sus rutinas + parámetros de entrenamiento sugeridos.
 *
 * <p>Los 60 nombres de la tabla CURADOS fueron verificados contra app/src/main/assets/
 * catalogo.json — todos existen literalmente.
 */
public final class RecomendadorEjercicios {

    private static final Map<String, List<String>> CURADOS = new LinkedHashMap<>();
    static {
        CURADOS.put("aumentar_brazoCm", Arrays.asList(
                "Curl de bíceps con barra recta",
                "Curl predicador con barra",
                "Curl martillo en polea con cuerda",
                "Extensión de tríceps acostado agarre cerrado con barra",
                "Patada de tríceps en polea",
                "Fondos de codo"));
        CURADOS.put("aumentar_piernaCm", Arrays.asList(
                "Sentadilla completa con barra",
                "Prensa de piernas a 45°",
                "Extensión de cuádriceps en máquina",
                "Curl femoral acostado en máquina",
                "Peso muerto rumano con barra",
                "Zancada con barra"));
        CURADOS.put("aumentar_pechoCm", Arrays.asList(
                "Press de banca con barra",
                "Press inclinado con barra",
                "Press banca inclinado con mancuerna",
                "Aperturas con mancuernas en banco plano",
                "Fondos de pecho"));
        CURADOS.put("reducir_cinturaCm", Arrays.asList(
                "Plancha lateral inclinada",
                "Elevación de piernas colgado",
                "Abdominal con press y barra",
                "Crunch bicicleta con banda",
                "Sentadilla completa con barra",
                "Peso muerto con barra",
                "Fondos de pecho"));
        CURADOS.put("reducir_caderaCm", Arrays.asList(
                "Sentadilla completa con barra",
                "Zancada con barra",
                "Peso muerto rumano con barra",
                "Puente de glúteos con barra",
                "Elevación de cadera acostado con barra"));
        CURADOS.put("peso_bajar", Arrays.asList(
                "Sentadilla completa con barra",
                "Peso muerto con barra",
                "Press de banca con barra",
                "Remo con barra inclinado",
                "Jalón al pecho en polea",
                "Fondos de pecho"));
        CURADOS.put("peso_subir", Arrays.asList(
                "Sentadilla completa con barra",
                "Peso muerto con barra",
                "Press de banca con barra",
                "Press militar sentado con barra",
                "Remo con barra inclinado",
                "Jalón al pecho en polea"));
        CURADOS.put("pr_press_banca", Arrays.asList(
                "Press de banca agarre cerrado con barra",
                "Extensión de tríceps acostado agarre cerrado con barra",
                "Press JM con barra",
                "Press inclinado con barra",
                "Aperturas con mancuernas en banco plano"));
        CURADOS.put("pr_sentadilla", Arrays.asList(
                "Sentadilla frontal con barra",
                "Prensa de piernas a 45°",
                "Extensión de cuádriceps en máquina",
                "Zancada con barra",
                "Peso muerto rumano con barra"));
        CURADOS.put("pr_peso_muerto", Arrays.asList(
                "Peso muerto rumano con barra",
                "Buenos días con barra",
                "Puente de glúteos con barra",
                "Remo con barra inclinado",
                "Hiperextensión lumbar"));
        CURADOS.put("pr_dominadas", Arrays.asList(
                "Remo con barra inclinado",
                "Jalón al pecho en polea",
                "Curl de bíceps con barra recta",
                "Pullover con barra"));
    }

    /** Rangos MEV–MAV (mínimo efectivo, máximo adaptativo) de series por semana por grupo. */
    private static final Map<String, int[]> RANGO_VOLUMEN = new HashMap<>();
    static {
        RANGO_VOLUMEN.put("Pectorales",           new int[]{10, 16});
        RANGO_VOLUMEN.put("Dorsales",             new int[]{10, 16});
        RANGO_VOLUMEN.put("Cuádriceps",           new int[]{10, 16});
        RANGO_VOLUMEN.put("Espalda alta",         new int[]{8, 14});
        RANGO_VOLUMEN.put("Hombros",              new int[]{8, 14});
        RANGO_VOLUMEN.put("Bíceps",               new int[]{8, 14});
        RANGO_VOLUMEN.put("Tríceps",              new int[]{8, 14});
        RANGO_VOLUMEN.put("Pantorrillas",         new int[]{8, 14});
        RANGO_VOLUMEN.put("Abdomen",              new int[]{8, 14});
        RANGO_VOLUMEN.put("Zona lumbar",          new int[]{6, 12});
        RANGO_VOLUMEN.put("Glúteos",              new int[]{6, 12});
        RANGO_VOLUMEN.put("Isquiotibiales",       new int[]{6, 12});
        RANGO_VOLUMEN.put("Trapecios",            new int[]{6, 12});
        RANGO_VOLUMEN.put("Aductores/Abductores", new int[]{6, 10});
        RANGO_VOLUMEN.put("Antebrazos",           new int[]{4, 10});
    }

    private RecomendadorEjercicios() {}

    // ---------- API pública ----------

    public static PlanRecomendado recomendar(Meta meta, DataStore ds) {
        String clave = construirClave(meta, ds);
        String grupo = grupoDeMeta(meta, ds);

        List<String> nombresCurados = clave != null
                ? CURADOS.getOrDefault(clave, Collections.emptyList())
                : Collections.emptyList();
        List<Ejercicio> candidatos = resolverNombres(nombresCurados, ds.getEjercicios());
        if (candidatos.isEmpty() && meta.getTipo() == Meta.TIPO_PR_EJERCICIO && grupo != null) {
            candidatos = fallbackPorMusculo(grupo, ds.getEjercicios());
        }

        Map<String, Integer> conteos = contarEnRutinas(ds.getRutinas());

        List<Recomendacion> resultado = new ArrayList<>();
        for (Ejercicio e : candidatos) {
            int veces = conteos.getOrDefault(e.getId(), 0);
            String etiqueta = veces == 0 ? "Falta en tus rutinas"
                    : veces == 1 ? "Ya lo haces en 1 rutina"
                    : "Ya lo haces en " + veces + " rutinas";
            int prioridad = veces == 0 ? 0 : (veces == 1 ? 1 : 2);
            resultado.add(new Recomendacion(e, etiqueta, prioridad));
        }
        resultado.sort(Comparator.comparingInt(Recomendacion::getPrioridad));

        int diasSemana = (ds.getUsuario() != null && ds.getUsuario().getDiasEntrenoSemana() > 0)
                ? ds.getUsuario().getDiasEntrenoSemana() : 3;
        Map<String, Ejercicio> catalogo = indexarCatalogo(ds.getEjercicios());
        int volActual = grupo != null
                ? seriesSemanalesDeGrupo(grupo, ds.getRutinas(), catalogo, diasSemana) : 0;
        int[] rango = grupo != null
                ? RANGO_VOLUMEN.getOrDefault(grupo, new int[]{8, 14})
                : new int[]{0, 0};
        String textoVolumen = grupo != null
                ? "Para " + grupo + ": " + rango[0] + "-" + rango[1]
                    + " series/semana. Actualmente tienes " + volActual + "."
                : "Sin cálculo de volumen para este tipo de meta.";

        ParamsProgresion params = paramsPara(meta);

        String textoCobertura = null;
        if (!resultado.isEmpty() && resultado.stream().allMatch(r -> r.getPrioridad() > 0)) {
            textoCobertura = "Ya tienes buena cobertura de este grupo en tus rutinas. "
                    + "Considera aumentar el volumen (más series o más peso) antes de agregar más ejercicios.";
        }

        return new PlanRecomendado(textoVolumen, volActual, rango[0], rango[1],
                params.series, params.repsMin, params.repsMax, params.esquemaProgresion,
                resultado, textoCobertura);
    }

    public static ParamsProgresion paramsPara(Meta meta) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
                return new ParamsProgresion(3, 8, 12, RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION);
            case Meta.TIPO_REDUCIR_MEDIDA:
                return new ParamsProgresion(3, 15, 20, RutinaEjercicio.ESQUEMA_LINEAL);
            case Meta.TIPO_PESO_CORPORAL:
                boolean subir = meta.getValorObjetivo() > meta.getValorInicial();
                return subir
                        ? new ParamsProgresion(3, 8, 10, RutinaEjercicio.ESQUEMA_AUTOMATICO)
                        : new ParamsProgresion(3, 15, 20, RutinaEjercicio.ESQUEMA_LINEAL);
            case Meta.TIPO_PR_EJERCICIO:
                return new ParamsProgresion(4, 5, 6, RutinaEjercicio.ESQUEMA_GREYSKULL);
            default:
                return new ParamsProgresion(3, 8, 12, RutinaEjercicio.ESQUEMA_AUTOMATICO);
        }
    }

    public static double pesoInicialSugerido(String ejercicioId, List<Sesion> sesiones) {
        if (ejercicioId == null || sesiones == null) return 0.0;
        for (int i = sesiones.size() - 1; i >= 0; i--) {
            Sesion s = sesiones.get(i);
            if (!s.estaFinalizada()) continue;
            EjercicioSesion es = s.buscarEjercicio(ejercicioId);
            if (es == null) continue;
            SerieSesion ultima = es.getUltimaSerieCompletada();
            if (ultima != null) return ultima.getPeso();
        }
        return 0.0;
    }

    // ---------- Helpers ----------

    private static String construirClave(Meta meta, DataStore ds) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
                return "aumentar_" + meta.getMedidaTipo();
            case Meta.TIPO_REDUCIR_MEDIDA:
                return "reducir_" + meta.getMedidaTipo();
            case Meta.TIPO_PESO_CORPORAL:
                return meta.getValorObjetivo() > meta.getValorInicial()
                        ? "peso_subir" : "peso_bajar";
            case Meta.TIPO_PR_EJERCICIO:
                Ejercicio ej = ds.buscarEjercicio(meta.getEjercicioId());
                return ej != null ? construirClavePr(ej) : null;
            default: return null;
        }
    }

    private static String construirClavePr(Ejercicio ej) {
        String n = normalizar(ej.getNombre());
        if (n.contains("press") && n.contains("banca")) return "pr_press_banca";
        if (n.contains("peso muerto")) return "pr_peso_muerto";
        if (n.contains("sentadilla")) return "pr_sentadilla";
        if (n.contains("dominada"))   return "pr_dominadas";
        return null;
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toLowerCase();
    }

    /** Grupo muscular objetivo de la meta (para calcular volumen). */
    private static String grupoDeMeta(Meta meta, DataStore ds) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
                if ("brazoCm".equals(meta.getMedidaTipo())) return "Bíceps"; // aproximación
                if ("piernaCm".equals(meta.getMedidaTipo())) return "Cuádriceps";
                if ("pechoCm".equals(meta.getMedidaTipo())) return "Pectorales";
                return null;
            case Meta.TIPO_REDUCIR_MEDIDA:
                return "Abdomen";
            case Meta.TIPO_PR_EJERCICIO:
                Ejercicio ej = ds.buscarEjercicio(meta.getEjercicioId());
                return ej != null ? ej.getMusculoObjetivo() : null;
            case Meta.TIPO_PESO_CORPORAL:
            default:
                return null;   // sin grupo específico
        }
    }

    private static List<Ejercicio> resolverNombres(List<String> nombres, List<Ejercicio> catalogo) {
        List<Ejercicio> resultado = new ArrayList<>();
        for (String nombre : nombres) {
            for (Ejercicio e : catalogo) {
                if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(nombre)) {
                    resultado.add(e);
                    break;
                }
            }
        }
        return resultado;
    }

    private static List<Ejercicio> fallbackPorMusculo(String grupo, List<Ejercicio> catalogo) {
        List<Ejercicio> lista = new ArrayList<>();
        for (Ejercicio e : catalogo) {
            if (grupo.equals(e.getMusculoObjetivo()) && e.tieneFichaTecnica()) {
                lista.add(e);
                if (lista.size() >= 5) break;
            }
        }
        return lista;
    }

    private static Map<String, Integer> contarEnRutinas(List<Rutina> rutinas) {
        Map<String, Integer> conteos = new HashMap<>();
        for (Rutina r : rutinas) {
            for (RutinaEjercicio re : r.getEjercicios()) {
                conteos.merge(re.getEjercicioId(), 1, Integer::sum);
            }
        }
        return conteos;
    }

    private static Map<String, Ejercicio> indexarCatalogo(List<Ejercicio> catalogo) {
        Map<String, Ejercicio> m = new HashMap<>();
        for (Ejercicio e : catalogo) m.put(e.getId(), e);
        return m;
    }

    /**
     * Estimación de series semanales para un grupo. Asume distribución uniforme entre rutinas
     * (cada rutina se hace {@code diasEntrenoSemana / N_rutinas} veces por semana).
     */
    public static int seriesSemanalesDeGrupo(String grupo, List<Rutina> rutinas,
                                              Map<String, Ejercicio> catalogo,
                                              int diasEntrenoSemana) {
        int seriesPorPasada = 0;
        for (Rutina r : rutinas) {
            for (RutinaEjercicio re : r.getEjercicios()) {
                Ejercicio ej = catalogo.get(re.getEjercicioId());
                if (ej != null && grupo.equals(ej.getMusculoObjetivo())) {
                    seriesPorPasada += re.getSeries();
                }
            }
        }
        int nRutinas = Math.max(1, rutinas.size());
        int pasadasSemana = Math.max(1, diasEntrenoSemana);
        return (int) Math.round((double) seriesPorPasada * pasadasSemana / nRutinas);
    }
}
```

- [ ] **Step 5: Compilar**

```bash
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`. Los símbolos `RutinaEjercicio.ESQUEMA_*` ya existen del Slice 1 (v0.9.0).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/util/metas/
git commit -m "Añadir RecomendadorEjercicios híbrido con tabla curada y filtro por rutinas"
```

---

### Task 6: Bloque `diasEntrenoSemana` en `AjustesActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/AjustesActivity.java`
- Modify: `app/src/main/res/layout/activity_ajustes.xml`

**Interfaces:**
- Consumes: `Usuario.getDiasEntrenoSemana()/setDiasEntrenoSemana(int)` (Task 1).
- Produces: nuevo `ChipGroup` en Ajustes con chips 1-7 (single-select), guarda inmediato.

- [ ] **Step 1: Agregar bloque en `activity_ajustes.xml`**

Al final del `LinearLayout` interno del `NestedScrollView` (después del bloque de fatiga que agregó el Slice 2):

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Días a la semana que entrenas"
            android:textAppearance="?attr/textAppearanceTitleMedium" />

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="Ayuda al recomendador de metas a calcular tu volumen semanal."
            android:textAppearance="?attr/textAppearanceBodySmall" />

        <com.google.android.material.chip.ChipGroup
            android:id="@+id/chip_group_dias_entreno"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            app:singleSelection="true"
            app:selectionRequired="true">

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_dias_1"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="1" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_dias_2"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="2" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_dias_3"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="3" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_dias_4"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="4" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_dias_5"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="5" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_dias_6"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="6" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_dias_7"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="7" />
        </com.google.android.material.chip.ChipGroup>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 2: Wire-up en `AjustesActivity.java`**

En `onCreate()`, después del bloque del switch de fatiga:

```java
com.google.android.material.chip.ChipGroup chipsDias = findViewById(R.id.chip_group_dias_entreno);
int diasActual = usuario != null ? usuario.getDiasEntrenoSemana() : 3;
int idInicial;
switch (diasActual) {
    case 1: idInicial = R.id.chip_dias_1; break;
    case 2: idInicial = R.id.chip_dias_2; break;
    case 4: idInicial = R.id.chip_dias_4; break;
    case 5: idInicial = R.id.chip_dias_5; break;
    case 6: idInicial = R.id.chip_dias_6; break;
    case 7: idInicial = R.id.chip_dias_7; break;
    case 3:
    default: idInicial = R.id.chip_dias_3; break;
}
chipsDias.check(idInicial);
chipsDias.setOnCheckedStateChangeListener((group, checkedIds) -> {
    if (checkedIds.isEmpty() || usuario == null) return;
    int id = checkedIds.get(0);
    int nuevo;
    if (id == R.id.chip_dias_1) nuevo = 1;
    else if (id == R.id.chip_dias_2) nuevo = 2;
    else if (id == R.id.chip_dias_4) nuevo = 4;
    else if (id == R.id.chip_dias_5) nuevo = 5;
    else if (id == R.id.chip_dias_6) nuevo = 6;
    else if (id == R.id.chip_dias_7) nuevo = 7;
    else nuevo = 3;
    if (nuevo != usuario.getDiasEntrenoSemana()) {
        usuario.setDiasEntrenoSemana(nuevo);
        dataManager.save();
    }
});
```

**Nota:** `Usuario` se serializa en `datos.json`, no en `catalogo_local.json`. Por eso `dataManager.save()` (async) y NO `saveCatalogo()`.

- [ ] **Step 3: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Abrir Ajustes → chip "3" seleccionado por default → cambiar a "5" → cerrar y reabrir Ajustes → chip "5" persiste.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/AjustesActivity.java app/src/main/res/layout/activity_ajustes.xml
git commit -m "Añadir selector de días de entrenamiento por semana en Ajustes"
```

---

### Task 7: `MetasFragment` reescrito + adapters de activas e historial

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/MetasFragment.java` (rewrite total)
- Create: `app/src/main/res/layout/fragment_metas.xml`
- Create: `app/src/main/res/layout/item_meta_activa.xml`
- Create: `app/src/main/res/layout/item_meta_historial.xml`
- Create: `app/src/main/java/com/ironquest/mvp/ui/MetasActivasAdapter.java`
- Create: `app/src/main/java/com/ironquest/mvp/ui/MetasHistorialAdapter.java`

**Interfaces:**
- Consumes: `DataManager.getMetasActivas()`, `getMetasHistorial()` (Task 2), `EvaluadorMeta.para()` + `calcularProgreso`/`textoProgreso`/`ultimoRegistroRelevante` (Task 3).
- Produces:
  - `MetasFragment` renderizable en el ViewPager (`MainActivity.pestañas`).
  - Adapters siguen la convención existente del proyecto (misma familia que `HistorialAdapter`).

**Nota UX:** los diálogos "Nueva meta", "Ver plan sugerido", "Editar" y "Descartar" se crean
en las Tasks 8-10. Por ahora, los botones/menús que los invocarían **solo hacen Toast
placeholder**. El plan queda así porque el fragment se puede probar aislado (renderizar la
lista de metas activas + historial con las metas que ya haya en `datos.json`; para
generarlas manualmente se puede editar el respaldo o esperar a las próximas tasks).

- [ ] **Step 1: Crear `fragment_metas.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/text_metas_header"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Metas · 0/3 activas"
                android:textAppearance="?attr/textAppearanceHeadlineSmall" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/button_nueva_meta"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="+ Nueva meta" />
        </LinearLayout>

        <com.google.android.material.card.MaterialCardView
            android:id="@+id/card_estado_vacio"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:visibility="gone">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="20dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Aún no tienes metas"
                    android:textAppearance="?attr/textAppearanceTitleMedium" />

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:text="Crea una para que la app te sugiera cómo alcanzarla."
                    android:textAppearance="?attr/textAppearanceBodyMedium" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/list_metas_activas"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:nestedScrollingEnabled="false" />

        <TextView
            android:id="@+id/header_historial"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            android:clickable="true"
            android:focusable="true"
            android:padding="8dp"
            android:text="Historial (0) ▸"
            android:textAppearance="?attr/textAppearanceTitleSmall" />

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/list_metas_historial"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:nestedScrollingEnabled="false"
            android:visibility="gone" />
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
```

- [ ] **Step 2: Crear `item_meta_activa.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:layout_marginBottom="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/text_meta_icono"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textSize="24sp"
                android:text="💪"
                android:layout_marginEnd="8dp" />

            <TextView
                android:id="@+id/text_meta_titulo"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textAppearance="?attr/textAppearanceTitleMedium"
                android:text="Meta" />

            <ImageButton
                android:id="@+id/button_meta_overflow"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="Más opciones"
                android:src="@android:drawable/ic_menu_more" />
        </LinearLayout>

        <com.google.android.material.progressindicator.LinearProgressIndicator
            android:id="@+id/progress_meta"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:max="100"
            android:progress="0" />

        <TextView
            android:id="@+id/text_meta_progreso"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textAppearance="?attr/textAppearanceBodyMedium"
            android:text="0 → 0 (0 %)" />

        <TextView
            android:id="@+id/text_meta_deadline"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:textAppearance="?attr/textAppearanceBodySmall"
            android:visibility="gone" />

        <TextView
            android:id="@+id/text_meta_recordatorio"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:padding="8dp"
            android:textAppearance="?attr/textAppearanceBodySmall"
            android:visibility="gone"
            android:background="?attr/colorSurfaceVariant"
            android:text="Actualiza tu medida en Mi físico para ver tu progreso" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/button_ver_plan"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end"
            android:layout_marginTop="8dp"
            android:text="Ver plan sugerido" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 3: Crear `item_meta_historial.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="4dp"
    android:layout_marginBottom="4dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp"
        android:gravity="center_vertical">

        <TextView
            android:id="@+id/text_hist_estado"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textSize="20sp"
            android:text="✓"
            android:layout_marginEnd="12dp" />

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/text_hist_titulo"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceTitleSmall" />

            <TextView
                android:id="@+id/text_hist_resultado"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceBodySmall" />

            <TextView
                android:id="@+id/text_hist_duracion"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textAppearance="?attr/textAppearanceBodySmall" />
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 4: Crear `MetasActivasAdapter.java`**

```java
package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.ironquest.mvp.R;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.util.metas.EvaluadorMeta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MetasActivasAdapter extends RecyclerView.Adapter<MetasActivasAdapter.VH> {

    public interface Listener {
        void onVerPlan(Meta meta);
        void onEditar(Meta meta);
        void onDescartar(Meta meta);
    }

    private final List<Meta> items;
    private final DataStore dataStore;
    private final Listener listener;
    /** Id de meta a resaltar por 3 s (viene de deeplink de notificación). */
    private String metaResaltadaId;

    public MetasActivasAdapter(List<Meta> items, DataStore dataStore, Listener listener) {
        this.items = items;
        this.dataStore = dataStore;
        this.listener = listener;
    }

    public void resaltar(String metaId) {
        this.metaResaltadaId = metaId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meta_activa, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        final TextView icono, titulo, progresoTxt, deadline, recordatorio;
        final LinearProgressIndicator progreso;
        final ImageButton overflow;
        final com.google.android.material.button.MaterialButton botonVerPlan;

        VH(@NonNull View itemView) {
            super(itemView);
            icono = itemView.findViewById(R.id.text_meta_icono);
            titulo = itemView.findViewById(R.id.text_meta_titulo);
            progresoTxt = itemView.findViewById(R.id.text_meta_progreso);
            deadline = itemView.findViewById(R.id.text_meta_deadline);
            recordatorio = itemView.findViewById(R.id.text_meta_recordatorio);
            progreso = itemView.findViewById(R.id.progress_meta);
            overflow = itemView.findViewById(R.id.button_meta_overflow);
            botonVerPlan = itemView.findViewById(R.id.button_ver_plan);
        }

        void bind(Meta m) {
            icono.setText(iconoDe(m.getTipo()));
            titulo.setText(m.getTitulo());

            EvaluadorMeta ev = EvaluadorMeta.para(m);
            double prog = ev.calcularProgreso(m, dataStore);
            progreso.setProgress((int) Math.round(prog * 100));
            progresoTxt.setText(ev.textoProgreso(m, dataStore));

            // Deadline
            if (m.getFechaObjetivo() != null && !m.getFechaObjetivo().isEmpty()) {
                LocalDate hoy = LocalDate.now();
                LocalDate obj = LocalDate.parse(m.getFechaObjetivo());
                long dias = ChronoUnit.DAYS.between(hoy, obj);
                deadline.setVisibility(View.VISIBLE);
                if (dias >= 0) {
                    deadline.setText("Faltan " + dias + " días");
                } else {
                    deadline.setText("Vencida hace " + (-dias) + " días");
                    deadline.setTextColor(0xFFC62828); // rojo
                }
            } else {
                deadline.setVisibility(View.GONE);
            }

            // Recordatorio pasivo "sin actualizar hace X días"
            String ultimo = ev.ultimoRegistroRelevante(m, dataStore);
            boolean mostrarRec = false;
            if (ultimo != null) {
                try {
                    LocalDate ultimaFecha = ultimo.length() >= 10
                            ? LocalDate.parse(ultimo.substring(0, 10))
                            : LocalDateTime.parse(ultimo).toLocalDate();
                    long dias = ChronoUnit.DAYS.between(ultimaFecha, LocalDate.now());
                    if (dias > 14) mostrarRec = true;
                } catch (Exception ignored) { }
            }
            recordatorio.setVisibility(mostrarRec ? View.VISIBLE : View.GONE);

            // Resaltado (deeplink de notificación)
            if (metaResaltadaId != null && metaResaltadaId.equals(m.getId())) {
                itemView.setBackgroundColor(0xFFFFF3E0); // acento claro
                itemView.postDelayed(() -> {
                    metaResaltadaId = null;
                    itemView.setBackgroundColor(0);
                }, 3000);
            }

            botonVerPlan.setOnClickListener(v -> listener.onVerPlan(m));
            overflow.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.getMenu().add(0, 1, 0, "Editar título/objetivo");
                menu.getMenu().add(0, 2, 1, "Descartar meta");
                menu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) listener.onEditar(m);
                    else if (item.getItemId() == 2) listener.onDescartar(m);
                    return true;
                });
                menu.show();
            });
        }

        private String iconoDe(int tipo) {
            switch (tipo) {
                case Meta.TIPO_AUMENTAR_MEDIDA: return "💪";
                case Meta.TIPO_REDUCIR_MEDIDA:  return "📉";
                case Meta.TIPO_PESO_CORPORAL:   return "⚖";
                case Meta.TIPO_PR_EJERCICIO:    return "🏋";
                default: return "•";
            }
        }
    }
}
```

- [ ] **Step 5: Crear `MetasHistorialAdapter.java`**

```java
package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Meta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MetasHistorialAdapter extends RecyclerView.Adapter<MetasHistorialAdapter.VH> {

    private final List<Meta> items;

    public MetasHistorialAdapter(List<Meta> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meta_historial, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView estado, titulo, resultado, duracion;

        VH(@NonNull View itemView) {
            super(itemView);
            estado = itemView.findViewById(R.id.text_hist_estado);
            titulo = itemView.findViewById(R.id.text_hist_titulo);
            resultado = itemView.findViewById(R.id.text_hist_resultado);
            duracion = itemView.findViewById(R.id.text_hist_duracion);
        }

        void bind(Meta m) {
            boolean cumplida = m.getEstado() == Meta.ESTADO_CUMPLIDA;
            estado.setText(cumplida ? "✓" : "✗");
            estado.setTextColor(cumplida ? 0xFF2E7D32 : 0xFF757575);
            titulo.setText(m.getTitulo());
            resultado.setText(cumplida
                    ? "Cumplida"
                    : "Descartada");

            // Duración desde creación hasta cierre
            String fechaCierre = cumplida ? m.getFechaCumplida() : m.getFechaDescartada();
            if (fechaCierre != null && m.getFechaCreacion() != null) {
                try {
                    LocalDate creada = LocalDateTime.parse(m.getFechaCreacion()).toLocalDate();
                    LocalDate cerrada = LocalDateTime.parse(fechaCierre).toLocalDate();
                    long dias = ChronoUnit.DAYS.between(creada, cerrada);
                    duracion.setText((cumplida ? "En " : "Descartada tras ") + dias + " días");
                } catch (Exception e) {
                    duracion.setText("");
                }
            } else {
                duracion.setText("");
            }
        }
    }
}
```

- [ ] **Step 6: Reescribir `MetasFragment.java`**

Reemplazar el archivo completo:

```java
package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;

import java.util.List;

/**
 * Tab "Metas": lista de metas activas + historial. Se refresca en onResume() para reflejar
 * cambios hechos desde otras pantallas (ej. cumplimiento por sesión).
 */
public class MetasFragment extends Fragment {

    /** Extra opcional: id de meta a resaltar (viene de tap en notificación push). */
    public static final String EXTRA_META_DESTACADA_ID = "meta_destacada_id";

    private DataManager dataManager;
    private DataStore dataStore;
    private RecyclerView listActivas;
    private RecyclerView listHistorial;
    private TextView headerHist;
    private TextView headerContador;
    private View cardVacio;
    private MaterialButton botonNueva;
    private MetasActivasAdapter activasAdapter;
    private MetasHistorialAdapter historialAdapter;
    private String metaDestacadaPendiente;

    public static MetasFragment crear() {
        return new MetasFragment();
    }

    /** Compatibilidad hacia atrás con la firma anterior (MainActivity.crear("Metas — próximamente")). */
    public static MetasFragment crear(String mensajeIgnorado) {
        return new MetasFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_metas, container, false);
        dataManager = DataManager.getInstance(requireContext());
        dataStore = dataManager.getDataStore();

        headerContador = v.findViewById(R.id.text_metas_header);
        botonNueva = v.findViewById(R.id.button_nueva_meta);
        cardVacio = v.findViewById(R.id.card_estado_vacio);
        listActivas = v.findViewById(R.id.list_metas_activas);
        listHistorial = v.findViewById(R.id.list_metas_historial);
        headerHist = v.findViewById(R.id.header_historial);

        listActivas.setLayoutManager(new LinearLayoutManager(getContext()));
        listHistorial.setLayoutManager(new LinearLayoutManager(getContext()));

        botonNueva.setOnClickListener(view -> abrirCrearMeta());
        headerHist.setOnClickListener(view -> toggleHistorial());

        // Extra desde intent (notificación push): resaltar meta
        Bundle args = getArguments();
        if (args != null) metaDestacadaPendiente = args.getString(EXTRA_META_DESTACADA_ID);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refrescar();
        if (metaDestacadaPendiente != null && activasAdapter != null) {
            activasAdapter.resaltar(metaDestacadaPendiente);
            metaDestacadaPendiente = null;
        }
    }

    private void refrescar() {
        List<Meta> activas = dataManager.getMetasActivas();
        List<Meta> historial = dataManager.getMetasHistorial();

        headerContador.setText("Metas · " + activas.size() + "/3 activas");
        botonNueva.setEnabled(activas.size() < 3);

        boolean vacio = activas.isEmpty() && historial.isEmpty();
        cardVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);

        activasAdapter = new MetasActivasAdapter(activas, dataStore,
                new MetasActivasAdapter.Listener() {
                    @Override public void onVerPlan(Meta meta) { abrirPlanSugerido(meta); }
                    @Override public void onEditar(Meta meta)  { abrirEditar(meta); }
                    @Override public void onDescartar(Meta meta){ confirmarDescartar(meta); }
                });
        listActivas.setAdapter(activasAdapter);

        historialAdapter = new MetasHistorialAdapter(historial);
        listHistorial.setAdapter(historialAdapter);
        headerHist.setText("Historial (" + historial.size() + ") "
                + (listHistorial.getVisibility() == View.VISIBLE ? "▾" : "▸"));
    }

    private void toggleHistorial() {
        boolean visible = listHistorial.getVisibility() == View.VISIBLE;
        listHistorial.setVisibility(visible ? View.GONE : View.VISIBLE);
        int hist = dataManager.getMetasHistorial().size();
        headerHist.setText("Historial (" + hist + ") " + (visible ? "▸" : "▾"));
    }

    // ---- placeholders para las Tasks 8-10 ----

    private void abrirCrearMeta() {
        // Se reemplaza en Task 8 por: CrearMetaDialog.mostrar(requireActivity(), dataManager, this::refrescar);
        Toast.makeText(getContext(), "Crear meta (próximamente)", Toast.LENGTH_SHORT).show();
    }

    private void abrirPlanSugerido(Meta meta) {
        // Se reemplaza en Task 9 por: PlanSugeridoDialog.mostrar(requireActivity(), dataManager, meta, this::refrescar);
        Toast.makeText(getContext(), "Plan sugerido (próximamente)", Toast.LENGTH_SHORT).show();
    }

    private void abrirEditar(Meta meta) {
        // Se reemplaza en Task 10.
        Toast.makeText(getContext(), "Editar meta (próximamente)", Toast.LENGTH_SHORT).show();
    }

    private void confirmarDescartar(Meta meta) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Descartar meta")
                .setMessage("¿Seguro que quieres descartar \"" + meta.getTitulo() + "\"? Podrás verla en el historial.")
                .setPositiveButton("Descartar", (d, w) -> {
                    dataManager.descartarMeta(meta.getId());
                    refrescar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
```

- [ ] **Step 7: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Abrir el tab Metas → sin metas debe verse el card "Aún no tienes metas" + botón "+ Nueva meta" habilitado. Tap → Toast "Crear meta (próximamente)". Header "Historial (0) ▸" no hace nada visual porque no hay historial.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/MetasFragment.java app/src/main/java/com/ironquest/mvp/ui/MetasActivasAdapter.java app/src/main/java/com/ironquest/mvp/ui/MetasHistorialAdapter.java app/src/main/res/layout/fragment_metas.xml app/src/main/res/layout/item_meta_activa.xml app/src/main/res/layout/item_meta_historial.xml
git commit -m "Reescribir MetasFragment con lista de activas, historial y estado vacío"
```

---

### Task 8: `CrearMetaDialog` + formularios dinámicos

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/ui/CrearMetaDialog.java`
- Create: `app/src/main/res/layout/dialog_crear_meta.xml`
- Create: `app/src/main/res/layout/form_meta_muscular.xml`
- Create: `app/src/main/res/layout/form_meta_reducir.xml`
- Create: `app/src/main/res/layout/form_meta_peso.xml`
- Create: `app/src/main/res/layout/form_meta_pr.xml`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/MetasFragment.java` (llamar al diálogo real)

**Interfaces:**
- Consumes: `ValidadorMeta.validar(Meta, DataStore)` (Task 4), `DataManager.agregarMeta(Meta)` (Task 2), `EjercicioPicker` existente (para tipo PR).
- Produces: `CrearMetaDialog.mostrar(Activity, DataManager, Runnable onCreada)`.

- [ ] **Step 1: Crear `dialog_crear_meta.xml`**

Layout raíz `ScrollView` con `LinearLayout` vertical, padding 20dp:

- Header: `TextView` "Nueva meta" (`textAppearanceHeadlineSmall`).
- `ChipGroup` id `chip_group_tipo_meta` singleSelection selectionRequired: chips "Muscular", "Cintura/cadera", "Peso corporal", "PR ejercicio" (ids `chip_tipo_muscular`, `chip_tipo_reducir`, `chip_tipo_peso`, `chip_tipo_pr`).
- `FrameLayout` id `container_formulario_meta`: donde se infla dinámico.
- `TextInputLayout` con `TextInputEditText` id `edit_titulo_meta`, hint "Título (editable)".
- `CheckBox` id `check_meta_deadline` "Poner fecha objetivo".
- `com.google.android.material.button.MaterialButton` id `button_meta_fecha` "Elegir fecha" (visibility gone hasta que se marque el checkbox), `text_meta_fecha_elegida` a la derecha.
- Botones al pie: `button_meta_crear` "Crear" y `button_meta_cancelar` "Cancelar" (dentro de un `LinearLayout` horizontal end).

Ejemplo compacto — big pickle expande siguiendo el patrón de otros diálogos existentes (`dialog_info_progresion.xml` es referencia):

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="20dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Nueva meta"
            android:textAppearance="?attr/textAppearanceHeadlineSmall" />

        <com.google.android.material.chip.ChipGroup
            android:id="@+id/chip_group_tipo_meta"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            app:singleSelection="true"
            app:selectionRequired="true">

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_tipo_muscular"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Muscular" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_tipo_reducir"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Cintura/cadera" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_tipo_peso"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Peso corporal" />
            <com.google.android.material.chip.Chip
                android:id="@+id/chip_tipo_pr"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="PR ejercicio" />
        </com.google.android.material.chip.ChipGroup>

        <FrameLayout
            android:id="@+id/container_formulario_meta"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp" />

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:hint="Título (editable)">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/edit_titulo_meta"
                android:layout_width="match_parent"
                android:layout_height="wrap_content" />
        </com.google.android.material.textfield.TextInputLayout>

        <CheckBox
            android:id="@+id/check_meta_deadline"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:text="Poner fecha objetivo" />

        <LinearLayout
            android:id="@+id/container_fecha_meta"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:visibility="gone">
            <com.google.android.material.button.MaterialButton
                android:id="@+id/button_meta_fecha"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Elegir fecha" />
            <TextView
                android:id="@+id/text_meta_fecha_elegida"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="12dp" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="end"
            android:layout_marginTop="16dp">
            <com.google.android.material.button.MaterialButton
                android:id="@+id/button_meta_cancelar"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Cancelar"
                android:layout_marginEnd="8dp" />
            <com.google.android.material.button.MaterialButton
                android:id="@+id/button_meta_crear"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Crear" />
        </LinearLayout>
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 2: Crear los 4 form_meta_*.xml**

**`form_meta_muscular.xml`** — Spinner + input:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Medida a aumentar" />

    <Spinner
        android:id="@+id/spinner_meta_medida"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:hint="Objetivo (cm)">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/edit_meta_objetivo_cm"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="numberDecimal" />
    </com.google.android.material.textfield.TextInputLayout>
</LinearLayout>
```

**`form_meta_reducir.xml`** — idéntico al muscular pero con Spinner de `cintura/cadera` y el texto "Medida a reducir". Reusa mismos ids (`spinner_meta_medida`, `edit_meta_objetivo_cm`).

**`form_meta_peso.xml`** — solo input:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Peso corporal objetivo (kg)">
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/edit_meta_peso_kg"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="numberDecimal" />
    </com.google.android.material.textfield.TextInputLayout>
</LinearLayout>
```

**`form_meta_pr.xml`** — selector de ejercicio + peso + reps:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <com.google.android.material.button.MaterialButton
        android:id="@+id/button_meta_pr_seleccionar_ejercicio"
        style="?attr/materialButtonOutlinedStyle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Seleccionar ejercicio" />

    <TextView
        android:id="@+id/text_meta_pr_ejercicio"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAppearance="?attr/textAppearanceBodyMedium"
        android:layout_marginTop="4dp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="8dp">

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="Peso (kg)"
            android:layout_marginEnd="8dp">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/edit_meta_pr_peso"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="numberDecimal" />
        </com.google.android.material.textfield.TextInputLayout>

        <com.google.android.material.textfield.TextInputLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:hint="Reps">
            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/edit_meta_pr_reps"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:inputType="number" />
        </com.google.android.material.textfield.TextInputLayout>
    </LinearLayout>
</LinearLayout>
```

- [ ] **Step 3: Crear `CrearMetaDialog.java`**

Archivo largo (~350 líneas). Estructura general que big pickle debe implementar:

```java
package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.*;
import com.ironquest.mvp.util.metas.Advertencia;
import com.ironquest.mvp.util.metas.ValidadorMeta;

import java.time.*;
import java.time.format.*;

public final class CrearMetaDialog extends Dialog {

    public interface OnCreada { void onCreada(); }

    // Ids de opciones para el spinner de medida muscular
    private static final String[] MEDIDAS_MUSCULARES = {"brazoCm", "piernaCm", "pechoCm"};
    private static final String[] MEDIDAS_MUSCULARES_LABELS = {"Brazo", "Pierna", "Pecho"};
    private static final String[] MEDIDAS_REDUCIR = {"cinturaCm", "caderaCm"};
    private static final String[] MEDIDAS_REDUCIR_LABELS = {"Cintura", "Cadera"};

    private final Activity activity;
    private final DataManager dataManager;
    private final DataStore dataStore;
    private final OnCreada onCreada;

    private int tipoSeleccionado = Meta.TIPO_AUMENTAR_MEDIDA;
    private String medidaSeleccionada = "brazoCm";
    private String prEjercicioId;
    private String prEjercicioNombre;
    private String fechaObjetivoIso;
    private boolean tituloEditadoPorUsuario = false;
    private EditText editTitulo;

    public static void mostrar(Activity activity, DataManager dataManager, OnCreada onCreada) {
        new CrearMetaDialog(activity, dataManager, onCreada).show();
    }

    private CrearMetaDialog(Activity activity, DataManager dataManager, OnCreada onCreada) {
        super(activity);
        this.activity = activity;
        this.dataManager = dataManager;
        this.dataStore = dataManager.getDataStore();
        this.onCreada = onCreada;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_crear_meta);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        editTitulo = findViewById(R.id.edit_titulo_meta);
        editTitulo.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (editTitulo.hasFocus()) tituloEditadoPorUsuario = true;
            }
        });

        ChipGroup chips = findViewById(R.id.chip_group_tipo_meta);
        chips.check(R.id.chip_tipo_muscular);
        chips.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_tipo_muscular)      tipoSeleccionado = Meta.TIPO_AUMENTAR_MEDIDA;
            else if (id == R.id.chip_tipo_reducir)  tipoSeleccionado = Meta.TIPO_REDUCIR_MEDIDA;
            else if (id == R.id.chip_tipo_peso)     tipoSeleccionado = Meta.TIPO_PESO_CORPORAL;
            else if (id == R.id.chip_tipo_pr)       tipoSeleccionado = Meta.TIPO_PR_EJERCICIO;
            inflarFormulario();
            regenerarTitulo();
        });
        inflarFormulario();

        CheckBox check = findViewById(R.id.check_meta_deadline);
        View containerFecha = findViewById(R.id.container_fecha_meta);
        check.setOnCheckedChangeListener((v, isChecked) ->
                containerFecha.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        findViewById(R.id.button_meta_fecha).setOnClickListener(v -> abrirDatePicker());

        findViewById(R.id.button_meta_cancelar).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_meta_crear).setOnClickListener(v -> intentarCrear());
    }

    private void inflarFormulario() {
        FrameLayout container = findViewById(R.id.container_formulario_meta);
        container.removeAllViews();
        int layoutId;
        switch (tipoSeleccionado) {
            case Meta.TIPO_AUMENTAR_MEDIDA: layoutId = R.layout.form_meta_muscular; break;
            case Meta.TIPO_REDUCIR_MEDIDA:  layoutId = R.layout.form_meta_reducir;  break;
            case Meta.TIPO_PESO_CORPORAL:   layoutId = R.layout.form_meta_peso;     break;
            case Meta.TIPO_PR_EJERCICIO:    layoutId = R.layout.form_meta_pr;       break;
            default: return;
        }
        getLayoutInflater().inflate(layoutId, container, true);
        conectarFormulario();
    }

    private void conectarFormulario() {
        if (tipoSeleccionado == Meta.TIPO_AUMENTAR_MEDIDA
                || tipoSeleccionado == Meta.TIPO_REDUCIR_MEDIDA) {
            Spinner sp = findViewById(R.id.spinner_meta_medida);
            String[] labels = tipoSeleccionado == Meta.TIPO_AUMENTAR_MEDIDA
                    ? MEDIDAS_MUSCULARES_LABELS : MEDIDAS_REDUCIR_LABELS;
            String[] ids = tipoSeleccionado == Meta.TIPO_AUMENTAR_MEDIDA
                    ? MEDIDAS_MUSCULARES : MEDIDAS_REDUCIR;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(adapter);
            medidaSeleccionada = ids[0];
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long l) {
                    medidaSeleccionada = ids[pos];
                    regenerarTitulo();
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
            EditText ed = findViewById(R.id.edit_meta_objetivo_cm);
            ed.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { regenerarTitulo(); }
            });
        } else if (tipoSeleccionado == Meta.TIPO_PESO_CORPORAL) {
            EditText ed = findViewById(R.id.edit_meta_peso_kg);
            ed.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { regenerarTitulo(); }
            });
        } else if (tipoSeleccionado == Meta.TIPO_PR_EJERCICIO) {
            findViewById(R.id.button_meta_pr_seleccionar_ejercicio).setOnClickListener(v -> {
                EjercicioPicker.mostrar(activity, dataManager, dataStore, ejercicio -> {
                    prEjercicioId = ejercicio.getId();
                    prEjercicioNombre = ejercicio.getNombre();
                    ((TextView) findViewById(R.id.text_meta_pr_ejercicio)).setText(prEjercicioNombre);
                    regenerarTitulo();
                });
            });
            SimpleTextWatcher w = new SimpleTextWatcher() {
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { regenerarTitulo(); }
            };
            ((EditText) findViewById(R.id.edit_meta_pr_peso)).addTextChangedListener(w);
            ((EditText) findViewById(R.id.edit_meta_pr_reps)).addTextChangedListener(w);
        }
    }

    private void regenerarTitulo() {
        if (tituloEditadoPorUsuario) return;
        String t = "";
        switch (tipoSeleccionado) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
                t = "Aumentar " + labelDeMedida(medidaSeleccionada) + " a "
                        + leerDouble(R.id.edit_meta_objetivo_cm) + " cm"; break;
            case Meta.TIPO_REDUCIR_MEDIDA:
                t = "Reducir " + labelDeMedida(medidaSeleccionada) + " a "
                        + leerDouble(R.id.edit_meta_objetivo_cm) + " cm"; break;
            case Meta.TIPO_PESO_CORPORAL:
                t = "Alcanzar " + leerDouble(R.id.edit_meta_peso_kg) + " kg"; break;
            case Meta.TIPO_PR_EJERCICIO:
                if (prEjercicioNombre != null) {
                    t = "PR " + prEjercicioNombre + ": " + leerDouble(R.id.edit_meta_pr_peso)
                            + " kg × " + leerInt(R.id.edit_meta_pr_reps);
                }
                break;
        }
        editTitulo.setText(t);
    }

    private void abrirDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Fecha objetivo")
                .build();
        picker.addOnPositiveButtonClickListener(millis -> {
            LocalDate fecha = LocalDate.ofEpochDay(millis / 86_400_000L);
            fechaObjetivoIso = fecha.toString();
            ((TextView) findViewById(R.id.text_meta_fecha_elegida)).setText(fecha.toString());
        });
        picker.show(((androidx.fragment.app.FragmentActivity) activity)
                .getSupportFragmentManager(), "fecha_meta");
    }

    private void intentarCrear() {
        Meta meta = construirMeta();
        if (meta == null) return;  // toast ya mostrado por construirMeta

        Advertencia adv = ValidadorMeta.validar(meta, dataStore);
        if (adv == null) {
            confirmarYPersistir(meta);
        } else {
            new AlertDialog.Builder(getContext())
                    .setTitle("Aviso sobre tu meta")
                    .setMessage(adv.getTexto())
                    .setPositiveButton("Crear igual", (d, w) -> {
                        meta.setAdvertenciaAceptada(true);
                        confirmarYPersistir(meta);
                    })
                    .setNegativeButton("Ajustar", null)
                    .show();
        }
    }

    private void confirmarYPersistir(Meta meta) {
        boolean ok = dataManager.agregarMeta(meta);
        if (!ok) {
            Toast.makeText(getContext(),
                    "Cumple o descarta una meta antes de crear otra", Toast.LENGTH_LONG).show();
            return;
        }
        if (onCreada != null) onCreada.onCreada();
        dismiss();
    }

    private Meta construirMeta() {
        String titulo = editTitulo.getText() != null ? editTitulo.getText().toString().trim() : "";
        if (TextUtils.isEmpty(titulo)) {
            Toast.makeText(getContext(), "Escribe un título", Toast.LENGTH_SHORT).show();
            return null;
        }
        String id = dataManager.newId("m");
        String fechaCreacion = LocalDateTime.now().toString();
        Meta m = new Meta(id, tipoSeleccionado, titulo, fechaCreacion);
        m.setFechaObjetivo(fechaObjetivoIso);

        // Snapshot inicial + campos por tipo
        switch (tipoSeleccionado) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
            case Meta.TIPO_REDUCIR_MEDIDA:
                m.setMedidaTipo(medidaSeleccionada);
                m.setValorObjetivo(leerDouble(R.id.edit_meta_objetivo_cm));
                m.setValorInicial(leerMedidaActual(medidaSeleccionada));
                if (m.getValorObjetivo() <= 0) { toast("Ingresa un objetivo válido"); return null; }
                break;
            case Meta.TIPO_PESO_CORPORAL:
                m.setValorObjetivo(leerDouble(R.id.edit_meta_peso_kg));
                m.setValorInicial(leerPesoActual());
                if (m.getValorObjetivo() <= 0) { toast("Ingresa un peso objetivo"); return null; }
                break;
            case Meta.TIPO_PR_EJERCICIO:
                if (prEjercicioId == null) { toast("Selecciona un ejercicio"); return null; }
                m.setEjercicioId(prEjercicioId);
                m.setPesoObjetivoKg(leerDouble(R.id.edit_meta_pr_peso));
                m.setRepsObjetivo(leerInt(R.id.edit_meta_pr_reps));
                if (m.getPesoObjetivoKg() <= 0 || m.getRepsObjetivo() <= 0) {
                    toast("Ingresa peso y reps > 0"); return null;
                }
                m.setValorInicial(m.getPesoObjetivoKg() * m.getRepsObjetivo() * 0.5);
                break;
        }
        return m;
    }

    // ---- helpers ----

    private String labelDeMedida(String medidaId) {
        switch (medidaId) {
            case "brazoCm": return "brazo";
            case "piernaCm": return "pierna";
            case "pechoCm": return "pecho";
            case "cinturaCm": return "cintura";
            case "caderaCm": return "cadera";
            default: return "medida";
        }
    }

    private double leerDouble(int id) {
        EditText e = findViewById(id);
        if (e == null || e.getText() == null) return 0;
        try { return Double.parseDouble(e.getText().toString().trim()); }
        catch (Exception ex) { return 0; }
    }

    private int leerInt(int id) {
        EditText e = findViewById(id);
        if (e == null || e.getText() == null) return 0;
        try { return Integer.parseInt(e.getText().toString().trim()); }
        catch (Exception ex) { return 0; }
    }

    private double leerMedidaActual(String medidaId) {
        java.util.List<RegistroFisico> h = dataStore.getHistorialFisico();
        if (h.isEmpty()) return 0;
        RegistroFisico r = h.get(h.size() - 1);
        switch (medidaId) {
            case "brazoCm": return r.getBrazoCm();
            case "piernaCm": return r.getPiernaCm();
            case "pechoCm": return r.getPechoCm();
            case "cinturaCm": return r.getCinturaCm();
            case "caderaCm": return r.getCaderaCm();
            default: return 0;
        }
    }

    private double leerPesoActual() {
        java.util.List<RegistroFisico> h = dataStore.getHistorialFisico();
        for (int i = h.size() - 1; i >= 0; i--) {
            if (h.get(i).getPesoKg() > 0) return h.get(i).getPesoKg();
        }
        return 0;
    }

    private void toast(String s) { Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show(); }

    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}
```

- [ ] **Step 4: Enganchar el diálogo real en `MetasFragment`**

Reemplazar el método `abrirCrearMeta()` del fragment:

```java
private void abrirCrearMeta() {
    CrearMetaDialog.mostrar(requireActivity(), dataManager, this::refrescar);
}
```

- [ ] **Step 5: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Ir a Metas → "+ Nueva meta" → diálogo abre. Probar los 4 tipos, verificar que el título se autogenera, que el checkbox de fecha muestra el picker, y que al tocar Crear con un valor absurdo dispara la advertencia.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/CrearMetaDialog.java app/src/main/java/com/ironquest/mvp/ui/MetasFragment.java app/src/main/res/layout/dialog_crear_meta.xml app/src/main/res/layout/form_meta_*.xml
git commit -m "Añadir CrearMetaDialog con formularios dinámicos y validación"
```

---

### Task 9: `PlanSugeridoDialog` + `AgregarEjercicioARutinaDialog`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/ui/PlanSugeridoDialog.java`
- Create: `app/src/main/java/com/ironquest/mvp/ui/AgregarEjercicioARutinaDialog.java`
- Create: `app/src/main/res/layout/dialog_plan_sugerido.xml`
- Create: `app/src/main/res/layout/item_plan_ejercicio_sugerido.xml`
- Create: `app/src/main/res/layout/dialog_agregar_ejercicio_a_rutina.xml`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/MetasFragment.java` (llamar al diálogo real)

**Interfaces:**
- Consumes: `RecomendadorEjercicios.recomendar(Meta, DataStore)` (Task 5), `RutinaEjercicio` existente.
- Produces:
  - `PlanSugeridoDialog.mostrar(Activity, DataManager, Meta, Runnable onCerrado)`.
  - `AgregarEjercicioARutinaDialog.mostrar(Activity, DataManager, Ejercicio, PlanRecomendado, Runnable onAgregado)`.

- [ ] **Step 1: Crear `dialog_plan_sugerido.xml`**

`ScrollView` + `LinearLayout` vertical con:
- Header `TextView` "Plan sugerido para: {título}" (id `text_plan_meta_titulo`).
- `MaterialCardView` con `text_plan_volumen`.
- `MaterialCardView` con `text_plan_params` ("Ejercicios nuevos: X × Y-Z, esquema W").
- `MaterialCardView` con `text_plan_cobertura` (visibility gone por default; texto grande cuando cubre todo).
- `RecyclerView` id `list_plan_ejercicios`.
- Botón "Cerrar" (id `button_plan_cerrar`).

Layout patrón similar a otros diálogos ya en el proyecto. Big pickle sigue el estilo de `dialog_info_progresion.xml`.

- [ ] **Step 2: Crear `item_plan_ejercicio_sugerido.xml`**

`MaterialCardView` compacta con:
- `TextView text_plan_item_nombre` (16sp bold).
- `TextView text_plan_item_etiqueta` (badge, 12sp, color según prioridad).
- `MaterialButton button_plan_item_agregar` "Agregar a rutina...".

- [ ] **Step 3: Crear `dialog_agregar_ejercicio_a_rutina.xml`**

`AlertDialog` custom con:
- `TextView` "Agregar {nombre ejercicio}" (id `text_agregar_titulo`).
- `TextView` "A qué rutina" + `ChipGroup id chip_group_rutinas_destino` (single-select).
- 4 `TextInputLayout` en horizontal (2×2): series, reps, reps máx, peso (ids `edit_agregar_series/reps/reps_max/peso`).
- `Spinner id spinner_agregar_esquema` con las 5 opciones de esquema.
- Botones "Cancelar" y "Agregar" (ids `button_agregar_cancelar/confirmar`).

- [ ] **Step 4: Crear `PlanSugeridoDialog.java`**

```java
package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.*;
import com.ironquest.mvp.util.metas.*;

public final class PlanSugeridoDialog extends Dialog {

    public interface OnCerrado { void onCerrado(); }

    private final Activity activity;
    private final DataManager dataManager;
    private final Meta meta;
    private final OnCerrado onCerrado;
    private PlanRecomendado plan;

    public static void mostrar(Activity activity, DataManager dm, Meta meta, OnCerrado onCerrado) {
        new PlanSugeridoDialog(activity, dm, meta, onCerrado).show();
    }

    private PlanSugeridoDialog(Activity activity, DataManager dm, Meta meta, OnCerrado onCerrado) {
        super(activity);
        this.activity = activity;
        this.dataManager = dm;
        this.meta = meta;
        this.onCerrado = onCerrado;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_plan_sugerido);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setOnDismissListener(d -> { if (onCerrado != null) onCerrado.onCerrado(); });

        ((TextView) findViewById(R.id.text_plan_meta_titulo)).setText("Plan sugerido: " + meta.getTitulo());

        plan = RecomendadorEjercicios.recomendar(meta, dataManager.getDataStore());
        ((TextView) findViewById(R.id.text_plan_volumen)).setText(plan.getTextoVolumen());
        ((TextView) findViewById(R.id.text_plan_params)).setText(formatearParams(plan));

        TextView cobertura = findViewById(R.id.text_plan_cobertura);
        if (plan.getTextoCoberturaCompleta() != null) {
            cobertura.setVisibility(View.VISIBLE);
            cobertura.setText(plan.getTextoCoberturaCompleta());
        } else {
            cobertura.setVisibility(View.GONE);
        }

        RecyclerView rv = findViewById(R.id.list_plan_ejercicios);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new PlanAdapter(plan.getEjercicios()));

        ((MaterialButton) findViewById(R.id.button_plan_cerrar)).setOnClickListener(v -> dismiss());
    }

    private String formatearParams(PlanRecomendado p) {
        String esquema = nombreEsquema(p.getEsquemaProgresionSugerido());
        String reps = p.getRepsSugeridas() == p.getRepsSugeridasMax()
                ? String.valueOf(p.getRepsSugeridas())
                : p.getRepsSugeridas() + "-" + p.getRepsSugeridasMax();
        return "Ejercicios nuevos: " + p.getSeriesSugeridas() + " × " + reps + " reps · Esquema " + esquema;
    }

    private String nombreEsquema(int e) {
        switch (e) {
            case RutinaEjercicio.ESQUEMA_LINEAL: return "Lineal";
            case RutinaEjercicio.ESQUEMA_GREYSKULL: return "Greyskull";
            case RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION: return "Doble progresión";
            case RutinaEjercicio.ESQUEMA_AUTOMATICO: return "Automático";
            default: return "Ninguno";
        }
    }

    private class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.VH> {
        private final java.util.List<Recomendacion> items;
        PlanAdapter(java.util.List<Recomendacion> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_plan_ejercicio_sugerido, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Recomendacion r = items.get(pos);
            h.nombre.setText(r.getEjercicio().getNombre());
            h.etiqueta.setText(r.getEtiqueta());
            h.etiqueta.setTextColor(r.getPrioridad() == 0 ? 0xFF2E7D32 : 0xFF757575);
            h.boton.setOnClickListener(v -> {
                AgregarEjercicioARutinaDialog.mostrar(
                        activity, dataManager, r.getEjercicio(), plan,
                        () -> notifyItemChanged(pos));  // refrescar etiqueta si aplica
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView nombre, etiqueta;
            MaterialButton boton;
            VH(@NonNull View v) {
                super(v);
                nombre = v.findViewById(R.id.text_plan_item_nombre);
                etiqueta = v.findViewById(R.id.text_plan_item_etiqueta);
                boton = v.findViewById(R.id.button_plan_item_agregar);
            }
        }
    }
}
```

- [ ] **Step 5: Crear `AgregarEjercicioARutinaDialog.java`**

```java
package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.*;
import com.ironquest.mvp.util.metas.PlanRecomendado;
import com.ironquest.mvp.util.metas.RecomendadorEjercicios;

import java.util.List;

public final class AgregarEjercicioARutinaDialog extends Dialog {

    public interface OnAgregado { void onAgregado(); }

    private final Activity activity;
    private final DataManager dataManager;
    private final Ejercicio ejercicio;
    private final PlanRecomendado plan;
    private final OnAgregado onAgregado;

    public static void mostrar(Activity activity, DataManager dm, Ejercicio ejercicio,
                                PlanRecomendado plan, OnAgregado onAgregado) {
        new AgregarEjercicioARutinaDialog(activity, dm, ejercicio, plan, onAgregado).show();
    }

    private AgregarEjercicioARutinaDialog(Activity activity, DataManager dm, Ejercicio ejercicio,
                                           PlanRecomendado plan, OnAgregado onAgregado) {
        super(activity);
        this.activity = activity;
        this.dataManager = dm;
        this.ejercicio = ejercicio;
        this.plan = plan;
        this.onAgregado = onAgregado;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_agregar_ejercicio_a_rutina);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ((TextView) findViewById(R.id.text_agregar_titulo)).setText("Agregar " + ejercicio.getNombre());

        // Chips de rutinas destino
        ChipGroup chips = findViewById(R.id.chip_group_rutinas_destino);
        List<Rutina> rutinas = dataManager.getDataStore().getRutinas();
        for (Rutina r : rutinas) {
            Chip c = new Chip(getContext());
            c.setText(r.getNombre());
            c.setCheckable(true);
            c.setTag(r.getId());
            chips.addView(c);
        }
        if (chips.getChildCount() > 0) {
            ((Chip) chips.getChildAt(0)).setChecked(true);
            if (chips.getChildCount() == 1) chips.getChildAt(0).setEnabled(false);
        }

        // Pre-llenar valores del plan
        ((EditText) findViewById(R.id.edit_agregar_series)).setText(String.valueOf(plan.getSeriesSugeridas()));
        ((EditText) findViewById(R.id.edit_agregar_reps)).setText(String.valueOf(plan.getRepsSugeridas()));
        ((EditText) findViewById(R.id.edit_agregar_reps_max)).setText(String.valueOf(plan.getRepsSugeridasMax()));
        double pesoInicial = RecomendadorEjercicios.pesoInicialSugerido(
                ejercicio.getId(), dataManager.getDataStore().getSesiones());
        ((EditText) findViewById(R.id.edit_agregar_peso)).setText(pesoInicial > 0
                ? formatNum(pesoInicial) : "");

        Spinner sp = findViewById(R.id.spinner_agregar_esquema);
        String[] labels = {"Ninguno", "Lineal", "Greyskull (AMRAP)", "Doble progresión", "Automático"};
        int[] valores = {
                RutinaEjercicio.ESQUEMA_NINGUNO, RutinaEjercicio.ESQUEMA_LINEAL,
                RutinaEjercicio.ESQUEMA_GREYSKULL, RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION,
                RutinaEjercicio.ESQUEMA_AUTOMATICO
        };
        ArrayAdapter<String> a = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, labels);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] == plan.getEsquemaProgresionSugerido()) { sp.setSelection(i, false); break; }
        }

        findViewById(R.id.button_agregar_cancelar).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_agregar_confirmar).setOnClickListener(v ->
                confirmar(chips, valores));
    }

    private void confirmar(ChipGroup chips, int[] valoresEsquema) {
        String rutinaId = null;
        for (int i = 0; i < chips.getChildCount(); i++) {
            Chip c = (Chip) chips.getChildAt(i);
            if (c.isChecked()) { rutinaId = (String) c.getTag(); break; }
        }
        if (rutinaId == null) {
            Toast.makeText(getContext(), "Selecciona una rutina", Toast.LENGTH_SHORT).show();
            return;
        }
        Rutina rutina = dataManager.getDataStore().buscarRutina(rutinaId);
        if (rutina == null) { dismiss(); return; }

        int series = parseInt(R.id.edit_agregar_series, 3);
        int reps = parseInt(R.id.edit_agregar_reps, 8);
        int repsMax = parseInt(R.id.edit_agregar_reps_max, reps);
        double peso = parseDouble(R.id.edit_agregar_peso, 0);
        int esquema = valoresEsquema[((Spinner) findViewById(R.id.spinner_agregar_esquema)).getSelectedItemPosition()];

        RutinaEjercicio re = new RutinaEjercicio(ejercicio.getId(), series, reps, peso);
        re.setRepeticionesMax(repsMax);
        re.setEsquemaProgresion(esquema);
        rutina.agregarEjercicio(re);
        dataManager.saveSync();
        dataManager.saveCatalogo();  // el ejercicio ya estaba, no cambia — pero es defensivo

        Toast.makeText(getContext(), "Agregado a " + rutina.getNombre(), Toast.LENGTH_SHORT).show();
        if (onAgregado != null) onAgregado.onAgregado();
        dismiss();
    }

    private int parseInt(int id, int def) {
        try { return Integer.parseInt(((EditText) findViewById(id)).getText().toString().trim()); }
        catch (Exception e) { return def; }
    }

    private double parseDouble(int id, double def) {
        try { return Double.parseDouble(((EditText) findViewById(id)).getText().toString().trim()); }
        catch (Exception e) { return def; }
    }

    private String formatNum(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}
```

- [ ] **Step 6: Enganchar en `MetasFragment`**

Reemplazar `abrirPlanSugerido`:

```java
private void abrirPlanSugerido(Meta meta) {
    PlanSugeridoDialog.mostrar(requireActivity(), dataManager, meta, this::refrescar);
}
```

- [ ] **Step 7: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Con una meta ya creada (usar Task 8), tap "Ver plan sugerido" → aparece diálogo con el texto de volumen, los parámetros y la lista de ejercicios. Tap "Agregar a rutina..." → diálogo con pre-llenado. Confirmar → Toast + el ejercicio queda agregado a la rutina (verificar en el editor de esa rutina).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/PlanSugeridoDialog.java app/src/main/java/com/ironquest/mvp/ui/AgregarEjercicioARutinaDialog.java app/src/main/java/com/ironquest/mvp/ui/MetasFragment.java app/src/main/res/layout/dialog_plan_sugerido.xml app/src/main/res/layout/dialog_agregar_ejercicio_a_rutina.xml app/src/main/res/layout/item_plan_ejercicio_sugerido.xml
git commit -m "Añadir PlanSugeridoDialog y AgregarEjercicioARutinaDialog con pre-llenado por meta"
```

---

### Task 10: `EditarMetaDialog`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/ui/EditarMetaDialog.java`
- Create: `app/src/main/res/layout/dialog_editar_meta.xml`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/MetasFragment.java`

**Interfaces:**
- Consumes: `Meta` (Task 1), `DataManager.save()`.
- Produces: `EditarMetaDialog.mostrar(Activity, DataManager, Meta, Runnable onCambio)` — permite editar título, objetivo y deadline. NO cambia el tipo ni la medida (para eso el usuario descarta y crea otra).

- [ ] **Step 1: Crear `dialog_editar_meta.xml`**

`AlertDialog` custom simple con `LinearLayout` vertical padding 20dp:
- `TextView` "Editar meta" (headline).
- `TextInputLayout` con `edit_editar_titulo`.
- `TextInputLayout` con `edit_editar_objetivo` (label dinámico "cm" o "kg" según tipo).
- `CheckBox` "Fecha objetivo" + botón "Elegir fecha" con label.
- Botones "Cancelar" / "Guardar".

- [ ] **Step 2: Crear `EditarMetaDialog.java`**

```java
package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.Meta;

import java.time.LocalDate;

public final class EditarMetaDialog extends Dialog {

    public interface OnCambio { void onCambio(); }

    private final Activity activity;
    private final DataManager dataManager;
    private final Meta meta;
    private final OnCambio onCambio;
    private String fechaIso;

    public static void mostrar(Activity a, DataManager dm, Meta m, OnCambio onCambio) {
        new EditarMetaDialog(a, dm, m, onCambio).show();
    }

    private EditarMetaDialog(Activity a, DataManager dm, Meta m, OnCambio onCambio) {
        super(a);
        this.activity = a; this.dataManager = dm; this.meta = m; this.onCambio = onCambio;
        this.fechaIso = m.getFechaObjetivo();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_editar_meta);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText titulo = findViewById(R.id.edit_editar_titulo);
        titulo.setText(meta.getTitulo());

        EditText objetivo = findViewById(R.id.edit_editar_objetivo);
        double valObj = meta.getTipo() == Meta.TIPO_PR_EJERCICIO
                ? meta.getPesoObjetivoKg() : meta.getValorObjetivo();
        objetivo.setText(valObj == Math.floor(valObj) ? String.valueOf((long) valObj) : String.valueOf(valObj));

        CheckBox check = findViewById(R.id.check_editar_deadline);
        TextView fechaLabel = findViewById(R.id.text_editar_fecha_elegida);
        View botonFecha = findViewById(R.id.button_editar_fecha);

        check.setChecked(fechaIso != null && !fechaIso.isEmpty());
        botonFecha.setEnabled(check.isChecked());
        fechaLabel.setText(fechaIso != null ? fechaIso : "");

        check.setOnCheckedChangeListener((v, isChecked) -> {
            botonFecha.setEnabled(isChecked);
            if (!isChecked) { fechaIso = null; fechaLabel.setText(""); }
        });
        botonFecha.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Fecha objetivo").build();
            picker.addOnPositiveButtonClickListener(millis -> {
                LocalDate f = LocalDate.ofEpochDay(millis / 86_400_000L);
                fechaIso = f.toString();
                fechaLabel.setText(fechaIso);
            });
            picker.show(((androidx.fragment.app.FragmentActivity) activity).getSupportFragmentManager(), "editar_fecha");
        });

        findViewById(R.id.button_editar_cancelar).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_editar_guardar).setOnClickListener(v -> {
            meta.setTitulo(titulo.getText().toString().trim());
            double nuevoObj;
            try { nuevoObj = Double.parseDouble(objetivo.getText().toString().trim()); }
            catch (Exception e) { Toast.makeText(getContext(), "Objetivo inválido", Toast.LENGTH_SHORT).show(); return; }
            if (meta.getTipo() == Meta.TIPO_PR_EJERCICIO) meta.setPesoObjetivoKg(nuevoObj);
            else meta.setValorObjetivo(nuevoObj);
            meta.setFechaObjetivo(fechaIso);
            dataManager.save();
            if (onCambio != null) onCambio.onCambio();
            dismiss();
        });
    }
}
```

- [ ] **Step 3: Enganchar en `MetasFragment.abrirEditar`**

```java
private void abrirEditar(Meta meta) {
    EditarMetaDialog.mostrar(requireActivity(), dataManager, meta, this::refrescar);
}
```

- [ ] **Step 4: Compilar y verificar en dispositivo**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/EditarMetaDialog.java app/src/main/java/com/ironquest/mvp/ui/MetasFragment.java app/src/main/res/layout/dialog_editar_meta.xml
git commit -m "Añadir EditarMetaDialog para modificar título, objetivo y deadline"
```

---

### Task 11: `CelebracionMetaDialog` + triggers de `evaluarMetas()` en PhysicalProfile y ActiveSession

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/ui/CelebracionMetaDialog.java`
- Create: `app/src/main/res/layout/dialog_celebracion_meta.xml`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/PhysicalProfileActivity.java`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java`

**Interfaces:**
- Consumes: `DataManager.evaluarMetas()` (Task 3).
- Produces: `CelebracionMetaDialog.mostrar(Activity, List<Meta>)` — muestra diálogo con emoji, lista y vibración.

- [ ] **Step 1: Crear `dialog_celebracion_meta.xml`**

`LinearLayout` vertical centrado con:
- `TextView` "🏆" (id `text_celebracion_emoji`, textSize 48sp).
- `TextView` "¡Meta cumplida!" (id `text_celebracion_titulo`, headline).
- `TextView` id `text_celebracion_lista` (una línea por meta cumplida).
- Botones "Ver mis metas" (id `button_celebracion_ver`) y "Continuar" (id `button_celebracion_continuar`).

- [ ] **Step 2: Crear `CelebracionMetaDialog.java`**

```java
package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.*;
import android.widget.TextView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Meta;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class CelebracionMetaDialog extends Dialog {

    private final List<Meta> cumplidas;

    public static void mostrar(Activity activity, List<Meta> cumplidas) {
        if (cumplidas == null || cumplidas.isEmpty()) return;
        new CelebracionMetaDialog(activity, cumplidas).show();
    }

    private CelebracionMetaDialog(Activity activity, List<Meta> cumplidas) {
        super(activity);
        this.cumplidas = cumplidas;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_celebracion_meta);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setCancelable(false);

        TextView lista = findViewById(R.id.text_celebracion_lista);
        StringBuilder sb = new StringBuilder();
        for (Meta m : cumplidas) {
            sb.append("• ").append(m.getTitulo()).append("\n");
            sb.append("   ").append(duracion(m)).append("\n\n");
        }
        lista.setText(sb.toString().trim());

        findViewById(R.id.button_celebracion_continuar).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_celebracion_ver).setOnClickListener(v -> {
            Intent i = new Intent(getContext(), MainActivity.class);
            i.putExtra(MetasFragment.EXTRA_META_DESTACADA_ID, cumplidas.get(0).getId());
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            dismiss();
        });

        Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vib != null && vib.hasVibrator()) {
            vib.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private static String duracion(Meta m) {
        try {
            LocalDateTime c = LocalDateTime.parse(m.getFechaCreacion());
            LocalDateTime f = LocalDateTime.parse(m.getFechaCumplida());
            long dias = ChronoUnit.DAYS.between(c.toLocalDate(), f.toLocalDate());
            if (dias == 0) return "En un día";
            if (dias < 14) return "En " + dias + " días";
            long semanas = dias / 7;
            long restoDias = dias % 7;
            return "En " + semanas + " semanas" + (restoDias > 0 ? " y " + restoDias + " días" : "");
        } catch (Exception e) { return ""; }
    }
}
```

- [ ] **Step 3: Trigger en `PhysicalProfileActivity`**

Buscar dónde se guarda el registro (línea 155 según grep del contexto — método que hace `dataManager.save()`). Después de ese save, agregar:

```java
java.util.List<com.ironquest.mvp.model.Meta> cumplidas = dataManager.evaluarMetas();
if (!cumplidas.isEmpty()) {
    CelebracionMetaDialog.mostrar(this, cumplidas);
}
```

- [ ] **Step 4: Trigger en `ActiveSessionActivity.finalizarSesion()`**

Después de `generarSugerenciasPendientes();` (línea 351 según contexto) y antes de armar el Intent al SessionSummary:

```java
java.util.List<com.ironquest.mvp.model.Meta> cumplidas = dataManager.evaluarMetas();
```

La celebración se muestra desde `SessionSummaryActivity` en lugar de `ActiveSessionActivity` (porque esta última hace `finish()` inmediato). Pasar por Intent:

```java
if (!cumplidas.isEmpty()) {
    java.util.ArrayList<String> ids = new java.util.ArrayList<>();
    for (com.ironquest.mvp.model.Meta m : cumplidas) ids.add(m.getId());
    intent.putStringArrayListExtra(SessionSummaryActivity.EXTRA_METAS_CUMPLIDAS_IDS, ids);
}
```

Y en `SessionSummaryActivity.java`, agregar:

```java
public static final String EXTRA_METAS_CUMPLIDAS_IDS = "extra_metas_cumplidas_ids";
```

En `onCreate` de `SessionSummaryActivity`, al final:

```java
java.util.ArrayList<String> metasIds = getIntent().getStringArrayListExtra(EXTRA_METAS_CUMPLIDAS_IDS);
if (metasIds != null && !metasIds.isEmpty()) {
    java.util.List<com.ironquest.mvp.model.Meta> metas = new java.util.ArrayList<>();
    for (String id : metasIds) {
        for (com.ironquest.mvp.model.Meta m : dataManager.getDataStore().getMetas()) {
            if (id.equals(m.getId())) { metas.add(m); break; }
        }
    }
    CelebracionMetaDialog.mostrar(this, metas);
}
```

- [ ] **Step 5: Compilar y verificar en dispositivo**

Crear una meta con umbral muy bajo (ej. "Aumentar brazo a X cm" donde X = medida actual), luego guardar un `RegistroFisico` con la medida al mismo valor → debe aparecer el diálogo con vibración.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/CelebracionMetaDialog.java app/src/main/java/com/ironquest/mvp/ui/PhysicalProfileActivity.java app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java app/src/main/java/com/ironquest/mvp/ui/SessionSummaryActivity.java app/src/main/res/layout/dialog_celebracion_meta.xml
git commit -m "Añadir celebración de meta cumplida con triggers en Mi físico y fin de sesión"
```

---

### Task 12: `NotificacionMetasHelper` (canal + notify de ≥80 %)

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/service/NotificacionMetasHelper.java`
- Modify: `app/src/main/java/com/ironquest/mvp/data/DataManager.java` (descomentar la llamada de la Task 3)
- Modify: `app/src/main/java/com/ironquest/mvp/ui/MainActivity.java` (manejar deeplink)

**Interfaces:**
- Consumes: `Meta` (Task 1).
- Produces: `NotificacionMetasHelper.notificarCerca(Context, Meta)`.

- [ ] **Step 1: Crear `NotificacionMetasHelper.java`**

```java
package com.ironquest.mvp.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.ui.MainActivity;
import com.ironquest.mvp.ui.MetasFragment;

/**
 * Dispara una notificación única cuando una Meta pasa de <80% a ≥80% de progreso. Reusa el
 * permiso POST_NOTIFICATIONS que ya solicita SesionTrackingService. Si el permiso no está
 * concedido, notify() falla silenciosamente.
 */
public final class NotificacionMetasHelper {

    private static final String CANAL_ID = "metas_progreso";

    private NotificacionMetasHelper() {}

    public static void notificarCerca(Context context, Meta meta) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        crearCanalSiHaceFalta(nm);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MetasFragment.EXTRA_META_DESTACADA_ID, meta.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getActivity(context,
                meta.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)   // reusar existente
                .setContentTitle("Estás cerca de cumplir tu meta")
                .setContentText(meta.getTitulo() + " — 80 % de progreso. ¡Sigue así!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(meta.getTitulo() + " — 80 % de progreso. ¡Sigue así!"))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            nm.notify(meta.getId().hashCode(), b.build());
        } catch (SecurityException ignored) {
            // Sin permiso POST_NOTIFICATIONS en Android 13+ → falla silenciosa.
        }
    }

    private static void crearCanalSiHaceFalta(NotificationManager nm) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        if (nm.getNotificationChannel(CANAL_ID) != null) return;
        NotificationChannel canal = new NotificationChannel(CANAL_ID,
                "Progreso de metas", NotificationManager.IMPORTANCE_DEFAULT);
        canal.setDescription("Avisa cuando estás cerca de cumplir una meta.");
        nm.createNotificationChannel(canal);
    }
}
```

- [ ] **Step 2: Descomentar la llamada en `DataManager.evaluarMetas()`**

Si en Task 3 se dejó comentada la línea de `NotificacionMetasHelper`, descomentarla ahora:

```java
} else if (!m.isNotificadaCerca() && ev.calcularProgreso(m, dataStore) >= 0.80) {
    m.setNotificadaCerca(true);
    com.ironquest.mvp.service.NotificacionMetasHelper.notificarCerca(appContext, m);
    cambio = true;
}
```

- [ ] **Step 3: Manejar deeplink en `MainActivity`**

En `MainActivity.onCreate` (o `onNewIntent`), detectar el extra y pasarlo a `MetasFragment`:

```java
String metaDestacada = getIntent().getStringExtra(MetasFragment.EXTRA_META_DESTACADA_ID);
if (metaDestacada != null) {
    // Cambiar a la pestaña Metas (índice según la ViewPager2 del proyecto)
    // Buscar por qué índice se instancia MetasFragment.crear(...) — típicamente el último tab.
    viewPager.setCurrentItem(INDICE_METAS, false);
    // Pasar el extra al fragment vía Bundle
    Bundle args = new Bundle();
    args.putString(MetasFragment.EXTRA_META_DESTACADA_ID, metaDestacada);
    // ...requiere adaptar el FragmentPagerAdapter para pasar args al MetasFragment
}
```

**Nota práctica:** el proyecto usa un `FragmentStateAdapter` estático que instancia `MetasFragment.crear(...)`. Big pickle adapta esto según el patrón exacto del `MainActivity` — probablemente cambiar `MetasFragment.crear()` a `MetasFragment.crear(metaDestacada)` cuando el extra esté presente, y el fragment consume el argumento en `onCreateView`.

- [ ] **Step 4: Compilar y verificar en dispositivo**

Crear una meta cerca del cumplimiento (ej. brazo actual 32, objetivo 33). Guardar un `RegistroFisico` con brazo=32.8 (≥80% de progreso) → notificación aparece. Tap → abre app en tab Metas con la meta resaltada por 3 s.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/service/NotificacionMetasHelper.java app/src/main/java/com/ironquest/mvp/data/DataManager.java app/src/main/java/com/ironquest/mvp/ui/MainActivity.java
git commit -m "Añadir notificación push única al cruzar 80% de progreso en una meta"
```

---

### Task 13: Banner de meta activa en `EditRoutineActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java`
- Modify: `app/src/main/res/layout/activity_edit_routine.xml`

**Interfaces:**
- Consumes: `DataManager.getMetasActivas()`, `RecomendadorEjercicios.recomendar()`, `PlanRecomendado.tieneAlgunoFaltante()`, `PlanSugeridoDialog` (Task 9).

- [ ] **Step 1: Agregar banner en `activity_edit_routine.xml`**

Encima del banner de límite muscular del Slice 2 (`card_banner_limite_grupo`):

```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card_banner_meta"
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
            android:id="@+id/text_banner_meta_titulo"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceTitleSmall" />

        <TextView
            android:id="@+id/text_banner_meta_volumen"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodyMedium"
            android:layout_marginTop="4dp" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="end"
            android:layout_marginTop="8dp">
            <com.google.android.material.button.MaterialButton
                android:id="@+id/button_banner_meta_ocultar"
                style="?attr/materialButtonOutlinedStyle"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Ocultar"
                android:layout_marginEnd="8dp" />
            <com.google.android.material.button.MaterialButton
                android:id="@+id/button_banner_meta_ver_plan"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Ver plan" />
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 2: Wire-up en `EditRoutineActivity.java`**

Nueva variable en la Activity:

```java
private boolean bannerMetaOculto = false;
```

Método nuevo:

```java
private void actualizarBannerMeta() {
    View card = findViewById(R.id.card_banner_meta);
    if (bannerMetaOculto) { card.setVisibility(View.GONE); return; }

    java.util.List<com.ironquest.mvp.model.Meta> activas = dataManager.getMetasActivas();
    com.ironquest.mvp.model.Meta metaConBrecha = null;
    com.ironquest.mvp.util.metas.PlanRecomendado planMeta = null;
    double peorProgreso = 1.0;
    for (com.ironquest.mvp.model.Meta m : activas) {
        com.ironquest.mvp.util.metas.PlanRecomendado p =
                com.ironquest.mvp.util.metas.RecomendadorEjercicios.recomendar(m, dataStore);
        if (!p.tieneAlgunoFaltante()) continue;
        double prog = com.ironquest.mvp.util.metas.EvaluadorMeta.para(m).calcularProgreso(m, dataStore);
        if (prog < peorProgreso) { peorProgreso = prog; metaConBrecha = m; planMeta = p; }
    }
    if (metaConBrecha == null) { card.setVisibility(View.GONE); return; }
    card.setVisibility(View.VISIBLE);
    ((TextView) findViewById(R.id.text_banner_meta_titulo))
            .setText("Meta activa: \"" + metaConBrecha.getTitulo() + "\"");
    ((TextView) findViewById(R.id.text_banner_meta_volumen)).setText(planMeta.getTextoVolumen());

    final com.ironquest.mvp.model.Meta metaFinal = metaConBrecha;
    findViewById(R.id.button_banner_meta_ver_plan).setOnClickListener(v ->
            PlanSugeridoDialog.mostrar(this, dataManager, metaFinal, this::actualizarBannerMeta));
    findViewById(R.id.button_banner_meta_ocultar).setOnClickListener(v -> {
        bannerMetaOculto = true;
        card.setVisibility(View.GONE);
    });
}
```

Llamar `actualizarBannerMeta()` desde `onCreate` (después de setup del adapter) y desde `onResume`.

- [ ] **Step 3: Compilar y verificar en dispositivo**

Con una meta activa que tenga brecha, abrir edit rutina → banner aparece con el volumen y botón "Ver plan". Confirmar que "Ocultar" lo esconde hasta reabrir la Activity.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java app/src/main/res/layout/activity_edit_routine.xml
git commit -m "Añadir banner de meta activa con brecha en EditRoutineActivity"
```

---

### Task 14: Verificación end-to-end, bump a v0.11.0 y release

**Files:**
- Modify: `app/build.gradle`

**Contexto:** protocolo idéntico a las Tasks 14 de auto-progresión (v0.9.0) y 12 de fatiga (v0.10.0). Bump a **v0.11.0**.

- [ ] **Step 1: Respaldar `datos.json` real**

```bash
ADB=/home/jdov/Android/Sdk/platform-tools/adb
mkdir -p /tmp/titan-metas
$ADB shell run-as com.ironquest.mvp cat files/datos.json > /tmp/titan-metas/respaldo_pre_v0.11.0.json
$ADB shell run-as com.ironquest.mvp cat files/catalogo_local.json > /tmp/titan-metas/respaldo_catalogo_pre_v0.11.0.json
ls -la /tmp/titan-metas/*.json
```

Esperado: ambos > 0 bytes.

- [ ] **Step 2: Bump versión**

En `app/build.gradle`:

```gradle
versionCode 13
versionName "0.11.0"
```

- [ ] **Step 3: Build de release**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
echo "Exit: ${PIPESTATUS[0]}"
ls -la app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 4: Chequeo del CLAUDE.md**

```bash
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Esperado: vacío.

- [ ] **Step 5: Instalar y checklist manual**

```bash
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB shell dumpsys package com.ironquest.mvp | grep versionName
```

Esperado: `versionName=0.11.0`.

Checklist manual en el teléfono (marcar cada uno):

- [ ] App arranca sin crash.
- [ ] Tab Metas muestra estado vacío la primera vez.
- [ ] Ajustes: bloque "Días a la semana que entrenas" con default 3, cambio persiste.
- [ ] Crear meta muscular (brazo 32 → 33 cm). Card aparece con progreso.
- [ ] Guardar un `RegistroFisico` con brazo=33 → celebración aparece + card pasa al historial.
- [ ] Crear meta con salto > 20 % → advertencia aparece con botones Ajustar / Crear igual.
- [ ] Con 3 metas activas, tap "+" muestra Toast "Cumple o descarta una meta antes de crear otra".
- [ ] Descartar meta → card pasa al historial marcada como Descartada.
- [ ] Ver plan sugerido: card con volumen semanal, params y lista de ejercicios ordenados (faltantes primero).
- [ ] Agregar ejercicio a rutina → pre-llenado correcto, se agrega y persiste tras reinstalar.
- [ ] Rutina con cobertura completa → mensaje "ya tienes buena cobertura" en vez de la lista.
- [ ] Banner en EditRoutineActivity con meta activa que tenga brecha → aparece con volumen; "Ocultar" lo esconde.
- [ ] Meta con progreso ≥ 80 % → notificación push (una sola vez). Tap → app abre en Metas con la meta resaltada.
- [ ] Recordatorio pasivo "actualiza tu medida" aparece si el último RegistroFisico con esa medida > 14 días atrás.

- [ ] **Step 6: Restaurar `datos.json` real si se usaron datos de prueba**

Idéntico al patrón de v0.10.1.

- [ ] **Step 7: Commit del bump**

```bash
git add app/build.gradle
git commit -m "Subir versión a 0.11.0 (versionCode 13)"
```

- [ ] **Step 8: Pedir aprobación del usuario para push y release**

Presentar commits desde `v0.10.1`. Preguntar: "¿Autorizas el push a `origin/main` y el release `v0.11.0`?"

- [ ] **Step 9: Push y release (solo con autorización)**

```bash
git push origin main
```

Notas de release en `/tmp/titan-metas/notas_v0.11.0.md`:

```markdown
## v0.11.0 — Sistema de metas

Nuevo tab **Metas** con soporte para 4 tipos de meta:
- Aumentar medida muscular (brazo, pierna, pecho).
- Reducir cintura o cadera.
- Alcanzar un peso corporal objetivo (subir o bajar).
- Lograr un PR en un ejercicio (peso × reps).

Cada meta tiene:
- **Validación de plausibilidad**: si el objetivo es poco realista o riesgoso según tus
  datos (IMC, ICC, deltas relativos), la app advierte antes de crearla — pero tú decides.
- **Progreso automático**: la app detecta cuando llegas al objetivo (al guardar un
  registro físico o cerrar una sesión) y te felicita.
- **Notificación cuando estás cerca**: al pasar del 80 % de progreso te avisa una vez.
- **Recomendador de ejercicios**: tap "Ver plan" muestra volumen semanal recomendado,
  series/reps sugeridas y ejercicios que te faltan en tus rutinas para acelerar la meta.
- **Deadline opcional**: si le pones fecha, la app te muestra los días que faltan.

Además:
- Nuevo campo en Ajustes: "Días a la semana que entrenas". Alimenta al recomendador.
- Banner en el editor de rutina cuando una meta activa tiene ejercicios sugeridos por
  agregar.
```

Copiar y publicar:

```bash
cp app/build/outputs/apk/debug/app-debug.apk /tmp/titan-metas/titan-score.apk
gh release create v0.11.0 /tmp/titan-metas/titan-score.apk \
    --repo JuanOV27/titan-score-app \
    --title "v0.11.0 — Sistema de metas" \
    --notes-file /tmp/titan-metas/notas_v0.11.0.md
gh release view v0.11.0 --repo JuanOV27/titan-score-app
```

- [ ] **Step 10: Confirmar al usuario**

Reportar URL del release, tamaño del APK, commits pusheados.

---

## Self-Review

**Cobertura del spec (2026-09-16-sistema-metas-design.md):**

- Entidad `Meta` con constantes y todos los campos → Task 1.
- `Usuario.diasEntrenoSemana` default 3 → Task 1.
- `DataStore.metas` + normalización → Task 1.
- CRUD de metas en `DataManager` + límite 3 → Task 2.
- Motor polimórfico `EvaluadorMeta` + 4 subclases → Task 3.
- `evaluarMetas()` en `DataManager` con detección de cumplimiento y notificación ≥80 % → Task 3 (+ enganche real en Task 12).
- `ValidadorMeta` con reglas por tipo + `Advertencia` → Task 4.
- Sin campo de sexo → umbral neutro ICC 0.72 (implementado en Task 4).
- `RecomendadorEjercicios` con `CURADOS`, `RANGO_VOLUMEN`, `ParamsProgresion`, `PlanRecomendado`, `Recomendacion`, matching por keywords para PR, fallback por músculo → Task 5.
- `seriesSemanalesDeGrupo` con distribución uniforme → Task 5.
- Nombres verificados contra `catalogo.json` real → hecho por Claude antes de escribir este plan.
- Bloque `diasEntrenoSemana` en `AjustesActivity` → Task 6.
- `MetasFragment` reescrito con adapters de activas + historial + estado vacío → Task 7.
- Card de meta activa con progreso, deadline, recordatorio pasivo, overflow menu → Task 7.
- Card de historial compacto → Task 7.
- `CrearMetaDialog` con formularios dinámicos + validación → Task 8.
- `PlanSugeridoDialog` + `AgregarEjercicioARutinaDialog` → Task 9.
- Cobertura completa (mensaje alternativo) → Task 9 (parte del PlanSugeridoDialog).
- `EditarMetaDialog` → Task 10.
- `CelebracionMetaDialog` + triggers en PhysicalProfile + ActiveSession → Task 11.
- `NotificacionMetasHelper` con canal + deeplink → Task 12.
- Banner de meta con brecha en `EditRoutineActivity` → Task 13.
- Verificación + release v0.11.0 → Task 14.

**Consistencia de tipos:**

- `Meta` constantes públicas `TIPO_*` y `ESTADO_*` usadas consistentemente entre Task 1 (definición), Tasks 2, 3, 5, 8, 11 (consumo).
- `EvaluadorMeta.para(Meta)` firma coincide entre Task 3 y Tasks 7, 13.
- `RecomendadorEjercicios.recomendar(Meta, DataStore)` retorna `PlanRecomendado` — coincide entre Task 5, 9, 13.
- `PlanRecomendado.tieneAlgunoFaltante()` referenciado en Task 13, definido en Task 5.
- `NotificacionMetasHelper.notificarCerca(Context, Meta)` referenciado en Task 3, definido en Task 12 (con nota explícita del orden).
- `MetasFragment.EXTRA_META_DESTACADA_ID` referenciado en Tasks 11 y 12, definido en Task 7.
- `SessionSummaryActivity.EXTRA_METAS_CUMPLIDAS_IDS` definido y consumido en Task 11.
- `RutinaEjercicio.ESQUEMA_*` — constantes existentes desde el Slice 1 (v0.9.0), referenciadas en Task 5, 9.

**Placeholders detectados y aceptables (delegaciones acotadas):**

- Task 7 y 12 mencionan "el patrón exacto de MainActivity para pasar args al fragment" — es una consulta al código existente, no un TBD.
- Task 8 dice "big pickle expande siguiendo el patrón de otros diálogos existentes" para los layouts pequeños — el ejemplo grande está entero; los form_*.xml son variaciones triviales del muscular.
- Task 9 layouts (dialog_plan_sugerido, item_plan_ejercicio_sugerido, dialog_agregar_ejercicio_a_rutina) tienen descripción de estructura con ids exactos pero no XML completo. Big pickle sigue el patrón visible en otros diálogos del proyecto (dialog_info_progresion, card_sugerencias). Ninguna decisión de diseño abierta.

No hay TBD, TODO, "implementar después" ni referencias a símbolos inexistentes.

---

## Handoff a big pickle

Este plan tiene **14 tareas**, ninguna con dependencia externa. Ejecutarlas en orden.
Cada tarea produce un commit propio. Big pickle NO tiene que decidir arquitectura ni
resolver ambigüedades — todas las decisiones están fijadas en el spec y en este plan.

Antes de arrancar Task 1, big pickle debe leer:
- `IronQuestApp/CLAUDE.md` (invariantes del proyecto).
- `IronQuestApp/docs/specs/2026-09-16-sistema-metas-design.md` (spec de referencia).
- Este archivo completo.

Cuando termine cada task: mostrar diff, esperar `git commit` que aparece en cada Step X.

La Task 14 pausa antes del push y del release, esperando autorización explícita del
usuario (como establece la memoria [confirmar-antes-de-push](/home/jdov/.claude/projects/-home-jdov-Documentos-titan-score/memory/confirmar-antes-de-push.md)).


