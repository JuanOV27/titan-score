package com.ironquest.mvp.data;

import androidx.annotation.Nullable;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

/** Único punto que toca {@link FirebaseAuth} en todo el proyecto. */
public class AuthManager {

    /** Resultado de un registro o inicio de sesión. */
    public interface Callback {
        void onExito(String uid);
        void onError(String mensaje);
    }

    private static AuthManager instance;

    private final FirebaseAuth firebaseAuth;

    private AuthManager() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    @Nullable
    public FirebaseUser usuarioActual() {
        return firebaseAuth.getCurrentUser();
    }

    public void registrar(String email, String password, Callback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(resultado -> callback.onExito(resultado.getUser().getUid()))
                .addOnFailureListener(e -> callback.onError(traducirError(e)));
    }

    public void iniciarSesion(String email, String password, Callback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(resultado -> callback.onExito(resultado.getUser().getUid()))
                .addOnFailureListener(e -> callback.onError(traducirError(e)));
    }

    private static String traducirError(Exception excepcion) {
        if (excepcion instanceof FirebaseAuthUserCollisionException) {
            return "Ese correo ya tiene una cuenta";
        }
        if (excepcion instanceof FirebaseAuthInvalidCredentialsException) {
            return "Correo o contraseña incorrectos";
        }
        if (excepcion instanceof FirebaseAuthInvalidUserException) {
            return "No existe una cuenta con ese correo";
        }
        if (excepcion instanceof FirebaseAuthWeakPasswordException) {
            return "La contraseña es demasiado débil";
        }
        if (excepcion instanceof FirebaseNetworkException) {
            return "Sin conexión a internet";
        }
        return excepcion.getMessage() != null ? excepcion.getMessage() : "No se pudo completar la operación";
    }
}
