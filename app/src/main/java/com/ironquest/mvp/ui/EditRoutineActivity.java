package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.util.HashMap;
import java.util.Map;

public class EditRoutineActivity extends BaseActivity {

    public static final String EXTRA_RUTINA_ID = "extra_rutina_id";

    private DataManager dataManager;
    private DataStore dataStore;
    private Rutina rutina;
    private boolean esNueva;

    private TextInputEditText editNombre;
    private RutinaEjercicioEditAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private Map<String, Ejercicio> catalogoPorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_routine);

        configurarToolbar(R.id.toolbar, null, false);

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
        adapter = new RutinaEjercicioEditAdapter(rutina.getEjercicios(), catalogoPorId, new RutinaEjercicioEditAdapter.Listener() {
            @Override
            public void onQuitar(int position) {
                rutina.quitarEjercicio(position);
                adapter.notifyItemRemoved(position);
            }

            @Override
            public void onEditarPersonalizado(int position) {
                RutinaEjercicio item = rutina.getEjercicios().get(position);
                Ejercicio ejercicio = catalogoPorId.get(item.getEjercicioId());
                if (ejercicio != null) {
                    mostrarDialogoEditarEjercicio(ejercicio, position);
                }
            }

            @Override
            public void onIniciarArrastre(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
        recycler.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                   @NonNull RecyclerView.ViewHolder target) {
                int desde = viewHolder.getBindingAdapterPosition();
                int hasta = target.getBindingAdapterPosition();
                if (desde == RecyclerView.NO_POSITION || hasta == RecyclerView.NO_POSITION) {
                    return false;
                }
                rutina.moverEjercicio(desde, hasta);
                adapter.notifyItemMoved(desde, hasta);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Sin swipe: solo se usa arrastre vertical.
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recycler);

        findViewById(R.id.button_agregar_ejercicio).setOnClickListener(v ->
                EjercicioPicker.mostrar(this, dataManager, dataStore, this::agregarEjercicioARutina));
        findViewById(R.id.button_guardar_rutina).setOnClickListener(v -> guardarRutina());
    }

    private void agregarEjercicioARutina(Ejercicio ejercicio) {
        catalogoPorId.putIfAbsent(ejercicio.getId(), ejercicio);
        rutina.agregarEjercicio(new RutinaEjercicio(ejercicio.getId(), 3, 10, 0.0));
        adapter.notifyItemInserted(rutina.getCantidadEjercicios() - 1);
    }

    private void mostrarDialogoEditarEjercicio(Ejercicio ejercicio, int position) {
        View view = getLayoutInflater().inflate(R.layout.dialog_custom_exercise, null);
        TextInputEditText editNombreEjercicio = view.findViewById(R.id.edit_nombre_ejercicio_personalizado);
        TextInputEditText editGrupo = view.findViewById(R.id.edit_grupo_ejercicio_personalizado);
        editNombreEjercicio.setText(ejercicio.getNombre());
        editGrupo.setText(ejercicio.getGrupoMuscular());

        new AlertDialog.Builder(this)
                .setTitle("Editar ejercicio personalizado")
                .setMessage("El cambio se aplicará en todas las rutinas donde uses este ejercicio.")
                .setView(view)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombreNuevo = editNombreEjercicio.getText() != null
                            ? editNombreEjercicio.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(nombreNuevo)) {
                        Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String grupoNuevo = editGrupo.getText() != null
                            ? editGrupo.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(grupoNuevo)) {
                        grupoNuevo = "Personalizado";
                    }
                    ejercicio.setNombre(nombreNuevo);
                    ejercicio.setGrupoMuscular(grupoNuevo);
                    adapter.notifyItemChanged(position);
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
