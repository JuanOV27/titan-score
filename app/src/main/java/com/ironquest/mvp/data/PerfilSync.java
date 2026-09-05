package com.ironquest.mvp.data;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.model.Usuario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Único punto que toca Firestore para el perfil del usuario. Respaldo "write-behind": cada
 * push reescribe TODO el estado actual (no hay sincronización incremental ni bandera de
 * "ya migré"), así un intento fallido simplemente se corrige solo en el próximo push.
 */
public class PerfilSync {

    public interface CallbackPull {
        void onListo(Usuario usuario, List<RegistroFisico> historial);
        void onError(String mensaje);
    }

    private static PerfilSync instance;

    private final FirebaseFirestore firestore;

    private PerfilSync() {
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized PerfilSync getInstance() {
        if (instance == null) {
            instance = new PerfilSync();
        }
        return instance;
    }

    public void pushCompleto(String uid, Usuario usuario, List<RegistroFisico> historial) {
        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection("usuarios").document(uid), mapaDeUsuario(usuario));
        for (RegistroFisico registro : historial) {
            batch.set(firestore.collection("usuarios").document(uid)
                    .collection("historialFisico").document(registro.getId()), mapaDeRegistro(registro));
        }
        batch.commit();
    }

    /** Lectura única, pensada para reponer el perfil en un teléfono nuevo o reinstalado. */
    public void pullUnaVez(String uid, CallbackPull callback) {
        firestore.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documento -> {
                    if (!documento.exists() || documento.getData() == null) {
                        callback.onError("No hay perfil guardado en la nube todavía");
                        return;
                    }
                    Usuario usuario = usuarioDesdeMapa(uid, documento.getData());
                    firestore.collection("usuarios").document(uid).collection("historialFisico").get()
                            .addOnSuccessListener(coleccion -> {
                                List<RegistroFisico> historial = new ArrayList<>();
                                for (QueryDocumentSnapshot doc : coleccion) {
                                    historial.add(registroDesdeMapa(doc.getId(), doc.getData()));
                                }
                                callback.onListo(usuario, historial);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private static Map<String, Object> mapaDeUsuario(Usuario usuario) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("username", usuario.getUsername());
        mapa.put("edad", usuario.getEdad());
        mapa.put("fechaRegistro", usuario.getFechaRegistro());
        mapa.put("aceptoTerminos", usuario.isAceptoTerminos());
        mapa.put("fechaAceptacionTerminos", usuario.getFechaAceptacionTerminos());
        return mapa;
    }

    private static Usuario usuarioDesdeMapa(String uid, Map<String, Object> mapa) {
        Usuario usuario = new Usuario();
        usuario.setFirebaseUid(uid);
        usuario.setUsername((String) mapa.get("username"));
        usuario.setEdad(entero(mapa.get("edad")));
        usuario.setFechaRegistro((String) mapa.get("fechaRegistro"));
        Object acepto = mapa.get("aceptoTerminos");
        usuario.setAceptoTerminos(acepto instanceof Boolean && (Boolean) acepto);
        usuario.setFechaAceptacionTerminos((String) mapa.get("fechaAceptacionTerminos"));
        return usuario;
    }

    private static Map<String, Object> mapaDeRegistro(RegistroFisico registro) {
        Map<String, Object> mapa = new HashMap<>();
        mapa.put("fecha", registro.getFecha());
        mapa.put("alturaCm", registro.getAlturaCm());
        mapa.put("pesoKg", registro.getPesoKg());
        mapa.put("imc", registro.getImc());
        mapa.put("pechoCm", registro.getPechoCm());
        mapa.put("cinturaCm", registro.getCinturaCm());
        mapa.put("caderaCm", registro.getCaderaCm());
        mapa.put("brazoCm", registro.getBrazoCm());
        mapa.put("piernaCm", registro.getPiernaCm());
        mapa.put("tipoCuerpo", registro.getTipoCuerpo());
        return mapa;
    }

    private static RegistroFisico registroDesdeMapa(String id, Map<String, Object> mapa) {
        RegistroFisico registro = new RegistroFisico();
        registro.setId(id);
        registro.setFecha((String) mapa.get("fecha"));
        registro.setAlturaCm(numero(mapa.get("alturaCm")));
        registro.setPesoKg(numero(mapa.get("pesoKg")));
        registro.setImc(numero(mapa.get("imc")));
        registro.setPechoCm(numero(mapa.get("pechoCm")));
        registro.setCinturaCm(numero(mapa.get("cinturaCm")));
        registro.setCaderaCm(numero(mapa.get("caderaCm")));
        registro.setBrazoCm(numero(mapa.get("brazoCm")));
        registro.setPiernaCm(numero(mapa.get("piernaCm")));
        registro.setTipoCuerpo((String) mapa.get("tipoCuerpo"));
        return registro;
    }

    private static double numero(Object valor) {
        return valor instanceof Number ? ((Number) valor).doubleValue() : 0.0;
    }

    private static int entero(Object valor) {
        return valor instanceof Number ? ((Number) valor).intValue() : 0;
    }
}
