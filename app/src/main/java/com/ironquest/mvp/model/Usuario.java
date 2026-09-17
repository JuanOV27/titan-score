package com.ironquest.mvp.model;

/** La cuenta, vinculada a Firebase Authentication mediante {@code firebaseUid}. */
public class Usuario extends EntidadIdentificable {

    private String username;
    private String email;
    private int edad;
    private String fechaRegistro;
    private String firebaseUid;
    private boolean aceptoTerminos;
    private String fechaAceptacionTerminos;
    /** Incremento por defecto que aplica el motor de progresión. Valor default: 2.5 kg. */
    private double incrementoPeso = 2.5;

    /** Toggle global opcional. Activa chips de fatiga por serie y detección dinámica. */
    private boolean seguimientoFatiga;

    /** Cuántos días a la semana entrena. Alimenta al recomendador para calcular
     *  volumen semanal esperado. Editable en Ajustes. Default 3. */
    private int diasEntrenoSemana = 3;

    /** Público: el registro construye el usuario vacío y lo va llenando campo a campo. */
    public Usuario() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public boolean isAceptoTerminos() {
        return aceptoTerminos;
    }

    public void setAceptoTerminos(boolean aceptoTerminos) {
        this.aceptoTerminos = aceptoTerminos;
    }

    public String getFechaAceptacionTerminos() {
        return fechaAceptacionTerminos;
    }

    public void setFechaAceptacionTerminos(String fechaAceptacionTerminos) {
        this.fechaAceptacionTerminos = fechaAceptacionTerminos;
    }

    public double getIncrementoPeso() {
        return incrementoPeso;
    }

    public void setIncrementoPeso(double incrementoPeso) {
        this.incrementoPeso = incrementoPeso;
    }

    public boolean isSeguimientoFatiga() {
        return seguimientoFatiga;
    }

    public void setSeguimientoFatiga(boolean seguimientoFatiga) {
        this.seguimientoFatiga = seguimientoFatiga;
    }

    public int getDiasEntrenoSemana() {
        return diasEntrenoSemana;
    }

    public void setDiasEntrenoSemana(int diasEntrenoSemana) {
        this.diasEntrenoSemana = diasEntrenoSemana;
    }

    @Override
    public String toString() {
        return username != null ? username : super.toString();
    }
}
