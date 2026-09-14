package com.ironquest.mvp.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.util.progresion.Sugerencia;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

/**
 * Páginas de la sesión activa: una card por ejercicio, con badge de músculo objetivo, GIF de
 * técnica con toggle, info expandible y el registro de series. Mutar la lista viva de
 * {@code sesionActual.getEjercicios()} + el {@code notify*} correspondiente es lo que mantiene
 * el pager al día.
 *
 * <p>El estado transitorio (gif oculto / info expandida) vive en {@link Set}s por identidad de
 * instancia de {@link EjercicioSesion}: el mismo ejercicio puede aparecer dos veces en la sesión
 * y con un {@code Set<String>} las dos páginas quedarían acopladas.
 */
public class EjercicioSesionPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onCambiarEjercicio(int position);
        void onEliminarEjercicio(int position);
        void onProgresoModificado();
        void onFinalizarSesion();
    }

    private static final long HOLD_FINALIZAR_MS = 3000L;
    private static final int VIEW_TYPE_EJERCICIO = 0;
    private static final int VIEW_TYPE_FINALIZAR = 1;
    private static final double PASO_PESO = 2.5;

    private final List<EjercicioSesion> items;
    private final Map<String, Ejercicio> catalogoPorId;
    private final Map<EjercicioSesion, Sugerencia> sugerenciasPorItem;
    private final Listener listener;
    private final Set<EjercicioSesion> gifOcultoItems =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<EjercicioSesion> infoExpandidoItems =
            Collections.newSetFromMap(new IdentityHashMap<>());

    public EjercicioSesionPagerAdapter(List<EjercicioSesion> items, Map<String, Ejercicio> catalogoPorId,
                                       Map<EjercicioSesion, Sugerencia> sugerenciasPorItem, Listener listener) {
        this.items = items;
        this.catalogoPorId = catalogoPorId;
        this.sugerenciasPorItem = sugerenciasPorItem;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FINALIZAR) {
            return new FinalizarViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_finalizar_sesion_block, parent, false));
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_ejercicio_sesion_block, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FinalizarViewHolder) {
            ((FinalizarViewHolder) holder).bind();
            return;
        }
        ViewHolder viewHolder = (ViewHolder) holder;
        EjercicioSesion ejercicioSesion = items.get(position);
        viewHolder.bind(ejercicioSesion, catalogoPorId.get(ejercicioSesion.getEjercicioId()),
                sugerenciasPorItem.get(ejercicioSesion));
    }

    @Override
    public int getItemViewType(int position) {
        return position == items.size() ? VIEW_TYPE_FINALIZAR : VIEW_TYPE_EJERCICIO;
    }

    @Override
    public int getItemCount() {
        return items.size() + 1;
    }

    /** Refresca la card de finalizar (resumen de series) tras tocar el progreso de la sesión. */
    public void refrescarCardFinal() {
        notifyItemChanged(getItemCount() - 1);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final LayoutInflater inflater;
        private final TextView nombre;
        private final LinearLayout rowInfoGif;
        private final TextView badge;
        private final ImageButton botonInfo;
        private final ImageButton botonToggleGif;
        private final GifImageView gif;
        private final LinearLayout containerInfo;
        private final TextView textMusculoInfo;
        private final TextView textSecundariosInfo;
        private final TextView textInstruccionesTituloInfo;
        private final TextView textInstruccionesInfo;
        private final TextView textExplicacion;
        private final LinearLayout containerSeries;
        private final MaterialButton botonAgregarSerie;
        private final ImageButton botonCambiar;
        private final ImageButton botonEliminar;
        private EjercicioSesion currentEjercicio;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            inflater = LayoutInflater.from(itemView.getContext());
            nombre = itemView.findViewById(R.id.text_nombre_ejercicio_sesion);
            rowInfoGif = itemView.findViewById(R.id.row_info_gif_ejercicio_sesion);
            badge = itemView.findViewById(R.id.badge_musculo_objetivo_sesion);
            botonInfo = itemView.findViewById(R.id.button_info_ejercicio_sesion);
            botonToggleGif = itemView.findViewById(R.id.button_toggle_gif_sesion);
            gif = itemView.findViewById(R.id.gif_ejercicio_sesion);
            containerInfo = itemView.findViewById(R.id.container_info_ejercicio_sesion);
            textMusculoInfo = itemView.findViewById(R.id.text_musculo_objetivo_info_sesion);
            textSecundariosInfo = itemView.findViewById(R.id.text_musculos_secundarios_info_sesion);
            textInstruccionesTituloInfo = itemView.findViewById(R.id.text_instrucciones_titulo_info_sesion);
            textInstruccionesInfo = itemView.findViewById(R.id.text_instrucciones_info_sesion);
            textExplicacion = itemView.findViewById(R.id.text_explicacion_progresion);
            containerSeries = itemView.findViewById(R.id.container_series);
            botonAgregarSerie = itemView.findViewById(R.id.button_agregar_serie);
            botonCambiar = itemView.findViewById(R.id.button_cambiar_ejercicio);
            botonEliminar = itemView.findViewById(R.id.button_eliminar_ejercicio_sesion);

            botonInfo.setOnClickListener(v -> alternarExpansion(infoExpandidoItems));
            botonToggleGif.setOnClickListener(v -> alternarExpansion(gifOcultoItems));
            botonCambiar.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onCambiarEjercicio(position);
                }
            });
            botonEliminar.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onEliminarEjercicio(position);
                }
            });
            botonAgregarSerie.setOnClickListener(v -> {
                if (currentEjercicio == null) {
                    return;
                }
                SerieSesion ultima = currentEjercicio.getSeries().isEmpty()
                        ? new SerieSesion(0, 0.0, 10, true)
                        : currentEjercicio.getSeries().get(currentEjercicio.getSeries().size() - 1);
                SerieSesion nueva = new SerieSesion(currentEjercicio.getSeries().size() + 1,
                        ultima.getPeso(), ultima.getRepeticiones(), true);
                currentEjercicio.agregarSerie(nueva);
                containerSeries.addView(crearFilaSerie(currentEjercicio, nueva));
                listener.onProgresoModificado();
            });
        }

        private void alternarExpansion(Set<EjercicioSesion> set) {
            int position = getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION || currentEjercicio == null) {
                return;
            }
            if (set.contains(currentEjercicio)) {
                set.remove(currentEjercicio);
            } else {
                set.add(currentEjercicio);
            }
            EjercicioSesionPagerAdapter.this.notifyItemChanged(position);
        }

        private void bind(EjercicioSesion ejercicioSesion, Ejercicio ejercicio, Sugerencia sugerencia) {
            currentEjercicio = ejercicioSesion;
            nombre.setText(ejercicio != null ? ejercicio.getNombre() : "Ejercicio");

            boolean tieneFicha = ejercicio != null && ejercicio.tieneFichaTecnica();

            if (ejercicio != null && ejercicio.getMusculoObjetivo() != null) {
                badge.setVisibility(View.VISIBLE);
                badge.setText(ejercicio.getMusculoObjetivo());
            } else {
                badge.setVisibility(View.GONE);
            }

            boolean hayAlgo = ejercicio != null && (ejercicio.getMusculoObjetivo() != null
                    || tieneFicha
                    || !ejercicio.getMusculosSecundarios().isEmpty()
                    || !ejercicio.getInstrucciones().isEmpty());
            rowInfoGif.setVisibility(hayAlgo ? View.VISIBLE : View.GONE);
            botonToggleGif.setVisibility(tieneFicha ? View.VISIBLE : View.GONE);

            boolean gifOculto = gifOcultoItems.contains(ejercicioSesion);
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

            boolean infoExpandido = infoExpandidoItems.contains(ejercicioSesion);
            containerInfo.setVisibility(infoExpandido ? View.VISIBLE : View.GONE);
            if (infoExpandido && ejercicio != null) {
                boolean hayMusculo = ejercicio.getMusculoObjetivo() != null;
                textMusculoInfo.setText("Músculo objetivo: " + ejercicio.getMusculoObjetivo());
                textMusculoInfo.setVisibility(hayMusculo ? View.VISIBLE : View.GONE);

                boolean haySecundarios = !ejercicio.getMusculosSecundarios().isEmpty();
                textSecundariosInfo.setText("También trabaja: "
                        + String.join(", ", ejercicio.getMusculosSecundarios()));
                textSecundariosInfo.setVisibility(haySecundarios ? View.VISIBLE : View.GONE);

                boolean hayInstrucciones = !ejercicio.getInstrucciones().isEmpty();
                textInstruccionesTituloInfo.setVisibility(hayInstrucciones ? View.VISIBLE : View.GONE);
                textInstruccionesInfo.setText(numerarInstrucciones(ejercicio.getInstrucciones()));
                textInstruccionesInfo.setVisibility(hayInstrucciones ? View.VISIBLE : View.GONE);
            }

            if (sugerencia != null && sugerencia.getExplicacion() != null) {
                textExplicacion.setText(sugerencia.getExplicacion());
                textExplicacion.setTextColor(ContextCompat.getColor(itemView.getContext(),
                        sugerencia.isEstancado() ? R.color.cumplimiento_bajo : R.color.cumplimiento_medio));
                textExplicacion.setVisibility(View.VISIBLE);
            } else {
                textExplicacion.setVisibility(View.GONE);
            }

            redibujarSeries(ejercicioSesion);
        }

        private void redibujarSeries(EjercicioSesion ejercicioSesion) {
            containerSeries.removeAllViews();
            for (SerieSesion serie : ejercicioSesion.getSeries()) {
                containerSeries.addView(crearFilaSerie(ejercicioSesion, serie));
            }
        }

        private void eliminarSerieConDeshacer(EjercicioSesion ejercicioSesion, SerieSesion serie) {
            int posicion = ejercicioSesion.getSeries().indexOf(serie);
            ejercicioSesion.quitarSerie(serie);
            redibujarSeries(ejercicioSesion);
            listener.onProgresoModificado();
            vibrarConfirmacion();

            Snackbar.make(containerSeries, "Serie eliminada", Snackbar.LENGTH_LONG)
                    .setAction("Deshacer", v -> {
                        ejercicioSesion.insertarSerie(posicion, serie);
                        redibujarSeries(ejercicioSesion);
                        listener.onProgresoModificado();
                    })
                    .show();
        }

        private View crearFilaSerie(EjercicioSesion ejercicioSesion, SerieSesion serie) {
            View row = inflater.inflate(R.layout.view_serie_sesion_row, containerSeries, false);

            TextView textNumero = row.findViewById(R.id.text_numero_serie);
            EditText textPeso = row.findViewById(R.id.text_peso);
            EditText textReps = row.findViewById(R.id.text_reps);
            ImageButton botonPesoMenos = row.findViewById(R.id.button_peso_menos);
            ImageButton botonPesoMas = row.findViewById(R.id.button_peso_mas);
            ImageButton botonRepsMenos = row.findViewById(R.id.button_reps_menos);
            ImageButton botonRepsMas = row.findViewById(R.id.button_reps_mas);
            ImageButton botonCompletada = row.findViewById(R.id.button_completada);

            textNumero.setText("Serie " + serie.getNumero());
            textPeso.setText(formatearPeso(serie.getPeso()));
            textReps.setText(String.valueOf(serie.getRepeticiones()));
            actualizarIconoCompletada(botonCompletada, serie.isCompletada());

            textNumero.setOnLongClickListener(v -> {
                eliminarSerieConDeshacer(ejercicioSesion, serie);
                return true;
            });

            botonPesoMenos.setOnClickListener(v -> {
                serie.setPeso(Math.max(0, serie.getPeso() - PASO_PESO));
                textPeso.setText(formatearPeso(serie.getPeso()));
                listener.onProgresoModificado();
            });
            botonPesoMas.setOnClickListener(v -> {
                serie.setPeso(serie.getPeso() + PASO_PESO);
                textPeso.setText(formatearPeso(serie.getPeso()));
                listener.onProgresoModificado();
            });
            botonRepsMenos.setOnClickListener(v -> {
                serie.setRepeticiones(Math.max(0, serie.getRepeticiones() - 1));
                textReps.setText(String.valueOf(serie.getRepeticiones()));
                listener.onProgresoModificado();
            });
            botonRepsMas.setOnClickListener(v -> {
                serie.setRepeticiones(serie.getRepeticiones() + 1);
                textReps.setText(String.valueOf(serie.getRepeticiones()));
                listener.onProgresoModificado();
            });
            botonCompletada.setOnClickListener(v -> {
                serie.setCompletada(!serie.isCompletada());
                actualizarIconoCompletada(botonCompletada, serie.isCompletada());
                listener.onProgresoModificado();
            });

            textPeso.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    serie.setPeso(parseDoubleOrZero(text));
                    listener.onProgresoModificado();
                }
            });
            textReps.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    serie.setRepeticiones(parseIntOrZero(text));
                    listener.onProgresoModificado();
                }
            });

            return row;
        }

        private void actualizarIconoCompletada(ImageButton boton, boolean completada) {
            boton.setImageResource(completada
                    ? android.R.drawable.checkbox_on_background
                    : android.R.drawable.checkbox_off_background);
            boton.setContentDescription(completada
                    ? "Marcar que no hiciste esta serie"
                    : "Marcar que sí hiciste esta serie");
        }

        private void vibrarConfirmacion() {
            Context context = itemView.getContext();
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    class FinalizarViewHolder extends RecyclerView.ViewHolder {

        private final TextView textResumen;

        private FinalizarViewHolder(@NonNull View itemView) {
            super(itemView);
            textResumen = itemView.findViewById(R.id.text_resumen_card_final);
            MaterialButton botonFinalizar = itemView.findViewById(R.id.button_finalizar_sesion_card);
            LinearProgressIndicator progreso = itemView.findViewById(R.id.progress_finalizar_sesion_card);

            ValueAnimator animator = ValueAnimator.ofInt(0, 100);
            animator.setDuration(HOLD_FINALIZAR_MS);
            animator.addUpdateListener(a -> progreso.setProgress((int) a.getAnimatedValue()));
            animator.addListener(new AnimatorListenerAdapter() {
                private boolean cancelado;

                @Override
                public void onAnimationCancel(Animator animation) {
                    cancelado = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    boolean seCompleto = !cancelado;
                    cancelado = false;
                    if (seCompleto) {
                        vibrarConfirmacion();
                        listener.onFinalizarSesion();
                    }
                }
            });

            botonFinalizar.setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        progreso.setProgress(0);
                        progreso.setVisibility(View.VISIBLE);
                        animator.start();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        animator.cancel();
                        progreso.setProgress(0);
                        progreso.setVisibility(View.INVISIBLE);
                        return true;
                    default:
                        return false;
                }
            });
        }

        private void bind() {
            int series = 0;
            int completadas = 0;
            for (EjercicioSesion ejercicioSesion : items) {
                for (SerieSesion serie : ejercicioSesion.getSeries()) {
                    series++;
                    if (serie.isCompletada()) {
                        completadas++;
                    }
                }
            }
            textResumen.setText("Ejercicios: " + items.size()
                    + " · Series completadas: " + completadas + " de " + series);
        }

        private void vibrarConfirmacion() {
            Context context = itemView.getContext();
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private static double parseDoubleOrZero(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseIntOrZero(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
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

    private static String formatearPeso(double peso) {
        if (peso == Math.floor(peso)) {
            return String.valueOf((long) peso);
        }
        return String.valueOf(peso);
    }
}