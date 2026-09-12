package com.ironquest.mvp.ui;

import android.app.Activity;

import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;

/** Fachada estable: la implementación vive en {@link EjercicioPickerDialog}. */
public final class EjercicioPicker {

    public interface Listener {
        void onEjercicioElegido(Ejercicio ejercicio);
    }

    private EjercicioPicker() {
    }

    public static void mostrar(Activity activity, DataManager dataManager, DataStore dataStore, Listener listener) {
        new EjercicioPickerDialog(activity, dataManager, dataStore, listener).show();
    }
}
