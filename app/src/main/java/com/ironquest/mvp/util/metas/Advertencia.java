package com.ironquest.mvp.util.metas;

/** Advertencia opcional que devuelve ValidadorMeta al crear una Meta. Inmutable. */
public final class Advertencia {

    public static final int SEVERIDAD_INFO = 0;
    public static final int SEVERIDAD_ATENCION = 1;

    private final String texto;
    private final int severidad;

    public Advertencia(String texto, int severidad) {
        this.texto = texto;
        this.severidad = severidad;
    }

    public String getTexto() { return texto; }
    public int getSeveridad() { return severidad; }
}
