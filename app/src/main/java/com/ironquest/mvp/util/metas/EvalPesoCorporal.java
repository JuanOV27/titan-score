package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;

import java.util.List;

/** Meta PESO_CORPORAL. Dirección se detecta por (objetivo - inicial). */
final class EvalPesoCorporal extends EvaluadorMeta {

    @Override
    public double calcularProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        double inicial = meta.getValorInicial();
        double objetivo = meta.getValorObjetivo();
        if (actual <= 0) return 0.0;
        double denom = objetivo - inicial;
        if (denom == 0) return actual == objetivo ? 1.0 : 0.0;
        return clamp01((actual - inicial) / denom);
    }

    @Override
    public boolean cumplida(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        if (actual <= 0) return false;
        boolean subir = meta.getValorObjetivo() > meta.getValorInicial();
        return subir ? actual >= meta.getValorObjetivo() : actual <= meta.getValorObjetivo();
    }

    @Override
    public String textoProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return formatearNumero(actual) + " → " + formatearNumero(meta.getValorObjetivo())
                + " kg (" + formatearPorcentaje(calcularProgreso(meta, ds)) + ")";
    }

    @Override
    protected double valorActual(Meta meta, DataStore ds) {
        RegistroFisico r = ultimoConPeso(ds);
        return r != null ? r.getPesoKg() : 0.0;
    }

    @Override
    public String ultimoRegistroRelevante(Meta meta, DataStore ds) {
        RegistroFisico r = ultimoConPeso(ds);
        return r != null ? r.getFecha() : null;
    }

    private static RegistroFisico ultimoConPeso(DataStore ds) {
        List<RegistroFisico> lista = ds.getHistorialFisico();
        for (int i = lista.size() - 1; i >= 0; i--) {
            if (lista.get(i).getPesoKg() > 0) return lista.get(i);
        }
        return null;
    }
}
