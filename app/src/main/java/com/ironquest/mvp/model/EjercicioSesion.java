package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

/** Las series que se hicieron de un ejercicio dentro de una sesión. */
public class EjercicioSesion {

    private String ejercicioId;
    private List<SerieSesion> series = new ArrayList<>();

    /** Constructor sin argumentos para Gson. */
    private EjercicioSesion() {
    }

    public EjercicioSesion(String ejercicioId) {
        this.ejercicioId = ejercicioId;
    }

    public void agregarSerie(SerieSesion serie) {
        series.add(serie);
    }

    /** Suma el esfuerzo de las series marcadas como completadas; las pendientes no cuentan. */
    public double calcularVolumen() {
        double total = 0;
        for (SerieSesion serie : series) {
            if (serie.isCompletada()) {
                total += serie.calcularVolumen();
            }
        }
        return total;
    }

    /** La última serie completada, que es la de referencia en el esquema Greyskull (AMRAP). */
    public SerieSesion getUltimaSerieCompletada() {
        SerieSesion ultima = null;
        for (SerieSesion serie : series) {
            if (serie.isCompletada()) {
                ultima = serie;
            }
        }
        return ultima;
    }

    public String getEjercicioId() {
        return ejercicioId;
    }

    public void setEjercicioId(String ejercicioId) {
        this.ejercicioId = ejercicioId;
    }

    /** Lista viva: los adaptadores de la sesión activa la mutan directamente. */
    public List<SerieSesion> getSeries() {
        return series;
    }

    public void setSeries(List<SerieSesion> series) {
        this.series = series;
    }
}
