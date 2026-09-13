# Ampliación del catálogo de ejercicios por equipo del gimnasio — Implementation Plan

> **For agentic workers:** Tasks 1-5 (preparación de datos) — REQUIRED SUB-SKILL:
> `superpowers:executing-plans` si se ejecutan dentro de Claude Code (son secuenciales y
> dependientes entre sí, no aptas para `subagent-driven-development` en paralelo). Task 6
> (código) — ejecutada por una herramienta externa (OpenCode/"big pickle"), sin dependencia de
> ningún skill de Claude Code; el paso a paso es autocontenido. Steps usan checkbox (`- [ ]`)
> para tracking.

> **Para quien ejecute esto (cualquier agente, no solo Claude):** lee primero
> `IronQuestApp/CLAUDE.md` (convenciones, trampas conocidas, invariantes) y el spec de diseño en
> [`docs/specs/2026-09-13-ampliacion-catalogo-gimnasio-design.md`](../specs/2026-09-13-ampliacion-catalogo-gimnasio-design.md).
> Ambos son autocontenidos. Este plan asume cero contexto de conversación previa.
>
> **Adaptación de formato:** este plan no sigue TDD clásico (no hay suite de tests en este
> proyecto Android — ver `CLAUDE.md`). Cada tarea igual termina en una verificación concreta y
> ejecutable, siguiendo el patrón de verificación manual que ya usa el proyecto
> (compilar → instalar → inspeccionar `datos.json` vía ADB).
>
> **División de trabajo:** Tareas 1-5 las ejecuta Claude (generación de datos — requiere
> descargar ~200 assets de internet y traducir texto). Tarea 6 la ejecuta **big pickle**
> (OpenCode) — es puramente código Java + build + verificación, sin necesidad de acceso a
> internet más allá de lo que ya use Gradle.

**Goal:** Extender `app/src/main/assets/catalogo.json` de 80 a ~277 ejercicios (80 + ~197
nuevos), filtrados por el equipo real del gimnasio del usuario, sin tocar ni un solo ejercicio
existente y sin requerir cambios de UI.

**Architecture:** Mismo esquema de datos que Slice 5 (`Ejercicio.java`, sin campos nuevos).
Los ejercicios nuevos se generan fuera de la app (script Python + descargas), se agregan al
mismo `catalogo.json`, y la migración aditiva ya existente (`migrarCatalogoDataset()`) los
recoge en el próximo bump de `catalogoVersion`.

**Tech Stack:** Python 3 stdlib (sin pip) para el pipeline de datos, `curl` para las descargas,
Gradle/ADB para la verificación en dispositivo.

## Global Constraints

