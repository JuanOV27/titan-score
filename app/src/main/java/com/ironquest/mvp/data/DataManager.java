package com.ironquest.mvp.data;

import android.content.Context;
import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaCompartida;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.SugerenciaPendiente;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataManager {

    private static final String TAG = "DataManager";
    private static final String FILE_NAME = "datos.json";
    private static final String FILE_CATALOGO = "catalogo_local.json";
    private static final int CATALOGO_VERSION_ACTUAL = 2;
    private static final String ASSET_CATALOGO = "catalogo.json";
    private static DataManager instance;

    private final Context appContext;
    private final File file;
    private final File fileCatalogo;

    /**
     * Gson estándar para leer/escribir el archivo del catálogo, importaciones y exportaciones
     * completas. Escribe todos los campos.
     */
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Gson que <b>omite {@code DataStore.ejercicios}</b> al serializar. El catálogo vive en
     * {@link #fileCatalogo}, aparte, y se re-escribe solo cuando cambia. Los taps del usuario
     * durante una sesión activa solo escriben ~35 KB (rutinas + sesiones + usuario) en vez de
     * ~290 KB (todo con catálogo). Esto redujo el jank de los botones peso/reps de ~1.5 s a
     * ~50 ms por tap.
     */
    private final Gson gsonSinCatalogo = new GsonBuilder()
            .setPrettyPrinting()
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    return f.getDeclaringClass() == DataStore.class
                            && "ejercicios".equals(f.getName());
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            })
            .create();

    private DataStore dataStore;

    /**
     * Un solo hilo de escritura → todas las {@code save()} son secuenciales, sin race conditions
     * y sin bloquear el hilo principal. La UI serializa a String (rápido, ~5 ms para 35 KB) y
     * este hilo hace el I/O real. Ver {@link #drainDatos()}.
     */
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "DataManager-disk");
        t.setPriority(Thread.MIN_PRIORITY + 1);
        return t;
    });

    /**
     * Coalescing: si el usuario dispara 10 saves seguidos, solo el último estado se escribe.
     * Los intermedios se descartan porque son obsoletos apenas llega el siguiente. Sincronizado
     * con {@link #saveLock}.
     */
    private final Object saveLock = new Object();
    private String pendingDatos;
    private boolean saveQueuedDatos;
    private String pendingCatalogo;
    private boolean saveQueuedCatalogo;

    private DataManager(Context context) {
        appContext = context.getApplicationContext();
        file = new File(appContext.getFilesDir(), FILE_NAME);
        fileCatalogo = new File(appContext.getFilesDir(), FILE_CATALOGO);
        dataStore = load();
        migrarSepararCatalogo();
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
            saveCatalogoSync();
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
     * Migración one-shot desde el layout previo (todo en {@code datos.json}) al nuevo layout
     * (rutinas/sesiones/usuario en {@code datos.json}, catálogo en {@code catalogo_local.json}).
     * Se ejecuta cuando el catálogo separado no existe todavía pero el DataStore en memoria
     * ya trae ejercicios cargados desde el datos.json viejo. Idempotente: si el archivo de
     * catálogo ya existe, no hace nada.
     */
    private void migrarSepararCatalogo() {
        if (fileCatalogo.exists()) {
            return;
        }
        if (dataStore.getEjercicios() == null || dataStore.getEjercicios().isEmpty()) {
            return;
        }
        // Escribir catálogo primero: si falla, no borramos nada de datos.json.
        saveCatalogoSync();
        // Ahora re-escribir datos.json sin la clave ejercicios (el gsonSinCatalogo la excluye).
        saveSync();
    }

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
        saveCatalogoSync();
        saveSync();
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

    // ---- API pública de persistencia ----

    /**
     * Encola una escritura de {@code datos.json} en background. El snapshot del estado se toma
     * en el hilo llamador (rápido, ~5 ms) y el I/O ocurre después. Múltiples llamadas seguidas
     * se colapsan: solo el último estado se escribe. Usar {@link #saveSync()} cuando se necesite
     * garantía de que llegó a disco antes de continuar.
     */
    public void save() {
        String json = gsonSinCatalogo.toJson(dataStore);
        synchronized (saveLock) {
            pendingDatos = json;
            if (!saveQueuedDatos) {
                saveQueuedDatos = true;
                diskExecutor.submit(this::drainDatos);
            }
        }
    }

    /**
     * Encola una escritura del catálogo. Se usa cuando el usuario crea, edita o migra ejercicios
     * — nunca durante una sesión activa por interacciones normales del usuario.
     */
    public void saveCatalogo() {
        String json = gson.toJson(dataStore.getEjercicios());
        synchronized (saveLock) {
            pendingCatalogo = json;
            if (!saveQueuedCatalogo) {
                saveQueuedCatalogo = true;
                diskExecutor.submit(this::drainCatalogo);
            }
        }
    }

    /**
     * Fuerza que todas las escrituras encoladas terminen antes de devolver. Usar en momentos
     * críticos donde no podemos perder datos: {@code onPause} de Activities de edición,
     * finalización de sesión, logout, cierre de la app.
     */
    public void saveSync() {
        save();
        drenar();
    }

    /** Variante sincrónica solo para el catálogo. Igual que {@link #saveSync()} pero encola solo el catálogo. */
    public void saveCatalogoSync() {
        saveCatalogo();
        drenar();
    }

    private void drenar() {
        try {
            // Un no-op enviado al mismo executor single-threaded espera por definición a que
            // todos los tasks previos (los drenajes ya encolados) terminen. Si el hilo llamador
            // ES el diskExecutor (caso raro: dentro de drainDatos/drainCatalogo), esto se
            // saltea para no deadlock.
            if (Thread.currentThread().getName().equals("DataManager-disk")) {
                return;
            }
            diskExecutor.submit(() -> { }).get();
        } catch (Exception e) {
            Log.e(TAG, "saveSync no pudo drenar el executor", e);
        }
    }

    private void drainDatos() {
        String toWrite;
        synchronized (saveLock) {
            toWrite = pendingDatos;
            pendingDatos = null;
            saveQueuedDatos = false;
        }
        if (toWrite == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(toWrite);
        } catch (IOException e) {
            Log.e(TAG, "No se pudo guardar " + FILE_NAME, e);
        }
    }

    private void drainCatalogo() {
        String toWrite;
        synchronized (saveLock) {
            toWrite = pendingCatalogo;
            pendingCatalogo = null;
            saveQueuedCatalogo = false;
        }
        if (toWrite == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(fileCatalogo)) {
            writer.write(toWrite);
        } catch (IOException e) {
            Log.e(TAG, "No se pudo guardar " + FILE_CATALOGO, e);
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
        // Exportación completa (incluye ejercicios) para que el archivo sea autocontenido.
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
        // Un import es one-shot desde UI: bloquear hasta que quede en disco garantiza que un
        // crash inmediato no deje el estado a medio importar.
        saveSync();
        saveCatalogoSync();
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
        // Logout: garantizar que el reset quedó en disco antes de que la app cierre la sesión.
        saveSync();
        saveCatalogoSync();
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
        boolean catalogoTocado = false;
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
                catalogoTocado = true;
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
        if (catalogoTocado) {
            saveCatalogo();
        }
        save();
        return nueva;
    }

    public List<SugerenciaPendiente> getSugerenciasPendientes() {
        return dataStore.getSugerenciasPendientes();
    }

    public List<SugerenciaPendiente> getSugerenciasPendientesDeRutina(String rutinaId) {
        List<SugerenciaPendiente> resultado = new java.util.ArrayList<>();
        if (rutinaId == null) {
            return resultado;
        }
        for (SugerenciaPendiente sp : dataStore.getSugerenciasPendientes()) {
            if (rutinaId.equals(sp.getRutinaId())) {
                resultado.add(sp);
            }
        }
        return resultado;
    }

    public void agregarSugerencia(SugerenciaPendiente sugerencia) {
        dataStore.getSugerenciasPendientes().add(sugerencia);
        save();
    }

    public void eliminarSugerencia(String sugerenciaId) {
        if (sugerenciaId == null) {
            return;
        }
        java.util.Iterator<SugerenciaPendiente> it = dataStore.getSugerenciasPendientes().iterator();
        while (it.hasNext()) {
            if (sugerenciaId.equals(it.next().getId())) {
                it.remove();
                save();
                return;
            }
        }
    }

    public void eliminarSugerenciasDeRutina(String rutinaId) {
        if (rutinaId == null) {
            return;
        }
        java.util.Iterator<SugerenciaPendiente> it = dataStore.getSugerenciasPendientes().iterator();
        boolean cambio = false;
        while (it.hasNext()) {
            if (rutinaId.equals(it.next().getRutinaId())) {
                it.remove();
                cambio = true;
            }
        }
        if (cambio) {
            save();
        }
    }

    /**
     * Aplica la sugerencia al {@link RutinaEjercicio} correspondiente y la elimina de la lista.
     * Un solo {@code save()} al final. Si la rutina o el ejercicio ya no existen, la sugerencia
     * se elimina igual (huérfana).
     */
    public void aplicarSugerencia(String sugerenciaId) {
        if (sugerenciaId == null) {
            return;
        }
        SugerenciaPendiente objetivo = null;
        for (SugerenciaPendiente sp : dataStore.getSugerenciasPendientes()) {
            if (sugerenciaId.equals(sp.getId())) {
                objetivo = sp;
                break;
            }
        }
        if (objetivo == null) {
            return;
        }
        Rutina rutina = dataStore.buscarRutina(objetivo.getRutinaId());
        if (rutina != null) {
            for (RutinaEjercicio re : rutina.getEjercicios()) {
                if (objetivo.getEjercicioId().equals(re.getEjercicioId())) {
                    re.setPeso(objetivo.getPesoSugerido());
                    re.setRepeticiones(objetivo.getRepeticionesSugeridas());
                    break;
                }
            }
        }
        dataStore.getSugerenciasPendientes().remove(objetivo);
        save();
    }

    public java.util.List<com.ironquest.mvp.model.Meta> getMetasActivas() {
        java.util.List<com.ironquest.mvp.model.Meta> resultado = new java.util.ArrayList<>();
        for (com.ironquest.mvp.model.Meta m : dataStore.getMetas()) {
            if (m.getEstado() == com.ironquest.mvp.model.Meta.ESTADO_ACTIVA) {
                resultado.add(m);
            }
        }
        return resultado;
    }

    public java.util.List<com.ironquest.mvp.model.Meta> getMetasHistorial() {
        java.util.List<com.ironquest.mvp.model.Meta> resultado = new java.util.ArrayList<>();
        for (com.ironquest.mvp.model.Meta m : dataStore.getMetas()) {
            if (m.getEstado() == com.ironquest.mvp.model.Meta.ESTADO_CUMPLIDA || m.getEstado() == com.ironquest.mvp.model.Meta.ESTADO_DESCARTADA) {
                resultado.add(m);
            }
        }
        // Ordenar por fecha de cierre descendente (más reciente arriba). Fallback: fecha creación.
        resultado.sort((a, b) -> {
            String fechaA = a.getFechaCumplida() != null ? a.getFechaCumplida()
                    : a.getFechaDescartada() != null ? a.getFechaDescartada()
                    : a.getFechaCreacion();
            String fechaB = b.getFechaCumplida() != null ? b.getFechaCumplida()
                    : b.getFechaDescartada() != null ? b.getFechaDescartada()
                    : b.getFechaCreacion();
            if (fechaA == null && fechaB == null) return 0;
            if (fechaA == null) return 1;
            if (fechaB == null) return -1;
            return fechaB.compareTo(fechaA);
        });
        return resultado;
    }

    /** Agrega una meta si no se supera el límite de 3 activas. Devuelve false si se rechaza. */
    public boolean agregarMeta(com.ironquest.mvp.model.Meta meta) {
        if (getMetasActivas().size() >= 3) {
            return false;
        }
        dataStore.getMetas().add(meta);
        save();
        return true;
    }

    public void descartarMeta(String metaId) {
        if (metaId == null) return;
        for (com.ironquest.mvp.model.Meta m : dataStore.getMetas()) {
            if (metaId.equals(m.getId()) && m.getEstado() == com.ironquest.mvp.model.Meta.ESTADO_ACTIVA) {
                m.setEstado(com.ironquest.mvp.model.Meta.ESTADO_DESCARTADA);
                m.setFechaDescartada(java.time.LocalDateTime.now().toString());
                save();
                return;
            }
        }
    }

    /**
     * Carga el DataStore desde disco. Lee {@link #FILE_NAME} para todo lo que no es catálogo
     * (rutinas, sesiones, usuario, historial físico, sugerencias, catalogoVersion), y
     * {@link #FILE_CATALOGO} para los ejercicios. Si {@link #FILE_CATALOGO} no existe todavía
     * pero {@code datos.json} viejo tiene ejercicios dentro, se deja al deserializador que
     * los recupere de ahí — la migración a dos archivos ocurre después en el constructor
     * (ver {@link #migrarSepararCatalogo()}).
     */
    private DataStore load() {
        DataStore loaded;
        if (!file.exists()) {
            loaded = new DataStore();
        } else {
            try (FileReader reader = new FileReader(file)) {
                loaded = gson.fromJson(reader, DataStore.class);
                if (loaded == null) {
                    loaded = new DataStore();
                }
            } catch (IOException e) {
                throw new RuntimeException("No se pudo leer " + FILE_NAME, e);
            }
        }
        loaded.normalizarColecciones();

        // Si existe el archivo de catálogo separado, tiene prioridad sobre lo que datos.json
        // pudiera traer todavía (usuarios en versiones intermedias podrían tener las dos
        // fuentes; la de catalogo_local.json es la canónica).
        if (fileCatalogo.exists()) {
            try (FileReader reader = new FileReader(fileCatalogo)) {
                Type tipoLista = new TypeToken<List<Ejercicio>>() { }.getType();
                List<Ejercicio> ejercicios = gson.fromJson(reader, tipoLista);
                if (ejercicios != null) {
                    loaded.getEjercicios().clear();
                    loaded.getEjercicios().addAll(ejercicios);
                }
            } catch (IOException e) {
                Log.e(TAG, "No se pudo leer " + FILE_CATALOGO + ", usando lo que traiga datos.json", e);
            }
        }

        // Sembrado inicial: solo si es una instalación nueva (ni catálogo separado ni ejercicios
        // en datos.json viejo).
        if (loaded.getEjercicios().isEmpty()) {
            loaded.getEjercicios().addAll(seedEjercicios());
        }
        return loaded;
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
