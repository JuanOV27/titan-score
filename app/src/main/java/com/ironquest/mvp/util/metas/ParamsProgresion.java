package com.ironquest.mvp.util.metas;

/** Parámetros de entrenamiento sugeridos según el tipo de meta. Valor inmutable. */
public final class ParamsProgresion {
    public final int series;
    public final int repsMin;
    public final int repsMax;         // si == repsMin, no aplica rango
    public final int esquemaProgresion;

    public ParamsProgresion(int series, int repsMin, int repsMax, int esquemaProgresion) {
        this.series = series;
        this.repsMin = repsMin;
        this.repsMax = repsMax;
        this.esquemaProgresion = esquemaProgresion;
    }
}
