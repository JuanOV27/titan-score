# Sistema de metas — diseño

> Reemplaza el `MetasFragment` stub actual (`"Metas — próximamente"`) con un sistema completo
> de metas de entrenamiento y composición corporal, con validación de plausibilidad y
> recomendador de ejercicios basado en el catálogo curado.

## Contexto y objetivo

El tab "Metas" hoy es solo un `TextView` con "próximamente". El usuario ya tiene tres tipos
de meta en mente desde el inicio del proyecto:

1. Aumentar la medida de un músculo (brazo, pierna, pecho).
2. Reducir cintura / cadera (o el peso corporal).
3. Lograr un PR en un ejercicio específico.

Al brainstormear se aclaró que la app **mide** el progreso, no promete que lo alcances (no
compite con Fitia ni depende de wearables). También se decidió que el recomendador no solo
sugiera ejercicios, sino un pequeño plan por meta: volumen semanal recomendado, series y
reps por meta, peso inicial sugerido y filtro por lo que ya está en las rutinas actuales.
El campo `Usuario.diasEntrenoSemana` se agrega como input explícito del recomendador.

## Decisiones del usuario (2026-09-16)

| Decisión | Elección |
|---|---|
| Cuántas metas activas simultáneas | Máximo 3 (validado al crear) |
| Deadline por meta | Opcional (nullable) |
| Cómo se marca cumplida | Automático al detectar registro/sesión que llegue al objetivo |
| Algoritmo del recomendador | Híbrido: tabla curada + filtro por rutinas actuales del usuario |
| Ubicación del recomendador | Card de meta en MetasFragment **y** banner en EditRoutineActivity |
| Tipos de meta soportados | AUMENTAR_MEDIDA (muscular), REDUCIR_MEDIDA (cintura/cadera), PESO_CORPORAL (subir/bajar), PR_EJERCICIO (peso × reps) |
| Mapeo meta → ejercicios | Tabla hardcoded en `RecomendadorEjercicios.CURADOS` (Claude cura los nombres contra `catalogo.json` real) |
| Avisos | Celebración al cumplir + recordatorio in-app "sin actualizar hace X" + notificación push única al pasar de <80% a ≥80% de progreso |
| Validación al crear | Solo advierte, deja crear si el usuario confirma (respeta autonomía) |
| Alcance del recomendador | A+B+C: ejercicios + series/reps sugeridos + peso inicial + volumen semanal informativo. Frecuencia = campo `Usuario.diasEntrenoSemana` explícito. **Distribución automática entre días queda fuera.** |
| Encargado de la integración de código | **big pickle** vía OpenCode |

## División de trabajo

**Claude:**
1. Este spec.
2. `docs/plans/2026-09-16-sistema-metas.md` con las tareas listas para big pickle.
3. Verificación previa: cross-check de los nombres de ejercicios en `CURADOS` contra
   `catalogo.json` real (para que el plan quede con nombres exactos).

**big pickle:**
1. Todos los cambios de modelo, motor de metas, recomendador, UI, layouts.
2. Verificación end-to-end en dispositivo por el checklist del plan.
3. Commit final + release cuando el usuario apruebe.

## Alcance en un vistazo

### Dentro

- Nueva entidad `Meta` en `DataStore.metas`.
- Nuevo campo `Usuario.diasEntrenoSemana` (default 3).
- Nueva sub-jerarquía `util/metas/` con `EvaluadorMeta` (abstracta) + 4 subclases (una por
  tipo), `ValidadorMeta`, `RecomendadorEjercicios`, valores inmutables
  (`Advertencia`, `PlanRecomendado`, `Recomendacion`, `ParamsProgresion`).
- Motor de evaluación automática al guardar `RegistroFisico` o cerrar `Sesion`.
- Motor de validación con advertencias basadas en IMC, ICC y deltas relativos.
- Recomendador ampliado: lista de ejercicios candidatos filtrada por rutinas actuales +
  parámetros sugeridos + resumen de volumen semanal por grupo.
- `MetasFragment` reescrito con lista de activas, historial y estado vacío.
- Cinco diálogos nuevos: crear meta, plan sugerido, agregar ejercicio a rutina,
  celebración, edición inline.
- Banner en `EditRoutineActivity` cuando hay meta activa con brecha.
- Bloque nuevo en `AjustesActivity` para `diasEntrenoSemana`.
- Notificación push única al cruzar el 80% de progreso.
- Recordatorio pasivo en card cuando el usuario deja de registrar la medida relevante.

### Fuera (ver "Fuera de alcance")

- Módulo de cardio (slice futuro independiente).
- Distribución automática de ejercicios entre días de la semana.
- Modificación automática de rutinas existentes.
- Estimación cross-ejercicio de peso inicial.
- Gráficas de evolución histórica por meta.
- Metas compartidas / red social.
- Coach conversacional o IA.

## Modelo de datos

Aditivo. Cumple trampas #3 (primitivos siempre seguros, colección nueva normalizada) y #4
(sin `save()` desde `load()`).

### Nueva entidad `Meta extends EntidadIdentificable`

Prefijo de id `"m"`. Constantes públicas:

