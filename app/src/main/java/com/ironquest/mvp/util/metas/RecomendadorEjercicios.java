package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recomendador híbrido: tabla curada de ejercicios por tipo de meta + filtro dinámico por
 * lo que el usuario ya tiene en sus rutinas + parámetros de entrenamiento sugeridos.
 *
 * <p>Los 60 nombres de la tabla CURADOS fueron verificados contra app/src/main/assets/
 * catalogo.json — todos existen literalmente.
 */
public final class RecomendadorEjercicios {

    private static final Map<String, List<String>> CURADOS = new LinkedHashMap<>();
    static {
        CURADOS.put("aumentar_brazoCm", Arrays.asList(
                "Curl de bíceps con barra recta",
                "Curl predicador con barra",
                "Curl martillo en polea con cuerda",
                "Extensión de tríceps acostado agarre cerrado con barra",
                "Patada de tríceps en polea",
                "Fondos de codo"));
        CURADOS.put("aumentar_piernaCm", Arrays.asList(
                "Sentadilla completa con barra",
                "Prensa de piernas a 45°",
                "Extensión de cuádriceps en máquina",
                "Curl femoral acostado en máquina",
                "Peso muerto rumano con barra",
                "Zancada con barra"));
        CURADOS.put("aumentar_pechoCm", Arrays.asList(
                "Press de banca con barra",
                "Press inclinado con barra",
                "Press banca inclinado con mancuerna",
                "Aperturas con mancuernas en banco plano",
                "Fondos de pecho"));
        CURADOS.put("reducir_cinturaCm", Arrays.asList(
                "Plancha lateral inclinada",
                "Elevación de piernas colgado",
                "Abdominal con press y barra",
                "Crunch bicicleta con banda",
                "Sentadilla completa con barra",
                "Peso muerto con barra",
                "Fondos de pecho"));
        CURADOS.put("reducir_caderaCm", Arrays.asList(
                "Sentadilla completa con barra",
                "Zancada con barra",
                "Peso muerto rumano con barra",
                "Puente de glúteos con barra",
                "Elevación de cadera acostado con barra"));
        CURADOS.put("peso_bajar", Arrays.asList(
                "Sentadilla completa con barra",
                "Peso muerto con barra",
                "Press de banca con barra",
                "Remo con barra inclinado",
                "Jalón al pecho en polea",
                "Fondos de pecho"));
        CURADOS.put("peso_subir", Arrays.asList(
                "Sentadilla completa con barra",
                "Peso muerto con barra",
                "Press de banca con barra",
                "Press militar sentado con barra",
                "Remo con barra inclinado",
                "Jalón al pecho en polea"));
        CURADOS.put("pr_press_banca", Arrays.asList(
                "Press de banca agarre cerrado con barra",
                "Extensión de tríceps acostado agarre cerrado con barra",
                "Press JM con barra",
                "Press inclinado con barra",
                "Aperturas con mancuernas en banco plano"));
        CURADOS.put("pr_sentadilla", Arrays.asList(
                "Sentadilla frontal con barra",
                "Prensa de piernas a 45°",
                "Extensión de cuádriceps en máquina",
                "Zancada con barra",
                "Peso muerto rumano con barra"));
        CURADOS.put("pr_peso_muerto", Arrays.asList(
                "Peso muerto rumano con barra",
                "Buenos días con barra",
                "Puente de glúteos con barra",
                "Remo con barra inclinado",
                "Hiperextensión lumbar"));
        CURADOS.put("pr_dominadas", Arrays.asList(
                "Remo con barra inclinado",
                "Jalón al pecho en polea",
                "Curl de bíceps con barra recta",
                "Pullover con barra"));
    }

    /** Rangos MEV–MAV (mínimo efectivo, máximo adaptativo) de series por semana por grupo. */
    private static final Map<String, int[]> RANGO_VOLUMEN = new HashMap<>();
    static {
        RANGO_VOLUMEN.put("Pectorales",           new int[]{10, 16});
        RANGO_VOLUMEN.put("Dorsales",             new int[]{10, 16});
        RANGO_VOLUMEN.put("Cuádriceps",           new int[]{10, 16});
        RANGO_VOLUMEN.put("Espalda alta",         new int[]{8, 14});
        RANGO_VOLUMEN.put("Hombros",              new int[]{8, 14});
        RANGO_VOLUMEN.put("Bíceps",               new int[]{8, 14});
        RANGO_VOLUMEN.put("Tríceps",              new int[]{8, 14});
        RANGO_VOLUMEN.put("Pantorrillas",         new int[]{8, 14});
        RANGO_VOLUMEN.put("Abdomen",              new int[]{8, 14});
        RANGO_VOLUMEN.put("Zona lumbar",          new int[]{6, 12});
        RANGO_VOLUMEN.put("Glúteos",              new int[]{6, 12});
        RANGO_VOLUMEN.put("Isquiotibiales",       new int[]{6, 12});
        RANGO_VOLUMEN.put("Trapecios",            new int[]{6, 12});
        RANGO_VOLUMEN.put("Aductores/Abductores", new int[]{6, 10});
        RANGO_VOLUMEN.put("Antebrazos",           new int[]{4, 10});
    }

