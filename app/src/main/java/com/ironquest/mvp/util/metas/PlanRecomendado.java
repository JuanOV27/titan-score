package com.ironquest.mvp.util.metas;

import java.util.List;

/** Salida del RecomendadorEjercicios. Valor inmutable. */
public final class PlanRecomendado {

    private final String textoVolumen;
    private final int volumenSemanalActual;
    private final int volumenObjetivoMin;
    private final int volumenObjetivoMax;
    private final int seriesSugeridas;
    private final int repsSugeridas;
    private final int repsSugeridasMax;
    private final int esquemaProgresionSugerido;
    private final List<Recomendacion> ejercicios;
    private final String textoCoberturaCompleta;   // null si hay al menos un ejercicio faltante

    public PlanRecomendado(String textoVolumen, int volumenSemanalActual,
                            int volumenObjetivoMin, int volumenObjetivoMax,
                            int seriesSugeridas, int repsSugeridas, int repsSugeridasMax,
                            int esquemaProgresionSugerido,
                            List<Recomendacion> ejercicios, String textoCoberturaCompleta) {
        this.textoVolumen = textoVolumen;
        this.volumenSemanalActual = volumenSemanalActual;
        this.volumenObjetivoMin = volumenObjetivoMin;
        this.volumenObjetivoMax = volumenObjetivoMax;
        this.seriesSugeridas = seriesSugeridas;
        this.repsSugeridas = repsSugeridas;
        this.repsSugeridasMax = repsSugeridasMax;
        this.esquemaProgresionSugerido = esquemaProgresionSugerido;
        this.ejercicios = ejercicios;
        this.textoCoberturaCompleta = textoCoberturaCompleta;
    }

    public String getTextoVolumen() { return textoVolumen; }
    public int getVolumenSemanalActual() { return volumenSemanalActual; }
    public int getVolumenObjetivoMin() { return volumenObjetivoMin; }
    public int getVolumenObjetivoMax() { return volumenObjetivoMax; }
    public int getSeriesSugeridas() { return seriesSugeridas; }
    public int getRepsSugeridas() { return repsSugeridas; }
    public int getRepsSugeridasMax() { return repsSugeridasMax; }
    public int getEsquemaProgresionSugerido() { return esquemaProgresionSugerido; }
    public List<Recomendacion> getEjercicios() { return ejercicios; }
    public String getTextoCoberturaCompleta() { return textoCoberturaCompleta; }

    public boolean tieneEjerciciosSugeridos() {
        return ejercicios != null && !ejercicios.isEmpty();
    }

    public boolean tieneAlgunoFaltante() {
        if (ejercicios == null) return false;
        for (Recomendacion r : ejercicios) if (r.getPrioridad() == 0) return true;
        return false;
    }
}
