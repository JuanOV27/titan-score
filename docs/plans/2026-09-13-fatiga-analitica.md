# Fatiga capturada y analítica muscular — plan de ejecución (Slice 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **En este proyecto** el ejecutor por defecto es **big pickle** (OpenCode). Cada tarea es autocontenida.

> **⚠ Este plan depende del Slice 1** (`2026-09-13-auto-progresion.md`). No arrancar antes de que el motor automático + `ProgresionAutomatica` + `AjustesActivity` estén en `main` y verificados en dispositivo.

**Goal:** Sumar captura de fatiga por serie (chips Fácil/Justo/Duro/Al fallo + RIR opcional), refinar el motor `ProgresionAutomatica` para consumir RIR cuando esté disponible, agregar pie chart de distribución muscular en resumen post-sesión y en `StatsFragment`, y avisos preventivos de exceso de trabajo por grupo muscular (editor, arranque de sesión y detección dinámica). Todo opt-in vía un solo toggle global.

**Architecture:** Nuevo campo primitivo `SerieSesion.rir` y `Usuario.seguimientoFatiga`. Nueva utilidad estática `util/AnalisisMuscular` con ponderación primario 1.0 + secundarios comparten 0.5. Nueva `PieChartView` casera (sin dependencias). Extensión de `ProgresionAutomatica` con overrides RIR. Chips por serie en `EjercicioSesionPagerAdapter`. Banners y cards en `EditRoutineActivity`, `ActiveSessionActivity`, `SessionSummaryActivity`, `StatsFragment` y `AjustesActivity`.

**Tech Stack:** Java 17, Android SDK 26–37, Material3, sin dependencias nuevas.

## Global Constraints

- **Dominio en español, API Android en inglés.**
- **Nada fuera de `data/` importa Gson/File/FileWriter/FileReader ni llama a `getFilesDir()`.** Chequeo:
  ```bash
  grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
  ```
  Debe seguir vacío.
- **Campos primitivos nuevos son siempre seguros contra `datos.json` viejo** (Trampa #3). `SerieSesion.rir = -1` en sesiones viejas por defecto (no capturado).
- **Textos hardcodeados en Java y layouts.**
- **No introducir dependencias nuevas** (ni MPAndroidChart, ni gráficas librerías). `PieChartView` se dibuja con `Canvas` como la `BarChartView` existente.
- **`CATALOGO_VERSION_ACTUAL` no se toca.**
- **Ejercicios personalizados sin `musculoObjetivo` agrupan bajo `"Otros"`** en el pie chart.
- **El toggle es global**: `Usuario.seguimientoFatiga`. No hay toggle por rutina ni por sesión.
- **Ponderación fija**: primario `1.0`, secundarios comparten `0.5 / n_secundarios`. Presupuesto por serie = 150%.
- **Umbral fijo**: `LIMITE_POR_GRUPO = 4` en `AnalisisMuscular`. No configurable en UI.
- **Refinamiento RIR solo aplica a `ProgresionAutomatica`.** Lineal, Greyskull y Doble no cambian.
- **Detección dinámica requiere el toggle activo.** Sin RIR no hay señal.
- **Datos reales del teléfono:** respaldar antes de instalar.
- **Commits en español, imperativos, una línea.**

---

### Task 1: Campos nuevos en `SerieSesion` y `Usuario`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/model/SerieSesion.java`
- Modify: `app/src/main/java/com/ironquest/mvp/model/Usuario.java`

**Interfaces:**
- Produces:
  - `SerieSesion.getRir()` → `int`, `setRir(int)`.
  - Nuevo constructor overload `SerieSesion(int numero, double peso, int repeticiones, boolean completada, int rir)` (opcional, si simplifica callers; si no, dejar solo el setter).
  - `Usuario.isSeguimientoFatiga()` → `boolean`, `setSeguimientoFatiga(boolean)`.

- [ ] **Step 1: Agregar campo `rir` en `SerieSesion.java`**

En [SerieSesion.java](../../app/src/main/java/com/ironquest/mvp/model/SerieSesion.java), agregar el campo (junto a los demás):

```java
/**
 * Reps-in-reserve capturado por el usuario tras la serie. {@code -1} = no capturado.
 * Escala 0-5: 0 = al fallo o con ayuda, 5 = con mucho margen. Solo se muestra si
 * {@link Usuario#isSeguimientoFatiga()} está activo. Primitivo por Trampa #3: sesiones
 * viejas leen -1 sin migración.
 */
private int rir = -1;
```

Getter/setter estándar. **No** cambiar el constructor público existente ni el privado de Gson — el default `-1` cubre el caso de sesiones antiguas.

- [ ] **Step 2: Agregar campo `seguimientoFatiga` en `Usuario.java`**

En [Usuario.java](../../app/src/main/java/com/ironquest/mvp/model/Usuario.java):

```java
/** Toggle global opcional. Activa chips de fatiga por serie y detección dinámica. */
private boolean seguimientoFatiga;
```

Getter/setter `isSeguimientoFatiga()` / `setSeguimientoFatiga(boolean)`.

- [ ] **Step 3: Compilar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
grep -rn "Gson\|FileWriter\|FileReader\|getFilesDir" --include=*.java app/src/main/java | grep -v "/data/"
```

Esperado: `BUILD SUCCESSFUL` + grep vacío.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/model/SerieSesion.java app/src/main/java/com/ironquest/mvp/model/Usuario.java
git commit -m "Añadir campos rir y seguimientoFatiga para el opt-in de fatiga"
```

---

### Task 2: Refinamiento de `ProgresionAutomatica` con RIR

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionAutomatica.java`

**Interfaces:**
- Consumes: `SerieSesion.getRir()` (Task 1).
- Produces: `ProgresionAutomatica.clasificarSesion(...)` ahora considera el RIR de la última serie completada antes de aplicar la regla de reps.

- [ ] **Step 1: Modificar `clasificarSesion(RutinaEjercicio, EjercicioSesion)` en `ProgresionAutomatica.java`**

Ubicación: [ProgresionAutomatica.java](../../app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionAutomatica.java), método privado `clasificarSesion` creado en el Slice 1.

Reemplazar el cuerpo por (mantiene el análisis de reps + agrega el override por RIR):

```java
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

    if (noCompletadasFinales >= 2) return ESTADO_FALLIDO;
    if (completadas == 0) return ESTADO_FALLIDO;

    // Override por RIR de la última serie completada, cuando esté disponible.
    SerieSesion ultima = pasada.getUltimaSerieCompletada();
    int rir = ultima != null ? ultima.getRir() : -1;

    if (rir >= 3 && minCompleto >= objetivo) {
        // Fácil o con mucho margen → tratar como MARGEN aunque las reps no lleguen a objetivo+2.
        return ESTADO_MARGEN;
    }
    if (rir == 0 && minCompleto >= objetivo && minCompleto < objetivo + 2) {
        // Al fallo cumpliendo justo → tratar como FALLIDO (no subir, ya está al límite).
        return ESTADO_FALLIDO;
    }

    // Sin RIR o RIR intermedio: fallback a la regla del Slice 1 (solo señal de reps).
    if (minCompleto < objetivo) return ESTADO_FALLIDO;
    if (minCompleto >= objetivo + 2) return ESTADO_MARGEN;
    return ESTADO_JUSTO;
}
```

**Nota:** los constantes `ESTADO_MARGEN`, `ESTADO_JUSTO`, `ESTADO_FALLIDO` ya existen del Slice 1 y no se tocan. El resto de `ProgresionAutomatica` (métodos `decidir`, `sugerir`, `fueExitosa`) no cambia.

- [ ] **Step 2: Compilar**

```bash
./gradlew assembleDebug
```

Esperado: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionAutomatica.java
git commit -m "Refinar clasificación de ProgresionAutomatica con RIR cuando esté disponible"
```

