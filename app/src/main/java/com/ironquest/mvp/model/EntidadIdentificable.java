package com.ironquest.mvp.model;

/**
 * Raíz de la jerarquía de modelos: todo lo que se guarda en {@code datos.json} con identidad
 * propia hereda de aquí y comparte el campo {@code id} y su acceso.
 *
 * <p>La heredan {@link Ejercicio}, {@link Rutina}, {@link Sesion}, {@link RegistroFisico} y
 * {@link Usuario}. Quedan deliberadamente fuera {@link RutinaEjercicio}, {@link SerieSesion},
 * {@link EjercicioSesion} y {@link DataStore}: son objetos de valor subordinados a la entidad
 * que los contiene y no tienen identidad propia, así que forzarles un {@code id} sería inventar
 * estado solo para lucir la jerarquía.
 *
 * <p>Gson recorre la cadena de superclases al serializar, de modo que {@code id} se sigue
 * escribiendo y leyendo igual que cuando estaba declarado en cada subclase.
 */
public abstract class EntidadIdentificable {

    private String id;

    /** Para Gson y para las subclases que se construyen vacías y se llenan por pasos. */
    protected EntidadIdentificable() {
    }

    protected EntidadIdentificable(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** Dos entidades son la misma si comparten tipo e identificador. */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (otro == null || getClass() != otro.getClass()) {
            return false;
        }
        EntidadIdentificable otraEntidad = (EntidadIdentificable) otro;
        return id != null && id.equals(otraEntidad.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + "}";
    }
}
