package com.ironquest.mvp.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarChartView extends View {

    /** Un valor a graficar. Objeto de valor inmutable: se construye desde fuera, se lee solo aquí. */
    public static final class Barra {

        private final String etiqueta;
        private final float valor;
        private final String textoValor;
        private final Integer color;

        public Barra(String etiqueta, float valor, String textoValor) {
            this(etiqueta, valor, textoValor, null);
        }

        public Barra(String etiqueta, float valor, String textoValor, Integer color) {
            this.etiqueta = etiqueta;
            this.valor = valor;
            this.textoValor = textoValor;
            this.color = color;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public float getValor() {
            return valor;
        }

        public String getTextoValor() {
            return textoValor;
        }

        public Integer getColor() {
            return color;
        }
    }

    private List<Barra> barras = new ArrayList<>();
    private float lineaReferencia = -1f;

    private final Paint paintBarra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintEtiqueta = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintReferencia = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rectBarra = new RectF();

    private final int colorPorDefecto = Color.parseColor("#FF6B35");

    public BarChartView(Context context) {
        super(context);
        init();
    }

    public BarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintBarra.setStyle(Paint.Style.FILL);

        paintTexto.setColor(Color.parseColor("#F2F2F2"));
        paintTexto.setTextSize(28f);
        paintTexto.setTextAlign(Paint.Align.CENTER);
        paintTexto.setFakeBoldText(true);

        paintEtiqueta.setColor(Color.parseColor("#99BBBBBB"));
        paintEtiqueta.setTextSize(24f);
        paintEtiqueta.setTextAlign(Paint.Align.CENTER);

        paintReferencia.setColor(Color.parseColor("#4422C55E"));
        paintReferencia.setStrokeWidth(4f);
        paintReferencia.setStyle(Paint.Style.STROKE);
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
        int height = 500;
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
        float padBottom = 80f;
        float padTop = 60f;
        float padSides = 40f;
        float chartWidth = width - (padSides * 2);
        float chartHeight = height - padBottom - padTop;

        float maxValor = lineaReferencia;
        for (Barra barra : barras) {
            maxValor = Math.max(maxValor, barra.getValor());
        }
        if (maxValor <= 0) {
            maxValor = 1f;
        }

        float slotWidth = chartWidth / barras.size();
        float barWidth = Math.min(slotWidth * 0.6f, 100f);

        if (lineaReferencia > 0) {
            float y = padTop + chartHeight - (lineaReferencia / maxValor) * chartHeight;
            canvas.drawLine(padSides, y, width - padSides, y, paintReferencia);
        }

        for (int i = 0; i < barras.size(); i++) {
            Barra barra = barras.get(i);
            float centerX = padSides + slotWidth * i + slotWidth / 2f;
            float barHeight = (barra.getValor() / maxValor) * chartHeight;

            float top = padTop + chartHeight - barHeight;
            float bottom = padTop + chartHeight;
            float left = centerX - barWidth / 2f;
            float right = centerX + barWidth / 2f;

            paintBarra.setColor(barra.getColor() != null ? barra.getColor() : colorPorDefecto);
            rectBarra.set(left, top, right, bottom);
            float radius = barWidth / 4f;
            canvas.drawRoundRect(rectBarra, radius, radius, paintBarra);

            String valorTexto = barra.getTextoValor() != null
                    ? barra.getTextoValor()
                    : String.format(Locale.getDefault(), "%.0f", barra.getValor());
            canvas.drawText(valorTexto, centerX, Math.max(top - 15f, 24f), paintTexto);
            canvas.drawText(barra.getEtiqueta(), centerX, height - 25f, paintEtiqueta);
        }
    }
}
