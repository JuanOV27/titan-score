package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Paquete para compartir una sola rutina entre instalaciones: la rutina y los ejercicios
 * completos que referencia, para que quien la reciba no necesite tenerlos ya. No hereda de
 * {@link EntidadIdentificable}: es un contenedor sin identidad propia, igual que {@link DataStore}.
 */
public class RutinaCompartida {

    private Rutina rutina;
    private List<Ejercicio> ejercicios = new ArrayList<>();

    public RutinaCompartida() {
    }

    public Rutina getRutina() {
        return rutina;
    }

    public void setRutina(Rutina rutina) {
        this.rutina = rutina;
    }

    public List<Ejercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<Ejercicio> ejercicios) {
        this.ejercicios = ejercicios;
    }
}
