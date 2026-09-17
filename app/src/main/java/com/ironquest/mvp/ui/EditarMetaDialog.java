package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.Meta;

import java.time.LocalDate;

public final class EditarMetaDialog extends Dialog {

    public interface OnCambio { void onCambio(); }

    private final Activity activity;
    private final DataManager dataManager;
    private final Meta meta;
    private final OnCambio onCambio;
    private String fechaIso;

    public static void mostrar(Activity a, DataManager dm, Meta m, OnCambio onCambio) {
        new EditarMetaDialog(a, dm, m, onCambio).show();
    }

    private EditarMetaDialog(Activity a, DataManager dm, Meta m, OnCambio onCambio) {
        super(a);
        this.activity = a; this.dataManager = dm; this.meta = m; this.onCambio = onCambio;
        this.fechaIso = m.getFechaObjetivo();
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_editar_meta);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText titulo = findViewById(R.id.edit_editar_titulo);
        titulo.setText(meta.getTitulo());

        EditText objetivo = findViewById(R.id.edit_editar_objetivo);
        double valObj = meta.getTipo() == Meta.TIPO_PR_EJERCICIO
                ? meta.getPesoObjetivoKg() : meta.getValorObjetivo();
        objetivo.setText(valObj == Math.floor(valObj) ? String.valueOf((long) valObj) : String.valueOf(valObj));

        CheckBox check = findViewById(R.id.check_editar_deadline);
        TextView fechaLabel = findViewById(R.id.text_editar_fecha_elegida);
        View botonFecha = findViewById(R.id.button_editar_fecha);

        check.setChecked(fechaIso != null && !fechaIso.isEmpty());
        botonFecha.setEnabled(check.isChecked());
        fechaLabel.setText(fechaIso != null ? fechaIso : "");

        check.setOnCheckedChangeListener((v, isChecked) -> {
            botonFecha.setEnabled(isChecked);
            if (!isChecked) { fechaIso = null; fechaLabel.setText(""); }
        });
        botonFecha.setOnClickListener(v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Fecha objetivo").build();
            picker.addOnPositiveButtonClickListener(millis -> {
                LocalDate f = LocalDate.ofEpochDay(millis / 86_400_000L);
                fechaIso = f.toString();
                fechaLabel.setText(fechaIso);
            });
            picker.show(((androidx.fragment.app.FragmentActivity) activity).getSupportFragmentManager(), "editar_fecha");
        });

        findViewById(R.id.button_editar_cancelar).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_editar_guardar).setOnClickListener(v -> {
            meta.setTitulo(titulo.getText().toString().trim());
            double nuevoObj;
            try { nuevoObj = Double.parseDouble(objetivo.getText().toString().trim()); }
            catch (Exception e) { Toast.makeText(getContext(), "Objetivo inválido", Toast.LENGTH_SHORT).show(); return; }
            if (meta.getTipo() == Meta.TIPO_PR_EJERCICIO) meta.setPesoObjetivoKg(nuevoObj);
            else meta.setValorObjetivo(nuevoObj);
            meta.setFechaObjetivo(fechaIso);
            dataManager.save();
            if (onCambio != null) onCambio.onCambio();
            dismiss();
        });
    }
}