```java
public static final int TIPO_AUMENTAR_MEDIDA = 0;
public static final int TIPO_REDUCIR_MEDIDA  = 1;
public static final int TIPO_PESO_CORPORAL   = 2;
public static final int TIPO_PR_EJERCICIO    = 3;

public static final int ESTADO_ACTIVA     = 0;
public static final int ESTADO_CUMPLIDA   = 1;
public static final int ESTADO_DESCARTADA = 2;
```

Campos privados con getters/setters, constructor sin args privado para Gson, constructor
público completo:

| Campo | Tipo | Notas |
|---|---|---|
| `tipo` | `int` | Una de las 4 constantes de tipo |
| `titulo` | `String` | Autogenerado al crear, editable ("Aumentar brazo a 40 cm") |
| `estado` | `int` | Una de las 3 constantes de estado |
| `medidaTipo` | `String` | Solo para AUMENTAR/REDUCIR: uno de `"brazoCm"`, `"piernaCm"`, `"pechoCm"`, `"cinturaCm"`, `"caderaCm"`. Coincide con los nombres de campos de `RegistroFisico` para dispatch por reflexión / switch. |
| `ejercicioId` | `String` | Solo para PR_EJERCICIO |
| `pesoObjetivoKg` | `double` | Solo para PR |
| `repsObjetivo` | `int` | Solo para PR |
| `valorInicial` | `double` | Fotografía al crear la meta. Para PR: peso × reps del último mejor registro (o 0). Para tipos de medida y peso: valor actual. |
| `valorObjetivo` | `double` | Para tipos de medida y peso corporal. Ignorado para PR. |
| `fechaCreacion` | `String` | ISO (`LocalDateTime.now().toString()`) |
| `fechaObjetivo` | `String` | ISO. `null` = sin deadline. |
| `fechaCumplida` | `String` | ISO. `null` si aún no cumplida. |
| `fechaDescartada` | `String` | ISO. `null` si sigue activa/cumplida. |
| `advertenciaAceptada` | `boolean` | `true` si el usuario aceptó una advertencia al crear (primitivo, default `false`) |
| `notificadaCerca` | `boolean` | `true` una vez que la notificación de "≥ 80%" ya se disparó (default `false`, no se resetea) |

### Cambio en `DataStore`

```java
private List<Meta> metas = new ArrayList<>();
```

Getter/setter estándar. En `normalizarColecciones()`:

```java
if (metas == null) {
    metas = new ArrayList<>();
}
```

### Cambio en `Usuario`

```java
/** Cuántos días a la semana entrena. Alimenta al recomendador para calcular
 *  volumen semanal esperado. Editable en Ajustes. Default 3. */
private int diasEntrenoSemana = 3;
```

Getter/setter. Primitivo, seguro contra `datos.json` viejo.

### Métodos nuevos en `DataManager`

- `getMetasActivas()` → `List<Meta>` filtrada por `ESTADO_ACTIVA`.
- `getMetasHistorial()` → `List<Meta>` con `CUMPLIDA` o `DESCARTADA`, ordenadas por fecha
  descendente (última cumplida/descartada arriba).
- `agregarMeta(Meta)` → añade + `save()`. Retorna `false` si ya hay 3 activas.
- `descartarMeta(String metaId)` → estado `DESCARTADA` + `fechaDescartada` + `save()`.
- `evaluarMetas()` → método central. Recorre metas activas, verifica cumplimiento y
  progreso ≥ 80%. Devuelve `List<Meta>` con las recién cumplidas (para que la UI muestre
  celebración). Al final hace `save()` una sola vez si hubo cambios.

### Migración

- `CATALOGO_VERSION_ACTUAL` no se toca.
- Sin migración explícita. `metas` es lista nueva; el inicializador de campo cubre el caso
  "clave ausente en el JSON viejo". `diasEntrenoSemana = 3` idem.
- La primera vez que `evaluarMetas()` corre no hay metas para evaluar → no-op.

## Motor de evaluación

Polimorfismo real (respeta requisito POO de la materia). Nueva jerarquía en `util/metas/`.

### `EvaluadorMeta` (abstracta)

```java
public abstract class EvaluadorMeta {

    public static EvaluadorMeta para(Meta meta) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA: return new EvalMedidaMuscular();
            case Meta.TIPO_REDUCIR_MEDIDA:  return new EvalReducirMedida();
            case Meta.TIPO_PESO_CORPORAL:   return new EvalPesoCorporal();
            case Meta.TIPO_PR_EJERCICIO:    return new EvalPrEjercicio();
            default: throw new IllegalArgumentException("Tipo desconocido: " + meta.getTipo());
        }
    }

    /** Progreso 0.0 a 1.0 (clamped). Fórmula unificada:
     *  (actual - inicial) / (objetivo - inicial), clamped a [0,1]. */
    public abstract double calcularProgreso(Meta meta, DataStore ds);

    /** Si el estado actual de los datos alcanza o supera el objetivo. */
    public abstract boolean cumplida(Meta meta, DataStore ds);

    /** Texto para pintar en el card: "32 → 40 cm (progreso: 20 %)" */
    public abstract String textoProgreso(Meta meta, DataStore ds);

    /** Valor "actual" que compara contra objetivo. Útil para calcular progreso y para el
     *  recordatorio pasivo (si no hay valor actual reciente, mostrar aviso). */
    protected abstract double valorActual(Meta meta, DataStore ds);
}
```

