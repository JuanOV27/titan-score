package com.ironquest.mvp.model;

/** Un ejercicio del catálogo. Los IDs {@code ex1..ex20} son los que se siembran por defecto. */
public class Ejercicio extends EntidadIdentificable {

    private String nombre;
    private String grupoMuscular;
    private boolean personalizado;

    /** Constructor sin argumentos para Gson. Privado: nadie más debe crear un Ejercicio vacío. */
    private Ejercicio() {
    }

    public Ejercicio(String id, String nombre, String grupoMuscular) {
        super(id);
        this.nombre = nombre;
        this.grupoMuscular = grupoMuscular;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(String grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public boolean isPersonalizado() {
        return personalizado;
    }

    public void setPersonalizado(boolean personalizado) {
        this.personalizado = personalizado;
    }

    @Override
    public String toString() {
        return nombre != null ? nombre : super.toString();
    }
}
