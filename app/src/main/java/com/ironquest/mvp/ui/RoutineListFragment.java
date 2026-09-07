package com.ironquest.mvp.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.service.SesionTrackingService;
import com.ironquest.mvp.util.PerfilFisicoUtil;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** Pestaña "Entrenar": lista de rutinas, sesión en curso y recordatorio de registro físico. */
public class RoutineListFragment extends Fragment implements RutinaAdapter.Listener {

    private DataManager dataManager;
    private RutinaAdapter adapter;
    private RecyclerView recyclerView;
    private TextView textEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_routine_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataManager = DataManager.getInstance(requireContext());

        recyclerView = view.findViewById(R.id.recycler_rutinas);
        textEmpty = view.findViewById(R.id.text_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RutinaAdapter(dataManager.getDataStore().getRutinas(), this);
        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_nueva_rutina);
        fab.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditRoutineActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        actualizarEstadoVacio();
        actualizarTarjetaSesionEnCurso();
        actualizarTarjetaRecordatorioFisico();
    }

    private void actualizarEstadoVacio() {
        boolean vacio = dataManager.getDataStore().getRutinas().isEmpty();
        textEmpty.setVisibility(vacio ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }

    private void actualizarTarjetaRecordatorioFisico() {
        View raiz = requireView();
        View card = raiz.findViewById(R.id.card_recordatorio_fisico);
        List<RegistroFisico> historial = dataManager.getDataStore().getHistorialFisico();
        boolean necesitaActualizar = PerfilFisicoUtil.necesitaActualizacion(historial);
        card.setVisibility(necesitaActualizar ? View.VISIBLE : View.GONE);
        if (necesitaActualizar) {
            TextView textDetalle = raiz.findViewById(R.id.text_recordatorio_fisico_detalle);
            textDetalle.setText(historial.isEmpty()
                    ? "Aún no has registrado tus medidas corporales."
                    : "Ya pasó un mes desde tu último registro físico.");
            raiz.findViewById(R.id.button_registrar_fisico).setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), PhysicalProfileActivity.class)));
        }
    }

    private void actualizarTarjetaSesionEnCurso() {
        View raiz = requireView();
        View card = raiz.findViewById(R.id.card_sesion_en_curso);
        Sesion enProgreso = dataManager.getDataStore().getSesionEnProgreso();
        if (enProgreso == null) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);
        long minutos = Duration.between(
                LocalDateTime.parse(enProgreso.getFechaHoraInicio()), LocalDateTime.now()).toMinutes();
        TextView textDetalle = raiz.findViewById(R.id.text_sesion_en_curso_detalle);
        textDetalle.setText(enProgreso.getRutinaNombre() + " · " + minutos + " min");
        raiz.findViewById(R.id.button_continuar_sesion)
                .setOnClickListener(v -> continuarSesionEnCurso());
    }

    private void continuarSesionEnCurso() {
        Sesion enProgreso = dataManager.getDataStore().getSesionEnProgreso();
        if (enProgreso == null) {
            return;
        }
        Intent intent = new Intent(requireContext(), ActiveSessionActivity.class);
        intent.putExtra(ActiveSessionActivity.EXTRA_RUTINA_ID, enProgreso.getRutinaId());
        intent.putExtra(ActiveSessionActivity.EXTRA_RESUMIR, true);
        startActivity(intent);
    }

    private void descartarSesionEnCurso() {
        dataManager.getDataStore().setSesionEnProgreso(null);
        dataManager.save();
        SesionTrackingService.detener(requireContext());
        actualizarTarjetaSesionEnCurso();
    }

    @Override
    public void onRutinaClick(Rutina rutina) {
        Sesion enProgreso = dataManager.getDataStore().getSesionEnProgreso();
        if (enProgreso != null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Ya tienes un entrenamiento en curso")
                    .setMessage("Tienes una sesión de \"" + enProgreso.getRutinaNombre() + "\" sin finalizar. ¿Qué quieres hacer?")
                    .setPositiveButton("Continuar la actual", (dialog, which) -> continuarSesionEnCurso())
                    .setNegativeButton("Descartar y empezar nueva", (dialog, which) -> {
                        descartarSesionEnCurso();
                        iniciarSesionNueva(rutina);
                    })
                    .setNeutralButton("Cancelar", null)
                    .show();
            return;
        }
        iniciarSesionNueva(rutina);
    }

    private void iniciarSesionNueva(Rutina rutina) {
        Intent intent = new Intent(requireContext(), ActiveSessionActivity.class);
        intent.putExtra(ActiveSessionActivity.EXTRA_RUTINA_ID, rutina.getId());
        startActivity(intent);
    }

    @Override
    public void onEditarClick(Rutina rutina) {
        Intent intent = new Intent(requireContext(), EditRoutineActivity.class);
        intent.putExtra(EditRoutineActivity.EXTRA_RUTINA_ID, rutina.getId());
        startActivity(intent);
    }

    @Override
    public void onEliminarClick(Rutina rutina) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar rutina")
                .setMessage("¿Eliminar \"" + rutina.getNombre() + "\"? Tu historial de sesiones ya registradas no se ve afectado.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    dataManager.getDataStore().getRutinas().remove(rutina);
                    dataManager.save();
                    adapter.notifyDataSetChanged();
                    actualizarEstadoVacio();
                    Toast.makeText(requireContext(), "Rutina eliminada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onCompartirClick(Rutina rutina) {
        try {
            File archivo = dataManager.exportarRutinaComoArchivo(rutina);
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", archivo);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartir rutina"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo compartir: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
