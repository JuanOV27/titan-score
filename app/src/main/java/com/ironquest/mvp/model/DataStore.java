package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Raíz del árbol de datos: es exactamente lo que se serializa a {@code datos.json}.
 * No hereda de {@link EntidadIdentificable} porque no es una entidad, es el contenedor.
 */
public class DataStore {

    private List<Ejercicio> ejercicios = new ArrayList<>();
    private List<Rutina> rutinas = new ArrayList<>();
    private List<Sesion> sesiones = new ArrayList<>();
    private Sesion sesionEnProgreso;
    private Usuario usuario;
    private List<RegistroFisico> historialFisico = new ArrayList<>();

    public DataStore() {
    }

    /** La rutina con ese id, o {@code null} si ya no existe. */
    public Rutina buscarRutina(String id) {
        if (id == null) {
            return null;
        }
        for (Rutina rutina : rutinas) {
            if (id.equals(rutina.getId())) {
                return rutina;
            }
        }
        return null;
    }

    /** El ejercicio del catálogo con ese id, o {@code null} si ya no existe. */
    public Ejercicio buscarEjercicio(String id) {
        if (id == null) {
            return null;
        }
        for (Ejercicio ejercicio : ejercicios) {
            if (id.equals(ejercicio.getId())) {
                return ejercicio;
            }
        }
        return null;
    }

    /**
     * Deja las colecciones en lista vacía si vinieron como {@code null} en el JSON.
     * Los inicializadores de campo ya cubren el caso de clave ausente, pero no el de una
     * clave presente con valor {@code null} explícito.
     */
    public void normalizarColecciones() {
        if (ejercicios == null) {
            ejercicios = new ArrayList<>();
        }
        if (rutinas == null) {
            rutinas = new ArrayList<>();
        }
        if (sesiones == null) {
            sesiones = new ArrayList<>();
        }
        if (historialFisico == null) {
            historialFisico = new ArrayList<>();
        }
    }

    /** Listas vivas: el resto de la app agrega y quita elementos directamente. */
    public List<Ejercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<Ejercicio> ejercicios) {
        this.ejercicios = ejercicios;
    }

    public List<Rutina> getRutinas() {
        return rutinas;
    }

    public void setRutinas(List<Rutina> rutinas) {
        this.rutinas = rutinas;
    }

    public List<Sesion> getSesiones() {
        return sesiones;
    }

    public void setSesiones(List<Sesion> sesiones) {
        this.sesiones = sesiones;
    }

    public Sesion getSesionEnProgreso() {
        return sesionEnProgreso;
    }

    public void setSesionEnProgreso(Sesion sesionEnProgreso) {
        this.sesionEnProgreso = sesionEnProgreso;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<RegistroFisico> getHistorialFisico() {
        return historialFisico;
    }

    public void setHistorialFisico(List<RegistroFisico> historialFisico) {
        this.historialFisico = historialFisico;
    }
}
