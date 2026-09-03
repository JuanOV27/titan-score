package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

/** Un entrenamiento: lo que se planeó al empezar y lo que realmente se registró. */
public class Sesion extends EntidadIdentificable {

    private String rutinaId;
    private String rutinaNombre;
    private String fechaHoraInicio;
    private String fechaHoraFin;
    private List<EjercicioSesion> ejercicios = new ArrayList<>();
    private double volumenPlaneado;
    private double volumenReal;
    private int porcentajeCumplimiento;

    /** Constructor sin argumentos para Gson. Privado: nadie más debe crear una Sesión vacía. */
    private Sesion() {
    }

    public Sesion(String id, String rutinaId, String rutinaNombre, String fechaHoraInicio) {
        super(id);
        this.rutinaId = rutinaId;
        this.rutinaNombre = rutinaNombre;
        this.fechaHoraInicio = fechaHoraInicio;
    }

    /**
     * Cierra la sesión: fija la hora de fin, suma el volumen realmente levantado y calcula el
     * porcentaje de cumplimiento contra lo planeado. Sin plan previo, cualquier trabajo
     * registrado cuenta como 100 %.
     */
    public void finalizar(String fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;

        double total = 0;
        for (EjercicioSesion ejercicioSesion : ejercicios) {
            total += ejercicioSesion.calcularVolumen();
        }
        this.volumenReal = total;

        int porcentaje;
        if (volumenPlaneado > 0) {
            porcentaje = (int) Math.round(total / volumenPlaneado * 100);
        } else {
            porcentaje = total > 0 ? 100 : 0;
        }
        this.porcentajeCumplimiento = Math.max(0, porcentaje);
    }

    public boolean estaFinalizada() {
        return fechaHoraFin != null;
    }

    public void agregarEjercicio(EjercicioSesion ejercicioSesion) {
        ejercicios.add(ejercicioSesion);
    }

    /** El bloque de este ejercicio dentro de la sesión, o {@code null} si no se hizo. */
    public EjercicioSesion buscarEjercicio(String ejercicioId) {
        if (ejercicioId == null) {
            return null;
        }
        for (EjercicioSesion ejercicioSesion : ejercicios) {
            if (ejercicioId.equals(ejercicioSesion.getEjercicioId())) {
                return ejercicioSesion;
            }
        }
        return null;
    }

    public String getRutinaId() {
        return rutinaId;
    }

    public void setRutinaId(String rutinaId) {
        this.rutinaId = rutinaId;
    }

    public String getRutinaNombre() {
        return rutinaNombre;
    }

    public void setRutinaNombre(String rutinaNombre) {
        this.rutinaNombre = rutinaNombre;
    }

    public String getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(String fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public String getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(String fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    /** Lista viva: {@code ActiveSessionActivity} agrega y quita bloques directamente. */
    public List<EjercicioSesion> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<EjercicioSesion> ejercicios) {
        this.ejercicios = ejercicios;
    }

    public double getVolumenPlaneado() {
        return volumenPlaneado;
    }

    public void setVolumenPlaneado(double volumenPlaneado) {
        this.volumenPlaneado = volumenPlaneado;
    }

    public double getVolumenReal() {
        return volumenReal;
    }

    public void setVolumenReal(double volumenReal) {
        this.volumenReal = volumenReal;
    }

    public int getPorcentajeCumplimiento() {
        return porcentajeCumplimiento;
    }

    public void setPorcentajeCumplimiento(int porcentajeCumplimiento) {
        this.porcentajeCumplimiento = porcentajeCumplimiento;
    }
}