### Subclases

- **`EvalMedidaMuscular`**: `valorActual` = último `RegistroFisico.get<Medida>Cm()` según
  `meta.medidaTipo`. `cumplida = valorActual >= meta.valorObjetivo`. Si no hay registros,
  progreso = 0 y `cumplida = false`.

- **`EvalReducirMedida`**: `valorActual` = último `RegistroFisico.get<Medida>Cm()`.
  `cumplida = valorActual <= meta.valorObjetivo && valorActual > 0`. Progreso invertido:
  `(inicial - actual) / (inicial - objetivo)`.

- **`EvalPesoCorporal`**: `valorActual` = último `RegistroFisico.pesoKg`. Dirección
  detectada: si `objetivo > inicial` es subir (cumplida cuando `actual >= objetivo`), si
  no es bajar (cumplida cuando `actual <= objetivo`). Progreso siempre positivo.

- **`EvalPrEjercicio`**: `valorActual` = mejor `peso × reps` en cualquier `SerieSesion.completada`
  con ese `ejercicioId`. `cumplida = existe alguna serie completada con
  peso >= meta.pesoObjetivoKg && reps >= meta.repsObjetivo`.
  Progreso = `mejor_score_actual / (pesoObjetivo × repsObjetivo)`, clamped.

### `DataManager.evaluarMetas()`

```java
public List<Meta> evaluarMetas() {
    List<Meta> recienCumplidas = new ArrayList<>();
    boolean cambio = false;
    for (Meta m : dataStore.getMetas()) {
        if (m.getEstado() != Meta.ESTADO_ACTIVA) continue;
        EvaluadorMeta ev = EvaluadorMeta.para(m);
        if (ev.cumplida(m, dataStore)) {
            m.setEstado(Meta.ESTADO_CUMPLIDA);
            m.setFechaCumplida(LocalDateTime.now().toString());
            recienCumplidas.add(m);
            cambio = true;
        } else if (!m.isNotificadaCerca() && ev.calcularProgreso(m, dataStore) >= 0.80) {
            m.setNotificadaCerca(true);
            NotificacionMetasHelper.notificarCerca(appContext, m);
            cambio = true;
        }
    }
    if (cambio) save();
    return recienCumplidas;
}
```

Se llama desde:
- `PhysicalProfileActivity` tras guardar `RegistroFisico`.
- `ActiveSessionActivity.finalizarSesion()` tras persistir la sesión.

Ambos callers muestran `CelebracionMetaDialog` si la lista devuelta no está vacía.

## Motor de validación

Nueva clase `ValidadorMeta` (fachada estática, sin polimorfismo — las reglas son pocas y
switch es más legible aquí que jerarquía por tipo).

```java
public final class ValidadorMeta {
    public static @Nullable Advertencia validar(Meta meta, DataStore ds) { ... }
    private ValidadorMeta() {}
}
```

Devuelve una `Advertencia` (valor inmutable con `String texto` + `int severidad`) o `null`
si la meta es plausible.

### Reglas

| Tipo | Regla | Mensaje sugerido |
|---|---|---|
| AUMENTAR_MEDIDA | `(objetivo - actual) / actual > 0.20` | *"Aumentar tu {medida} de {actual} a {objetivo} cm es un salto de {%}. Un aumento realista suele ser de 1-2 cm por mes con entrenamiento constante."* |
| REDUCIR_MEDIDA (cintura) | Si objetivo llevaría a `ICC < 0.72` (umbral neutro conservador, sin campo de sexo) | *"Ese objetivo te llevaría a un índice cintura-cadera muy bajo ({icc}). Considera un valor más moderado."* |
| REDUCIR_MEDIDA | `(actual - objetivo) / actual > 0.15` | *"Una reducción de {%} de tu {medida} es agresiva. Considera dividirla en metas más pequeñas."* |
| PESO_CORPORAL | IMC resultante < 18.5 | *"Ese peso te dejaría en bajo peso (IMC {imc}). Es riesgoso para tu salud."* |
| PESO_CORPORAL | IMC resultante > 30 | *"Ese peso te dejaría en rango de obesidad (IMC {imc})."* |
| PESO_CORPORAL | `abs(objetivo - actual) / actual > 0.15` | *"Un cambio de {%} de tu peso corporal es agresivo. Considera dividirlo en metas más pequeñas."* |
| PR_EJERCICIO | `pesoObjetivo > mejorPesoActual × 1.20` (mismo reps o más) | *"Subir {actual} → {objetivo} kg es un salto de {%}. Considera dividirlo (ej. primero {actual+5} kg × {reps})."* |
| PR_EJERCICIO | Sin historial del ejercicio | *"No tenemos registros previos de este ejercicio. La meta se crea, pero no podremos comparar contra un PR anterior."* |

Ninguna regla **bloquea**. El diálogo de crear meta muestra la advertencia y el usuario
elige entre **[Ajustar]** (vuelve al formulario) o **[Crear igual]** (`advertenciaAceptada = true`).

### Sin campo de sexo — decisión deliberada

