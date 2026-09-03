# Refactorización a Programación Orientada a Objetos

> Diseño aprobado el 2026-09-02. Motivo: el profesor de Móviles 2 exige que el código presente
> una estructura orientada a objetos explícita — getters y setters, métodos, visibilidad de
> clases, constructores. Se implementan los cuatro pilares completos (encapsulación,
> abstracción, herencia, polimorfismo).

## Estado de partida

35 clases Java, 3.742 líneas, en `f51e38f`. Los 9 modelos de `model/` exponen **campos
públicos sin encapsulación** — una decisión que hoy está documentada como deliberada en
`IronQuestApp/CLAUDE.md` y que este refactor invierte. Hay ~250 accesos directos a esos campos
repartidos por `ui/`, `util/`, `data/` y `service/`.

## Alcance

| Capa | Cambio |
|---|---|
| `model/` | Campos `private` + getters/setters + constructores. Clase abstracta `EntidadIdentificable`. Métodos de dominio. |
| `util/progresion/` | Paquete nuevo. `ProgresionUtil` se disuelve en una jerarquía de estrategias. |
| `ui/` | Clase abstracta `BaseActivity`. Auditoría de visibilidad. |
| `data/`, `service/` | Solo adaptación a los getters/setters y a la visibilidad. Sin rediseño. |

Fuera de alcance: la clase base genérica para los cuatro `RecyclerView.Adapter` (descartada —
los adapters son lo bastante distintos entre sí como para que la base añada indirección sin
ahorro real). Tampoco se toca ninguna funcionalidad: el refactor es **invariante en
comportamiento**, con la única excepción declarada en «Efectos secundarios buscados».

## Invariantes que se conservan

- Nada fuera de `data/` importa `Gson`, `File`, `FileReader`, `FileWriter` ni llama a
  `getFilesDir()`. Verificable con el `grep` de `CLAUDE.md`.
- Sin dependencias nuevas. Sin Room, sin inyección de dependencias, sin ViewModel, sin
  corrutinas.
- Dominio en español, API de Android en inglés.
- El formato de `datos.json` no cambia de contenido (ver «Compatibilidad de persistencia»).

---

## Pilar 1 — Encapsulación

Los 9 modelos pasan a campos `private` con getter y setter por cada campo mutable. Forma
canónica, con `Sesion` de ejemplo:

```java
public class Sesion extends EntidadIdentificable {

    private String rutinaId;
    private String rutinaNombre;
    private String fechaHoraInicio;
    private String fechaHoraFin;
    private List<EjercicioSesion> ejercicios = new ArrayList<>();
    private double volumenPlaneado;
    private double volumenReal;
    private int porcentajeCumplimiento;

    /** Constructor sin argumentos para Gson. Privado: nadie más debe crear una Sesión vacía. */
    private Sesion() {
    }

    public Sesion(String id, String rutinaId, String rutinaNombre, String fechaHoraInicio) {
        super(id);
        this.rutinaId = rutinaId;
        this.rutinaNombre = rutinaNombre;
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public String getRutinaNombre() { return rutinaNombre; }

    public void setRutinaNombre(String rutinaNombre) { this.rutinaNombre = rutinaNombre; }

    // ... resto de accesores
}
```

### Constructores por clase

| Clase | Constructor sin argumentos | Constructor completo |
|---|---|---|
| `DataStore` | `public` (se usa `new DataStore()` en `DataManager`) | — |
| `Ejercicio` | `private` (solo Gson) | `public Ejercicio(id, nombre, grupoMuscular)` |
| `EjercicioSesion` | `private` | `public EjercicioSesion(ejercicioId)` |
| `RegistroFisico` | `public` (hoy se construye vacío y se llena por pasos) | — |
| `RutinaEjercicio` | `private` | `public RutinaEjercicio(ejercicioId, series, repeticiones, peso)` |
| `Rutina` | `private` | `public Rutina(id, nombre)` |
| `SerieSesion` | `private` | `public SerieSesion(numero, peso, repeticiones, completada)` |
| `Sesion` | `private` | `public Sesion(id, rutinaId, rutinaNombre, fechaHoraInicio)` |
| `Usuario` | `public` (hoy se construye vacío y se llena por pasos) | — |

