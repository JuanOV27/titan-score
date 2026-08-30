package com.ironquest.mvp.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.service.SesionTrackingService;
import com.ironquest.mvp.util.EstadisticasUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class ActiveSessionActivity extends AppCompatActivity {

    public static final String EXTRA_RUTINA_ID = "extra_rutina_id";
    public static final String EXTRA_RESUMIR = "extra_resumir";

    private static final double PASO_PESO = 2.5;
    private static final long HOLD_FINALIZAR_MS = 3000L;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private DataManager dataManager;
    private DataStore dataStore;
    private Sesion sesionActual;
    private LocalDateTime inicioSesion;
    private long duracionDescansoMillis = 60_000L;
    private RestTimerDialog dialogDescansoActivo;
    private boolean sesionFinalizada;

    private Map<String, Ejercicio> catalogoPorId;
    private LinearLayout containerEjercicios;
    private LayoutInflater inflater;

    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_session);

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();

        catalogoPorId = new HashMap<>();
        for (Ejercicio ejercicio : dataStore.ejercicios) {
            catalogoPorId.put(ejercicio.id, ejercicio);
        }

        containerEjercicios = findViewById(R.id.container_ejercicios);
        inflater = LayoutInflater.from(this);
        TextView textHoraInicio = findViewById(R.id.text_hora_inicio);

        boolean resumir = getIntent().getBooleanExtra(EXTRA_RESUMIR, false);
        if (resumir && dataStore.sesionEnProgreso != null) {
            sesionActual = dataStore.sesionEnProgreso;
            inicioSesion = LocalDateTime.parse(sesionActual.fechaHoraInicio);
            setTitle(sesionActual.rutinaNombre);
            textHoraInicio.setText("Inicio: " + inicioSesion.format(FORMATO_HORA));
            for (EjercicioSesion ejercicioSesion : sesionActual.ejercicios) {
                agregarBloqueEjercicio(ejercicioSesion);
            }
        } else {
            String rutinaId = getIntent().getStringExtra(EXTRA_RUTINA_ID);
            Rutina rutina = buscarRutina(rutinaId);
            if (rutina == null) {
                Toast.makeText(this, "No se encontró la rutina", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            inicioSesion = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            sesionActual = new Sesion(dataManager.newId("s"), rutina.id, rutina.nombre, inicioSesion.toString());

            setTitle(rutina.nombre);
            textHoraInicio.setText("Inicio: " + inicioSesion.format(FORMATO_HORA));

            double volumenPlaneado = 0;
            for (RutinaEjercicio re : rutina.ejercicios) {
                EjercicioSesion ejercicioSesion = new EjercicioSesion(re.ejercicioId);
                for (int i = 1; i <= re.series; i++) {
                    ejercicioSesion.series.add(new SerieSesion(i, re.peso, re.repeticiones, true));
                }
                sesionActual.ejercicios.add(ejercicioSesion);
                agregarBloqueEjercicio(ejercicioSesion);
                volumenPlaneado += unidadEsfuerzo(re.peso, re.repeticiones) * re.series;
            }
            sesionActual.volumenPlaneado = volumenPlaneado;
        }

        guardarProgreso();
        iniciarServicioSeguimiento();

        configurarBotonFinalizar();
        findViewById(R.id.button_iniciar_descanso).setOnClickListener(v -> mostrarTemporizadorDescanso());
        findViewById(R.id.button_agregar_ejercicio_sesion).setOnClickListener(v ->
                EjercicioPicker.mostrar(this, dataManager, dataStore, ejercicio -> {
                    catalogoPorId.putIfAbsent(ejercicio.id, ejercicio);
                    EjercicioSesion nuevo = new EjercicioSesion(ejercicio.id);
                    for (int i = 1; i <= 3; i++) {
                        nuevo.series.add(new SerieSesion(i, 0.0, 10, true));
                    }
                    sesionActual.ejercicios.add(nuevo);
                    agregarBloqueEjercicio(nuevo);
                    guardarProgreso();
                }));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!sesionFinalizada) {
            guardarProgreso();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (dialogDescansoActivo != null && dialogDescansoActivo.isShowing()) {
            dialogDescansoActivo.actualizarOrientacion();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_active_session, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_descartar_sesion) {
            confirmarDescartarSesion();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmarDescartarSesion() {
        new AlertDialog.Builder(this)
                .setTitle("Descartar sesión")
                .setMessage("¿Estás seguro de que quieres descartar esta sesión de entrenamiento? Al descartarla, no quedará registrada en tu historial.")
                .setPositiveButton("Descartar", (dialog, which) -> descartarSesion())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void descartarSesion() {
        sesionFinalizada = true;
        if (dialogDescansoActivo != null && dialogDescansoActivo.isShowing()) {
            dialogDescansoActivo.dismiss();
        }
        dataStore.sesionEnProgreso = null;
        dataManager.save();
        SesionTrackingService.detener(this);
        Toast.makeText(this, "Sesión descartada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void guardarProgreso() {
        dataStore.sesionEnProgreso = sesionActual;
        dataManager.save();
    }

    private void iniciarServicioSeguimiento() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        SesionTrackingService.iniciar(this, sesionActual.rutinaNombre, sesionActual.fechaHoraInicio);
    }

    private void configurarBotonFinalizar() {
        MaterialButton botonFinalizar = findViewById(R.id.button_finalizar_sesion);
        LinearProgressIndicator progreso = findViewById(R.id.progress_finalizar_sesion);

        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(HOLD_FINALIZAR_MS);
        animator.addUpdateListener(a -> progreso.setProgress((int) a.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelado;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelado = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                boolean seCompleto = !cancelado;
                cancelado = false;
                if (seCompleto) {
                    vibrarConfirmacion();
                    finalizarSesion();
                }
            }
        });

        botonFinalizar.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    progreso.setProgress(0);
                    progreso.setVisibility(View.VISIBLE);
                    animator.start();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animator.cancel();
                    progreso.setProgress(0);
                    progreso.setVisibility(View.INVISIBLE);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void vibrarConfirmacion() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void agregarBloqueEjercicio(EjercicioSesion ejercicioSesion) {
        View block = inflater.inflate(R.layout.view_ejercicio_sesion_block, containerEjercicios, false);
        TextView nombre = block.findViewById(R.id.text_nombre_ejercicio_sesion);
        ImageButton botonCambiar = block.findViewById(R.id.button_cambiar_ejercicio);
        ImageButton botonEliminar = block.findViewById(R.id.button_eliminar_ejercicio_sesion);
        LinearLayout containerSeries = block.findViewById(R.id.container_series);

        actualizarNombreBloque(nombre, ejercicioSesion.ejercicioId);

        for (SerieSesion serie : ejercicioSesion.series) {
            containerSeries.addView(crearFilaSerie(inflater, containerSeries, serie));
        }

        MaterialButton botonAgregarSerie = block.findViewById(R.id.button_agregar_serie);
        botonAgregarSerie.setOnClickListener(v -> {
            SerieSesion ultima = ejercicioSesion.series.isEmpty()
                    ? new SerieSesion(0, 0.0, 10, true)
                    : ejercicioSesion.series.get(ejercicioSesion.series.size() - 1);
            SerieSesion nueva = new SerieSesion(ejercicioSesion.series.size() + 1, ultima.peso, ultima.repeticiones, true);
            ejercicioSesion.series.add(nueva);
            containerSeries.addView(crearFilaSerie(inflater, containerSeries, nueva));
            guardarProgreso();
        });

        botonCambiar.setOnClickListener(v -> EjercicioPicker.mostrar(this, dataManager, dataStore, nuevoEjercicio -> {
            catalogoPorId.putIfAbsent(nuevoEjercicio.id, nuevoEjercicio);
            ejercicioSesion.ejercicioId = nuevoEjercicio.id;
            actualizarNombreBloque(nombre, nuevoEjercicio.id);
            guardarProgreso();
        }));

        botonEliminar.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Eliminar ejercicio")
                .setMessage("¿Quitar este ejercicio de la sesión?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    sesionActual.ejercicios.remove(ejercicioSesion);
                    containerEjercicios.removeView(block);
                    guardarProgreso();
                })
                .setNegativeButton("Cancelar", null)
                .show());

        containerEjercicios.addView(block);
    }

    private void actualizarNombreBloque(TextView nombre, String ejercicioId) {
        Ejercicio ejercicio = catalogoPorId.get(ejercicioId);
        nombre.setText(ejercicio != null ? ejercicio.nombre : "Ejercicio");
    }

    private void mostrarTemporizadorDescanso() {
        dialogDescansoActivo = new RestTimerDialog(this, duracionDescansoMillis, millis -> duracionDescansoMillis = millis);
        dialogDescansoActivo.setOnDismissListener(d -> dialogDescansoActivo = null);
        dialogDescansoActivo.show();
    }

    private View crearFilaSerie(LayoutInflater inflater, LinearLayout parent, SerieSesion serie) {
        View row = inflater.inflate(R.layout.view_serie_sesion_row, parent, false);

        TextView textNumero = row.findViewById(R.id.text_numero_serie);
        TextView textPeso = row.findViewById(R.id.text_peso);
        TextView textReps = row.findViewById(R.id.text_reps);
        ImageButton botonPesoMenos = row.findViewById(R.id.button_peso_menos);
        ImageButton botonPesoMas = row.findViewById(R.id.button_peso_mas);
        ImageButton botonRepsMenos = row.findViewById(R.id.button_reps_menos);
        ImageButton botonRepsMas = row.findViewById(R.id.button_reps_mas);
        ImageButton botonCompletada = row.findViewById(R.id.button_completada);

        textNumero.setText("Serie " + serie.numero);
        textPeso.setText(formatearPeso(serie.peso));
        textReps.setText(String.valueOf(serie.repeticiones));
        actualizarIconoCompletada(botonCompletada, serie.completada);

        botonPesoMenos.setOnClickListener(v -> {
            serie.peso = Math.max(0, serie.peso - PASO_PESO);
            textPeso.setText(formatearPeso(serie.peso));
            guardarProgreso();
        });
        botonPesoMas.setOnClickListener(v -> {
            serie.peso += PASO_PESO;
            textPeso.setText(formatearPeso(serie.peso));
            guardarProgreso();
        });
        botonRepsMenos.setOnClickListener(v -> {
            serie.repeticiones = Math.max(0, serie.repeticiones - 1);
            textReps.setText(String.valueOf(serie.repeticiones));
            guardarProgreso();
        });
        botonRepsMas.setOnClickListener(v -> {
            serie.repeticiones += 1;
            textReps.setText(String.valueOf(serie.repeticiones));
            guardarProgreso();
        });
        botonCompletada.setOnClickListener(v -> {
            serie.completada = !serie.completada;
            actualizarIconoCompletada(botonCompletada, serie.completada);
            guardarProgreso();
        });

        return row;
    }

    private void actualizarIconoCompletada(ImageButton boton, boolean completada) {
        boton.setImageResource(completada
                ? android.R.drawable.checkbox_on_background
                : android.R.drawable.checkbox_off_background);
        boton.setContentDescription(completada
                ? "Marcar que no hiciste esta serie"
                : "Marcar que sí hiciste esta serie");
    }

    private String formatearPeso(double peso) {
        if (peso == Math.floor(peso)) {
            return String.valueOf((long) peso);
        }
        return String.valueOf(peso);
    }

    private static double unidadEsfuerzo(double peso, int repeticiones) {
        return peso > 0 ? peso * repeticiones : repeticiones;
    }

    private Rutina buscarRutina(String id) {
        if (id == null) {
            return null;
        }
        for (Rutina r : dataStore.rutinas) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    private void finalizarSesion() {
        sesionFinalizada = true;
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        sesionActual.fechaHoraFin = ahora.toString();

        double volumenReal = 0;
        for (EjercicioSesion ejercicioSesion : sesionActual.ejercicios) {
            for (SerieSesion serie : ejercicioSesion.series) {
                if (serie.completada) {
                    volumenReal += unidadEsfuerzo(serie.peso, serie.repeticiones);
                }
            }
        }
        sesionActual.volumenReal = volumenReal;

        int porcentaje;
        if (sesionActual.volumenPlaneado > 0) {
            porcentaje = (int) Math.round(volumenReal / sesionActual.volumenPlaneado * 100);
        } else {
            porcentaje = volumenReal > 0 ? 100 : 0;
        }
        sesionActual.porcentajeCumplimiento = Math.max(0, porcentaje);

        long duracionMinutos = Duration.between(inicioSesion, ahora).toMinutes();

        dataStore.sesiones.add(sesionActual);
        dataStore.sesionEnProgreso = null;
        dataManager.save();

        int racha = EstadisticasUtil.calcularRachaDias(dataStore.sesiones);

        SesionTrackingService.detener(this);

        Intent intent = new Intent(this, SessionSummaryActivity.class);
        intent.putExtra(SessionSummaryActivity.EXTRA_RUTINA_NOMBRE, sesionActual.rutinaNombre);
        intent.putExtra(SessionSummaryActivity.EXTRA_DURACION_MINUTOS, duracionMinutos);
        intent.putExtra(SessionSummaryActivity.EXTRA_RACHA_DIAS, racha);
        intent.putExtra(SessionSummaryActivity.EXTRA_PORCENTAJE, sesionActual.porcentajeCumplimiento);
        startActivity(intent);
        finish();
    }
}
