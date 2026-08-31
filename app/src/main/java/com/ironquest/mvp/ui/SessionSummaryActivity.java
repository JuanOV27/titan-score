package com.ironquest.mvp.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.ironquest.mvp.R;
import android.widget.TextView;

public class SessionSummaryActivity extends AppCompatActivity {

    public static final String EXTRA_RUTINA_NOMBRE = "extra_rutina_nombre";
    public static final String EXTRA_DURACION_MINUTOS = "extra_duracion_minutos";
    public static final String EXTRA_RACHA_DIAS = "extra_racha_dias";
    public static final String EXTRA_PORCENTAJE = "extra_porcentaje";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_summary);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Resumen del entrenamiento");

        String rutinaNombre = getIntent().getStringExtra(EXTRA_RUTINA_NOMBRE);
        long duracionMinutos = getIntent().getLongExtra(EXTRA_DURACION_MINUTOS, 0);
        int racha = getIntent().getIntExtra(EXTRA_RACHA_DIAS, 0);
        int porcentaje = getIntent().getIntExtra(EXTRA_PORCENTAJE, 0);

        TextView textRutina = findViewById(R.id.text_rutina_nombre);
        textRutina.setText(rutinaNombre != null ? rutinaNombre : "");

        TextView textDuracion = findViewById(R.id.text_duracion);
        long horas = duracionMinutos / 60;
        long minutos = duracionMinutos % 60;
        textDuracion.setText(horas > 0 ? (horas + "h " + minutos + "min") : (minutos + " min"));

        TextView textRacha = findViewById(R.id.text_racha);
        textRacha.setText(racha == 1 ? "1 día" : racha + " días");

        TextView textCumplimiento = findViewById(R.id.text_cumplimiento);
        textCumplimiento.setText(porcentaje + "%");
        int colorRes = porcentaje >= 100
                ? R.color.cumplimiento_alto
                : porcentaje >= 60
                ? R.color.cumplimiento_medio
                : R.color.cumplimiento_bajo;
        textCumplimiento.setTextColor(ContextCompat.getColor(this, colorRes));

        findViewById(R.id.button_volver_inicio).setOnClickListener(v -> volverInicio());
    }

    @Override
    public void onBackPressed() {
        volverInicio();
    }

    private void volverInicio() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
