package com.ironquest.mvp.util.progresion;

import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.RutinaEjercicio;

/**
 * Objeto Nulo: el ejercicio no tiene esquema asignado, así que la "sugerencia" es siempre el
 * plan manual que el usuario ya configuró en la rutina, sin mirar el historial.
 */
final class SinProgresion extends EstrategiaProgresion {

    @Override
    protected boolean requiereHistorial() {
        return false;
    }

    @Override
    protected Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento) {
        return new Sugerencia(config.getPeso(), config.getRepeticiones(), null, false);
    }

    @Override
    protected boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada) {
        // Nunca se invoca: requiereHistorial() en false corta el flujo antes del conteo de
        // estancamiento. Se implementa igual, sin lanzar, para no dejar una trampa si el flujo
        // de EstrategiaProgresion cambia en el futuro.
        return true;
    }
}