- **Migración aditiva y nunca destructiva** — los IDs `ex1..ex20` y `gv_0001..gv_9999` ya
  existentes en `catalogo.json` no se tocan ni se reordenan (`CLAUDE.md`, trampa #5).
- **`id` nuevo = `"gv_" + id del dataset`** (ej. `gv_0301`), sin excepción — es lo que evita
  colisión con `ex1..ex20` y con personalizados (`ex_xxxxxxxx`).
- **`musculoObjetivo` debe ser uno de los 15 valores fijos** de `Ejercicio.MUSCULOS_OBJETIVO`
  (Pectorales, Dorsales, Trapecios, Espalda alta, Zona lumbar, Cuádriceps, Isquiotibiales,
  Glúteos, Aductores/Abductores, Pantorrillas, Hombros, Bíceps, Tríceps, Antebrazos, Abdomen) —
  un typo no rompe el build, solo deja el ejercicio invisible bajo cualquier chip.
- **Formato exacto de `catalogo.json`:** `json.dump(data, f, indent=2, ensure_ascii=False)`,
  orden de claves `id, nombre, grupoMuscular, equipo, musculoObjetivo, musculosSecundarios,
  instrucciones, imgAsset, gifAsset, personalizado`, sin newline final. Verificado byte-a-byte
  que este formato reproduce el archivo actual sin cambios — cualquier desviación ensucia el
  diff de git sobre las 80 entradas existentes.
- **`JAVA_HOME`** no está en `PATH`: `export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr`.
- **No usar pipe con `./gradlew`** sin comprobar `${PIPESTATUS[0]}` — un build fallido puede
  parecer exitoso.
- **Mensajes de commit:** español, imperativo, una línea.
- **No tocar** `ui/MigracionEjerciciosDialog.java`, `ui/EjercicioPicker.java`,
  `ui/EjercicioPickerDialog.java`, `ui/RutinaEjercicioEditAdapter.java`,
  `res/layout/activity_edit_routine.xml`, `res/layout/item_ejercicio_rutina_edit.xml` — tienen
  cambios sin commitear de otra slice en curso.

---

### Task 1: Generar shortlist recortada (684 candidatos → ~197)

**Files:**
- Create: `/tmp/titan-score-catalogo/exercises.json` (descarga, no se commitea)
- Create: `/tmp/titan-score-catalogo/recortar_shortlist.py`
- Create: `/tmp/titan-score-catalogo/shortlist.md` (salida, no se commitea)

**Interfaces:**
- Consumes: `IronQuestApp/app/src/main/assets/catalogo.json` (80 ids ya usados, para excluir)
- Produces: `shortlist.md` — lista de ~197 candidatos agrupados por estación, con `id` (formato
  `gv_XXXX`), `name` en inglés, `equipment`, `body_part/target`. Task 2 consume esta lista.

- [ ] **Paso 1: Descargar el dataset completo**

```bash
mkdir -p /tmp/titan-score-catalogo && cd /tmp/titan-score-catalogo
curl -sL --max-time 60 -o exercises.json \
  "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/data/exercises.json"
python3 -c "import json; print(len(json.load(open('exercises.json'))))"
```

Expected: `1324`

- [ ] **Paso 2: Guardar el script de recorte**

Guardar como `/tmp/titan-score-catalogo/recortar_shortlist.py` (ya verificado en esta sesión,
produce 197 candidatos):

```python
import json, re, collections

d = json.load(open('exercises.json'))
cat = json.load(open('/home/jdov/Documentos/titan score/IronQuestApp/app/src/main/assets/catalogo.json'))
used_ids = set(x['id'].replace('gv_', '') for x in cat)
pool = [x for x in d if x['id'] not in used_ids]

def any_word(name, *words):
    n = name.lower()
    return any(re.search(r'\b' + re.escape(w) + r'\b', n) for w in words)

UPPER_BODY_PARTS = {'chest', 'back', 'shoulders', 'upper arms', 'lower arms', 'waist'}

STATIONS = [
    ('01_press_banca_mancuernas', lambda x: x['equipment'] == 'dumbbell' and x['body_part'] == 'chest' and any_word(x['name'], 'press')),
    ('02_polea_dual_tren_superior', lambda x: x['equipment'] == 'cable' and x['body_part'] in UPPER_BODY_PARTS),
    ('03_banco_predicador', lambda x: any_word(x['name'], 'preacher')),
    ('04_fondos_dips', lambda x: any_word(x['name'], 'dip', 'dips')),
    ('05_polea_alta_espalda', lambda x: x['equipment'] == 'cable' and x['body_part'] == 'back' and (any_word(x['name'], 'pulldown') or 'pull-down' in x['name'].lower() or any_word(x['name'], 'pullover') or 'pull-over' in x['name'].lower())),
    ('06_polea_baja_espalda', lambda x: x['equipment'] == 'cable' and x['body_part'] == 'back' and any_word(x['name'], 'row')),
    ('07_smith_machine', lambda x: x['equipment'] == 'smith machine'),
    ('08_remo_t', lambda x: bool(re.search(r'(?<![a-z])t[- ]bar(?![a-z])', x['name'].lower()))),
    ('09_pec_deck', lambda x: x['equipment'] == 'leverage machine' and x['body_part'] in ('chest', 'shoulders', 'back') and ('fly' in x['name'].lower() or 'pec deck' in x['name'].lower())),
    ('10_aductores_abductores', lambda x: x['target'] in ('adductors', 'abductors') or any_word(x['name'], 'adductor', 'abductor')),
    ('11_prensa_piernas', lambda x: 'leg press' in x['name'].lower()),
    ('12_hack_squat', lambda x: 'hack squat' in x['name'].lower()),
    ('13_rack_sentadilla', lambda x: x['equipment'] in ('barbell', 'olympic barbell') and any_word(x['name'], 'squat')),
    ('14_leg_extension', lambda x: 'leg extension' in x['name'].lower()),
    ('15_femoral_acostado', lambda x: 'leg curl' in x['name'].lower() and ('lying' in x['name'].lower() or 'prone' in x['name'].lower())),
    ('17_pantorrillas_sentado', lambda x: 'seated calf raise' in x['name'].lower()),
]
# 16 (hip thrust con barra/maquina) y 18 (v-squat) se omiten a propósito: 0 resultados fieles
# en el dataset (ver spec de diseño, sección "Fuera de alcance").

EXCLUDE_PATTERNS = [r'\(back pov\)', r'\(side pov\)', r'\bv\.\s*[23]\b', r'exercise ball', r'stability ball', r'bosu']
def es_ruido(name):
    n = name.lower()
    return any(re.search(p, n) for p in EXCLUDE_PATTERNS)

MAX_PER_STATION = {
    '01_press_banca_mancuernas': 10, '02_polea_dual_tren_superior': 40, '03_banco_predicador': 12,
    '04_fondos_dips': 12, '05_polea_alta_espalda': 12, '06_polea_baja_espalda': 12,
    '07_smith_machine': 20, '08_remo_t': None, '09_pec_deck': None, '10_aductores_abductores': None,
    '11_prensa_piernas': None, '12_hack_squat': None, '13_rack_sentadilla': 12,
    '14_leg_extension': None, '15_femoral_acostado': None, '17_pantorrillas_sentado': None,
}

already = set()
shortlist = collections.OrderedDict()
for key, fn in STATIONS:
    items = [x for x in pool if fn(x) and not es_ruido(x['name']) and x['id'] not in already]
    items = sorted(items, key=lambda x: x['name'])
    cap = MAX_PER_STATION[key]
    if cap:
        items = items[:cap]
    shortlist[key] = items
    already |= set(x['id'] for x in items)

pesos_libres = [x for x in pool if x['equipment'] in ('dumbbell', 'barbell', 'ez barbell', 'olympic barbell', 'trap bar')
                and x['id'] not in already and not es_ruido(x['name'])]
pesos_libres = sorted(pesos_libres, key=lambda x: x['name'])[:40]
shortlist['19_pesos_libres_otros'] = pesos_libres

total = 0
out = []
for key, items in shortlist.items():
    out.append(f'## {key} ({len(items)})')
    for x in items:
        out.append(f"- gv_{x['id']} | {x['name']} | {x['equipment']} | {x['body_part']}/{x['target']}")
    total += len(items)
out.append(f'\nTOTAL: {total}')
open('shortlist.md', 'w').write('\n'.join(out))
print('TOTAL', total)
for key, items in shortlist.items():
    print(' ', key, len(items))
```

- [ ] **Paso 3: Correr el script**

```bash
cd /tmp/titan-score-catalogo && python3 recortar_shortlist.py
```

Expected (verificado en esta sesión de diseño):
```
TOTAL 197
  01_press_banca_mancuernas 10
  02_polea_dual_tren_superior 40
  03_banco_predicador 12
  04_fondos_dips 12
  05_polea_alta_espalda 9
  06_polea_baja_espalda 12
  07_smith_machine 20
  08_remo_t 2
  09_pec_deck 3
  10_aductores_abductores 10
  11_prensa_piernas 6
  12_hack_squat 3
  13_rack_sentadilla 12
  14_leg_extension 1
  15_femoral_acostado 2
  17_pantorrillas_sentado 3
  19_pesos_libres_otros 40
```

- [ ] **Paso 4: Checkpoint de aprobación (manual, no scriptable)**

Mostrar `shortlist.md` al usuario agrupado por estación. Es el punto de control acordado en el
spec — el usuario puede tachar ejercicios individuales antes de que se descarguen assets. No
avanzar a Task 2 sin esta aprobación explícita.

---

### Task 2: Traducir y mapear campos → construir el delta de `catalogo.json`

**Files:**
- Create: `/tmp/titan-score-catalogo/mapeos.py`
- Create: `/tmp/titan-score-catalogo/delta_catalogo.json`

**Interfaces:**
- Consumes: `shortlist.md` (Task 1, ya aprobada por el usuario)
- Produces: `delta_catalogo.json` — lista JSON de objetos `Ejercicio` (mismo esquema que
  `catalogo.json`), **sin** `imgAsset`/`gifAsset` todavía (Task 3 los completa)

Los 4 mapeos mecánicos (deterministas, sin juicio humano) — **completos, no parciales**:

```python
GRUPO_MUSCULAR_MAP = {
    'chest': 'Pecho', 'back': 'Espalda', 'upper legs': 'Pierna', 'lower legs': 'Pierna',
    'shoulders': 'Hombro', 'upper arms': 'Brazo', 'lower arms': 'Brazo', 'waist': 'Abdomen',
}

EQUIPO_MAP = {
    'body weight': 'Peso corporal', 'dumbbell': 'Mancuerna', 'cable': 'Polea',
    'barbell': 'Barra', 'ez barbell': 'Barra', 'olympic barbell': 'Barra', 'trap bar': 'Barra',
    'leverage machine': 'Máquina de palanca', 'band': 'Banda elástica',
    'resistance band': 'Banda elástica', 'smith machine': 'Máquina Smith',
    'sled machine': 'Máquina de deslizamiento', 'weighted': 'Con peso adicional',
    'assisted': 'Asistido',
}

MUSCULO_OBJETIVO_MAP = {
    'pectorals': 'Pectorales', 'serratus anterior': 'Pectorales', 'lats': 'Dorsales',
    'traps': 'Trapecios', 'upper back': 'Espalda alta', 'spine': 'Zona lumbar',
    'quads': 'Cuádriceps', 'hamstrings': 'Isquiotibiales', 'glutes': 'Glúteos',
    'abductors': 'Aductores/Abductores', 'adductors': 'Aductores/Abductores',
    'calves': 'Pantorrillas', 'delts': 'Hombros', 'biceps': 'Bíceps', 'triceps': 'Tríceps',
    'forearms': 'Antebrazos', 'abs': 'Abdomen',
}

SECUNDARIOS_MAP = {
    'shoulders': 'Hombros', 'hamstrings': 'Isquiotibiales', 'forearms': 'Antebrazos',
    'triceps': 'Tríceps', 'biceps': 'Bíceps', 'quadriceps': 'Cuádriceps', 'calves': 'Pantorrillas',
    'glutes': 'Glúteos', 'core': 'Core', 'chest': 'Pecho', 'hip flexors': 'Flexores de cadera',
    'obliques': 'Oblicuos', 'lower back': 'Zona lumbar', 'rhomboids': 'Romboides',
    'trapezius': 'Trapecios', 'upper back': 'Espalda alta', 'traps': 'Trapecios',
    'deltoids': 'Deltoides', 'rear deltoids': 'Deltoides posterior', 'brachialis': 'Braquial',
    'back': 'Espalda', 'ankles': 'Tobillos', 'feet': 'Pies', 'rotator cuff': 'Manguito rotador',
    'latissimus dorsi': 'Dorsales', 'ankle stabilizers': 'Estabilizadores del tobillo',
    'soleus': 'Sóleo', 'wrists': 'Muñecas', 'upper chest': 'Pecho superior',
    'wrist flexors': 'Flexores de muñeca', 'wrist extensors': 'Extensores de muñeca',
    'abdominals': 'Abdominales', 'sternocleidomastoid': 'Esternocleidomastoideo',
    'hands': 'Manos', 'groin': 'Ingle', 'grip muscles': 'Músculos de agarre',
    'lower abs': 'Abdomen inferior', 'lats': 'Dorsales', 'inner thighs': 'Aductores',
    'shins': 'Espinillas',
}
```

`MUSCULO_OBJETIVO_MAP` y `GRUPO_MUSCULAR_MAP` son los mismos ya fijados en Slice 5 (spec
`docs/specs/2026-09-13-ampliacion-catalogo-gimnasio-design.md`, sección "Pipeline de datos y
mapeos"), copiados literal. `EQUIPO_MAP` extiende el diccionario ya usado en las 80 entradas
existentes con `Máquina Smith` y `Máquina de deslizamiento` (nuevos). `SECUNDARIOS_MAP` cubre
las 38 variantes de `secondary_muscles` que existen en el dataset completo — no hace falta
ampliarlo aunque cambie la selección final de la shortlist.

**El campo `nombre` es el único que requiere juicio humano** (traducción a término estándar de
gimnasio, no mecánica) — mismo criterio que se usó para las 80 entradas actuales. Estilo
confirmado inspeccionando `catalogo.json`: mayúscula inicial solamente, español natural,
equipo/variante incluido cuando aclara ("con barra", "en polea", "en máquina"). Ejemplos reales
ya traducidos para calibrar el criterio:

| Inglés (`name`) | Español (`nombre`) |
|---|---|
| dumbbell decline bench press | Press banca declinado con mancuerna |
| dumbbell incline hammer press | Press inclinado martillo con mancuerna |
| cable seated row | Remo sentado en polea |
| cable lateral pulldown | Jalón al pecho en polea |
| barbell preacher curl | Curl predicador con barra |
| dumbbell preacher curl | Curl predicador con mancuerna |
| chest dip | Fondos de pecho |
| lever t bar row | Remo en máquina T |
| lever seated fly | Aperturas en pec deck |
| lever seated hip adduction | Aducción de cadera en máquina |
| smith squat | Sentadilla en máquina Smith |
| barbell hack squat | Hack squat con barra |
| lever lying leg curl | Curl femoral acostado en máquina |
| barbell romanian deadlift | Peso muerto rumano con barra |
| dumbbell walking lunge | Zancada caminando con mancuerna |

- [ ] **Paso 1: Traducir cada `nombre` de la shortlist aprobada**

Ejecutor (Claude): recorrer `shortlist.md` línea por línea, escribir el `nombre` en español
siguiendo el estilo de la tabla de ejemplos. Guardar como diccionario `{id: nombre_es}` en
`traducciones.py` dentro del mismo directorio scratch.

- [ ] **Paso 2: Construir `delta_catalogo.json`**

```python
import json

d = json.load(open('exercises.json'))
by_id = {x['id']: x for x in d}
shortlist_ids = [...]  # ids aprobados en Task 1 (sin prefijo gv_)
from traducciones import TRADUCCIONES  # {id: nombre_es}, del paso anterior

delta = []
for eid in shortlist_ids:
    ex = by_id[eid]
    if ex['equipment'] not in EQUIPO_MAP:
        raise ValueError(f'equipment sin mapear: {ex["equipment"]} (id {eid})')
    if ex['target'] not in MUSCULO_OBJETIVO_MAP:
        raise ValueError(f'target sin mapear: {ex["target"]} (id {eid})')
    secundarios = []
    for s in ex['secondary_muscles']:
        if s not in SECUNDARIOS_MAP:
            raise ValueError(f'secondary_muscle sin mapear: {s} (id {eid})')
        secundarios.append(SECUNDARIOS_MAP[s])
    delta.append({
        'id': f'gv_{eid}',
        'nombre': TRADUCCIONES[eid],
        'grupoMuscular': GRUPO_MUSCULAR_MAP[ex['body_part']],
        'equipo': EQUIPO_MAP[ex['equipment']],
        'musculoObjetivo': MUSCULO_OBJETIVO_MAP[ex['target']],
        'musculosSecundarios': secundarios,
        'instrucciones': ex['instruction_steps']['es'],
        'imgAsset': f'img/gv_{eid}.jpg',
        'gifAsset': f'gifs/gv_{eid}.gif',
        'personalizado': False,
    })

json.dump(delta, open('delta_catalogo.json', 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
print(len(delta), 'ejercicios en el delta')
```

Fallar rápido (`raise`) ante cualquier valor sin mapear es intencional — un `.get(x, 'algo')`
silencioso dejaría un ejercicio con `musculoObjetivo=None` invisible en los chips sin que nadie
lo note (mismo bug silencioso que ya advierte el spec).

- [ ] **Paso 3: Verificar el delta**

```bash
python3 -c "
import json
delta = json.load(open('delta_catalogo.json'))
validos = {'Pectorales','Dorsales','Trapecios','Espalda alta','Zona lumbar','Cuádriceps',
  'Isquiotibiales','Glúteos','Aductores/Abductores','Pantorrillas','Hombros','Bíceps',
  'Tríceps','Antebrazos','Abdomen'}
for e in delta:
    assert e['musculoObjetivo'] in validos, e['id']
    assert e['instrucciones'], e['id']
    assert e['nombre'][0].isupper(), e['id']
ids = [e['id'] for e in delta]
assert len(ids) == len(set(ids)), 'ids duplicados dentro del delta'
print('OK,', len(delta), 'ejercicios validados')
"
```

Expected: `OK, 197 ejercicios validados` (o el número que haya quedado tras el checkpoint de
Task 1 si el usuario tachó alguno).

---

### Task 3: Descargar GIF + imagen para cada entrada del delta

**Files:**
- Create: `IronQuestApp/app/src/main/assets/img/gv_XXXX.jpg` (~197 archivos nuevos)
- Create: `IronQuestApp/app/src/main/assets/gifs/gv_XXXX.gif` (~197 archivos nuevos)

**Interfaces:**
- Consumes: `delta_catalogo.json` (Task 2), `exercises.json` (Task 1, para las rutas `image`/`gif_url` originales)

- [ ] **Paso 1: Descargar**

```python
import json, subprocess, os

d = {x['id']: x for x in json.load(open('exercises.json'))}
delta = json.load(open('delta_catalogo.json'))
base = 'https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/'
dest = '/home/jdov/Documentos/titan score/IronQuestApp/app/src/main/assets'

for e in delta:
    eid = e['id'].replace('gv_', '')
    orig = d[eid]
    for url_field, dest_path in [(orig['image'], f"{dest}/{e['imgAsset']}"),
                                   (orig['gif_url'], f"{dest}/{e['gifAsset']}")]:
        if os.path.exists(dest_path):
            continue
        r = subprocess.run(['curl', '-sL', '--max-time', '30', '-o', dest_path, base + url_field])
        if r.returncode != 0:
            raise RuntimeError(f'descarga falló: {url_field} (id {eid})')
print('descarga completa')
```

- [ ] **Paso 2: Verificar conteo e integridad**

```bash
cd "/home/jdov/Documentos/titan score/IronQuestApp/app/src/main/assets"
python3 -c "
import json, os
delta = json.load(open('/tmp/titan-score-catalogo/delta_catalogo.json'))
faltantes = [e['id'] for e in delta if not os.path.exists(e['imgAsset']) or not os.path.exists(e['gifAsset'])]
assert not faltantes, f'faltan assets: {faltantes}'
print('OK, todos los assets presentes:', len(delta) * 2, 'archivos')
"
du -sh gifs/ img/
```

Expected: sin faltantes; tamaño total de `gifs/` + `img/` crece de ~8.6 MB a ~28-30 MB (197
ejercicios × ~104 KB promedio, ver spec).

---

### Task 4: Fusionar el delta en `catalogo.json` (aditivo, diff limpio)

**Files:**
- Modify: `IronQuestApp/app/src/main/assets/catalogo.json`

**Interfaces:**
- Consumes: `catalogo.json` actual (80 entradas) + `delta_catalogo.json` (Task 2, ~197 entradas)

- [ ] **Paso 1: Fusionar preservando formato exacto**

```python
import json

path = '/home/jdov/Documentos/titan score/IronQuestApp/app/src/main/assets/catalogo.json'
actual = json.load(open(path, encoding='utf-8'))
delta = json.load(open('/tmp/titan-score-catalogo/delta_catalogo.json', encoding='utf-8'))

ids_actuales = {e['id'] for e in actual}
colision = [e['id'] for e in delta if e['id'] in ids_actuales]
assert not colision, f'colisión de id con catálogo existente: {colision}'

fusion = actual + delta
json.dump(fusion, open(path, 'w', encoding='utf-8'), indent=2, ensure_ascii=False)
print(len(actual), '+', len(delta), '=', len(fusion))
```

**Por qué `indent=2, ensure_ascii=False` y no otra cosa:** verificado byte-a-byte en esta sesión
de diseño que ese formato exacto reproduce el `catalogo.json` actual sin diferencias — así el
`git diff` de este paso muestra **solo** las líneas agregadas, ninguna de las 80 existentes se
reformatea.

- [ ] **Paso 2: Verificar el diff**

```bash
cd "/home/jdov/Documentos/titan score/IronQuestApp" && git diff --stat app/src/main/assets/catalogo.json
```

Expected: solo líneas `+` (inserciones), cero líneas `-` sobre las 80 entradas existentes. Si
aparece alguna `-` en una entrada vieja, algo en el formato no calzó — no seguir, corregir el
script de fusión primero.

- [ ] **Paso 3: Validación final de conteo**

```bash
python3 -c "
import json
d = json.load(open('app/src/main/assets/catalogo.json'))
print('total ejercicios en catalogo.json:', len(d))
"
```

Expected: `277` (80 + 197, o el número final tras el checkpoint de Task 1).

---

### Task 5: Commit Checkpoint A (datos)

**Files:** ninguno nuevo — commitea lo generado en Tasks 2-4.

- [ ] **Paso 1: Commit**

```bash
cd "/home/jdov/Documentos/titan score/IronQuestApp"
git add app/src/main/assets/catalogo.json app/src/main/assets/img/ app/src/main/assets/gifs/
git commit -m "Ampliar catálogo de ejercicios con ~197 curados por equipo del gimnasio"
```

No incluir en este commit los archivos de la otra slice en curso (`MigracionEjerciciosDialog`,
etc.) — no tocarlos, no stagearlos.

---

### Task 6 [PARA BIG PICKLE]: Bump de versión, build y verificación en dispositivo

**Files:**
- Modify: `IronQuestApp/app/src/main/java/com/ironquest/mvp/data/DataManager.java`

**Interfaces:**
- Consumes: `catalogo.json` ya extendido (Task 5, ya commiteado)

- [ ] **Paso 1: Bump de versión**

Buscar en `DataManager.java`:
```java
private static final int CATALOGO_VERSION_ACTUAL = 1;
```
Cambiar a:
```java
private static final int CATALOGO_VERSION_ACTUAL = 2;
```

Es el único cambio de código de toda esta ampliación. `migrarCatalogoDataset()` ya es aditiva
e idempotente (código de Slice 5, no se toca) — compara cada entrada de `catalogo.json` contra
`dataStore.buscarEjercicio(id)` y solo agrega las que no existen. Subir la versión simplemente
hace que la función corra una vez más: en instalaciones existentes los 80 ids viejos se saltan,
los ~197 nuevos se agregan.

- [ ] **Paso 2: Compilar**

```bash
export JAVA_HOME=/home/jdov/Documentos/android-studio-quail3-patch1-linux/android-studio/jbr
cd "/home/jdov/Documentos/titan score/IronQuestApp"
./gradlew assembleDebug
echo "exit: ${PIPESTATUS[0]}"
```

Expected: `BUILD SUCCESSFUL`, `exit: 0`.

- [ ] **Paso 3: Verificar tamaño de APK**

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
```

Expected: sube ~15-20 MB respecto al APK anterior a esta ampliación. Si sube mucho más, algo
salió mal en Task 3 (assets duplicados o sin comprimir) — detener y revisar antes de instalar.

- [ ] **Paso 4: Respaldar `datos.json` del dispositivo antes de instalar**

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json > /tmp/titan-score-catalogo/datos_respaldo_pre_v2.json
wc -c /tmp/titan-score-catalogo/datos_respaldo_pre_v2.json
```

Expected: archivo no vacío (regla no negociable de `CLAUDE.md` — nunca instalar sobre datos
reales sin respaldo previo).

- [ ] **Paso 5: Instalar y verificar en dispositivo**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Abrir la app → selector de ejercicios (picker):
- Conteo de ejercicios = 80 + N (N = tamaño final del delta, confirmar contra Task 5).
- Los chips de músculo siguen siendo los mismos 15 — ningún chip nuevo, ningún ejercicio sin
  chip.
- Elegir un ejercicio nuevo de cada estación (ej. uno de "Máquina Smith", uno de "Banco
  predicador") → su GIF carga bajo "ver técnica" o inline.

- [ ] **Paso 6: Verificar `datos.json` post-instalación**

```bash
adb shell run-as com.ironquest.mvp cat files/datos.json | python3 -c "
import json, sys
d = json.load(sys.stdin)
print('catalogoVersion:', d.get('catalogoVersion'))
print('total ejercicios:', len(d.get('ejercicios', [])))
"
```

Expected: `catalogoVersion: 2`. El total de ejercicios sube exactamente en N respecto al
respaldo de Task 6 Paso 4 (si sube distinto, hay colisión de id con algo del usuario —
investigar antes de seguir, no commitear).

- [ ] **Paso 7: Logout/login y verificación de rutinas previas**

Cerrar sesión → entrar con otra cuenta (o la misma) → confirmar que el catálogo sigue completo
(cubre el fix ya existente de `limpiarDatosDeUsuario()` de Slice 5). Confirmar que rutinas y
sesiones previas del respaldo siguen intactas.

- [ ] **Paso 8: Commit Checkpoint B**

```bash
cd "/home/jdov/Documentos/titan score/IronQuestApp"
git add app/src/main/java/com/ironquest/mvp/data/DataManager.java
git commit -m "Subir catalogoVersion a 2 para incorporar los ejercicios del gimnasio"
```

---

## Self-Review

**Cobertura del spec:** las 4 secciones del spec de diseño (división de trabajo, pipeline de
datos, cambio de código, checkpoints/verificación) tienen tarea correspondiente (Tasks 1-2,
Tasks 2-4, Task 6, Tasks 5+6). Los "fuera de alcance" del spec (V-squat, hip thrust, UI) no
tienen tarea — correcto, no debían tenerla.

**Placeholders:** ninguno — los 4 diccionarios de mapeo están completos (no parciales), los
scripts de Tasks 1, 3 y 4 ya se ejecutaron/verificaron en esta sesión de diseño con números
reales. El único paso no mecanizable (traducir `nombre`) está resuelto con una tabla de 15
ejemplos reales que fijan el criterio, no con un "traducir según corresponda" vago.

**Consistencia de tipos/nombres:** `delta_catalogo.json` (Task 2) es el nombre usado
consistentemente como entrada de Tasks 3 y 4. `EQUIPO_MAP`, `GRUPO_MUSCULAR_MAP`,
`MUSCULO_OBJETIVO_MAP`, `SECUNDARIOS_MAP` se definen una vez en Task 2 y se referencian igual
en el Paso 2 de esa misma tarea.
