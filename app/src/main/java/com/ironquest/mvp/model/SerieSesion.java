package com.ironquest.mvp.model;

public class SerieSesion {
    public int numero;
    public double peso;
    public int repeticiones;
    public boolean completada;

    public SerieSesion(int numero, double peso, int repeticiones, boolean completada) {
        this.numero = numero;
        this.peso = peso;
        this.repeticiones = repeticiones;
        this.completada = completada;
    }
}
