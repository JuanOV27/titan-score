package com.ironquest.mvp.util.metas;

import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;

/**
 * Contrato de evaluación de una Meta contra el estado actual del DataStore. Polimórfico por
 * tipo de meta (una subclase por cada constante TIPO_* de Meta). Sin estado propio; cada
 * llamada recibe la meta y el dataStore.
 */
public abstract class EvaluadorMeta {

    /** Instancia la subclase correcta según el tipo de meta. */
    public static EvaluadorMeta para(Meta meta) {
        switch (meta.getTipo()) {
            case Meta.TIPO_AUMENTAR_MEDIDA: return new EvalMedidaMuscular();
            case Meta.TIPO_REDUCIR_MEDIDA:  return new EvalReducirMedida();
            case Meta.TIPO_PESO_CORPORAL:   return new EvalPesoCorporal();
            case Meta.TIPO_PR_EJERCICIO:    return new EvalPrEjercicio();
            default:
                throw new IllegalArgumentException("Tipo de meta desconocido: " + meta.getTipo());
        }
    }

    /** Progreso 0.0 a 1.0 (clamped). */
    public abstract double calcularProgreso(Meta meta, DataStore ds);

    /** Si el estado actual del DataStore alcanza o supera el objetivo. */
    public abstract boolean cumplida(Meta meta, DataStore ds);

    /** Texto legible: "32 → 40 cm (20 %)" o "45 kg × 6 → objetivo 50 kg × 8". */
    public abstract String textoProgreso(Meta meta, DataStore ds);

    /** Valor "actual" que compara contra objetivo. Devuelve 0.0 si no hay dato. */
    protected abstract double valorActual(Meta meta, DataStore ds);

    /**
     * Fecha ISO del último dato relevante (último RegistroFisico con la medida, o última
     * sesión con el ejercicio de la meta). Devuelve null si no hay ninguno. La UI usa esto
     * para el banner "actualiza tu {medida}" cuando pasó > 14 días.
     */
    public abstract String ultimoRegistroRelevante(Meta meta, DataStore ds);

    // ---- helpers compartidos ----

    protected static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    protected static String formatearPorcentaje(double frac) {
        return ((int) Math.round(frac * 100)) + " %";
    }

    protected static String formatearNumero(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}
