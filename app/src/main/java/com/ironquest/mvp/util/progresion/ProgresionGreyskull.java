package com.ironquest.mvp.util.progresion;

import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.SerieSesion;

/**
 * Regla de progresión por AMRAP tomada de Greyskull LP: solo la última serie completada de la
 * sesión (la "AMRAP") decide si se sube el peso. No implementa el programa A/B completo, que no
 * encaja con rutinas rotativas arbitrarias definidas por el usuario.
 */
final class ProgresionGreyskull extends EstrategiaProgresion {

    @Override
    protected boolean requiereHistorial() {
        return true;
    }

    @Override
    protected Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase) {
        SerieSesion amrap = ultima.getUltimaSerieCompletada();
        if (amrap == null) {
            return new Sugerencia(config.getPeso(), config.getRepeticiones(),
                    "Repite " + formatearPeso(config.getPeso()) + "kg.", false);
        }
        if (amrap.getRepeticiones() >= config.getRepeticiones()) {
            double nuevoPeso = amrap.getPeso() + INCREMENTO_KG;
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

    @Override
    protected boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada) {
        SerieSesion amrap = sesionPasada.getUltimaSerieCompletada();
        return amrap != null && amrap.getRepeticiones() >= config.getRepeticiones();
    }
}
