package com.ironquest.mvp.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Ejercicio;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/** Ficha técnica de un ejercicio curado: GIF animado, músculos y atribución obligatoria. */
final class DetalleEjercicioDialog extends Dialog {

    private final Ejercicio ejercicio;

    DetalleEjercicioDialog(Context context, Ejercicio ejercicio) {
        super(context);
        this.ejercicio = ejercicio;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_detalle_ejercicio);
        setCancelable(true);

        TextView nombre = findViewById(R.id.text_nombre_detalle_ejercicio);
        GifImageView gif = findViewById(R.id.gif_detalle_ejercicio);
        TextView musculo = findViewById(R.id.text_musculo_objetivo_detalle);
        TextView secundarios = findViewById(R.id.text_musculos_secundarios_detalle);
        TextView instrucciones = findViewById(R.id.text_instrucciones_detalle);

        nombre.setText(ejercicio.getNombre());

        if (ejercicio.getMusculoObjetivo() != null) {
            musculo.setVisibility(View.VISIBLE);
            musculo.setText("Músculo objetivo: " + ejercicio.getMusculoObjetivo());
        } else {
            musculo.setVisibility(View.GONE);
        }

        if (!ejercicio.getMusculosSecundarios().isEmpty()) {
            secundarios.setVisibility(View.VISIBLE);
            secundarios.setText("También trabaja: " + String.join(", ", ejercicio.getMusculosSecundarios()));
        } else {
            secundarios.setVisibility(View.GONE);
        }

        if (!ejercicio.getInstrucciones().isEmpty()) {
            instrucciones.setVisibility(View.VISIBLE);
            instrucciones.setText(numerarInstrucciones(ejercicio.getInstrucciones()));
        } else {
            instrucciones.setVisibility(View.GONE);
        }

        if (ejercicio.tieneFichaTecnica()) {
            try {
                gif.setImageDrawable(new GifDrawable(getContext().getAssets(), ejercicio.getGifAsset()));
            } catch (IOException e) {
                gif.setVisibility(View.GONE);
            }
        } else {
            gif.setVisibility(View.GONE);
        }

        findViewById(R.id.button_cerrar_detalle_ejercicio).setOnClickListener(v -> dismiss());
    }

    private static String numerarInstrucciones(java.util.List<String> pasos) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < pasos.size(); i++) {
            if (i > 0) {
                texto.append('\n');
            }
            texto.append(i + 1).append(". ").append(pasos.get(i));
        }
        return texto.toString();
    }
}
