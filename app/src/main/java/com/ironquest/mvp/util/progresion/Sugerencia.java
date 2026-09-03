package com.ironquest.mvp.util.progresion;

/**
 * El próximo peso/reps sugeridos para un ejercicio, con la explicación de por qué. Valor de
 * salida inmutable: solo lo construyen las subclases de {@link EstrategiaProgresion} y solo se
 * lee desde fuera del paquete.
 */
public final class Sugerencia {

    private final double peso;
    private final int repeticiones;
    private final String explicacion;
    private final boolean estancado;

    Sugerencia(double peso, int repeticiones, String explicacion, boolean estancado) {
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.explicacion = explicacion;
        this.estancado = estancado;
    }

    public double getPeso() {
        return peso;
    }

    public int getRepeticiones() {
        return repeticiones;
    }

    public String getExplicacion() {
        return explicacion;
    }

    public boolean isEstancado() {
        return estancado;
    }
}
