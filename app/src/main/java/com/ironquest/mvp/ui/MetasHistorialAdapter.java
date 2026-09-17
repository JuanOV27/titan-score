package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Meta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MetasHistorialAdapter extends RecyclerView.Adapter<MetasHistorialAdapter.VH> {

    private final List<Meta> items;

    public MetasHistorialAdapter(List<Meta> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meta_historial, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView estado, titulo, resultado, duracion;

        VH(@NonNull View itemView) {
            super(itemView);
            estado = itemView.findViewById(R.id.text_hist_estado);
            titulo = itemView.findViewById(R.id.text_hist_titulo);
            resultado = itemView.findViewById(R.id.text_hist_resultado);
            duracion = itemView.findViewById(R.id.text_hist_duracion);
        }

        void bind(Meta m) {
            boolean cumplida = m.getEstado() == Meta.ESTADO_CUMPLIDA;
            estado.setText(cumplida ? "✓" : "✗");
            estado.setTextColor(cumplida ? 0xFF2E7D32 : 0xFF757575);
            titulo.setText(m.getTitulo());
            resultado.setText(cumplida
                    ? "Cumplida"
                    : "Descartada");

            // Duración desde creación hasta cierre
            String fechaCierre = cumplida ? m.getFechaCumplida() : m.getFechaDescartada();
            if (fechaCierre != null && m.getFechaCreacion() != null) {
                try {
                    LocalDate creada = LocalDateTime.parse(m.getFechaCreacion()).toLocalDate();
                    LocalDate cerrada = LocalDateTime.parse(fechaCierre).toLocalDate();
                    long dias = ChronoUnit.DAYS.between(creada, cerrada);
                    duracion.setText((cumplida ? "En " : "Descartada tras ") + dias + " días");
                } catch (Exception e) {
                    duracion.setText("");
                }
            } else {
                duracion.setText("");
            }
        }
    }
}
