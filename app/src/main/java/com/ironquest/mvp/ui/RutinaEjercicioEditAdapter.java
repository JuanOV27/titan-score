package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.util.List;
import java.util.Map;

public class RutinaEjercicioEditAdapter extends RecyclerView.Adapter<RutinaEjercicioEditAdapter.ViewHolder> {

    public interface Listener {
        void onQuitar(int position);
    }

    private static final String[] OPCIONES_ESQUEMA =
            {"Manual", "Lineal", "Greyskull (AMRAP)", "Doble progresión"};

    private final List<RutinaEjercicio> items;
    private final Map<String, Ejercicio> catalogoPorId;
    private final Listener listener;

    public RutinaEjercicioEditAdapter(List<RutinaEjercicio> items, Map<String, Ejercicio> catalogoPorId, Listener listener) {
        this.items = items;
        this.catalogoPorId = catalogoPorId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio_rutina_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
        holder.botonQuitar.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                listener.onQuitar(adapterPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nombre;
        final EditText series;
        final EditText repeticiones;
        final EditText peso;
        final Spinner esquemaProgresion;
        final EditText repeticionesMax;
        final ImageButton botonQuitar;
        private RutinaEjercicio current;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.text_nombre_ejercicio);
            series = itemView.findViewById(R.id.edit_series);
            repeticiones = itemView.findViewById(R.id.edit_repeticiones);
            peso = itemView.findViewById(R.id.edit_peso);
            esquemaProgresion = itemView.findViewById(R.id.spinner_esquema_progresion);
            repeticionesMax = itemView.findViewById(R.id.edit_repeticiones_max);
            botonQuitar = itemView.findViewById(R.id.button_quitar_ejercicio);

            esquemaProgresion.setAdapter(new ArrayAdapter<>(itemView.getContext(),
                    android.R.layout.simple_spinner_dropdown_item, OPCIONES_ESQUEMA));

            series.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.series = parseIntOrZero(text);
                    }
                }
            });
            repeticiones.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.repeticiones = parseIntOrZero(text);
                    }
                }
            });
            peso.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.peso = parseDoubleOrZero(text);
                    }
                }
            });
            repeticionesMax.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.repeticionesMax = parseIntOrZero(text);
                    }
                }
            });
            esquemaProgresion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (current != null) {
                        current.esquemaProgresion = position;
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        void bind(RutinaEjercicio item) {
            current = null;
            Ejercicio ejercicio = catalogoPorId.get(item.ejercicioId);
            nombre.setText(ejercicio != null ? ejercicio.nombre : "Ejercicio");
            series.setText(String.valueOf(item.series));
            repeticiones.setText(String.valueOf(item.repeticiones));
            peso.setText(String.valueOf(item.peso));
            repeticionesMax.setText(String.valueOf(item.repeticionesMax));
            esquemaProgresion.setSelection(item.esquemaProgresion);
            current = item;
        }
    }

    private static int parseIntOrZero(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDoubleOrZero(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
