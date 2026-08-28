package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

public class Rutina {
    public String id;
    public String nombre;
    public List<RutinaEjercicio> ejercicios = new ArrayList<>();

    public Rutina(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }
}
