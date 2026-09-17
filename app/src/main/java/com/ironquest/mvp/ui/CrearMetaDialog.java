package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.util.metas.Advertencia;
import com.ironquest.mvp.util.metas.ValidadorMeta;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CrearMetaDialog extends Dialog {

    public interface OnCreada { void onCreada(); }

    private static final String[] MEDIDAS_MUSCULARES = {"brazoCm", "piernaCm", "pechoCm"};
    private static final String[] MEDIDAS_MUSCULARES_LABELS = {"Brazo", "Pierna", "Pecho"};
    private static final String[] MEDIDAS_REDUCIR = {"cinturaCm", "caderaCm"};
    private static final String[] MEDIDAS_REDUCIR_LABELS = {"Cintura", "Cadera"};

    private final Activity activity;
    private final DataManager dataManager;
    private final DataStore dataStore;
    private final OnCreada onCreada;

    private int tipoSeleccionado = Meta.TIPO_AUMENTAR_MEDIDA;
    private String medidaSeleccionada = "brazoCm";
    private String prEjercicioId;
    private String prEjercicioNombre;
    private String fechaObjetivoIso;
    private boolean tituloEditadoPorUsuario = false;
    private EditText editTitulo;

    public static void mostrar(Activity activity, DataManager dataManager, OnCreada onCreada) {
        new CrearMetaDialog(activity, dataManager, onCreada).show();
    }

    private CrearMetaDialog(Activity activity, DataManager dataManager, OnCreada onCreada) {
        super(activity);
        this.activity = activity;
        this.dataManager = dataManager;
        this.dataStore = dataManager.getDataStore();
        this.onCreada = onCreada;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_crear_meta);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        editTitulo = findViewById(R.id.edit_titulo_meta);
        editTitulo.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (editTitulo.hasFocus()) tituloEditadoPorUsuario = true;
            }
        });

        ChipGroup chips = findViewById(R.id.chip_group_tipo_meta);
        chips.check(R.id.chip_tipo_muscular);
        chips.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_tipo_muscular)      tipoSeleccionado = Meta.TIPO_AUMENTAR_MEDIDA;
            else if (id == R.id.chip_tipo_reducir)  tipoSeleccionado = Meta.TIPO_REDUCIR_MEDIDA;
            else if (id == R.id.chip_tipo_peso)     tipoSeleccionado = Meta.TIPO_PESO_CORPORAL;
            else if (id == R.id.chip_tipo_pr)       tipoSeleccionado = Meta.TIPO_PR_EJERCICIO;
            inflarFormulario();
            regenerarTitulo();
        });
        inflarFormulario();

        CheckBox check = findViewById(R.id.check_meta_deadline);
        View containerFecha = findViewById(R.id.container_fecha_meta);
        check.setOnCheckedChangeListener((v, isChecked) ->
                containerFecha.setVisibility(isChecked ? View.VISIBLE : View.GONE));
        findViewById(R.id.button_meta_fecha).setOnClickListener(v -> abrirDatePicker());

        findViewById(R.id.button_meta_cancelar).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_meta_crear).setOnClickListener(v -> intentarCrear());
    }

    private void inflarFormulario() {
        FrameLayout container = findViewById(R.id.container_formulario_meta);
        container.removeAllViews();
        int layoutId;
        switch (tipoSeleccionado) {
            case Meta.TIPO_AUMENTAR_MEDIDA: layoutId = R.layout.form_meta_muscular; break;
            case Meta.TIPO_REDUCIR_MEDIDA:  layoutId = R.layout.form_meta_reducir;  break;
            case Meta.TIPO_PESO_CORPORAL:   layoutId = R.layout.form_meta_peso;     break;
            case Meta.TIPO_PR_EJERCICIO:    layoutId = R.layout.form_meta_pr;       break;
            default: return;
        }
        getLayoutInflater().inflate(layoutId, container, true);
        conectarFormulario();
    }

    private void conectarFormulario() {
        if (tipoSeleccionado == Meta.TIPO_AUMENTAR_MEDIDA
                || tipoSeleccionado == Meta.TIPO_REDUCIR_MEDIDA) {
            Spinner sp = findViewById(R.id.spinner_meta_medida);
            String[] labels = tipoSeleccionado == Meta.TIPO_AUMENTAR_MEDIDA
                    ? MEDIDAS_MUSCULARES_LABELS : MEDIDAS_REDUCIR_LABELS;
            String[] ids = tipoSeleccionado == Meta.TIPO_AUMENTAR_MEDIDA
                    ? MEDIDAS_MUSCULARES : MEDIDAS_REDUCIR;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sp.setAdapter(adapter);
            medidaSeleccionada = ids[0];
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long l) {
                    medidaSeleccionada = ids[pos];
                    regenerarTitulo();
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
            EditText ed = findViewById(R.id.edit_meta_objetivo_cm);
            ed.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { regenerarTitulo(); }
            });
        } else if (tipoSeleccionado == Meta.TIPO_PESO_CORPORAL) {
            EditText ed = findViewById(R.id.edit_meta_peso_kg);
            ed.addTextChangedListener(new SimpleTextWatcher() {
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { regenerarTitulo(); }
            });
        } else if (tipoSeleccionado == Meta.TIPO_PR_EJERCICIO) {
            findViewById(R.id.button_meta_pr_seleccionar_ejercicio).setOnClickListener(v -> {
                EjercicioPicker.mostrar(activity, dataManager, dataStore, ejercicio -> {
                    prEjercicioId = ejercicio.getId();
                    prEjercicioNombre = ejercicio.getNombre();
                    ((TextView) findViewById(R.id.text_meta_pr_ejercicio)).setText(prEjercicioNombre);
                    regenerarTitulo();
                });
            });
            SimpleTextWatcher w = new SimpleTextWatcher() {
                @Override public void onTextChanged(CharSequence s, int st, int b, int c) { regenerarTitulo(); }
            };
            ((EditText) findViewById(R.id.edit_meta_pr_peso)).addTextChangedListener(w);
            ((EditText) findViewById(R.id.edit_meta_pr_reps)).addTextChangedListener(w);
        }
    }

    private void regenerarTitulo() {
        if (tituloEditadoPorUsuario) return;
        String t = "";
        switch (tipoSeleccionado) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
                t = "Aumentar " + labelDeMedida(medidaSeleccionada) + " a "
                        + leerDouble(R.id.edit_meta_objetivo_cm) + " cm"; break;
            case Meta.TIPO_REDUCIR_MEDIDA:
                t = "Reducir " + labelDeMedida(medidaSeleccionada) + " a "
                        + leerDouble(R.id.edit_meta_objetivo_cm) + " cm"; break;
            case Meta.TIPO_PESO_CORPORAL:
                t = "Alcanzar " + leerDouble(R.id.edit_meta_peso_kg) + " kg"; break;
            case Meta.TIPO_PR_EJERCICIO:
                if (prEjercicioNombre != null) {
                    t = "PR " + prEjercicioNombre + ": " + leerDouble(R.id.edit_meta_pr_peso)
                            + " kg × " + leerInt(R.id.edit_meta_pr_reps);
                }
                break;
        }
        editTitulo.setText(t);
    }

    private void abrirDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Fecha objetivo")
                .build();
        picker.addOnPositiveButtonClickListener(millis -> {
            LocalDate fecha = LocalDate.ofEpochDay(millis / 86_400_000L);
            fechaObjetivoIso = fecha.toString();
            ((TextView) findViewById(R.id.text_meta_fecha_elegida)).setText(fecha.toString());
        });
        picker.show(((androidx.fragment.app.FragmentActivity) activity)
                .getSupportFragmentManager(), "fecha_meta");
    }

    private void intentarCrear() {
        Meta meta = construirMeta();
        if (meta == null) return;

        Advertencia adv = ValidadorMeta.validar(meta, dataStore);
        if (adv == null) {
            confirmarYPersistir(meta);
        } else {
            new AlertDialog.Builder(getContext())
                    .setTitle("Aviso sobre tu meta")
                    .setMessage(adv.getTexto())
                    .setPositiveButton("Crear igual", (d, w) -> {
                        meta.setAdvertenciaAceptada(true);
                        confirmarYPersistir(meta);
                    })
                    .setNegativeButton("Ajustar", null)
                    .show();
        }
    }

    private void confirmarYPersistir(Meta meta) {
        boolean ok = dataManager.agregarMeta(meta);
        if (!ok) {
            Toast.makeText(getContext(),
                    "Cumple o descarta una meta antes de crear otra", Toast.LENGTH_LONG).show();
            return;
        }
        if (onCreada != null) onCreada.onCreada();
        dismiss();
    }

    private Meta construirMeta() {
        String titulo = editTitulo.getText() != null ? editTitulo.getText().toString().trim() : "";
        if (TextUtils.isEmpty(titulo)) {
            Toast.makeText(getContext(), "Escribe un título", Toast.LENGTH_SHORT).show();
            return null;
        }
        String id = dataManager.newId("m");
        String fechaCreacion = LocalDateTime.now().toString();
        Meta m = new Meta(id, tipoSeleccionado, titulo, fechaCreacion);
        m.setFechaObjetivo(fechaObjetivoIso);

        switch (tipoSeleccionado) {
            case Meta.TIPO_AUMENTAR_MEDIDA:
            case Meta.TIPO_REDUCIR_MEDIDA:
                m.setMedidaTipo(medidaSeleccionada);
                m.setValorObjetivo(leerDouble(R.id.edit_meta_objetivo_cm));
                m.setValorInicial(leerMedidaActual(medidaSeleccionada));
                if (m.getValorObjetivo() <= 0) { toast("Ingresa un objetivo válido"); return null; }
                break;
            case Meta.TIPO_PESO_CORPORAL:
                m.setValorObjetivo(leerDouble(R.id.edit_meta_peso_kg));
                m.setValorInicial(leerPesoActual());
                if (m.getValorObjetivo() <= 0) { toast("Ingresa un peso objetivo"); return null; }
                break;
            case Meta.TIPO_PR_EJERCICIO:
                if (prEjercicioId == null) { toast("Selecciona un ejercicio"); return null; }
                m.setEjercicioId(prEjercicioId);
                m.setPesoObjetivoKg(leerDouble(R.id.edit_meta_pr_peso));
                m.setRepsObjetivo(leerInt(R.id.edit_meta_pr_reps));
                if (m.getPesoObjetivoKg() <= 0 || m.getRepsObjetivo() <= 0) {
                    toast("Ingresa peso y reps > 0"); return null;
                }
                m.setValorInicial(m.getPesoObjetivoKg() * m.getRepsObjetivo() * 0.5);
                break;
        }
        return m;
    }

    // ---- helpers ----

    private String labelDeMedida(String medidaId) {
        switch (medidaId) {
            case "brazoCm": return "brazo";
            case "piernaCm": return "pierna";
            case "pechoCm": return "pecho";
            case "cinturaCm": return "cintura";
            case "caderaCm": return "cadera";
            default: return "medida";
        }
    }

    private double leerDouble(int id) {
        EditText e = findViewById(id);
        if (e == null || e.getText() == null) return 0;
        try { return Double.parseDouble(e.getText().toString().trim()); }
        catch (Exception ex) { return 0; }
    }

    private int leerInt(int id) {
        EditText e = findViewById(id);
        if (e == null || e.getText() == null) return 0;
        try { return Integer.parseInt(e.getText().toString().trim()); }
        catch (Exception ex) { return 0; }
    }

    private double leerMedidaActual(String medidaId) {
        java.util.List<RegistroFisico> h = dataStore.getHistorialFisico();
        if (h.isEmpty()) return 0;
        RegistroFisico r = h.get(h.size() - 1);
        switch (medidaId) {
            case "brazoCm": return r.getBrazoCm();
            case "piernaCm": return r.getPiernaCm();
            case "pechoCm": return r.getPechoCm();
            case "cinturaCm": return r.getCinturaCm();
            case "caderaCm": return r.getCaderaCm();
            default: return 0;
        }
    }

    private double leerPesoActual() {
        java.util.List<RegistroFisico> h = dataStore.getHistorialFisico();
        for (int i = h.size() - 1; i >= 0; i--) {
            if (h.get(i).getPesoKg() > 0) return h.get(i).getPesoKg();
        }
        return 0;
    }

    private void toast(String s) { Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show(); }

    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void afterTextChanged(Editable s) {}
    }
}
