package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;

import java.util.List;

/** Meta AUMENTAR_MEDIDA (brazo/pierna/pecho). */
final class EvalMedidaMuscular extends EvaluadorMeta {

    @Override
    public double calcularProgreso(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        double inicial = meta.getValorInicial();
        double objetivo = meta.getValorObjetivo();
        if (objetivo <= inicial) return actual >= objetivo ? 1.0 : 0.0;
        return clamp01((actual - inicial) / (objetivo - inicial));
    }

    @Override
    public boolean cumplida(Meta meta, DataStore ds) {
        double actual = valorActual(meta, ds);
        return actual > 0 && actual >= meta.getValorObjetivo();
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
        return ultimo != null ? leerMedida(ultimo, meta.getMedidaTipo()) : 0.0;
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
            if (leerMedida(r, medidaTipo) > 0) return r;
        }
        return null;
    }

    static double leerMedida(RegistroFisico r, String medidaTipo) {
        if (medidaTipo == null) return 0.0;
        switch (medidaTipo) {
            case "brazoCm":   return r.getBrazoCm();
            case "piernaCm":  return r.getPiernaCm();
            case "pechoCm":   return r.getPechoCm();
            case "cinturaCm": return r.getCinturaCm();
            case "caderaCm":  return r.getCaderaCm();
            default: return 0.0;
        }
    }
}
