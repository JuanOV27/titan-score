package com.ironquest.mvp.util;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.RegistroFisico;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public class PerfilFisicoUtil {

    private PerfilFisicoUtil() {
    }

    public static double calcularImc(double pesoKg, double alturaCm) {
        double alturaM = alturaCm / 100.0;
        if (alturaM <= 0) {
            return 0;
        }
        return pesoKg / (alturaM * alturaM);
    }

    public static String clasificarImc(double imc) {
        if (imc < 18.5) {
            return "Bajo peso";
        }
        if (imc < 25) {
            return "Normal";
        }
        if (imc < 30) {
            return "Sobrepeso";
        }
        return "Obesidad";
    }

    public static int colorParaImc(double imc) {
        if (imc < 18.5) {
            return R.color.cumplimiento_medio;
        }
        if (imc < 25) {
            return R.color.cumplimiento_alto;
        }
        if (imc < 30) {
            return R.color.cumplimiento_medio;
        }
        return R.color.cumplimiento_bajo;
    }

    public static boolean necesitaActualizacion(List<RegistroFisico> historial) {
        if (historial == null || historial.isEmpty()) {
            return true;
        }
        RegistroFisico ultimo = historial.get(historial.size() - 1);
        LocalDate fecha = LocalDate.parse(ultimo.getFecha());
        return Period.between(fecha, LocalDate.now()).toTotalMonths() >= 1;
    }

    public static double calcularIcc(double cinturaCm, double caderaCm) {
        return cinturaCm / caderaCm;
    }

    public static String clasificarIcc(double icc, String tipoCuerpo) {
        if (esHombre(tipoCuerpo)) {
            if (icc < 0.90) {
                return "Bajo riesgo";
            }
            if (icc < 1.0) {
                return "Riesgo moderado";
            }
            return "Riesgo alto";
        }
        if (icc < 0.80) {
            return "Bajo riesgo";
        }
        if (icc < 0.85) {
            return "Riesgo moderado";
        }
        return "Riesgo alto";
    }

    public static int colorParaIcc(double icc, String tipoCuerpo) {
        String clasificacion = clasificarIcc(icc, tipoCuerpo);
        if (clasificacion.equals("Bajo riesgo")) {
            return R.color.cumplimiento_alto;
        }
        if (clasificacion.equals("Riesgo moderado")) {
            return R.color.cumplimiento_medio;
        }
        return R.color.cumplimiento_bajo;
    }

    public static double calcularIcEst(double cinturaCm, double alturaCm) {
        return cinturaCm / alturaCm;
    }

    public static String clasificarIcEst(double icEst) {
        if (icEst < 0.5) {
            return "Saludable";
        }
        if (icEst < 0.6) {
            return "Riesgo aumentado";
        }
        return "Riesgo alto";
    }

    public static int colorParaIcEst(double icEst) {
        if (icEst < 0.5) {
            return R.color.cumplimiento_alto;
        }
        if (icEst < 0.6) {
            return R.color.cumplimiento_medio;
        }
        return R.color.cumplimiento_bajo;
    }

    public static double calcularVTaper(double pechoCm, double cinturaCm) {
        return pechoCm / cinturaCm;
    }

    public static String clasificarVTaper(double vTaper) {
        if (vTaper >= 1.4) {
            return "Muy marcado";
        }
        if (vTaper >= 1.25) {
            return "Marcado";
        }
        if (vTaper >= 1.1) {
            return "Moderado";
        }
        return "Poco contraste";
    }

    public static int colorParaVTaper(double vTaper) {
        if (vTaper >= 1.25) {
            return R.color.cumplimiento_alto;
        }
        if (vTaper >= 1.1) {
            return R.color.cumplimiento_medio;
        }
        return R.color.cumplimiento_bajo;
    }

    public static List<String> generarRecomendaciones(RegistroFisico r) {
        List<String> lista = new ArrayList<>();
        boolean esHombre = esHombre(r.getTipoCuerpo());
        double icc = calcularIcc(r.getCinturaCm(), r.getCaderaCm());
        double icEst = calcularIcEst(r.getCinturaCm(), r.getAlturaCm());
        double vTaper = calcularVTaper(r.getPechoCm(), r.getCinturaCm());
        double piernaBrazo = r.getPiernaCm() / r.getBrazoCm();

        if (icEst >= 0.5) {
            lista.add("Tu cintura es amplia en relación a tu altura: reducir grasa abdominal mejoraría tu proporción general y tu salud metabólica.");
        }

        if (esHombre) {
            if (vTaper < 1.25) {
                lista.add("Trabajar pecho, espalda y hombros ayudaría a marcar más el contraste con tu cintura (efecto \"V\").");
            }
            if (icc >= 0.90) {
                lista.add("Tu índice cintura-cadera está por encima del rango recomendado; combina fuerza con un manejo calórico adecuado si buscas reducirlo.");
            }
        } else {
            if (icc >= 0.80) {
                lista.add("Tu índice cintura-cadera indica una cintura relativamente amplia frente a tu cadera; trabajo de core y control calórico pueden ayudar a marcar más la curva.");
            }
            if (vTaper < 1.05) {
                lista.add("Desarrollar espalda y hombros puede ayudar a equilibrar visualmente tu figura.");
            }
        }

        if (piernaBrazo < 1.4) {
            lista.add("Tus piernas se ven poco desarrolladas en proporción a tus brazos; vale la pena revisar si le estás dando suficiente volumen al entrenamiento de pierna.");
        } else if (piernaBrazo > 2.3) {
            lista.add("Tus brazos se ven poco desarrollados en proporción a tus piernas; suma más volumen de bíceps y tríceps.");
        }

        if (lista.isEmpty()) {
            lista.add("Tus proporciones están bien equilibradas según tus medidas actuales. ¡Sigue así!");
        }
        return lista;
    }

    private static boolean esHombre(String tipoCuerpo) {
        return "Hombre".equalsIgnoreCase(tipoCuerpo);
    }
}
