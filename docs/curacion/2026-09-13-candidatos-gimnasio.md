# Candidatos de ampliación del catálogo — filtrado por equipo del gimnasio

Fuente: `hasaneyldrm/exercises-dataset` (mismo dataset ya integrado en `catalogo.json`). 
Se excluyen los 80 ejercicios ya usados. Pool disponible total: 1244 de 1324.

## Resumen

| Estación | Candidatos |
|---|---|
| Bancos de press banca (plano/inclinado/declinado) — variantes con mancuernas | 18 |
| Estación de poleas / polea dual — tren superior (todas las variaciones) | 137 |
| Banco predicador (barra, mancuerna, polea) | 26 |
| Barras de fondos (dips) | 28 |
| Polea alta — espalda (pulldowns / pull-over) | 15 |
| Polea baja — espalda (remos en polea) | 22 |
| Máquina Smith (tren superior e inferior) | 48 |
| Remo T | 2 |
| Pec deck (pecho / hombro / espalda) | 3 |
| Máquina de aductores / abductores | 10 |
| Prensa de piernas | 9 |
| Hack squat | 4 |
| Rack de sentadilla (barra libre) | 27 |
| Máquina de cuádriceps (leg extension) | 1 |
| Femoral acostado (lying leg curl) | 2 |
| Hip thrust | 1 |
| Pantorrillas sentado | 3 |
| V-squat machine | 0 |
| Pesos libres — mancuerna / barra / barra Z / barra olímpica (resto, no cubierto arriba) | 374 |
| **Total único (sin duplicar entre estaciones)** | **684** |

## Notas de cobertura del dataset

- **V-squat machine**: no existe ningún ejercicio con ese nombre en el dataset (0 candidatos). No se puede cubrir con esta fuente.
- **Hip thrust**: el dataset solo trae una variante con banda de resistencia (`resistance band hip thrusts on knees`); no hay hip thrust con barra ni de máquina.
- **Remo T** y **Pec deck**: el dataset tiene muy pocas entradas bajo esos nombres (2 y 3 respectivamente) — cobertura limitada de por sí en la fuente, no es un filtro demasiado estricto.
- Las estaciones **02 (polea dual)**, **05 (polea alta)** y **06 (polea baja)** se solapan a propósito: 05 y 06 son subconjuntos de 02 (los pulldowns/remos específicos de espalda), listados aparte porque el usuario nombró esas máquinas por separado.
- El bucket **19 (pesos libres)** es todo lo que queda en `dumbbell`/`barbell`/`ez barbell`/`olympic barbell`/`trap bar` que no cayó ya en una estación específica — incluye bastante variación redundante del dataset (ej. versiones "on exercise ball", "one arm", "v. 2") que conviene revisar antes de integrar.

## Bancos de press banca (plano/inclinado/declinado) — variantes con mancuernas (18)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0301 | dumbbell decline bench press | dumbbell | chest / pectorals |
| gv_0303 | dumbbell decline hammer press | dumbbell | chest / pectorals |
| gv_3545 | dumbbell incline alternate press | dumbbell | chest / pectorals |
| gv_0314 | dumbbell incline bench press | dumbbell | chest / pectorals |
| gv_0321 | dumbbell incline hammer press | dumbbell | chest / pectorals |
| gv_1281 | dumbbell incline one arm press | dumbbell | chest / pectorals |
| gv_1282 | dumbbell incline one arm press on exercise ball | dumbbell | chest / pectorals |
| gv_0324 | dumbbell incline palm-in press | dumbbell | chest / pectorals |
| gv_1283 | dumbbell incline press on exercise ball | dumbbell | chest / pectorals |
| gv_0340 | dumbbell lying hammer press | dumbbell | chest / pectorals |
| gv_0343 | dumbbell lying one arm press | dumbbell | chest / pectorals |
| gv_0342 | dumbbell lying one arm press v. 2 | dumbbell | chest / pectorals |
| gv_1287 | dumbbell one arm decline chest press | dumbbell | chest / pectorals |
| gv_1289 | dumbbell one arm incline chest press | dumbbell | chest / pectorals |
| gv_1290 | dumbbell one arm press on exercise ball | dumbbell | chest / pectorals |
| gv_1622 | dumbbell one arm reverse grip press | dumbbell | chest / pectorals |
| gv_1293 | dumbbell press on exercise ball | dumbbell | chest / pectorals |
| gv_1624 | dumbbell reverse bench press | dumbbell | chest / pectorals |

