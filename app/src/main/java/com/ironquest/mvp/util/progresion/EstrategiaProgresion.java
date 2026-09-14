package com.ironquest.mvp.util.progresion;

import com.ironquest.mvp.model.Ejercicio;
import com.ironquest.mvp.model.EjercicioSesion;
import com.ironquest.mvp.model.RutinaEjercicio;
import com.ironquest.mvp.model.Sesion;
import com.ironquest.mvp.model.SerieSesion;
import com.ironquest.mvp.model.Usuario;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sugiere el peso/reps del próximo intento de un ejercicio a partir de su historial real, según
 * el esquema de progresión configurado en la rutina. La sugerencia nunca se persiste: se
 * recalcula cada vez que arranca una sesión nueva, igual que la racha.
 *
 * <p>El peso base para incrementar siempre sale del último peso realmente registrado (no de
 * {@code RutinaEjercicio.getPeso()}, que ninguna estrategia toca), porque el usuario puede haber
 * ajustado el peso con los botones +/- durante la sesión.
 *
 * <p><b>Frontera de persistencia:</b> lo que se guarda en {@code datos.json} sigue siendo el
 * {@code int esquemaProgresion} de {@link RutinaEjercicio}. La estrategia se resuelve en memoria
 * a partir de ese entero con {@link #para(int)} y nunca se serializa: Gson no sabe reconstruir un
 * tipo polimórfico sin un adaptador dedicado, y este proyecto no introduce uno.
 */
public abstract class EstrategiaProgresion {

    /** Fallback si el usuario no está seteado (edge case en tests o boot). */
    private static final double INCREMENTO_KG_DEFAULT = 2.5;
    private static final int SESIONES_PARA_ESTANCAMIENTO = 3;

    /** Traduce el esquema guardado en la rutina a la estrategia que lo implementa. */
    public static EstrategiaProgresion para(int esquemaProgresion) {
        switch (esquemaProgresion) {
            case RutinaEjercicio.ESQUEMA_AUTOMATICO:
                return new ProgresionAutomatica();
            case RutinaEjercicio.ESQUEMA_LINEAL:
                return new ProgresionLineal();
            case RutinaEjercicio.ESQUEMA_GREYSKULL:
                return new ProgresionGreyskull();
            case RutinaEjercicio.ESQUEMA_DOBLE_PROGRESION:
                return new ProgresionDoble();
            case RutinaEjercicio.ESQUEMA_NINGUNO:
            default:
                return new SinProgresion();
        }
    }

    /**
     * Método plantilla: fija el flujo común a las cuatro estrategias y delega en
     * {@link #calcular} el único paso que varía entre ellas. No es {@code final} porque
     * {@link ProgresionAutomatica} lo reimplementa para trabajar sobre las últimas 3 sesiones
     * en vez de la última.
     */
    public Sugerencia sugerir(RutinaEjercicio config, Ejercicio ejercicio, Usuario usuario, List<Sesion> historial) {
        double incremento = getIncrementoEfectivo(ejercicio, usuario);
        if (!requiereHistorial()) {
            return calcular(config, null, config.getPeso(), incremento);
        }

        List<EjercicioSesion> pasadas = historialDelEjercicio(config.getEjercicioId(), historial);
        if (pasadas.isEmpty()) {
            return new Sugerencia(config.getPeso(), config.getRepeticiones(),
                    "Primera vez con este esquema: arrancas con el plan de la rutina.", false);
        }

        EjercicioSesion ultima = pasadas.get(pasadas.size() - 1);
        double pesoBase = pesoDeLaSesion(ultima, config.getPeso());

        int fallosSeguidos = contarFallosSeguidos(config, pasadas);
        if (fallosSeguidos >= SESIONES_PARA_ESTANCAMIENTO) {
            double pesoDeload = redondearA(pesoBase * 0.9, incremento);
            return new Sugerencia(pesoDeload, config.getRepeticiones(),
                    "Llevas " + fallosSeguidos + " sesiones seguidas sin cumplir el objetivo. Posible "
                            + "estancamiento: baja a " + formatearPeso(pesoDeload) + "kg y retoma la progresión.",
                    true);
        }

        return calcular(config, ultima, pesoBase, incremento);
    }

    /**
     * Incremento de peso a usar para este ejercicio: override del ejercicio si > 0, si no el
     * global del usuario. Fallback 2.5 kg si no hay usuario (no debería pasar en runtime).
     */
    protected static double getIncrementoEfectivo(Ejercicio ejercicio, Usuario usuario) {
        if (ejercicio != null && ejercicio.getIncrementoPeso() > 0) {
            return ejercicio.getIncrementoPeso();
        }
        if (usuario != null && usuario.getIncrementoPeso() > 0) {
            return usuario.getIncrementoPeso();
        }
        return INCREMENTO_KG_DEFAULT;
    }

    /**
     * Distingue una estrategia real de la ausencia de estrategia (Objeto Nulo): solo
     * {@link SinProgresion} responde {@code false}, y por eso es la única que nunca ve el
     * mensaje de "primera vez" ni el conteo de estancamiento.
     */
    protected abstract boolean requiereHistorial();

    /** El único paso que varía por esquema: qué sugerir a partir de la última sesión real. */
    protected abstract Sugerencia calcular(RutinaEjercicio config, EjercicioSesion ultima, double pesoBase, double incremento);

    /** Si esa sesión pasada cumplió el objetivo según esta estrategia. Alimenta el conteo de estancamiento. */
    protected abstract boolean fueExitosa(RutinaEjercicio config, EjercicioSesion sesionPasada);

    // ---- flujo compartido ----

    private static List<EjercicioSesion> historialDelEjercicio(String ejercicioId, List<Sesion> sesiones) {
        List<Sesion> finalizadas = new ArrayList<>();
        for (Sesion sesion : sesiones) {
            if (sesion.estaFinalizada()) {
                finalizadas.add(sesion);
            }
        }
        finalizadas.sort(Comparator.comparing(s -> LocalDateTime.parse(s.getFechaHoraInicio())));

        List<EjercicioSesion> resultado = new ArrayList<>();
        for (Sesion sesion : finalizadas) {
            EjercicioSesion es = sesion.buscarEjercicio(ejercicioId);
            if (es != null) {
                resultado.add(es);
            }
        }
        return resultado;
    }

    /** Cuenta hacia atrás desde la sesión más reciente; despacho polimórfico vía {@link #fueExitosa}. */
    private int contarFallosSeguidos(RutinaEjercicio config, List<EjercicioSesion> pasadas) {
        int fallos = 0;
        for (int i = pasadas.size() - 1; i >= 0; i--) {
            if (fueExitosa(config, pasadas.get(i))) {
                break;
            }
            fallos++;
        }
        return fallos;
    }

    // ---- ayudantes para las subclases ----

    protected static boolean todasLasSeriesCumplen(EjercicioSesion sesionPasada, int repsObjetivo) {
        boolean algunaCompletada = false;
        for (SerieSesion serie : sesionPasada.getSeries()) {
            if (!serie.isCompletada()) {
                continue;
            }
            algunaCompletada = true;
            if (serie.getRepeticiones() < repsObjetivo) {
                return false;
            }
        }
        return algunaCompletada;
    }

    /** Mínimo de reps entre las series completadas: la serie más floja determina si el rango se cumplió. */
    protected static int repeticionesMinimasCompletadas(EjercicioSesion sesionPasada) {
        int minimo = -1;
        for (SerieSesion serie : sesionPasada.getSeries()) {
            if (!serie.isCompletada()) {
                continue;
            }
            if (minimo == -1 || serie.getRepeticiones() < minimo) {
                minimo = serie.getRepeticiones();
            }
        }
        return Math.max(minimo, 0);
    }

    protected static double pesoDeLaSesion(EjercicioSesion sesionPasada, double fallback) {
        SerieSesion ultima = sesionPasada.getUltimaSerieCompletada();
        return ultima != null ? ultima.getPeso() : fallback;
    }

    protected static double redondearA(double valor, double paso) {
        return Math.round(valor / paso) * paso;
    }

    protected static String formatearPeso(double peso) {
        if (peso == Math.floor(peso)) {
            return String.valueOf((long) peso);
        }
        return String.valueOf(peso);
    }
}
