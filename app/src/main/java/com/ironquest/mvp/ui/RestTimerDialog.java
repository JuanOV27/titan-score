package com.ironquest.mvp.ui;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.ironquest.mvp.R;

import java.util.Locale;
import java.util.Random;

public class RestTimerDialog extends Dialog {

    public interface OnDurationChangeListener {
        void onDurationChanged(long millis);
    }

    private static final long MIN_DURATION_MS = 15_000L;
    private static final long MAX_DURATION_MS = 600_000L;
    private static final long STEP_MS = 15_000L;

    private final long initialDurationMillis;
    private final OnDurationChangeListener durationChangeListener;
    private final Random random = new Random();

    private CountDownTimer countDownTimer;
    private long totalDurationMillis;
    private long remainingMillis;
    private boolean running;
    private boolean finished;
    private int puntos = 0;
    private ToneGenerator toneGenerator;

    private TextView textCountdown;
    private TextView textEstado;
    private TextView textPuntos;
    private MaterialButton buttonIniciarPausar;
    private FrameLayout gameArea;
    private MaterialButton buttonTarget;

    public RestTimerDialog(Context context, long initialDurationMillis, OnDurationChangeListener listener) {
        super(context);
        this.initialDurationMillis = initialDurationMillis;
        this.durationChangeListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_rest_timer);
        setCancelable(true);

        totalDurationMillis = initialDurationMillis;
        remainingMillis = initialDurationMillis;

        bindViews();
        refrescarUI();
        iniciar();
    }

    /**
     * Reaplica el layout (permitiendo que Android elija la variante land/port) y
     * vuelve a enlazar las vistas sin tocar el estado del conteo, que vive en este
     * objeto y no en la ventana.
     */
    public void actualizarOrientacion() {
        if (!isShowing()) {
            return;
        }
        setContentView(R.layout.dialog_rest_timer);
        bindViews();
        refrescarUI();
    }

    private void bindViews() {
        textCountdown = findViewById(R.id.text_countdown);
        textEstado = findViewById(R.id.text_estado_descanso);
        textPuntos = findViewById(R.id.text_puntos);
        buttonIniciarPausar = findViewById(R.id.button_iniciar_pausar);
        gameArea = findViewById(R.id.game_area);
        buttonTarget = findViewById(R.id.button_target);

        findViewById(R.id.button_preset_30).setOnClickListener(v -> setDuration(30_000L));
        findViewById(R.id.button_preset_60).setOnClickListener(v -> setDuration(60_000L));
        findViewById(R.id.button_preset_90).setOnClickListener(v -> setDuration(90_000L));
        findViewById(R.id.button_preset_120).setOnClickListener(v -> setDuration(120_000L));
        findViewById(R.id.button_menos_15).setOnClickListener(v -> ajustarDuracion(-STEP_MS));
        findViewById(R.id.button_mas_15).setOnClickListener(v -> ajustarDuracion(STEP_MS));
        buttonIniciarPausar.setOnClickListener(v -> alternarIniciarPausar());
        findViewById(R.id.button_saltar_descanso).setOnClickListener(v -> dismiss());

        buttonTarget.setOnClickListener(v -> {
            puntos++;
            textPuntos.setText("Puntos: " + puntos);
            reposicionarObjetivo();
        });
    }

    private void refrescarUI() {
        actualizarTextoCountdown();
        textPuntos.setText("Puntos: " + puntos);
        if (finished) {
            textEstado.setText("¡Descanso terminado! 💪");
            buttonIniciarPausar.setText("Cerrar");
        } else {
            textEstado.setText("");
            buttonIniciarPausar.setText(running ? "Pausar" : "Iniciar");
        }
    }

    private void setDuration(long millis) {
        boolean estabaCorriendo = running;
        pausar();
        totalDurationMillis = millis;
        remainingMillis = millis;
        finished = false;
        textEstado.setText("");
        actualizarTextoCountdown();
        notificarDuracion();
        if (estabaCorriendo) {
            iniciar();
        }
    }

    private void ajustarDuracion(long deltaMillis) {
        long base = running ? remainingMillis : totalDurationMillis;
        long nuevo = Math.max(MIN_DURATION_MS, Math.min(MAX_DURATION_MS, base + deltaMillis));
        boolean estabaCorriendo = running;
        pausar();
        totalDurationMillis = nuevo;
        remainingMillis = nuevo;
        finished = false;
        actualizarTextoCountdown();
        notificarDuracion();
        if (estabaCorriendo) {
            iniciar();
        }
    }

    private void notificarDuracion() {
        if (durationChangeListener != null) {
            durationChangeListener.onDurationChanged(totalDurationMillis);
        }
    }

    private void alternarIniciarPausar() {
        if (finished) {
            dismiss();
            return;
        }
        if (running) {
            pausar();
        } else {
            iniciar();
        }
    }

    private void iniciar() {
        running = true;
        buttonIniciarPausar.setText("Pausar");
        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisRestantes) {
                remainingMillis = millisRestantes;
                actualizarTextoCountdown();
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                running = false;
                finished = true;
                actualizarTextoCountdown();
                textEstado.setText("¡Descanso terminado! 💪");
                buttonIniciarPausar.setText("Cerrar");
                avisarFinDescanso();
            }
        }.start();
    }

    private void pausar() {
        running = false;
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        if (!finished) {
            buttonIniciarPausar.setText("Iniciar");
        }
    }

    private void actualizarTextoCountdown() {
        long totalSegundos = (remainingMillis + 999) / 1000;
        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;
        textCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos));
    }

    private void reposicionarObjetivo() {
        gameArea.post(() -> {
            int maxX = gameArea.getWidth() - buttonTarget.getWidth();
            int maxY = gameArea.getHeight() - buttonTarget.getHeight();
            if (maxX <= 0 || maxY <= 0) {
                return;
            }
            float nuevaX = random.nextInt(maxX);
            float nuevaY = random.nextInt(maxY);
            buttonTarget.animate().x(nuevaX).y(nuevaY).setDuration(120).start();
        });
    }

    private void avisarFinDescanso() {
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 200, 100, 200, 100, 300}, -1));
        }
        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90);
            }
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 400);
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void dismiss() {
        pausar();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
        super.dismiss();
    }
}