    private RecomendadorEjercicios() {}

    // ---------- API pública ----------

    public static PlanRecomendado recomendar(Meta meta, DataStore ds) {
        String clave = construirClave(meta, ds);
        String grupo = grupoDeMeta(meta, ds);

        List<String> nombresCurados = clave != null
                ? CURADOS.getOrDefault(clave, Collections.emptyList())
                : Collections.emptyList();
        List<Ejercicio> candidatos = resolverNombres(nombresCurados, ds.getEjercicios());
        if (candidatos.isEmpty() && meta.getTipo() == Meta.TIPO_PR_EJERCICIO && grupo != null) {
            candidatos = fallbackPorMusculo(grupo, ds.getEjercicios());
        }

        Map<String, Integer> conteos = contarEnRutinas(ds.getRutinas());

        List<Recomendacion> resultado = new ArrayList<>();
        for (Ejercicio e : candidatos) {
            int veces = conteos.getOrDefault(e.getId(), 0);
            String etiqueta = veces == 0 ? "Falta en tus rutinas"
                    : veces == 1 ? "Ya lo haces en 1 rutina"
                    : "Ya lo haces en " + veces + " rutinas";
            int prioridad = veces == 0 ? 0 : (veces == 1 ? 1 : 2);
            resultado.add(new Recomendacion(e, etiqueta, prioridad));
        }
        resultado.sort(Comparator.comparingInt(Recomendacion::getPrioridad));

        int diasSemana = (ds.getUsuario() != null && ds.getUsuario().getDiasEntrenoSemana() > 0)
                ? ds.getUsuario().getDiasEntrenoSemana() : 3;
        Map<String, Ejercicio> catalogo = indexarCatalogo(ds.getEjercicios());
        int volActual = grupo != null
                ? seriesSemanalesDeGrupo(grupo, ds.getRutinas(), catalogo, diasSemana) : 0;
        int[] rango = grupo != null
                ? RANGO_VOLUMEN.getOrDefault(grupo, new int[]{8, 14})
                : new int[]{0, 0};
        String textoVolumen = grupo != null
                ? "Para " + grupo + ": " + rango[0] + "-" + rango[1]
                    + " series/semana. Actualmente tienes " + volActual + "."
                : "Sin cálculo de volumen para este tipo de meta.";

        ParamsProgresion params = paramsPara(meta);

        String textoCobertura = null;
        if (!resultado.isEmpty() && resultado.stream().allMatch(r -> r.getPrioridad() > 0)) {
            textoCobertura = "Ya tienes buena cobertura de este grupo en tus rutinas. "
                    + "Considera aumentar el volumen (más series o más peso) antes de agregar más ejercicios.";
        }

        return new PlanRecomendado(textoVolumen, volActual, rango[0], rango[1],
                params.series, params.repsMin, params.repsMax, params.esquemaProgresion,
                resultado, textoCobertura);
    }

