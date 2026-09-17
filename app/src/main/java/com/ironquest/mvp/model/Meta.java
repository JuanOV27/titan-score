package com.ironquest.mvp.model;

/**
 * Meta de entrenamiento o composición corporal. Cuatro tipos: aumentar medida muscular,
 * reducir cintura/cadera, alcanzar peso corporal, PR en ejercicio. Se evalúa automáticamente
 * cada vez que el usuario guarda un RegistroFisico o finaliza una Sesion.
 */
public class Meta extends EntidadIdentificable {

    public static final int TIPO_AUMENTAR_MEDIDA = 0;
    public static final int TIPO_REDUCIR_MEDIDA  = 1;
    public static final int TIPO_PESO_CORPORAL   = 2;
    public static final int TIPO_PR_EJERCICIO    = 3;

    public static final int ESTADO_ACTIVA     = 0;
    public static final int ESTADO_CUMPLIDA   = 1;
    public static final int ESTADO_DESCARTADA = 2;

    private int tipo;
    private String titulo;
    private int estado;

    /** Nombre del campo en RegistroFisico ("brazoCm", "cinturaCm", ...). Solo tipos de medida. */
    private String medidaTipo;

    /** Solo para PR: referencia al Ejercicio principal. */
    private String ejercicioId;
    private double pesoObjetivoKg;
    private int repsObjetivo;

    /** Fotografía del valor al momento de crear la meta. */
    private double valorInicial;

    /** Objetivo numérico. Para tipos de medida y peso; ignorado para PR (que usa peso+reps). */
    private double valorObjetivo;

    private String fechaCreacion;
    private String fechaObjetivo;
    private String fechaCumplida;
    private String fechaDescartada;

    /** True si el usuario aceptó una advertencia del validador al crear. */
    private boolean advertenciaAceptada;

    /** True una vez que la notificación push de "≥80% de progreso" ya se disparó. No se resetea. */
    private boolean notificadaCerca;

    /** Constructor sin argumentos para Gson. */
    private Meta() {
    }

    public Meta(String id, int tipo, String titulo, String fechaCreacion) {
        super(id);
        this.tipo = tipo;
        this.titulo = titulo;
        this.estado = ESTADO_ACTIVA;
        this.fechaCreacion = fechaCreacion;
    }

    public int getTipo() { return tipo; }
    public void setTipo(int tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public int getEstado() { return estado; }
    public void setEstado(int estado) { this.estado = estado; }

    public String getMedidaTipo() { return medidaTipo; }
    public void setMedidaTipo(String medidaTipo) { this.medidaTipo = medidaTipo; }

    public String getEjercicioId() { return ejercicioId; }
    public void setEjercicioId(String ejercicioId) { this.ejercicioId = ejercicioId; }

    public double getPesoObjetivoKg() { return pesoObjetivoKg; }
    public void setPesoObjetivoKg(double pesoObjetivoKg) { this.pesoObjetivoKg = pesoObjetivoKg; }

    public int getRepsObjetivo() { return repsObjetivo; }
    public void setRepsObjetivo(int repsObjetivo) { this.repsObjetivo = repsObjetivo; }

    public double getValorInicial() { return valorInicial; }
    public void setValorInicial(double valorInicial) { this.valorInicial = valorInicial; }

    public double getValorObjetivo() { return valorObjetivo; }
    public void setValorObjetivo(double valorObjetivo) { this.valorObjetivo = valorObjetivo; }

    public String getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getFechaObjetivo() { return fechaObjetivo; }
    public void setFechaObjetivo(String fechaObjetivo) { this.fechaObjetivo = fechaObjetivo; }

    public String getFechaCumplida() { return fechaCumplida; }
    public void setFechaCumplida(String fechaCumplida) { this.fechaCumplida = fechaCumplida; }

    public String getFechaDescartada() { return fechaDescartada; }
    public void setFechaDescartada(String fechaDescartada) { this.fechaDescartada = fechaDescartada; }

    public boolean isAdvertenciaAceptada() { return advertenciaAceptada; }
    public void setAdvertenciaAceptada(boolean advertenciaAceptada) { this.advertenciaAceptada = advertenciaAceptada; }

    public boolean isNotificadaCerca() { return notificadaCerca; }
    public void setNotificadaCerca(boolean notificadaCerca) { this.notificadaCerca = notificadaCerca; }
}