Gson acepta constructores sin argumentos privados: los invoca vía reflexión con
`setAccessible(true)`.

### Efecto secundario buscado: se elimina la trampa #3

Hoy, seis de las nueve clases no declaran constructor sin argumentos, así que Gson recurre a
`UnsafeAllocator` y **los inicializadores de campo no se ejecutan al deserializar**. Por eso
`Sesion.ejercicios = new ArrayList<>()` puede quedar en `null` si un `datos.json` viejo no
trae la clave. Al declarar el constructor sin argumentos en todas, Gson lo usa, los
inicializadores corren y las colecciones dejan de poder ser `null` por esa vía.

Es el único cambio de comportamiento del refactor, es estrictamente más seguro, y obliga a
reescribir la trampa #3 de `CLAUDE.md`.

---

## Pilar 2 — Abstracción

### `model/EntidadIdentificable`

```java
public abstract class EntidadIdentificable {

    private String id;

    protected EntidadIdentificable() {
    }

    protected EntidadIdentificable(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}
```

La heredan las cinco entidades que tienen identidad propia: `Ejercicio`, `Rutina`, `Sesion`,
`RegistroFisico`, `Usuario`.

**No la heredan** `RutinaEjercicio`, `SerieSesion`, `EjercicioSesion` ni `DataStore`: no tienen
campo `id` y forzarles uno sería inventar estado para lucir la jerarquía. Son objetos de valor
subordinados a su entidad contenedora.

### `ui/BaseActivity`

Abstracta, extiende `AppCompatActivity`. La heredan las 7 Activities que montan toolbar
(`AuthActivity` no tiene). Expone un único punto de entrada protegido:

```java
protected void configurarToolbar(int toolbarId, String titulo, boolean conFlechaAtras)
```

que hace `setSupportActionBar()`, luego `setTitle()` —en ese orden, que es lo que importa— y
opcionalmente `setDisplayHomeAsUpEnabled(true)`. Con eso la **trampa #1** de `CLAUDE.md`
(`setSupportActionBar` pisa el título del XML con el `android:label` del manifiesto) deja de
ser posible por construcción.

---

## Pilar 3 — Herencia + Pilar 4 — Polimorfismo

### Paquete `util/progresion/`

Hoy `ProgresionUtil` decide el comportamiento con dos `switch` sobre constantes `int`
(`ProgresionUtil.java:71` y `:180`). Se sustituyen por despacho polimórfico:

```
EstrategiaProgresion          abstract, public
├── SinProgresion             public final
├── ProgresionLineal          public final
├── ProgresionGreyskull       public final
└── ProgresionDoble           public final
```

Más `Sugerencia` (clase `public final` con campos `private final` y solo getters, sin setters:
es un valor inmutable de salida).

**Método plantilla.** `EstrategiaProgresion.sugerir(...)` es `public final` y fija el flujo
común, idéntico al de hoy:

1. Sin historial del ejercicio → mensaje de «primera vez».
2. `contarFallosSeguidos(...) >= 3` → deload a `pesoBase * 0.9` redondeado a 2,5 kg, con
   `estancado = true`.
3. En cualquier otro caso, delega en el abstracto `calcular(...)`.

Las subclases implementan dos métodos abstractos:

```java
protected abstract Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase);

protected abstract boolean fueExitosa(RutinaEjercicio config, EjercicioSesion pasada);
```

`fueExitosa` es lo que hoy es el segundo `switch`, y lo consume el conteo de estancamiento de
la clase base — un caso limpio de polimorfismo consumido desde código compartido.

Los ayudantes actuales (`ultimaSerieCompletada`, `todasLasSeriesCumplen`,
`repeticionesMinimasCompletadas`, `redondearA`, `formatearPeso`, `historialDelEjercicio`)
bajan a `protected` o `protected static` en la base.

