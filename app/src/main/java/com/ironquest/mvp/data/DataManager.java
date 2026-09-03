package com.ironquest.mvp.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public class DataManager {

    private static final String FILE_NAME = "datos.json";
    private static DataManager instance;

    private final Context appContext;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private DataStore dataStore;

    private DataManager(Context context) {
        appContext = context.getApplicationContext();
        file = new File(appContext.getFilesDir(), FILE_NAME);
        dataStore = load();
    }

    public static synchronized DataManager getInstance(Context context) {
        if (instance == null) {
            instance = new DataManager(context);
        }
        return instance;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(dataStore, writer);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar datos.json", e);
        }
    }

    public String newId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public File exportarComoArchivo() throws IOException {
        File dir = new File(appContext.getCacheDir(), "exportaciones");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de exportación");
        }
        File exportFile = new File(dir, "ironquest_backup.json");
        try (FileWriter writer = new FileWriter(exportFile)) {
            gson.toJson(dataStore, writer);
        }
        return exportFile;
    }

    public DataStore leerDataStoreDesde(InputStream inputStream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            DataStore importado = gson.fromJson(reader, DataStore.class);
            if (importado == null) {
                throw new IOException("El archivo no contiene datos válidos");
            }
            importado.normalizarColecciones();
            return importado;
        }
    }

    public void reemplazarTodo(DataStore nuevo) {
        dataStore.getEjercicios().clear();
        dataStore.getEjercicios().addAll(nuevo.getEjercicios());
        dataStore.getRutinas().clear();
        dataStore.getRutinas().addAll(nuevo.getRutinas());
        dataStore.getSesiones().clear();
        dataStore.getSesiones().addAll(nuevo.getSesiones());
        save();
    }

    private DataStore load() {
        if (!file.exists()) {
            DataStore fresh = new DataStore();
            fresh.getEjercicios().addAll(seedEjercicios());
            dataStore = fresh;
            save();
            return fresh;
        }
        try (FileReader reader = new FileReader(file)) {
            DataStore loaded = gson.fromJson(reader, DataStore.class);
            if (loaded == null) {
                loaded = new DataStore();
            }
            // No puede llamar a save(): load() corre antes de que this.dataStore esté asignado.
            loaded.normalizarColecciones();
            if (loaded.getEjercicios().isEmpty()) {
                loaded.getEjercicios().addAll(seedEjercicios());
            }
            return loaded;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer datos.json", e);
        }
    }

    private static java.util.List<Ejercicio> seedEjercicios() {
        java.util.List<Ejercicio> lista = new java.util.ArrayList<>();
        lista.add(new Ejercicio("ex1", "Press banca", "Pecho"));
        lista.add(new Ejercicio("ex2", "Press inclinado con mancuernas", "Pecho"));
        lista.add(new Ejercicio("ex3", "Aperturas con mancuernas", "Pecho"));
        lista.add(new Ejercicio("ex4", "Fondos en paralelas", "Pecho"));
        lista.add(new Ejercicio("ex5", "Dominadas", "Espalda"));
        lista.add(new Ejercicio("ex6", "Remo con barra", "Espalda"));
        lista.add(new Ejercicio("ex7", "Jalón al pecho", "Espalda"));
        lista.add(new Ejercicio("ex8", "Remo con mancuerna a un brazo", "Espalda"));
        lista.add(new Ejercicio("ex9", "Peso muerto", "Espalda"));
        lista.add(new Ejercicio("ex10", "Sentadilla", "Pierna"));
        lista.add(new Ejercicio("ex11", "Prensa de piernas", "Pierna"));
        lista.add(new Ejercicio("ex12", "Zancadas", "Pierna"));
        lista.add(new Ejercicio("ex13", "Extensión de cuádriceps", "Pierna"));
        lista.add(new Ejercicio("ex14", "Curl femoral", "Pierna"));
        lista.add(new Ejercicio("ex15", "Elevación de talones", "Pierna"));
        lista.add(new Ejercicio("ex16", "Curl de bíceps con barra", "Brazo"));
        lista.add(new Ejercicio("ex17", "Curl martillo", "Brazo"));
        lista.add(new Ejercicio("ex18", "Extensión de tríceps en polea", "Brazo"));
        lista.add(new Ejercicio("ex19", "Press militar", "Hombro"));
        lista.add(new Ejercicio("ex20", "Elevaciones laterales", "Hombro"));
        return lista;
    }
}
