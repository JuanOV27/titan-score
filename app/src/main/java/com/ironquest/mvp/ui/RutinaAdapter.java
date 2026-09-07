package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
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
        void onCompartirClick(Rutina rutina);
        void onEliminarClick(Rutina rutina);
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
        holder.botonIniciar.setOnClickListener(v -> listener.onRutinaClick(rutina));
        holder.botonMasOpciones.setOnClickListener(v -> mostrarMenuOpciones(holder.botonMasOpciones, rutina));
    }

    private void mostrarMenuOpciones(View ancla, Rutina rutina) {
        PopupMenu menu = new PopupMenu(ancla.getContext(), ancla);
        menu.inflate(R.menu.menu_rutina_item);
        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_editar_rutina) {
                listener.onEditarClick(rutina);
                return true;
            }
            if (id == R.id.action_compartir_rutina) {
                listener.onCompartirClick(rutina);
                return true;
            }
            if (id == R.id.action_eliminar_rutina) {
                listener.onEliminarClick(rutina);
                return true;
            }
            return false;
        });
        menu.show();
    }

    @Override
    public int getItemCount() {
        return rutinas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nombre;
        private final TextView resumen;
        private final ImageButton botonIniciar;
        private final ImageButton botonMasOpciones;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.text_nombre_rutina);
            resumen = itemView.findViewById(R.id.text_resumen_rutina);
            botonIniciar = itemView.findViewById(R.id.button_iniciar_rutina);
            botonMasOpciones = itemView.findViewById(R.id.button_mas_opciones_rutina);
        }
    }
}
