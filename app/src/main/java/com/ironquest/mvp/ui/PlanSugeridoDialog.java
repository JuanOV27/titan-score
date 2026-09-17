package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.*;
import com.ironquest.mvp.util.metas.*;

public final class PlanSugeridoDialog extends Dialog {

    public interface OnCerrado { void onCerrado(); }

    private final Activity activity;
    private final DataManager dataManager;
    private final Meta meta;
    private final OnCerrado onCerrado;
    private PlanRecomendado plan;

    public static void mostrar(Activity activity, DataManager dm, Meta meta, OnCerrado onCerrado) {
        new PlanSugeridoDialog(activity, dm, meta, onCerrado).show();
    }

    private PlanSugeridoDialog(Activity activity, DataManager dm, Meta meta, OnCerrado onCerrado) {
        super(activity);
        this.activity = activity;
        this.dataManager = dm;
        this.meta = meta;
        this.onCerrado = onCerrado;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_plan_sugerido);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setOnDismissListener(d -> { if (onCerrado != null) onCerrado.onCerrado(); });

        ((TextView) findViewById(R.id.text_plan_meta_titulo)).setText("Plan sugerido: " + meta.getTitulo());

        plan = RecomendadorEjercicios.recomendar(meta, dataManager.getDataStore());
        ((TextView) findViewById(R.id.text_plan_volumen)).setText(plan.getTextoVolumen());
        ((TextView) findViewById(R.id.text_plan_params)).setText(formatearParams(plan));

        TextView cobertura = findViewById(R.id.text_plan_cobertura);
        if (plan.getTextoCoberturaCompleta() != null) {
            cobertura.setVisibility(View.VISIBLE);
            cobertura.setText(plan.getTextoCoberturaCompleta());
        } else {
            cobertura.setVisibility(View.GONE);
        }

        RecyclerView rv = findViewById(R.id.list_plan_ejercicios);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new PlanAdapter(plan.getEjercicios()));

        ((MaterialButton) findViewById(R.id.button_plan_cerrar)).setOnClickListener(v -> dismiss());
    }

    private String formatearParams(PlanRecomendado p) {
        String esquema = nombreEsquema(p.getEsquemaProgresionSugerido());
        String reps = p.getRepsSugeridas() == p.getRepsSugeridasMax()
                ? String.valueOf(p.getRepsSugeridas())
                : p.getRepsSugeridas() + "-" + p.getRepsSugeridasMax();
        return "Ejercicios nuevos: " + p.getSeriesSugeridas() + " × " + reps + " reps · Esquema " + esquema;
    }

    private String nombreEsquema(int e) {
        switch (e) {
            case RutinaEjercicio.ESQUEMA_LINEAL: return "Lineal";
            case RutinaEjercicio.ESQUEMA_GREYSKULL: return "Greyskull";
            case RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION: return "Doble progresión";
            case RutinaEjercicio.ESQUEMA_AUTOMATICO: return "Automático";
            default: return "Ninguno";
        }
    }

    private class PlanAdapter extends RecyclerView.Adapter<PlanAdapter.VH> {
        private final java.util.List<Recomendacion> items;
        PlanAdapter(java.util.List<Recomendacion> items) { this.items = items; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_plan_ejercicio_sugerido, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Recomendacion r = items.get(pos);
            h.nombre.setText(r.getEjercicio().getNombre());
            h.etiqueta.setText(r.getEtiqueta());
            h.etiqueta.setTextColor(r.getPrioridad() == 0 ? 0xFF2E7D32 : 0xFF757575);
            h.boton.setOnClickListener(v -> {
                AgregarEjercicioARutinaDialog.mostrar(
                        activity, dataManager, r.getEjercicio(), plan,
                        () -> notifyItemChanged(pos));
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView nombre, etiqueta;
            MaterialButton boton;
            VH(@NonNull View v) {
                super(v);
                nombre = v.findViewById(R.id.text_plan_item_nombre);
                etiqueta = v.findViewById(R.id.text_plan_item_etiqueta);
                boton = v.findViewById(R.id.button_plan_item_agregar);
            }
        }
    }
}