## Estación de poleas / polea dual — tren superior (todas las variaciones) (137)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0007 | alternate lateral pulldown | cable | back / lats |
| gv_0149 | cable alternate triceps extension | cable | upper arms / triceps |
| gv_0150 | cable bar lateral pulldown | cable | back / lats |
| gv_0151 | cable bench press | cable | chest / pectorals |
| gv_1630 | cable close grip curl | cable | upper arms / biceps |
| gv_1631 | cable concentration curl | cable | upper arms / biceps |
| gv_0152 | cable concentration extension (on knee) | cable | upper arms / triceps |
| gv_0153 | cable cross-over lateral pulldown | cable | back / lats |
| gv_0154 | cable cross-over revers fly | cable | shoulders / delts |
| gv_0155 | cable cross-over variation | cable | chest / pectorals |
| gv_0868 | cable curl | cable | upper arms / biceps |
| gv_0158 | cable decline fly | cable | chest / pectorals |
| gv_1260 | cable decline one arm press | cable | chest / pectorals |
| gv_1261 | cable decline press | cable | chest / pectorals |
| gv_0159 | cable decline seated wide-grip row | cable | back / upper back |
| gv_1632 | cable drag curl | cable | upper arms / biceps |
| gv_0160 | cable floor seated wide-grip row | cable | back / upper back |
| gv_0161 | cable forward raise | cable | shoulders / delts |
| gv_0162 | cable front raise | cable | shoulders / delts |
| gv_0164 | cable front shoulder raise | cable | shoulders / delts |
| gv_1722 | cable high pulley overhead tricep extension | cable | upper arms / triceps |
| gv_0167 | cable high row (kneeling) | cable | back / upper back |
| gv_0169 | cable incline bench press | cable | chest / pectorals |
| gv_1318 | cable incline bench row | cable | back / upper back |
| gv_0171 | cable incline fly | cable | chest / pectorals |
| gv_0170 | cable incline fly (on stability ball) | cable | chest / pectorals |
| gv_0172 | cable incline pushdown | cable | back / lats |
| gv_0173 | cable incline triceps extension | cable | upper arms / triceps |
| gv_0174 | cable judo flip | cable | waist / abs |
| gv_3697 | cable kneeling rear delt row (with rope) (male) | cable | shoulders / delts |
| gv_0176 | cable kneeling triceps extension | cable | upper arms / triceps |
| gv_0177 | cable lateral pulldown (with rope attachment) | cable | back / lats |
| gv_2616 | cable lateral pulldown with v-bar | cable | back / lats |
| gv_0179 | cable low fly | cable | chest / pectorals |
| gv_1634 | cable lying bicep curl | cable | upper arms / biceps |
| gv_0182 | cable lying close-grip curl | cable | upper arms / biceps |
| gv_0184 | cable lying extension pullover (with rope attachment) | cable | back / lats |
| gv_0185 | cable lying fly | cable | chest / pectorals |
| gv_0186 | cable lying triceps extension v. 2 | cable | upper arms / triceps |
| gv_0188 | cable middle fly | cable | chest / pectorals |
| gv_0189 | cable one arm bent over row | cable | back / upper back |
| gv_0190 | cable one arm curl | cable | upper arms / biceps |
| gv_1262 | cable one arm decline chest fly | cable | chest / pectorals |
| gv_1263 | cable one arm fly on exercise ball | cable | chest / pectorals |
| gv_1264 | cable one arm incline fly on exercise ball | cable | chest / pectorals |
| gv_1265 | cable one arm incline press | cable | chest / pectorals |
| gv_1266 | cable one arm incline press on exercise ball | cable | chest / pectorals |
| gv_0191 | cable one arm lateral bent-over | cable | chest / pectorals |
| gv_0192 | cable one arm lateral raise | cable | shoulders / delts |
| gv_1633 | cable one arm preacher curl | cable | upper arms / biceps |
| gv_1267 | cable one arm press on exercise ball | cable | chest / pectorals |
| gv_3563 | cable one arm pulldown | cable | back / lats |
| gv_1635 | cable one arm reverse preacher curl | cable | upper arms / biceps |
| gv_0193 | cable one arm straight back high row (kneeling) | cable | back / upper back |
| gv_1723 | cable one arm tricep pushdown | cable | upper arms / triceps |
| gv_1636 | cable overhead curl | cable | upper arms / biceps |
| gv_1637 | cable overhead curl on exercise ball | cable | upper arms / biceps |
| gv_0194 | cable overhead triceps extension (rope attachment) | cable | upper arms / triceps |
| gv_1319 | cable palm rotational row | cable | back / upper back |
| gv_0195 | cable preacher curl | cable | upper arms / biceps |
| gv_1268 | cable press on exercise ball | cable | chest / pectorals |
| gv_0198 | cable pulldown | cable | back / lats |
| gv_0197 | cable pulldown (pro lat bar) | cable | back / lats |
| gv_1638 | cable pulldown bicep curl | cable | upper arms / biceps |
| gv_0201 | cable pushdown | cable | upper arms / triceps |
| gv_0199 | cable pushdown (straight arm) v. 2 | cable | back / lats |
| gv_0200 | cable pushdown (with rope attachment) | cable | upper arms / triceps |
| gv_0202 | cable rear delt row (stirrups) | cable | shoulders / delts |
| gv_0203 | cable rear delt row (with rope) | cable | shoulders / delts |
| gv_0204 | cable rear drive | cable | upper arms / triceps |
| gv_0205 | cable rear pulldown | cable | back / lats |
| gv_0873 | cable reverse crunch | cable | waist / abs |
| gv_0206 | cable reverse curl | cable | upper arms / biceps |
| gv_1413 | cable reverse one arm curl | cable | upper arms / biceps |
| gv_0209 | cable reverse preacher curl | cable | upper arms / biceps |
| gv_0207 | cable reverse-grip pushdown | cable | upper arms / triceps |
| gv_0208 | cable reverse-grip straight back seated high row | cable | back / upper back |
| gv_1320 | cable rope crossover seated row | cable | back / upper back |
| gv_1321 | cable rope elevated seated row | cable | back / upper back |
| gv_1322 | cable rope extension incline bench row | cable | back / upper back |
| gv_1639 | cable rope hammer preacher curl | cable | upper arms / biceps |
| gv_1724 | cable rope high pulley overhead tricep extension | cable | upper arms / triceps |
| gv_1725 | cable rope incline tricep extension | cable | upper arms / triceps |
| gv_1726 | cable rope lying on floor tricep extension | cable | upper arms / triceps |
| gv_1640 | cable rope one arm hammer preacher curl | cable | upper arms / biceps |
| gv_1323 | cable rope seated row | cable | back / upper back |
| gv_0212 | cable seated crunch | cable | waist / abs |
| gv_1641 | cable seated curl | cable | upper arms / biceps |
| gv_0213 | cable seated high row (v-bar) | cable | back / lats |
| gv_0214 | cable seated one arm alternate row | cable | back / upper back |
| gv_1642 | cable seated one arm concentration curl | cable | upper arms / biceps |
| gv_1643 | cable seated overhead curl | cable | upper arms / biceps |
| gv_0215 | cable seated rear lateral raise | cable | shoulders / delts |
| gv_0861 | cable seated row | cable | back / upper back |
| gv_0216 | cable seated shoulder internal rotation | cable | shoulders / delts |
| gv_2399 | cable seated twist | cable | waist / abs |
| gv_0218 | cable seated wide-grip row | cable | back / upper back |
| gv_0219 | cable shoulder press | cable | shoulders / delts |
| gv_0220 | cable shrug | cable | back / traps |
| gv_0222 | cable side bend | cable | waist / abs |
| gv_0221 | cable side bend crunch (bosu ball) | cable | waist / abs |
| gv_0223 | cable side crunch | cable | waist / abs |
| gv_1717 | cable squat row (with rope attachment) | cable | back / lats |
| gv_1644 | cable squatting curl | cable | upper arms / biceps |
| gv_0226 | cable standing crunch | cable | waist / abs |
| gv_0874 | cable standing crunch (with rope attachment) | cable | waist / abs |
| gv_0227 | cable standing fly | cable | chest / pectorals |
| gv_0229 | cable standing inner curl | cable | upper arms / biceps |
| gv_0230 | cable standing lift | cable | waist / abs |
| gv_0231 | cable standing one arm triceps extension | cable | upper arms / triceps |
| gv_0232 | cable standing pulldown (with rope) | cable | upper arms / biceps |
| gv_0233 | cable standing rear delt row (with rope) | cable | shoulders / delts |
| gv_1727 | cable standing reverse grip one arm overhead tricep extension | cable | upper arms / triceps |
| gv_0234 | cable standing row (v-bar) | cable | back / upper back |
| gv_0235 | cable standing shoulder external rotation | cable | shoulders / delts |
| gv_0236 | cable standing twist row (v-bar) | cable | back / upper back |
| gv_1269 | cable standing up straight crossovers | cable | chest / pectorals |
| gv_0238 | cable straight arm pulldown | cable | back / lats |
| gv_0237 | cable straight arm pulldown (with rope) | cable | back / lats |
| gv_0239 | cable straight back seated row | cable | back / upper back |
| gv_0240 | cable supine reverse fly | cable | shoulders / delts |
| gv_2464 | cable thibaudeau kayak row | cable | back / lats |
| gv_0241 | cable triceps pushdown (v-bar) | cable | upper arms / triceps |
| gv_2405 | cable triceps pushdown (v-bar) (with arm blaster) | cable | upper arms / triceps |
| gv_0242 | cable tuck reverse crunch | cable | waist / abs |
| gv_0243 | cable twist | cable | waist / abs |
| gv_0862 | cable twist (up-down) | cable | waist / abs |
| gv_0244 | cable twisting pull | cable | back / lats |
| gv_1645 | cable two arm curl on incline bench | cable | upper arms / biceps |
| gv_1728 | cable two arm tricep kickback | cable | upper arms / triceps |
| gv_0245 | cable underhand pulldown | cable | back / lats |
| gv_1270 | cable upper chest crossovers | cable | chest / pectorals |
| gv_1324 | cable upper row | cable | back / upper back |
| gv_0246 | cable upright row | cable | shoulders / delts |
| gv_1325 | cable wide grip rear pulldown behind neck | cable | back / lats |
| gv_0247 | cable wrist curl | cable | lower arms / forearms |
| gv_0818 | twin handle parallel grip lat pulldown | cable | back / lats |

## Banco predicador (barra, mancuerna, polea) (26)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0070 | barbell preacher curl | barbell | upper arms / biceps |
| gv_0081 | barbell reverse preacher curl | barbell | upper arms / biceps |
| gv_1633 | cable one arm preacher curl | cable | upper arms / biceps |
| gv_1635 | cable one arm reverse preacher curl | cable | upper arms / biceps |
| gv_0195 | cable preacher curl | cable | upper arms / biceps |
| gv_0209 | cable reverse preacher curl | cable | upper arms / biceps |
| gv_1639 | cable rope hammer preacher curl | cable | upper arms / biceps |
| gv_1640 | cable rope one arm hammer preacher curl | cable | upper arms / biceps |
| gv_1646 | dumbbell alternate hammer preacher curl | dumbbell | upper arms / biceps |
| gv_1647 | dumbbell alternate preacher curl | dumbbell | upper arms / biceps |
| gv_1663 | dumbbell one arm hammer preacher curl | dumbbell | upper arms / biceps |
| gv_1414 | dumbbell one arm reverse preacher curl | dumbbell | upper arms / biceps |
| gv_1672 | dumbbell one arm zottman preacher curl | dumbbell | upper arms / biceps |
| gv_0372 | dumbbell preacher curl | dumbbell | upper arms / biceps |
| gv_1673 | dumbbell preacher curl over exercise ball | dumbbell | upper arms / biceps |
| gv_0384 | dumbbell reverse preacher curl | dumbbell | upper arms / biceps |
| gv_0402 | dumbbell seated preacher curl | dumbbell | upper arms / biceps |
| gv_0428 | dumbbell standing preacher curl | dumbbell | upper arms / biceps |
| gv_2293 | dumbbell standing zottman preacher curl | dumbbell | upper arms / biceps |
| gv_2294 | dumbbell zottman preacher curl | dumbbell | upper arms / biceps |
| gv_1627 | ez barbell close grip preacher curl | ez barbell | upper arms / biceps |
| gv_0452 | ez barbell reverse grip preacher curl | ez barbell | upper arms / biceps |
| gv_1615 | lever hammer grip preacher curl | leverage machine | upper arms / biceps |
| gv_0592 | lever preacher curl | leverage machine | upper arms / biceps |
| gv_1614 | lever preacher curl v. 2 | leverage machine | upper arms / biceps |
| gv_1616 | lever reverse grip preacher curl | leverage machine | upper arms / biceps |

