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
        mostrar(activity, dataManager, dataStore, null, null, listener);
    }

    /** Abre el selector pre-filtrado: chip de músculo y buscador prefill, o {@code null} para dejarlos en "Todas". */
    public static void mostrar(Activity activity, DataManager dataManager, DataStore dataStore,
                               String musculoInicial, String textoInicial, Listener listener) {
        new EjercicioPickerDialog(activity, dataManager, dataStore, musculoInicial, textoInicial, listener).show();
    }
}
