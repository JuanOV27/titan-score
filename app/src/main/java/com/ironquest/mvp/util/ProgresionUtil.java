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
 * {@code RutinaEjercicio.peso}, que nunca lo toca este motor), porque el usuario puede haber
 * ajustado el peso con los botones +/- durante la sesión.
 */
public final class ProgresionUtil {

    private static final double INCREMENTO_KG = 2.5;
    private static final int SESIONES_PARA_ESTANCAMIENTO = 3;

    private ProgresionUtil() {
    }

    public static class Sugerencia {
        public final double peso;
        public final int repeticiones;
        public final String explicacion;
        public final boolean estancado;

        Sugerencia(double peso, int repeticiones, String explicacion, boolean estancado) {
            this.peso = peso;
            this.repeticiones = repeticiones;
            this.explicacion = explicacion;
            this.estancado = estancado;
        }
    }

    public static Sugerencia sugerir(RutinaEjercicio config, List<Sesion> historial) {
        if (config.esquemaProgresion == RutinaEjercicio.ESQUEMA_NINGUNO) {
            return new Sugerencia(config.peso, config.repeticiones, null, false);
        }

        List<EjercicioSesion> pasadas = historialDelEjercicio(config.ejercicioId, historial);
        if (pasadas.isEmpty()) {
            return new Sugerencia(config.peso, config.repeticiones,
                    "Primera vez con este esquema: arrancas con el plan de la rutina.", false);
        }

        EjercicioSesion ultima = pasadas.get(pasadas.size() - 1);
        double pesoBase = pesoDeLaSesion(ultima, config.peso);

        int fallosSeguidos = contarFallosSeguidos(config, pasadas);
        if (fallosSeguidos >= SESIONES_PARA_ESTANCAMIENTO) {
            double pesoDeload = redondearA(pesoBase * 0.9, INCREMENTO_KG);
            return new Sugerencia(pesoDeload, config.repeticiones,
                    "Llevas " + fallosSeguidos + " sesiones seguidas sin cumplir el objetivo. Posible "
                            + "estancamiento: baja a " + formatearPeso(pesoDeload) + "kg y retoma la progresión.",
                    true);
        }

        switch (config.esquemaProgresion) {
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
        boolean exito = todasLasSeriesCumplen(ultima, config.repeticiones);
        if (exito) {
            double nuevoPeso = pesoBase + INCREMENTO_KG;
            return new Sugerencia(nuevoPeso, config.repeticiones,
                    "Cumpliste el objetivo de " + config.repeticiones + " reps en todas las series con "
                            + formatearPeso(pesoBase) + "kg. Sugerencia: sube a " + formatearPeso(nuevoPeso) + "kg.",
                    false);
        }
        return new Sugerencia(pesoBase, config.repeticiones,
                "No completaste el objetivo de " + config.repeticiones + " reps la última vez. Repite "
                        + formatearPeso(pesoBase) + "kg.",
                false);
    }

    private static Sugerencia sugerirGreyskull(RutinaEjercicio config, EjercicioSesion ultima) {
        SerieSesion amrap = ultimaSerieCompletada(ultima);
        if (amrap == null) {
            return new Sugerencia(config.peso, config.repeticiones,
                    "Repite " + formatearPeso(config.peso) + "kg.", false);
        }
        if (amrap.repeticiones >= config.repeticiones) {
            double nuevoPeso = amrap.peso + INCREMENTO_KG;
            return new Sugerencia(nuevoPeso, config.repeticiones,
                    "Tu última serie (AMRAP) fue de " + amrap.repeticiones + " reps con " + formatearPeso(amrap.peso)
                            + "kg, cumpliendo el objetivo de " + config.repeticiones + ". Sugerencia: sube a "
                            + formatearPeso(nuevoPeso) + "kg.",
                    false);
        }
        return new Sugerencia(amrap.peso, config.repeticiones,
                "Tu última serie (AMRAP) fue de " + amrap.repeticiones + " reps, por debajo del objetivo de "
                        + config.repeticiones + ". Repite " + formatearPeso(amrap.peso) + "kg.",
                false);
    }

    private static Sugerencia sugerirDobleProgresion(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase) {
        int techo = config.repeticionesMax > 0 ? config.repeticionesMax : config.repeticiones;
        int repsLogradas = repeticionesMinimasCompletadas(ultima);

        if (repsLogradas < config.repeticiones) {
            return new Sugerencia(pesoBase, config.repeticiones,
                    "No llegaste a " + config.repeticiones + " reps en todas las series. Repite "
                            + formatearPeso(pesoBase) + "kg.",
                    false);
        }
        if (repsLogradas >= techo) {
            double nuevoPeso = pesoBase + INCREMENTO_KG;
            return new Sugerencia(nuevoPeso, config.repeticiones,
                    "Llegaste a " + techo + " reps en todas las series con " + formatearPeso(pesoBase)
                            + "kg — tope del rango. Sugerencia: sube a " + formatearPeso(nuevoPeso) + "kg y vuelve a "
                            + config.repeticiones + " reps.",
                    false);
        }
        int siguienteReps = repsLogradas + 1;
        return new Sugerencia(pesoBase, siguienteReps,
                "Vas en " + repsLogradas + " de " + config.repeticiones + "-" + techo + " reps con "
                        + formatearPeso(pesoBase) + "kg. Sugerencia: intenta " + siguienteReps + " reps.",
                false);
    }

    // ---- historial ----

    private static List<EjercicioSesion> historialDelEjercicio(String ejercicioId, List<Sesion> sesiones) {
        List<Sesion> finalizadas = new ArrayList<>();
        for (Sesion sesion : sesiones) {
            if (sesion.fechaHoraFin != null) {
                finalizadas.add(sesion);
            }
        }
        finalizadas.sort(Comparator.comparing(s -> LocalDateTime.parse(s.fechaHoraInicio)));

        List<EjercicioSesion> resultado = new ArrayList<>();
        for (Sesion sesion : finalizadas) {
            for (EjercicioSesion es : sesion.ejercicios) {
                if (es.ejercicioId.equals(ejercicioId)) {
                    resultado.add(es);
                    break;
                }
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
        switch (config.esquemaProgresion) {
            case RutinaEjercicio.ESQUEMA_GREYSKULL: {
                SerieSesion amrap = ultimaSerieCompletada(sesionPasada);
                return amrap != null && amrap.repeticiones >= config.repeticiones;
            }
            case RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION:
                return repeticionesMinimasCompletadas(sesionPasada) >= config.repeticiones;
            case RutinaEjercicio.ESQUEMA_LINEAL:
            default:
                return todasLasSeriesCumplen(sesionPasada, config.repeticiones);
        }
    }

    private static boolean todasLasSeriesCumplen(EjercicioSesion sesionPasada, int repsObjetivo) {
        boolean algunaCompletada = false;
        for (SerieSesion serie : sesionPasada.series) {
            if (!serie.completada) {
                continue;
            }
            algunaCompletada = true;
            if (serie.repeticiones < repsObjetivo) {
                return false;
            }
        }
        return algunaCompletada;
    }

    /** Mínimo de reps entre las series completadas: la serie más floja determina si el rango se cumplió. */
    private static int repeticionesMinimasCompletadas(EjercicioSesion sesionPasada) {
        int minimo = -1;
        for (SerieSesion serie : sesionPasada.series) {
            if (!serie.completada) {
                continue;
            }
            if (minimo == -1 || serie.repeticiones < minimo) {
                minimo = serie.repeticiones;
            }
        }
        return Math.max(minimo, 0);
    }

    private static SerieSesion ultimaSerieCompletada(EjercicioSesion sesionPasada) {
        SerieSesion ultima = null;
        for (SerieSesion serie : sesionPasada.series) {
            if (serie.completada) {
                ultima = serie;
            }
        }
        return ultima;
    }

    private static double pesoDeLaSesion(EjercicioSesion sesionPasada, double fallback) {
        SerieSesion ultima = ultimaSerieCompletada(sesionPasada);
        return ultima != null ? ultima.peso : fallback;
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