**Método fábrica.** `public static EstrategiaProgresion para(int esquema)` traduce el entero
guardado en `RutinaEjercicio.esquemaProgresion` a la instancia correspondiente. Las cuatro
constantes `ESQUEMA_*` permanecen donde están, en `RutinaEjercicio`.

**Frontera de persistencia (crítica).** Lo que se guarda en `datos.json` sigue siendo el `int
esquemaProgresion`. La estrategia se resuelve en memoria a partir de ese entero y **nunca se
serializa**: Gson no sabe reconstruir un tipo polimórfico sin un adaptador dedicado, y no
vamos a introducir uno. Esta frontera queda anotada en el Javadoc de `EstrategiaProgresion`.

Decisión relacionada: **no** se añade `RutinaEjercicio.getEstrategia()`. Haría que `model/`
dependiera de `util/progresion/` mientras `util/progresion/` depende de `model/` — un ciclo
entre paquetes innecesario. La resolución se hace en el punto de uso,
`ActiveSessionActivity`.

`ProgresionUtil` se elimina.

---

## Métodos de dominio

Regla: **solo se traslada lógica que ya existe.** No se inventa comportamiento nuevo. Lista
cerrada:

| Método nuevo | Origen actual |
|---|---|
| `Sesion.finalizar(String fechaHoraFin)` | `ActiveSessionActivity.java:421-439` — fija la fecha, suma el volumen real y calcula el % de cumplimiento |
| `Sesion.estaFinalizada()` | las comprobaciones `fechaHoraFin != null` dispersas en `ProgresionUtil` y `EstadisticasUtil` |
| `Sesion.buscarEjercicio(String ejercicioId)` | el bucle anidado de `ProgresionUtil.historialDelEjercicio` |
| `EjercicioSesion.calcularVolumen()` | `ActiveSessionActivity.unidadEsfuerzo(...)` (`:402`) aplicado a las series completadas |
| `EjercicioSesion.agregarSerie(SerieSesion)` | los `series.add(...)` de `ActiveSessionActivity` |
| `SerieSesion.registrar(double peso, int repeticiones)` | los ajustes con los botones +/- |
| `SerieSesion.calcularVolumen()` | `unidadEsfuerzo(peso, repeticiones)` |
| `Rutina.agregarEjercicio(RutinaEjercicio)` / `quitarEjercicio(int)` | `EditRoutineActivity` |
| `Rutina.getCantidadEjercicios()` | `rutina.ejercicios.size()` en adapters |
| `DataStore.buscarRutina(String id)` | bucle duplicado en `EditRoutineActivity.java:80` y `ActiveSessionActivity.java:410` |
| `DataStore.buscarEjercicio(String id)` | los mapas `catalogoPorId` que hoy se arman a mano |

Los setters completos siguen existiendo aunque el método de dominio cubra el caso; conviven.

---

## Compatibilidad de persistencia

Gson serializa **por nombre de campo**, no por visibilidad, y recorre la cadena de superclases.
Por eso encapsular y heredar no altera el *contenido* de `datos.json`: los mismos nombres, los
mismos valores, los mismos tipos.

**Salvedad honesta sobre el orden de claves.** `ReflectiveTypeAdapterFactory` recorre primero
los campos declarados en la clase y después los de la superclase. Al subir `id` a
`EntidadIdentificable`, esa clave pasará a escribirse **al final** de cada entidad en lugar de
al principio, la próxima vez que la app guarde. Es inocuo — Gson lee por nombre, el orden le da
igual, y el archivo se reescribe entero en cada `save()` — pero significa que la comprobación
de regresión **no puede ser un `diff` de bytes**. Debe ser una comparación semántica del árbol
JSON.

### Prueba de ida y vuelta, antes de tocar el teléfono

Con `javac`/`java` en JVM pura, contra los modelos refactorizados reales:

1. Cargar `respaldo_ironquest_telefono.json` con Gson.
2. Volver a serializarlo.
3. Comparar los dos árboles con `JsonParser.parseString(...).equals(...)` — igualdad
   estructural, insensible al orden de claves.
