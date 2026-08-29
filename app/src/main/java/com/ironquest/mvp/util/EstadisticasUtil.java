package com.ironquest.mvp.util;

import com.ironquest.mvp.model.Sesion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class EstadisticasUtil {

    private EstadisticasUtil() {
    }

    /**
     * Días consecutivos (incluyendo hoy o ayer como punto de partida) con al menos
     * una sesión finalizada.
     */
    public static int calcularRachaDias(List<Sesion> sesiones) {
        Set<LocalDate> dias = new HashSet<>();
        for (Sesion sesion : sesiones) {
            if (sesion.fechaHoraFin != null) {
                dias.add(LocalDateTime.parse(sesion.fechaHoraFin).toLocalDate());
            }
        }

        LocalDate cursor = LocalDate.now();
        if (!dias.contains(cursor)) {
            cursor = cursor.minusDays(1);
            if (!dias.contains(cursor)) {
                return 0;
            }
        }

        int racha = 0;
        while (dias.contains(cursor)) {
            racha++;
            cursor = cursor.minusDays(1);
        }
        return racha;
    }
}
