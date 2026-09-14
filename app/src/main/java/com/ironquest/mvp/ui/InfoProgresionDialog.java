package com.ironquest.mvp.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.RutinaEjercicio;

/**
 * Diálogo educativo sobre los esquemas de progresión, el deload y el silencio de
 * sugerencias. Se abre desde el icono "?" del overlay activo y desde el banner
 * de Editar Rutina.
 */
public class InfoProgresionDialog {

    private static final int[] ESQUEMA_DRAWABLES = {
            R.drawable.esquema_ninguno,
            R.drawable.esquema_lineal,
            R.drawable.esquema_greyskull,
            R.drawable.esquema_doble,
            R.drawable.esquema_automatico
    };

    private static final int[] ESQUEMA_TITULOS = {
            R.string.esquema_nombre_ninguno,
            R.string.esquema_nombre_lineal,
            R.string.esquema_nombre_greyskull,
            R.string.esquema_nombre_doble,
            R.string.esquema_nombre_automatico
    };

    private static final int[] ESQUEMA_DESCRIPCIONES = {
            R.string.esquema_desc_ninguno,
            R.string.esquema_desc_lineal,
            R.string.esquema_desc_greyskull,
            R.string.esquema_desc_doble,
            R.string.esquema_desc_automatico
    };

    private InfoProgresionDialog() {
    }

    public static void mostrar(Context contexto) {
        ScrollView vista = (ScrollView) LayoutInflater.from(contexto)
                .inflate(R.layout.dialog_info_progresion, null);
        LinearLayout container = vista.findViewById(R.id.container_esquemas);

        for (int i = 0; i < ESQUEMA_DRAWABLES.length; i++) {
            View item = LayoutInflater.from(contexto).inflate(R.layout.item_esquema_info, container, false);
            ((ImageView) item.findViewById(R.id.image_esquema)).setImageResource(ESQUEMA_DRAWABLES[i]);
            ((TextView) item.findViewById(R.id.text_esquema_titulo)).setText(ESQUEMA_TITULOS[i]);
            ((TextView) item.findViewById(R.id.text_esquema_descripcion)).setText(ESQUEMA_DESCRIPCIONES[i]);
            container.addView(item);
        }

        AlertDialog dialog = new AlertDialog.Builder(contexto)
                .setTitle("¿Cómo funcionan las progresiones?")
                .setView(vista)
                .setPositiveButton("Entendido", null)
                .create();

        dialog.setOnShowListener(d -> {
            TextView headerDeload = vista.findViewById(R.id.header_deload);
            TextView bodyDeload = vista.findViewById(R.id.body_deload);
            TextView headerSilenciar = vista.findViewById(R.id.header_silenciar);
            TextView bodySilenciar = vista.findViewById(R.id.body_silenciar);
            headerDeload.setOnClickListener(v -> alternarCuerpo(bodyDeload, headerDeload));
            headerSilenciar.setOnClickListener(v -> alternarCuerpo(bodySilenciar, headerSilenciar));
            asignarTextosDeload(headerDeload, bodyDeload);
            asignarTextosSilenciar(headerSilenciar, bodySilenciar);
        });

        dialog.show();
    }

    private static void asignarTextosSilenciar(TextView header, TextView body) {
        header.setText("▸ Silenciar sugerencias");
        body.setText("El botón de la campana detiene las sugerencias para este ejercicio "
                + "hasta que lo vuelvas a activar. Sirve para ejercicios donde prefieres "
                + "manejar el progreso a mano.");
    }

    private static void asignarTextosDeload(TextView header, TextView body) {
        header.setText("▸ Deload: por qué a veces se sugiere bajar");
        body.setText("Cuando fallas 2 o 3 sesiones seguidas, la app propone reducir un 10% "
                + "el peso antes de volver a subir. Es una pausa estratégica para recuperar "
                + "fuerza y evitar estancarte.");
    }

    private static void alternarCuerpo(TextView cuerpo, TextView header) {
        boolean visible = cuerpo.getVisibility() == View.VISIBLE;
        cuerpo.setVisibility(visible ? View.GONE : View.VISIBLE);
        header.setText(cuerpo.getVisibility() == View.VISIBLE
                ? header.getText().toString().replace("▸ ", "▾ ")
                : header.getText().toString().replace("▾ ", "▸ "));
    }
}