package com.ironquest.mvp.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaCompartida;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private static final String FILE_NAME = "datos.json";
    private static final int CATALOGO_VERSION_ACTUAL = 2;
    private static final String ASSET_CATALOGO = "catalogo.json";
    private static DataManager instance;

    private final Context appContext;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private DataStore dataStore;

    private DataManager(Context context) {
        appContext = context.getApplicationContext();
        file = new File(appContext.getFilesDir(), FILE_NAME);
        dataStore = load();
        migrarEjerciciosPersonalizados();
        migrarCatalogoDataset();
    }

    /**
     * Antes de que existiera el campo {@code personalizado}, los ejercicios creados por el
     * usuario ya llevaban un id con prefijo "ex_" (via {@code newId("ex")}), distinto de los 20
     * ids sembrados ("ex1".."ex20", sin guion bajo). Se usa ese prefijo, una sola vez, para
     * marcarlos retroactivamente — no vuelve a aplicar una vez migrados, porque ya quedan en true.
     */
    private void migrarEjerciciosPersonalizados() {
        boolean cambio = false;
        for (Ejercicio ejercicio : dataStore.getEjercicios()) {
            if (!ejercicio.isPersonalizado() && ejercicio.getId() != null && ejercicio.getId().startsWith("ex_")) {
                ejercicio.setPersonalizado(true);
                cambio = true;
            }
        }
        if (cambio) {
            save();
        }
    }

    /**
     * Los 20 ejercicios sembrados no traían {@code musculoObjetivo} porque ese campo nació con
     * el catálogo curado del dataset. Se les asigna aquí, desde el mismo vocabulario fijo
     * ({@link Ejercicio#MUSCULOS_OBJETIVO}), para que compartan taxonomía de chips con los
     * ejercicios nuevos en vez de quedar huérfanos bajo cualquier filtro específico.
     */
    private static Map<String, String> crearRetagSeed() {
        Map<String, String> m = new HashMap<>();
        m.put("ex1", "Pectorales");
        m.put("ex2", "Pectorales");
        m.put("ex3", "Pectorales");
        m.put("ex4", "Pectorales");
        m.put("ex5", "Dorsales");
        m.put("ex6", "Dorsales");
        m.put("ex7", "Dorsales");
        m.put("ex8", "Dorsales");
        m.put("ex9", "Zona lumbar");
        m.put("ex10", "Cuádriceps");
        m.put("ex11", "Cuádriceps");
        m.put("ex12", "Glúteos");
        m.put("ex13", "Cuádriceps");
        m.put("ex14", "Isquiotibiales");
        m.put("ex15", "Pantorrillas");
        m.put("ex16", "Bíceps");
        m.put("ex17", "Bíceps");
        m.put("ex18", "Tríceps");
        m.put("ex19", "Hombros");
        m.put("ex20", "Hombros");
        return m;
    }

    private static final Map<String, String> RETAG_SEED = crearRetagSeed();

    /**
     * Migración aditiva del catálogo curado: nunca se ejecuta más de una vez por versión
     * (Trampa #5) y nunca toca ejercicios existentes salvo para completar
     * {@code musculoObjetivo} en los 20 sembrados. Se llama desde el constructor, nunca desde
     * {@link #load()} (Trampa #4).
     */
    private void migrarCatalogoDataset() {
        if (dataStore.getCatalogoVersion() >= CATALOGO_VERSION_ACTUAL) {
            return;
        }
        for (Map.Entry<String, String> entry : RETAG_SEED.entrySet()) {
            Ejercicio ejercicio = dataStore.buscarEjercicio(entry.getKey());
            if (ejercicio != null && ejercicio.getMusculoObjetivo() == null) {
                ejercicio.setMusculoObjetivo(entry.getValue());
            }
        }
        try (InputStreamReader reader = new InputStreamReader(
                appContext.getAssets().open(ASSET_CATALOGO), StandardCharsets.UTF_8)) {
            Type tipoLista = new TypeToken<List<Ejercicio>>() { }.getType();
            List<Ejercicio> catalogoNuevo = gson.fromJson(reader, tipoLista);
            if (catalogoNuevo != null) {
                for (Ejercicio candidato : catalogoNuevo) {
                    if (dataStore.buscarEjercicio(candidato.getId()) == null) {
                        dataStore.getEjercicios().add(candidato);
                    }
                }
            }
        } catch (IOException e) {
            // No debe tumbar el arranque: el catálogo semilla ex1..ex20 sigue disponible.
        }
        dataStore.setCatalogoVersion(CATALOGO_VERSION_ACTUAL);
        save();
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
        // Un respaldo importado trae su propio catalogoVersion (a menudo 0): se resetea para
        // forzar el merge del catálogo curado, si no, un respaldo viejo se queda sin él.
        dataStore.setCatalogoVersion(0);
        migrarCatalogoDataset();
    }

    /**
     * Borra todos los datos ligados a la cuenta actual y deja el catálogo semilla listo para
     * que otra cuenta empiece desde cero. Se llama al cerrar sesión y, defensivamente, al
     * detectar que la cuenta que inicia sesión no coincide con el usuario que había en local
     * (por ejemplo si la app se cerró sin logout previo).
     */
    public void limpiarDatosDeUsuario() {
        dataStore.setUsuario(null);
        dataStore.getHistorialFisico().clear();
        dataStore.getRutinas().clear();
        dataStore.getSesiones().clear();
        dataStore.setSesionEnProgreso(null);
        dataStore.getEjercicios().clear();
        dataStore.getEjercicios().addAll(seedEjercicios());
        // Sin esto, el catálogo curado (y el retag de ex1..ex20) no se reaplica dentro de la
        // misma instancia de DataManager — migrarCatalogoDataset() solo corre una vez por
        // versión, y como es singleton, un logout/cambio de cuenta lo dejaría degradado hasta
        // reiniciar la app.
        dataStore.setCatalogoVersion(0);
        migrarCatalogoDataset();
    }

    public File exportarRutinaComoArchivo(Rutina rutina) throws IOException {
        File dir = new File(appContext.getCacheDir(), "exportaciones");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de exportación");
        }
        RutinaCompartida paquete = new RutinaCompartida();
        paquete.setRutina(rutina);
        java.util.List<Ejercicio> ejerciciosUsados = new java.util.ArrayList<>();
        for (RutinaEjercicio re : rutina.getEjercicios()) {
            Ejercicio ejercicio = dataStore.buscarEjercicio(re.getEjercicioId());
            if (ejercicio != null) {
                ejerciciosUsados.add(ejercicio);
            }
        }
        paquete.setEjercicios(ejerciciosUsados);

        File exportFile = new File(dir, "rutina_" + rutina.getId() + ".json");
        try (FileWriter writer = new FileWriter(exportFile)) {
            gson.toJson(paquete, writer);
        }
        return exportFile;
    }

    public RutinaCompartida leerRutinaCompartidaDesde(InputStream inputStream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            RutinaCompartida paquete = gson.fromJson(reader, RutinaCompartida.class);
            if (paquete == null || paquete.getRutina() == null) {
                throw new IOException("El archivo no contiene una rutina válida");
            }
            if (paquete.getEjercicios() == null) {
                paquete.setEjercicios(new java.util.ArrayList<>());
            }
            return paquete;
        }
    }

    public Rutina importarRutina(RutinaCompartida paquete) {
        Map<String, String> idsRemapeados = new HashMap<>();
        for (Ejercicio ejercicioImportado : paquete.getEjercicios()) {
            Ejercicio existente = dataStore.buscarEjercicio(ejercicioImportado.getId());
            if (existente != null) {
                idsRemapeados.put(ejercicioImportado.getId(), existente.getId());
            } else {
                String nuevoId = newId("ex");
                Ejercicio nuevo = new Ejercicio(
                        nuevoId, ejercicioImportado.getNombre(), ejercicioImportado.getGrupoMuscular());
                nuevo.setPersonalizado(true);
                dataStore.getEjercicios().add(nuevo);
                idsRemapeados.put(ejercicioImportado.getId(), nuevoId);
            }
        }

        Rutina original = paquete.getRutina();
        Rutina nueva = new Rutina(newId("r"), original.getNombre());
        for (RutinaEjercicio re : original.getEjercicios()) {
            String idRemapeado = idsRemapeados.getOrDefault(re.getEjercicioId(), re.getEjercicioId());
            RutinaEjercicio copia = new RutinaEjercicio(idRemapeado, re.getSeries(), re.getRepeticiones(), re.getPeso());
            copia.setEsquemaProgresion(re.getEsquemaProgresion());
            copia.setRepeticionesMax(re.getRepeticionesMax());
            nueva.agregarEjercicio(copia);
        }
        dataStore.getRutinas().add(nueva);
        save();
        return nueva;
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