---

### Task 3: Utilidad `AnalisisMuscular`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/util/AnalisisMuscular.java`

**Interfaces:**
- Consumes: `Ejercicio.getMusculoObjetivo()`, `Ejercicio.getMusculosSecundarios()`, `Sesion.getEjercicios()`, `EjercicioSesion.getSeries()`, `SerieSesion.calcularVolumen()`, `SerieSesion.isCompletada()`.
- Produces:
  - `AnalisisMuscular.LIMITE_POR_GRUPO = 4` (constante `public static final int`).
  - `AnalisisMuscular.calcularVolumenPorGrupo(Sesion, Map<String, Ejercicio>)` → `LinkedHashMap<String, Double>` (orden estable).
  - `AnalisisMuscular.calcularVolumenPorGrupo(List<Sesion>, LocalDate desde, LocalDate hasta, Map<String, Ejercicio>)` → `LinkedHashMap<String, Double>`. `desde`/`hasta` pueden ser `null` (sin límite).
  - `AnalisisMuscular.ejerciciosPorGrupo(Rutina, Map<String, Ejercicio>)` → `LinkedHashMap<String, Integer>` (solo primario).
  - `AnalisisMuscular.gruposSobreLimite(Rutina, Map<String, Ejercicio>)` → `LinkedHashMap<String, Integer>` (solo los que superan el límite).
  - `AnalisisMuscular.OTROS = "Otros"` — grupo comodín para ejercicios sin `musculoObjetivo`.

- [ ] **Step 1: Crear `AnalisisMuscular.java`**

```java
package com.ironquest.mvp.util;

import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo de volumen ponderado y conteo de ejercicios por grupo muscular.
 * <p>Ponderación: el músculo objetivo se lleva 100% del volumen de la serie; los músculos
 * secundarios comparten un 50% adicional, dividido entre ellos. Presupuesto por serie = 150%.
 * <p>Ejercicios sin {@code musculoObjetivo} caen en {@link #OTROS}.
 */
public final class AnalisisMuscular {

    public static final int LIMITE_POR_GRUPO = 4;
    public static final String OTROS = "Otros";

    private AnalisisMuscular() {
    }

    /** Volumen ponderado de una sola sesión. */
    public static LinkedHashMap<String, Double> calcularVolumenPorGrupo(
            Sesion sesion, Map<String, Ejercicio> catalogoPorId) {
        LinkedHashMap<String, Double> resultado = new LinkedHashMap<>();
        if (sesion == null) return resultado;
        acumularSesion(sesion, catalogoPorId, resultado);
        return resultado;
    }

    /** Volumen ponderado agregado de varias sesiones. {@code desde}/{@code hasta} inclusivos (o {@code null}). */
    public static LinkedHashMap<String, Double> calcularVolumenPorGrupo(
            List<Sesion> sesiones, LocalDate desde, LocalDate hasta, Map<String, Ejercicio> catalogoPorId) {
        LinkedHashMap<String, Double> resultado = new LinkedHashMap<>();
        if (sesiones == null) return resultado;
        for (Sesion s : sesiones) {
            if (!s.estaFinalizada()) continue;
            if (!enRango(s, desde, hasta)) continue;
            acumularSesion(s, catalogoPorId, resultado);
        }
        return resultado;
    }

    /** Conteo de ejercicios por grupo muscular primario en una rutina. */
    public static LinkedHashMap<String, Integer> ejerciciosPorGrupo(
            Rutina rutina, Map<String, Ejercicio> catalogoPorId) {
        LinkedHashMap<String, Integer> resultado = new LinkedHashMap<>();
        if (rutina == null) return resultado;
        for (RutinaEjercicio re : rutina.getEjercicios()) {
            Ejercicio ej = catalogoPorId.get(re.getEjercicioId());
            String grupo = grupoDe(ej);
            resultado.merge(grupo, 1, Integer::sum);
        }
        return resultado;
    }

    /** Filtrado del anterior: solo grupos que superan {@link #LIMITE_POR_GRUPO}. */
    public static LinkedHashMap<String, Integer> gruposSobreLimite(
            Rutina rutina, Map<String, Ejercicio> catalogoPorId) {
        LinkedHashMap<String, Integer> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : ejerciciosPorGrupo(rutina, catalogoPorId).entrySet()) {
            if (e.getValue() > LIMITE_POR_GRUPO && !OTROS.equals(e.getKey())) {
                resultado.put(e.getKey(), e.getValue());
            }
        }
        return resultado;
    }

    private static void acumularSesion(Sesion sesion, Map<String, Ejercicio> catalogoPorId,
                                        LinkedHashMap<String, Double> acumulador) {
        for (EjercicioSesion es : sesion.getEjercicios()) {
            Ejercicio ej = catalogoPorId.get(es.getEjercicioId());
            if (ej == null) {
                for (SerieSesion s : es.getSeries()) {
                    if (s.isCompletada()) {
                        acumular(acumulador, OTROS, s.calcularVolumen());
                    }
                }
                continue;
            }
            String primario = ej.getMusculoObjetivo();
            List<String> secundarios = ej.getMusculosSecundarios();
            for (SerieSesion s : es.getSeries()) {
                if (!s.isCompletada()) continue;
                double vol = s.calcularVolumen();
                acumular(acumulador, primario != null && !primario.isEmpty() ? primario : OTROS, vol);
                if (secundarios != null && !secundarios.isEmpty()) {
                    double porCada = 0.5 * vol / secundarios.size();
                    for (String m : secundarios) {
                        if (m != null && !m.isEmpty()) {
                            acumular(acumulador, m, porCada);
                        }
                    }
                }
            }
        }
    }

    private static boolean enRango(Sesion s, LocalDate desde, LocalDate hasta) {
        try {
            LocalDate fecha = LocalDateTime.parse(s.getFechaHoraInicio()).toLocalDate();
            if (desde != null && fecha.isBefore(desde)) return false;
            if (hasta != null && fecha.isAfter(hasta)) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void acumular(LinkedHashMap<String, Double> mapa, String clave, double valor) {
        mapa.merge(clave, valor, Double::sum);
    }

    private static String grupoDe(Ejercicio ej) {
        if (ej == null || ej.getMusculoObjetivo() == null || ej.getMusculoObjetivo().isEmpty()) {
            return OTROS;
        }
        return ej.getMusculoObjetivo();
    }
}
```