`Usuario` no tiene sexo/género. Agregar el campo abre otra conversación (UX de género
inclusivo, privacidad, etc.). Para no ampliar scope, se usa un umbral neutro conservador
para ICC (0.72) y para IMC los rangos estándar de OMS que no dependen de sexo. Si en el
futuro se agrega `Usuario.sexo`, el validador se ajusta trivialmente.

`PerfilFisicoUtil` ya calcula IMC e ICC (verificado). Si algún método necesario es
privado, se hace público.

## Recomendador ampliado

`util/metas/RecomendadorEjercicios.java` combina tabla curada + filtrado dinámico +
parámetros de entrenamiento sugeridos.

### Retorno: `PlanRecomendado`

```java
public class PlanRecomendado {
    private String textoVolumen;               // "Para bíceps: 10-14 series/semana. Tienes 6."
    private int volumenSemanalActual;
    private int volumenObjetivoMin, volumenObjetivoMax;
    private int seriesSugeridas;
    private int repsSugeridas;
    private int repsSugeridasMax;              // techo del rango si aplica
    private int esquemaProgresionSugerido;     // constantes de RutinaEjercicio.ESQUEMA_*
    private List<Recomendacion> ejercicios;    // ordenados por prioridad (faltantes primero)
    private String textoCoberturaCompleta;     // no-null solo si todos los candidatos ya están
}
```

### `Recomendacion`

```java
public class Recomendacion {
    private final Ejercicio ejercicio;
    private final String etiqueta;      // "Falta en tus rutinas" / "Ya lo haces en 2 rutinas"
    private final int prioridad;        // 0=falta, 1=1 rutina, 2=2+ rutinas
    // getters
}
```

### Tabla curada de ejercicios (`CURADOS`)

Mapa `Map<String, List<String>>` estático en `RecomendadorEjercicios`. Las claves son
estables (no dependen de UI ni traducción) y las listas usan nombres exactos como aparecen
en `Ejercicio.nombre` del catálogo curado. **Los 60 nombres de esta tabla ya fueron
verificados contra `app/src/main/assets/catalogo.json` — todos existen literalmente.**

```java
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
```

### Cómo se elige la clave para meta de PR

Se normaliza el nombre del ejercicio principal de la meta (lowercase, sin acentos) y se
busca por **substring** de palabras clave (más robusto que match exacto porque el catálogo
tiene múltiples variantes de cada lift):

```java
private static String construirClavePr(Ejercicio ej) {
    String n = normalizar(ej.getNombre());  // lowercase + sin acentos
    if (n.contains("press") && n.contains("banca")) return "pr_press_banca";
    if (n.contains("peso muerto"))                   return "pr_peso_muerto";
    if (n.contains("sentadilla"))                    return "pr_sentadilla";
    if (n.contains("dominada"))                      return "pr_dominadas";
    return null;  // dispara el fallback
}
```

Ejemplos de match:
- "Press de banca con barra" → `pr_press_banca`
- "Press banca inclinado con mancuerna" → `pr_press_banca`
- "Sentadilla completa con barra" → `pr_sentadilla`
- "Sentadilla frontal con barra" → `pr_sentadilla`
- "Peso muerto rumano con barra" → `pr_peso_muerto`
- "Dominada arquero" → `pr_dominadas`

Si el resultado es `null` (ej. PR de "Elevaciones laterales"), fallback: 5 ejercicios del
catálogo con mismo `musculoObjetivo` que el ejercicio de la meta, ordenados por
menos-usado-en-rutinas.

### Tabla de rango de volumen semanal (`RANGO_VOLUMEN`)

```java
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
```

Referencia: rangos MEV-MAV de literatura de hipertrofia (Renaissance Periodization).

### Cálculo de volumen semanal actual

Nueva función en `AnalisisMuscular` (o en `RecomendadorEjercicios` para no cargar más
`AnalisisMuscular`):

```java
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
    // Cuántas veces por semana toca cada rutina, asumiendo distribución uniforme
    int nRutinas = Math.max(1, rutinas.size());
    int pasadasSemana = Math.max(1, diasEntrenoSemana);
    return (int) Math.round((double) seriesPorPasada * pasadasSemana / nRutinas);
}
```

Aproximación conservadora (asume distribución uniforme entre rutinas). Es suficiente para
un rango orientativo.

### Parámetros por tipo de meta (`ParamsProgresion`)

```java
public class ParamsProgresion {
    public final int series;
    public final int repsMin;
    public final int repsMax;         // si == repsMin, no aplica rango (esquema no-doble)
    public final int esquemaProgresion;
}

// En RecomendadorEjercicios:
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
```

### Peso inicial sugerido

```java
private static double pesoInicialSugerido(String ejercicioId, List<Sesion> sesiones) {
    // Iterar de más reciente a más antigua; devolver peso de la última serie completada
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
```

Sin cross-referencia entre ejercicios distintos. Si no hay historial, `0.0` y el usuario
completa manualmente.

### Algoritmo `recomendar(Meta, DataStore)`

