package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.Ejercicio;

/** Un ejercicio candidato con etiqueta y prioridad para el PlanSugeridoDialog. Inmutable. */
public final class Recomendacion {
    private final Ejercicio ejercicio;
    private final String etiqueta;
    private final int prioridad;   // 0 = falta en rutinas, 1 = en 1 rutina, 2 = en 2+ rutinas

    public Recomendacion(Ejercicio ejercicio, String etiqueta, int prioridad) {
        this.ejercicio = ejercicio;
        this.etiqueta = etiqueta;
        this.prioridad = prioridad;
    }

    public Ejercicio getEjercicio() { return ejercicio; }
    public String getEtiqueta() { return etiqueta; }
    public int getPrioridad() { return prioridad; }
}
