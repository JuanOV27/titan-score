# Diseño orientado a objetos — Titan Score

> Mapa de los cuatro pilares de POO (encapsulación, abstracción, herencia, polimorfismo) a
> clases y líneas concretas del código. Escrito para la evidencia académica del curso Móviles 2
> (Universidad Popular del Cesar), a pedido explícito del profesor. El diseño completo y su
> justificación están en `docs/specs/2026-09-02-refactor-poo-design.md`; este documento es el
> resumen navegable.

## 1. Encapsulación

Los 9 modelos de `model/` tienen **campos privados**, **getters y setters**, y **constructores**
— uno sin argumentos (para que Gson lo use al leer `datos.json`) y uno completo (para el resto
del código). Ejemplo, [`model/Sesion.java`](../app/src/main/java/com/ironquest/mvp/model/Sesion.java):

```java
public class Sesion extends EntidadIdentificable {
    private String rutinaId;
    private String rutinaNombre;
    private double volumenReal;
    // ...
    private Sesion() { }                                    // constructor sin argumentos, para Gson
    public Sesion(String id, String rutinaId, ...) { ... }  // constructor completo

    public double getVolumenReal() { return volumenReal; }
    public void setVolumenReal(double volumenReal) { this.volumenReal = volumenReal; }
}
```

Además de los getters/setters, los modelos exponen **métodos de dominio** que encapsulan
comportamiento, no solo datos — evita que la lógica de negocio viva dispersa en las Activities:

| Método | Archivo |
|---|---|
| `Sesion.finalizar(fechaHoraFin)` | [`model/Sesion.java`](../app/src/main/java/com/ironquest/mvp/model/Sesion.java) |
| `Sesion.estaFinalizada()`, `buscarEjercicio(id)` | ídem |
| `EjercicioSesion.calcularVolumen()`, `getUltimaSerieCompletada()` | [`model/EjercicioSesion.java`](../app/src/main/java/com/ironquest/mvp/model/EjercicioSesion.java) |
| `SerieSesion.calcularVolumen()` | [`model/SerieSesion.java`](../app/src/main/java/com/ironquest/mvp/model/SerieSesion.java) |
| `Rutina.agregarEjercicio(...)`, `quitarEjercicio(pos)` | [`model/Rutina.java`](../app/src/main/java/com/ironquest/mvp/model/Rutina.java) |
| `Usuario.coincideHash(hash)` | [`model/Usuario.java`](../app/src/main/java/com/ironquest/mvp/model/Usuario.java) |
| `DataStore.buscarRutina(id)`, `buscarEjercicio(id)` | [`model/DataStore.java`](../app/src/main/java/com/ironquest/mvp/model/DataStore.java) |

Fuera de `model/`, la encapsulación se aplicó donde había campos públicos equivalentes:
`BarChartView.Barra` ([`ui/BarChartView.java`](../app/src/main/java/com/ironquest/mvp/ui/BarChartView.java))
y `Sugerencia` ([`util/progresion/Sugerencia.java`](../app/src/main/java/com/ironquest/mvp/util/progresion/Sugerencia.java))
son objetos de valor inmutables: campos `private final`, solo getters, sin setters — no tiene
sentido mutar un resultado ya calculado.

## 2. Abstracción

Dos clases abstractas, cada una resolviendo un problema real del proyecto:

- **`EntidadIdentificable`** ([`model/EntidadIdentificable.java`](../app/src/main/java/com/ironquest/mvp/model/EntidadIdentificable.java))
  — el campo `id` y su acceso, una sola vez. La heredan las 5 entidades con identidad propia
  (`Ejercicio`, `Rutina`, `Sesion`, `RegistroFisico`, `Usuario`).
- **`EstrategiaProgresion`** ([`util/progresion/EstrategiaProgresion.java`](../app/src/main/java/com/ironquest/mvp/util/progresion/EstrategiaProgresion.java))
  — ver §4, es también el ejemplo central de polimorfismo.
- **`BaseActivity`** ([`ui/BaseActivity.java`](../app/src/main/java/com/ironquest/mvp/ui/BaseActivity.java))
  — un único método `configurarToolbar(...)` que resuelve, en un solo lugar, un bug que ya
  había costado tiempo de depuración (`setSupportActionBar()` debe llamarse antes que
  `setTitle()`, o el título del manifiesto pisa el del layout).

## 3. Herencia

```
EntidadIdentificable (abstracta)
├── Ejercicio
├── Rutina
├── Sesion
├── RegistroFisico
└── Usuario

BaseActivity (abstracta, extends AppCompatActivity)
├── MainActivity
├── ActiveSessionActivity
├── EditRoutineActivity
├── HistoryActivity
├── SessionSummaryActivity
├── PhysicalProfileActivity
└── PhysicalHistoryActivity

EstrategiaProgresion (abstracta)
├── SinProgresion
├── ProgresionLineal
├── ProgresionGreyskull
└── ProgresionDoble
```

