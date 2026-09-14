# Editar personalizados, reordenar y compartir rutinas — Plan de implementación

> **Para el ejecutor:** usar superpowers:subagent-driven-development (recomendado) o
> superpowers:executing-plans para ejecutar tarea por tarea. Pasos con checkbox (`- [ ]`).

**Objetivo:** permitir editar el nombre/grupo muscular de un ejercicio personalizado (aplica
a todas las rutinas que lo usan), reordenar ejercicios dentro de una rutina con arrastrar y
soltar, y exportar/importar una rutina individual como archivo para compartirla.

**Arquitectura:** todo vive en las clases ya existentes de la pantalla de editar rutina y del
listado de rutinas — no se crean Activities nuevas. El intercambio de archivos reutiliza el
mismo mecanismo de `FileProvider`/`ACTION_SEND`/`ACTION_OPEN_DOCUMENT` que ya usa "Exportar
datos"/"Importar datos" en `MainActivity`, con un formato de archivo más chico
(`RutinaCompartida`) y una importación que **agrega**, nunca reemplaza. Detalle completo del
razonamiento en `docs/specs/2026-09-06-editar-personalizados-reordenar-compartir-design.md`.

**Tech Stack:** `androidx.recyclerview.widget.ItemTouchHelper` para el arrastre; Gson para el
nuevo formato `RutinaCompartida`; sin dependencias nuevas.

## Global Constraints

- Nada fuera de `data/` toca Gson/File/FileReader/FileWriter/`getFilesDir()` (invariante ya
  documentada en `CLAUDE.md`) — todo el I/O de `RutinaCompartida` va en `DataManager`.
- Modelos con campos privados + getters/setters + constructor sin argumentos para Gson
  (convención ya establecida en `model/`).
- Texto de UI hardcodeado en Java/XML — nada nuevo en `strings.xml`.
- Sin ViewModel, sin corrutinas, sin Room, sin DI.
- Adaptación al método de verificación de este proyecto: no hay JUnit/Espresso — cada tarea
  de código se verifica compilando (`./gradlew compileDebugJava`); la verificación real en el
  teléfono ocurre solo en las Tareas 6 y 11 (los dos cierres de rebanada), igual que en el
  plan anterior de este proyecto.
- Respaldar `datos.json` antes de cualquier prueba en el teléfono real que toque datos reales
  (reordenar una rutina real, importar una rutina) — regla no negociable de `CLAUDE.md`.
- Al comitear, cada subagente hace `git add` de rutas exactas, nunca `-A` ni `.`.

---

## Rebanada 1 — Editar personalizados + reordenar

### Tarea 1: `Ejercicio.personalizado` + marcarlo en `EjercicioPicker`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/model/Ejercicio.java`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/EjercicioPicker.java:63`

**Interfaces:**
- Produce: `Ejercicio.isPersonalizado()/setPersonalizado(boolean)` — usado por las Tareas 3, 4.

- [ ] **Paso 1: Reemplazar el contenido completo de `Ejercicio.java`**

```java
package com.ironquest.mvp.model;

/** Un ejercicio del catálogo. Los IDs {@code ex1..ex20} son los que se siembran por defecto. */
public class Ejercicio extends EntidadIdentificable {

    private String nombre;
    private String grupoMuscular;
    private boolean personalizado;

    /** Constructor sin argumentos para Gson. Privado: nadie más debe crear un Ejercicio vacío. */
    private Ejercicio() {
    }

    public Ejercicio(String id, String nombre, String grupoMuscular) {
        super(id);
        this.nombre = nombre;
        this.grupoMuscular = grupoMuscular;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getGrupoMuscular() {
        return grupoMuscular;
    }

    public void setGrupoMuscular(String grupoMuscular) {
        this.grupoMuscular = grupoMuscular;
    }

    public boolean isPersonalizado() {
        return personalizado;
    }

    public void setPersonalizado(boolean personalizado) {
        this.personalizado = personalizado;
    }

    @Override
    public String toString() {
        return nombre != null ? nombre : super.toString();
    }
}
```

- [ ] **Paso 2: Marcar `personalizado=true` al crearlo**

En `app/src/main/java/com/ironquest/mvp/ui/EjercicioPicker.java`, reemplazar (línea 63):