- [ ] **Step 2: Compilar**

```bash
./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/util/AnalisisMuscular.java
git commit -m "Añadir AnalisisMuscular con ponderación primario/secundarios y conteo por grupo"
```

---

### Task 4: `PieChartView` casera

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/ui/PieChartView.java`

**Interfaces:**
- Produces:
  - `PieChartView.setDatos(Map<String, Double>)` — recibe el mapa de la utilidad de Task 3.
  - `PieChartView.getColorPorGrupo(String)` — devuelve el hex del grupo (útil para la leyenda).

**Contexto:** View custom que dibuja arcos proporcionales. Sin dependencias. Similar en espíritu a la [BarChartView](../../app/src/main/java/com/ironquest/mvp/ui/BarChartView.java) ya existente. Cuadrado (proporción 1:1). Paleta hardcoded interna, 15 colores + gris para `Otros`.

- [ ] **Step 1: Crear `PieChartView.java`**

```java
package com.ironquest.mvp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.ironquest.mvp.util.AnalisisMuscular;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * View custom que dibuja un gráfico circular a partir de un {@code Map<String, Double>}.
 * Paleta fija hardcoded para los 15 grupos musculares del catálogo + gris para "Otros".
 * Proporción 1:1 (cuadrado); ancho recomendado 240dp.
 */
public class PieChartView extends View {

    /** Paleta derivada del acento naranja del tema con suficiente contraste entre valores adyacentes. */
    private static final Map<String, Integer> COLORES = new LinkedHashMap<>();
    static {
        COLORES.put("Pectorales",           Color.parseColor("#FF6B35")); // acento
        COLORES.put("Dorsales",             Color.parseColor("#3B7DDD"));
        COLORES.put("Trapecios",            Color.parseColor("#8E44AD"));
        COLORES.put("Espalda alta",         Color.parseColor("#2ECC71"));
        COLORES.put("Zona lumbar",          Color.parseColor("#E67E22"));
        COLORES.put("Cuádriceps",           Color.parseColor("#1ABC9C"));
        COLORES.put("Isquiotibiales",       Color.parseColor("#9B59B6"));
        COLORES.put("Glúteos",              Color.parseColor("#F39C12"));
        COLORES.put("Aductores/Abductores", Color.parseColor("#16A085"));
        COLORES.put("Pantorrillas",         Color.parseColor("#C0392B"));
        COLORES.put("Hombros",              Color.parseColor("#2980B9"));
        COLORES.put("Bíceps",               Color.parseColor("#D35400"));
        COLORES.put("Tríceps",              Color.parseColor("#7F8C8D"));
        COLORES.put("Antebrazos",           Color.parseColor("#27AE60"));
        COLORES.put("Abdomen",              Color.parseColor("#E74C3C"));
        COLORES.put(AnalisisMuscular.OTROS, Color.parseColor("#95A5A6"));
    }

    private static final int COLOR_FALLBACK = Color.parseColor("#BDC3C7");

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();
    private Map<String, Double> datos = new LinkedHashMap<>();

