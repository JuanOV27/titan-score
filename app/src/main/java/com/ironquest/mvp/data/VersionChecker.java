package com.ironquest.mvp.data;

import com.google.firebase.firestore.FirebaseFirestore;

/** Único punto que toca Firestore para el documento público config/version. */
public class VersionChecker {

    /** Resultado de comparar la versión instalada contra config/version. */
    public static class Resultado {
        public final boolean bloqueoDuro;
        public final boolean avisoDisponible;
        public final String latestVersionName;
        public final String notas;
        public final String urlDescarga;

        Resultado(boolean bloqueoDuro, boolean avisoDisponible, String latestVersionName,
                  String notas, String urlDescarga) {
            this.bloqueoDuro = bloqueoDuro;
            this.avisoDisponible = avisoDisponible;
            this.latestVersionName = latestVersionName;
            this.notas = notas;
            this.urlDescarga = urlDescarga;
        }
    }

    public interface Callback {
        void onResultado(Resultado resultado);
    }

    private static VersionChecker instance;

    private final FirebaseFirestore firestore;

    private VersionChecker() {
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized VersionChecker getInstance() {
        if (instance == null) {
            instance = new VersionChecker();
        }
        return instance;
    }

    /** Sin red o sin documento: no llama al callback — no hay nada que avisar. */
    public void verificar(int versionCodeInstalado, Callback callback) {
        firestore.collection("config").document("version").get()
                .addOnSuccessListener(documento -> {
                    if (!documento.exists()) {
                        return;
                    }
                    Long minVersionCode = documento.getLong("minVersionCode");
                    Long latestVersionCode = documento.getLong("latestVersionCode");
                    if (minVersionCode == null || latestVersionCode == null) {
                        return;
                    }
                    boolean bloqueoDuro = versionCodeInstalado < minVersionCode;
                    boolean avisoDisponible = !bloqueoDuro && versionCodeInstalado < latestVersionCode;
                    if (!bloqueoDuro && !avisoDisponible) {
                        return;
                    }
                    callback.onResultado(new Resultado(
                            bloqueoDuro,
                            avisoDisponible,
                            documento.getString("latestVersionName"),
                            documento.getString("notas"),
                            documento.getString("urlDescarga")));
                });
    }
}
