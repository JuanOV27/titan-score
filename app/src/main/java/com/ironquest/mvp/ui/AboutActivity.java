package com.ironquest.mvp.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.ironquest.mvp.R;

/** Créditos del catálogo de ejercicios y atribución obligatoria de los medios de Gym visual. */
public class AboutActivity extends BaseActivity {

    private static final String URL_TERMINOS_GYM_VISUAL = "https://gymvisual.com/content/3-terms-and-conditions-of-use";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        configurarToolbar(R.id.toolbar, "Acerca de", true);

        TextView textVersion = findViewById(R.id.text_about_app_version);
        textVersion.setText(getString(R.string.app_name) + " " + obtenerVersionName());

        findViewById(R.id.text_about_terminos_link).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_TERMINOS_GYM_VISUAL))));
    }

    private String obtenerVersionName() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return "v" + info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }
}
