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
import com.ironquest.mvp.model.Rutina;

import java.io.File;
import java.io.InputStream;

public class RoutineListActivity extends AppCompatActivity implements RutinaAdapter.Listener {

    public static final String EXTRA_RUTINA_ID = "extra_rutina_id";
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
        setContentView(R.layout.activity_routine_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataManager = DataManager.getInstance(this);

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
