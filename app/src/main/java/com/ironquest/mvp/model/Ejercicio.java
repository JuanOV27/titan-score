package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Un ejercicio del catálogo. Los IDs {@code ex1..ex20} son los que se siembran por defecto. */
public class Ejercicio extends EntidadIdentificable {

    /**
     * Vocabulario fijo de músculos específicos, usado tanto por los chips de filtro del
     * selector como por el script de curación del catálogo (para detectar typos: un valor de
     * {@link #musculoObjetivo} que no esté aquí queda invisible bajo cualquier chip).
     */
    public static final String[] MUSCULOS_OBJETIVO = {
            "Pectorales", "Dorsales", "Trapecios", "Espalda alta", "Zona lumbar",
            "Cuádriceps", "Isquiotibiales", "Glúteos", "Aductores/Abductores", "Pantorrillas",
            "Hombros", "Bíceps", "Tríceps", "Antebrazos", "Abdomen"
    };

    private String nombre;
    private String grupoMuscular;
    private boolean personalizado;
    private String equipo;
    private String musculoObjetivo;
    private List<String> musculosSecundarios = new ArrayList<>();
    private List<String> instrucciones = new ArrayList<>();
    private String imgAsset;
    private String gifAsset;
    /** Override del incremento del usuario. {@code 0} = usar el del {@link Usuario}. */
    private double incrementoPeso;

    /** Constructor sin argumentos para Gson. Privado: nadie más debe crear un Ejercicio vacío. */
    private Ejercicio() {
    }

    public Ejercicio(String id, String nombre, String grupoMuscular) {
        super(id);
        this.nombre = nombre;
        this.grupoMuscular = grupoMuscular;
    }

    /** Si tiene GIF de técnica — señal única que usan el picker y "ver técnica" para degradar. */
    public boolean tieneFichaTecnica() {
        return gifAsset != null && !gifAsset.isEmpty();
    }

    /**
     * Vocabulario cerrado de grupos musculares para ejercicios personalizados: "Personalizado"
     * como valor por defecto más los grupos que ya usan los ejercicios curados (los no
     * personalizados), ordenados alfabéticamente. Fuerza selección en un Spinner en vez de
     * texto libre, para no reproducir variantes duplicadas como "Brazo"/"brazo"/"brazos".
     */
    public static List<String> gruposMusculares(List<Ejercicio> ejercicios) {
        TreeSet<String> grupos = new TreeSet<>();
        for (Ejercicio ejercicio : ejercicios) {
            if (!ejercicio.isPersonalizado() && ejercicio.getGrupoMuscular() != null) {
                String grupo = ejercicio.getGrupoMuscular().trim();
                if (!grupo.isEmpty() && !"Personalizado".equalsIgnoreCase(grupo)) {
                    grupos.add(grupo);
                }
            }
        }
        List<String> opciones = new ArrayList<>();
        opciones.add("Personalizado");
        opciones.addAll(grupos);
        return opciones;
    }

    /** Índice de {@code actual} dentro de {@code opciones}; 0 ("Personalizado") si no aparece. */
    public static int indiceGrupoMuscular(List<String> opciones, String actual) {
        if (actual != null) {
            String limpio = actual.trim();
            for (int i = 0; i < opciones.size(); i++) {
                if (opciones.get(i).equals(limpio)) {
                    return i;
                }
            }
        }
        return 0;
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

    public String getEquipo() {
        return equipo;
    }

    public void setEquipo(String equipo) {
        this.equipo = equipo;
    }

    public String getMusculoObjetivo() {
        return musculoObjetivo;
    }

    public void setMusculoObjetivo(String musculoObjetivo) {
        this.musculoObjetivo = musculoObjetivo;
    }

    public List<String> getMusculosSecundarios() {
        return musculosSecundarios;
    }

    public void setMusculosSecundarios(List<String> musculosSecundarios) {
        this.musculosSecundarios = musculosSecundarios;
    }

    public List<String> getInstrucciones() {
        return instrucciones;
    }

    public void setInstrucciones(List<String> instrucciones) {
        this.instrucciones = instrucciones;
    }

    public String getImgAsset() {
        return imgAsset;
    }

    public void setImgAsset(String imgAsset) {
        this.imgAsset = imgAsset;
    }

    public String getGifAsset() {
        return gifAsset;
    }

    public void setGifAsset(String gifAsset) {
        this.gifAsset = gifAsset;
    }

    public double getIncrementoPeso() {
        return incrementoPeso;
    }

    public void setIncrementoPeso(double incrementoPeso) {
        this.incrementoPeso = incrementoPeso;
    }

    @Override
    public String toString() {
        return nombre != null ? nombre : super.toString();
    }
}
