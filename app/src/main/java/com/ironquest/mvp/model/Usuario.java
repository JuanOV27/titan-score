package com.ironquest.mvp.model;

/** La cuenta local. Solo hay una por instalación; la contraseña se guarda hasheada. */
public class Usuario extends EntidadIdentificable {

    private String username;
    private String email;
    private String passwordHash;
    private int edad;
    private boolean sesionActiva;
    private String fechaRegistro;

    /** Público: el registro construye el usuario vacío y lo va llenando campo a campo. */
    public Usuario() {
    }

    /**
     * Compara contra el hash almacenado. Recibe el hash ya calculado, no la contraseña en
     * claro: el hasheo vive en {@code PasswordUtil} y el modelo no debe depender de él.
     */
    public boolean coincideHash(String hash) {
        return passwordHash != null && passwordHash.equals(hash);
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public boolean isSesionActiva() {
        return sesionActiva;
    }

    public void setSesionActiva(boolean sesionActiva) {
        this.sesionActiva = sesionActiva;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @Override
    public String toString() {
        return username != null ? username : super.toString();
    }
}
