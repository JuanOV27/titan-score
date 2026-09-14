package com.ironquest.mvp.util.progresion;

import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.RutinaEjercicio;

/** Sube el peso cuando todas las series completadas de la última sesión llegaron al objetivo. */
final class ProgresionLineal extends EstrategiaProgresion {

    @Override
    protected boolean requiereHistorial() {
        return true;
    }

    @Override
    protected Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento) {
        boolean exito = fueExitosa(config, ultima);
        if (exito) {
            double nuevoPeso = pesoBase + incremento;
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

    @Override
    protected boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada) {
        return todasLasSeriesCumplen(sesionPasada, config.getRepeticiones());
    }
}