## Barras de fondos (dips) (28)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_2364 | assisted wide-grip chest dip (kneeling) | leverage machine | chest / pectorals |
| gv_0129 | bench dip (knees bent) | body weight | upper arms / triceps |
| gv_1399 | bench dip on floor | body weight | upper arms / triceps |
| gv_0251 | chest dip | body weight | chest / pectorals |
| gv_1430 | chest dip (on dip-pull-up cage) | body weight | chest / pectorals |
| gv_2462 | chest dip on straight bar | body weight | chest / pectorals |
| gv_3287 | elbow dips | body weight | upper arms / triceps |
| gv_1744 | exercise ball dip | stability ball | upper arms / triceps |
| gv_3289 | impossible dips | body weight | upper arms / triceps |
| gv_3288 | korean dips | body weight | chest / pectorals |
| gv_0591 | lever overhand triceps dip | leverage machine | upper arms / triceps |
| gv_1451 | lever seated dip | leverage machine | upper arms / triceps |
| gv_0639 | one arm dip | body weight | upper arms / triceps |
| gv_0672 | reverse dip | body weight | upper arms / triceps |
| gv_0677 | ring dips | body weight | upper arms / triceps |
| gv_3012 | scapula dips | body weight | back / traps |
| gv_1753 | three bench dip | body weight | upper arms / triceps |
| gv_0814 | triceps dip | body weight | upper arms / triceps |
| gv_0812 | triceps dip (bench leg) | body weight | upper arms / triceps |
| gv_0813 | triceps dip (between benches) | body weight | upper arms / triceps |
| gv_0815 | triceps dips floor | body weight | upper arms / triceps |
| gv_0830 | weighted bench dip | weighted | upper arms / triceps |
| gv_2987 | weighted close grip chin-up on dip cage | weighted | back / lats |
| gv_3313 | weighted straight bar dip | weighted | chest / pectorals |
| gv_1754 | weighted three bench dips | weighted | upper arms / triceps |
| gv_1755 | weighted tricep dips | weighted | upper arms / triceps |
| gv_1767 | weighted triceps dip on high parallel bars | weighted | upper arms / triceps |
| gv_2363 | wide-grip chest dip on high parallel bars | body weight | chest / pectorals |

## Polea alta — espalda (pulldowns / pull-over) (15)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0007 | alternate lateral pulldown | cable | back / lats |
| gv_0150 | cable bar lateral pulldown | cable | back / lats |
| gv_0153 | cable cross-over lateral pulldown | cable | back / lats |
| gv_0177 | cable lateral pulldown (with rope attachment) | cable | back / lats |
| gv_2616 | cable lateral pulldown with v-bar | cable | back / lats |
| gv_0184 | cable lying extension pullover (with rope attachment) | cable | back / lats |
| gv_3563 | cable one arm pulldown | cable | back / lats |
| gv_0198 | cable pulldown | cable | back / lats |
| gv_0197 | cable pulldown (pro lat bar) | cable | back / lats |
| gv_0205 | cable rear pulldown | cable | back / lats |
| gv_0238 | cable straight arm pulldown | cable | back / lats |
| gv_0237 | cable straight arm pulldown (with rope) | cable | back / lats |
| gv_0245 | cable underhand pulldown | cable | back / lats |
| gv_1325 | cable wide grip rear pulldown behind neck | cable | back / lats |
| gv_0818 | twin handle parallel grip lat pulldown | cable | back / lats |

## Polea baja — espalda (remos en polea) (22)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0159 | cable decline seated wide-grip row | cable | back / upper back |
| gv_0160 | cable floor seated wide-grip row | cable | back / upper back |
| gv_0167 | cable high row (kneeling) | cable | back / upper back |
| gv_1318 | cable incline bench row | cable | back / upper back |
| gv_0189 | cable one arm bent over row | cable | back / upper back |
| gv_0193 | cable one arm straight back high row (kneeling) | cable | back / upper back |
| gv_1319 | cable palm rotational row | cable | back / upper back |
| gv_0208 | cable reverse-grip straight back seated high row | cable | back / upper back |
| gv_1320 | cable rope crossover seated row | cable | back / upper back |
| gv_1321 | cable rope elevated seated row | cable | back / upper back |
| gv_1322 | cable rope extension incline bench row | cable | back / upper back |
| gv_1323 | cable rope seated row | cable | back / upper back |
| gv_0213 | cable seated high row (v-bar) | cable | back / lats |
| gv_0214 | cable seated one arm alternate row | cable | back / upper back |
| gv_0861 | cable seated row | cable | back / upper back |
| gv_0218 | cable seated wide-grip row | cable | back / upper back |
| gv_1717 | cable squat row (with rope attachment) | cable | back / lats |
| gv_0234 | cable standing row (v-bar) | cable | back / upper back |
| gv_0236 | cable standing twist row (v-bar) | cable | back / upper back |
| gv_0239 | cable straight back seated row | cable | back / upper back |
| gv_2464 | cable thibaudeau kayak row | cable | back / lats |
| gv_1324 | cable upper row | cable | back / upper back |

## Máquina Smith (tren superior e inferior) (48)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0746 | smith back shrug | smith machine | back / traps |
| gv_0747 | smith behind neck press | smith machine | shoulders / delts |
| gv_0748 | smith bench press | smith machine | chest / pectorals |
| gv_0749 | smith bent knee good morning | smith machine | upper legs / glutes |
| gv_1359 | smith bent over row | smith machine | back / upper back |
| gv_0750 | smith chair squat | smith machine | upper legs / quads |
| gv_0751 | smith close-grip bench press | smith machine | upper arms / triceps |
| gv_0752 | smith deadlift | smith machine | upper legs / glutes |
| gv_0753 | smith decline bench press | smith machine | chest / pectorals |
| gv_0754 | smith decline reverse-grip press | smith machine | chest / pectorals |
| gv_1433 | smith front squat (clean grip) | smith machine | upper legs / glutes |
| gv_3281 | smith full squat | smith machine | upper legs / glutes |
| gv_0755 | smith hack squat | smith machine | upper legs / glutes |
| gv_0756 | smith hip raise | smith machine | waist / abs |
| gv_0757 | smith incline bench press | smith machine | chest / pectorals |
| gv_0758 | smith incline reverse-grip press | smith machine | chest / pectorals |
| gv_0759 | smith incline shoulder raises | smith machine | chest / serratus anterior |
| gv_0760 | smith leg press | smith machine | upper legs / glutes |
| gv_1434 | smith low bar squat | smith machine | upper legs / glutes |
| gv_1683 | smith machine bicep curl | smith machine | upper arms / biceps |
| gv_1625 | smith machine decline close grip bench press | smith machine | upper arms / triceps |
| gv_1752 | smith machine incline tricep extension | smith machine | upper arms / triceps |
| gv_1626 | smith machine reverse decline close grip bench press | smith machine | chest / pectorals |
| gv_0761 | smith narrow row | smith machine | back / upper back |
| gv_1360 | smith one arm row | smith machine | back / upper back |
| gv_1393 | smith one leg floor calf raise | smith machine | lower legs / calves |
| gv_0762 | smith rear delt row | smith machine | shoulders / delts |
| gv_0763 | smith reverse calf raises | smith machine | lower legs / calves |
| gv_1394 | smith reverse calf raises | smith machine | lower legs / calves |
| gv_1361 | smith reverse grip bent over row | smith machine | back / upper back |
| gv_0764 | smith reverse-grip press | smith machine | chest / pectorals |
| gv_1395 | smith seated one leg calf raise | smith machine | lower legs / calves |
| gv_0765 | smith seated shoulder press | smith machine | shoulders / delts |
| gv_1426 | smith seated wrist curl | smith machine | lower arms / forearms |
| gv_0766 | smith shoulder press | smith machine | shoulders / delts |
| gv_0767 | smith shrug | smith machine | back / traps |
| gv_0768 | smith single leg split squat | smith machine | upper legs / quads |
| gv_0769 | smith sprint lunge | smith machine | upper legs / glutes |
| gv_0770 | smith squat | smith machine | upper legs / glutes |
| gv_0771 | smith standing back wrist curl | smith machine | lower arms / forearms |
| gv_0772 | smith standing behind head military press | smith machine | shoulders / delts |
| gv_0773 | smith standing leg calf raise | smith machine | lower legs / calves |
| gv_0774 | smith standing military press | smith machine | shoulders / delts |
| gv_3142 | smith sumo squat | smith machine | upper legs / glutes |
| gv_1396 | smith toe raise | smith machine | lower legs / calves |
| gv_0775 | smith upright row | smith machine | shoulders / delts |
| gv_1308 | smith wide grip bench press | smith machine | chest / pectorals |
| gv_1309 | smith wide grip decline bench press | smith machine | chest / pectorals |

