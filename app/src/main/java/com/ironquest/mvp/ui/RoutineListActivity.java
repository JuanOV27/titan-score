package com.ironquest.mvp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.Usuario;
import com.ironquest.mvp.service.SesionTrackingService;
import com.ironquest.mvp.util.PerfilFisicoUtil;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class RoutineListActivity extends AppCompatActivity implements RutinaAdapter.Listener {

    public static final String EXTRA_RUTINA_ID = "extra_rutina_id";
    public static final String EXTRA_SUGERIR_FISICO = "extra_sugerir_fisico";
    private static final String FEEDBACK_URL =
            "https://docs.google.com/forms/d/e/1FAIpQLSfO6Hd6-_l30bv5td8t-mhByoRCZwt4cvNqig72Vf1QI5yZEg/viewform?usp=header";

    private DataManager dataManager;
    private RutinaAdapter adapter;
    private RecyclerView recyclerView;
    private TextView textEmpty;
    private ActivityResultLauncher<String[]> importLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataManager = DataManager.getInstance(this);
        Usuario usuario = dataManager.getDataStore().usuario;
        if (usuario == null || !usuario.sesionActiva) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_routine_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getIntent().getBooleanExtra(EXTRA_SUGERIR_FISICO, false)) {
            mostrarSugerenciaRegistroFisico();
        }

        recyclerView = findViewById(R.id.recycler_rutinas);
        textEmpty = findViewById(R.id.text_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RutinaAdapter(dataManager.getDataStore().rutinas, this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_nueva_rutina);
        fab.setOnClickListener(v -> startActivity(new Intent(this, EditRoutineActivity.class)));

        importLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onArchivoSeleccionado);
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        boolean vacio = dataManager.getDataStore().rutinas.isEmpty();
        textEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(vacio ? View.GONE : View.VISIBLE);
        actualizarTarjetaSesionEnCurso();
        actualizarTarjetaRecordatorioFisico();
    }

    private void mostrarSugerenciaRegistroFisico() {
        new AlertDialog.Builder(this)
                .setTitle("Registra tu físico")
                .setMessage("Registra tu peso, altura y medidas corporales para calcular tu IMC y llevar seguimiento de tu progreso. Podrás actualizarlas cada mes.")
                .setPositiveButton("Registrar ahora", (dialog, which) ->
                        startActivity(new Intent(this, PhysicalProfileActivity.class)))
                .setNegativeButton("Más tarde", null)
                .show();
    }

    private void actualizarTarjetaRecordatorioFisico() {
        View card = findViewById(R.id.card_recordatorio_fisico);
        List<RegistroFisico> historial = dataManager.getDataStore().historialFisico;
        boolean necesitaActualizar = PerfilFisicoUtil.necesitaActualizacion(historial);
        card.setVisibility(necesitaActualizar ? View.VISIBLE : View.GONE);
        if (necesitaActualizar) {
            TextView textDetalle = findViewById(R.id.text_recordatorio_fisico_detalle);
            textDetalle.setText(historial.isEmpty()
                    ? "Aún no has registrado tus medidas corporales."
                    : "Ya pasó un mes desde tu último registro físico.");
            findViewById(R.id.button_registrar_fisico).setOnClickListener(v ->
                    startActivity(new Intent(this, PhysicalProfileActivity.class)));
        }
    }

    private void actualizarTarjetaSesionEnCurso() {
        View card = findViewById(R.id.card_sesion_en_curso);
        Sesion enProgreso = dataManager.getDataStore().sesionEnProgreso;
        if (enProgreso == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        long minutos = Duration.between(LocalDateTime.parse(enProgreso.fechaHoraInicio), LocalDateTime.now()).toMinutes();
        TextView textDetalle = findViewById(R.id.text_sesion_en_curso_detalle);
        textDetalle.setText(enProgreso.rutinaNombre + " · " + minutos + " min");
        findViewById(R.id.button_continuar_sesion).setOnClickListener(v -> continuarSesionEnCurso());
    }

    private void continuarSesionEnCurso() {
        Sesion enProgreso = dataManager.getDataStore().sesionEnProgreso;
        if (enProgreso == null) {
            return;
        }
        Intent intent = new Intent(this, ActiveSessionActivity.class);
        intent.putExtra(EXTRA_RUTINA_ID, enProgreso.rutinaId);
        intent.putExtra(ActiveSessionActivity.EXTRA_RESUMIR, true);
        startActivity(intent);
    }

    private void descartarSesionEnCurso() {
        dataManager.getDataStore().sesionEnProgreso = null;
        dataManager.save();
        SesionTrackingService.detener(this);
        actualizarTarjetaSesionEnCurso();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_routine_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_historial) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        }
        if (id == R.id.action_estadisticas) {
            startActivity(new Intent(this, StatsActivity.class));
            return true;
        }
        if (id == R.id.action_mi_fisico) {
            startActivity(new Intent(this, PhysicalHistoryActivity.class));
            return true;
        }
        if (id == R.id.action_exportar) {
            exportarDatos();
            return true;
        }
        if (id == R.id.action_importar) {
            importLauncher.launch(new String[]{"application/json", "text/plain", "application/octet-stream", "*/*"});
            return true;
        }
        if (id == R.id.action_feedback) {
            abrirFormularioFeedback();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void abrirFormularioFeedback() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(FEEDBACK_URL)));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir el formulario", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportarDatos() {
        try {
            File archivo = dataManager.exportarComoArchivo();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", archivo);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Exportar datos de IronQuest"));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void onArchivoSeleccionado(Uri uri) {
        if (uri == null) {
            return;
        }
        DataStore importado;
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new java.io.IOException("No se pudo abrir el archivo");
            }
            importado = dataManager.leerDataStoreDesde(inputStream);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo leer el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        int rutinasNuevas = importado.rutinas.size();
        int sesionesNuevas = importado.sesiones.size();
        new AlertDialog.Builder(this)
                .setTitle("Importar datos")
                .setMessage("Esto reemplazará tus rutinas, ejercicios y sesiones actuales por los del archivo (" +
                        rutinasNuevas + " rutinas, " + sesionesNuevas + " sesiones). ¿Continuar?")
                .setPositiveButton("Reemplazar", (dialog, which) -> {
                    dataManager.reemplazarTodo(importado);
                    adapter.notifyDataSetChanged();
                    boolean vacio = dataManager.getDataStore().rutinas.isEmpty();
                    textEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(vacio ? View.GONE : View.VISIBLE);
                    Toast.makeText(this, "Datos importados correctamente", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onRutinaClick(Rutina rutina) {
        Sesion enProgreso = dataManager.getDataStore().sesionEnProgreso;
        if (enProgreso != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Ya tienes un entrenamiento en curso")
                    .setMessage("Tienes una sesión de \"" + enProgreso.rutinaNombre + "\" sin finalizar. ¿Qué quieres hacer?")
                    .setPositiveButton("Continuar la actual", (dialog, which) -> continuarSesionEnCurso())
                    .setNegativeButton("Descartar y empezar nueva", (dialog, which) -> {
                        descartarSesionEnCurso();
                        iniciarSesionNueva(rutina);
                    })
                    .setNeutralButton("Cancelar", null)
                    .show();
            return;
        }
        iniciarSesionNueva(rutina);
    }

    private void iniciarSesionNueva(Rutina rutina) {
        Intent intent = new Intent(this, ActiveSessionActivity.class);
        intent.putExtra(EXTRA_RUTINA_ID, rutina.id);
        startActivity(intent);
    }

    @Override
    public void onEditarClick(Rutina rutina) {
        Intent intent = new Intent(this, EditRoutineActivity.class);
        intent.putExtra(EXTRA_RUTINA_ID, rutina.id);
        startActivity(intent);
    }
}
