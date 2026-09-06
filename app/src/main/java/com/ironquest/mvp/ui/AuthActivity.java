package com.ironquest.mvp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ironquest.mvp.R;
import com.ironquest.mvp.data.AuthManager;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.data.PerfilSync;
import com.ironquest.mvp.model.DataStore;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.model.Usuario;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AuthActivity extends AppCompatActivity {

    private enum Modo { REGISTRO, LOGIN, VINCULAR }

    private static final String TERMINOS_TEXTO =
            "Al usar Titan Score, aceptas que la app guarde tu nombre de usuario, correo "
            + "electrónico, edad y las medidas corporales que registres (peso, altura, pecho, "
            + "cintura, cadera, brazo, pierna) asociadas a tu cuenta, con el fin de mostrarte tu "
            + "progreso físico a lo largo del tiempo.\n\n"
            + "Estos datos se almacenan de dos formas: localmente en tu teléfono, y como "
            + "respaldo en Firebase (Google), un servicio en la nube, para que no los pierdas si "
            + "cambias de equipo o reinstalas la app.\n\n"
            + "Tus rutinas, ejercicios y sesiones de entrenamiento se guardan únicamente en tu "
            + "teléfono y no se suben a ningún servidor.\n\n"
            + "No compartimos tus datos con terceros. Puedes dejar de usar la app en cualquier "
            + "momento; los datos seguirán guardados hasta que los elimines manualmente.";

    private DataManager dataManager;
    private DataStore dataStore;
    private AuthManager authManager;
    private Modo modo;
    private boolean esVinculacion;

    private TextView textTitulo;
    private TextView textAuthSubtitulo;
    private View layoutCamposRegistro;
    private View layoutCampoEdad;
    private View layoutTerminos;
    private TextInputLayout layoutInputPassword;
    private TextInputEditText editUsername;
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private TextInputEditText editEdad;
    private CheckBox checkTerminos;
    private TextView textVerTerminos;
    private MaterialButton buttonSubmit;
    private TextView textToggleModo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        dataManager = DataManager.getInstance(this);
        dataStore = dataManager.getDataStore();
        authManager = AuthManager.getInstance();

        textTitulo = findViewById(R.id.text_auth_title);
        textAuthSubtitulo = findViewById(R.id.text_auth_subtitulo);
        layoutCamposRegistro = findViewById(R.id.layout_campos_registro);
        layoutCampoEdad = findViewById(R.id.layout_campo_edad);
        layoutTerminos = findViewById(R.id.layout_terminos);
        layoutInputPassword = findViewById(R.id.layout_input_password);
        editUsername = findViewById(R.id.edit_username);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        editEdad = findViewById(R.id.edit_edad);
        checkTerminos = findViewById(R.id.check_terminos);
        textVerTerminos = findViewById(R.id.text_ver_terminos);
        buttonSubmit = findViewById(R.id.button_auth_submit);
        textToggleModo = findViewById(R.id.text_toggle_auth_mode);

        Usuario usuarioLocal = dataStore.getUsuario();
        esVinculacion = usuarioLocal != null
                && (usuarioLocal.getFirebaseUid() == null || usuarioLocal.getFirebaseUid().isEmpty());
        modo = esVinculacion ? Modo.VINCULAR : (usuarioLocal == null ? Modo.REGISTRO : Modo.LOGIN);

        textToggleModo.setOnClickListener(v -> {
            modo = (modo == Modo.REGISTRO) ? Modo.LOGIN : Modo.REGISTRO;
            actualizarModo();
        });
        textVerTerminos.setOnClickListener(v -> mostrarTerminos());
        buttonSubmit.setOnClickListener(v -> enviar());

        actualizarModo();
    }

    private void actualizarModo() {
        layoutCamposRegistro.setVisibility(modo == Modo.REGISTRO ? View.VISIBLE : View.GONE);
        layoutCampoEdad.setVisibility(modo == Modo.REGISTRO ? View.VISIBLE : View.GONE);
        layoutTerminos.setVisibility(modo == Modo.LOGIN ? View.GONE : View.VISIBLE);
        textToggleModo.setVisibility(esVinculacion ? View.GONE : View.VISIBLE);

        editEmail.setEnabled(modo != Modo.VINCULAR);
        if (modo == Modo.VINCULAR) {
            editEmail.setText(dataStore.getUsuario().getEmail());
            layoutInputPassword.setHint("Crea una contraseña para la nube");
        } else {
            layoutInputPassword.setHint("Contraseña");
        }

        if (modo == Modo.REGISTRO) {
            textTitulo.setText("Crea tu cuenta");
            textAuthSubtitulo.setText(R.string.app_name);
            buttonSubmit.setText("Registrarme");
            textToggleModo.setText("¿Ya tienes cuenta? Inicia sesión");
        } else if (modo == Modo.LOGIN) {
            textTitulo.setText("Inicia sesión");
            textAuthSubtitulo.setText(R.string.app_name);
            buttonSubmit.setText("Iniciar sesión");
            textToggleModo.setText("¿No tienes cuenta? Regístrate");
        } else {
            textTitulo.setText("Vincula tu cuenta");
            textAuthSubtitulo.setText("Ya tienes datos guardados en este teléfono. Crea una "
                    + "contraseña para respaldarlos en la nube y no perderlos.");
            buttonSubmit.setText("Vincular");
        }
    }

    private void mostrarTerminos() {
        new AlertDialog.Builder(this)
                .setTitle("Términos y condiciones")
                .setMessage(TERMINOS_TEXTO)
                .setPositiveButton("Cerrar", null)
                .show();
    }

    private void enviar() {
        if (modo != Modo.LOGIN && !checkTerminos.isChecked()) {
            Toast.makeText(this, "Debes aceptar los términos y condiciones para continuar", Toast.LENGTH_SHORT).show();
            return;
        }
        if (modo == Modo.REGISTRO) {
            registrarNuevo();
        } else if (modo == Modo.VINCULAR) {
            vincularExistente();
        } else {
            iniciarSesion();
        }
    }

    private void registrarNuevo() {
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

        int edadFinal = edad;
        buttonSubmit.setEnabled(false);
        authManager.registrar(email, password, new AuthManager.Callback() {
            @Override
            public void onExito(String uid) {
                Usuario usuario = new Usuario();
                usuario.setId(dataManager.newId("user"));
                usuario.setUsername(username);
                usuario.setEmail(email);
                usuario.setEdad(edadFinal);
                usuario.setFechaRegistro(LocalDate.now().toString());
                usuario.setFirebaseUid(uid);
                usuario.setAceptoTerminos(true);
                usuario.setFechaAceptacionTerminos(LocalDateTime.now().toString());

                dataStore.setUsuario(usuario);
                dataManager.save();
                PerfilSync.getInstance().pushCompleto(uid, usuario, dataStore.getHistorialFisico());

                continuarDespuesDeAuth();
            }

            @Override
            public void onError(String mensaje) {
                buttonSubmit.setEnabled(true);
                Toast.makeText(AuthActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void vincularExistente() {
        String password = contrasenaDe(editPassword);
        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario usuario = dataStore.getUsuario();
        buttonSubmit.setEnabled(false);
        authManager.registrar(usuario.getEmail(), password, new AuthManager.Callback() {
            @Override
            public void onExito(String uid) {
                usuario.setFirebaseUid(uid);
                usuario.setAceptoTerminos(true);
                usuario.setFechaAceptacionTerminos(LocalDateTime.now().toString());
                dataManager.save();
                PerfilSync.getInstance().pushCompleto(uid, usuario, dataStore.getHistorialFisico());

                continuarDespuesDeAuth();
            }

            @Override
            public void onError(String mensaje) {
                buttonSubmit.setEnabled(true);
                Toast.makeText(AuthActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void iniciarSesion() {
        String email = textoDe(editEmail);
        String password = contrasenaDe(editPassword);

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSubmit.setEnabled(false);
        authManager.iniciarSesion(email, password, new AuthManager.Callback() {
            @Override
            public void onExito(String uid) {
                if (dataStore.getUsuario() != null) {
                    continuarDespuesDeAuth();
                    return;
                }
                PerfilSync.getInstance().pullUnaVez(uid, new PerfilSync.CallbackPull() {
                    @Override
                    public void onListo(Usuario usuarioDescargado, List<RegistroFisico> historial) {
                        dataStore.setUsuario(usuarioDescargado);
                        dataStore.getHistorialFisico().addAll(historial);
                        dataManager.save();
                        continuarDespuesDeAuth();
                    }

                    @Override
                    public void onError(String mensaje) {
                        Usuario usuarioMinimo = new Usuario();
                        usuarioMinimo.setId(dataManager.newId("user"));
                        usuarioMinimo.setEmail(email);
                        usuarioMinimo.setUsername(email);
                        usuarioMinimo.setFechaRegistro(LocalDate.now().toString());
                        usuarioMinimo.setFirebaseUid(uid);
                        usuarioMinimo.setAceptoTerminos(true);
                        usuarioMinimo.setFechaAceptacionTerminos(LocalDateTime.now().toString());
                        dataStore.setUsuario(usuarioMinimo);
                        dataManager.save();
                        continuarDespuesDeAuth();
                    }
                });
            }

            @Override
            public void onError(String mensaje) {
                buttonSubmit.setEnabled(true);
                Toast.makeText(AuthActivity.this, mensaje, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void continuarDespuesDeAuth() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_SUGERIR_FISICO, dataStore.getHistorialFisico().isEmpty());
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
