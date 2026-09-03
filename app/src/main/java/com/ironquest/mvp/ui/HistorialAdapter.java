package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.SerieSesion;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final List<Sesion> sesiones;
    private final Map<String, Ejercicio> catalogoPorId;
    private final Set<String> expandidas = new HashSet<>();

    public HistorialAdapter(List<Sesion> sesiones, Map<String, Ejercicio> catalogoPorId) {
        this.sesiones = sesiones;
        this.catalogoPorId = catalogoPorId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sesion_historial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sesion sesion = sesiones.get(position);
        holder.rutina.setText(sesion.getRutinaNombre() != null ? sesion.getRutinaNombre() : "Rutina");

        LocalDateTime inicio = LocalDateTime.parse(sesion.getFechaHoraInicio());
        holder.fecha.setText(inicio.format(FORMATO_FECHA));

        if (sesion.estaFinalizada()) {
            LocalDateTime fin = LocalDateTime.parse(sesion.getFechaHoraFin());
            long minutos = Duration.between(inicio, fin).toMinutes();
            holder.duracion.setText("Duración: " + minutos + " min");
        } else {
            holder.duracion.setText("Duración: --");
        }

        holder.cumplimiento.setText("Cumplimiento: " + sesion.getPorcentajeCumplimiento() + "%");
        int colorRes = sesion.getPorcentajeCumplimiento() >= 100
                ? R.color.cumplimiento_alto
                : sesion.getPorcentajeCumplimiento() >= 60
                ? R.color.cumplimiento_medio
                : R.color.cumplimiento_bajo;
        holder.cumplimiento.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorRes));

        boolean expandida = expandidas.contains(sesion.getId());
        actualizarDetalle(holder, sesion, expandida);

        holder.itemView.setOnClickListener(v -> {
            boolean nuevoEstado = !expandidas.contains(sesion.getId());
            if (nuevoEstado) {
                expandidas.add(sesion.getId());
            } else {
                expandidas.remove(sesion.getId());
            }
            actualizarDetalle(holder, sesion, nuevoEstado);
        });
    }

    private void actualizarDetalle(ViewHolder holder, Sesion sesion, boolean expandida) {
        holder.icono.setRotation(expandida ? 180f : 0f);
        holder.detalle.setVisibility(expandida ? View.VISIBLE : View.GONE);
        holder.detalle.removeAllViews();
        if (!expandida) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
        for (EjercicioSesion ejercicioSesion : sesion.getEjercicios()) {
            View bloque = inflater.inflate(R.layout.view_ejercicio_historial_detalle, holder.detalle, false);
            TextView nombre = bloque.findViewById(R.id.text_nombre_ejercicio_detalle);
            TextView series = bloque.findViewById(R.id.text_series_detalle);

            Ejercicio ejercicio = catalogoPorId.get(ejercicioSesion.getEjercicioId());
            nombre.setText(ejercicio != null ? ejercicio.getNombre() : "Ejercicio");
            series.setText(formatearSeries(ejercicioSesion.getSeries()));

            holder.detalle.addView(bloque);
        }
    }

    private static String formatearSeries(List<SerieSesion> series) {
        StringBuilder texto = new StringBuilder();
        for (SerieSesion serie : series) {
            if (texto.length() > 0) {
                texto.append('\n');
            }
            texto.append("Serie ").append(serie.getNumero()).append(": ")
                    .append(formatearPeso(serie.getPeso())).append(" kg × ")
                    .append(serie.getRepeticiones()).append(" reps");
            if (!serie.isCompletada()) {
                texto.append(" — no la hiciste");
            }
        }
        return texto.toString();
    }

    private static String formatearPeso(double peso) {
        if (peso == Math.floor(peso)) {
            return String.valueOf((long) peso);
        }
        return String.valueOf(peso);
    }

    @Override
    public int getItemCount() {
        return sesiones.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView rutina;
        private final TextView fecha;
        private final TextView duracion;
        private final TextView cumplimiento;
        private final ImageView icono;
        private final LinearLayout detalle;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            rutina = itemView.findViewById(R.id.text_rutina_sesion);
            fecha = itemView.findViewById(R.id.text_fecha_sesion);
            duracion = itemView.findViewById(R.id.text_duracion_sesion);
            cumplimiento = itemView.findViewById(R.id.text_cumplimiento_sesion);
            icono = itemView.findViewById(R.id.icon_expandir_sesion);
            detalle = itemView.findViewById(R.id.container_detalle_sesion);
        }
    }
}
