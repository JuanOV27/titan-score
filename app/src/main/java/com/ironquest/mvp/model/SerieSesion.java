package com.ironquest.mvp.model;

/** Una serie dentro de un ejercicio de una sesión. Objeto de valor, sin identidad propia. */
public class SerieSesion {

    private int numero;
    private double peso;
    private int repeticiones;
    private boolean completada;

    /** Constructor sin argumentos para Gson. */
    private SerieSesion() {
    }

    public SerieSesion(int numero, double peso, int repeticiones, boolean completada) {
        this.numero = numero;
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.completada = completada;
    }

    /**
     * Esfuerzo de la serie. El peso corporal se registra como 0 kg y en ese caso el esfuerzo son
     * las repeticiones a secas: de lo contrario una sesión de dominadas valdría cero.
     */
    public double calcularVolumen() {
        return peso > 0 ? peso * repeticiones : repeticiones;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public int getRepeticiones() {
        return repeticiones;
    }

    public void setRepeticiones(int repeticiones) {
        this.repeticiones = repeticiones;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }
}