## Remo T (2)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0606 | lever t bar row | leverage machine | back / upper back |
| gv_1351 | lever t-bar reverse grip row | leverage machine | back / upper back |

## Pec deck (pecho / hombro / espalda) (3)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0596 | lever seated fly | leverage machine | chest / pectorals |
| gv_0602 | lever seated reverse fly | leverage machine | shoulders / delts |
| gv_0601 | lever seated reverse fly (parallel grip) | leverage machine | shoulders / delts |

## Máquina de aductores / abductores (10)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_1712 | assisted side lying adductor stretch | assisted | upper legs / adductors |
| gv_1494 | butterfly yoga pose | body weight | upper legs / adductors |
| gv_0168 | cable hip adduction | cable | upper legs / adductors |
| gv_0598 | lever seated hip adduction | leverage machine | upper legs / adductors |
| gv_3006 | resistance band seated hip abduction | resistance band | upper legs / abductors |
| gv_1774 | side bridge hip abduction | body weight | upper legs / abductors |
| gv_0710 | side hip abduction | body weight | upper legs / abductors |
| gv_3667 | side lying hip adduction (male) | body weight | upper legs / adductors |
| gv_1775 | side plank hip adduction | body weight | upper legs / adductors |
| gv_1427 | straight leg outer hip abductor | body weight | upper legs / abductors |

## Prensa de piernas (9)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_2611 | lever horizontal one leg press | leverage machine | upper legs / glutes |
| gv_1385 | lever seated squat calf raise on leg press machine | leverage machine | lower legs / calves |
| gv_1425 | sled 45 degrees one leg press | sled machine | upper legs / glutes |
| gv_1463 | sled 45° leg press (side pov) | sled machine | upper legs / glutes |
| gv_0739 | sled 45в° leg press | sled machine | upper legs / glutes |
| gv_1464 | sled 45в° leg press (back pov) | sled machine | upper legs / glutes |
| gv_1391 | sled calf press on leg press | sled machine | lower legs / calves |
| gv_1392 | sled one leg calf press on leg press | sled machine | lower legs / calves |
| gv_0760 | smith leg press | smith machine | upper legs / glutes |

## Hack squat (4)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0046 | barbell hack squat | barbell | upper legs / glutes |
| gv_0741 | sled closer hack squat | sled machine | upper legs / glutes |
| gv_0743 | sled hack squat | sled machine | upper legs / glutes |
| gv_0755 | smith hack squat | smith machine | upper legs / glutes |

## Rack de sentadilla (barra libre) (27)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0026 | barbell bench squat | barbell | upper legs / quads |
| gv_0029 | barbell clean-grip front squat | barbell | upper legs / glutes |
| gv_0039 | barbell front chest squat | barbell | upper legs / glutes |
| gv_0042 | barbell front squat | barbell | upper legs / glutes |
| gv_1461 | barbell full squat (back pov) | barbell | upper legs / glutes |
| gv_1462 | barbell full squat (side pov) | barbell | upper legs / glutes |
| gv_1545 | barbell full zercher squat | barbell | upper legs / glutes |
| gv_0046 | barbell hack squat | barbell | upper legs / glutes |
| gv_1436 | barbell high bar squat | barbell | upper legs / glutes |
| gv_0051 | barbell jefferson squat | barbell | upper legs / glutes |
| gv_0053 | barbell jump squat | barbell | upper legs / glutes |
| gv_1435 | barbell low bar squat | barbell | upper legs / glutes |
| gv_0063 | barbell narrow stance squat | barbell | upper legs / glutes |
| gv_0068 | barbell one leg squat | barbell | upper legs / quads |
| gv_0069 | barbell overhead squat | barbell | upper legs / quads |
| gv_0098 | barbell side split squat | barbell | upper legs / quads |
| gv_0097 | barbell side split squat v. 2 | barbell | upper legs / quads |
| gv_0099 | barbell single leg split squat | barbell | upper legs / quads |
| gv_0101 | barbell speed squat | barbell | upper legs / glutes |
| gv_2810 | barbell split squat v. 2 | barbell | upper legs / quads |
| gv_0102 | barbell squat (on knees) | barbell | upper legs / quads |
| gv_2798 | barbell squat jump step rear lunge | barbell | upper legs / quads |
| gv_0124 | barbell wide squat | barbell | upper legs / quads |
| gv_0127 | barbell zercher squat | barbell | upper legs / glutes |
| gv_3194 | frankenstein squat | barbell | upper legs / glutes |
| gv_1420 | kneeling jump squat | barbell | upper legs / glutes |
| gv_0786 | squat jerk | barbell | upper legs / quads |

## Máquina de cuádriceps (leg extension) (1)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_3007 | resistance band leg extension | resistance band | upper legs / quads |

## Femoral acostado (lying leg curl) (2)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0586 | lever lying leg curl | leverage machine | upper legs / hamstrings |
| gv_3195 | lever lying two-one leg curl | leverage machine | upper legs / hamstrings |

## Hip thrust (1)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_3236 | resistance band hip thrusts on knees (female) | resistance band | upper legs / glutes |

## Pantorrillas sentado (3)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_1371 | barbell seated calf raise | barbell | lower legs / calves |
| gv_1379 | dumbbell seated calf raise | dumbbell | lower legs / calves |
| gv_0594 | lever seated calf raise | leverage machine | lower legs / calves |

## V-squat machine (0)

_Sin candidatos en el dataset._

## Pesos libres — mancuerna / barra / barra Z / barra olímpica (resto, no cubierto arriba) (374)