```java
public static PlanRecomendado recomendar(Meta meta, DataStore ds) {
    // 1. Determinar clave y grupo objetivo
    String clave = construirClave(meta, ds);
    String grupoObjetivo = grupoDeMeta(meta, ds);  // p. ej. "Bíceps" para brazoCm

    // 2. Candidatos del mapa curado
    List<String> nombresCurados = CURADOS.getOrDefault(clave, Collections.emptyList());
    List<Ejercicio> candidatos = resolverNombres(nombresCurados, ds.getEjercicios());
    if (candidatos.isEmpty() && meta.getTipo() == Meta.TIPO_PR_EJERCICIO) {
        candidatos = fallbackPorMusculo(grupoObjetivo, ds.getEjercicios());
    }

    // 3. Contar apariciones en rutinas para etiquetar
    Map<String, Integer> conteos = contarEnRutinas(ds.getRutinas());

    // 4. Construir Recomendaciones ordenadas
    List<Recomendacion> resultado = new ArrayList<>();
    for (Ejercicio e : candidatos) {
        int veces = conteos.getOrDefault(e.getId(), 0);
        String etiqueta = veces == 0 ? "Falta en tus rutinas"
                        : veces == 1 ? "Ya lo haces en 1 rutina"
                                     : "Ya lo haces en " + veces + " rutinas";
        int prioridad = veces == 0 ? 0 : veces == 1 ? 1 : 2;
        resultado.add(new Recomendacion(e, etiqueta, prioridad));
    }
    resultado.sort(Comparator.comparingInt(Recomendacion::getPrioridad));

    // 5. Volumen semanal
    Map<String, Ejercicio> catalogo = indexarCatalogo(ds.getEjercicios());
    int actual = seriesSemanalesDeGrupo(grupoObjetivo, ds.getRutinas(), catalogo,
                                         ds.getUsuario() != null ? ds.getUsuario().getDiasEntrenoSemana() : 3);
    int[] rango = RANGO_VOLUMEN.getOrDefault(grupoObjetivo, new int[]{8, 14});
    String textoVolumen = "Para " + grupoObjetivo + ": " + rango[0] + "-" + rango[1]
        + " series/semana. Actualmente tienes " + actual + ".";

    // 6. Params por tipo
    ParamsProgresion params = paramsPara(meta);

    // 7. Detectar cobertura completa
    String textoCobertura = null;
    if (!resultado.isEmpty() && resultado.stream().allMatch(r -> r.getPrioridad() > 0)) {
        textoCobertura = "Ya tienes buena cobertura de este grupo en tus rutinas. "
            + "Considera aumentar el volumen (más series o más peso) antes de agregar más ejercicios.";
    }

    return new PlanRecomendado(textoVolumen, actual, rango[0], rango[1],
        params.series, params.repsMin, params.repsMax, params.esquemaProgresion,
        resultado, textoCobertura);
}
```

### Flujo de "Agregar ejercicio a rutina"

Cuando el usuario tap "Agregar a rutina..." en el `PlanSugeridoDialog`:

1. Se abre `AgregarEjercicioARutinaDialog` con:
   - Selector de rutina destino (chips o dropdown, auto-selecciona si solo hay una).
   - Preview editable: series, repeticiones (piso del rango), repeticionesMax (techo si
     aplica), peso (sugerido o 0 editable), esquema (sugerido, editable con spinner).
2. Confirmar → crea `RutinaEjercicio` con esos valores → `rutina.agregarEjercicio(...)` →
   `dataManager.saveSync()` (persistir antes de cerrar el diálogo).
3. Toast confirmatorio: *"Agregado a {nombreRutina}"*.

## UI

### 1. `MetasFragment` reescrito

Layout raíz `NestedScrollView` con `LinearLayout` vertical:

- **Header**: `TextView` "Metas · N/3 activas" + `MaterialButton` "+ Nueva meta".
  Botón deshabilitado si N == 3.
- **Sección Activas**: `RecyclerView` (id `list_metas_activas`) vertical con adapter
  `MetasActivasAdapter`.
- **Sección Historial**: `TextView` clickable "Historial (M)" con chevron; al tap expande
  un `RecyclerView` (id `list_metas_historial`) con adapter `MetasHistorialAdapter`.
- **Estado vacío**: si no hay metas activas ni historial, muestra card ilustrativo con
  texto explicativo y CTA "+ Crear tu primera meta".

Layout: `fragment_metas.xml`.

### 2. Card de meta activa (`item_meta_activa.xml`)

`MaterialCardView` con margen 12dp:

- Header horizontal: emoji del tipo (💪 aumentar, 📉 reducir, ⚖ peso, 🏋 PR — como texto
  literal en `TextView`, sin drawable) + `text_titulo` + menú overflow (`ImageButton`).
- `LinearProgressIndicator` horizontal, color acento naranja.
- `text_progreso`: "32 → 40 cm (20 %)"
- `text_deadline`: "faltan 30 días" (o "vencida hace X días" en rojo). Solo visible si
  `fechaObjetivo != null`.
- `text_recordatorio_actualizar`: banner tenue "Actualiza tu {medida} en Mi físico". Solo
  visible si la meta requiere una medida y el último `RegistroFisico` con esa medida > 0
  fue hace > 14 días. Para PR: si la última sesión con ese `ejercicioId` fue hace > 14
  días.
- Botón `button_ver_plan`: "Ver plan sugerido" → abre `PlanSugeridoDialog`.

