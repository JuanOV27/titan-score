package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Selector de ejercicios: chips de filtro por músculo específico (vocabulario fijo de
 * {@link Ejercicio#MUSCULOS_OBJETIVO}) sobre una lista con miniatura, nombre y equipo. Un
 * ejercicio personalizado nunca tiene {@code musculoObjetivo} de la lista fija, así que solo
 * aparece bajo "Todas" — limitación aceptada, fuera de alcance de esta slice.
 */
final class EjercicioPickerDialog extends Dialog {

    private final DataManager dataManager;
    private final DataStore dataStore;
    private final EjercicioPicker.Listener listener;

    private ChipGroup chipGroup;
    private RecyclerView recyclerView;
    private TextView textoVacio;
    private EditText editBuscar;
    private EjercicioPickerAdapter adapter;
    private String musculoSeleccionado;
    private String textoBusqueda = "";

    EjercicioPickerDialog(Activity activity, DataManager dataManager, DataStore dataStore,
                           EjercicioPicker.Listener listener) {
        super(activity);
        this.dataManager = dataManager;
        this.dataStore = dataStore;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_ejercicio_picker);
        setCancelable(true);

        // La lista de ~100 ejercicios + chips necesita casi toda la pantalla, a diferencia de
        // RestTimerDialog (contenido compacto que cabe en el tamaño default del tema).
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.85));

        chipGroup = findViewById(R.id.chip_group_musculos);
        recyclerView = findViewById(R.id.recycler_ejercicios_picker);
        textoVacio = findViewById(R.id.text_picker_vacio);
        editBuscar = findViewById(R.id.edit_buscar_ejercicio);

        adapter = new EjercicioPickerAdapter(getContext(), ejercicio -> {
            listener.onEjercicioElegido(ejercicio);
            dismiss();
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        editBuscar.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onChanged(String text) {
                textoBusqueda = text.trim().toLowerCase(Locale.getDefault());
                aplicarFiltro();
            }
        });

        findViewById(R.id.button_ejercicio_personalizado).setOnClickListener(v -> mostrarPersonalizado());

        poblarChips();
        aplicarFiltro();
    }

    private void poblarChips() {
        Set<String> presentes = new LinkedHashSet<>();
        for (Ejercicio ejercicio : dataStore.getEjercicios()) {
            if (ejercicio.getMusculoObjetivo() != null) {
                presentes.add(ejercicio.getMusculoObjetivo());
            }
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());

        Chip chipTodas = (Chip) inflater.inflate(R.layout.item_chip_musculo, chipGroup, false);
        chipTodas.setText("Todas");
        chipTodas.setTag(null);
        chipGroup.addView(chipTodas);
        chipTodas.setChecked(true);

        for (String musculo : Ejercicio.MUSCULOS_OBJETIVO) {
            if (!presentes.contains(musculo)) {
                continue;
            }
            Chip chip = (Chip) inflater.inflate(R.layout.item_chip_musculo, chipGroup, false);
            chip.setText(musculo);
            chip.setTag(musculo);
            chipGroup.addView(chip);
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip seleccionado = group.findViewById(checkedIds.get(0));
            musculoSeleccionado = (String) seleccionado.getTag();
            aplicarFiltro();
        });
    }

    /**
     * Filtro combinado: AND entre el chip de músculo y el texto tecleado. Sin folding de acentos
     * a propósito — los nombres del catálogo curado son texto español bien acentuado y las
     * búsquedas se teclean en un teclado móvil con autocorrección; el costo de complejidad de
     * {@code Normalizer.NFD} no se justifica. No "arreglar" sin re-derivar este trade-off.
     */
    private void aplicarFiltro() {
        List<Ejercicio> filtrados = new ArrayList<>();
        for (Ejercicio ejercicio : dataStore.getEjercicios()) {
            boolean coincideMusculo = musculoSeleccionado == null
                    || musculoSeleccionado.equals(ejercicio.getMusculoObjetivo());
            boolean coincideTexto = textoBusqueda.isEmpty()
                    || ejercicio.getNombre().toLowerCase(Locale.getDefault()).contains(textoBusqueda);
            if (coincideMusculo && coincideTexto) {
                filtrados.add(ejercicio);
            }
        }
        adapter.submitLista(filtrados);
        boolean vacio = filtrados.isEmpty();
        recyclerView.setVisibility(vacio ? View.GONE : View.VISIBLE);
        textoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
    }

    private void mostrarPersonalizado() {
        Activity activity = (Activity) getContext();
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_custom_exercise, null);
        EditText editNombre = view.findViewById(R.id.edit_nombre_ejercicio_personalizado);
        EditText editGrupo = view.findViewById(R.id.edit_grupo_ejercicio_personalizado);

        new AlertDialog.Builder(activity)
                .setTitle("Ejercicio personalizado")
                .setView(view)
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String nombre = editNombre.getText().toString().trim();
                    if (TextUtils.isEmpty(nombre)) {
                        Toast.makeText(activity, "Escribe un nombre", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String grupo = editGrupo.getText().toString().trim();
                    if (TextUtils.isEmpty(grupo)) {
                        grupo = "Personalizado";
                    }
                    Ejercicio nuevo = new Ejercicio(dataManager.newId("ex"), nombre, grupo);
                    nuevo.setPersonalizado(true);
                    dataStore.getEjercicios().add(nuevo);
                    listener.onEjercicioElegido(nuevo);
                    dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