    public static ParamsProgresion paramsPara(Meta meta) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
                return new ParamsProgresion(3, 8, 12, RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION);
            case Meta.TIPO_REDUCIR_MEDIDA:
                return new ParamsProgresion(3, 15, 20, RutinaEjercicio.ESQUEMA_LINEAL);
            case Meta.TIPO_PESO_CORPORAL:
                boolean subir = meta.getValorObjetivo() > meta.getValorInicial();
                return subir
                        ? new ParamsProgresion(3, 8, 10, RutinaEjercicio.ESQUEMA_AUTOMATICO)
                        : new ParamsProgresion(3, 15, 20, RutinaEjercicio.ESQUEMA_LINEAL);
            case Meta.TIPO_PR_EJERCICIO:
                return new ParamsProgresion(4, 5, 6, RutinaEjercicio.ESQUEMA_GREYSKULL);
            default:
                return new ParamsProgresion(3, 8, 12, RutinaEjercicio.ESQUEMA_AUTOMATICO);
        }
    }

    public static double pesoInicialSugerido(String ejercicioId, List<Sesion> sesiones) {
        if (ejercicioId == null || sesiones == null) return 0.0;
        for (int i = sesiones.size() - 1; i >= 0; i--) {
            Sesion s = sesiones.get(i);
            if (!s.estaFinalizada()) continue;
            EjercicioSesion es = s.buscarEjercicio(ejercicioId);
            if (es == null) continue;
            SerieSesion ultima = es.getUltimaSerieCompletada();
            if (ultima != null) return ultima.getPeso();
        }
        return 0.0;
    }

    // ---------- Helpers ----------

    private static String construirClave(Meta meta, DataStore ds) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
                return "aumentar_" + meta.getMedidaTipo();
            case Meta.TIPO_REDUCIR_MEDIDA:
                return "reducir_" + meta.getMedidaTipo();
            case Meta.TIPO_PESO_CORPORAL:
                return meta.getValorObjetivo() > meta.getValorInicial()
                        ? "peso_subir" : "peso_bajar";
            case Meta.TIPO_PR_EJERCICIO:
                Ejercicio ej = ds.buscarEjercicio(meta.getEjercicioId());
                return ej != null ? construirClavePr(ej) : null;
            default: return null;
        }
    }

    private static String construirClavePr(Ejercicio ej) {
        String n = normalizar(ej.getNombre());
        if (n.contains("press") && n.contains("banca")) return "pr_press_banca";
        if (n.contains("peso muerto")) return "pr_peso_muerto";
        if (n.contains("sentadilla")) return "pr_sentadilla";
        if (n.contains("dominada"))   return "pr_dominadas";
        return null;
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        String sinAcentos = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toLowerCase();
    }

    /** Grupo muscular objetivo de la meta (para calcular volumen). */
    private static String grupoDeMeta(Meta meta, DataStore ds) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
                if ("brazoCm".equals(meta.getMedidaTipo())) return "Bíceps"; // aproximación
                if ("piernaCm".equals(meta.getMedidaTipo())) return "Cuádriceps";
                if ("pechoCm".equals(meta.getMedidaTipo())) return "Pectorales";
                return null;
            case Meta.TIPO_REDUCIR_MEDIDA:
                return "Abdomen";
            case Meta.TIPO_PR_EJERCICIO:
                Ejercicio ej = ds.buscarEjercicio(meta.getEjercicioId());
                return ej != null ? ej.getMusculoObjetivo() : null;
            case Meta.TIPO_PESO_CORPORAL:
            default:
                return null;   // sin grupo específico
        }
    }

    private static List<Ejercicio> resolverNombres(List<String> nombres, List<Ejercicio> catalogo) {
        List<Ejercicio> resultado = new ArrayList<>();
        for (String nombre : nombres) {
            for (Ejercicio e : catalogo) {
                if (e.getNombre() != null && e.getNombre().equalsIgnoreCase(nombre)) {
                    resultado.add(e);
                    break;
                }
            }
        }
        return resultado;
    }

    private static List<Ejercicio> fallbackPorMusculo(String grupo, List<Ejercicio> catalogo) {
        List<Ejercicio> lista = new ArrayList<>();
        for (Ejercicio e : catalogo) {
            if (grupo.equals(e.getMusculoObjetivo()) && e.tieneFichaTecnica()) {
                lista.add(e);
                if (lista.size() >= 5) break;
            }
        }
        return lista;
    }

    private static Map<String, Integer> contarEnRutinas(List<Rutina> rutinas) {
        Map<String, Integer> conteos = new HashMap<>();
        for (Rutina r : rutinas) {
            for (RutinaEjercicio re : r.getEjercicios()) {
                conteos.merge(re.getEjercicioId(), 1, Integer::sum);
            }
        }
        return conteos;
    }

    private static Map<String, Ejercicio> indexarCatalogo(List<Ejercicio> catalogo) {
        Map<String, Ejercicio> m = new HashMap<>();
        for (Ejercicio e : catalogo) m.put(e.getId(), e);
        return m;
    }

    /**
     * Estimación de series semanales para un grupo. Asume distribución uniforme entre rutinas
     * (cada rutina se hace {@code diasEntrenoSemana / N_rutinas} veces por semana).
     */
    public static int seriesSemanalesDeGrupo(String grupo, List<Rutina> rutinas,
                                              Map<String, Ejercicio> catalogo,
                                              int diasEntrenoSemana) {
        int seriesPorPasada = 0;
        for (Rutina r : rutinas) {
            for (RutinaEjercicio re : r.getEjercicios()) {
                Ejercicio ej = catalogo.get(re.getEjercicioId());
                if (ej != null && grupo.equals(ej.getMusculoObjetivo())) {
                    seriesPorPasada += re.getSeries();
                }
            }
        }
        int nRutinas = Math.max(1, rutinas.size());
        int pasadasSemana = Math.max(1, diasEntrenoSemana);
        return (int) Math.round((double) seriesPorPasada * pasadasSemana / nRutinas);
    }
}
