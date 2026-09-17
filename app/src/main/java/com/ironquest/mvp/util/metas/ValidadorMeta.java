package com.ironquest.mvp.util.metas;

import androidx.annotation.Nullable;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.util.PerfilFisicoUtil;

import java.util.List;

/**
 * Valida al crear una Meta. Nunca bloquea, solo advierte. El diálogo de crear muestra el
 * texto y el usuario decide entre Ajustar y Crear igual.
 */
public final class ValidadorMeta {

    private static final double DELTA_AGRESIVO_MEDIDA_AUMENTAR = 0.20; // >20% del valor actual
    private static final double DELTA_AGRESIVO_MEDIDA_REDUCIR  = 0.15;
    private static final double DELTA_AGRESIVO_PESO            = 0.15;
    private static final double DELTA_AGRESIVO_PR              = 0.20;
    private static final double IMC_MIN_SANO                   = 18.5;
    private static final double IMC_MAX_SANO                   = 30.0;
    private static final double ICC_NEUTRO_MIN                 = 0.72;

    private ValidadorMeta() {}

    public static @Nullable Advertencia validar(Meta meta, DataStore ds) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA: return validarAumentarMedida(meta, ds);
            case Meta.TIPO_REDUCIR_MEDIDA:  return validarReducirMedida(meta, ds);
            case Meta.TIPO_PESO_CORPORAL:   return validarPesoCorporal(meta, ds);
            case Meta.TIPO_PR_EJERCICIO:    return validarPrEjercicio(meta, ds);
            default: return null;
        }
    }

    private static Advertencia validarAumentarMedida(Meta meta, DataStore ds) {
        double actual = leerMedidaActual(ds, meta.getMedidaTipo());
        if (actual <= 0) return null;
        double delta = (meta.getValorObjetivo() - actual) / actual;
        if (delta > DELTA_AGRESIVO_MEDIDA_AUMENTAR) {
            int porc = (int) Math.round(delta * 100);
            return new Advertencia(
                    "Aumentar tu " + nombreLegibleMedida(meta.getMedidaTipo())
                            + " de " + formatCm(actual) + " a " + formatCm(meta.getValorObjetivo())
                            + " cm es un salto de " + porc + " %. Un aumento realista suele ser de "
                            + "1-2 cm por mes con entrenamiento constante.",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        return null;
    }

    private static Advertencia validarReducirMedida(Meta meta, DataStore ds) {
        double actual = leerMedidaActual(ds, meta.getMedidaTipo());
        if (actual <= 0) return null;

        // Regla ICC: si la meta es cintura y llevaría a ICC < 0.72
        if ("cinturaCm".equals(meta.getMedidaTipo())) {
            RegistroFisico ultimo = ultimoRegistro(ds);
            if (ultimo != null && ultimo.getCaderaCm() > 0) {
                double iccPropuesto = meta.getValorObjetivo() / ultimo.getCaderaCm();
                if (iccPropuesto < ICC_NEUTRO_MIN) {
                    return new Advertencia(
                            "Ese objetivo te llevaría a un índice cintura-cadera muy bajo ("
                                    + String.format("%.2f", iccPropuesto)
                                    + "). Considera un valor más moderado.",
                            Advertencia.SEVERIDAD_ATENCION);
                }
            }
        }

        // Regla delta relativo
        double delta = (actual - meta.getValorObjetivo()) / actual;
        if (delta > DELTA_AGRESIVO_MEDIDA_REDUCIR) {
            int porc = (int) Math.round(delta * 100);
            return new Advertencia(
                    "Una reducción de " + porc + " % de tu " + nombreLegibleMedida(meta.getMedidaTipo())
                            + " es agresiva. Considera dividirla en metas más pequeñas.",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        return null;
    }

    private static Advertencia validarPesoCorporal(Meta meta, DataStore ds) {
        RegistroFisico ultimo = ultimoRegistro(ds);
        double actual = ultimo != null ? ultimo.getPesoKg() : 0;
        double altura = ultimo != null ? ultimo.getAlturaCm() : 0;
        if (actual <= 0 || altura <= 0) return null;

        double imcObj = PerfilFisicoUtil.calcularImc(meta.getValorObjetivo(), altura);
        if (imcObj < IMC_MIN_SANO) {
            return new Advertencia(
                    "Ese peso te dejaría en bajo peso (IMC " + String.format("%.1f", imcObj)
                            + "). Es riesgoso para tu salud.",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        if (imcObj > IMC_MAX_SANO) {
            return new Advertencia(
                    "Ese peso te dejaría en rango de obesidad (IMC "
                            + String.format("%.1f", imcObj) + ").",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        double delta = Math.abs(meta.getValorObjetivo() - actual) / actual;
        if (delta > DELTA_AGRESIVO_PESO) {
            int porc = (int) Math.round(delta * 100);
            return new Advertencia(
                    "Un cambio de " + porc + " % de tu peso corporal es agresivo. "
                            + "Considera dividirlo en metas más pequeñas.",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        return null;
    }

    private static Advertencia validarPrEjercicio(Meta meta, DataStore ds) {
        double mejorPeso = mejorPesoRegistrado(ds, meta.getEjercicioId(), meta.getRepsObjetivo());
        if (mejorPeso <= 0) {
            return new Advertencia(
                    "No tenemos registros previos de este ejercicio. La meta se crea, pero no "
                            + "podremos comparar contra un PR anterior.",
                    Advertencia.SEVERIDAD_INFO);
        }
        double delta = (meta.getPesoObjetivoKg() - mejorPeso) / mejorPeso;
        if (delta > DELTA_AGRESIVO_PR) {
            int porc = (int) Math.round(delta * 100);
            return new Advertencia(
                    "Subir " + formatKg(mejorPeso) + " → " + formatKg(meta.getPesoObjetivoKg())
                            + " kg es un salto de " + porc + " %. Considera dividirlo (ej. primero "
                            + formatKg(mejorPeso + 5) + " kg × " + meta.getRepsObjetivo() + ").",
                    Advertencia.SEVERIDAD_ATENCION);
        }
        return null;
    }

    // ---- helpers ----

    private static double leerMedidaActual(DataStore ds, String medidaTipo) {
        RegistroFisico r = ultimoRegistro(ds);
        if (r == null || medidaTipo == null) return 0;
        switch (medidaTipo) {
            case "brazoCm":   return r.getBrazoCm();
            case "piernaCm":  return r.getPiernaCm();
            case "pechoCm":   return r.getPechoCm();
            case "cinturaCm": return r.getCinturaCm();
            case "caderaCm":  return r.getCaderaCm();
            default: return 0;
        }
    }

    private static RegistroFisico ultimoRegistro(DataStore ds) {
        List<RegistroFisico> h = ds.getHistorialFisico();
        return h.isEmpty() ? null : h.get(h.size() - 1);
    }

    /** Mejor peso alcanzado con al menos `repsMinimas` reps completadas. */
    private static double mejorPesoRegistrado(DataStore ds, String ejercicioId, int repsMinimas) {
        double mejor = 0;
        for (Sesion s : ds.getSesiones()) {
            if (!s.estaFinalizada()) continue;
            EjercicioSesion es = s.buscarEjercicio(ejercicioId);
            if (es == null) continue;
            for (SerieSesion serie : es.getSeries()) {
                if (!serie.isCompletada()) continue;
                if (serie.getRepeticiones() >= repsMinimas && serie.getPeso() > mejor) {
                    mejor = serie.getPeso();
                }
            }
        }
        return mejor;
    }

    private static String nombreLegibleMedida(String medidaTipo) {
        if (medidaTipo == null) return "medida";
        switch (medidaTipo) {
            case "brazoCm":   return "brazo";
            case "piernaCm":  return "pierna";
            case "pechoCm":   return "pecho";
            case "cinturaCm": return "cintura";
            case "caderaCm":  return "cadera";
            default: return "medida";
        }
    }

    private static String formatCm(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }

    private static String formatKg(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }
}
