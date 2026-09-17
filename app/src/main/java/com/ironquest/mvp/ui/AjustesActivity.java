package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.chip.ChipGroup;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.Usuario;

public class AjustesActivity extends BaseActivity {

    private DataManager dataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        configurarToolbar(R.id.toolbar, "Ajustes", true);

        dataManager = DataManager.getInstance(this);
        Usuario usuario = dataManager.getDataStore().getUsuario();

        ChipGroup chips = findViewById(R.id.chip_group_incremento);
        double actual = usuario != null ? usuario.getIncrementoPeso() : 2.5;
        if (actual == 1.0) {
            chips.check(R.id.chip_1);
        } else if (actual == 5.0) {
            chips.check(R.id.chip_5);
        } else {
            chips.check(R.id.chip_2_5);
        }
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || usuario == null) {
                return;
            }
            int id = checkedIds.get(0);
            double nuevo;
            if (id == R.id.chip_1) {
                nuevo = 1.0;
            } else if (id == R.id.chip_5) {
                nuevo = 5.0;
            } else {
                nuevo = 2.5;
            }
            usuario.setIncrementoPeso(nuevo);
            dataManager.save();
        });

        com.google.android.material.materialswitch.MaterialSwitch switchFatiga =
                findViewById(R.id.switch_seguimiento_fatiga);
        switchFatiga.setChecked(usuario != null && usuario.isSeguimientoFatiga());
        switchFatiga.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (usuario == null) return;
            usuario.setSeguimientoFatiga(isChecked);
            dataManager.save();
        });

        TextView headerConvencion = findViewById(R.id.header_convencion);
        View bodyConvencion = findViewById(R.id.body_convencion);
        headerConvencion.setOnClickListener(v -> {
            bodyConvencion.setVisibility(
                    bodyConvencion.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            headerConvencion.setText(bodyConvencion.getVisibility() == View.VISIBLE
                    ? "Cómo registro el peso ▾"
                    : "Cómo registro el peso ▸");
        });

        findViewById(R.id.link_info_progresion).setOnClickListener(v ->
                InfoProgresionDialog.mostrar(AjustesActivity.this));

        com.google.android.material.chip.ChipGroup chipsDias = findViewById(R.id.chip_group_dias_entreno);
        int diasActual = usuario != null ? usuario.getDiasEntrenoSemana() : 3;
        int idInicial;
        switch (diasActual) {
            case 1: idInicial = R.id.chip_dias_1; break;
            case 2: idInicial = R.id.chip_dias_2; break;
            case 4: idInicial = R.id.chip_dias_4; break;
            case 5: idInicial = R.id.chip_dias_5; break;
            case 6: idInicial = R.id.chip_dias_6; break;
            case 7: idInicial = R.id.chip_dias_7; break;
            case 3:
            default: idInicial = R.id.chip_dias_3; break;
        }
        chipsDias.check(idInicial);
        chipsDias.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty() || usuario == null) return;
            int id = checkedIds.get(0);
            int nuevo;
            if (id == R.id.chip_dias_1) nuevo = 1;
            else if (id == R.id.chip_dias_2) nuevo = 2;
            else if (id == R.id.chip_dias_4) nuevo = 4;
            else if (id == R.id.chip_dias_5) nuevo = 5;
            else if (id == R.id.chip_dias_6) nuevo = 6;
            else if (id == R.id.chip_dias_7) nuevo = 7;
            else nuevo = 3;
            if (nuevo != usuario.getDiasEntrenoSemana()) {
                usuario.setDiasEntrenoSemana(nuevo);
                dataManager.save();
            }
        });
    }
}