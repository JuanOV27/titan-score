package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

/** Pestaña "Estadísticas": resumen acumulado y gráficas de volumen, cumplimiento y frecuencia. */
public class StatsFragment extends Fragment {

    private static final DateTimeFormatter FORMATO_ETIQUETA = DateTimeFormatter.ofPattern("dd/MM");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        renderizar(requireView());
    }

    private void renderizar(View raiz) {
        DataManager dataManager = DataManager.getInstance(requireContext());
        List<Sesion> sesiones = new ArrayList<>(dataManager.getDataStore().getSesiones());
        sesiones.sort(Comparator.comparing(s -> s.getFechaHoraInicio()));

        View containerResumen = raiz.findViewById(R.id.container_resumen);
        View labelVolumen = raiz.findViewById(R.id.label_volumen);
        BarChartView chartVolumen = raiz.findViewById(R.id.chart_volumen);
        View labelCumplimiento = raiz.findViewById(R.id.label_cumplimiento);
        BarChartView chartCumplimiento = raiz.findViewById(R.id.chart_cumplimiento);
        View labelFrecuencia = raiz.findViewById(R.id.label_frecuencia);
        BarChartView chartFrecuencia = raiz.findViewById(R.id.chart_frecuencia);
        TextView textSinDatos = raiz.findViewById(R.id.text_sin_datos);

        // Fijar visibilidad en AMBAS ramas: como fragment reutilizable, quedarse solo con la
        // rama GONE dejaría la pantalla en blanco para siempre tras registrar la primera sesión.
        boolean vacio = sesiones.isEmpty();
        int visibilidadDatos = vacio ? View.GONE : View.VISIBLE;
        textSinDatos.setVisibility(vacio ? View.VISIBLE : View.GONE);
        containerResumen.setVisibility(visibilidadDatos);
        labelVolumen.setVisibility(visibilidadDatos);
        chartVolumen.setVisibility(visibilidadDatos);
        labelCumplimiento.setVisibility(visibilidadDatos);
        chartCumplimiento.setVisibility(visibilidadDatos);
        labelFrecuencia.setVisibility(visibilidadDatos);
        chartFrecuencia.setVisibility(visibilidadDatos);

        if (vacio) {
            return;
        }

        mostrarResumen(raiz, sesiones);

        List<Sesion> ultimas = sesiones.subList(Math.max(0, sesiones.size() - 10), sesiones.size());

        List<BarChartView.Barra> barrasVolumen = new ArrayList<>();
        List<BarChartView.Barra> barrasCumplimiento = new ArrayList<>();
        for (Sesion sesion : ultimas) {
            String etiqueta = LocalDateTime.parse(sesion.getFechaHoraInicio()).format(FORMATO_ETIQUETA);
            barrasVolumen.add(new BarChartView.Barra(etiqueta, (float) sesion.getVolumenReal(),
                    String.format(Locale.getDefault(), "%.0f", sesion.getVolumenReal())));
            barrasCumplimiento.add(new BarChartView.Barra(etiqueta, sesion.getPorcentajeCumplimiento(),
                    sesion.getPorcentajeCumplimiento() + "%"));
        }
        chartVolumen.setDatos(barrasVolumen);
        chartCumplimiento.setDatos(barrasCumplimiento);
        chartCumplimiento.setLineaReferencia(100f);

        chartFrecuencia.setDatos(calcularFrecuenciaSemanal(sesiones));
    }

    private void mostrarResumen(View raiz, List<Sesion> sesiones) {
        TextView textTotalSesiones = raiz.findViewById(R.id.text_total_sesiones);
        TextView textVolumenTotal = raiz.findViewById(R.id.text_volumen_total);
        TextView textCumplimientoPromedio = raiz.findViewById(R.id.text_cumplimiento_promedio);

        double volumenTotal = 0;
        long sumaCumplimiento = 0;
        int sesionesFinalizadas = 0;
        for (Sesion sesion : sesiones) {
            volumenTotal += sesion.getVolumenReal();
            if (sesion.estaFinalizada()) {
                sumaCumplimiento += sesion.getPorcentajeCumplimiento();
                sesionesFinalizadas++;
            }
        }

        textTotalSesiones.setText(String.valueOf(sesiones.size()));
        textVolumenTotal.setText(String.format(Locale.getDefault(), "%.0f", volumenTotal));
        int promedio = sesionesFinalizadas > 0
                ? Math.round((float) sumaCumplimiento / sesionesFinalizadas) : 0;
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
            LocalDate fecha = LocalDateTime.parse(sesion.getFechaHoraInicio()).toLocalDate();
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
