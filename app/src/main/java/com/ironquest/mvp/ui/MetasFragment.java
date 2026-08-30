package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Marcador de posición reutilizable. Sirve de stub para las pestañas todavía sin construir;
 * cada una pasa su propio mensaje, así que no hace falta una clase desechable por pestaña.
 */
public class MetasFragment extends Fragment {

    private static final String ARG_MENSAJE = "arg_mensaje";

    public static MetasFragment crear(String mensaje) {
        MetasFragment fragment = new MetasFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MENSAJE, mensaje);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        TextView texto = new TextView(requireContext());
        texto.setGravity(Gravity.CENTER);
        texto.setTextSize(16f);
        texto.setText(getArguments() == null
                ? "Próximamente"
                : getArguments().getString(ARG_MENSAJE, "Próximamente"));
        return texto;
    }
}
