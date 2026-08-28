package com.ironquest.mvp.ui;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;

public final class EjercicioPicker {

    public interface Listener {
        void onEjercicioElegido(Ejercicio ejercicio);
    }

    private EjercicioPicker() {
    }

    public static void mostrar(Activity activity, DataManager dataManager, DataStore dataStore, Listener listener) {
        String[] opciones = new String[dataStore.ejercicios.size() + 1];
        for (int i = 0; i < dataStore.ejercicios.size(); i++) {
            Ejercicio ejercicio = dataStore.ejercicios.get(i);
            opciones[i] = ejercicio.nombre + " (" + ejercicio.grupoMuscular + ")";
        }
        opciones[opciones.length - 1] = "+ Ejercicio personalizado";

        new AlertDialog.Builder(activity)
                .setTitle("Elige un ejercicio")
                .setItems(opciones, (dialog, which) -> {
                    if (which == opciones.length - 1) {
                        mostrarPersonalizado(activity, dataManager, dataStore, listener);
                    } else {
                        listener.onEjercicioElegido(dataStore.ejercicios.get(which));
                    }
                })
                .show();
    }

    private static void mostrarPersonalizado(Activity activity, DataManager dataManager, DataStore dataStore, Listener listener) {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_custom_exercise, null);
        EditText editNombre = view.findViewById(R.id.edit_nombre_ejercicio_personalizado);
        EditText editGrupo = view.findViewById(R.id.edit_grupo_ejercicio_personalizado);

        new AlertDialog.Builder(activity)
                .setTitle("Ejercicio personalizado")
                .setView(view)
                .setPositiveButton("Agregar", (dialog, which) -> {
                    String nombre = editNombre.getText().toString().trim();
                    if (TextUtils.isEmpty(nombre)) {
                        Toast.makeText(activity, "Escribe un nombre", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String grupo = editGrupo.getText().toString().trim();
                    if (TextUtils.isEmpty(grupo)) {
                        grupo = "Personalizado";
                    }
                    Ejercicio nuevo = new Ejercicio(dataManager.newId("ex"), nombre, grupo);
                    dataStore.ejercicios.add(nuevo);
                    listener.onEjercicioElegido(nuevo);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
