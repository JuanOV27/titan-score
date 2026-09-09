package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.util.EstadisticasUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pestaña "Estadísticas": resumen acumulado y gráficas de duración, cumplimiento y frecuencia. */
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
        View cardDuracion = raiz.findViewById(R.id.card_duracion);
        BarChartView chartDuracion = raiz.findViewById(R.id.chart_duracion);
        View cardCumplimiento = raiz.findViewById(R.id.card_cumplimiento);
        BarChartView chartCumplimiento = raiz.findViewById(R.id.chart_cumplimiento);
        View cardFrecuencia = raiz.findViewById(R.id.card_frecuencia);
        BarChartView chartFrecuencia = raiz.findViewById(R.id.chart_frecuencia);
        View textSinDatos = raiz.findViewById(R.id.text_sin_datos);

        // Fijar visibilidad en AMBAS ramas: como fragment reutilizable, quedarse solo con la
        // rama GONE dejaría la pantalla en blanco para siempre tras registrar la primera sesión.
        boolean vacio = sesiones.isEmpty();
        int visibilidadDatos = vacio ? View.GONE : View.VISIBLE;
        textSinDatos.setVisibility(vacio ? View.VISIBLE : View.GONE);
        containerResumen.setVisibility(visibilidadDatos);
        cardDuracion.setVisibility(visibilidadDatos);
        cardCumplimiento.setVisibility(visibilidadDatos);
        cardFrecuencia.setVisibility(visibilidadDatos);

        if (vacio) {
            return;
        }

        mostrarResumen(raiz, sesiones);

        List<Sesion> ultimas = sesiones.subList(Math.max(0, sesiones.size() - 7), sesiones.size());

        int colorDuracion = ContextCompat.getColor(requireContext(), R.color.ironquest_orange);

        List<BarChartView.Barra> barrasDuracion = new ArrayList<>();
        List<BarChartView.Barra> barrasCumplimiento = new ArrayList<>();
        for (Sesion sesion : ultimas) {
            String etiqueta = LocalDateTime.parse(sesion.getFechaHoraInicio()).format(FORMATO_ETIQUETA);
            long duracionMinutos = EstadisticasUtil.calcularDuracionMinutos(sesion);
            barrasDuracion.add(new BarChartView.Barra(etiqueta, duracionMinutos,
                    EstadisticasUtil.formatearDuracion(duracionMinutos), colorDuracion));
            barrasCumplimiento.add(new BarChartView.Barra(etiqueta, sesion.getPorcentajeCumplimiento(),
                    sesion.getPorcentajeCumplimiento() + "%", colorCumplimiento(sesion.getPorcentajeCumplimiento())));
        }
        chartDuracion.setDatos(barrasDuracion);
        chartCumplimiento.setDatos(barrasCumplimiento);
        chartCumplimiento.setLineaReferencia(100f);

        chartFrecuencia.setDatos(calcularFrecuenciaSemanal(sesiones));
    }

    private void mostrarResumen(View raiz, List<Sesion> sesiones) {
        TextView textTotalSesiones = raiz.findViewById(R.id.text_total_sesiones);
        TextView textDuracionPromedio = raiz.findViewById(R.id.text_duracion_promedio);
        TextView textCumplimientoPromedio = raiz.findViewById(R.id.text_cumplimiento_promedio);

        long sumaDuracion = 0;
        long sumaCumplimiento = 0;
        int sesionesFinalizadas = 0;
        for (Sesion sesion : sesiones) {
            if (sesion.estaFinalizada()) {
                sumaDuracion += EstadisticasUtil.calcularDuracionMinutos(sesion);
                sumaCumplimiento += sesion.getPorcentajeCumplimiento();
                sesionesFinalizadas++;
            }
        }

        textTotalSesiones.setText(String.valueOf(sesiones.size()));
        long duracionPromedio = sesionesFinalizadas > 0 ? sumaDuracion / sesionesFinalizadas : 0;
        textDuracionPromedio.setText(EstadisticasUtil.formatearDuracion(duracionPromedio));
        int promedio = sesionesFinalizadas > 0
                ? Math.round((float) sumaCumplimiento / sesionesFinalizadas) : 0;
        textCumplimientoPromedio.setText(promedio + "%");
    }

    /** Mismos umbrales que HistorialAdapter/SessionSummaryActivity para el color de cumplimiento. */
    private int colorCumplimiento(int porcentaje) {
        int colorRes = porcentaje >= 100
                ? R.color.cumplimiento_alto
                : porcentaje >= 60
                ? R.color.cumplimiento_medio
                : R.color.cumplimiento_bajo;
        return ContextCompat.getColor(requireContext(), colorRes);
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

        int colorFrecuencia = ContextCompat.getColor(requireContext(), R.color.ironquest_orange);
        List<BarChartView.Barra> barras = new ArrayList<>();
        for (int i = 0; i < inicios.size(); i++) {
            String etiqueta = inicios.get(i).format(FORMATO_ETIQUETA);
            barras.add(new BarChartView.Barra(etiqueta, conteos[i], String.valueOf(conteos[i]), colorFrecuencia));
        }
        return barras;
    }
}
