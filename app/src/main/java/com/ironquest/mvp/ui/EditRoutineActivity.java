package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.ironquest.mvp.model.SugerenciaPendiente;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private View cardMigracion;
    private TextView textMigracionDescripcion;

    private View cardBannerSugerencias;
    private TextView textBannerSugerencias;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_routine);

        configurarToolbar(R.id.toolbar, null, true);

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
        double incrementoGlobal = dataStore.getUsuario() != null
                ? dataStore.getUsuario().getIncrementoPeso() : 2.5;
        adapter = new RutinaEjercicioEditAdapter(rutina.getEjercicios(), catalogoPorId, incrementoGlobal,
                new RutinaEjercicioEditAdapter.Listener() {
            @Override
            public void onQuitar(int position) {
                rutina.quitarEjercicio(position);
                adapter.notifyItemRemoved(position);
                actualizarBannerMigracion();
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
        findViewById(R.id.button_cancelar_rutina).setOnClickListener(v -> finish());

        cardMigracion = findViewById(R.id.card_migracion_ejercicios);
        textMigracionDescripcion = findViewById(R.id.text_migracion_descripcion);
        findViewById(R.id.button_migrar_ejercicios).setOnClickListener(v -> abrirMigracion());
        actualizarBannerMigracion();

        cardBannerSugerencias = findViewById(R.id.card_banner_sugerencias);
        textBannerSugerencias = findViewById(R.id.text_banner_sugerencias);
        findViewById(R.id.button_revisar_sugerencias).setOnClickListener(v -> abrirDialogoRevisar());
        findViewById(R.id.button_ignorar_todos_sugerencias).setOnClickListener(v -> {
            dataManager.eliminarSugerenciasDeRutina(rutina.getId());
            actualizarBannerSugerencias();
        });
        actualizarBannerSugerencias();
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarBannerSugerencias();
    }

    private void actualizarBannerSugerencias() {
        List<SugerenciaPendiente> sp = dataManager.getSugerenciasPendientesDeRutina(rutina.getId());
        if (sp.isEmpty()) {
            cardBannerSugerencias.setVisibility(View.GONE);
            return;
        }
        cardBannerSugerencias.setVisibility(View.VISIBLE);
        int n = sp.size();
        textBannerSugerencias.setText(n + (n == 1
                ? " ejercicio tiene un ajuste sugerido."
                : " ejercicios tienen ajustes sugeridos."));
    }

    private void abrirDialogoRevisar() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_revisar_sugerencias);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        View card = dialog.findViewById(R.id.card_sugerencias_dialog);
        renderSugerenciasEnCard(card, dialog);
        dialog.findViewById(R.id.button_cerrar_dialog).setOnClickListener(v -> {
            dialog.dismiss();
            actualizarBannerSugerencias();
        });
        dialog.setOnDismissListener(d -> actualizarBannerSugerencias());
        dialog.show();
    }

    private void renderSugerenciasEnCard(View card, android.app.Dialog dialog) {
        List<SugerenciaPendiente> deLaRutina = dataManager.getSugerenciasPendientesDeRutina(rutina.getId());
        if (deLaRutina.isEmpty()) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        LinearLayout container = card.findViewById(R.id.container_sugerencias);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (SugerenciaPendiente sp : deLaRutina) {
            View item = inflater.inflate(R.layout.item_sugerencia_pendiente, container, false);
            Ejercicio ej = dataStore.buscarEjercicio(sp.getEjercicioId());
            String nombre = ej != null ? ej.getNombre() : "Ejercicio";
            ((TextView) item.findViewById(R.id.text_nombre_ejercicio)).setText(nombre);
            ((TextView) item.findViewById(R.id.text_cambio)).setText(formatearCambio(sp));
            ((TextView) item.findViewById(R.id.text_explicacion)).setText(sp.getExplicacion());
            item.findViewById(R.id.button_aceptar).setOnClickListener(v -> {
                dataManager.aplicarSugerencia(sp.getId());
                renderSugerenciasEnCard(card, dialog);
                refrescarListadoRutina();
            });
            item.findViewById(R.id.button_ignorar).setOnClickListener(v -> {
                dataManager.eliminarSugerencia(sp.getId());
                renderSugerenciasEnCard(card, dialog);
            });
            container.addView(item);
        }
        card.findViewById(R.id.button_aceptar_todas).setOnClickListener(v -> {
            for (SugerenciaPendiente sp : new ArrayList<>(deLaRutina)) {
                dataManager.aplicarSugerencia(sp.getId());
            }
            renderSugerenciasEnCard(card, dialog);
            refrescarListadoRutina();
        });
    }

    private void refrescarListadoRutina() {
        adapter.notifyDataSetChanged();
    }

    private static String formatearCambio(SugerenciaPendiente sp) {
        return formatearPeso(sp.getPesoActual()) + " kg × " + sp.getRepeticionesActuales()
                + " → " + formatearPeso(sp.getPesoSugerido()) + " kg × " + sp.getRepeticionesSugeridas();
    }

    private static String formatearPeso(double p) {
        if (p == 0) {
            return "0";
        }
        if (p == Math.floor(p)) {
            return String.valueOf((long) p);
        }
        return String.valueOf(p);
    }

    /**
     * Fila pendiente de migración: su ejercicio viene de una versión anterior — no tiene ficha
     * técnica/GIF y no está marcado como creado por el usuario. Los personalizados y los curados
     * no se cuentan; reemplazarlos o marcarlos deja de "obligar" el guardado.
     */
    private int contarPendientesMigracion() {
        int pendientes = 0;
        for (RutinaEjercicio item : rutina.getEjercicios()) {
            Ejercicio ejercicio = catalogoPorId.get(item.getEjercicioId());
            if (ejercicio == null || (!ejercicio.isPersonalizado() && !ejercicio.tieneFichaTecnica())) {
                pendientes++;
            }
        }
        return pendientes;
    }

    private void actualizarBannerMigracion() {
        int pendientes = contarPendientesMigracion();
        if (!esNueva && pendientes > 0) {
            textMigracionDescripcion.setText(pendientes + " ejercicios provienen de versiones "
                    + "anteriores y no tienen técnica. Reemplázalos o márcalos como tuyos para guardar.");
            cardMigracion.setVisibility(View.VISIBLE);
        } else {
            cardMigracion.setVisibility(View.GONE);
        }
    }

    private void abrirMigracion() {
        MigracionEjerciciosDialog dialog = new MigracionEjerciciosDialog(this, dataManager,
                dataStore, catalogoPorId, rutina, this::refrescarTrasMigracion);
        // "Ahora no" / back también dejan resoluciones aplicadas en memoria: refrescar siempre.
        dialog.setOnDismissListener(d -> refrescarTrasMigracion());
        dialog.show();
    }

    private void refrescarTrasMigracion() {
        adapter.notifyDataSetChanged();
        actualizarBannerMigracion();
    }

    private void agregarEjercicioARutina(Ejercicio ejercicio) {
        catalogoPorId.putIfAbsent(ejercicio.getId(), ejercicio);
        RutinaEjercicio nuevo = new RutinaEjercicio(ejercicio.getId(), 3, 10, 0.0);
        nuevo.setEsquemaProgresion(RutinaEjercicio.ESQUEMA_AUTOMATICO);
        nuevo.setRepeticionesMax(nuevo.getRepeticiones() + 4);
        rutina.agregarEjercicio(nuevo);
        adapter.notifyItemInserted(rutina.getCantidadEjercicios() - 1);
    }

    private void mostrarDialogoEditarEjercicio(Ejercicio ejercicio, int position) {
        View view = getLayoutInflater().inflate(R.layout.dialog_custom_exercise, null);
        TextInputEditText editNombreEjercicio = view.findViewById(R.id.edit_nombre_ejercicio_personalizado);
        Spinner spinnerGrupo = view.findViewById(R.id.spinner_grupo_ejercicio_personalizado);
        List<String> opcionesGrupo = new ArrayList<>(Ejercicio.gruposMusculares(dataStore.getEjercicios()));
        String grupoActual = ejercicio.getGrupoMuscular();
        if (grupoActual != null && !grupoActual.trim().isEmpty()
                && Ejercicio.indiceGrupoMuscular(opcionesGrupo, grupoActual) == 0
                && !"Personalizado".equalsIgnoreCase(grupoActual.trim())) {
            opcionesGrupo.add(0, grupoActual.trim());
        }
        ArrayAdapter<String> adapterGrupo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, opcionesGrupo);
        adapterGrupo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGrupo.setAdapter(adapterGrupo);
        spinnerGrupo.setSelection(Ejercicio.indiceGrupoMuscular(opcionesGrupo, grupoActual));
        editNombreEjercicio.setText(ejercicio.getNombre());

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
                    String grupoNuevo = (String) spinnerGrupo.getSelectedItem();
                    if (grupoNuevo == null) {
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
        int pendientes = contarPendientesMigracion();
        if (pendientes > 0) {
            Toast.makeText(this, "Resuelve " + pendientes + " ejercicios sin técnica antes de guardar",
                    Toast.LENGTH_LONG).show();
            abrirMigracion();
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
