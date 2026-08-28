package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.Sesion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    private static final DateTimeFormatter FORMATO_ETIQUETA = DateTimeFormatter.ofPattern("dd/MM");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Estadísticas");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        DataManager dataManager = DataManager.getInstance(this);
        List<Sesion> sesiones = new ArrayList<>(dataManager.getDataStore().sesiones);
        sesiones.sort(Comparator.comparing(s -> s.fechaHoraInicio));

        View containerResumen = findViewById(R.id.container_resumen);
        View labelVolumen = findViewById(R.id.label_volumen);
        BarChartView chartVolumen = findViewById(R.id.chart_volumen);
        View labelCumplimiento = findViewById(R.id.label_cumplimiento);
        BarChartView chartCumplimiento = findViewById(R.id.chart_cumplimiento);
        View labelFrecuencia = findViewById(R.id.label_frecuencia);
        BarChartView chartFrecuencia = findViewById(R.id.chart_frecuencia);
        TextView textSinDatos = findViewById(R.id.text_sin_datos);

        if (sesiones.isEmpty()) {
            textSinDatos.setVisibility(View.VISIBLE);
            containerResumen.setVisibility(View.GONE);
            labelVolumen.setVisibility(View.GONE);
            chartVolumen.setVisibility(View.GONE);
            labelCumplimiento.setVisibility(View.GONE);
            chartCumplimiento.setVisibility(View.GONE);
            labelFrecuencia.setVisibility(View.GONE);
            chartFrecuencia.setVisibility(View.GONE);
            return;
        }

        mostrarResumen(sesiones);

        List<Sesion> ultimas = sesiones.subList(Math.max(0, sesiones.size() - 10), sesiones.size());

        List<BarChartView.Barra> barrasVolumen = new ArrayList<>();
        List<BarChartView.Barra> barrasCumplimiento = new ArrayList<>();
        for (Sesion sesion : ultimas) {
            String etiqueta = LocalDateTime.parse(sesion.fechaHoraInicio).format(FORMATO_ETIQUETA);
            barrasVolumen.add(new BarChartView.Barra(etiqueta, (float) sesion.volumenReal,
                    String.format(Locale.getDefault(), "%.0f", sesion.volumenReal)));
            barrasCumplimiento.add(new BarChartView.Barra(etiqueta, sesion.porcentajeCumplimiento,
                    sesion.porcentajeCumplimiento + "%"));
        }
        chartVolumen.setDatos(barrasVolumen);
        chartCumplimiento.setDatos(barrasCumplimiento);
        chartCumplimiento.setLineaReferencia(100f);

        chartFrecuencia.setDatos(calcularFrecuenciaSemanal(sesiones));
    }

    private void mostrarResumen(List<Sesion> sesiones) {
        TextView textTotalSesiones = findViewById(R.id.text_total_sesiones);
        TextView textVolumenTotal = findViewById(R.id.text_volumen_total);
        TextView textCumplimientoPromedio = findViewById(R.id.text_cumplimiento_promedio);

        double volumenTotal = 0;
        long sumaCumplimiento = 0;
        int sesionesFinalizadas = 0;
        for (Sesion sesion : sesiones) {
            volumenTotal += sesion.volumenReal;
            if (sesion.fechaHoraFin != null) {
                sumaCumplimiento += sesion.porcentajeCumplimiento;
                sesionesFinalizadas++;
            }
        }

        textTotalSesiones.setText(String.valueOf(sesiones.size()));
        textVolumenTotal.setText(String.format(Locale.getDefault(), "%.0f", volumenTotal));
        int promedio = sesionesFinalizadas > 0 ? Math.round((float) sumaCumplimiento / sesionesFinalizadas) : 0;
        textCumplimientoPromedio.setText(promedio + "%");
    }

    private List<BarChartView.Barra> calcularFrecuenciaSemanal(List<Sesion> sesiones) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemanaActual = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<LocalDate> inicios = new ArrayList<>();
        for (int i = 7; i >= 0; i--) {
            inicios.add(inicioSemanaActual.minusWeeks(i));
        }

        int[] conteos = new int[inicios.size()];
        for (Sesion sesion : sesiones) {
            LocalDate fecha = LocalDateTime.parse(sesion.fechaHoraInicio).toLocalDate();
            for (int i = 0; i < inicios.size(); i++) {
                LocalDate inicioSemana = inicios.get(i);
                LocalDate finSemana = inicioSemana.plusDays(6);
                if (!fecha.isBefore(inicioSemana) && !fecha.isAfter(finSemana)) {
                    conteos[i]++;
                    break;
                }
            }
        }

        List<BarChartView.Barra> barras = new ArrayList<>();
        for (int i = 0; i < inicios.size(); i++) {
            String etiqueta = inicios.get(i).format(FORMATO_ETIQUETA);
            barras.add(new BarChartView.Barra(etiqueta, conteos[i], String.valueOf(conteos[i])));
        }
        return barras;
    }
}
