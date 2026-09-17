package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;

import java.util.List;

/** Meta REDUCIR_MEDIDA (cintura/cadera). Progreso invertido: bajar es avanzar. */
final class EvalReducirMedida extends EvaluadorMeta {

    @Override
    public double calcularProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        double inicial = meta.getValorInicial();
        double objetivo = meta.getValorObjetivo();
        if (inicial <= objetivo || actual <= 0) return actual > 0 && actual <= objetivo ? 1.0 : 0.0;
        return clamp01((inicial - actual) / (inicial - objetivo));
    }

    @Override
    public boolean cumplida(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return actual > 0 && actual <= meta.getValorObjetivo();
    }

    @Override
    public String textoProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return formatearNumero(actual) + " → " + formatearNumero(meta.getValorObjetivo())
                + " cm (" + formatearPorcentaje(calcularProgreso(meta, ds)) + ")";
    }

    @Override
    protected double valorActual(Meta meta, DataStore ds) {
        RegistroFisico ultimo = ultimoRegistroConMedida(ds, meta.getMedidaTipo());
        return ultimo != null ? EvalMedidaMuscular.leerMedida(ultimo, meta.getMedidaTipo()) : 0.0;
    }

    @Override
    public String ultimoRegistroRelevante(Meta meta, DataStore ds) {
        RegistroFisico r = ultimoRegistroConMedida(ds, meta.getMedidaTipo());
        return r != null ? r.getFecha() : null;
    }

    private static RegistroFisico ultimoRegistroConMedida(DataStore ds, String medidaTipo) {
        List<RegistroFisico> lista = ds.getHistorialFisico();
        for (int i = lista.size() - 1; i >= 0; i--) {
            RegistroFisico r = lista.get(i);
            if (EvalMedidaMuscular.leerMedida(r, medidaTipo) > 0) return r;
        }
        return null;
    }
}
