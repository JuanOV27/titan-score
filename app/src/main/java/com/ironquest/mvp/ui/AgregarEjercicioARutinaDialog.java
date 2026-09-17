package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.*;
import com.ironquest.mvp.util.metas.PlanRecomendado;
import com.ironquest.mvp.util.metas.RecomendadorEjercicios;

import java.util.List;

public final class AgregarEjercicioARutinaDialog extends Dialog {

    public interface OnAgregado { void onAgregado(); }

    private final Activity activity;
    private final DataManager dataManager;
    private final Ejercicio ejercicio;
    private final PlanRecomendado plan;
    private final OnAgregado onAgregado;

    public static void mostrar(Activity activity, DataManager dm, Ejercicio ejercicio,
                                PlanRecomendado plan, OnAgregado onAgregado) {
        new AgregarEjercicioARutinaDialog(activity, dm, ejercicio, plan, onAgregado).show();
    }

    private AgregarEjercicioARutinaDialog(Activity activity, DataManager dm, Ejercicio ejercicio,
                                           PlanRecomendado plan, OnAgregado onAgregado) {
        super(activity);
        this.activity = activity;
        this.dataManager = dm;
        this.ejercicio = ejercicio;
        this.plan = plan;
        this.onAgregado = onAgregado;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_agregar_ejercicio_a_rutina);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ((TextView) findViewById(R.id.text_agregar_titulo)).setText("Agregar " + ejercicio.getNombre());

        // Chips de rutinas destino
        ChipGroup chips = findViewById(R.id.chip_group_rutinas_destino);
        List<Rutina> rutinas = dataManager.getDataStore().getRutinas();
        for (Rutina r : rutinas) {
            Chip c = new Chip(getContext());
            c.setText(r.getNombre());
            c.setCheckable(true);
            c.setTag(r.getId());
            chips.addView(c);
        }
        if (chips.getChildCount() > 0) {
            ((Chip) chips.getChildAt(0)).setChecked(true);
            if (chips.getChildCount() == 1) chips.getChildAt(0).setEnabled(false);
        }

        // Pre-llenar valores del plan
        ((EditText) findViewById(R.id.edit_agregar_series)).setText(String.valueOf(plan.getSeriesSugeridas()));
        ((EditText) findViewById(R.id.edit_agregar_reps)).setText(String.valueOf(plan.getRepsSugeridas()));
        ((EditText) findViewById(R.id.edit_agregar_reps_max)).setText(String.valueOf(plan.getRepsSugeridasMax()));
        double pesoInicial = RecomendadorEjercicios.pesoInicialSugerido(
                ejercicio.getId(), dataManager.getDataStore().getSesiones());
        ((EditText) findViewById(R.id.edit_agregar_peso)).setText(pesoInicial > 0
                ? formatNum(pesoInicial) : "");

        Spinner sp = findViewById(R.id.spinner_agregar_esquema);
        String[] labels = {"Ninguno", "Lineal", "Greyskull (AMRAP)", "Doble progresión", "Automático"};
        int[] valores = {
                RutinaEjercicio.ESQUEMA_NINGUNO, RutinaEjercicio.ESQUEMA_LINEAL,
                RutinaEjercicio.ESQUEMA_GREYSKULL, RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION,
                RutinaEjercicio.ESQUEMA_AUTOMATICO
        };
        ArrayAdapter<String> a = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, labels);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(a);
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] == plan.getEsquemaProgresionSugerido()) { sp.setSelection(i, false); break; }
        }

        findViewById(R.id.button_agregar_cancelar).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_agregar_confirmar).setOnClickListener(v ->
                confirmar(chips, valores));
    }

    private void confirmar(ChipGroup chips, int[] valoresEsquema) {
        String rutinaId = null;
        for (int i = 0; i < chips.getChildCount(); i++) {
            Chip c = (Chip) chips.getChildAt(i);
            if (c.isChecked()) { rutinaId = (String) c.getTag(); break; }
        }
        if (rutinaId == null) {
            Toast.makeText(getContext(), "Selecciona una rutina", Toast.LENGTH_SHORT).show();
            return;
        }
        Rutina rutina = dataManager.getDataStore().buscarRutina(rutinaId);
        if (rutina == null) { dismiss(); return; }

        int series = parseInt(R.id.edit_agregar_series, 3);
        int reps = parseInt(R.id.edit_agregar_reps, 8);
        int repsMax = parseInt(R.id.edit_agregar_reps_max, reps);
        double peso = parseDouble(R.id.edit_agregar_peso, 0);
        int esquema = valoresEsquema[((Spinner) findViewById(R.id.spinner_agregar_esquema)).getSelectedItemPosition()];

        RutinaEjercicio re = new RutinaEjercicio(ejercicio.getId(), series, reps, peso);
        re.setRepeticionesMax(repsMax);
        re.setEsquemaProgresion(esquema);
        rutina.agregarEjercicio(re);
        dataManager.saveSync();
        dataManager.saveCatalogo();

        Toast.makeText(getContext(), "Agregado a " + rutina.getNombre(), Toast.LENGTH_SHORT).show();
        if (onAgregado != null) onAgregado.onAgregado();
        dismiss();
    }

    private int parseInt(int id, int def) {
        try { return Integer.parseInt(((EditText) findViewById(id)).getText().toString().trim()); }
        catch (Exception e) { return def; }
    }

    private double parseDouble(int id, double def) {
        try { return Double.parseDouble(((EditText) findViewById(id)).getText().toString().trim()); }
        catch (Exception e) { return def; }
    }

    private String formatNum(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }
}
