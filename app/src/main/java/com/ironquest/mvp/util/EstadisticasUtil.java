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
     * Días transcurridos desde el inicio de la racha activa hasta hoy. No exige entrenar
     * todos los días: se mantiene mientras el hueco entre sesiones consecutivas (y entre la
     * última sesión y hoy) no supere {@value #TOLERANCIA_DIAS} días. Un hueco mayor corta la
     * racha.
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

        LocalDate inicio = ultima;
        for (int i = dias.size() - 2; i >= 0; i--) {
            LocalDate anterior = dias.get(i);
            if (ChronoUnit.DAYS.between(anterior, inicio) > TOLERANCIA_DIAS) {
                break;
            }
            inicio = anterior;
        }

        return (int) ChronoUnit.DAYS.between(inicio, hoy) + 1;
    }

    private static List<LocalDate> diasConSesion(List<Sesion> sesiones) {
        Set<LocalDate> unicos = new HashSet<>();
        for (Sesion sesion : sesiones) {
            if (sesion.fechaHoraFin != null) {
                unicos.add(LocalDateTime.parse(sesion.fechaHoraFin).toLocalDate());
            }
        }
        List<LocalDate> ordenados = new ArrayList<>(unicos);
        ordenados.sort(LocalDate::compareTo);
        return ordenados;
    }
}
