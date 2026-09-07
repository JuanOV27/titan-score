package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

/** Una rutina: el plan de ejercicios con sus series, repeticiones y pesos objetivo. */
public class Rutina extends EntidadIdentificable {

    private String nombre;
    private List<RutinaEjercicio> ejercicios = new ArrayList<>();

    /** Constructor sin argumentos para Gson. Privado: nadie más debe crear una Rutina vacía. */
    private Rutina() {
    }

    public Rutina(String id, String nombre) {
        super(id);
        this.nombre = nombre;
    }

    public void agregarEjercicio(RutinaEjercicio ejercicio) {
        ejercicios.add(ejercicio);
    }

    /** Cambia la posición de un ejercicio dentro de la rutina (para arrastrar y soltar). */
    public void moverEjercicio(int desde, int hasta) {
        ejercicios.add(hasta, ejercicios.remove(desde));
    }

    public void quitarEjercicio(int posicion) {
        if (posicion >= 0 && posicion < ejercicios.size()) {
            ejercicios.remove(posicion);
        }
    }

    public int getCantidadEjercicios() {
        return ejercicios.size();
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** Lista viva: el adaptador de edición de rutinas la mutan directamente. */
    public List<RutinaEjercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<RutinaEjercicio> ejercicios) {
        this.ejercicios = ejercicios;
    }

    @Override
    public String toString() {
        return nombre != null ? nombre : super.toString();
    }
}
