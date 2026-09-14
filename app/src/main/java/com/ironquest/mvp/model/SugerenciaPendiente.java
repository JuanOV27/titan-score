package com.ironquest.mvp.model;

/**
 * Sugerencia de ajuste de peso/reps que el motor generó al finalizar una sesión. Vive en
 * {@link DataStore} hasta que el usuario la acepta (se aplica a {@link RutinaEjercicio}) o
 * la ignora (se elimina). Se persiste como entidad con identidad para poder aparecer en
 * dos superficies sin duplicar estado.
 */
public class SugerenciaPendiente extends EntidadIdentificable {

    public static final int TIPO_SUBIR_PESO = 0;
    public static final int TIPO_MANTENER = 1;
    public static final int TIPO_DELOAD = 2;
    public static final int TIPO_SUBIR_REPS = 3;

    private String rutinaId;
    private String ejercicioId;
    private String sesionOrigenId;
    private double pesoActual;
    private double pesoSugerido;
    private int repeticionesActuales;
    private int repeticionesSugeridas;
    private String explicacion;
    private int tipo;
    private String fechaCreacion;

    /** Constructor sin argumentos para Gson. */
    private SugerenciaPendiente() {
    }

    public SugerenciaPendiente(String id, String rutinaId, String ejercicioId,
                                String sesionOrigenId, double pesoActual, double pesoSugerido,
                                int repeticionesActuales, int repeticionesSugeridas,
                                String explicacion, int tipo, String fechaCreacion) {
        super(id);
        this.rutinaId = rutinaId;
        this.ejercicioId = ejercicioId;
        this.sesionOrigenId = sesionOrigenId;
        this.pesoActual = pesoActual;
        this.pesoSugerido = pesoSugerido;
        this.repeticionesActuales = repeticionesActuales;
        this.repeticionesSugeridas = repeticionesSugeridas;
        this.explicacion = explicacion;
        this.tipo = tipo;
        this.fechaCreacion = fechaCreacion;
    }

    public String getRutinaId() { return rutinaId; }
    public void setRutinaId(String rutinaId) { this.rutinaId = rutinaId; }

    public String getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(String ejercicioId) { this.ejercicioId = ejercicioId; }

    public String getSesionOrigenId() { return sesionOrigenId; }
    public void setSesionOrigenId(String sesionOrigenId) { this.sesionOrigenId = sesionOrigenId; }

    public double getPesoActual() { return pesoActual; }
    public void setPesoActual(double pesoActual) { this.pesoActual = pesoActual; }

    public double getPesoSugerido() { return pesoSugerido; }
    public void setPesoSugerido(double pesoSugerido) { this.pesoSugerido = pesoSugerido; }

    public int getRepeticionesActuales() { return repeticionesActuales; }
    public void setRepeticionesActuales(int repeticionesActuales) { this.repeticionesActuales = repeticionesActuales; }

    public int getRepeticionesSugeridas() { return repeticionesSugeridas; }
    public void setRepeticionesSugeridas(int repeticionesSugeridas) { this.repeticionesSugeridas = repeticionesSugeridas; }

    public String getExplicacion() { return explicacion; }
    public void setExplicacion(String explicacion) { this.explicacion = explicacion; }

    public int getTipo() { return tipo; }
    public void setTipo(int tipo) { this.tipo = tipo; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}