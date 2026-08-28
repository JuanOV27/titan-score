package com.ironquest.mvp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarChartView extends View {

    public static class Barra {
        public final String etiqueta;
        public final float valor;
        public final String textoValor;

        public Barra(String etiqueta, float valor, String textoValor) {
            this.etiqueta = etiqueta;
            this.valor = valor;
            this.textoValor = textoValor;
        }
    }

    private List<Barra> barras = new ArrayList<>();
    private float lineaReferencia = -1f;

    private final Paint paintBarra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintEtiqueta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintReferencia = new Paint(Paint.ANTI_ALIAS_FLAG);

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintBarra.setColor(Color.parseColor("#FF6B35"));
        paintTexto.setColor(Color.parseColor("#33261D"));
        paintTexto.setTextSize(28f);
        paintTexto.setTextAlign(Paint.Align.CENTER);
        paintEtiqueta.setColor(Color.parseColor("#66332000"));
        paintEtiqueta.setColor(Color.DKGRAY);
        paintEtiqueta.setTextSize(24f);
        paintEtiqueta.setTextAlign(Paint.Align.CENTER);
        paintReferencia.setColor(Color.parseColor("#8822C55E"));
        paintReferencia.setStrokeWidth(3f);
    }

    public void setDatos(List<Barra> barras) {
        this.barras = barras != null ? barras : new ArrayList<>();
        requestLayout();
        invalidate();
    }

    public void setLineaReferencia(float valor) {
        this.lineaReferencia = valor;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = 420;
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (barras.isEmpty()) {
            paintEtiqueta.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Sin datos todavía", getWidth() / 2f, getHeight() / 2f, paintEtiqueta);
            return;
        }

        int width = getWidth();
        int height = getHeight();
        float padBottom = 60f;
        float padTop = 30f;
        float chartHeight = height - padBottom - padTop;

        float maxValor = lineaReferencia;
        for (Barra barra : barras) {
            maxValor = Math.max(maxValor, barra.valor);
        }
        if (maxValor <= 0) {
            maxValor = 1f;
        }

        float slotWidth = (float) width / barras.size();
        float barWidth = Math.min(slotWidth * 0.55f, 90f);

        if (lineaReferencia > 0) {
            float y = padTop + chartHeight - (lineaReferencia / maxValor) * chartHeight;
            canvas.drawLine(0, y, width, y, paintReferencia);
        }

        for (int i = 0; i < barras.size(); i++) {
            Barra barra = barras.get(i);
            float centerX = slotWidth * i + slotWidth / 2f;
            float barHeight = maxValor > 0 ? (barra.valor / maxValor) * chartHeight : 0;
            float top = padTop + chartHeight - barHeight;
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;

            canvas.drawRoundRect(left, top, right, padTop + chartHeight, 8f, 8f, paintBarra);

            String valorTexto = barra.textoValor != null
                    ? barra.textoValor
                    : String.format(Locale.getDefault(), "%.0f", barra.valor);
            canvas.drawText(valorTexto, centerX, Math.max(top - 10f, 24f), paintTexto);
            canvas.drawText(barra.etiqueta, centerX, height - 15f, paintEtiqueta);
        }
    }
}
