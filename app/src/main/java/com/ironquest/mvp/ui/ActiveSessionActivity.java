package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.SerieSesion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class ActiveSessionActivity extends AppCompatActivity {

    private static final double PASO_PESO = 2.5;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private DataManager dataManager;
    private DataStore dataStore;
    private Sesion sesionActual;
    private long duracionDescansoMillis = 60_000L;

    private Map<String, Ejercicio> catalogoPorId;
    private LinearLayout containerEjercicios;
    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_session);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();

        String rutinaId = getIntent().getStringExtra(RoutineListActivity.EXTRA_RUTINA_ID);
        Rutina rutina = buscarRutina(rutinaId);
        if (rutina == null) {
            Toast.makeText(this, "No se encontró la rutina", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        catalogoPorId = new HashMap<>();
        for (Ejercicio ejercicio : dataStore.ejercicios) {
            catalogoPorId.put(ejercicio.id, ejercicio);
        }

        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        sesionActual = new Sesion(dataManager.newId("s"), rutina.id, rutina.nombre, ahora.toString());

        setTitle(rutina.nombre);
        TextView textHoraInicio = findViewById(R.id.text_hora_inicio);
        textHoraInicio.setText("Inicio: " + ahora.format(FORMATO_HORA));

        containerEjercicios = findViewById(R.id.container_ejercicios);
        inflater = LayoutInflater.from(this);

        double volumenPlaneado = 0;
        for (RutinaEjercicio re : rutina.ejercicios) {
            EjercicioSesion ejercicioSesion = new EjercicioSesion(re.ejercicioId);
            sesionActual.ejercicios.add(ejercicioSesion);
            agregarBloqueEjercicio(ejercicioSesion, re.series, re.repeticiones, re.peso);
            volumenPlaneado += unidadEsfuerzo(re.peso, re.repeticiones) * re.series;
        }
        sesionActual.volumenPlaneado = volumenPlaneado;

        findViewById(R.id.button_finalizar_sesion).setOnClickListener(v -> finalizarSesion());
        findViewById(R.id.button_iniciar_descanso).setOnClickListener(v -> mostrarTemporizadorDescanso());
        findViewById(R.id.button_agregar_ejercicio_sesion).setOnClickListener(v ->
                EjercicioPicker.mostrar(this, dataManager, dataStore, ejercicio -> {
                    catalogoPorId.putIfAbsent(ejercicio.id, ejercicio);
                    EjercicioSesion nuevo = new EjercicioSesion(ejercicio.id);
                    sesionActual.ejercicios.add(nuevo);
                    agregarBloqueEjercicio(nuevo, 3, 10, 0.0);
                }));
    }

    private void agregarBloqueEjercicio(EjercicioSesion ejercicioSesion, int seriesIniciales, int repsIniciales, double pesoInicial) {
        View block = inflater.inflate(R.layout.view_ejercicio_sesion_block, containerEjercicios, false);
        TextView nombre = block.findViewById(R.id.text_nombre_ejercicio_sesion);
        ImageButton botonCambiar = block.findViewById(R.id.button_cambiar_ejercicio);
        ImageButton botonEliminar = block.findViewById(R.id.button_eliminar_ejercicio_sesion);
        LinearLayout containerSeries = block.findViewById(R.id.container_series);

        actualizarNombreBloque(nombre, ejercicioSesion.ejercicioId);

        for (int i = 1; i <= seriesIniciales; i++) {
            SerieSesion serie = new SerieSesion(i, pesoInicial, repsIniciales, false);
            ejercicioSesion.series.add(serie);
            containerSeries.addView(crearFilaSerie(inflater, containerSeries, serie));
        }

        MaterialButton botonAgregarSerie = block.findViewById(R.id.button_agregar_serie);
        botonAgregarSerie.setOnClickListener(v -> {
            SerieSesion ultima = ejercicioSesion.series.isEmpty()
                    ? new SerieSesion(0, pesoInicial, repsIniciales, false)
                    : ejercicioSesion.series.get(ejercicioSesion.series.size() - 1);
            SerieSesion nueva = new SerieSesion(ejercicioSesion.series.size() + 1, ultima.peso, ultima.repeticiones, false);
            ejercicioSesion.series.add(nueva);
            containerSeries.addView(crearFilaSerie(inflater, containerSeries, nueva));
        });

        botonCambiar.setOnClickListener(v -> EjercicioPicker.mostrar(this, dataManager, dataStore, nuevoEjercicio -> {
            catalogoPorId.putIfAbsent(nuevoEjercicio.id, nuevoEjercicio);
            ejercicioSesion.ejercicioId = nuevoEjercicio.id;
            actualizarNombreBloque(nombre, nuevoEjercicio.id);
        }));

        botonEliminar.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Eliminar ejercicio")
                .setMessage("¿Quitar este ejercicio de la sesión?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    sesionActual.ejercicios.remove(ejercicioSesion);
                    containerEjercicios.removeView(block);
                })
                .setNegativeButton("Cancelar", null)
                .show());

        containerEjercicios.addView(block);
    }

    private void actualizarNombreBloque(TextView nombre, String ejercicioId) {
        Ejercicio ejercicio = catalogoPorId.get(ejercicioId);
        nombre.setText(ejercicio != null ? ejercicio.nombre : "Ejercicio");
    }

    private void mostrarTemporizadorDescanso() {
        new RestTimerDialog(this, duracionDescansoMillis, millis -> duracionDescansoMillis = millis).show();
    }

    private View crearFilaSerie(LayoutInflater inflater, LinearLayout parent, SerieSesion serie) {
        View row = inflater.inflate(R.layout.view_serie_sesion_row, parent, false);

        TextView textNumero = row.findViewById(R.id.text_numero_serie);
        TextView textPeso = row.findViewById(R.id.text_peso);
        TextView textReps = row.findViewById(R.id.text_reps);
        ImageButton botonPesoMenos = row.findViewById(R.id.button_peso_menos);
        ImageButton botonPesoMas = row.findViewById(R.id.button_peso_mas);
        ImageButton botonRepsMenos = row.findViewById(R.id.button_reps_menos);
        ImageButton botonRepsMas = row.findViewById(R.id.button_reps_mas);
        ImageButton botonCompletada = row.findViewById(R.id.button_completada);

        textNumero.setText("Serie " + serie.numero);
        textPeso.setText(formatearPeso(serie.peso));
        textReps.setText(String.valueOf(serie.repeticiones));
        actualizarIconoCompletada(botonCompletada, serie.completada);

        botonPesoMenos.setOnClickListener(v -> {
            serie.peso = Math.max(0, serie.peso - PASO_PESO);
            textPeso.setText(formatearPeso(serie.peso));
        });
        botonPesoMas.setOnClickListener(v -> {
            serie.peso += PASO_PESO;
            textPeso.setText(formatearPeso(serie.peso));
        });
        botonRepsMenos.setOnClickListener(v -> {
            serie.repeticiones = Math.max(0, serie.repeticiones - 1);
            textReps.setText(String.valueOf(serie.repeticiones));
        });
        botonRepsMas.setOnClickListener(v -> {
            serie.repeticiones += 1;
            textReps.setText(String.valueOf(serie.repeticiones));
        });
        botonCompletada.setOnClickListener(v -> {
            serie.completada = !serie.completada;
            actualizarIconoCompletada(botonCompletada, serie.completada);
            if (serie.completada) {
                mostrarTemporizadorDescanso();
            }
        });

        return row;
    }

    private void actualizarIconoCompletada(ImageButton boton, boolean completada) {
        boton.setImageResource(completada
                ? android.R.drawable.checkbox_on_background
                : android.R.drawable.checkbox_off_background);
    }

    private String formatearPeso(double peso) {
        if (peso == Math.floor(peso)) {
            return String.valueOf((long) peso);
        }
        return String.valueOf(peso);
    }

    private static double unidadEsfuerzo(double peso, int repeticiones) {
        return peso > 0 ? peso * repeticiones : repeticiones;
    }

    private Rutina buscarRutina(String id) {
        if (id == null) {
            return null;
        }
        for (Rutina r : dataStore.rutinas) {
            if (r.id.equals(id)) {
                return r;
            }
        }
        return null;
    }

    private void finalizarSesion() {
        LocalDateTime ahora = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        sesionActual.fechaHoraFin = ahora.toString();

        double volumenReal = 0;
        for (EjercicioSesion ejercicioSesion : sesionActual.ejercicios) {
            for (SerieSesion serie : ejercicioSesion.series) {
                if (serie.completada) {
                    volumenReal += unidadEsfuerzo(serie.peso, serie.repeticiones);
                }
            }
        }
        sesionActual.volumenReal = volumenReal;

        int porcentaje;
        if (sesionActual.volumenPlaneado > 0) {
            porcentaje = (int) Math.round(volumenReal / sesionActual.volumenPlaneado * 100);
        } else {
            porcentaje = volumenReal > 0 ? 100 : 0;
        }
        sesionActual.porcentajeCumplimiento = Math.max(0, porcentaje);

        dataStore.sesiones.add(sesionActual);
        dataManager.save();
        Toast.makeText(this, "Sesión guardada — Cumplimiento: " + sesionActual.porcentajeCumplimiento + "%", Toast.LENGTH_LONG).show();
        finish();
    }
}
