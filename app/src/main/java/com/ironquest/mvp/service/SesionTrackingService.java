package com.ironquest.mvp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ironquest.mvp.R;
import com.ironquest.mvp.ui.RoutineListActivity;

import java.time.Duration;
import java.time.LocalDateTime;

public class SesionTrackingService extends Service {

    private static final String EXTRA_RUTINA_NOMBRE = "extra_rutina_nombre";
    private static final String EXTRA_INICIO_ISO = "extra_inicio_iso";
    private static final String EXTRA_DESCANSO_FIN_EN = "extra_descanso_fin_en";

    private static final String ACTION_DESCANSO_ACTUALIZAR = "com.ironquest.mvp.action.DESCANSO_ACTUALIZAR";
    private static final String ACTION_DESCANSO_CANCELAR = "com.ironquest.mvp.action.DESCANSO_CANCELAR";
    private static final String ACTION_DESCANSO_FINALIZADO = "com.ironquest.mvp.action.DESCANSO_FINALIZADO";

    private static final String CANAL_PROGRESO = "sesion_progreso";
    private static final String CANAL_RECORDATORIO = "sesion_recordatorio";
    private static final String CANAL_DESCANSO_FIN = "descanso_fin";
    private static final int NOTIF_ID_PROGRESO = 1001;
    private static final int NOTIF_ID_RECORDATORIO = 1002;
    private static final int NOTIF_ID_DESCANSO = 1003;
    private static final long INTERVALO_ACTUALIZACION_MS = 60_000L;
    private static final long UMBRAL_RECORDATORIO_MS = 60L * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String rutinaNombre = "tu rutina";
    private LocalDateTime inicio;
    private boolean recordatorioEnviado;

    private final Runnable actualizarRunnable = new Runnable() {
        @Override
        public void run() {
            actualizarNotificacionProgreso();
            handler.postDelayed(this, INTERVALO_ACTUALIZACION_MS);
        }
    };

    public static void iniciar(Context context, String rutinaNombre, String inicioIso) {
        Intent intent = new Intent(context, SesionTrackingService.class);
        intent.putExtra(EXTRA_RUTINA_NOMBRE, rutinaNombre);
        intent.putExtra(EXTRA_INICIO_ISO, inicioIso);
        context.startForegroundService(intent);
    }

    public static void detener(Context context) {
        context.stopService(new Intent(context, SesionTrackingService.class));
    }

    public static void actualizarDescanso(Context context, long finEnMillis) {
        Intent intent = new Intent(context, SesionTrackingService.class);
        intent.setAction(ACTION_DESCANSO_ACTUALIZAR);
        intent.putExtra(EXTRA_DESCANSO_FIN_EN, finEnMillis);
        context.startForegroundService(intent);
    }

    public static void cancelarDescanso(Context context) {
        Intent intent = new Intent(context, SesionTrackingService.class);
        intent.setAction(ACTION_DESCANSO_CANCELAR);
        context.startForegroundService(intent);
    }