```java
                    Ejercicio nuevo = new Ejercicio(dataManager.newId("ex"), nombre, grupo);
                    dataStore.getEjercicios().add(nuevo);
```

por:

```java
                    Ejercicio nuevo = new Ejercicio(dataManager.newId("ex"), nombre, grupo);
                    nuevo.setPersonalizado(true);
                    dataStore.getEjercicios().add(nuevo);
```

- [ ] **Paso 3: Verificar que compila**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
cd "/home/jdov/Documentos/titan score/IronQuestApp"
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Paso 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/model/Ejercicio.java \
  app/src/main/java/com/ironquest/mvp/ui/EjercicioPicker.java
git commit -m "Marcar los ejercicios creados por el usuario como personalizados"
```

---

### Tarea 2: `Rutina.moverEjercicio(int, int)`

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/model/Rutina.java:21-23`

**Interfaces:**
- Produce: `Rutina.moverEjercicio(int desde, int hasta)` — usado por la Tarea 5.

- [ ] **Paso 1: Agregar el método**

Insertar, justo después de `agregarEjercicio` (después de la línea 23, `}`):

```java

    /** Cambia la posición de un ejercicio dentro de la rutina (para arrastrar y soltar). */
    public void moverEjercicio(int desde, int hasta) {
        ejercicios.add(hasta, ejercicios.remove(desde));
    }
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Paso 3: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/model/Rutina.java
git commit -m "Agregar Rutina.moverEjercicio para reordenar ejercicios"
```

---

### Tarea 3: Layout `item_ejercicio_rutina_edit.xml` — manija de arrastre + lápiz de edición

**Files:**
- Modify: `app/src/main/res/layout/item_ejercicio_rutina_edit.xml`

**Interfaces:**
- Produce: ids `icon_arrastrar_ejercicio`, `button_editar_nombre_ejercicio` — usados por la
  Tarea 4.

- [ ] **Paso 1: Reemplazar el contenido completo del archivo**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="8dp"
    app:cardElevation="1dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="12dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:id="@+id/icon_arrastrar_ejercicio"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:padding="8dp"
                android:text="☰"
                android:textSize="18sp"
                android:contentDescription="Arrastrar para reordenar" />

            <TextView
                android:id="@+id/text_nombre_ejercicio"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:textStyle="bold"
                android:textSize="16sp" />

            <ImageButton
                android:id="@+id/button_editar_nombre_ejercicio"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="Editar ejercicio personalizado"
                android:src="@android:drawable/ic_menu_edit"
                android:visibility="gone" />

            <ImageButton
                android:id="@+id/button_quitar_ejercicio"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="Quitar ejercicio"
                android:src="@android:drawable/ic_menu_delete" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="8dp">

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginEnd="4dp"
                android:hint="Series">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_series"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="4dp"
                android:layout_marginEnd="4dp"
                android:hint="Reps">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_repeticiones"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number" />
            </com.google.android.material.textfield.TextInputLayout>

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="4dp"
                android:hint="Peso (kg)">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_peso"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="numberDecimal" />
            </com.google.android.material.textfield.TextInputLayout>

        </LinearLayout>

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="8dp"
            android:gravity="center_vertical">

            <Spinner
                android:id="@+id/spinner_esquema_progresion"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginEnd="4dp" />

            <com.google.android.material.textfield.TextInputLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:layout_marginStart="4dp"
                android:hint="Reps máx (doble prog.)">

                <com.google.android.material.textfield.TextInputEditText
                    android:id="@+id/edit_repeticiones_max"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:inputType="number" />
            </com.google.android.material.textfield.TextInputLayout>

        </LinearLayout>

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL` (puede fallar por Java en otros archivos aún no tocados en esta
tarea — si el único error mencionado es en `RutinaEjercicioEditAdapter.java`, el layout en sí
está bien; eso se resuelve en la Tarea 4).

- [ ] **Paso 3: Commit**

```bash
git add app/src/main/res/layout/item_ejercicio_rutina_edit.xml
git commit -m "Agregar manija de arrastre y lápiz de edición al layout de ejercicio"
```

---

### Tarea 4: `RutinaEjercicioEditAdapter` — wiring de manija y lápiz

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/RutinaEjercicioEditAdapter.java`

