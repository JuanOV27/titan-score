package com.ironquest.mvp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.SugerenciaPendiente;
import com.ironquest.mvp.util.AnalisisMuscular;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionSummaryActivity extends BaseActivity {

    public static final String EXTRA_RUTINA_NOMBRE = "extra_rutina_nombre";
    public static final String EXTRA_DURACION_MINUTOS = "extra_duracion_minutos";
    public static final String EXTRA_RACHA_DIAS = "extra_racha_dias";
    public static final String EXTRA_PORCENTAJE = "extra_porcentaje";
    public static final String EXTRA_SESION_ID = "extra_sesion_id";
    public static final String EXTRA_RUTINA_ID = "extra_rutina_id";
    public static final String EXTRA_METAS_CUMPLIDAS_IDS = "extra_metas_cumplidas_ids";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_summary);

        configurarToolbar(R.id.toolbar, "Resumen del entrenamiento", false);

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

        DataManager dataManager = DataManager.getInstance(this);
        DataStore dataStore = dataManager.getDataStore();
        String rutinaId = getIntent().getStringExtra(EXTRA_RUTINA_ID);
        String sesionId = getIntent().getStringExtra(EXTRA_SESION_ID);

        View cardSugerencias = findViewById(R.id.card_sugerencias);
        renderSugerencias(cardSugerencias, dataManager, dataStore, rutinaId, sesionId);

        Sesion sesion = null;
        if (sesionId != null) {
            for (Sesion s : dataStore.getSesiones()) {
                if (sesionId.equals(s.getId())) {
                    sesion = s;
                    break;
                }
            }
        }
        if (sesion != null) {
            Map<String, Ejercicio> catalogoPorId = new HashMap<>();
            for (Ejercicio ej : dataStore.getEjercicios()) {
                catalogoPorId.put(ej.getId(), ej);
            }
            LinkedHashMap<String, Double> datos =
                    AnalisisMuscular.calcularVolumenPorGrupo(sesion, catalogoPorId);
            PieChartRender.render(findViewById(R.id.view_pie_sesion), datos);
        }

        findViewById(R.id.button_volver_inicio).setOnClickListener(v -> volverInicio());

        java.util.ArrayList<String> metasIds = getIntent().getStringArrayListExtra(EXTRA_METAS_CUMPLIDAS_IDS);
        if (metasIds != null && !metasIds.isEmpty()) {
            java.util.List<com.ironquest.mvp.model.Meta> metas = new java.util.ArrayList<>();
            for (String id : metasIds) {
                for (com.ironquest.mvp.model.Meta m : dataManager.getDataStore().getMetas()) {
                    if (id.equals(m.getId())) { metas.add(m); break; }
                }
            }
            CelebracionMetaDialog.mostrar(this, metas);
        }
    }

    private void renderSugerencias(View card, DataManager dataManager, DataStore dataStore,
                                   String rutinaId, String sesionId) {
        List<SugerenciaPendiente> todas = dataManager.getSugerenciasPendientesDeRutina(rutinaId);
        List<SugerenciaPendiente> deEstaSesion = new ArrayList<>();
        for (SugerenciaPendiente sp : todas) {
            if (sesionId != null && sesionId.equals(sp.getSesionOrigenId())) {
                deEstaSesion.add(sp);
            }
        }
        if (deEstaSesion.isEmpty()) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        LinearLayout container = card.findViewById(R.id.container_sugerencias);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (SugerenciaPendiente sp : deEstaSesion) {
            View item = inflater.inflate(R.layout.item_sugerencia_pendiente, container, false);
            Ejercicio ej = dataStore.buscarEjercicio(sp.getEjercicioId());
            String nombre = ej != null ? ej.getNombre() : "Ejercicio";
            ((TextView) item.findViewById(R.id.text_nombre_ejercicio)).setText(nombre);
            ((TextView) item.findViewById(R.id.text_cambio)).setText(formatearCambio(sp));
            ((TextView) item.findViewById(R.id.text_explicacion)).setText(sp.getExplicacion());
            item.findViewById(R.id.button_aceptar).setOnClickListener(v -> {
                dataManager.aplicarSugerencia(sp.getId());
                renderSugerencias(card, dataManager, dataStore, rutinaId, sesionId);
            });
            item.findViewById(R.id.button_ignorar).setOnClickListener(v -> {
                dataManager.eliminarSugerencia(sp.getId());
                renderSugerencias(card, dataManager, dataStore, rutinaId, sesionId);
            });
            container.addView(item);
        }
        card.findViewById(R.id.button_aceptar_todas).setOnClickListener(v -> {
            for (SugerenciaPendiente sp : new ArrayList<>(deEstaSesion)) {
                dataManager.aplicarSugerencia(sp.getId());
            }
            renderSugerencias(card, dataManager, dataStore, rutinaId, sesionId);
        });
    }

    private static String formatearCambio(SugerenciaPendiente sp) {
        return formatearPeso(sp.getPesoActual()) + " kg × " + sp.getRepeticionesActuales()
                + " → " + formatearPeso(sp.getPesoSugerido()) + " kg × " + sp.getRepeticionesSugeridas();
    }

    private static String formatearPeso(double p) {
        if (p == 0) {
            return "0";
        }
        if (p == Math.floor(p)) {
            return String.valueOf((long) p);
        }
        return String.valueOf(p);
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
