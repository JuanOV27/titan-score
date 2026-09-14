package com.ironquest.mvp.util;

import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cálculo de volumen ponderado y conteo de ejercicios por grupo muscular.
 * <p>Ponderación: el músculo objetivo se lleva 100% del volumen de la serie; los músculos
 * secundarios comparten un 50% adicional, dividido entre ellos. Presupuesto por serie = 150%.
 * <p>Ejercicios sin {@code musculoObjetivo} caen en {@link #OTROS}.
 */
public final class AnalisisMuscular {

    public static final int LIMITE_POR_GRUPO = 4;
    public static final String OTROS = "Otros";

    private AnalisisMuscular() {
    }

    /** Volumen ponderado de una sola sesión. */
    public static LinkedHashMap<String, Double> calcularVolumenPorGrupo(
            Sesion sesion, Map<String, Ejercicio> catalogoPorId) {
        LinkedHashMap<String, Double> resultado = new LinkedHashMap<>();
        if (sesion == null) return resultado;
        acumularSesion(sesion, catalogoPorId, resultado);
        return resultado;
    }

    /** Volumen ponderado agregado de varias sesiones. {@code desde}/{@code hasta} inclusivos (o {@code null}). */
    public static LinkedHashMap<String, Double> calcularVolumenPorGrupo(
            List<Sesion> sesiones, LocalDate desde, LocalDate hasta, Map<String, Ejercicio> catalogoPorId) {
        LinkedHashMap<String, Double> resultado = new LinkedHashMap<>();
        if (sesiones == null) return resultado;
        for (Sesion s : sesiones) {
            if (!s.estaFinalizada()) continue;
            if (!enRango(s, desde, hasta)) continue;
            acumularSesion(s, catalogoPorId, resultado);
        }
        return resultado;
    }

    /** Conteo de ejercicios por grupo muscular primario en una rutina. */
    public static LinkedHashMap<String, Integer> ejerciciosPorGrupo(
            Rutina rutina, Map<String, Ejercicio> catalogoPorId) {
        LinkedHashMap<String, Integer> resultado = new LinkedHashMap<>();
        if (rutina == null) return resultado;
        for (RutinaEjercicio re : rutina.getEjercicios()) {
            Ejercicio ej = catalogoPorId.get(re.getEjercicioId());
            String grupo = grupoDe(ej);
            resultado.merge(grupo, 1, Integer::sum);
        }
        return resultado;
    }

    /** Filtrado del anterior: solo grupos que superan {@link #LIMITE_POR_GRUPO}. */
    public static LinkedHashMap<String, Integer> gruposSobreLimite(
            Rutina rutina, Map<String, Ejercicio> catalogoPorId) {
        LinkedHashMap<String, Integer> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : ejerciciosPorGrupo(rutina, catalogoPorId).entrySet()) {
            if (e.getValue() > LIMITE_POR_GRUPO && !OTROS.equals(e.getKey())) {
                resultado.put(e.getKey(), e.getValue());
            }
        }
        return resultado;
    }

    private static void acumularSesion(Sesion sesion, Map<String, Ejercicio> catalogoPorId,
                                        LinkedHashMap<String, Double> acumulador) {
        for (EjercicioSesion es : sesion.getEjercicios()) {
            Ejercicio ej = catalogoPorId.get(es.getEjercicioId());
            if (ej == null) {
                for (SerieSesion s : es.getSeries()) {
                    if (s.isCompletada()) {
                        acumular(acumulador, OTROS, s.calcularVolumen());
                    }
                }
                continue;
            }
            String primario = ej.getMusculoObjetivo();
            List<String> secundarios = ej.getMusculosSecundarios();
            for (SerieSesion s : es.getSeries()) {
                if (!s.isCompletada()) continue;
                double vol = s.calcularVolumen();
                acumular(acumulador, primario != null && !primario.isEmpty() ? primario : OTROS, vol);
                if (secundarios != null && !secundarios.isEmpty()) {
                    double porCada = 0.5 * vol / secundarios.size();
                    for (String m : secundarios) {
                        if (m != null && !m.isEmpty()) {
                            acumular(acumulador, m, porCada);
                        }
                    }
                }
            }
        }
    }

    private static boolean enRango(Sesion s, LocalDate desde, LocalDate hasta) {
        try {
            LocalDate fecha = LocalDateTime.parse(s.getFechaHoraInicio()).toLocalDate();
            if (desde != null && fecha.isBefore(desde)) return false;
            if (hasta != null && fecha.isAfter(hasta)) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void acumular(LinkedHashMap<String, Double> mapa, String clave, double valor) {
        mapa.merge(clave, valor, Double::sum);
    }

    private static String grupoDe(Ejercicio ej) {
        if (ej == null || ej.getMusculoObjetivo() == null || ej.getMusculoObjetivo().isEmpty()) {
            return OTROS;
        }
        return ej.getMusculoObjetivo();
    }
}