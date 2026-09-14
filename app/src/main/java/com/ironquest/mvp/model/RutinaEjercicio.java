package com.ironquest.mvp.model;

/**
 * La configuración de un ejercicio dentro de una rutina: cuántas series, cuántas repeticiones,
 * con cuánto peso, y bajo qué esquema de progresión. Objeto de valor, sin identidad propia.
 */
public class RutinaEjercicio {

    public static final int ESQUEMA_NINGUNO = 0;
    public static final int ESQUEMA_LINEAL = 1;
    public static final int ESQUEMA_GREYSKULL = 2;
    public static final int ESQUEMA_DOBLE_PROGRESION = 3;
    public static final int ESQUEMA_AUTOMATICO = 4;

    private String ejercicioId;
    private int series;
    private int repeticiones;
    private double peso;
    /** {@code int} primitivo a propósito: en datos.json viejo lee 0 (=ESQUEMA_NINGUNO) sin migración. */
    private int esquemaProgresion;
    /** Techo del rango para ESQUEMA_DOBLE_PROGRESION; repeticiones (arriba) es el piso. */
    private int repeticionesMax;
    /** Si es {@code true}, el motor no genera SugerenciaPendiente para este ejercicio. */
    private boolean silenciarSugerencia;

    /** Constructor sin argumentos para Gson. */
    private RutinaEjercicio() {
    }

    public RutinaEjercicio(String ejercicioId, int series, int repeticiones, double peso) {
        this.ejercicioId = ejercicioId;
        this.series = series;
        this.repeticiones = repeticiones;
        this.peso = peso;
    }

    /** Techo efectivo del rango de doble progresión: sin techo configurado, el piso hace de tope. */
    public int getTechoRepeticiones() {
        return repeticionesMax > 0 ? repeticionesMax : repeticiones;
    }

    public String getEjercicioId() {
        return ejercicioId;
    }

    public void setEjercicioId(String ejercicioId) {
        this.ejercicioId = ejercicioId;
    }

    public int getSeries() {
        return series;
    }

    public void setSeries(int series) {
        this.series = series;
    }

    public int getRepeticiones() {
        return repeticiones;
    }

    public void setRepeticiones(int repeticiones) {
        this.repeticiones = repeticiones;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public int getEsquemaProgresion() {
        return esquemaProgresion;
    }

    public void setEsquemaProgresion(int esquemaProgresion) {
        this.esquemaProgresion = esquemaProgresion;
    }

    public int getRepeticionesMax() {
        return repeticionesMax;
    }

    public void setRepeticionesMax(int repeticionesMax) {
        this.repeticionesMax = repeticionesMax;
    }

    public boolean isSilenciarSugerencia() {
        return silenciarSugerencia;
    }

    public void setSilenciarSugerencia(boolean silenciarSugerencia) {
        this.silenciarSugerencia = silenciarSugerencia;
    }
}
