package com.ironquest.mvp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ironquest.mvp.BuildConfig;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.data.VersionChecker;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Usuario;
import com.google.firebase.auth.FirebaseUser;
import com.ironquest.mvp.data.AuthManager;
import com.ironquest.mvp.model.RutinaCompartida;

import java.io.File;
import java.io.InputStream;

/**
 * Contenedor de las cuatro pestañas principales (Inicio, Entrenar, Estadísticas, Metas).
 * Es dueño del toolbar, del menú de nivel app y de la verja de autenticación; las pantallas
 * de detalle siguen siendo Activities que los fragments lanzan con un Intent.
 */
public class MainActivity extends BaseActivity {

    public static final String EXTRA_SUGERIR_FISICO = "extra_sugerir_fisico";
    public static final String EXTRA_TAB_INICIAL = "extra_tab_inicial";

    private static final String ESTADO_TAB = "estado_tab";
    private static final String FEEDBACK_URL =
            "https://docs.google.com/forms/d/e/1FAIpQLSfO6Hd6-_l30bv5td8t-mhByoRCZwt4cvNqig72Vf1QI5yZEg/viewform?usp=header";

    /**
     * Pestaña de arranque. Apunta a Entrenar mientras Inicio siga siendo un marcador de
     * posición: abrir la app en un "próximamente" sería peor que abrirla en algo funcional.
     * Volver a {@code R.id.tab_inicio} cuando HomeFragment tenga contenido real.
     */
    private static final int TAB_POR_DEFECTO = R.id.tab_entrenar;

    private DataManager dataManager;
    private BottomNavigationView bottomNav;
    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<String[]> importRutinaLauncher;
    private int tabActual = TAB_POR_DEFECTO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VersionChecker.getInstance().verificar(BuildConfig.VERSION_CODE, resultado -> {
            if (!isFinishing() && !isDestroyed()) {
                mostrarDialogoVersion(resultado);
            }
        });

        dataManager = DataManager.getInstance(this);
        Usuario usuario = dataManager.getDataStore().getUsuario();
        FirebaseUser firebaseUser = AuthManager.getInstance().usuarioActual();
        boolean sesionValida = usuario != null && firebaseUser != null
                && firebaseUser.getUid().equals(usuario.getFirebaseUid());
        if (!sesionValida) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        configurarToolbar(R.id.toolbar, null, false);

        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onArchivoSeleccionado);
        importRutinaLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onArchivoRutinaSeleccionado);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            mostrarTab(item.getItemId());
            return true;
        });

        if (savedInstanceState != null) {
            tabActual = savedInstanceState.getInt(ESTADO_TAB, TAB_POR_DEFECTO);
        } else {
            tabActual = getIntent().getIntExtra(EXTRA_TAB_INICIAL, TAB_POR_DEFECTO);
        }

        // setSelectedItemId no dispara el listener si el id no cambió, así que la primera
        // pintada (y la que sigue a una rotación) hay que hacerla a mano.
        bottomNav.setSelectedItemId(tabActual);
        mostrarTab(tabActual);

        if (getIntent().getBooleanExtra(EXTRA_SUGERIR_FISICO, false)) {
            mostrarSugerenciaRegistroFisico();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_TAB, tabActual);
    }

    private void mostrarDialogoVersion(VersionChecker.Resultado resultado) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(resultado.bloqueoDuro ? "Actualización requerida" : "Nueva versión disponible")
                .setMessage((resultado.bloqueoDuro
                        ? "Necesitas actualizar Titan Score para seguir usándolo."
                        : "Hay una nueva versión de Titan Score disponible (" + resultado.latestVersionName + ").")
                        + (resultado.notas != null && !resultado.notas.isEmpty() ? "\n\n" + resultado.notas : ""))
                .setPositiveButton("Actualizar", (dialog, which) ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(resultado.urlDescarga))))
                .setCancelable(!resultado.bloqueoDuro);
        if (!resultado.bloqueoDuro) {
            builder.setNegativeButton("Ahora no", null);
        }
        builder.show();
    }

    /** Permite que un fragment mande al usuario a otra pestaña sin duplicar su lógica. */
    public void seleccionarTab(int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    private void mostrarTab(int itemId) {
        tabActual = itemId;

        Fragment fragment;
        String titulo;
        if (itemId == R.id.tab_entrenar) {
            fragment = new RoutineListFragment();
            titulo = "Entrenar";
        } else if (itemId == R.id.tab_estadisticas) {
            fragment = new StatsFragment();
            titulo = "Estadísticas";
        } else if (itemId == R.id.tab_metas) {
            fragment = MetasFragment.crear("Metas — próximamente");
            titulo = "Metas";
        } else {
            fragment = new HomeFragment();
            titulo = "Titan Score";
        }

        setTitle(titulo);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_historial) {
            startActivity(new Intent(this, HistoryActivity.class));
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
        if (id == R.id.action_importar_rutina) {
            importRutinaLauncher.launch(new String[]{"application/json", "text/plain", "application/octet-stream", "*/*"});
            return true;
        }
        if (id == R.id.action_feedback) {
            abrirFormularioFeedback();
            return true;
        }
        if (id == R.id.action_cerrar_sesion) {
            confirmarCerrarSesion();
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

    private void confirmarCerrarSesion() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres cerrar sesión?")
                .setPositiveButton("Cerrar sesión", (dialog, which) -> cerrarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void cerrarSesion() {
        AuthManager.getInstance().cerrarSesion();
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

        int rutinasNuevas = importado.getRutinas().size();
        int sesionesNuevas = importado.getSesiones().size();
        new AlertDialog.Builder(this)
                .setTitle("Importar datos")
                .setMessage("Esto reemplazará tus rutinas, ejercicios y sesiones actuales por los del archivo (" +
                        rutinasNuevas + " rutinas, " + sesionesNuevas + " sesiones). ¿Continuar?")
                .setPositiveButton("Reemplazar", (dialog, which) -> {
                    dataManager.reemplazarTodo(importado);
                    // Volver a montar la pestaña actual es lo que refresca la lista.
                    mostrarTab(tabActual);
                    Toast.makeText(this, "Datos importados correctamente", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void onArchivoRutinaSeleccionado(Uri uri) {
        if (uri == null) {
            return;
        }
        RutinaCompartida paquete;
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new java.io.IOException("No se pudo abrir el archivo");
            }
            paquete = dataManager.leerRutinaCompartidaDesde(inputStream);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo leer el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        int cantidadEjercicios = paquete.getRutina().getCantidadEjercicios();
        new AlertDialog.Builder(this)
                .setTitle("Importar rutina")
                .setMessage("¿Agregar la rutina \"" + paquete.getRutina().getNombre() + "\" con "
                        + cantidadEjercicios + " ejercicios? Tus rutinas actuales no se modifican.")
                .setPositiveButton("Agregar", (dialog, which) -> {
                    dataManager.importarRutina(paquete);
                    mostrarTab(tabActual);
                    Toast.makeText(this, "Rutina agregada correctamente", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
