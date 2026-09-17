package com.ironquest.mvp.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.ui.MainActivity;
import com.ironquest.mvp.ui.MetasFragment;

/**
 * Dispara una notificación única cuando una Meta pasa de <80% a ≥80% de progreso. Reusa el
 * permiso POST_NOTIFICATIONS que ya solicita SesionTrackingService. Si el permiso no está
 * concedido, notify() falla silenciosamente.
 */
public final class NotificacionMetasHelper {

    private static final String CANAL_ID = "metas_progreso";

    private NotificacionMetasHelper() {}

    public static void notificarCerca(Context context, Meta meta) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        crearCanalSiHaceFalta(nm);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MetasFragment.EXTRA_META_DESTACADA_ID, meta.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pi = PendingIntent.getActivity(context,
                meta.getId().hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Estás cerca de cumplir tu meta")
                .setContentText(meta.getTitulo() + " — 80 % de progreso. ¡Sigue así!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(meta.getTitulo() + " — 80 % de progreso. ¡Sigue así!"))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            nm.notify(meta.getId().hashCode(), b.build());
        } catch (SecurityException ignored) {
            // Sin permiso POST_NOTIFICATIONS en Android 13+ → falla silenciosa.
        }
    }

    private static void crearCanalSiHaceFalta(NotificationManager nm) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        if (nm.getNotificationChannel(CANAL_ID) != null) return;
        NotificationChannel canal = new NotificationChannel(CANAL_ID,
                "Progreso de metas", NotificationManager.IMPORTANCE_DEFAULT);
        canal.setDescription("Avisa cuando estás cerca de cumplir una meta.");
        nm.createNotificationChannel(canal);
    }
}
