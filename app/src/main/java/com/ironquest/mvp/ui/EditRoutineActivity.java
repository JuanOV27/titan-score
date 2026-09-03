package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.util.HashMap;
import java.util.Map;

public class EditRoutineActivity extends AppCompatActivity {

    public static final String EXTRA_RUTINA_ID = "extra_rutina_id";

    private DataManager dataManager;
    private DataStore dataStore;
    private Rutina rutina;
    private boolean esNueva;

    private TextInputEditText editNombre;
    private RutinaEjercicioEditAdapter adapter;
    private Map<String, Ejercicio> catalogoPorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_routine);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();

        catalogoPorId = new HashMap<>();
        for (Ejercicio ejercicio : dataStore.getEjercicios()) {
            catalogoPorId.put(ejercicio.getId(), ejercicio);
        }

        String rutinaId = getIntent().getStringExtra(EXTRA_RUTINA_ID);
        if (rutinaId != null) {
            rutina = dataStore.buscarRutina(rutinaId);
            esNueva = false;
        }
        if (rutina == null) {
            rutina = new Rutina(dataManager.newId("r"), "");
            esNueva = true;
        }
        setTitle(esNueva ? "Nueva rutina" : "Editar rutina");

        editNombre = findViewById(R.id.edit_nombre_rutina);
        editNombre.setText(rutina.getNombre());

        RecyclerView recycler = findViewById(R.id.recycler_ejercicios_rutina);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RutinaEjercicioEditAdapter(rutina.getEjercicios(), catalogoPorId, position -> {
            rutina.quitarEjercicio(position);
            adapter.notifyItemRemoved(position);
        });
        recycler.setAdapter(adapter);

        findViewById(R.id.button_agregar_ejercicio).setOnClickListener(v ->
                EjercicioPicker.mostrar(this, dataManager, dataStore, this::agregarEjercicioARutina));
        findViewById(R.id.button_guardar_rutina).setOnClickListener(v -> guardarRutina());
    }

    private void agregarEjercicioARutina(Ejercicio ejercicio) {
        catalogoPorId.putIfAbsent(ejercicio.getId(), ejercicio);
        rutina.agregarEjercicio(new RutinaEjercicio(ejercicio.getId(), 3, 10, 0.0));
        adapter.notifyItemInserted(rutina.getCantidadEjercicios() - 1);
    }

    private void guardarRutina() {
        String nombre = editNombre.getText() != null ? editNombre.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nombre)) {
            Toast.makeText(this, "Ponle un nombre a la rutina", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rutina.getEjercicios().isEmpty()) {
            Toast.makeText(this, "Agrega al menos un ejercicio", Toast.LENGTH_SHORT).show();
            return;
        }
        rutina.setNombre(nombre);
        if (esNueva) {
            dataStore.getRutinas().add(rutina);
        }
        dataManager.save();
        finish();
    }
}