    public PieChartView(Context context) {
        super(context);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDatos(Map<String, Double> datos) {
        this.datos = datos != null ? datos : new LinkedHashMap<>();
        invalidate();
    }

    public static int getColorPorGrupo(String grupo) {
        Integer c = COLORES.get(grupo);
        return c != null ? c : COLOR_FALLBACK;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.max(200, Math.min(w, h));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        double total = 0;
        for (Double v : datos.values()) {
            if (v != null && v > 0) total += v;
        }
        if (total <= 0) return;

        int padding = 8;
        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);
        float startAngle = -90f; // arrancar arriba
        paint.setStyle(Paint.Style.FILL);

        for (Map.Entry<String, Double> e : datos.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            float sweep = (float) (360.0 * e.getValue() / total);
            paint.setColor(getColorPorGrupo(e.getKey()));
            canvas.drawArc(rectF, startAngle, sweep, true, paint);
            startAngle += sweep;
        }
    }
}
```

- [ ] **Step 2: Compilar**

```bash
./gradlew assembleDebug
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/PieChartView.java
git commit -m "Añadir PieChartView casera con paleta hardcoded de 15 grupos + Otros"
```

---

### Task 5: Toggle de fatiga en `AjustesActivity` + chips por serie en `EjercicioSesionPagerAdapter`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/AjustesActivity.java`
- Modify: `app/src/main/res/layout/activity_ajustes.xml`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/EjercicioSesionPagerAdapter.java`
- Modify: `app/src/main/res/layout/item_serie_sesion.xml` (verificar nombre exacto en el proyecto)
- Create: `app/src/main/res/layout/view_fatiga_chips.xml`

**Interfaces:**
- Consumes: `Usuario.isSeguimientoFatiga()/setSeguimientoFatiga(...)` (Task 1), `SerieSesion.getRir()/setRir(int)` (Task 1).
- Produces: bloque nuevo en `AjustesActivity` con toggle. Chips de fatiga inflados por serie en el pager de sesión activa, ocultos si el toggle está apagado, guardan inmediato al tap.

- [ ] **Step 1: Agregar bloque de fatiga en `activity_ajustes.xml`**

Al final del `LinearLayout` interno del scroll, agregar un nuevo `MaterialCardView`:

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

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Seguimiento de fatiga avanzado"
                android:textAppearance="?attr/textAppearanceTitleMedium" />

            <com.google.android.material.materialswitch.MaterialSwitch
                android:id="@+id/switch_seguimiento_fatiga"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content" />
        </LinearLayout>

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:text="Marca cómo te sentiste al terminar cada serie. La app usa esta info para afinar sus sugerencias."
            android:textAppearance="?attr/textAppearanceBodySmall" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 2: Wire-up del toggle en `AjustesActivity.java`**

Agregar en `onCreate()`:

```java
com.google.android.material.materialswitch.MaterialSwitch switchFatiga = findViewById(R.id.switch_seguimiento_fatiga);
switchFatiga.setChecked(usuario != null && usuario.isSeguimientoFatiga());
switchFatiga.setOnCheckedChangeListener((buttonView, isChecked) -> {
    if (usuario == null) return;
    usuario.setSeguimientoFatiga(isChecked);
    dataManager.save();
});
```

- [ ] **Step 3: Crear `view_fatiga_chips.xml`**

```xml
<merge xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <LinearLayout
        android:id="@+id/container_fatiga"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:visibility="gone"
        android:layout_marginTop="6dp">

        <com.google.android.material.chip.ChipGroup
            android:id="@+id/chip_group_fatiga"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:singleSelection="true">

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_facil"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Fácil" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_justo"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Justo" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_duro"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Duro" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_al_fallo"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Al fallo" />
        </com.google.android.material.chip.ChipGroup>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:layout_marginTop="4dp">

            <TextView
                android:id="@+id/button_toggle_rir"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="+ RIR exacto"
                android:textColor="?attr/colorPrimary"
                android:padding="4dp"
                android:clickable="true"
                android:focusable="true" />

            <NumberPicker
                android:id="@+id/picker_rir"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:visibility="gone" />
        </LinearLayout>
    </LinearLayout>
</merge>
```

- [ ] **Step 4: Incluir el bloque en `item_serie_sesion.xml`**

Primero localizar el archivo exacto:

```bash
grep -rln "item_serie" app/src/main/res/layout/
```

En el layout de cada fila de serie (probablemente `item_serie_sesion.xml`), agregar al final del `LinearLayout` raíz:

```xml
<include layout="@layout/view_fatiga_chips" />
```

- [ ] **Step 5: Wire-up en `EjercicioSesionPagerAdapter.java`**

En [EjercicioSesionPagerAdapter.java](../../app/src/main/java/com/ironquest/mvp/ui/EjercicioSesionPagerAdapter.java), pasar el `Usuario` al constructor o exponerlo vía DataManager. Opción más simple: leer `DataManager.getInstance(context).getDataStore().getUsuario()` desde el ViewHolder.

En el `bind()` de la fila de serie, después de configurar el resto de la fila:

```java
android.view.View container = itemView.findViewById(R.id.container_fatiga);
Usuario usuario = DataManager.getInstance(itemView.getContext()).getDataStore().getUsuario();
boolean fatigaActiva = usuario != null && usuario.isSeguimientoFatiga();
if (!fatigaActiva || !serie.isCompletada()) {
    container.setVisibility(View.GONE);
    return;
}
container.setVisibility(View.VISIBLE);

ChipGroup chips = itemView.findViewById(R.id.chip_group_fatiga);
chips.setOnCheckedStateChangeListener(null); // limpiar por reciclaje
int rir = serie.getRir();
if (rir == 4 || rir == 5) chips.check(R.id.chip_facil);
else if (rir == 2 || rir == 3) chips.check(R.id.chip_justo);
else if (rir == 1) chips.check(R.id.chip_duro);
else if (rir == 0) chips.check(R.id.chip_al_fallo);
else chips.clearCheck();

chips.setOnCheckedStateChangeListener((group, ids) -> {
    if (ids.isEmpty()) return;
    int id = ids.get(0);
    int nuevoRir;
    if (id == R.id.chip_facil) nuevoRir = 4;
    else if (id == R.id.chip_justo) nuevoRir = 2;
    else if (id == R.id.chip_duro) nuevoRir = 1;
    else nuevoRir = 0;
    if (nuevoRir != serie.getRir()) {
        serie.setRir(nuevoRir);
        listener.onProgresoModificado(); // reutiliza el callback existente que dispara guardarProgreso()
    }
    detectarFatigaAcumulada(ejercicioSesion, itemView);
});

