package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Rutina;

import java.util.List;

public class RutinaAdapter extends RecyclerView.Adapter<RutinaAdapter.ViewHolder> {

    public interface Listener {
        void onRutinaClick(Rutina rutina);
        void onEditarClick(Rutina rutina);
    }

    private final List<Rutina> rutinas;
    private final Listener listener;

    public RutinaAdapter(List<Rutina> rutinas, Listener listener) {
        this.rutinas = rutinas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rutina, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Rutina rutina = rutinas.get(position);
        holder.nombre.setText(rutina.getNombre());
        int cantidadEjercicios = rutina.getCantidadEjercicios();
        holder.resumen.setText(cantidadEjercicios == 1
                ? "1 ejercicio"
                : cantidadEjercicios + " ejercicios");
        holder.itemView.setOnClickListener(v -> listener.onRutinaClick(rutina));
        holder.botonEditar.setOnClickListener(v -> listener.onEditarClick(rutina));
    }

    @Override
    public int getItemCount() {
        return rutinas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final TextView resumen;
        final ImageButton botonEditar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.text_nombre_rutina);
            resumen = itemView.findViewById(R.id.text_resumen_rutina);
            botonEditar = itemView.findViewById(R.id.button_editar_rutina);
        }
    }
}
