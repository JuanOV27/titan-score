package com.ironquest.mvp.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Asistente de migración de ejercicios legacy: recorre las filas de la rutina cuyo ejercicio
 * viene de una versión anterior (sin ficha técnica/GIF y sin la marca de personalizado) y pide
 * por cada una un reemplazo del catálogo curado o la marca "creado por mí". No persiste nada:
 * las marcas y reemplazos viven en memoria hasta que la rutina se guarda.
 */
final class MigracionEjerciciosDialog extends Dialog {

    public interface Listener {
        void onTerminada();
    }

    private final Activity activity;
    private final DataManager dataManager;
    private final DataStore dataStore;
    private final Map<String, Ejercicio> catalogoPorId;
    private final RoutineStep[] pendientes;
    private final Listener listener;
    private int indice;

    private TextView textProgreso;
    private TextView textNombre;
    private TextView textExplicacion;
    private View buttonReemplazar;
    private View buttonMarcar;
    private View buttonAhoraNo;

    private static class RoutineStep {
        final RutinaEjercicio row;
        final Ejercicio ejercicio;
        /** Estado original de la fila y del ejercicio compartido: para deshacer si se aborta. */
        final String ejercicioIdOriginal;
        final boolean personalizadoOriginal;

        RoutineStep(RutinaEjercicio row, Ejercicio ejercicio) {
            this.row = row;
            this.ejercicio = ejercicio;
            this.ejercicioIdOriginal = row.getEjercicioId();
            this.personalizadoOriginal = ejercicio != null && ejercicio.isPersonalizado();
        }

        void deshacer() {
            row.setEjercicioId(ejercicioIdOriginal);
            if (ejercicio != null) {
                ejercicio.setPersonalizado(personalizadoOriginal);
            }
        }
    }

    MigracionEjerciciosDialog(Activity activity, DataManager dataManager, DataStore dataStore,
                              Map<String, Ejercicio> catalogoPorId, Rutina rutina, Listener listener) {
        super(activity);
        this.activity = activity;
        this.dataManager = dataManager;
        this.dataStore = dataStore;
        this.catalogoPorId = catalogoPorId;
        this.listener = listener;

        List<RoutineStep> acumulado = new ArrayList<>();
        for (RutinaEjercicio row : rutina.getEjercicios()) {
            Ejercicio ejercicio = catalogoPorId.get(row.getEjercicioId());
            boolean pendiente = ejercicio == null
                    || (!ejercicio.isPersonalizado() && !ejercicio.tieneFichaTecnica());
            if (pendiente) {
                acumulado.add(new RoutineStep(row, ejercicio));
            }
        }
        pendientes = acumulado.toArray(new RoutineStep[0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_migracion_ejercicio);
        setCancelable(false);
        setCanceledOnTouchOutside(false);
        setOnCancelListener(d -> abortar());
        getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        textProgreso = findViewById(R.id.text_progreso_migracion);
        textNombre = findViewById(R.id.text_nombre_ejercicio_migracion);
        textExplicacion = findViewById(R.id.text_explicacion_migracion);
        buttonReemplazar = findViewById(R.id.button_reemplazar_tecnica_migracion);
        buttonMarcar = findViewById(R.id.button_marcar_como_mio_migracion);
        buttonAhoraNo = findViewById(R.id.button_ahora_no_migracion);

        buttonReemplazar.setOnClickListener(v -> elegirReemplazo());
        buttonMarcar.setOnClickListener(v -> {
            pendientes[indice].ejercicio.setPersonalizado(true);
            avanzar();
        });
        buttonAhoraNo.setOnClickListener(v -> abortar());

        if (pendientes.length == 0) {
            dismiss();
            return;
        }
        render();
    }

    private void render() {
        RoutineStep paso = pendientes[indice];
        textProgreso.setText("Ejercicio " + (indice + 1) + " de " + pendientes.length);
        textProgreso.setVisibility(View.VISIBLE);
        textNombre.setText(paso.ejercicio != null ? paso.ejercicio.getNombre() : "Ejercicio");
        textExplicacion.setText("Este ejercicio viene de una versión anterior y no tiene ficha "
                + "técnica. Reemplázalo por un ejercicio con técnica del catálogo, o márcalo "
                + "como creado por ti para conservarlo.");
        // Sin objeto Ejercicio no hay nada que marcar: solo queda reemplazar.
        buttonMarcar.setVisibility(paso.ejercicio != null ? View.VISIBLE : View.GONE);
    }

    private void elegirReemplazo() {
        RoutineStep paso = pendientes[indice];
        String musculoInicial = paso.ejercicio != null ? paso.ejercicio.getMusculoObjetivo() : null;
        String textoInicial = palabraClave(paso.ejercicio);
        EjercicioPicker.mostrar(activity, dataManager, dataStore, musculoInicial, textoInicial, nuevoEjercicio -> {
            catalogoPorId.putIfAbsent(nuevoEjercicio.getId(), nuevoEjercicio);
            paso.row.setEjercicioId(nuevoEjercicio.getId());
            avanzar();
        });
    }
        private void avanzar() {
        indice++;
        if (indice >= pendientes.length) {
            if (listener != null) {
                listener.onTerminada();
            }
            dismiss();
        } else {
            render();
        }
    }

    /**
     * "Ahora no" / back abandonan el asistente a medio camino: ninguno de los cambios aplicados
     * en esta pasada debe sobrevivir. Sin este deshacer, un guardado posterior de otra pantalla
     * arrastraría migraciones incompletas a datos.json (misma trampa que el cancelar de un
     * renombrado personalizado, pero con alcance global por compartir los objetos Ejercicio).
     */
    private void abortar() {
        for (RoutineStep paso : pendientes) {
            paso.deshacer();
        }
        if (listener != null) {
            listener.onTerminada();
        }
        dismiss();
    }

    /**
     * Punto de partida del buscador: primera palabra del nombre legacy (p. ej. "Press banca" → "press").
     * Solo se prellena cuando esa palabra existe en algún nombre del catálogo curado; si no (como
     * "Elevaciones laterales" frente a "Elevación lateral en polea"), devuelve null para dejar la
     * búsqueda vacía y que el chip de músculo pre-seleccionado baste. Un prefill que no empareja
     * nada convierte el selector en un callejón sin salida ("No hay ejercicios para este filtro").
     */
    private String palabraClave(Ejercicio ejercicio) {
        if (ejercicio == null || ejercicio.getNombre() == null) {
            return null;
        }
        String[] palabras = ejercicio.getNombre().trim().split("\\s+");
        if (palabras.length == 0) {
            return null;
        }
        String candidata = palabras[0].toLowerCase(Locale.getDefault());
        for (Ejercicio candidato : dataStore.getEjercicios()) {
            if (candidato.tieneFichaTecnica()
                    && candidato.getNombre() != null
                    && candidato.getNombre().toLowerCase(Locale.getDefault()).contains(candidata)) {
                return candidata;
            }
        }
        return null;
    }
}