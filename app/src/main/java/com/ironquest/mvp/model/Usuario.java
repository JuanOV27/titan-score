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

    @Override
    public String toString() {
        return username != null ? username : super.toString();
    }
}
