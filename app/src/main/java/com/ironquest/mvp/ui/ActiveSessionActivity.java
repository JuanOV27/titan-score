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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

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
import com.ironquest.mvp.util.progresion.EstrategiaProgresion;
import com.ironquest.mvp.util.progresion.Sugerencia;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class ActiveSessionActivity extends BaseActivity {

    public static final String EXTRA_RUTINA_ID = "extra_rutina_id";
    public static final String EXTRA_RESUMIR = "extra_resumir";

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
    private Map<EjercicioSesion, Sugerencia> sugerencias;
    private ViewPager2 pagerEjercicios;
    private EjercicioSesionPagerAdapter pagerAdapter;
    private TextView textIndicadorPagina;

    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_session);

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

        configurarToolbar(R.id.toolbar, null, false);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();

        catalogoPorId = new HashMap<>();
        for (Ejercicio ejercicio : dataStore.getEjercicios()) {
            catalogoPorId.put(ejercicio.getId(), ejercicio);
        }

        pagerEjercicios = findViewById(R.id.pager_ejercicios_sesion);
        textIndicadorPagina = findViewById(R.id.text_indicador_pagina_sesion);
        TextView textHoraInicio = findViewById(R.id.text_hora_inicio);

        boolean resumir = getIntent().getBooleanExtra(EXTRA_RESUMIR, false);
        if (resumir && dataStore.getSesionEnProgreso() != null) {
            sesionActual = dataStore.getSesionEnProgreso();
            inicioSesion = LocalDateTime.parse(sesionActual.getFechaHoraInicio());
            setTitle(sesionActual.getRutinaNombre());
            textHoraInicio.setText("Inicio: " + inicioSesion.format(FORMATO_HORA));
            // Resumir nunca muestra explicación de progresión — se preserva ese comportamiento.
            sugerencias = new IdentityHashMap<>();
        } else {
            String rutinaId = getIntent().getStringExtra(EXTRA_RUTINA_ID);
            Rutina rutina = dataStore.buscarRutina(rutinaId);
            if (rutina == null) {
                Toast.makeText(this, "No se encontró la rutina", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            inicioSesion = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            sesionActual = new Sesion(dataManager.newId("s"), rutina.getId(), rutina.getNombre(), inicioSesion.toString());
            sugerencias = new IdentityHashMap<>();

            setTitle(rutina.getNombre());
            textHoraInicio.setText("Inicio: " + inicioSesion.format(FORMATO_HORA));

            double volumenPlaneado = 0;
            for (RutinaEjercicio re : rutina.getEjercicios()) {
                Sugerencia sugerencia = EstrategiaProgresion.para(re.getEsquemaProgresion())
                        .sugerir(re, dataStore.getSesiones());
                EjercicioSesion ejercicioSesion = new EjercicioSesion(re.getEjercicioId());
                for (int i = 1; i <= re.getSeries(); i++) {
                    ejercicioSesion.agregarSerie(
                            new SerieSesion(i, sugerencia.getPeso(), sugerencia.getRepeticiones(), true));
                }
                sesionActual.agregarEjercicio(ejercicioSesion);
                sugerencias.put(ejercicioSesion, sugerencia);
                // Todas las series nacen marcadas como completadas, así que el volumen del bloque
                // recién creado es exactamente el plan de este ejercicio.
                volumenPlaneado += ejercicioSesion.calcularVolumen();
            }
            sesionActual.setVolumenPlaneado(volumenPlaneado);
        }

        pagerAdapter = new EjercicioSesionPagerAdapter(
                sesionActual.getEjercicios(), catalogoPorId, sugerencias,
                new EjercicioSesionPagerAdapter.Listener() {
                    @Override
                    public void onCambiarEjercicio(int position) {
                        cambiarEjercicio(position);
                    }

                    @Override
                    public void onEliminarEjercicio(int position) {
                        confirmarEliminarEjercicio(position);
                    }

                    @Override
                    public void onProgresoModificado() {
                        guardarProgreso();
                    }
                });
        pagerEjercicios.setAdapter(pagerAdapter);
        pagerEjercicios.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                actualizarIndicadorPagina();
            }
        });
        actualizarIndicadorPagina();

        guardarProgreso();
        iniciarServicioSeguimiento();

        configurarBotonFinalizar();
        findViewById(R.id.button_iniciar_descanso).setOnClickListener(v -> mostrarTemporizadorDescanso());
        findViewById(R.id.button_agregar_ejercicio_sesion).setOnClickListener(v ->
                EjercicioPicker.mostrar(this, dataManager, dataStore, ejercicio -> {
                    catalogoPorId.putIfAbsent(ejercicio.getId(), ejercicio);
                    EjercicioSesion nuevo = new EjercicioSesion(ejercicio.getId());
                    for (int i = 1; i <= 3; i++) {
                        nuevo.agregarSerie(new SerieSesion(i, 0.0, 10, true));
                    }
                    sesionActual.agregarEjercicio(nuevo);
                    int posicion = sesionActual.getEjercicios().size() - 1;
                    pagerAdapter.notifyItemInserted(posicion);
                    pagerEjercicios.setCurrentItem(posicion, true);
                    actualizarIndicadorPagina();
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
        dataStore.setSesionEnProgreso(null);
        dataManager.save();
        SesionTrackingService.detener(this);
        Toast.makeText(this, "Sesión descartada", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void guardarProgreso() {
        dataStore.setSesionEnProgreso(sesionActual);
        dataManager.save();
    }

    private void iniciarServicioSeguimiento() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        SesionTrackingService.iniciar(this, sesionActual.getRutinaNombre(), sesionActual.getFechaHoraInicio());
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

    private void cambiarEjercicio(int position) {
        if (position < 0 || position >= sesionActual.getEjercicios().size()) {
            return;
        }
        EjercicioSesion ejercicioSesion = sesionActual.getEjercicios().get(position);
        EjercicioPicker.mostrar(this, dataManager, dataStore, nuevoEjercicio -> {
            catalogoPorId.putIfAbsent(nuevoEjercicio.getId(), nuevoEjercicio);
            ejercicioSesion.setEjercicioId(nuevoEjercicio.getId());
            // La explicación de progresión pertenecía al ejercicio planeado; tras un swap se
            // descarta para no mostrar una sugerencia de otro ejercicio.
            sugerencias.remove(ejercicioSesion);
            pagerAdapter.notifyItemChanged(position);
            guardarProgreso();
        });
    }

    private void confirmarEliminarEjercicio(int position) {
        if (position < 0 || position >= sesionActual.getEjercicios().size()) {
            return;
        }
        EjercicioSesion ejercicioSesion = sesionActual.getEjercicios().get(position);
        new AlertDialog.Builder(this)
                .setTitle("Eliminar ejercicio")
                .setMessage("¿Quitar este ejercicio de la sesión?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    sesionActual.getEjercicios().remove(ejercicioSesion);
                    sugerencias.remove(ejercicioSesion);
                    pagerAdapter.notifyItemRemoved(position);
                    actualizarIndicadorPagina();
                    guardarProgreso();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarIndicadorPagina() {
        int total = sesionActual.getEjercicios().size();
        if (total == 0) {
            textIndicadorPagina.setText("Sin ejercicios");
            return;
        }
        int actual = Math.min(pagerEjercicios.getCurrentItem(), total - 1);
        textIndicadorPagina.setText("Ejercicio " + (actual + 1) + " de " + total);
    }

    private void mostrarTemporizadorDescanso() {
        dialogDescansoActivo = new RestTimerDialog(this, duracionDescansoMillis, millis -> duracionDescansoMillis = millis);
        dialogDescansoActivo.setOnDismissListener(d -> dialogDescansoActivo = null);
        dialogDescansoActivo.show();
    }

    private void finalizarSesion() {
        sesionFinalizada = true;
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        sesionActual.finalizar(ahora.toString());

        long duracionMinutos = Duration.between(inicioSesion, ahora).toMinutes();

        dataStore.getSesiones().add(sesionActual);
        dataStore.setSesionEnProgreso(null);
        dataManager.save();

        int racha = EstadisticasUtil.calcularRachaDias(dataStore.getSesiones());

        SesionTrackingService.detener(this);

        Intent intent = new Intent(this, SessionSummaryActivity.class);
        intent.putExtra(SessionSummaryActivity.EXTRA_RUTINA_NOMBRE, sesionActual.getRutinaNombre());
        intent.putExtra(SessionSummaryActivity.EXTRA_DURACION_MINUTOS, duracionMinutos);
        intent.putExtra(SessionSummaryActivity.EXTRA_RACHA_DIAS, racha);
        intent.putExtra(SessionSummaryActivity.EXTRA_PORCENTAJE, sesionActual.getPorcentajeCumplimiento());
        startActivity(intent);
        finish();
    }
}
