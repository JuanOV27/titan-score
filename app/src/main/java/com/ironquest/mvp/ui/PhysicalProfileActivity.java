package com.ironquest.mvp.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.util.PerfilFisicoUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;

public class PhysicalProfileActivity extends BaseActivity {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);

    private DataManager dataManager;
    private DataStore dataStore;
    private LayoutInflater inflater;

    private double peso = 70.0;
    private double altura = 170.0;
    private double pecho = 90.0;
    private double cintura = 80.0;
    private double cadera = 95.0;
    private double brazo = 30.0;
    private double pierna = 50.0;
    private String tipoCuerpo;
    private LocalDate fechaSeleccionada;

    private TextView textImc;
    private TextView textFecha;
    private MaterialButton buttonHombre;
    private MaterialButton buttonMujer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_physical_profile);

        configurarToolbar(R.id.toolbar, "Registrar medidas", true);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();
        inflater = LayoutInflater.from(this);
        fechaSeleccionada = LocalDate.now();

        precargarUltimoRegistro();

        textFecha = findViewById(R.id.text_fecha_registro);
        textImc = findViewById(R.id.text_imc);
        actualizarTextoFecha();
        findViewById(R.id.text_cambiar_fecha).setOnClickListener(v -> mostrarSelectorFecha());

        LinearLayout containerComposicion = findViewById(R.id.container_composicion);
        agregarStepper(containerComposicion, "Peso", peso, 0.5, 30, 300, " kg", valor -> {
            peso = valor;
            actualizarImc();
        });
        agregarStepper(containerComposicion, "Altura", altura, 1, 100, 250, " cm", valor -> {
            altura = valor;
            actualizarImc();
        });
        actualizarImc();

        LinearLayout containerMedidas = findViewById(R.id.container_medidas);
        agregarStepper(containerMedidas, "Pecho", pecho, 0.5, 40, 200, " cm", valor -> pecho = valor);
        agregarStepper(containerMedidas, "Cintura", cintura, 0.5, 40, 200, " cm", valor -> cintura = valor);
        agregarStepper(containerMedidas, "Cadera", cadera, 0.5, 40, 200, " cm", valor -> cadera = valor);
        agregarStepper(containerMedidas, "Brazo", brazo, 0.5, 15, 80, " cm", valor -> brazo = valor);
        agregarStepper(containerMedidas, "Piernas", pierna, 0.5, 25, 120, " cm", valor -> pierna = valor);

        buttonHombre = findViewById(R.id.button_tipo_hombre);
        buttonMujer = findViewById(R.id.button_tipo_mujer);
        buttonHombre.setOnClickListener(v -> tipoCuerpo = "Hombre");
        buttonMujer.setOnClickListener(v -> tipoCuerpo = "Mujer");
        if ("Hombre".equals(tipoCuerpo)) {
            buttonHombre.setChecked(true);
        } else if ("Mujer".equals(tipoCuerpo)) {
            buttonMujer.setChecked(true);
        }

        findViewById(R.id.button_guardar_medidas).setOnClickListener(v -> guardarMedidas());
    }

    private void precargarUltimoRegistro() {
        List<RegistroFisico> historial = dataStore.getHistorialFisico();
        if (historial.isEmpty()) {
            return;
        }
        RegistroFisico ultimo = historial.get(historial.size() - 1);
        peso = ultimo.getPesoKg();
        altura = ultimo.getAlturaCm();
        pecho = ultimo.getPechoCm();
        cintura = ultimo.getCinturaCm();
        cadera = ultimo.getCaderaCm();
        brazo = ultimo.getBrazoCm();
        pierna = ultimo.getPiernaCm();
        tipoCuerpo = ultimo.getTipoCuerpo();
    }

    private void mostrarSelectorFecha() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            fechaSeleccionada = LocalDate.of(year, month + 1, dayOfMonth);
            actualizarTextoFecha();
        }, fechaSeleccionada.getYear(), fechaSeleccionada.getMonthValue() - 1, fechaSeleccionada.getDayOfMonth())
                .show();
    }

    private void actualizarTextoFecha() {
        boolean esHoy = fechaSeleccionada.equals(LocalDate.now());
        textFecha.setText((esHoy ? "Hoy, " : "") + fechaSeleccionada.format(FORMATO_FECHA));
    }

    private void actualizarImc() {
        double imc = PerfilFisicoUtil.calcularImc(peso, altura);
        textImc.setText(String.format(Locale.US, "IMC: %.1f (%s)", imc, PerfilFisicoUtil.clasificarImc(imc)));
        textImc.setTextColor(getColor(PerfilFisicoUtil.colorParaImc(imc)));
    }

    private void guardarMedidas() {
        if (tipoCuerpo == null) {
            Toast.makeText(this, "Selecciona tu tipo de cuerpo", Toast.LENGTH_SHORT).show();
            return;
        }

        RegistroFisico registro = new RegistroFisico();
        registro.setId(dataManager.newId("fis"));
        registro.setFecha(fechaSeleccionada.toString());
        registro.setPesoKg(peso);
        registro.setAlturaCm(altura);
        registro.setImc(PerfilFisicoUtil.calcularImc(peso, altura));
        registro.setPechoCm(pecho);
        registro.setCinturaCm(cintura);
        registro.setCaderaCm(cadera);
        registro.setBrazoCm(brazo);
        registro.setPiernaCm(pierna);
        registro.setTipoCuerpo(tipoCuerpo);

        dataStore.getHistorialFisico().add(registro);
        dataManager.save();

        Toast.makeText(this, "Medidas guardadas", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void agregarStepper(LinearLayout container, String etiqueta, double valorInicial, double paso,
                                 double min, double max, String unidad, DoubleConsumer onChange) {
        View row = inflater.inflate(R.layout.view_medida_stepper_row, container, false);
        TextView label = row.findViewById(R.id.text_label_medida);
        TextView valorTexto = row.findViewById(R.id.text_valor_medida);
        ImageButton menos = row.findViewById(R.id.button_medida_menos);
        ImageButton mas = row.findViewById(R.id.button_medida_mas);

        label.setText(etiqueta);
        double[] valor = {valorInicial};
        valorTexto.setText(formatearValor(valor[0], unidad));

        menos.setOnClickListener(v -> {
            valor[0] = Math.max(min, redondear(valor[0] - paso));
            valorTexto.setText(formatearValor(valor[0], unidad));
            onChange.accept(valor[0]);
        });
        mas.setOnClickListener(v -> {
            valor[0] = Math.min(max, redondear(valor[0] + paso));
            valorTexto.setText(formatearValor(valor[0], unidad));
            onChange.accept(valor[0]);
        });

        container.addView(row);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private static String formatearValor(double valor, String unidad) {
        if (valor == Math.floor(valor)) {
            return (long) valor + unidad;
        }
        return String.format(Locale.US, "%.1f%s", valor, unidad);
    }
}
