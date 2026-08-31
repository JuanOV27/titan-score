package com.ironquest.mvp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ironquest.mvp.R;
import com.ironquest.mvp.data.DataManager;
import com.ironquest.mvp.util.EstadisticasUtil;

/** Pestaña "Inicio": por ahora solo la racha real; avatar, XP y metas llegan en slices futuras. */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        DataManager dataManager = DataManager.getInstance(requireContext());
        int racha = EstadisticasUtil.calcularRachaDias(dataManager.getDataStore().sesiones);

        TextView textRacha = requireView().findViewById(R.id.text_racha_inicio);
        textRacha.setText(racha == 1 ? "1 día" : racha + " días");
    }
}
