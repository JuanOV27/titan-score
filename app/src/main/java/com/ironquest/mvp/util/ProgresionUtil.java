package com.ironquest.mvp.util;

import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.SerieSesion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sugiere el peso/reps del próximo intento de un ejercicio a partir de su historial real,
 * según el esquema de progresión configurado en la rutina (lineal, Greyskull-AMRAP o doble
 * progresión). La sugerencia nunca se persiste: se recalcula cada vez que arranca una sesión
 * nueva, igual que la racha.
 *
 * El peso base para incrementar siempre sale del último peso realmente registrado (no de
 * {@code RutinaEjercicio.getPeso()}, que nunca lo toca este motor), porque el usuario puede haber
 * ajustado el peso con los botones +/- durante la sesión.
 */
public final class ProgresionUtil {

    private static final double INCREMENTO_KG = 2.5;
    private static final int SESIONES_PARA_ESTANCAMIENTO = 3;

    private ProgresionUtil() {
    }

    /** Valor de salida inmutable: solo se construye aquí dentro y solo se lee desde fuera. */
    public static class Sugerencia {

        private final double peso;
        private final int repeticiones;
        private final String explicacion;
        private final boolean estancado;

        Sugerencia(double peso, int repeticiones, String explicacion, boolean estancado) {
            this.peso = peso;
            this.repeticiones = repeticiones;
            this.explicacion = explicacion;
            this.estancado = estancado;
        }

        public double getPeso() {
            return peso;
        }

        public int getRepeticiones() {
            return repeticiones;
        }

        public String getExplicacion() {
            return explicacion;
        }

        public boolean isEstancado() {
            return estancado;
        }
    }

    public static Sugerencia sugerir(RutinaEjercicio config, List<Sesion> historial) {
        if (config.getEsquemaProgresion() == RutinaEjercicio.ESQUEMA_NINGUNO) {
            return new Sugerencia(config.getPeso(), config.getRepeticiones(), null, false);
        }

        List<EjercicioSesion> pasadas = historialDelEjercicio(config.getEjercicioId(), historial);
        if (pasadas.isEmpty()) {
            return new Sugerencia(config.getPeso(), config.getRepeticiones(),
                    "Primera vez con este esquema: arrancas con el plan de la rutina.", false);
        }

        EjercicioSesion ultima = pasadas.get(pasadas.size() - 1);
        double pesoBase = pesoDeLaSesion(ultima, config.getPeso());

        int fallosSeguidos = contarFallosSeguidos(config, pasadas);
        if (fallosSeguidos >= SESIONES_PARA_ESTANCAMIENTO) {
            double pesoDeload = redondearA(pesoBase * 0.9, INCREMENTO_KG);
            return new Sugerencia(pesoDeload, config.getRepeticiones(),
                    "Llevas " + fallosSeguidos + " sesiones seguidas sin cumplir el objetivo. Posible "
                            + "estancamiento: baja a " + formatearPeso(pesoDeload) + "kg y retoma la progresión.",
                    true);
        }

        switch (config.getEsquemaProgresion()) {
            case RutinaEjercicio.ESQUEMA_GREYSKULL:
                return sugerirGreyskull(config, ultima);
            case RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION:
                return sugerirDobleProgresion(config, ultima, pesoBase);
            case RutinaEjercicio.ESQUEMA_LINEAL:
            default:
                return sugerirLineal(config, ultima, pesoBase);
        }
    }

