package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

public class Sesion {
    public String id;
    public String rutinaId;
    public String rutinaNombre;
    public String fechaHoraInicio;
    public String fechaHoraFin;
    public List<EjercicioSesion> ejercicios = new ArrayList<>();
    public double volumenPlaneado;
    public double volumenReal;
    public int porcentajeCumplimiento;

    public Sesion(String id, String rutinaId, String rutinaNombre, String fechaHoraInicio) {
        this.id = id;
        this.rutinaId = rutinaId;
        this.rutinaNombre = rutinaNombre;
        this.fechaHoraInicio = fechaHoraInicio;
    }
}
