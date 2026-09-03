package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Sesion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        configurarToolbar(R.id.toolbar, "Historial", true);

        DataManager dataManager = DataManager.getInstance(this);
        DataStore dataStore = dataManager.getDataStore();
        List<Sesion> sesiones = new ArrayList<>(dataStore.getSesiones());
        Collections.sort(sesiones, (a, b) -> b.getFechaHoraInicio().compareTo(a.getFechaHoraInicio()));

        Map<String, Ejercicio> catalogoPorId = new HashMap<>();
        for (Ejercicio ejercicio : dataStore.getEjercicios()) {
            catalogoPorId.put(ejercicio.getId(), ejercicio);
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_historial);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new HistorialAdapter(sesiones, catalogoPorId));

        TextView textEmpty = findViewById(R.id.text_empty_historial);
        boolean vacio = sesiones.isEmpty();
        textEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }
}
