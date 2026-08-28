package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

public class EjercicioSesion {
    public String ejercicioId;
    public List<SerieSesion> series = new ArrayList<>();

    public EjercicioSesion(String ejercicioId) {
        this.ejercicioId = ejercicioId;
    }
}
