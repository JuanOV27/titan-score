package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.MotionEvent;
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
        void onEditarPersonalizado(int position);
        void onIniciarArrastre(RecyclerView.ViewHolder viewHolder);
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
        holder.bind(items.get(position), catalogoPorId.get(items.get(position).getEjercicioId()));
        holder.botonQuitar.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                listener.onQuitar(adapterPos);
            }
        });
        holder.botonEditarNombre.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                listener.onEditarPersonalizado(adapterPos);
            }
        });
        holder.manija.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                listener.onIniciarArrastre(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView manija;
        private final TextView nombre;
        private final ImageButton botonEditarNombre;
        private final EditText series;
        private final EditText repeticiones;
        private final EditText peso;
        private final Spinner esquemaProgresion;
        private final EditText repeticionesMax;
        private final ImageButton botonQuitar;
        private RutinaEjercicio current;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            manija = itemView.findViewById(R.id.icon_arrastrar_ejercicio);
            nombre = itemView.findViewById(R.id.text_nombre_ejercicio);
            botonEditarNombre = itemView.findViewById(R.id.button_editar_nombre_ejercicio);
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
                        current.setSeries(parseIntOrZero(text));
                    }
                }
            });
            repeticiones.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.setRepeticiones(parseIntOrZero(text));
                    }
                }
            });
            peso.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.setPeso(parseDoubleOrZero(text));
                    }
                }
            });
            repeticionesMax.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.setRepeticionesMax(parseIntOrZero(text));
                    }
                }
            });
            esquemaProgresion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (current != null) {
                        current.setEsquemaProgresion(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        private void bind(RutinaEjercicio item, Ejercicio ejercicio) {
            current = null;
            nombre.setText(ejercicio != null ? ejercicio.getNombre() : "Ejercicio");
            botonEditarNombre.setVisibility(ejercicio != null && ejercicio.isPersonalizado()
                    ? View.VISIBLE : View.GONE);
            series.setText(String.valueOf(item.getSeries()));
            repeticiones.setText(String.valueOf(item.getRepeticiones()));
            peso.setText(String.valueOf(item.getPeso()));
            repeticionesMax.setText(String.valueOf(item.getRepeticionesMax()));
            esquemaProgresion.setSelection(item.getEsquemaProgresion());
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
