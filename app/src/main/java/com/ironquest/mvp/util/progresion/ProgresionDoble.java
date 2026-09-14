package com.ironquest.mvp.util.progresion;

import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.RutinaEjercicio;

/**
 * Doble progresión por rango de reps: primero se sube dentro del rango
 * {@code repeticiones}–{@code repeticionesMax}, y solo al llegar al techo con todas las series
 * sube el peso y se reinicia al piso.
 */
final class ProgresionDoble extends EstrategiaProgresion {

    @Override
    protected boolean requiereHistorial() {
        return true;
    }

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

    @Override
    protected boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada) {
        return repeticionesMinimasCompletadas(sesionPasada) >= config.getRepeticiones();
    }
}
