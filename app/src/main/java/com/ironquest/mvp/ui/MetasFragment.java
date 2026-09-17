package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;

import java.util.List;

/**
 * Tab "Metas": lista de metas activas + historial. Se refresca en onResume() para reflejar
 * cambios hechos desde otras pantallas (ej. cumplimiento por sesión).
 */
public class MetasFragment extends Fragment {

    /** Extra opcional: id de meta a resaltar (viene de tap en notificación push). */
    public static final String EXTRA_META_DESTACADA_ID = "meta_destacada_id";

    private DataManager dataManager;
    private DataStore dataStore;
    private RecyclerView listActivas;
    private RecyclerView listHistorial;
    private TextView headerHist;
    private TextView headerContador;
    private View cardVacio;
    private MaterialButton botonNueva;
    private MetasActivasAdapter activasAdapter;
    private MetasHistorialAdapter historialAdapter;
    private String metaDestacadaPendiente;

    public static MetasFragment crear() {
        return new MetasFragment();
    }

    /** Compatibilidad hacia atrás con la firma anterior (MainActivity.crear("Metas — próximamente")). */
    public static MetasFragment crear(String mensajeIgnorado) {
        return new MetasFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_metas, container, false);
        dataManager = DataManager.getInstance(requireContext());
        dataStore = dataManager.getDataStore();

        headerContador = v.findViewById(R.id.text_metas_header);
        botonNueva = v.findViewById(R.id.button_nueva_meta);
        cardVacio = v.findViewById(R.id.card_estado_vacio);
        listActivas = v.findViewById(R.id.list_metas_activas);
        listHistorial = v.findViewById(R.id.list_metas_historial);
        headerHist = v.findViewById(R.id.header_historial);

        listActivas.setLayoutManager(new LinearLayoutManager(getContext()));
        listHistorial.setLayoutManager(new LinearLayoutManager(getContext()));

        botonNueva.setOnClickListener(view -> abrirCrearMeta());
        headerHist.setOnClickListener(view -> toggleHistorial());

        // Extra desde intent (notificación push): resaltar meta
        Bundle args = getArguments();
        if (args != null) metaDestacadaPendiente = args.getString(EXTRA_META_DESTACADA_ID);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        refrescar();
        if (metaDestacadaPendiente != null && activasAdapter != null) {
            activasAdapter.resaltar(metaDestacadaPendiente);
            metaDestacadaPendiente = null;
        }
    }

    private void refrescar() {
        List<Meta> activas = dataManager.getMetasActivas();
        List<Meta> historial = dataManager.getMetasHistorial();

        headerContador.setText("Metas · " + activas.size() + "/3 activas");
        botonNueva.setEnabled(activas.size() < 3);

        boolean vacio = activas.isEmpty() && historial.isEmpty();
        cardVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);

        activasAdapter = new MetasActivasAdapter(activas, dataStore,
                new MetasActivasAdapter.Listener() {
                    @Override public void onVerPlan(Meta meta) { abrirPlanSugerido(meta); }
                    @Override public void onEditar(Meta meta)  { abrirEditar(meta); }
                    @Override public void onDescartar(Meta meta){ confirmarDescartar(meta); }
                });
        listActivas.setAdapter(activasAdapter);

        historialAdapter = new MetasHistorialAdapter(historial);
        listHistorial.setAdapter(historialAdapter);
        headerHist.setText("Historial (" + historial.size() + ") "
                + (listHistorial.getVisibility() == View.VISIBLE ? "▾" : "▸"));
    }

    private void toggleHistorial() {
        boolean visible = listHistorial.getVisibility() == View.VISIBLE;
        listHistorial.setVisibility(visible ? View.GONE : View.VISIBLE);
        int hist = dataManager.getMetasHistorial().size();
        headerHist.setText("Historial (" + hist + ") " + (visible ? "▸" : "▾"));
    }

    // ---- placeholders para las Tasks 8-10 ----

    private void abrirCrearMeta() {
        CrearMetaDialog.mostrar(requireActivity(), dataManager, this::refrescar);
    }

    private void abrirPlanSugerido(Meta meta) {
        PlanSugeridoDialog.mostrar(requireActivity(), dataManager, meta, this::refrescar);
    }

    private void abrirEditar(Meta meta) {
        // Se reemplaza en Task 10.
        Toast.makeText(getContext(), "Editar meta (próximamente)", Toast.LENGTH_SHORT).show();
    }

    private void confirmarDescartar(Meta meta) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Descartar meta")
                .setMessage("¿Seguro que quieres descartar \"" + meta.getTitulo() + "\"? Podrás verla en el historial.")
                .setPositiveButton("Descartar", (d, w) -> {
                    dataManager.descartarMeta(meta.getId());
                    refrescar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
