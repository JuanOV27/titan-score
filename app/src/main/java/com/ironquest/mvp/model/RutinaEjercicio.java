package com.ironquest.mvp.model;

public class RutinaEjercicio {
    public String ejercicioId;
    public int series;
    public int repeticiones;
    public double peso;

    public RutinaEjercicio(String ejercicioId, int series, int repeticiones, double peso) {
        this.ejercicioId = ejercicioId;
        this.series = series;
        this.repeticiones = repeticiones;
        this.peso = peso;
    }
}