| id | nombre (en) | equipo | body_part / target |
|---|---|---|---|
| gv_0023 | barbell alternate biceps curl | barbell | upper arms / biceps |
| gv_1316 | barbell bent arm pullover | barbell | back / lats |
| gv_2407 | barbell biceps curl (with arm blaster) | barbell | upper arms / biceps |
| gv_0028 | barbell clean and press | barbell | upper legs / quads |
| gv_0032 | barbell deadlift | barbell | upper legs / glutes |
| gv_0034 | barbell decline bent arm pullover | barbell | back / lats |
| gv_0035 | barbell decline close grip to skull press | barbell | upper arms / triceps |
| gv_1255 | barbell decline pullover | barbell | chest / pectorals |
| gv_0036 | barbell decline wide-grip press | barbell | chest / pectorals |
| gv_0037 | barbell decline wide-grip pullover | barbell | back / lats |
| gv_0038 | barbell drag curl | barbell | upper arms / biceps |
| gv_0040 | barbell front raise and pullover | barbell | chest / pectorals |
| gv_1409 | barbell glute bridge | barbell | upper legs / glutes |
| gv_3562 | barbell glute bridge two legs on bench (male) | barbell | upper legs / glutes |
| gv_0045 | barbell guillotine bench press | barbell | chest / pectorals |
| gv_1719 | barbell incline close grip bench press | barbell | upper arms / triceps |
| gv_0048 | barbell incline reverse-grip press | barbell | upper arms / triceps |
| gv_0049 | barbell incline row | barbell | back / upper back |
| gv_0050 | barbell incline shoulder raise | barbell | chest / serratus anterior |
| gv_0052 | barbell jm bench press | barbell | upper arms / triceps |
| gv_0054 | barbell lunge | barbell | upper legs / glutes |
| gv_1720 | barbell lying back of the head tricep extension | barbell | upper arms / triceps |
| gv_0055 | barbell lying close-grip press | barbell | upper arms / triceps |
| gv_0057 | barbell lying extension | barbell | upper arms / triceps |
| gv_0058 | barbell lying lifting (on hip) | barbell | upper legs / glutes |
| gv_0061 | barbell lying triceps extension | barbell | upper arms / triceps |
| gv_0064 | barbell one arm bent over row | barbell | back / upper back |
| gv_0065 | barbell one arm floor press | barbell | upper arms / triceps |
| gv_0066 | barbell one arm side deadlift | barbell | upper legs / glutes |
| gv_0067 | barbell one arm snatch | barbell | shoulders / delts |
| gv_3017 | barbell pendlay row | barbell | back / upper back |
| gv_1751 | barbell pin presses | barbell | upper arms / triceps |
| gv_0071 | barbell press sit-up | barbell | waist / abs |
| gv_0072 | barbell prone incline curl | barbell | upper arms / biceps |
| gv_0073 | barbell pullover | barbell | back / lats |
| gv_0022 | barbell pullover to press | barbell | back / lats |
| gv_0074 | barbell rack pull | barbell | upper legs / glutes |
| gv_0076 | barbell rear delt row | barbell | shoulders / delts |
| gv_0078 | barbell rear lunge | barbell | upper legs / glutes |
| gv_0077 | barbell rear lunge v. 2 | barbell | upper legs / glutes |
| gv_2187 | barbell reverse close-grip bench press | barbell | upper arms / triceps |
| gv_0080 | barbell reverse curl | barbell | upper arms / biceps |
| gv_0118 | barbell reverse grip bent over row | barbell | back / upper back |
| gv_1256 | barbell reverse grip decline bench press | barbell | chest / pectorals |
| gv_1257 | barbell reverse grip incline bench press | barbell | chest / pectorals |
| gv_1317 | barbell reverse grip incline bench row | barbell | back / upper back |
| gv_1721 | barbell reverse grip skullcrusher | barbell | upper arms / triceps |
| gv_0084 | barbell rollerout | barbell | waist / abs |
| gv_0083 | barbell rollerout from bench | barbell | waist / abs |
| gv_0087 | barbell seated bradford rocky press | barbell | shoulders / delts |
| gv_1718 | barbell seated close grip behind neck triceps extension | barbell | upper arms / triceps |
| gv_0090 | barbell seated good morning | barbell | upper legs / glutes |
| gv_0092 | barbell seated overhead triceps extension | barbell | upper arms / triceps |
| gv_0094 | barbell seated twist | barbell | waist / abs |
| gv_0096 | barbell side bent v. 2 | barbell | waist / abs |
| gv_1756 | barbell single leg deadlift | barbell | upper legs / glutes |
| gv_2800 | barbell sitted alternate leg raise (female) | barbell | waist / abs |
| gv_0100 | barbell skier | barbell | shoulders / delts |
| gv_0103 | barbell standing ab rollerout | barbell | waist / abs |
| gv_0105 | barbell standing bradford press | barbell | shoulders / delts |
| gv_0106 | barbell standing close grip curl | barbell | upper arms / biceps |
| gv_1456 | barbell standing close grip military press | barbell | shoulders / delts |
| gv_2414 | barbell standing concentration curl | barbell | upper arms / biceps |
| gv_0107 | barbell standing front raise over head | barbell | shoulders / delts |
| gv_0109 | barbell standing overhead triceps extension | barbell | upper arms / triceps |
| gv_0110 | barbell standing reverse grip curl | barbell | upper arms / biceps |
| gv_0112 | barbell standing twist | barbell | waist / abs |
| gv_1629 | barbell standing wide grip biceps curl | barbell | upper arms / biceps |
| gv_1457 | barbell standing wide military press | barbell | shoulders / delts |
| gv_0113 | barbell standing wide-grip curl | barbell | upper arms / biceps |
| gv_0115 | barbell stiff leg good morning | barbell | upper legs / glutes |
| gv_0116 | barbell straight leg deadlift | barbell | upper legs / hamstrings |
| gv_0117 | barbell sumo deadlift | barbell | upper legs / glutes |
| gv_3305 | barbell thruster | barbell | shoulders / delts |
| gv_0119 | barbell upright row v. 2 | barbell | shoulders / delts |
| gv_0121 | barbell upright row v. 3 | barbell | shoulders / delts |
| gv_0122 | barbell wide bench press | barbell | chest / pectorals |
| gv_1258 | barbell wide reverse grip bench press | barbell | chest / pectorals |
| gv_0123 | barbell wide-grip upright row | barbell | shoulders / delts |
| gv_0125 | barbell wrist curl v. 2 | barbell | lower arms / forearms |
| gv_0248 | cambered bar lying row | barbell | back / upper back |
| gv_1274 | deep push up | dumbbell | chest / pectorals |
| gv_0285 | dumbbell alternate biceps curl | dumbbell | upper arms / biceps |
| gv_2403 | dumbbell alternate biceps curl (with arm blaster) | dumbbell | upper arms / biceps |
| gv_1648 | dumbbell alternate seated hammer curl | dumbbell | upper arms / biceps |
| gv_0286 | dumbbell alternate side press | dumbbell | shoulders / delts |
| gv_1649 | dumbbell alternating bicep curl with leg raised on exercise ball | dumbbell | upper arms / biceps |
| gv_1650 | dumbbell alternating seated bicep curl on exercise ball | dumbbell | upper arms / biceps |
| gv_0287 | dumbbell arnold press v. 2 | dumbbell | shoulders / delts |
| gv_0288 | dumbbell around pullover | dumbbell | chest / pectorals |
| gv_0290 | dumbbell bench seated press | dumbbell | shoulders / delts |
| gv_0291 | dumbbell bench squat | dumbbell | upper legs / glutes |
| gv_0293 | dumbbell bent over row | dumbbell | back / upper back |
| gv_1651 | dumbbell bicep curl lunge with bowling motion | dumbbell | upper arms / biceps |
| gv_1652 | dumbbell bicep curl on exercise ball with leg raised | dumbbell | upper arms / biceps |
| gv_1653 | dumbbell bicep curl with stork stance | dumbbell | upper arms / biceps |
| gv_0294 | dumbbell biceps curl | dumbbell | upper arms / biceps |
| gv_2401 | dumbbell biceps curl (with arm blaster) | dumbbell | upper arms / biceps |
| gv_1654 | dumbbell biceps curl reverse | dumbbell | upper arms / biceps |
| gv_1655 | dumbbell biceps curl squat | dumbbell | upper arms / biceps |
| gv_1656 | dumbbell biceps curl v sit on bosu ball | dumbbell | upper arms / biceps |
| gv_1201 | dumbbell burpee | dumbbell | cardio / cardiovascular system |
| gv_0295 | dumbbell clean | dumbbell | upper legs / glutes |
| gv_1731 | dumbbell close grip press | dumbbell | upper arms / triceps |
| gv_0296 | dumbbell close-grip press | dumbbell | upper arms / triceps |
| gv_0297 | dumbbell concentration curl | dumbbell | upper arms / biceps |
| gv_3635 | dumbbell contralateral forward lunge | dumbbell | upper legs / glutes |
| gv_0298 | dumbbell cross body hammer curl | dumbbell | upper arms / biceps |
| gv_1657 | dumbbell cross body hammer curl v. 2 | dumbbell | upper arms / biceps |
| gv_0299 | dumbbell cuban press | dumbbell | shoulders / delts |
| gv_2136 | dumbbell cuban press v. 2 | dumbbell | shoulders / delts |
| gv_0300 | dumbbell deadlift | dumbbell | upper legs / glutes |
| gv_0302 | dumbbell decline fly | dumbbell | chest / pectorals |
| gv_1276 | dumbbell decline one arm fly | dumbbell | chest / pectorals |
| gv_1617 | dumbbell decline one arm hammer press | dumbbell | upper arms / triceps |
| gv_0305 | dumbbell decline shrug | dumbbell | back / traps |
| gv_0304 | dumbbell decline shrug v. 2 | dumbbell | back / traps |
| gv_0306 | dumbbell decline triceps extension | dumbbell | upper arms / triceps |
| gv_0307 | dumbbell decline twist fly | dumbbell | chest / pectorals |
| gv_1437 | dumbbell finger curls | dumbbell | lower arms / forearms |
| gv_1277 | dumbbell fly on exercise ball | dumbbell | chest / pectorals |
| gv_1732 | dumbbell forward lunge triceps extension | dumbbell | upper arms / triceps |
| gv_0310 | dumbbell front raise | dumbbell | shoulders / delts |
| gv_0309 | dumbbell front raise v. 2 | dumbbell | shoulders / delts |
| gv_0311 | dumbbell full can lateral raise | dumbbell | shoulders / delts |
| gv_1760 | dumbbell goblet squat | dumbbell | upper legs / quads |
| gv_0313 | dumbbell hammer curl | dumbbell | upper arms / biceps |
| gv_1659 | dumbbell hammer curl on exercise ball | dumbbell | upper arms / biceps |
| gv_0312 | dumbbell hammer curl v. 2 | dumbbell | upper arms / biceps |
| gv_2402 | dumbbell hammer curls (with arm blaster) | dumbbell | upper arms / biceps |
| gv_1664 | dumbbell high curl | dumbbell | upper arms / biceps |
| gv_0315 | dumbbell incline biceps curl | dumbbell | upper arms / biceps |
| gv_0316 | dumbbell incline breeding | dumbbell | chest / pectorals |
| gv_0318 | dumbbell incline curl | dumbbell | upper arms / biceps |
| gv_0317 | dumbbell incline curl v. 2 | dumbbell | upper arms / biceps |
| gv_0319 | dumbbell incline fly | dumbbell | chest / pectorals |
| gv_1278 | dumbbell incline fly on exercise ball | dumbbell | chest / pectorals |
| gv_0320 | dumbbell incline hammer curl | dumbbell | upper arms / biceps |
| gv_1618 | dumbbell incline hammer press on exercise ball | dumbbell | upper arms / triceps |
| gv_0322 | dumbbell incline inner biceps curl | dumbbell | upper arms / biceps |
| gv_1279 | dumbbell incline one arm fly | dumbbell | chest / pectorals |
| gv_1280 | dumbbell incline one arm fly on exercise ball | dumbbell | chest / pectorals |
| gv_1619 | dumbbell incline one arm hammer press | dumbbell | upper arms / triceps |
| gv_1620 | dumbbell incline one arm hammer press on exercise ball | dumbbell | upper arms / triceps |
| gv_0323 | dumbbell incline one arm lateral raise | dumbbell | shoulders / delts |
| gv_0325 | dumbbell incline raise | dumbbell | shoulders / delts |
| gv_0326 | dumbbell incline rear lateral raise | dumbbell | shoulders / delts |
| gv_0327 | dumbbell incline row | dumbbell | back / upper back |
| gv_0328 | dumbbell incline shoulder raise | dumbbell | chest / serratus anterior |
| gv_0329 | dumbbell incline shrug | dumbbell | back / traps |
| gv_3542 | dumbbell incline t-raise | dumbbell | shoulders / delts |
| gv_0330 | dumbbell incline triceps extension | dumbbell | upper arms / triceps |
| gv_0331 | dumbbell incline twisted flyes | dumbbell | chest / pectorals |
| gv_1733 | dumbbell incline two arm extension | dumbbell | upper arms / triceps |
| gv_3541 | dumbbell incline y-raise | dumbbell | back / upper back |
| gv_0332 | dumbbell iron cross | dumbbell | shoulders / delts |
| gv_0333 | dumbbell kickback | dumbbell | upper arms / triceps |
| gv_1734 | dumbbell kickbacks on exercise ball | dumbbell | upper arms / triceps |
| gv_1660 | dumbbell kneeling bicep curl exercise ball | dumbbell | upper arms / biceps |
| gv_0334 | dumbbell lateral raise | dumbbell | shoulders / delts |
| gv_0335 | dumbbell lateral to front raise | dumbbell | shoulders / delts |
| gv_0336 | dumbbell lunge | dumbbell | upper legs / glutes |
| gv_1658 | dumbbell lunge with bicep curl | dumbbell | upper arms / biceps |
| gv_1729 | dumbbell lying alternate extension | dumbbell | upper arms / triceps |
| gv_0338 | dumbbell lying elbow press | dumbbell | upper arms / triceps |
| gv_0337 | dumbbell lying extension (across face) | dumbbell | upper arms / triceps |
| gv_0863 | dumbbell lying external shoulder rotation | dumbbell | shoulders / delts |
| gv_0339 | dumbbell lying femoral | dumbbell | upper legs / hamstrings |
| gv_2470 | dumbbell lying on floor rear delt raise | dumbbell | shoulders / delts |
| gv_0341 | dumbbell lying one arm deltoid rear | dumbbell | shoulders / delts |
| gv_0344 | dumbbell lying one arm pronated triceps extension | dumbbell | upper arms / triceps |
| gv_0345 | dumbbell lying one arm rear lateral raise | dumbbell | shoulders / delts |
| gv_0346 | dumbbell lying one arm supinated triceps extension | dumbbell | upper arms / triceps |
| gv_0347 | dumbbell lying pronation | dumbbell | lower arms / forearms |
| gv_2705 | dumbbell lying pronation on floor | dumbbell | lower arms / forearms |
| gv_1284 | dumbbell lying pullover on exercise ball | dumbbell | chest / pectorals |
| gv_1328 | dumbbell lying rear delt row | dumbbell | back / upper back |
| gv_0348 | dumbbell lying rear lateral raise | dumbbell | shoulders / delts |
| gv_1735 | dumbbell lying single extension | dumbbell | upper arms / triceps |
| gv_0349 | dumbbell lying supination | dumbbell | lower arms / forearms |
| gv_2706 | dumbbell lying supination on floor | dumbbell | lower arms / forearms |
| gv_1661 | dumbbell lying supine biceps curl | dumbbell | upper arms / biceps |
| gv_0350 | dumbbell lying supine curl | dumbbell | upper arms / biceps |
| gv_0351 | dumbbell lying triceps extension | dumbbell | upper arms / triceps |
| gv_1662 | dumbbell lying wide curl | dumbbell | upper arms / biceps |
| gv_0352 | dumbbell neutral grip bench press | dumbbell | upper arms / triceps |
| gv_1285 | dumbbell one arm bench fly | dumbbell | chest / pectorals |
| gv_0292 | dumbbell one arm bent-over row | dumbbell | back / upper back |
| gv_1286 | dumbbell one arm chest fly on exercise ball | dumbbell | chest / pectorals |
| gv_0353 | dumbbell one arm concentration curl (on stability ball) | dumbbell | upper arms / biceps |
| gv_1288 | dumbbell one arm fly on exercise ball | dumbbell | chest / pectorals |
| gv_1736 | dumbbell one arm french press on exercise ball | dumbbell | upper arms / triceps |
| gv_1621 | dumbbell one arm hammer press on exercise ball | dumbbell | upper arms / triceps |
| gv_0354 | dumbbell one arm kickback | dumbbell | upper arms / triceps |
| gv_0355 | dumbbell one arm lateral raise | dumbbell | shoulders / delts |
| gv_0356 | dumbbell one arm lateral raise with support | dumbbell | shoulders / delts |
| gv_1665 | dumbbell one arm prone curl | dumbbell | upper arms / biceps |
| gv_1666 | dumbbell one arm prone hammer curl | dumbbell | upper arms / biceps |
| gv_1291 | dumbbell one arm pullover on exercise ball | dumbbell | chest / pectorals |
| gv_0359 | dumbbell one arm reverse fly (with support) | dumbbell | shoulders / delts |
| gv_1667 | dumbbell one arm reverse spider curl | dumbbell | upper arms / biceps |
| gv_0358 | dumbbell one arm reverse wrist curl | dumbbell | lower arms / forearms |
| gv_1668 | dumbbell one arm seated bicep curl on exercise ball | dumbbell | upper arms / biceps |
| gv_1669 | dumbbell one arm seated hammer curl | dumbbell | upper arms / biceps |
| gv_1415 | dumbbell one arm seated neutral wrist curl | dumbbell | lower arms / forearms |
| gv_0361 | dumbbell one arm shoulder press | dumbbell | shoulders / delts |
| gv_0360 | dumbbell one arm shoulder press v. 2 | dumbbell | shoulders / delts |
| gv_3888 | dumbbell one arm snatch | dumbbell | upper legs / glutes |
| gv_1670 | dumbbell one arm standing curl | dumbbell | upper arms / biceps |
| gv_1671 | dumbbell one arm standing hammer curl | dumbbell | upper arms / biceps |
| gv_0362 | dumbbell one arm triceps extension (on bench) | dumbbell | upper arms / triceps |
| gv_0363 | dumbbell one arm upright row | dumbbell | shoulders / delts |
| gv_0364 | dumbbell one arm wrist curl | dumbbell | lower arms / forearms |
| gv_1292 | dumbbell one leg fly on exercise ball | dumbbell | chest / pectorals |
| gv_0365 | dumbbell over bench neutral wrist curl | dumbbell | upper arms / biceps |
| gv_0366 | dumbbell over bench one arm neutral wrist curl | dumbbell | upper arms / biceps |
| gv_1441 | dumbbell over bench one arm reverse wrist curl | dumbbell | lower arms / forearms |
| gv_0367 | dumbbell over bench one arm wrist curl | dumbbell | lower arms / forearms |
| gv_0368 | dumbbell over bench revers wrist curl | dumbbell | lower arms / forearms |
| gv_0369 | dumbbell over bench wrist curl | dumbbell | lower arms / forearms |
| gv_1329 | dumbbell palm rotational bent over row | dumbbell | back / upper back |
| gv_1623 | dumbbell palms in incline bench press | dumbbell | upper arms / triceps |
| gv_0370 | dumbbell peacher hammer curl | dumbbell | upper arms / biceps |
| gv_0371 | dumbbell plyo squat | dumbbell | upper legs / glutes |
| gv_0373 | dumbbell pronate-grip triceps extension | dumbbell | upper arms / triceps |
| gv_0374 | dumbbell prone incline curl | dumbbell | upper arms / biceps |
| gv_1674 | dumbbell prone incline hammer curl | dumbbell | upper arms / biceps |
| gv_0375 | dumbbell pullover | dumbbell | chest / pectorals |
| gv_1294 | dumbbell pullover hip extension on exercise ball | dumbbell | chest / pectorals |
| gv_1295 | dumbbell pullover on exercise ball | dumbbell | chest / pectorals |
| gv_1700 | dumbbell push press | dumbbell | shoulders / delts |
| gv_0376 | dumbbell raise | dumbbell | shoulders / delts |
| gv_2292 | dumbbell rear delt raise | dumbbell | shoulders / delts |
| gv_0377 | dumbbell rear delt row_shoulder | dumbbell | shoulders / delts |
| gv_0378 | dumbbell rear fly | dumbbell | shoulders / delts |
| gv_0380 | dumbbell rear lateral raise | dumbbell | shoulders / delts |
| gv_0379 | dumbbell rear lateral raise (support head) | dumbbell | shoulders / delts |
| gv_0381 | dumbbell rear lunge | dumbbell | upper legs / glutes |
| gv_0382 | dumbbell revers grip biceps curl | dumbbell | upper arms / biceps |
| gv_0383 | dumbbell reverse fly | dumbbell | shoulders / delts |
| gv_1330 | dumbbell reverse grip incline bench one arm row | dumbbell | back / upper back |
| gv_1331 | dumbbell reverse grip incline bench two arm row | dumbbell | back / upper back |
| gv_2327 | dumbbell reverse grip row (female) | dumbbell | back / upper back |
| gv_1675 | dumbbell reverse spider curl | dumbbell | upper arms / biceps |
| gv_0385 | dumbbell reverse wrist curl | dumbbell | lower arms / forearms |
| gv_1459 | dumbbell romanian deadlift | dumbbell | upper legs / glutes |
| gv_0386 | dumbbell rotation reverse fly | dumbbell | shoulders / delts |
| gv_2397 | dumbbell scott press | dumbbell | shoulders / delts |
| gv_0387 | dumbbell seated alternate front raise | dumbbell | shoulders / delts |
| gv_1676 | dumbbell seated alternate hammer curl on exercise ball | dumbbell | upper arms / biceps |
| gv_0388 | dumbbell seated alternate press | dumbbell | shoulders / delts |
| gv_3546 | dumbbell seated alternate shoulder | dumbbell | shoulders / delts |
| gv_0389 | dumbbell seated bench extension | dumbbell | upper arms / triceps |
| gv_2317 | dumbbell seated bent arm lateral raise | dumbbell | shoulders / delts |
| gv_1730 | dumbbell seated bent over alternate kickback | dumbbell | upper arms / triceps |
| gv_1737 | dumbbell seated bent over triceps extension | dumbbell | upper arms / triceps |
| gv_1677 | dumbbell seated bicep curl | dumbbell | upper arms / biceps |
| gv_0390 | dumbbell seated biceps curl (on stability ball) | dumbbell | upper arms / biceps |
| gv_3547 | dumbbell seated biceps curl to shoulder press | dumbbell | upper arms / biceps |
| gv_0391 | dumbbell seated curl | dumbbell | upper arms / biceps |
| gv_0392 | dumbbell seated front raise | dumbbell | shoulders / delts |
| gv_1678 | dumbbell seated hammer curl | dumbbell | upper arms / biceps |
| gv_0393 | dumbbell seated inner biceps curl | dumbbell | upper arms / biceps |
| gv_0394 | dumbbell seated kickback | dumbbell | upper arms / triceps |
| gv_0396 | dumbbell seated lateral raise | dumbbell | shoulders / delts |
| gv_0395 | dumbbell seated lateral raise v. 2 | dumbbell | shoulders / delts |
| gv_0397 | dumbbell seated neutral wrist curl | dumbbell | upper arms / biceps |
| gv_1679 | dumbbell seated one arm bicep curl on exercise ball with leg raised | dumbbell | upper arms / biceps |
| gv_0398 | dumbbell seated one arm kickback | dumbbell | upper arms / triceps |
| gv_0399 | dumbbell seated one arm rotate | dumbbell | lower arms / forearms |
| gv_0400 | dumbbell seated one leg calf raise | dumbbell | lower legs / calves |
| gv_1380 | dumbbell seated one leg calf raise - hammer grip | dumbbell | lower legs / calves |
| gv_1381 | dumbbell seated one leg calf raise - palm up | dumbbell | lower legs / calves |
| gv_0401 | dumbbell seated palms up wrist curl | dumbbell | lower arms / forearms |
| gv_0403 | dumbbell seated revers grip concentration curl | dumbbell | upper arms / biceps |
| gv_1738 | dumbbell seated reverse grip one arm overhead tricep extension | dumbbell | upper arms / triceps |
| gv_0405 | dumbbell seated shoulder press | dumbbell | shoulders / delts |
| gv_0404 | dumbbell seated shoulder press (parallel grip) | dumbbell | shoulders / delts |
| gv_2188 | dumbbell seated triceps extension | dumbbell | upper arms / triceps |
| gv_0406 | dumbbell shrug | dumbbell | back / traps |
| gv_0407 | dumbbell side bend | dumbbell | waist / abs |
| gv_0408 | dumbbell side lying one hand raise | dumbbell | shoulders / delts |
| gv_3664 | dumbbell side plank with rear fly | dumbbell | back / upper back |
| gv_3548 | dumbbell single arm overhead carry | dumbbell | shoulders / delts |
| gv_0409 | dumbbell single leg calf raise | dumbbell | lower legs / calves |
| gv_1757 | dumbbell single leg deadlift | dumbbell | upper legs / glutes |
| gv_2805 | dumbbell single leg deadlift with stepbox support | dumbbell | upper legs / glutes |
| gv_0410 | dumbbell single leg split squat | dumbbell | upper legs / quads |
| gv_0411 | dumbbell single leg squat | dumbbell | upper legs / glutes |
| gv_0413 | dumbbell squat | dumbbell | upper legs / glutes |
| gv_3560 | dumbbell standing alternate hammer curl and press | dumbbell | upper arms / biceps |
| gv_0414 | dumbbell standing alternate overhead press | dumbbell | shoulders / delts |
| gv_0415 | dumbbell standing alternate raise | dumbbell | shoulders / delts |
| gv_1739 | dumbbell standing alternating tricep kickback | dumbbell | upper arms / triceps |
| gv_2143 | dumbbell standing around world | dumbbell | shoulders / delts |
| gv_1740 | dumbbell standing bent over one arm triceps extension | dumbbell | upper arms / triceps |
| gv_1741 | dumbbell standing bent over two arm triceps extension | dumbbell | upper arms / triceps |
| gv_0416 | dumbbell standing biceps curl | dumbbell | upper arms / biceps |
| gv_0417 | dumbbell standing calf raise | dumbbell | lower legs / calves |
| gv_0418 | dumbbell standing concentration curl | dumbbell | upper arms / biceps |
| gv_0419 | dumbbell standing front raise above head | dumbbell | shoulders / delts |
| gv_2321 | dumbbell standing inner biceps curl v. 2 | dumbbell | upper arms / biceps |
| gv_0420 | dumbbell standing kickback | dumbbell | upper arms / triceps |
| gv_0421 | dumbbell standing one arm concentration curl | dumbbell | upper arms / biceps |
| gv_0422 | dumbbell standing one arm curl (over incline bench) | dumbbell | upper arms / biceps |
| gv_1680 | dumbbell standing one arm curl over incline bench | dumbbell | upper arms / biceps |
| gv_0423 | dumbbell standing one arm extension | dumbbell | upper arms / triceps |
| gv_0424 | dumbbell standing one arm palm in press | dumbbell | shoulders / delts |
| gv_0425 | dumbbell standing one arm reverse curl | dumbbell | upper arms / biceps |
| gv_0426 | dumbbell standing overhead press | dumbbell | shoulders / delts |
| gv_0427 | dumbbell standing palms in press | dumbbell | shoulders / delts |
| gv_0429 | dumbbell standing reverse curl | dumbbell | upper arms / biceps |
| gv_0430 | dumbbell standing triceps extension | dumbbell | upper arms / triceps |
| gv_1684 | dumbbell step up single leg balance with bicep curl | dumbbell | upper arms / biceps |
| gv_0431 | dumbbell step-up | dumbbell | upper legs / glutes |
| gv_2796 | dumbbell step-up lunge | dumbbell | upper legs / quads |
| gv_2812 | dumbbell step-up split squat | dumbbell | upper legs / quads |
| gv_0432 | dumbbell stiff leg deadlift | dumbbell | upper legs / glutes |
| gv_0433 | dumbbell straight arm pullover | dumbbell | chest / pectorals |
| gv_0434 | dumbbell straight leg deadlift | dumbbell | upper legs / glutes |
| gv_2808 | dumbbell sumo pull through | dumbbell | upper legs / glutes |
| gv_2803 | dumbbell supported squat | dumbbell | upper legs / quads |
| gv_0436 | dumbbell tate press | dumbbell | upper arms / triceps |
| gv_1742 | dumbbell tricep kickback with stork stance | dumbbell | upper arms / triceps |
| gv_1743 | dumbbell twisting bench press | dumbbell | upper arms / triceps |
| gv_0437 | dumbbell upright row | dumbbell | shoulders / delts |
| gv_1765 | dumbbell upright row (back pov) | dumbbell | shoulders / delts |
| gv_0864 | dumbbell upright shoulder external rotation | dumbbell | shoulders / delts |
| gv_0438 | dumbbell w-press | dumbbell | shoulders / delts |
| gv_5201 | dumbbell waiter biceps curl | dumbbell | upper arms / biceps |
| gv_0439 | dumbbell zottman curl | dumbbell | upper arms / biceps |
| gv_2189 | dumbbells seated triceps extension | dumbbell | upper arms / triceps |
| gv_1382 | exercise ball on the wall calf raise | dumbbell | lower legs / calves |
| gv_3241 | exercise ball on the wall calf raise (tennis ball between ankles) | dumbbell | lower legs / calves |
| gv_3240 | exercise ball on the wall calf raise (tennis ball between knees) | dumbbell | lower legs / calves |
| gv_1746 | exercise ball supine triceps extension | dumbbell | upper arms / triceps |
| gv_1747 | ez bar french press on exercise ball | ez barbell | upper arms / triceps |
| gv_3010 | ez bar lying bent arms pullover | ez barbell | back / lats |
| gv_1748 | ez bar lying close grip triceps extension behind head | ez barbell | upper arms / triceps |
| gv_1344 | ez bar reverse grip bent over row | ez barbell | back / upper back |
| gv_1682 | ez bar seated close grip concentration curl | ez barbell | upper arms / biceps |
| gv_1749 | ez bar standing french press | ez barbell | upper arms / triceps |
| gv_0445 | ez barbell anti gravity press | ez barbell | shoulders / delts |
| gv_0446 | ez barbell close-grip curl | ez barbell | upper arms / biceps |
| gv_0447 | ez barbell curl | ez barbell | upper arms / biceps |
| gv_0448 | ez barbell decline close grip face press | ez barbell | upper arms / triceps |
| gv_2186 | ez barbell decline triceps extension | ez barbell | upper arms / triceps |
| gv_0449 | ez barbell incline triceps extension | ez barbell | upper arms / triceps |
| gv_0450 | ez barbell jm bench press | ez barbell | upper arms / triceps |
| gv_0451 | ez barbell reverse grip curl | ez barbell | upper arms / biceps |
| gv_1458 | ez barbell seated curls | ez barbell | upper arms / biceps |
| gv_0453 | ez barbell seated triceps extension | ez barbell | upper arms / triceps |
| gv_0454 | ez barbell spider curl | ez barbell | upper arms / biceps |
| gv_1628 | ez barbell spider curl | ez barbell | upper arms / biceps |
| gv_2404 | ez-bar biceps curl (with arm blaster) | ez barbell | upper arms / biceps |
| gv_2432 | ez-bar close-grip bench press | ez barbell | upper arms / triceps |
| gv_2741 | ez-barbell standing wide grip biceps curl | ez barbell | upper arms / biceps |
| gv_2133 | farmers walk | dumbbell | upper legs / quads |
| gv_0455 | finger curls | barbell | lower arms / forearms |
| gv_0458 | floor fly (with barbell) | barbell | chest / pectorals |
| gv_3234 | hyght dumbbell fly | dumbbell | chest / pectorals |
| gv_0562 | landmine 180 | barbell | waist / abs |
| gv_3237 | landmine lateral raise | barbell | shoulders / delts |
| gv_0574 | lever bent over row | barbell | back / upper back |
| gv_0589 | lever one arm bent over row | barbell | back / upper back |
| gv_0636 | olympic barbell hammer curl | olympic barbell | upper arms / biceps |
| gv_0637 | olympic barbell triceps extension | olympic barbell | upper arms / triceps |
| gv_0648 | power clean | barbell | upper legs / hamstrings |
| gv_0660 | push-up close-grip off dumbbell | dumbbell | upper arms / triceps |
| gv_0727 | single leg calf raise (on a dumbbell) | dumbbell | lower legs / calves |
| gv_0776 | snatch pull | barbell | upper legs / quads |
| gv_0777 | spell caster | dumbbell | waist / abs |
| gv_0788 | standing behind neck press | barbell | shoulders / delts |
| gv_0811 | trap bar deadlift | trap bar | upper legs / glutes |