**Interfaces:**
- Consume: ids de la Tarea 3, `Ejercicio.isPersonalizado()` (Tarea 1).
- Produce: interfaz `Listener` con `onQuitar(int)`, `onEditarPersonalizado(int)`,
  `onIniciarArrastre(RecyclerView.ViewHolder)` — consumida por la Tarea 5.

- [ ] **Paso 1: Reemplazar el contenido completo del archivo**

```java
package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.util.List;
import java.util.Map;

public class RutinaEjercicioEditAdapter extends RecyclerView.Adapter<RutinaEjercicioEditAdapter.ViewHolder> {

    public interface Listener {
        void onQuitar(int position);
        void onEditarPersonalizado(int position);
        void onIniciarArrastre(RecyclerView.ViewHolder viewHolder);
    }

    private static final String[] OPCIONES_ESQUEMA =
            {"Manual", "Lineal", "Greyskull (AMRAP)", "Doble progresión"};

    private final List<RutinaEjercicio> items;
    private final Map<String, Ejercicio> catalogoPorId;
    private final Listener listener;

    public RutinaEjercicioEditAdapter(List<RutinaEjercicio> items, Map<String, Ejercicio> catalogoPorId, Listener listener) {
        this.items = items;
        this.catalogoPorId = catalogoPorId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio_rutina_edit, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), catalogoPorId.get(items.get(position).getEjercicioId()));
        holder.botonQuitar.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                listener.onQuitar(adapterPos);
            }
        });
        holder.botonEditarNombre.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION) {
                listener.onEditarPersonalizado(adapterPos);
            }
        });
        holder.manija.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                listener.onIniciarArrastre(holder);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView manija;
        private final TextView nombre;
        private final ImageButton botonEditarNombre;
        private final EditText series;
        private final EditText repeticiones;
        private final EditText peso;
        private final Spinner esquemaProgresion;
        private final EditText repeticionesMax;
        private final ImageButton botonQuitar;
        private RutinaEjercicio current;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            manija = itemView.findViewById(R.id.icon_arrastrar_ejercicio);
            nombre = itemView.findViewById(R.id.text_nombre_ejercicio);
            botonEditarNombre = itemView.findViewById(R.id.button_editar_nombre_ejercicio);
            series = itemView.findViewById(R.id.edit_series);
            repeticiones = itemView.findViewById(R.id.edit_repeticiones);
            peso = itemView.findViewById(R.id.edit_peso);
            esquemaProgresion = itemView.findViewById(R.id.spinner_esquema_progresion);
            repeticionesMax = itemView.findViewById(R.id.edit_repeticiones_max);
            botonQuitar = itemView.findViewById(R.id.button_quitar_ejercicio);

            esquemaProgresion.setAdapter(new ArrayAdapter<>(itemView.getContext(),
                    android.R.layout.simple_spinner_dropdown_item, OPCIONES_ESQUEMA));

            series.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.setSeries(parseIntOrZero(text));
                    }
                }
            });
            repeticiones.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.setRepeticiones(parseIntOrZero(text));
                    }
                }
            });
            peso.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.setPeso(parseDoubleOrZero(text));
                    }
                }
            });
            repeticionesMax.addTextChangedListener(new SimpleTextWatcher() {
                @Override
                public void onChanged(String text) {
                    if (current != null) {
                        current.setRepeticionesMax(parseIntOrZero(text));
                    }
                }
            });
            esquemaProgresion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (current != null) {
                        current.setEsquemaProgresion(position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        private void bind(RutinaEjercicio item, Ejercicio ejercicio) {
            current = null;
            nombre.setText(ejercicio != null ? ejercicio.getNombre() : "Ejercicio");
            botonEditarNombre.setVisibility(ejercicio != null && ejercicio.isPersonalizado()
                    ? View.VISIBLE : View.GONE);
            series.setText(String.valueOf(item.getSeries()));
            repeticiones.setText(String.valueOf(item.getRepeticiones()));
            peso.setText(String.valueOf(item.getPeso()));
            repeticionesMax.setText(String.valueOf(item.getRepeticionesMax()));
            esquemaProgresion.setSelection(item.getEsquemaProgresion());
            current = item;
        }
    }

    private static int parseIntOrZero(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double parseDoubleOrZero(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: error solo en `EditRoutineActivity.java` (aún no actualizado — se resuelve en la
Tarea 5); ningún error dentro de `RutinaEjercicioEditAdapter.java`.

- [ ] **Paso 3: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/RutinaEjercicioEditAdapter.java
git commit -m "Conectar la manija de arrastre y el lápiz de edición en el adaptador"
```

