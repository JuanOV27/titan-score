package com.ironquest.mvp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.ironquest.mvp.util.AnalisisMuscular;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * View custom que dibuja un gráfico circular a partir de un {@code Map<String, Double>}.
 * Paleta fija hardcoded para los 15 grupos musculares del catálogo + gris para "Otros".
 * Proporción 1:1 (cuadrado); ancho recomendado 240dp.
 */
public class PieChartView extends View {

    /** Paleta derivada del acento naranja del tema con suficiente contraste entre valores adyacentes. */
    private static final Map<String, Integer> COLORES = new LinkedHashMap<>();
    static {
        COLORES.put("Pectorales",           Color.parseColor("#FF6B35")); // acento
        COLORES.put("Dorsales",             Color.parseColor("#3B7DDD"));
        COLORES.put("Trapecios",            Color.parseColor("#8E44AD"));
        COLORES.put("Espalda alta",         Color.parseColor("#2ECC71"));
        COLORES.put("Zona lumbar",          Color.parseColor("#E67E22"));
        COLORES.put("Cuádriceps",           Color.parseColor("#1ABC9C"));
        COLORES.put("Isquiotibiales",       Color.parseColor("#9B59B6"));
        COLORES.put("Glúteos",              Color.parseColor("#F39C12"));
        COLORES.put("Aductores/Abductores", Color.parseColor("#16A085"));
        COLORES.put("Pantorrillas",         Color.parseColor("#C0392B"));
        COLORES.put("Hombros",              Color.parseColor("#2980B9"));
        COLORES.put("Bíceps",               Color.parseColor("#D35400"));
        COLORES.put("Tríceps",              Color.parseColor("#7F8C8D"));
        COLORES.put("Antebrazos",           Color.parseColor("#27AE60"));
        COLORES.put("Abdomen",              Color.parseColor("#E74C3C"));
        COLORES.put(AnalisisMuscular.OTROS, Color.parseColor("#95A5A6"));
    }

    private static final int COLOR_FALLBACK = Color.parseColor("#BDC3C7");

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectF = new RectF();
    private Map<String, Double> datos = new LinkedHashMap<>();

    public PieChartView(Context context) {
        super(context);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDatos(Map<String, Double> datos) {
        this.datos = datos != null ? datos : new LinkedHashMap<>();
        invalidate();
    }

    public static int getColorPorGrupo(String grupo) {
        Integer c = COLORES.get(grupo);
        return c != null ? c : COLOR_FALLBACK;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);
        int size = Math.max(200, Math.min(w, h));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        double total = 0;
        for (Double v : datos.values()) {
            if (v != null && v > 0) total += v;
        }
        if (total <= 0) return;

        int padding = 8;
        rectF.set(padding, padding, getWidth() - padding, getHeight() - padding);
        float startAngle = -90f; // arrancar arriba
        paint.setStyle(Paint.Style.FILL);

        for (Map.Entry<String, Double> e : datos.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            float sweep = (float) (360.0 * e.getValue() / total);
            paint.setColor(getColorPorGrupo(e.getKey()));
            canvas.drawArc(rectF, startAngle, sweep, true, paint);
            startAngle += sweep;
        }
    }
}