4. Comprobar además que ningún campo quedó en `null` que antes tuviera valor.

**Si esta prueba no pasa, el refactor se detiene y nada llega al dispositivo.**

---

## Verificación

Además de la ida y vuelta de Gson, se reconstruyen y ejecutan las dos suites de prueba
independientes que ya se usaron en las rebanadas anteriores, compiladas directamente contra el
código de producción:

- **21 casos del motor de progresión** (los tres esquemas, estancamiento, deload, peso base
  dinámico). Deben dar resultados idénticos a los de `ProgresionUtil`.
- **6 casos del cálculo de racha.**

Después, por rebanada: `./gradlew assembleDebug` → `adb install -r` → humo en pantalla →
`adb shell run-as com.ironquest.mvp cat files/datos.json` comparado semánticamente con el
respaldo previo → commit.

### Datos reales del usuario

Aplica la regla no negociable de `CLAUDE.md`. `datos.json` en el teléfono contiene historial
real de entrenamiento. Se respalda antes de cada instalación, no se escribe sobre él sin
aprobación explícita y específica, y no se fabrican sesiones de prueba en el historial real.
La verificación en dispositivo es de solo lectura salvo aprobación puntual.

---

## Plan de entrega: tres rebanadas

Un solo commit de ~250 llamados sería imposible de revisar y de bisecar. Una sesión por
rebanada, un commit por rebanada, verificada en el teléfono antes de cerrar.

### R1 — Encapsulación de modelos

Los 9 modelos a campos privados con accesores y constructores; `EntidadIdentificable`; los
métodos de dominio de la tabla; y los ~250 puntos de llamada actualizados en `ui/`, `util/`,
`data/` y `service/`. Incluye la prueba de ida y vuelta de Gson. Es la rebanada grande.

### R2 — Jerarquía de estrategias de progresión

Se crea `util/progresion/` con las seis clases, se elimina `ProgresionUtil`, se adapta
`ActiveSessionActivity`. Validada contra los 21 casos.

### R3 — `BaseActivity`, visibilidad y documentación

`BaseActivity` y su adopción en las 7 Activities con toolbar. Auditoría de visibilidad en
`ui/`, `service/` y `data/`: campos y métodos auxiliares a `private`, constantes a
`private static final`, `ViewHolder` a `private static` donde no necesiten la instancia
externa. Más:

- **`CLAUDE.md` actualizado.** Hoy afirma *«Modelos con campos públicos, sin encapsulación. Es
  una decisión consciente del proyecto»* — queda al revés. La trampa #3 se reescribe para
  reflejar que el constructor sin argumentos ya la neutraliza.
- **`docs/diseno_orientado_a_objetos.md`**: tabla que mapea cada pilar a las clases y líneas
  concretas donde se materializa, para que el profesor no tenga que buscarlo.

---

## Criterios de aceptación

1. Ningún campo de instancia público queda en `model/`. Las únicas declaraciones `public` que
   sobreviven ahí son las cuatro constantes `public static final int ESQUEMA_*` de
   `RutinaEjercicio`, que son parte deliberada de su API.
2. Cada modelo tiene getter para todos sus campos y setter para todos los mutables.
3. Existen y se usan `EntidadIdentificable`, `BaseActivity` y la jerarquía
   `EstrategiaProgresion`.
4. `ProgresionUtil` no existe; ningún `switch` sobre `ESQUEMA_*` queda en el código.
5. El `grep` de la invariante de capas de `CLAUDE.md` sigue devolviendo vacío.
6. La prueba de ida y vuelta de Gson pasa sobre el respaldo real.
7. Las suites de 21 y 6 casos pasan íntegras.
8. La app compila, instala, y el recorrido de humo (Inicio → Entrenar → sesión → resumen →
   Historial → Estadísticas → Mi físico) se comporta igual que antes.
9. El `datos.json` del teléfono es semánticamente idéntico antes y después.
