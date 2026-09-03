package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.Sesion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        configurarToolbar(R.id.toolbar, "Historial", true);

        DataManager dataManager = DataManager.getInstance(this);
        List<Sesion> sesiones = new ArrayList<>(dataManager.getDataStore().getSesiones());
        Collections.sort(sesiones, (a, b) -> b.getFechaHoraInicio().compareTo(a.getFechaHoraInicio()));

        RecyclerView recyclerView = findViewById(R.id.recycler_historial);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new HistorialAdapter(sesiones));

        TextView textEmpty = findViewById(R.id.text_empty_historial);
        boolean vacio = sesiones.isEmpty();
        textEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }
}
