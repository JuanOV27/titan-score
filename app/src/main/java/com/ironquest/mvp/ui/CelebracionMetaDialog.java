package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.*;
import android.widget.TextView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Meta;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class CelebracionMetaDialog extends Dialog {

    private final List<Meta> cumplidas;

    public static void mostrar(Activity activity, List<Meta> cumplidas) {
        if (cumplidas == null || cumplidas.isEmpty()) return;
        new CelebracionMetaDialog(activity, cumplidas).show();
    }

    private CelebracionMetaDialog(Activity activity, List<Meta> cumplidas) {
        super(activity);
        this.cumplidas = cumplidas;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_celebracion_meta);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setCancelable(false);

        TextView lista = findViewById(R.id.text_celebracion_lista);
        StringBuilder sb = new StringBuilder();
        for (Meta m : cumplidas) {
            sb.append("• ").append(m.getTitulo()).append("\n");
            sb.append("   ").append(duracion(m)).append("\n\n");
        }
        lista.setText(sb.toString().trim());

        findViewById(R.id.button_celebracion_continuar).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_celebracion_ver).setOnClickListener(v -> {
            Intent i = new Intent(getContext(), MainActivity.class);
            i.putExtra(MetasFragment.EXTRA_META_DESTACADA_ID, cumplidas.get(0).getId());
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            dismiss();
        });

        Vibrator vib = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vib != null && vib.hasVibrator()) {
            vib.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private static String duracion(Meta m) {
        try {
            LocalDateTime c = LocalDateTime.parse(m.getFechaCreacion());
            LocalDateTime f = LocalDateTime.parse(m.getFechaCumplida());
            long dias = ChronoUnit.DAYS.between(c.toLocalDate(), f.toLocalDate());
            if (dias == 0) return "En un día";
            if (dias < 14) return "En " + dias + " días";
            long semanas = dias / 7;
            long restoDias = dias % 7;
            return "En " + semanas + " semanas" + (restoDias > 0 ? " y " + restoDias + " días" : "");
        } catch (Exception e) { return ""; }
    }
}