`RutinaEjercicio`, `SerieSesion`, `EjercicioSesion` y `DataStore` **no** heredan de
`EntidadIdentificable`: son objetos de valor sin identidad propia, subordinados a la entidad que
los contiene. `AuthActivity` **no** hereda de `BaseActivity`: es la única Activity sin toolbar.
Ninguna de las dos exclusiones es un descuido — forzar la herencia ahí habría sido decoración,
no diseño.

## 4. Polimorfismo

El ejemplo más completo está en el motor de progresión. Antes, `ProgresionUtil` decidía el
comportamiento con dos `switch` sobre una constante `int`. Ahora, cuatro clases implementan la
misma interfaz de dos métodos y el despacho lo hace la JVM, no un `switch`:

```java
// EstrategiaProgresion.java — método plantilla, fija el flujo común
public final Sugerencia sugerir(RutinaEjercicio config, List<Sesion> historial) {
    if (!requiereHistorial()) {
        return calcular(config, null, config.getPeso());
    }
    // ... busca el historial, cuenta fallos consecutivos ...
    return calcular(config, ultima, pesoBase);   // <-- despacho polimórfico
}

protected abstract Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase);
protected abstract boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada);
```

Cada subclase implementa `calcular()` (qué sugerir) y `fueExitosa()` (si una sesión pasada
cumplió el objetivo, consumido por el conteo de estancamiento en la clase base — un caso claro
de que el código compartido no necesita saber *cómo* cada esquema define el éxito, solo que
puede preguntarlo):

| Subclase | Regla | Archivo |
|---|---|---|
| `SinProgresion` | Objeto Nulo: sin esquema asignado, repite el plan manual | [`SinProgresion.java`](../app/src/main/java/com/ironquest/mvp/util/progresion/SinProgresion.java) |
| `ProgresionLineal` | Sube peso si todas las series llegaron al objetivo | [`ProgresionLineal.java`](../app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionLineal.java) |
| `ProgresionGreyskull` | Solo la última serie (AMRAP) decide | [`ProgresionGreyskull.java`](../app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionGreyskull.java) |
| `ProgresionDoble` | Sube reps dentro de un rango antes de subir peso | [`ProgresionDoble.java`](../app/src/main/java/com/ironquest/mvp/util/progresion/ProgresionDoble.java) |

La fábrica `EstrategiaProgresion.para(int esquemaProgresion)` traduce el entero que sí se
persiste en `datos.json` a la instancia correspondiente — la elección de estrategia nunca se
serializa, se resuelve en memoria cada vez que se pide una sugerencia.

En [`ui/ActiveSessionActivity.java`](../app/src/main/java/com/ironquest/mvp/ui/ActiveSessionActivity.java):

```java
Sugerencia sugerencia = EstrategiaProgresion.para(re.getEsquemaProgresion())
        .sugerir(re, dataStore.getSesiones());
```

Ese único punto de llamada no sabe ni le importa cuál de las cuatro estrategias se ejecuta.

## Visibilidad

Auditoría aplicada a `ui/`, `service/` y `data/`: todo campo y método que no forma parte de un
contrato usado desde otra clase es `private`. Ejemplos concretos:

- Los `ViewHolder` de los 4 adapters (`RutinaAdapter`, `HistorialAdapter`,
  `PhysicalHistoryAdapter`, `RutinaEjercicioEditAdapter`) tienen sus campos `View` y su
  constructor en `private` — la clase en sí queda con visibilidad de paquete porque
  `RecyclerView.Adapter<VH>` lo exige en la firma genérica, pero nada de lo que hay dentro
  necesita ser accesible desde fuera del archivo.
- `DataManager` (en `data/`) es la excepción deliberada: su superficie pública es
  intencionalmente amplia porque es el único punto de contacto entre el resto de la app y la
  persistencia — es la costura pensada para una futura migración a Firebase (`CLAUDE.md`).

## Lo que se decidió no forzar

- **Sin interfaces adicionales** para los adapters o las Activities: la arquitectura
  deliberadamente simple del proyecto (sin DI, sin Room, sin ViewModel — ver `CLAUDE.md`) sigue
  vigente. Los cuatro pilares se aplicaron donde resuelven un problema real del código, no como
  ejercicio decorativo.
- **Sin clase base genérica para los 4 `RecyclerView.Adapter`**: se evaluó y se descartó — son
  lo bastante distintos entre sí como para que una base añadiera indirección sin ahorro real
  (detalle en el spec de diseño).
- **El esquema de progresión sigue siendo un `int` en `datos.json`.** La jerarquía de
  `EstrategiaProgresion` vive enteramente en memoria; persistir un tipo polimórfico exigiría un
  adaptador de Gson dedicado que este proyecto no necesita.