---

### Tarea 5: `EditRoutineActivity` — `ItemTouchHelper` + diálogo de edición

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java`

**Interfaces:**
- Consume: `RutinaEjercicioEditAdapter.Listener` (Tarea 4), `Rutina.moverEjercicio` (Tarea 2),
  `Ejercicio.setNombre/setGrupoMuscular` (existentes), layout `dialog_custom_exercise.xml`
  (existente, ids `edit_nombre_ejercicio_personalizado`/`edit_grupo_ejercicio_personalizado`).

- [ ] **Paso 1: Reemplazar el contenido completo del archivo**

```java
package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.util.HashMap;
import java.util.Map;

public class EditRoutineActivity extends BaseActivity {

    public static final String EXTRA_RUTINA_ID = "extra_rutina_id";

    private DataManager dataManager;
    private DataStore dataStore;
    private Rutina rutina;
    private boolean esNueva;

    private TextInputEditText editNombre;
    private RutinaEjercicioEditAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private Map<String, Ejercicio> catalogoPorId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_routine);

        configurarToolbar(R.id.toolbar, null, false);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();

        catalogoPorId = new HashMap<>();
        for (Ejercicio ejercicio : dataStore.getEjercicios()) {
            catalogoPorId.put(ejercicio.getId(), ejercicio);
        }

        String rutinaId = getIntent().getStringExtra(EXTRA_RUTINA_ID);
        if (rutinaId != null) {
            rutina = dataStore.buscarRutina(rutinaId);
            esNueva = false;
        }
        if (rutina == null) {
            rutina = new Rutina(dataManager.newId("r"), "");
            esNueva = true;
        }
        setTitle(esNueva ? "Nueva rutina" : "Editar rutina");

        editNombre = findViewById(R.id.edit_nombre_rutina);
        editNombre.setText(rutina.getNombre());

        RecyclerView recycler = findViewById(R.id.recycler_ejercicios_rutina);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RutinaEjercicioEditAdapter(rutina.getEjercicios(), catalogoPorId, new RutinaEjercicioEditAdapter.Listener() {
            @Override
            public void onQuitar(int position) {
                rutina.quitarEjercicio(position);
                adapter.notifyItemRemoved(position);
            }

            @Override
            public void onEditarPersonalizado(int position) {
                RutinaEjercicio item = rutina.getEjercicios().get(position);
                Ejercicio ejercicio = catalogoPorId.get(item.getEjercicioId());
                if (ejercicio != null) {
                    mostrarDialogoEditarEjercicio(ejercicio, position);
                }
            }

            @Override
            public void onIniciarArrastre(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
        recycler.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                   @NonNull RecyclerView.ViewHolder target) {
                int desde = viewHolder.getBindingAdapterPosition();
                int hasta = target.getBindingAdapterPosition();
                if (desde == RecyclerView.NO_POSITION || hasta == RecyclerView.NO_POSITION) {
                    return false;
                }
                rutina.moverEjercicio(desde, hasta);
                adapter.notifyItemMoved(desde, hasta);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Sin swipe: solo se usa arrastre vertical.
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recycler);

        findViewById(R.id.button_agregar_ejercicio).setOnClickListener(v ->
                EjercicioPicker.mostrar(this, dataManager, dataStore, this::agregarEjercicioARutina));
        findViewById(R.id.button_guardar_rutina).setOnClickListener(v -> guardarRutina());
    }

    private void agregarEjercicioARutina(Ejercicio ejercicio) {
        catalogoPorId.putIfAbsent(ejercicio.getId(), ejercicio);
        rutina.agregarEjercicio(new RutinaEjercicio(ejercicio.getId(), 3, 10, 0.0));
        adapter.notifyItemInserted(rutina.getCantidadEjercicios() - 1);
    }

    private void mostrarDialogoEditarEjercicio(Ejercicio ejercicio, int position) {
        View view = getLayoutInflater().inflate(R.layout.dialog_custom_exercise, null);
        TextInputEditText editNombreEjercicio = view.findViewById(R.id.edit_nombre_ejercicio_personalizado);
        TextInputEditText editGrupo = view.findViewById(R.id.edit_grupo_ejercicio_personalizado);
        editNombreEjercicio.setText(ejercicio.getNombre());
        editGrupo.setText(ejercicio.getGrupoMuscular());

        new AlertDialog.Builder(this)
                .setTitle("Editar ejercicio personalizado")
                .setMessage("El cambio se aplicará en todas las rutinas donde uses este ejercicio.")
                .setView(view)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nombreNuevo = editNombreEjercicio.getText() != null
                            ? editNombreEjercicio.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(nombreNuevo)) {
                        Toast.makeText(this, "Escribe un nombre", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String grupoNuevo = editGrupo.getText() != null
                            ? editGrupo.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(grupoNuevo)) {
                        grupoNuevo = "Personalizado";
                    }
                    ejercicio.setNombre(nombreNuevo);
                    ejercicio.setGrupoMuscular(grupoNuevo);
                    adapter.notifyItemChanged(position);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarRutina() {
        String nombre = editNombre.getText() != null ? editNombre.getText().toString().trim() : "";
        if (TextUtils.isEmpty(nombre)) {
            Toast.makeText(this, "Ponle un nombre a la rutina", Toast.LENGTH_SHORT).show();
            return;
        }
        if (rutina.getEjercicios().isEmpty()) {
            Toast.makeText(this, "Agrega al menos un ejercicio", Toast.LENGTH_SHORT).show();
            return;
        }
        rutina.setNombre(nombre);
        if (esNueva) {
            dataStore.getRutinas().add(rutina);
        }
        dataManager.save();
        finish();
    }
}
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL` — con esta tarea el proyecto completo vuelve a compilar limpio.

- [ ] **Paso 3: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/ui/EditRoutineActivity.java
git commit -m "Agregar arrastrar y soltar y edición de ejercicios personalizados"
```

---

### Tarea 6: Verificación en dispositivo real y cierre de la rebanada

**Files:** ninguno (solo build, ADB).

- [ ] **Paso 1: Respaldar `datos.json`**

```bash
export PATH="$PATH:/home/jdov/Android/Sdk/platform-tools"
adb shell run-as com.ironquest.mvp cat files/datos.json > \
  "/tmp/claude-1000/-home-jdov-Documentos-titan-score/b8c695f0-e7b0-48b1-bfbd-c5b4163060d1/scratchpad/datos_respaldo_slice2.json"
wc -l "/tmp/claude-1000/-home-jdov-Documentos-titan-score/b8c695f0-e7b0-48b1-bfbd-c5b4163060d1/scratchpad/datos_respaldo_slice2.json"
```
Expected: el archivo tiene contenido real (no vacío). **No continuar si falla.**

- [ ] **Paso 2: Compilar e instalar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: `BUILD SUCCESSFUL` y `Success`.

- [ ] **Paso 3: Verificar el arrastre en una rutina con 5+ ejercicios**

Abrir "Editar rutina" en una rutina con varios ejercicios (p. ej. "push"). Mantener presionada
la manija (☰) de un ejercicio y arrastrarlo a otra posición. Expected: el orden visual cambia
y se mantiene al soltar.

- [ ] **Paso 4: Verificar la edición de un personalizado**

Confirmar que solo los ejercicios personalizados (creados con "+ Ejercicio personalizado")
muestran el lápiz; tocar uno, cambiar nombre/grupo, guardar. Expected: el cambio se refleja
en la tarjeta inmediatamente. Si ese mismo ejercicio aparece en otra rutina, abrir esa otra
rutina y confirmar que también se ve el nombre nuevo.

- [ ] **Paso 5: Guardar y confirmar en `datos.json`**

Tocar "Guardar rutina". Luego:

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json
```
Expected: el orden de `ejercicios` dentro de esa rutina refleja el arrastre hecho, y el
`Ejercicio` editado tiene el nombre/grupo nuevos — el resto de rutinas y sesiones intacto.

---

## Rebanada 2 — Exportar/importar una rutina

### Tarea 7: Modelo `RutinaCompartida`

**Files:**
- Create: `app/src/main/java/com/ironquest/mvp/model/RutinaCompartida.java`

**Interfaces:**
- Produce: `RutinaCompartida` con `getRutina()/setRutina(Rutina)`,
  `getEjercicios()/setEjercicios(List<Ejercicio>)` — usado por las Tareas 8, 10.

- [ ] **Paso 1: Crear el archivo completo**

```java
package com.ironquest.mvp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Paquete para compartir una sola rutina entre instalaciones: la rutina y los ejercicios
 * completos que referencia, para que quien la reciba no necesite tenerlos ya. No hereda de
 * {@link EntidadIdentificable}: es un contenedor sin identidad propia, igual que {@link DataStore}.
 */
public class RutinaCompartida {

    private Rutina rutina;
    private List<Ejercicio> ejercicios = new ArrayList<>();

    public RutinaCompartida() {
    }

    public Rutina getRutina() {
        return rutina;
    }

    public void setRutina(Rutina rutina) {
        this.rutina = rutina;
    }

    public List<Ejercicio> getEjercicios() {
        return ejercicios;
    }

    public void setEjercicios(List<Ejercicio> ejercicios) {
        this.ejercicios = ejercicios;
    }
}
```

- [ ] **Paso 2: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Paso 3: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/model/RutinaCompartida.java
git commit -m "Agregar el modelo RutinaCompartida para exportar una sola rutina"
```

---

### Tarea 8: `DataManager` — exportar/importar una rutina

**Files:**
- Modify: `app/src/main/java/com/ironquest/mvp/data/DataManager.java`

**Interfaces:**
- Consume: `RutinaCompartida` (Tarea 7), `DataStore.buscarEjercicio(String)` (existente),
  `Rutina.agregarEjercicio` (existente).
- Produce: `DataManager.exportarRutinaComoArchivo(Rutina): File`,
  `DataManager.leerRutinaCompartidaDesde(InputStream): RutinaCompartida`,
  `DataManager.importarRutina(RutinaCompartida): Rutina` — usados por las Tareas 9, 10.

- [ ] **Paso 1: Agregar imports**

Agregar junto a los imports existentes (después de `import com.ironquest.mvp.model.Ejercicio;`):

```java
import com.ironquest.mvp.model.Rutina;
import com.ironquest.mvp.model.RutinaCompartida;
import com.ironquest.mvp.model.RutinaEjercicio;

import java.util.HashMap;
import java.util.Map;
```

- [ ] **Paso 2: Agregar los tres métodos nuevos**

Insertar antes del método `private DataStore load()`:

```java
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
```

- [ ] **Paso 3: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Paso 4: Commit**

```bash
git add app/src/main/java/com/ironquest/mvp/data/DataManager.java
git commit -m "Agregar exportar/importar una rutina individual en DataManager"
```

---

### Tarea 9: Botón "Compartir" en cada rutina

**Files:**
- Modify: `app/src/main/res/layout/item_rutina.xml`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/RutinaAdapter.java`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/RoutineListFragment.java`

**Interfaces:**
- Consume: `DataManager.exportarRutinaComoArchivo(Rutina)` (Tarea 8).
- Produce: `RutinaAdapter.Listener.onCompartirClick(Rutina)` — implementado en
  `RoutineListFragment`, no consumido por otras tareas.

- [ ] **Paso 1: Agregar el botón en `item_rutina.xml`**

Reemplazar el bloque del `ImageButton` de editar (líneas 37-43) por:

```xml
        <ImageButton
            android:id="@+id/button_compartir_rutina"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Compartir rutina"
            android:src="@android:drawable/ic_menu_share" />

        <ImageButton
            android:id="@+id/button_editar_rutina"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Editar rutina"
            android:src="@android:drawable/ic_menu_edit" />
```

- [ ] **Paso 2: Reemplazar el contenido completo de `RutinaAdapter.java`**

```java
package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Rutina;

import java.util.List;

public class RutinaAdapter extends RecyclerView.Adapter<RutinaAdapter.ViewHolder> {

    public interface Listener {
        void onRutinaClick(Rutina rutina);
        void onEditarClick(Rutina rutina);
        void onCompartirClick(Rutina rutina);
    }

    private final List<Rutina> rutinas;
    private final Listener listener;

    public RutinaAdapter(List<Rutina> rutinas, Listener listener) {
        this.rutinas = rutinas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rutina, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Rutina rutina = rutinas.get(position);
        holder.nombre.setText(rutina.getNombre());
        int cantidadEjercicios = rutina.getCantidadEjercicios();
        holder.resumen.setText(cantidadEjercicios == 1
                ? "1 ejercicio"
                : cantidadEjercicios + " ejercicios");
        holder.itemView.setOnClickListener(v -> listener.onRutinaClick(rutina));
        holder.botonEditar.setOnClickListener(v -> listener.onEditarClick(rutina));
        holder.botonCompartir.setOnClickListener(v -> listener.onCompartirClick(rutina));
    }

    @Override
    public int getItemCount() {
        return rutinas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nombre;
        private final TextView resumen;
        private final ImageButton botonEditar;
        private final ImageButton botonCompartir;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.text_nombre_rutina);
            resumen = itemView.findViewById(R.id.text_resumen_rutina);
            botonEditar = itemView.findViewById(R.id.button_editar_rutina);
            botonCompartir = itemView.findViewById(R.id.button_compartir_rutina);
        }
    }
}
```

- [ ] **Paso 3: Agregar el manejo en `RoutineListFragment.java`**

Agregar imports (junto a los existentes):

```java
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
```

Agregar el método `onCompartirClick`, después de `onEditarClick` (al final de la clase, antes
del `}` de cierre):

```java

    @Override
    public void onCompartirClick(Rutina rutina) {
        try {
            File archivo = dataManager.exportarRutinaComoArchivo(rutina);
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", archivo);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartir rutina"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo compartir: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
```

- [ ] **Paso 4: Verificar que compila**

```bash
./gradlew compileDebugJava
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Paso 5: Commit**

```bash
git add app/src/main/res/layout/item_rutina.xml \
  app/src/main/java/com/ironquest/mvp/ui/RutinaAdapter.java \
  app/src/main/java/com/ironquest/mvp/ui/RoutineListFragment.java
git commit -m "Agregar botón para compartir una rutina individual"
```

---

### Tarea 10: "Importar rutina" en el menú principal

**Files:**
- Modify: `app/src/main/res/menu/menu_main.xml`
- Modify: `app/src/main/java/com/ironquest/mvp/ui/MainActivity.java`

**Interfaces:**
- Consume: `DataManager.leerRutinaCompartidaDesde(InputStream)`,
  `DataManager.importarRutina(RutinaCompartida)` (Tarea 8).

- [ ] **Paso 1: Agregar el ítem de menú**

En `app/src/main/res/menu/menu_main.xml`, agregar, después del ítem `action_importar`:

```xml
    <item
        android:id="@+id/action_importar_rutina"
        android:title="Importar rutina"
        android:showAsAction="never" />
```

- [ ] **Paso 2: Agregar imports en `MainActivity.java`**

Agregar junto a los imports existentes:

```java
import com.ironquest.mvp.model.RutinaCompartida;
```

- [ ] **Paso 3: Agregar el segundo `ActivityResultLauncher`**

Reemplazar (línea 52):

```java
    private ActivityResultLauncher<String[]> importLauncher;
```

por:

```java
    private ActivityResultLauncher<String[]> importLauncher;
    private ActivityResultLauncher<String[]> importRutinaLauncher;
```

Reemplazar (líneas 79-80):

```java
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onArchivoSeleccionado);
```

por:

```java
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onArchivoSeleccionado);
        importRutinaLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::onArchivoRutinaSeleccionado);