TextView toggleRir = itemView.findViewById(R.id.button_toggle_rir);
NumberPicker pickerRir = itemView.findViewById(R.id.picker_rir);
pickerRir.setMinValue(0);
pickerRir.setMaxValue(5);
if (rir >= 0 && rir <= 5) pickerRir.setValue(rir);
toggleRir.setOnClickListener(v -> {
    pickerRir.setVisibility(pickerRir.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
});
pickerRir.setOnValueChangedListener((picker, oldVal, newVal) -> {
    if (newVal != serie.getRir()) {
        serie.setRir(newVal);
        chips.clearCheck(); // el numérico pisa el chip
        // Reflejar el numérico en el chip equivalente
        if (newVal >= 4) chips.check(R.id.chip_facil);
        else if (newVal >= 2) chips.check(R.id.chip_justo);
        else if (newVal == 1) chips.check(R.id.chip_duro);
        else chips.check(R.id.chip_al_fallo);
        listener.onProgresoModificado();
        detectarFatigaAcumulada(ejercicioSesion, itemView);
    }
});
```

Imports:
```java
import android.widget.NumberPicker;
import android.widget.TextView;
import android.view.View;
import com.google.android.material.chip.ChipGroup;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.Usuario;
```

Y el helper `detectarFatigaAcumulada`:

```java
private void detectarFatigaAcumulada(EjercicioSesion es, View itemView) {
    // Cuenta hacia atrás desde la última serie completada; si las últimas 3 tienen rir == 0, avisa.
    int racha = 0;
    List<SerieSesion> series = es.getSeries();
    for (int i = series.size() - 1; i >= 0 && racha < 3; i--) {
        SerieSesion s = series.get(i);
        if (!s.isCompletada()) continue;
        if (s.getRir() == 0) {
            racha++;
        } else {
            break;
        }
    }
    if (racha >= 3) {
        com.google.android.material.snackbar.Snackbar.make(
                itemView, "Llevas 3 series al fallo — considera parar aquí.",
                com.google.android.material.snackbar.Snackbar.LENGTH_LONG
        ).show();
    }
}
```

- [ ] **Step 6: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

En el teléfono:
- Ajustes → prender toggle → guardar.
- Arrancar sesión → chips aparecen debajo de cada serie completada.
- Tap "Duro" → verificar (vía Data Inspector si se tiene, o reabriendo la sesión) que `SerieSesion.rir == 1`.
- Expandir "+ RIR exacto" y poner 3 → chip se mueve a "Justo", rir queda en 3.
- 3 series consecutivas "Al fallo" → snackbar aparece.
- Apagar toggle → chips desaparecen de la próxima serie.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/AjustesActivity.java app/src/main/java/com/ironquest/mvp/ui/EjercicioSesionPagerAdapter.java app/src/main/res/layout/activity_ajustes.xml app/src/main/res/layout/view_fatiga_chips.xml app/src/main/res/layout/item_serie_sesion.xml
git commit -m "Capturar RIR por serie con chips y detección dinámica de fatiga"
```

---

### Task 6: Pie chart en `SessionSummaryActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/SessionSummaryActivity.java`
- Modify: `app/src/main/res/layout/activity_session_summary.xml`
- Create: `app/src/main/res/layout/view_pie_chart_con_leyenda.xml`

**Interfaces:**
- Consumes: `AnalisisMuscular.calcularVolumenPorGrupo(Sesion, Map<String,Ejercicio>)` (Task 3), `PieChartView.setDatos(...)` y `PieChartView.getColorPorGrupo(...)` (Task 4). Usa el `EXTRA_SESION_ID` que el Slice 1 ya pasa al SessionSummary.
- Produces: nueva sección "Distribución muscular" al final del summary con el pie chart + leyenda con % por grupo.

- [ ] **Step 1: Crear `view_pie_chart_con_leyenda.xml`**

Layout raíz `LinearLayout` vertical:
- `TextView` título "Distribución muscular" (18sp bold, margen 8dp).
- `LinearLayout` horizontal:
  - `com.ironquest.mvp.ui.PieChartView` id `pie_chart` a la izquierda, `layout_width="200dp"` `layout_height="200dp"`.
  - `LinearLayout` id `container_leyenda` vertical a la derecha, `layout_weight="1"`.
- `TextView` id `text_pie_vacio` con `visibility="gone"` texto "Sin datos suficientes" (fallback).

- [ ] **Step 2: Incluir en `activity_session_summary.xml`**

Al final del contenido, después del card de sugerencias del Slice 1:

```xml
<include
    android:id="@+id/view_pie_sesion"
    layout="@layout/view_pie_chart_con_leyenda" />
```

- [ ] **Step 3: Wire-up en `SessionSummaryActivity.java`**

En `onCreate()`, después del bloque de sugerencias del Slice 1, agregar:

```java
String sesionIdActual = getIntent().getStringExtra(EXTRA_SESION_ID);
Sesion sesion = null;
if (sesionIdActual != null) {
    for (Sesion s : dataStore.getSesiones()) {
        if (sesionIdActual.equals(s.getId())) {
            sesion = s;
            break;
        }
    }
}
if (sesion != null) {
    Map<String, Ejercicio> catalogoPorId = new HashMap<>();
    for (Ejercicio ej : dataStore.getEjercicios()) {
        catalogoPorId.put(ej.getId(), ej);
    }
    LinkedHashMap<String, Double> datos = AnalisisMuscular.calcularVolumenPorGrupo(sesion, catalogoPorId);
    renderPieChart(findViewById(R.id.view_pie_sesion), datos);
}
```

Método helper:

```java
private void renderPieChart(View viewPie, LinkedHashMap<String, Double> datos) {
    PieChartView pieChart = viewPie.findViewById(R.id.pie_chart);
    LinearLayout leyenda = viewPie.findViewById(R.id.container_leyenda);
    TextView vacio = viewPie.findViewById(R.id.text_pie_vacio);

    double total = 0;
    for (Double v : datos.values()) if (v != null) total += v;
    if (total <= 0) {
        pieChart.setVisibility(View.GONE);
        leyenda.setVisibility(View.GONE);
        vacio.setVisibility(View.VISIBLE);
        return;
    }
    vacio.setVisibility(View.GONE);
    pieChart.setVisibility(View.VISIBLE);
    leyenda.setVisibility(View.VISIBLE);
    pieChart.setDatos(datos);
    leyenda.removeAllViews();
    LayoutInflater inflater = LayoutInflater.from(this);
    for (Map.Entry<String, Double> e : datos.entrySet()) {
        int porcentaje = (int) Math.round(100.0 * e.getValue() / total);
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setPadding(8, 4, 8, 4);
        View colorBox = new View(this);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(24, 24);
        boxParams.gravity = android.view.Gravity.CENTER_VERTICAL;
        boxParams.rightMargin = 8;
        colorBox.setLayoutParams(boxParams);
        colorBox.setBackgroundColor(PieChartView.getColorPorGrupo(e.getKey()));
        TextView label = new TextView(this);
        label.setText(e.getKey() + "  " + porcentaje + "%");
        label.setTextSize(13);
        fila.addView(colorBox);
        fila.addView(label);
        leyenda.addView(fila);
    }
}
```

Imports nuevos:
```java
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.util.AnalisisMuscular;
```

- [ ] **Step 4: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Correr una sesión con al menos 3 ejercicios de grupos musculares distintos, terminarla, verificar en SessionSummary:
- Pie chart aparece con arcos proporcionales.
- Leyenda a la derecha con cuadraditos de color + nombre + %.
- Porcentajes suman aproximadamente 100 (redondeo).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/SessionSummaryActivity.java app/src/main/res/layout/activity_session_summary.xml app/src/main/res/layout/view_pie_chart_con_leyenda.xml
git commit -m "Añadir pie chart de distribución muscular en SessionSummaryActivity"
```

---

### Task 7: Pie chart agregado en `StatsFragment`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/StatsFragment.java`
- Modify: `app/src/main/res/layout/fragment_stats.xml`

**Interfaces:**
- Consumes: `AnalisisMuscular.calcularVolumenPorGrupo(List<Sesion>, LocalDate, LocalDate, Map<String,Ejercicio>)` (Task 3), `PieChartView` (Task 4). Reusa `view_pie_chart_con_leyenda.xml` (Task 6).
- Produces: nueva sección al final de `StatsFragment` con selector Semana/Mes/Todo + pie chart + leyenda.

- [ ] **Step 1: Agregar sección en `fragment_stats.xml`**

Al final del layout raíz (ScrollView interno):

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">

        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Distribución muscular"
            android:textAppearance="?attr/textAppearanceTitleMedium" />

        <com.google.android.material.chip.ChipGroup
            android:id="@+id/chip_group_periodo"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:singleSelection="true"
            app:selectionRequired="true"
            android:layout_marginTop="8dp">

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_periodo_semana"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Semana" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_periodo_mes"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Mes" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_periodo_todo"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Todo" />
        </com.google.android.material.chip.ChipGroup>

        <include
            android:id="@+id/view_pie_stats"
            layout="@layout/view_pie_chart_con_leyenda" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 2: Wire-up en `StatsFragment.java`**

Después del bloque de estadísticas existentes en `onViewCreated()` (o donde el fragment configura sus vistas):

```java
ChipGroup chipsPeriodo = view.findViewById(R.id.chip_group_periodo);
chipsPeriodo.check(R.id.chip_periodo_semana);
View pieView = view.findViewById(R.id.view_pie_stats);
actualizarPieStats(view, pieView, R.id.chip_periodo_semana);
chipsPeriodo.setOnCheckedStateChangeListener((group, ids) -> {
    if (!ids.isEmpty()) actualizarPieStats(view, pieView, ids.get(0));
});
```

Método helper:

```java
private void actualizarPieStats(View root, View pieView, int chipId) {
    DataManager dm = DataManager.getInstance(requireContext());
    DataStore ds = dm.getDataStore();
    LocalDate hoy = LocalDate.now();
    LocalDate desde, hasta;
    if (chipId == R.id.chip_periodo_semana) {
        desde = hoy.minusDays(7);
        hasta = hoy;
    } else if (chipId == R.id.chip_periodo_mes) {
        desde = hoy.minusDays(30);
        hasta = hoy;
    } else {
        desde = null;
        hasta = null;
    }
    Map<String, Ejercicio> catalogoPorId = new HashMap<>();
    for (Ejercicio ej : ds.getEjercicios()) catalogoPorId.put(ej.getId(), ej);
    LinkedHashMap<String, Double> datos = AnalisisMuscular.calcularVolumenPorGrupo(
            ds.getSesiones(), desde, hasta, catalogoPorId);
    renderPieChart(pieView, datos);
}

private void renderPieChart(View pieView, LinkedHashMap<String, Double> datos) {
    // Copia exacta del método de SessionSummaryActivity.
    // Nota: si el ejecutor prefiere no duplicar, extraer a una utilidad estática
    // `PieChartRender.render(view, datos, context)` — decisión estilística.
    // ...cuerpo idéntico al de Task 6 step 3...
}
```

Imports nuevos:
```java
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.android.material.chip.ChipGroup;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.util.AnalisisMuscular;
```

**Recomendación**: extraer `renderPieChart(...)` a una clase utilitaria `ui/PieChartRender.java` estática para no duplicarlo entre `SessionSummaryActivity` y `StatsFragment`. Si big pickle lo hace, ajustar imports en ambos.

- [ ] **Step 3: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Abrir StatsFragment → verificar que el pie chart aparece con datos de la semana en curso por default. Cambiar a Mes y Todo → el pie chart actualiza sin recrear el fragment.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/StatsFragment.java app/src/main/res/layout/fragment_stats.xml
git commit -m "Añadir sección de distribución muscular en StatsFragment con selector de período"
```

---

### Task 8: Banner de límite por grupo en `EditRoutineActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java`
- Modify: `app/src/main/res/layout/activity_edit_routine.xml`

**Interfaces:**
- Consumes: `AnalisisMuscular.gruposSobreLimite(Rutina, Map)` (Task 3).
- Produces: banner encima del banner de sugerencias del Slice 1, con lista de grupos que superan `LIMITE_POR_GRUPO`.

- [ ] **Step 1: Agregar el banner en `activity_edit_routine.xml`**

Encima del `card_banner_sugerencias` (que se agregó en el Slice 1):

```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card_banner_limite_grupo"
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
            android:id="@+id/text_banner_limite_grupo"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <Button
            android:id="@+id/button_ocultar_banner_limite"
            style="?attr/materialButtonOutlinedStyle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end"
            android:layout_marginTop="8dp"
            android:text="Entendido" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 2: Wire-up en `EditRoutineActivity.java`**

Campos:
```java
private View cardBannerLimite;
private TextView textBannerLimite;
private boolean bannerLimiteOculto = false;
```

En `onCreate()`:
```java
cardBannerLimite = findViewById(R.id.card_banner_limite_grupo);
textBannerLimite = findViewById(R.id.text_banner_limite_grupo);
findViewById(R.id.button_ocultar_banner_limite).setOnClickListener(v -> {
    bannerLimiteOculto = true;
    cardBannerLimite.setVisibility(View.GONE);
});
```

Método:
```java
private void actualizarBannerLimite() {
    if (bannerLimiteOculto) return;
    Map<String, Ejercicio> catalogoPorId = new HashMap<>();
    for (Ejercicio ej : dataStore.getEjercicios()) catalogoPorId.put(ej.getId(), ej);
    LinkedHashMap<String, Integer> excedidos = AnalisisMuscular.gruposSobreLimite(rutina, catalogoPorId);
    if (excedidos.isEmpty()) {
        cardBannerLimite.setVisibility(View.GONE);
        return;
    }
    StringBuilder sb = new StringBuilder("Aviso:\n");
    for (Map.Entry<String, Integer> e : excedidos.entrySet()) {
        sb.append("• ").append(e.getValue()).append(" ejercicios de \"").append(e.getKey()).append("\"\n");
    }
    sb.append("Se recomienda máximo ").append(AnalisisMuscular.LIMITE_POR_GRUPO).append(" por sesión.");
    textBannerLimite.setText(sb.toString());
    cardBannerLimite.setVisibility(View.VISIBLE);
}
```

Llamar a `actualizarBannerLimite()` en `onResume()` y cada vez que se agrega/quita/reemplaza un ejercicio de la rutina (buscar los puntos correspondientes en la Activity — típicamente después de `adapter.notifyDataSetChanged()` o similar).

Imports:
```java
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.ironquest.mvp.util.AnalisisMuscular;
```

- [ ] **Step 3: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Crear rutina con 5 ejercicios cuyo primario sea "Pectorales" → banner aparece con conteo correcto. Quitar uno → banner desaparece (queda con 4, no supera el límite). Tap "Entendido" → banner se oculta hasta cerrar y reabrir la Activity.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java app/src/main/res/layout/activity_edit_routine.xml
git commit -m "Añadir banner de aviso por exceso de ejercicios por grupo muscular"
```

---

### Task 9: Card de aviso al arrancar sesión en `ActiveSessionActivity`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java`
- Modify: `app/src/main/res/layout/activity_active_session.xml` (verificar nombre exacto)

**Interfaces:**
- Consumes: `AnalisisMuscular.gruposSobreLimite(Rutina, Map)` (Task 3).
- Produces: `MaterialCardView` que aparece brevemente al arrancar sesión si la rutina supera el límite; botón "Empezar de todos modos" lo oculta.

- [ ] **Step 1: Agregar el card en `activity_active_session.xml`**

Encima del pager, agregar:

```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card_aviso_fatiga"
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
            android:id="@+id/text_aviso_fatiga"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <Button
            android:id="@+id/button_arrancar_igual"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="end"
            android:layout_marginTop="8dp"
            android:text="Empezar de todos modos" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 2: Wire-up en `ActiveSessionActivity.java`**

En `onCreate()`, después de configurar la sesión y el pager, pero antes de renderizar el primer ítem:

```java
View cardAvisoFatiga = findViewById(R.id.card_aviso_fatiga);
TextView textAvisoFatiga = findViewById(R.id.text_aviso_fatiga);
Rutina rutinaActual = dataStore.buscarRutina(sesionActual.getRutinaId());
if (rutinaActual != null) {
    LinkedHashMap<String, Integer> excedidos = AnalisisMuscular.gruposSobreLimite(rutinaActual, catalogoPorId);
    if (!excedidos.isEmpty()) {
        StringBuilder sb = new StringBuilder("Cuidado con la fatiga:\n");
        for (Map.Entry<String, Integer> e : excedidos.entrySet()) {
            sb.append("• ").append(e.getValue()).append(" ejercicios de \"").append(e.getKey()).append("\"\n");
        }
        sb.append("Se recomienda máximo ").append(AnalisisMuscular.LIMITE_POR_GRUPO).append(" por sesión.");
        textAvisoFatiga.setText(sb.toString());
        cardAvisoFatiga.setVisibility(View.VISIBLE);
    }
}
findViewById(R.id.button_arrancar_igual).setOnClickListener(v -> cardAvisoFatiga.setVisibility(View.GONE));
```

Imports:
```java
import java.util.LinkedHashMap;
import java.util.Map;
import com.ironquest.mvp.util.AnalisisMuscular;
```

- [ ] **Step 3: Compilar y verificar en dispositivo**

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Con la rutina "sobrecargada" del Task 8, arrancar sesión → card aparece con la lista de grupos afectados. Tap "Empezar de todos modos" → card desaparece y el pager sigue accesible.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java app/src/main/res/layout/activity_active_session.xml
git commit -m "Añadir card de aviso de fatiga al arrancar sesión con rutina sobrecargada"
```

---

### Task 10: Verificación end-to-end, bump de versión y release

**Files:**
- Modify: `app/build.gradle`

**Contexto:** Igual protocolo que Task 12 del Slice 1. Bump a `v0.10.0`.

- [ ] **Step 1: Respaldar `datos.json`**

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json > /tmp/respaldo_pre_v0.10.0.json
ls -la /tmp/respaldo_pre_v0.10.0.json
```

- [ ] **Step 2: Bump versión**

En [app/build.gradle](../../app/build.gradle):

```gradle
versionCode 11
versionName "0.10.0"
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

Vacío.

- [ ] **Step 5: Instalar y checklist en dispositivo**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell dumpsys package com.ironquest.mvp | grep versionName
```

Esperado: `versionName=0.10.0`.

Checklist manual:

- [ ] App arranca sin crash.
- [ ] Sesiones antiguas (de Slice 1) se abren normal, series sin chips (por `seguimientoFatiga` default `false`).
- [ ] Ajustes → prender "Seguimiento de fatiga avanzado" → guarda.
- [ ] Arrancar sesión → chips aparecen debajo de cada serie completada. Tap "Duro" → guarda `rir = 1`. Expandir "+ RIR exacto" → `NumberPicker` visible. Poner "3" → chip se mueve a "Justo".
- [ ] Correr sesión 3×8 justo con RIR "Fácil" en todas las series → sugerencia post-sesión sube peso (motor refinado).
- [ ] Correr sesión 3×8 justo con RIR "Al fallo" → sugerencia mantiene peso.
- [ ] 3 series consecutivas "Al fallo" del mismo ejercicio → snackbar aparece.
- [ ] Apagar toggle → chips desaparecen. Los `rir` ya guardados se mantienen (verificar en próxima sesión editada del historial).
- [ ] Rutina con 5 ejercicios de "Pectorales" → banner en editor + card al arrancar. Tap "Empezar de todos modos" → card desaparece.
- [ ] Sesión post-cerrada con al menos 3 grupos musculares → pie chart aparece en `SessionSummary`, con arcos + leyenda + porcentajes que suman ~100.
- [ ] StatsFragment → sección "Distribución muscular" aparece, selector Semana/Mes/Todo actualiza el gráfico.
- [ ] Test específico de ponderación: sesión de 5×5 peso muerto con 100 kg → volumen primario "Zona lumbar" = 2500, cada secundario 250 (5×5×100 × 0.5 / 5 secundarios). Total del pie: 2500 + 5×250 = 3750 (150% de 2500).

- [ ] **Step 6: Restaurar `datos.json` real si se usaron datos de prueba**

Idéntico al Task 12 step 6 del Slice 1.

- [ ] **Step 7: Commit del bump**

```bash
git add app/build.gradle
git commit -m "Subir versión a 0.10.0 (versionCode 11)"
```

- [ ] **Step 8: Pedir aprobación del usuario para push y release**

Presentar commits nuevos desde `v0.9.0`. Preguntar: "¿Autorizas el push a origin/main y el release v0.10.0?"

- [ ] **Step 9: Push y release (solo con autorización)**

```bash
git push origin main
```

Notas de release en `/tmp/notas_v0.10.0.md`:

```markdown
## v0.10.0 — Fatiga capturada y analítica muscular

- **Seguimiento de fatiga opcional**: activa el toggle en Ajustes y marca cómo te sentiste
  al terminar cada serie (Fácil / Justo / Duro / Al fallo). Opcionalmente ingresa RIR exacto.
- **Motor de sugerencias refinado**: cuando el toggle está activo, ProgresionAutomatica
  considera el RIR — si vas al fallo, no sube peso aunque cumplas; si vas fácil, sube más.
- **Distribución muscular**: gráfico circular al terminar cada sesión (SessionSummary) y
  en Estadísticas (agregado por semana / mes / todo). Ponderación: primario 100%,
  secundarios comparten 50%.
- **Aviso de exceso**: banner en el editor de rutina y card al arrancar sesión cuando la
  rutina supera 4 ejercicios de un mismo grupo muscular. Detección dinámica durante la
  sesión (3 series al fallo seguidas → aviso).

Todos los cambios son opt-in. Con el toggle apagado, la app se comporta idéntico a v0.9.0.
Sesiones y datos existentes intactos.
```

Copiar y publicar:

```bash
cp app/build/outputs/apk/debug/app-debug.apk /tmp/titan-score.apk
gh release create v0.10.0 /tmp/titan-score.apk \
    --repo JuanOV27/titan-score-app \
    --title "v0.10.0 — Fatiga capturada y analítica muscular" \
    --notes-file /tmp/notas_v0.10.0.md
gh release view v0.10.0 --repo JuanOV27/titan-score-app
```

- [ ] **Step 10: Confirmar al usuario**

Reportar URL del release, tamaño del APK, commits pusheados.

---

## Self-Review

**Cobertura del spec (2026-09-13-fatiga-analitica-design.md):**
- `SerieSesion.rir` + `Usuario.seguimientoFatiga` → Task 1.
- Refinamiento de `ProgresionAutomatica` con RIR → Task 2.
- Utilidad `AnalisisMuscular` con ponderación y conteos → Task 3.
- `PieChartView` casera con paleta hardcoded → Task 4.
- Toggle de fatiga en `AjustesActivity` → Task 5.
- Chips + RIR expandible en pager de sesión activa → Task 5.
- Detección dinámica (3 series al fallo) → Task 5.
- Pie chart en `SessionSummaryActivity` → Task 6.
- Sección "Distribución muscular" en `StatsFragment` con Semana/Mes/Todo → Task 7.
- Banner de límite por grupo en `EditRoutineActivity` → Task 8.
- Card al arrancar sesión → Task 9.
- Verificación + release → Task 10.
- Opt-in por default (chips y detección dinámica solo con `seguimientoFatiga == true`) → Task 5.
- Retro-compat sesiones viejas (`rir = -1`) → Task 1 + Task 2 (fallback).
- Ejercicios personalizados sin `musculoObjetivo` caen en `Otros` → Task 3.

**Consistencia de tipos:**
- `AnalisisMuscular.calcularVolumenPorGrupo` devuelve `LinkedHashMap<String, Double>` — coincide en Tasks 3, 6, 7.
- `AnalisisMuscular.gruposSobreLimite` devuelve `LinkedHashMap<String, Integer>` — coincide en Tasks 3, 8, 9.
- `PieChartView.setDatos(Map<String, Double>)` — coincide con lo que Task 3 produce.
- Escala RIR consistente (0=al fallo, 4=fácil) entre Task 1 (docstring), Task 2 (uso `>= 3`, `== 0`), y Task 5 (wire-up de chips).
- Constantes de UI (`R.id.chip_facil`, `R.id.container_fatiga`, etc.) definidas en Task 5 layouts y consumidas en Task 5 adapter.

**Placeholders:**
- Task 5 step 4 dice "verificar nombre exacto en el proyecto" del layout de fila de serie — instrucción explícita al ejecutor de confirmar el nombre real; no es TBD.
- Task 7 step 2 dice "recomendación: extraer a `PieChartRender`" — sugerencia de refactor con criterio, no laguna.
- Task 8 step 2 menciona "buscar los puntos correspondientes" para llamar `actualizarBannerLimite()` — instrucción de exploración acotada.

Estas tres son delegaciones al ejecutor para respetar código existente, no gaps de diseño.
