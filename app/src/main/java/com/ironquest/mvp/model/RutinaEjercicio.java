package com.ironquest.mvp.model;

public class RutinaEjercicio {

    public static final int ESQUEMA_NINGUNO = 0;
    public static final int ESQUEMA_LINEAL = 1;
    public static final int ESQUEMA_GREYSKULL = 2;
    public static final int ESQUEMA_DOBLE_PROGRESION = 3;

    public String ejercicioId;
    public int series;
    public int repeticiones;
    public double peso;
    /** {@code int} primitivo a propósito: en datos.json viejo lee 0 (=ESQUEMA_NINGUNO) sin migración. */
    public int esquemaProgresion;
    /** Techo del rango para ESQUEMA_DOBLE_PROGRESION; repeticiones (arriba) es el piso. */
    public int repeticionesMax;

    public RutinaEjercicio(String ejercicioId, int series, int repeticiones, double peso) {
        this.ejercicioId = ejercicioId;
        this.series = series;
        this.repeticiones = repeticiones;
        this.peso = peso;
    }
}
