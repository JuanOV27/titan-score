package com.ironquest.mvp.ui;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ironquest.mvp.R;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pinta un {@link PieChartView} + leyenda con porcentajes a partir de un mapa de distribución. */
public final class PieChartRender {

    private PieChartRender() {
    }

    public static void render(View viewPie, LinkedHashMap<String, Double> datos) {
        PieChartView pieChart = viewPie.findViewById(R.id.pie_chart);
        LinearLayout leyenda = viewPie.findViewById(R.id.container_leyenda);
        TextView vacio = viewPie.findViewById(R.id.text_pie_vacio);

        double total = 0;
        for (Double v : datos.values()) {
            if (v != null) total += v;
        }
        if (total <= 0) {
            pieChart.setVisibility(View.GONE);
            leyenda.setVisibility(View.GONE);
            vacio.setVisibility(View.VISIBLE);
            return;
        }
        vacio.setVisibility(View.GONE);
        pieChart.setVisibility(View.VISIBLE);
        leyenda.setVisibility(View.VISIBLE);
        pieChart.setDatos(datos);
        leyenda.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(viewPie.getContext());
        for (Map.Entry<String, Double> e : datos.entrySet()) {
            int porcentaje = (int) Math.round(100.0 * e.getValue() / total);
            LinearLayout fila = new LinearLayout(viewPie.getContext());
            fila.setOrientation(LinearLayout.HORIZONTAL);
            fila.setGravity(Gravity.CENTER_VERTICAL);
            fila.setPadding(0, 4, 0, 4);

            View colorBox = new View(viewPie.getContext());
            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(24, 24);
            boxParams.rightMargin = 8;
            colorBox.setLayoutParams(boxParams);
            colorBox.setBackgroundColor(PieChartView.getColorPorGrupo(e.getKey()));

            TextView label = new TextView(viewPie.getContext());
            label.setText(e.getKey() + "  " + porcentaje + "%");
            label.setTextSize(13);

            fila.addView(colorBox);
            fila.addView(label);
            leyenda.addView(fila);
        }
    }
}