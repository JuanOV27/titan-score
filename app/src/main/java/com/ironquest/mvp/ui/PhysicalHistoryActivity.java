package com.ironquest.mvp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.util.PerfilFisicoUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PhysicalHistoryActivity extends AppCompatActivity {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);

    private DataManager dataManager;
    private RecyclerView recyclerView;
    private TextView textEmpty;
    private androidx.cardview.widget.CardView cardImcActual;
    private TextView textImcActual;
    private TextView textFechaUltimoRegistro;
    private androidx.cardview.widget.CardView cardAnalisisFisico;
    private TextView textIcc;
    private TextView textIcEst;
    private TextView textVTaper;
    private TextView textRecomendaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_physical_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("Mi físico");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        dataManager = DataManager.getInstance(this);

        recyclerView = findViewById(R.id.recycler_historial_fisico);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        textEmpty = findViewById(R.id.text_empty_fisico);
        cardImcActual = findViewById(R.id.card_imc_actual);
        textImcActual = findViewById(R.id.text_imc_actual);
        textFechaUltimoRegistro = findViewById(R.id.text_fecha_ultimo_registro);
        cardAnalisisFisico = findViewById(R.id.card_analisis_fisico);
        textIcc = findViewById(R.id.text_icc);
        textIcEst = findViewById(R.id.text_icest);
        textVTaper = findViewById(R.id.text_vtaper);
        textRecomendaciones = findViewById(R.id.text_recomendaciones);

        findViewById(R.id.button_nuevo_registro_fisico).setOnClickListener(v ->
                startActivity(new Intent(this, PhysicalProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        actualizarLista();
    }

    private void actualizarLista() {
        List<RegistroFisico> historial = dataManager.getDataStore().historialFisico;

        boolean vacio = historial.isEmpty();
        textEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(vacio ? View.GONE : View.VISIBLE);
        cardImcActual.setVisibility(vacio ? View.GONE : View.VISIBLE);
        cardAnalisisFisico.setVisibility(vacio ? View.GONE : View.VISIBLE);

        if (!vacio) {
            RegistroFisico ultimo = historial.get(historial.size() - 1);
            textImcActual.setText(String.format(Locale.US, "%.1f (%s)", ultimo.imc,
                    PerfilFisicoUtil.clasificarImc(ultimo.imc)));
            textImcActual.setTextColor(ContextCompat.getColor(this, PerfilFisicoUtil.colorParaImc(ultimo.imc)));
            textFechaUltimoRegistro.setText("Último registro: " + LocalDate.parse(ultimo.fecha).format(FORMATO_FECHA));

            actualizarAnalisisFisico(ultimo);
        }

        List<RegistroFisico> ordenDescendente = new ArrayList<>(historial);
        Collections.reverse(ordenDescendente);
        recyclerView.setAdapter(new PhysicalHistoryAdapter(ordenDescendente));
    }

    private void actualizarAnalisisFisico(RegistroFisico r) {
        double icc = PerfilFisicoUtil.calcularIcc(r.cinturaCm, r.caderaCm);
        textIcc.setText(String.format(Locale.US, "Índice cintura-cadera: %.2f (%s)", icc,
                PerfilFisicoUtil.clasificarIcc(icc, r.tipoCuerpo)));
        textIcc.setTextColor(ContextCompat.getColor(this, PerfilFisicoUtil.colorParaIcc(icc, r.tipoCuerpo)));

        double icEst = PerfilFisicoUtil.calcularIcEst(r.cinturaCm, r.alturaCm);
        textIcEst.setText(String.format(Locale.US, "Índice cintura-altura: %.2f (%s)", icEst,
                PerfilFisicoUtil.clasificarIcEst(icEst)));
        textIcEst.setTextColor(ContextCompat.getColor(this, PerfilFisicoUtil.colorParaIcEst(icEst)));

        double vTaper = PerfilFisicoUtil.calcularVTaper(r.pechoCm, r.cinturaCm);
        textVTaper.setText(String.format(Locale.US, "Contraste pecho-cintura: %.2f (%s)", vTaper,
                PerfilFisicoUtil.clasificarVTaper(vTaper)));
        textVTaper.setTextColor(ContextCompat.getColor(this, PerfilFisicoUtil.colorParaVTaper(vTaper)));

        StringBuilder recomendaciones = new StringBuilder();
        List<String> sugerencias = PerfilFisicoUtil.generarRecomendaciones(r);
        for (int i = 0; i < sugerencias.size(); i++) {
            if (i > 0) {
                recomendaciones.append("\n\n");
            }
            recomendaciones.append("• ").append(sugerencias.get(i));
        }
        textRecomendaciones.setText(recomendaciones.toString());
    }
}
