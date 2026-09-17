package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.ironquest.mvp.R;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Meta;
import com.ironquest.mvp.util.metas.EvaluadorMeta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MetasActivasAdapter extends RecyclerView.Adapter<MetasActivasAdapter.VH> {

    public interface Listener {
        void onVerPlan(Meta meta);
        void onEditar(Meta meta);
        void onDescartar(Meta meta);
    }

    private final List<Meta> items;
    private final DataStore dataStore;
    private final Listener listener;
    /** Id de meta a resaltar por 3 s (viene de deeplink de notificación). */
    private String metaResaltadaId;

    public MetasActivasAdapter(List<Meta> items, DataStore dataStore, Listener listener) {
        this.items = items;
        this.dataStore = dataStore;
        this.listener = listener;
    }

    public void resaltar(String metaId) {
        this.metaResaltadaId = metaId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meta_activa, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class VH extends RecyclerView.ViewHolder {
        final TextView icono, titulo, progresoTxt, deadline, recordatorio;
        final LinearProgressIndicator progreso;
        final ImageButton overflow;
        final com.google.android.material.button.MaterialButton botonVerPlan;

        VH(@NonNull View itemView) {
            super(itemView);
            icono = itemView.findViewById(R.id.text_meta_icono);
            titulo = itemView.findViewById(R.id.text_meta_titulo);
            progresoTxt = itemView.findViewById(R.id.text_meta_progreso);
            deadline = itemView.findViewById(R.id.text_meta_deadline);
            recordatorio = itemView.findViewById(R.id.text_meta_recordatorio);
            progreso = itemView.findViewById(R.id.progress_meta);
            overflow = itemView.findViewById(R.id.button_meta_overflow);
            botonVerPlan = itemView.findViewById(R.id.button_ver_plan);
        }

        void bind(Meta m) {
            icono.setText(iconoDe(m.getTipo()));
            titulo.setText(m.getTitulo());

            EvaluadorMeta ev = EvaluadorMeta.para(m);
            double prog = ev.calcularProgreso(m, dataStore);
            progreso.setProgress((int) Math.round(prog * 100));
            progresoTxt.setText(ev.textoProgreso(m, dataStore));

            // Deadline
            if (m.getFechaObjetivo() != null && !m.getFechaObjetivo().isEmpty()) {
                try {
                    LocalDate hoy = LocalDate.now();
                    LocalDate obj = LocalDate.parse(m.getFechaObjetivo());
                    long dias = ChronoUnit.DAYS.between(hoy, obj);
                    deadline.setVisibility(View.VISIBLE);
                    if (dias >= 0) {
                        deadline.setText("Faltan " + dias + " días");
                        deadline.setTextColor(0xFF757575);
                    } else {
                        deadline.setText("Vencida hace " + (-dias) + " días");
                        deadline.setTextColor(0xFFC62828); // rojo
                    }
                } catch (Exception e) {
                    deadline.setVisibility(View.GONE);
                }
            } else {
                deadline.setVisibility(View.GONE);
            }

            // Recordatorio pasivo "sin actualizar hace X días"
            String ultimo = ev.ultimoRegistroRelevante(m, dataStore);
            boolean mostrarRec = false;
            if (ultimo != null) {
                try {
                    LocalDate ultimaFecha = ultimo.length() >= 10
                            ? LocalDate.parse(ultimo.substring(0, 10))
                            : LocalDateTime.parse(ultimo).toLocalDate();
                    long dias = ChronoUnit.DAYS.between(ultimaFecha, LocalDate.now());
                    if (dias > 14) mostrarRec = true;
                } catch (Exception ignored) { }
            }
            recordatorio.setVisibility(mostrarRec ? View.VISIBLE : View.GONE);

            // Resaltado (deeplink de notificación)
            if (metaResaltadaId != null && metaResaltadaId.equals(m.getId())) {
                itemView.setBackgroundColor(0xFFFFF3E0); // acento claro
                itemView.postDelayed(() -> {
                    metaResaltadaId = null;
                    itemView.setBackgroundColor(0);
                }, 3000);
            }

            botonVerPlan.setOnClickListener(v -> listener.onVerPlan(m));
            overflow.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.getMenu().add(0, 1, 0, "Editar título/objetivo");
                menu.getMenu().add(0, 2, 1, "Descartar meta");
                menu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) listener.onEditar(m);
                    else if (item.getItemId() == 2) listener.onDescartar(m);
                    return true;
                });
                menu.show();
            });
        }

        private String iconoDe(int tipo) {
            switch (tipo) {
                case Meta.TIPO_AUMENTAR_MEDIDA: return "💪";
                case Meta.TIPO_REDUCIR_MEDIDA:  return "📉";
                case Meta.TIPO_PESO_CORPORAL:   return "⚖";
                case Meta.TIPO_PR_EJERCICIO:    return "🏋";
                default: return "•";
            }
        }
    }
}
