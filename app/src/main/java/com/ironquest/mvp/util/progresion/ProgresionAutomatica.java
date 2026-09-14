package com.ironquest.mvp.util.progresion;

import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.Usuario;

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
        // Fallback sin ventana: la lógica real vive en sugerirAutomatica(), sobre las últimas
        // 3 sesiones. Aquí solo queda el caso teórico de una sesión única.
        int estadoUnico = clasificarSesion(config, ultima);
        return decidir(config, pesoBase, incremento, new int[]{estadoUnico});
    }

    @Override
    protected boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada) {
        return clasificarSesion(config, sesionPasada) != ESTADO_FALLIDO;
    }

    /**
     * Reimplementa el flujo estándar de sugerir(): este esquema necesita ver las últimas
     * 3 sesiones, no solo la última. La clase base filtra el historial por ejercicio;
     * replicamos ese filtrado aquí para trabajar con la ventana.
     */
    @Override
    public Sugerencia sugerir(RutinaEjercicio config, Ejercicio ejercicio, Usuario usuario, List<Sesion> historial) {
        double incremento = getIncrementoEfectivo(ejercicio, usuario);

        List<EjercicioSesion> pasadas = historialDelEjercicio(config.getEjercicioId(), historial);
        if (pasadas.isEmpty()) {
            return new Sugerencia(config.getPeso(), config.getRepeticiones(),
                    "Primera vez con este esquema: arrancas con el plan de la rutina.", false);
        }

        // Tomar las últimas 3 (o menos si hay menos).
        int desde = Math.max(0, pasadas.size() - 3);
        List<EjercicioSesion> ventana = pasadas.subList(desde, pasadas.size());

        int[] estados = new int[ventana.size()];
        for (int i = 0; i < ventana.size(); i++) {
            estados[i] = clasificarSesion(config, ventana.get(i));
        }

        double pesoBase = pesoDeLaSesion(ventana.get(ventana.size() - 1), config.getPeso());
        return decidir(config, pesoBase, incremento, estados);
    }

    /** Copia local del filtrado por ejercicio (privado en la clase base). Ordena por fecha ISO. */
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