Menú overflow: **Editar** (título + objetivo + deadline; se muestra en `EditarMetaDialog`
simple, reusa parte del layout de crear) y **Descartar**.

### 3. Card de meta en historial (`item_meta_historial.xml`)

`MaterialCardView` más compacta:

- Ícono de estado: ✓ verde si cumplida, ✗ gris si descartada.
- `text_titulo`.
- `text_meta_resultado`: para cumplida "Alcanzada: 40 cm"; para descartada "Descartada".
- `text_meta_duracion`: "cumplida en 6 semanas y 2 días".

### 4. `CrearMetaDialog`

`AlertDialog` custom con `LinearLayout` vertical dentro de un `ScrollView` (para no
recortar en pantallas pequeñas):

- **`ChipGroup` de tipo** (`Muscular`, `Cintura/cadera`, `Peso corporal`, `PR ejercicio`),
  single-select. Cambiar chip cambia el formulario debajo.
- **Contenedor dinámico** `FrameLayout` (id `container_formulario_meta`) donde se infla el
  layout correcto según tipo:
  - *Muscular*: `layout/form_meta_muscular.xml` — `Spinner` (brazo/pierna/pecho) +
    `TextInputEditText` "objetivo cm".
  - *Cintura/cadera*: `layout/form_meta_reducir.xml` — `Spinner` (cintura/cadera) +
    `TextInputEditText` "objetivo cm".
  - *Peso corporal*: `layout/form_meta_peso.xml` — `TextInputEditText` "objetivo kg".
  - *PR ejercicio*: `layout/form_meta_pr.xml` — `MaterialButton` "Seleccionar ejercicio"
    (abre `EjercicioPicker` filtrado por ejercicios con historial en sesiones) +
    `TextInputEditText` peso kg + reps.
- **Título** (autogenerado, editable): `TextInputEditText` `edit_titulo_meta`. Se
  regenera cada vez que cambian los valores, hasta que el usuario lo modifique
  manualmente (detectar con flag `usuarioModificoTitulo`).
- **Deadline opcional**: `CheckBox` "Poner fecha objetivo" → al marcar aparece
  `MaterialDatePicker` inline.
- Botones **Crear** / **Cancelar**.

Al tap Crear:
1. Construir `Meta` en memoria (con `valorInicial` fotografiado del último registro/PR).
2. `Advertencia adv = ValidadorMeta.validar(meta, dataStore)`.
3. Si `adv == null` → `dataManager.agregarMeta(meta)`. Si retorna `false` (ya hay 3
   activas), Toast y no cerrar diálogo.
4. Si `adv != null` → `MaterialAlertDialog` con `adv.texto` + botones **[Ajustar]** (dismiss
   sin cerrar el diálogo de crear, vuelve al formulario) / **[Crear igual]**
   (`meta.setAdvertenciaAceptada(true)` + `dataManager.agregarMeta(meta)`).

### 5. `PlanSugeridoDialog`

`Dialog` con `ScrollView` + `LinearLayout` vertical:

- Header: "Plan sugerido para: {titulo}"
- Card 1 — Volumen: `text_volumen_semanal` con texto del `PlanRecomendado.textoVolumen`.
- Card 2 — Parámetros: "Ejercicios nuevos: {series}×{reps} reps, esquema {nombre}"
  formateando `PlanRecomendado.params`.
- Card 3 — Lista de ejercicios: `RecyclerView` interno de items compactos
  (`item_plan_ejercicio_sugerido.xml`):
  - Nombre del ejercicio.
  - Badge (`text_etiqueta`) con color según prioridad (verde/gris).
  - Botón "Agregar a rutina..." (abre `AgregarEjercicioARutinaDialog`).

  Si `PlanRecomendado.textoCoberturaCompleta != null`, oculta la lista y muestra ese texto
  en un card informativo.
- Botón Cerrar al final.

### 6. `AgregarEjercicioARutinaDialog`

`AlertDialog` custom:

- Header: "Agregar {nombre ejercicio}"
- Selector de rutina destino (`ChipGroup` con las rutinas del usuario, single-select).
  Si hay solo una, se auto-selecciona y el ChipGroup queda deshabilitado.
- Preview editable: `TextInputEditText` series, reps, reps máx (si aplica), peso.
  `Spinner` de esquema.
- Botones **Agregar** / **Cancelar**.

Al confirmar: construye `RutinaEjercicio` con valores editados,
`rutina.agregarEjercicio(re)`, `dataManager.saveSync()`, Toast "Agregado a {rutina}",
cierra y también cierra `PlanSugeridoDialog` para volver al card de meta.

### 7. Banner en `EditRoutineActivity`

Encima del banner de límite muscular (Slice 2) y del banner de sugerencias de progresión
(Slice 1). Se muestra si hay al menos una meta activa con `PlanRecomendado.ejercicios` no
vacío y algún candidato con prioridad 0 (falta):

```
Meta activa: "Aumentar brazo a 40 cm"
Tienes 6/10-14 series semanales de bíceps.       [ Ver plan ] [ Ocultar ]
```

"Ver plan" → abre `PlanSugeridoDialog` con esa meta. "Ocultar" → oculta hasta reabrir la
Activity (flag no persistido, solo en memoria).