    public static void finalizarDescanso(Context context) {
        Intent intent = new Intent(context, SesionTrackingService.class);
        intent.setAction(ACTION_DESCANSO_FINALIZADO);
        context.startForegroundService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        crearCanales();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (action == null) {
            if (intent != null) {
                String nombre = intent.getStringExtra(EXTRA_RUTINA_NOMBRE);
                if (nombre != null) {
                    rutinaNombre = nombre;
                }
                String inicioIso = intent.getStringExtra(EXTRA_INICIO_ISO);
                if (inicioIso != null) {
                    inicio = LocalDateTime.parse(inicioIso);
                }
            }
            if (inicio == null) {
                inicio = LocalDateTime.now();
            }
        }

        startForeground(NOTIF_ID_PROGRESO, construirNotificacionProgreso());
        handler.removeCallbacks(actualizarRunnable);
        handler.postDelayed(actualizarRunnable, INTERVALO_ACTUALIZACION_MS);

        if (ACTION_DESCANSO_ACTUALIZAR.equals(action) && intent != null) {
            long finEn = intent.getLongExtra(EXTRA_DESCANSO_FIN_EN, System.currentTimeMillis());
            mostrarNotificacionDescanso(finEn);
        } else if (ACTION_DESCANSO_CANCELAR.equals(action)) {
            cancelarNotificacionDescanso();
        } else if (ACTION_DESCANSO_FINALIZADO.equals(action)) {
            notificarDescansoFinalizado();
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(actualizarRunnable);
        super.onDestroy();
    }

    private void actualizarNotificacionProgreso() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIF_ID_PROGRESO, construirNotificacionProgreso());
        }

        long minutos = Duration.between(inicio, LocalDateTime.now()).toMinutes();
        if (!recordatorioEnviado && minutos * 60_000L >= UMBRAL_RECORDATORIO_MS) {
            recordatorioEnviado = true;
            enviarRecordatorioUnaHora();
        }
    }

    private Notification construirNotificacionProgreso() {
        long minutos = Duration.between(inicio, LocalDateTime.now()).toMinutes();
        String texto = rutinaNombre + " · " + minutos + " min entrenando";

        return new NotificationCompat.Builder(this, CANAL_PROGRESO)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Entrenamiento en curso")
                .setContentText(texto)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntentAbrirSesion())
                .build();
    }

    private void mostrarNotificacionDescanso(long finEnMillis) {
        Notification notificacion = new NotificationCompat.Builder(this, CANAL_PROGRESO)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Descanso en curso")
                .setContentText("Toca para volver a tu sesión")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(finEnMillis)
                .setContentIntent(pendingIntentAbrirSesion())
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIF_ID_DESCANSO, notificacion);
        }
    }

    private void cancelarNotificacionDescanso() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(NOTIF_ID_DESCANSO);
        }
    }

    private void notificarDescansoFinalizado() {
        Notification notificacion = new NotificationCompat.Builder(this, CANAL_DESCANSO_FIN)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("¡Descanso terminado! 💪")
                .setContentText("Toca para volver a tu sesión")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntentAbrirSesion())
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIF_ID_DESCANSO, notificacion);
        }
    }

    private void enviarRecordatorioUnaHora() {
        Notification notificacion = new NotificationCompat.Builder(this, CANAL_RECORDATORIO)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("¿Ya terminaste tu entrenamiento?")
                .setContentText("Llevas más de 1 hora en " + rutinaNombre + " sin cerrar la sesión.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntentAbrirSesion())
                .build();

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIF_ID_RECORDATORIO, notificacion);
        }
    }

    private PendingIntent pendingIntentAbrirSesion() {
        Intent intent = new Intent(this, RoutineListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(this, 0, intent, flags);
    }

    private void crearCanales() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        AudioAttributes atributosAlerta = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel progreso = new NotificationChannel(
                CANAL_PROGRESO, "Progreso de entrenamiento", NotificationManager.IMPORTANCE_LOW);
        progreso.setDescription("Muestra cuánto tiempo llevas entrenando y el descanso en curso");
        progreso.setShowBadge(false);
        manager.createNotificationChannel(progreso);

        NotificationChannel recordatorio = new NotificationChannel(
                CANAL_RECORDATORIO, "Recordatorio de sesión", NotificationManager.IMPORTANCE_HIGH);
        recordatorio.setDescription("Avisa cuando llevas mucho tiempo sin cerrar tu sesión");
        recordatorio.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), atributosAlerta);
        recordatorio.enableVibration(true);
        manager.createNotificationChannel(recordatorio);

        NotificationChannel descansoFin = new NotificationChannel(
                CANAL_DESCANSO_FIN, "Descanso terminado", NotificationManager.IMPORTANCE_HIGH);
        descansoFin.setDescription("Suena cuando termina el temporizador de descanso");
        descansoFin.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), atributosAlerta);
        descansoFin.enableVibration(true);
        descansoFin.setVibrationPattern(new long[]{0, 200, 100, 200, 100, 300});
        manager.createNotificationChannel(descansoFin);
    }
}
