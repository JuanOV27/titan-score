package com.ironquest.mvp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.Usuario;
import com.ironquest.mvp.util.PasswordUtil;

import java.time.LocalDate;

public class AuthActivity extends AppCompatActivity {

    private DataManager dataManager;
    private DataStore dataStore;
    private boolean modoRegistro;

    private TextView textTitulo;
    private View layoutCamposRegistro;
    private View layoutCampoEdad;
    private TextInputEditText editUsername;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextInputEditText editEdad;
    private MaterialButton buttonSubmit;
    private TextView textToggleModo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();

        textTitulo = findViewById(R.id.text_auth_title);
        layoutCamposRegistro = findViewById(R.id.layout_campos_registro);
        layoutCampoEdad = findViewById(R.id.layout_campo_edad);
        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        editEdad = findViewById(R.id.edit_edad);
        buttonSubmit = findViewById(R.id.button_auth_submit);
        textToggleModo = findViewById(R.id.text_toggle_auth_mode);

        modoRegistro = dataStore.usuario == null;

        textToggleModo.setOnClickListener(v -> {
            modoRegistro = !modoRegistro;
            actualizarModo();
        });
        buttonSubmit.setOnClickListener(v -> {
            if (modoRegistro) {
                registrar();
            } else {
                iniciarSesion();
            }
        });

        actualizarModo();
    }

    private void actualizarModo() {
        boolean hayUsuario = dataStore.usuario != null;
        layoutCamposRegistro.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        layoutCampoEdad.setVisibility(modoRegistro ? View.VISIBLE : View.GONE);
        textTitulo.setText(modoRegistro ? "Crea tu cuenta" : "Inicia sesión");
        buttonSubmit.setText(modoRegistro ? "Registrarme" : "Iniciar sesión");
        textToggleModo.setVisibility(hayUsuario ? View.VISIBLE : View.GONE);
        textToggleModo.setText(modoRegistro ? "¿Ya tienes cuenta? Inicia sesión" : "¿No tienes cuenta? Regístrate");
    }

    private void registrar() {
        String username = textoDe(editUsername);
        String email = textoDe(editEmail);
        String password = contrasenaDe(editPassword);
        String edadTexto = textoDe(editEdad);

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || edadTexto.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ingresa un correo válido", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }
        int edad;
        try {
            edad = Integer.parseInt(edadTexto);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ingresa una edad válida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (edad < 10 || edad > 100) {
            Toast.makeText(this, "Ingresa una edad válida", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario usuario = new Usuario();
        usuario.id = dataManager.newId("user");
        usuario.username = username;
        usuario.email = email;
        usuario.passwordHash = PasswordUtil.hash(password);
        usuario.edad = edad;
        usuario.sesionActiva = true;
        usuario.fechaRegistro = LocalDate.now().toString();

        dataStore.usuario = usuario;
        dataManager.save();

        continuarDespuesDeAuth();
    }

    private void iniciarSesion() {
        String email = textoDe(editEmail);
        String password = contrasenaDe(editPassword);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario usuario = dataStore.usuario;
        if (usuario == null || !usuario.email.equalsIgnoreCase(email)
                || !usuario.passwordHash.equals(PasswordUtil.hash(password))) {
            Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show();
            return;
        }

        usuario.sesionActiva = true;
        dataManager.save();

        continuarDespuesDeAuth();
    }

    private void continuarDespuesDeAuth() {
        Intent intent = new Intent(this, RoutineListActivity.class);
        intent.putExtra(RoutineListActivity.EXTRA_SUGERIR_FISICO, dataStore.historialFisico.isEmpty());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String textoDe(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString().trim();
    }

    private String contrasenaDe(TextInputEditText edit) {
        return edit.getText() == null ? "" : edit.getText().toString();
    }
}