Si hay varias metas activas con brecha, se muestra la que tenga menor % de progreso (más
lejana del objetivo).

### 8. `CelebracionMetaDialog`

`MaterialAlertDialog` centrado:

- 🏆 grande al inicio (emoji en `TextView`, 48sp).
- Título: **"¡Meta cumplida!"**
- Cuerpo: nombre de la meta + tiempo desde creación calculado a la vuelo ("En 6 semanas
  y 2 días").
- Si hay múltiples cumplidas simultáneamente: enumeración vertical.
- Vibración corta al mostrar (reusa `Vibrator` del proyecto).
- Botones: **"Ver mis metas"** (abre `MetasFragment` con highlight de la card recién
  cumplida durante 3s) / **"Continuar"** (dismiss).

### 9. Notificación push ≥ 80 %

Nuevo helper `service/NotificacionMetasHelper.java` con:

- Canal `"metas_progreso"` (importance DEFAULT — interrumpe) creado la primera vez que se
  usa (`NotificationManager.createNotificationChannel`).
- `notificarCerca(Context, Meta)`: construye `NotificationCompat.Builder` con:
  - Título: "Estás cerca de cumplir tu meta"
  - Cuerpo: "{titulo} — 82 % de progreso. ¡Sigue así!"
  - Ícono: `R.drawable.ic_notification` (o el que ya usa `SesionTrackingService`)
  - Intent: abre `MainActivity` con extra `"meta_destacada_id"` = `meta.id` para que
    MetasFragment resalte el card al abrir.
  - Auto-cancel al tap.
- Id de notificación: `meta.getId().hashCode()` (único por meta, no se pisan entre metas
  distintas).

Reusa el permiso `POST_NOTIFICATIONS` que ya solicita `ActiveSessionActivity` en Android
13+. Si el permiso no está concedido, `notify()` falla silenciosamente — se acepta (el
recordatorio del card + el detectivo automático de cumplimiento siguen funcionando).

### 10. Ajustes: bloque `diasEntrenoSemana`

En `AjustesActivity`, agregar un `MaterialCardView` nuevo (después del bloque de fatiga):

- Título: "Días a la semana que entrenas"
- Subtítulo: "Ayuda al recomendador a calcular tu volumen semanal"
- `ChipGroup` con chips 1 / 2 / 3 / 4 / 5 / 6 / 7 (single-select, `selectionRequired=true`,
  default el valor guardado en `Usuario.diasEntrenoSemana`).
- Al cambiar el chip: `usuario.setDiasEntrenoSemana(valor)` + `dataManager.save()`
  (async). `Usuario` se serializa en `datos.json`, no en `catalogo_local.json`, por eso
  `save()` y no `saveCatalogo()`.

### Archivos nuevos y modificados

**Nuevos Java:**

- `model/Meta.java`
- `util/metas/EvaluadorMeta.java` (abstract)
- `util/metas/EvalMedidaMuscular.java`, `EvalReducirMedida.java`, `EvalPesoCorporal.java`,
  `EvalPrEjercicio.java` (package-private)
- `util/metas/ValidadorMeta.java`
- `util/metas/Advertencia.java` (valor inmutable)
- `util/metas/RecomendadorEjercicios.java`
- `util/metas/PlanRecomendado.java`, `Recomendacion.java`, `ParamsProgresion.java` (valores)
- `ui/MetasActivasAdapter.java`, `MetasHistorialAdapter.java`
- `ui/CrearMetaDialog.java`, `EditarMetaDialog.java`, `PlanSugeridoDialog.java`,
  `AgregarEjercicioARutinaDialog.java`, `CelebracionMetaDialog.java`
- `service/NotificacionMetasHelper.java`

**Modificados Java:**

- `model/DataStore.java`: lista `metas` + normalización.
- `model/Usuario.java`: campo `diasEntrenoSemana`.
- `data/DataManager.java`: `getMetasActivas`, `getMetasHistorial`, `agregarMeta`,
  `descartarMeta`, `evaluarMetas`.
- `ui/MetasFragment.java`: rewrite total.
- `ui/AjustesActivity.java` + `activity_ajustes.xml`: bloque de días.
- `ui/PhysicalProfileActivity.java`: llamar `evaluarMetas()` tras guardar + mostrar
  `CelebracionMetaDialog`.
- `ui/ActiveSessionActivity.java`: llamar `evaluarMetas()` en `finalizarSesion()` +
  mostrar `CelebracionMetaDialog`.
- `ui/EditRoutineActivity.java` + `activity_edit_routine.xml`: banner de recomendación.
- `ui/MainActivity.java`: manejar extra `"meta_destacada_id"` para pasar a MetasFragment.
- `util/AnalisisMuscular.java` (opcional): agregar `seriesSemanalesDeGrupo` si conviene
  centralizar allí en vez de en `RecomendadorEjercicios`.

**Layouts nuevos:**

`fragment_metas.xml`, `item_meta_activa.xml`, `item_meta_historial.xml`,
`dialog_crear_meta.xml`, `form_meta_muscular.xml`, `form_meta_reducir.xml`,
`form_meta_peso.xml`, `form_meta_pr.xml`, `dialog_plan_sugerido.xml`,
`item_plan_ejercicio_sugerido.xml`, `dialog_agregar_ejercicio_a_rutina.xml`,
`dialog_editar_meta.xml`.

**Layouts modificados:** `activity_ajustes.xml`, `activity_edit_routine.xml`.

**Manifest**: nada (canal de notificación se registra en runtime).

## Testing y verificación

Sin tests unitarios (convención del proyecto). Verificación manual en dispositivo. El
plan de ejecución detalla el checklist completo; puntos críticos:

- Compilación + chequeo de invariante Gson.
- Respaldo de `datos.json` real antes de instalar.
- Retro-compat con datos.json viejo (sin `metas` ni `diasEntrenoSemana`).
- Flujo end-to-end de crear meta y verla cumplirse al guardar registro/sesión.
- Validaciones al crear: los 4 tipos con casos de advertencia.
- Recomendador: verificar que ejercicios curados sí existen en el catálogo y aparecen
  ordenados correctamente (faltantes primero).
- Aceptar ejercicio sugerido: pre-llenado correcto, se agrega a la rutina elegida y
  persiste tras reinstalar.
- Banner en editor: aparece con brecha, se oculta con "Ocultar".
- Notificación push única al cruzar 80 %.
- Límite de 3 activas.
- Ajustes: cambio de `diasEntrenoSemana` recalcula volumen mostrado en cards.
- Restaurar `datos.json` real.

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Nombres en `CURADOS` no matchean `catalogo.json` | Claude verifica cada nombre antes de commitear el plan; big pickle corre un chequeo previo a implementar. |
| Trampa #4 (`save()` desde `load()`) | `evaluarMetas()` solo se llama desde UI, nunca desde carga inicial. |
| Trampa #3 (colección nueva null en JSON viejo) | `DataStore.normalizarColecciones()` cubre `metas`. |
| Cambio de peso corporal dispara evaluación pero la validación es one-shot al crear | `evaluarMetas()` solo detecta cumplimiento, no re-valida metas existentes. Documentado. |
| Falta campo de sexo en `Usuario` para umbrales de ICC | Umbral neutro conservador (0.72). Si se agrega sexo después, ajuste trivial. |
| Notificación requiere `POST_NOTIFICATIONS` en Android 13+ | Reusa el permiso que ya solicita `SesionTrackingService`. Fallback silencioso si no está. |
| Personas con dos metas contradictorias (subir peso + reducir cintura) | No se prohíbe; cada meta se evalúa independiente. Puede ser legítimo (recomposición corporal). |
| El recomendador infiere frecuencia por `diasEntrenoSemana / N_rutinas` — usuario con 6 rutinas y 3 días entrena cada una cada 2 semanas | Aproximación conservadora es aceptable; el usuario ve el número y lo interpreta. |

## Fuera de alcance

- **Módulo de cardio** — slice futuro independiente. Extenderá el catálogo con categoría
  (cardio/HIIT/estiramiento) o creará un catálogo cardio aparte, y necesitará modelo
  distinto de sesión (tiempo/distancia en vez de peso/reps).
- **Distribución automática de ejercicios entre días** — el recomendador solo lista
  candidatos; el usuario elige a qué rutina agregar cada uno. Un "planificador de rutinas
  semanales" es su propio slice.
- **Modificación automática de rutinas existentes** — el sistema nunca cambia una rutina
  sin confirmación explícita.
- **Estimación cross-ejercicio de peso inicial** (ej. deducir press inclinado 80 % del
  press plano). Solo usamos el último peso histórico del mismo ejercicio; si no hay,
  dejamos 0.
- **Gráfica de evolución histórica por meta** (progreso a lo largo del tiempo). El card
  muestra solo el estado actual. Slice futuro si se pide.
- **Compartir metas entre usuarios / red social de metas**. Excluido por §5.2 del
  proyecto.
- **Coach conversacional o IA que dé consejos**. Excluido por §5.2.
- **Metas compuestas** como un único objetivo. Se maneja creando dos metas separadas.
- **Cross-referenciar metas del pasado** en nuevas metas (ej. "cuando lograste bajar 5 kg
  antes, tardaste X"). Historial existe pero no se cruza.
- **Integración con nutrición / descanso** — parte de la visión a largo plazo (ver
  memoria `vision-app-integral`), no este slice.

## No-metas

- **La app no garantiza que alcances tu meta.** Es un termómetro, no un coach.
- **La app no reemplaza a un profesional médico.** Las advertencias son señales de sentido
  común basadas en IMC/ICC, no diagnóstico.
- **El recomendador no es un programa de entrenamiento personalizado.** Es una tabla
  curada + filtro por lo que ya tienes.

## Riesgos aceptados

- Rangos MEV/MAV y series/reps por meta son puntos de referencia genéricos, no
  personalizados. Un usuario avanzado puede querer más volumen del que la app sugiere.
  Se acepta: la app orienta, no dicta.
- Match por nombre exacto entre `CURADOS` y `catalogo.json` es frágil. Se acepta con la
  mitigación del cross-check previo.
- La notificación push única no repite si el usuario retrocede y vuelve al 80 %. Se
  acepta para evitar spam.
- Sin campo de sexo para umbrales de ICC — umbral neutro (0.72). Se acepta con nota para
  ajuste futuro si se agrega el campo.