```

- [ ] **Paso 4: Agregar el manejo del menú**

Reemplazar (líneas 188-191):

```java
        if (id == R.id.action_importar) {
            importLauncher.launch(new String[]{"application/json", "text/plain", "application/octet-stream", "*/*"});
            return true;
        }
```

por:

```java
        if (id == R.id.action_importar) {
            importLauncher.launch(new String[]{"application/json", "text/plain", "application/octet-stream", "*/*"});
            return true;
        }
        if (id == R.id.action_importar_rutina) {
            importRutinaLauncher.launch(new String[]{"application/json", "text/plain", "application/octet-stream", "*/*"});
            return true;
        }
```

- [ ] **Paso 5: Agregar el método `onArchivoRutinaSeleccionado`**

Agregar después de `onArchivoSeleccionado` (al final de la clase, antes del `}` de cierre):

```java

    private void onArchivoRutinaSeleccionado(Uri uri) {
        if (uri == null) {
            return;
        }
        RutinaCompartida paquete;
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new java.io.IOException("No se pudo abrir el archivo");
            }
            paquete = dataManager.leerRutinaCompartidaDesde(inputStream);
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo leer el archivo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        int cantidadEjercicios = paquete.getRutina().getCantidadEjercicios();
        new AlertDialog.Builder(this)
                .setTitle("Importar rutina")
                .setMessage("¿Agregar la rutina \"" + paquete.getRutina().getNombre() + "\" con "
                        + cantidadEjercicios + " ejercicios? Tus rutinas actuales no se modifican.")
                .setPositiveButton("Agregar", (dialog, which) -> {
                    dataManager.importarRutina(paquete);
                    mostrarTab(tabActual);
                    Toast.makeText(this, "Rutina agregada correctamente", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
```

- [ ] **Paso 6: Verificar que compila**

```bash
./gradlew assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Paso 7: Commit**

```bash
git add app/src/main/res/menu/menu_main.xml \
  app/src/main/java/com/ironquest/mvp/ui/MainActivity.java
git commit -m "Agregar Importar rutina al menú principal"
```

---

### Tarea 11: Verificación en dispositivo real y cierre de la rebanada

**Files:** ninguno (solo build, ADB).

- [ ] **Paso 1: Respaldar `datos.json`**

```bash
export PATH="$PATH:/home/jdov/Android/Sdk/platform-tools"
adb shell run-as com.ironquest.mvp cat files/datos.json > \
  "/tmp/claude-1000/-home-jdov-Documentos-titan-score/b8c695f0-e7b0-48b1-bfbd-c5b4163060d1/scratchpad/datos_respaldo_slice2_export.json"
wc -l "/tmp/claude-1000/-home-jdov-Documentos-titan-score/b8c695f0-e7b0-48b1-bfbd-c5b4163060d1/scratchpad/datos_respaldo_slice2_export.json"
```
Expected: contenido real. **No continuar si falla.**

- [ ] **Paso 2: Compilar e instalar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Expected: `BUILD SUCCESSFUL` y `Success`.

- [ ] **Paso 3: Exportar una rutina con un ejercicio personalizado**

Tocar el ícono de compartir en una rutina que tenga al menos un ejercicio personalizado.
Expected: se abre el selector de Android para compartir un archivo `.json`; se puede guardar
o enviar (p. ej. a "Archivos" o a uno mismo por algún medio).

- [ ] **Paso 4: Importar esa misma rutina (con aprobación explícita antes de este paso, ya
  que agrega una rutina real al `datos.json` real)**

Ir a Importar rutina, elegir el archivo exportado en el Paso 3. Confirmar el diálogo.
Expected: aparece una rutina nueva en la lista, con el mismo nombre y cantidad de ejercicios;
las rutinas existentes no cambian.

- [ ] **Paso 5: Confirmar en `datos.json`**

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json
```
Expected: la rutina importada aparece con un id nuevo distinto al original; el ejercicio
personalizado que traía aparece como un `Ejercicio` nuevo (id nuevo, `personalizado: true`);
ninguna rutina, sesión ni ejercicio previo se modificó o se perdió.

---

## Auto-revisión

**Cobertura del spec:** marcar personalizado → Tarea 1. Editar nombre/grupo → Tareas 3, 4, 5.
Reordenar con arrastre → Tareas 2, 3, 4, 5. Exportar/importar sin reemplazar → Tareas 7, 8, 9,
10. Los dos commits-checkpoint del spec (rebanada de edición/reorden, rebanada de compartir)
se respetan en las Tareas 6 y 11.

**Placeholders:** ninguno — cada paso de código trae el archivo completo o el fragmento exacto
a reemplazar.

**Consistencia de tipos:** `RutinaEjercicioEditAdapter.Listener` se define en la Tarea 4 con
tres métodos y se implementa exactamente igual en la Tarea 5. `RutinaCompartida` (Tarea 7) se
usa con la misma forma en `DataManager` (Tarea 8) y en `MainActivity` (Tarea 10).
