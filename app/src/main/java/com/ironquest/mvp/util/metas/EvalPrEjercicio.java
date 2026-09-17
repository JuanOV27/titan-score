package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;

import java.util.List;

/**
 * Meta PR_EJERCICIO. Cumplida cuando alguna serie completada del historial iguala o supera
 * simultáneamente peso y reps objetivo. Progreso = mejor score actual / score objetivo,
 * donde score = peso × reps.
 */
final class EvalPrEjercicio extends EvaluadorMeta {

    @Override
    public double calcularProgreso(Meta meta, DataStore ds) {
        double scoreObjetivo = meta.getPesoObjetivoKg() * meta.getRepsObjetivo();
        if (scoreObjetivo <= 0) return 0.0;
        double mejorActual = mejorScore(meta, ds);
        return clamp01(mejorActual / scoreObjetivo);
    }

    @Override
    public boolean cumplida(Meta meta, DataStore ds) {
        for (Sesion s : ds.getSesiones()) {
            if (!s.estaFinalizada()) continue;
            EjercicioSesion es = s.buscarEjercicio(meta.getEjercicioId());
            if (es == null) continue;
            for (SerieSesion serie : es.getSeries()) {
                if (!serie.isCompletada()) continue;
                if (serie.getPeso() >= meta.getPesoObjetivoKg()
                        && serie.getRepeticiones() >= meta.getRepsObjetivo()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String textoProgreso(Meta meta, DataStore ds) {
        double mejor = mejorScore(meta, ds);
        double scoreObj = meta.getPesoObjetivoKg() * meta.getRepsObjetivo();
        return "Mejor actual: " + formatearNumero(mejor)
                + " · Objetivo: " + formatearNumero(scoreObj)
                + " (" + formatearPorcentaje(calcularProgreso(meta, ds)) + ")";
    }

    @Override
    protected double valorActual(Meta meta, DataStore ds) {
        return mejorScore(meta, ds);
    }

    @Override
    public String ultimoRegistroRelevante(Meta meta, DataStore ds) {
        List<Sesion> sesiones = ds.getSesiones();
        for (int i = sesiones.size() - 1; i >= 0; i--) {
            Sesion s = sesiones.get(i);
            if (!s.estaFinalizada()) continue;
            if (s.buscarEjercicio(meta.getEjercicioId()) != null) {
                return s.getFechaHoraInicio();
            }
        }
        return null;
    }

    private static double mejorScore(Meta meta, DataStore ds) {
        double mejor = 0.0;
        for (Sesion s : ds.getSesiones()) {
            if (!s.estaFinalizada()) continue;
            EjercicioSesion es = s.buscarEjercicio(meta.getEjercicioId());
            if (es == null) continue;
            for (SerieSesion serie : es.getSeries()) {
                if (!serie.isCompletada()) continue;
                double score = serie.getPeso() * serie.getRepeticiones();
                if (score > mejor) mejor = score;
            }
        }
        return mejor;
    }
}
