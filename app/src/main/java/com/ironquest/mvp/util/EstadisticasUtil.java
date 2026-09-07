package com.ironquest.mvp.util;

import com.ironquest.mvp.model.Sesion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EstadisticasUtil {

    /** Propuesta §5.1: la racha se mantiene mientras no pasen más de esto sin una sesión. */
    private static final int TOLERANCIA_DIAS = 7;

    private EstadisticasUtil() {
    }

    /**
     * Cantidad de días con sesión registrada dentro de la racha activa (no días de calendario
     * transcurridos: un hueco sin entrenar no suma). No exige entrenar todos los días: la racha
     * se mantiene mientras el hueco entre sesiones consecutivas (y entre la última sesión y
     * hoy) no supere {@value #TOLERANCIA_DIAS} días. Un hueco mayor la corta.
     */
    public static int calcularRachaDias(List<Sesion> sesiones) {
        List<LocalDate> dias = diasConSesion(sesiones);
        if (dias.isEmpty()) {
            return 0;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate ultima = dias.get(dias.size() - 1);
        if (ChronoUnit.DAYS.between(ultima, hoy) > TOLERANCIA_DIAS) {
            return 0;
        }

        int racha = 1;
        LocalDate cursor = ultima;
        for (int i = dias.size() - 2; i >= 0; i--) {
            LocalDate anterior = dias.get(i);
            if (ChronoUnit.DAYS.between(anterior, cursor) > TOLERANCIA_DIAS) {
                break;
            }
            racha++;
            cursor = anterior;
        }

        return racha;
    }

    /** Minutos reales entre inicio y fin de la sesión. 0 si todavía no está finalizada. */
    public static long calcularDuracionMinutos(Sesion sesion) {
        if (!sesion.estaFinalizada()) {
            return 0;
        }
        return ChronoUnit.MINUTES.between(
                LocalDateTime.parse(sesion.getFechaHoraInicio()),
                LocalDateTime.parse(sesion.getFechaHoraFin()));
    }

    /** "45m", "2h" o "1h30m" — compacto para caber en la etiqueta de una barra. */
    public static String formatearDuracion(long minutosTotales) {
        long horas = minutosTotales / 60;
        long minutos = minutosTotales % 60;
        if (horas == 0) {
            return minutos + "m";
        }
        if (minutos == 0) {
            return horas + "h";
        }
        return horas + "h" + minutos + "m";
    }

    private static List<LocalDate> diasConSesion(List<Sesion> sesiones) {
        Set<LocalDate> unicos = new HashSet<>();
        for (Sesion sesion : sesiones) {
            if (sesion.estaFinalizada()) {
                unicos.add(LocalDateTime.parse(sesion.getFechaHoraFin()).toLocalDate());
            }
        }
        List<LocalDate> ordenados = new ArrayList<>(unicos);
        ordenados.sort(LocalDate::compareTo);
        return ordenados;
    }
}
