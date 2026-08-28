package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Sesion;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final List<Sesion> sesiones;

    public HistorialAdapter(List<Sesion> sesiones) {
        this.sesiones = sesiones;
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
        holder.rutina.setText(sesion.rutinaNombre != null ? sesion.rutinaNombre : "Rutina");

        LocalDateTime inicio = LocalDateTime.parse(sesion.fechaHoraInicio);
        holder.fecha.setText(inicio.format(FORMATO_FECHA));

        if (sesion.fechaHoraFin != null) {
            LocalDateTime fin = LocalDateTime.parse(sesion.fechaHoraFin);
            long minutos = Duration.between(inicio, fin).toMinutes();
            holder.duracion.setText("Duración: " + minutos + " min");
        } else {
            holder.duracion.setText("Duración: --");
        }

        holder.cumplimiento.setText("Cumplimiento: " + sesion.porcentajeCumplimiento + "%");
        int colorRes = sesion.porcentajeCumplimiento >= 100
                ? R.color.cumplimiento_alto
                : sesion.porcentajeCumplimiento >= 60
                ? R.color.cumplimiento_medio
                : R.color.cumplimiento_bajo;
        holder.cumplimiento.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), colorRes));
    }

    @Override
    public int getItemCount() {
        return sesiones.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView rutina;
        final TextView fecha;
        final TextView duracion;
        final TextView cumplimiento;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rutina = itemView.findViewById(R.id.text_rutina_sesion);
            fecha = itemView.findViewById(R.id.text_fecha_sesion);
            duracion = itemView.findViewById(R.id.text_duracion_sesion);
            cumplimiento = itemView.findViewById(R.id.text_cumplimiento_sesion);
        }
    }
}
