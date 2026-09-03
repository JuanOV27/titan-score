package com.ironquest.mvp.ui;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Base de las Activities que montan un {@link MaterialToolbar}. Centraliza el único orden que
 * funciona: {@code setSupportActionBar()} antes de {@code setTitle()}, porque al revés el
 * {@code android:label} del manifiesto pisa el título del layout.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /**
     * @param toolbarId      id del {@link MaterialToolbar} en el layout ya inflado
     * @param titulo         título a mostrar, o {@code null} si la Activity lo fija después
     *                       (por ejemplo, cuando depende de datos que aún no cargó)
     * @param conFlechaAtras si se muestra la flecha de "arriba" y esta Activity se cierra al
     *                       tocarla
     */
    protected void configurarToolbar(int toolbarId, String titulo, boolean conFlechaAtras) {
        MaterialToolbar toolbar = findViewById(toolbarId);
        setSupportActionBar(toolbar);
        if (titulo != null) {
            setTitle(titulo);
        }
        if (conFlechaAtras) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }
}
