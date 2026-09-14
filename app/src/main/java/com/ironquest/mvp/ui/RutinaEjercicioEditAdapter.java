package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class RutinaEjercicioEditAdapter extends RecyclerView.Adapter<RutinaEjercicioEditAdapter.ViewHolder> {

    public interface Listener {
        void onQuitar(int position);
        void onEditarPersonalizado(int position);
        void onIniciarArrastre(RecyclerView.ViewHolder viewHolder);
        /** Alguna mutación al catálogo (p. ej. override de incremento por ejercicio) fue aplicada. */
        void onCatalogoModificado();
    }

    private static final String[] OPCIONES_ESQUEMA =
            {"Manual", "Lineal", "Greyskull (AMRAP)", "Doble progresión", "Automática"};

    private final List<RutinaEjercicio> items;
    private final Map<String, Ejercicio> catalogoPorId;
    private final double incrementoGlobal;
    private final Listener listener;

    /**
     * Estado transitorio por fila (gif oculto / info expandida), no persistido a datos.json.
     * Se usa identidad de instancia y no el String {@code ejercicioId}: un usuario PUEDE agregar
     * el mismo ejercicio dos veces a la misma rutina, y con un {@code Set<String>} las dos filas
     * quedarían acopladas. La identidad del objeto sobrevive al drag-reorder del ItemTouchHelper.
     */
    private final Set<RutinaEjercicio> gifOcultoItems =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<RutinaEjercicio> infoExpandidoItems =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public RutinaEjercicioEditAdapter(List<RutinaEjercicio> items, Map<String, Ejercicio> catalogoPorId,
                                      double incrementoGlobal, Listener listener) {
        this.items = items;
        this.catalogoPorId = catalogoPorId;
        this.incrementoGlobal = incrementoGlobal;
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
        holder.botonInfo.setOnClickListener(v -> alternarEnSet(infoExpandidoItems, holder));
        holder.botonToggleGif.setOnClickListener(v -> alternarEnSet(gifOcultoItems, holder));
    }

    private void alternarEnSet(Set<RutinaEjercicio> set, ViewHolder holder) {
        int adapterPos = holder.getBindingAdapterPosition();
        if (adapterPos != RecyclerView.NO_POSITION) {
            RutinaEjercicio item = items.get(adapterPos);
            if (set.contains(item)) {
                set.remove(item);
            } else {
                set.add(item);
            }
            notifyItemChanged(adapterPos);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView manija;
        private final TextView nombre;
        private final ImageButton botonEditarNombre;
        private final TextView textAvisoMigracion;
        private final EditText series;
        private final EditText repeticiones;
        private final EditText peso;
        private final Spinner esquemaProgresion;
        private final EditText repeticionesMax;
        private final ImageView botonInfoProgresion;
        private final CheckBox checkSilenciar;
        private final ChipGroup chipsIncremento;
        private final ImageButton botonQuitar;
        private final LinearLayout rowBadgeInfo;
        private final TextView badgeMusculo;
        private final ImageButton botonInfo;
        private final ImageButton botonToggleGif;
        private final GifImageView gif;
        private final LinearLayout containerInfo;
        private final TextView textMusculoInfo;
        private final TextView textSecundariosInfo;
        private final TextView textInstruccionesInfo;
        private RutinaEjercicio current;
        private Ejercicio ejercicioActual;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            manija = itemView.findViewById(R.id.icon_arrastrar_ejercicio);
            nombre = itemView.findViewById(R.id.text_nombre_ejercicio);
            botonEditarNombre = itemView.findViewById(R.id.button_editar_nombre_ejercicio);
            textAvisoMigracion = itemView.findViewById(R.id.text_aviso_migracion);
            series = itemView.findViewById(R.id.edit_series);
            repeticiones = itemView.findViewById(R.id.edit_repeticiones);
            peso = itemView.findViewById(R.id.edit_peso);
            esquemaProgresion = itemView.findViewById(R.id.spinner_esquema_progresion);
            repeticionesMax = itemView.findViewById(R.id.edit_repeticiones_max);
            botonInfoProgresion = itemView.findViewById(R.id.button_info_progresion);
            checkSilenciar = itemView.findViewById(R.id.check_silenciar_sugerencia);
            chipsIncremento = itemView.findViewById(R.id.chip_group_incremento_ejercicio);
            botonQuitar = itemView.findViewById(R.id.button_quitar_ejercicio);
            rowBadgeInfo = itemView.findViewById(R.id.row_badge_info_ejercicio_rutina);
            badgeMusculo = itemView.findViewById(R.id.badge_musculo_objetivo_rutina);
            botonInfo = itemView.findViewById(R.id.button_info_ejercicio_rutina);
            botonToggleGif = itemView.findViewById(R.id.button_toggle_gif_rutina);
            gif = itemView.findViewById(R.id.gif_ejercicio_rutina);
            containerInfo = itemView.findViewById(R.id.container_info_ejercicio_rutina);
            textMusculoInfo = itemView.findViewById(R.id.text_musculo_objetivo_info_rutina);
            textSecundariosInfo = itemView.findViewById(R.id.text_musculos_secundarios_info_rutina);
            textInstruccionesInfo = itemView.findViewById(R.id.text_instrucciones_info_rutina);

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

            botonInfoProgresion.setOnClickListener(v ->
                    InfoProgresionDialog.mostrar(itemView.getContext()));

            chipsIncremento.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty() || ejercicioActual == null) {
                    return;
                }
                int id = checkedIds.get(0);
                double nuevo;
                if (id == R.id.chip_incremento_1) {
                    nuevo = 1.0;
                } else if (id == R.id.chip_incremento_2_5) {
                    nuevo = 2.5;
                } else if (id == R.id.chip_incremento_5) {
                    nuevo = 5.0;
                } else {
                    nuevo = 0.0;
                }
                ejercicioActual.setIncrementoPeso(nuevo);
                listener.onCatalogoModificado();
            });
        }

        private void bind(RutinaEjercicio item, Ejercicio ejercicio) {
            current = null;
            ejercicioActual = null;
            nombre.setText(ejercicio != null ? ejercicio.getNombre() : "Ejercicio");
            botonEditarNombre.setVisibility(ejercicio != null && ejercicio.isPersonalizado()
                    ? View.VISIBLE : View.GONE);
            // Ejercicio pendiente de migración: viene de una versión anterior y no tiene ficha
            // técnica/GIF ni está marcado como creado por el usuario. Los personalizados y los
            // curados nunca muestran el aviso.
            boolean pendienteMigracion = ejercicio == null
                    || (!ejercicio.isPersonalizado() && !ejercicio.tieneFichaTecnica());
            textAvisoMigracion.setVisibility(pendienteMigracion ? View.VISIBLE : View.GONE);
            current = null;
            ejercicioActual = null;
            // Checkbox silenciar: limpiar listener previo por reciclaje antes de setChecked.
            checkSilenciar.setOnCheckedChangeListener(null);
            checkSilenciar.setChecked(item.isSilenciarSugerencia());
            checkSilenciar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (current != null && isChecked != current.isSilenciarSugerencia()) {
                    current.setSilenciarSugerencia(isChecked);
                }
            });

            // Chips de incremento por ejercicio (override de catálogo).
            chipsIncremento.clearCheck();
            double inc = ejercicio != null ? ejercicio.getIncrementoPeso() : 0.0;
            int chipId;
            if (inc == 1.0) {
                chipId = R.id.chip_incremento_1;
            } else if (inc == 2.5) {
                chipId = R.id.chip_incremento_2_5;
            } else if (inc == 5.0) {
                chipId = R.id.chip_incremento_5;
            } else {
                chipId = R.id.chip_incremento_global;
            }
            Chip chipGlobal = itemView.findViewById(R.id.chip_incremento_global);
            chipGlobal.setText("Global (" + formatearIncremento(incrementoGlobal) + " kg)");
            chipsIncremento.check(chipId);
            ejercicioActual = ejercicio;

            series.setText(String.valueOf(item.getSeries()));
            repeticiones.setText(String.valueOf(item.getRepeticiones()));
            peso.setText(String.valueOf(item.getPeso()));
            repeticionesMax.setText(String.valueOf(item.getRepeticionesMax()));
            esquemaProgresion.setSelection(item.getEsquemaProgresion());

            boolean tieneFicha = ejercicio != null && ejercicio.tieneFichaTecnica();

            if (ejercicio != null && ejercicio.getMusculoObjetivo() != null) {
                badgeMusculo.setVisibility(View.VISIBLE);
                badgeMusculo.setText(ejercicio.getMusculoObjetivo());
            } else {
                badgeMusculo.setVisibility(View.GONE);
            }

            boolean hayAlgo = ejercicio != null && (ejercicio.getMusculoObjetivo() != null
                    || tieneFicha
                    || !ejercicio.getMusculosSecundarios().isEmpty()
                    || !ejercicio.getInstrucciones().isEmpty());
            rowBadgeInfo.setVisibility(hayAlgo ? View.VISIBLE : View.GONE);
            botonToggleGif.setVisibility(tieneFicha ? View.VISIBLE : View.GONE);

            boolean gifOculto = gifOcultoItems.contains(item);
            if (tieneFicha && !gifOculto) {
                gif.setVisibility(View.VISIBLE);
                String asset = ejercicio.getGifAsset();
                if (!asset.equals(gif.getTag())) {
                    try {
                        gif.setImageDrawable(new GifDrawable(itemView.getContext().getAssets(), asset));
                        gif.setTag(asset);
                    } catch (IOException e) {
                        gif.setVisibility(View.GONE);
                    }
                }
            } else {
                gif.setVisibility(View.GONE);
            }

            boolean infoExpandido = infoExpandidoItems.contains(item);
            containerInfo.setVisibility(infoExpandido ? View.VISIBLE : View.GONE);
            if (infoExpandido && ejercicio != null) {
                boolean esVisible = ejercicio.getMusculoObjetivo() != null;
                textMusculoInfo.setText("Músculo objetivo: " + ejercicio.getMusculoObjetivo());
                textMusculoInfo.setVisibility(esVisible ? View.VISIBLE : View.GONE);

                boolean haySecundarios = !ejercicio.getMusculosSecundarios().isEmpty();
                textSecundariosInfo.setText("También trabaja: "
                        + String.join(", ", ejercicio.getMusculosSecundarios()));
                textSecundariosInfo.setVisibility(haySecundarios ? View.VISIBLE : View.GONE);

                boolean hayInstrucciones = !ejercicio.getInstrucciones().isEmpty();
                textInstruccionesInfo.setText(numerarInstrucciones(ejercicio.getInstrucciones()));
                textInstruccionesInfo.setVisibility(hayInstrucciones ? View.VISIBLE : View.GONE);
            }
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

    private static String formatearIncremento(double v) {
        if (v == Math.floor(v)) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }

    /** Función pura duplicada de {@link DetalleEjercicioDialog}, igual que los {@code parse*}. */
    private static String numerarInstrucciones(List<String> pasos) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < pasos.size(); i++) {
            if (i > 0) {
                texto.append('\n');
            }
            texto.append(i + 1).append(". ").append(pasos.get(i));
        }
        return texto.toString();
    }
}