    private static Sugerencia sugerirLineal(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase) {
        boolean exito = todasLasSeriesCumplen(ultima, config.getRepeticiones());
        if (exito) {
            double nuevoPeso = pesoBase + INCREMENTO_KG;
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

    private static Sugerencia sugerirGreyskull(RutinaEjercicio config, EjercicioSesion ultima) {
        SerieSesion amrap = ultima.getUltimaSerieCompletada();
        if (amrap == null) {
            return new Sugerencia(config.getPeso(), config.getRepeticiones(),
                    "Repite " + formatearPeso(config.getPeso()) + "kg.", false);
        }
        if (amrap.getRepeticiones() >= config.getRepeticiones()) {
            double nuevoPeso = amrap.getPeso() + INCREMENTO_KG;
            return new Sugerencia(nuevoPeso, config.getRepeticiones(),
                    "Tu última serie (AMRAP) fue de " + amrap.getRepeticiones() + " reps con " + formatearPeso(amrap.getPeso())
                            + "kg, cumpliendo el objetivo de " + config.getRepeticiones() + ". Sugerencia: sube a "
                            + formatearPeso(nuevoPeso) + "kg.",
                    false);
        }
        return new Sugerencia(amrap.getPeso(), config.getRepeticiones(),
                "Tu última serie (AMRAP) fue de " + amrap.getRepeticiones() + " reps, por debajo del objetivo de "
                        + config.getRepeticiones() + ". Repite " + formatearPeso(amrap.getPeso()) + "kg.",
                false);
    }

    private static Sugerencia sugerirDobleProgresion(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase) {
        int techo = config.getTechoRepeticiones();
        int repsLogradas = repeticionesMinimasCompletadas(ultima);

        if (repsLogradas < config.getRepeticiones()) {
            return new Sugerencia(pesoBase, config.getRepeticiones(),
                    "No llegaste a " + config.getRepeticiones() + " reps en todas las series. Repite "
                            + formatearPeso(pesoBase) + "kg.",
                    false);
        }
        if (repsLogradas >= techo) {
            double nuevoPeso = pesoBase + INCREMENTO_KG;
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

    // ---- historial ----

    private static List<EjercicioSesion> historialDelEjercicio(String ejercicioId, List<Sesion> sesiones) {
        List<Sesion> finalizadas = new ArrayList<>();
        for (Sesion sesion : sesiones) {
            if (sesion.estaFinalizada()) {
                finalizadas.add(sesion);
            }
        }
        finalizadas.sort(Comparator.comparing(s -> LocalDateTime.parse(s.getFechaHoraInicio())));

        List<EjercicioSesion> resultado = new ArrayList<>();
        for (Sesion sesion : finalizadas) {
            EjercicioSesion es = sesion.buscarEjercicio(ejercicioId);
            if (es != null) {
                resultado.add(es);
            }
        }
        return resultado;
    }

    private static int contarFallosSeguidos(RutinaEjercicio config, List<EjercicioSesion> pasadas) {
        int fallos = 0;
        for (int i = pasadas.size() - 1; i >= 0; i--) {
            if (fueExitosa(config, pasadas.get(i))) {
                break;
            }
            fallos++;
        }
        return fallos;
    }

    private static boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada) {
        switch (config.getEsquemaProgresion()) {
            case RutinaEjercicio.ESQUEMA_GREYSKULL: {
                SerieSesion amrap = sesionPasada.getUltimaSerieCompletada();
                return amrap != null && amrap.getRepeticiones() >= config.getRepeticiones();
            }
            case RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION:
                return repeticionesMinimasCompletadas(sesionPasada) >= config.getRepeticiones();
            case RutinaEjercicio.ESQUEMA_LINEAL:
            default:
                return todasLasSeriesCumplen(sesionPasada, config.getRepeticiones());
        }
    }

    private static boolean todasLasSeriesCumplen(EjercicioSesion sesionPasada, int repsObjetivo) {
        boolean algunaCompletada = false;
        for (SerieSesion serie : sesionPasada.getSeries()) {
            if (!serie.isCompletada()) {
                continue;
            }
            algunaCompletada = true;
            if (serie.getRepeticiones() < repsObjetivo) {
                return false;
            }
        }
        return algunaCompletada;
    }

    /** Mínimo de reps entre las series completadas: la serie más floja determina si el rango se cumplió. */
    private static int repeticionesMinimasCompletadas(EjercicioSesion sesionPasada) {
        int minimo = -1;
        for (SerieSesion serie : sesionPasada.getSeries()) {
            if (!serie.isCompletada()) {
                continue;
            }
            if (minimo == -1 || serie.getRepeticiones() < minimo) {
                minimo = serie.getRepeticiones();
            }
        }
        return Math.max(minimo, 0);
    }


    private static double pesoDeLaSesion(EjercicioSesion sesionPasada, double fallback) {
        SerieSesion ultima = sesionPasada.getUltimaSerieCompletada();
        return ultima != null ? ultima.getPeso() : fallback;
    }

    private static double redondearA(double valor, double paso) {
        return Math.round(valor / paso) * paso;
    }

    private static String formatearPeso(double peso) {
        if (peso == Math.floor(peso)) {
            return String.valueOf((long) peso);
        }
        return String.valueOf(peso);
    }
}
