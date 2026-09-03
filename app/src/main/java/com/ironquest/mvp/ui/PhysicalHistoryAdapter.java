package com.ironquest.mvp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.RegistroFisico;
import com.ironquest.mvp.util.PerfilFisicoUtil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PhysicalHistoryAdapter extends RecyclerView.Adapter<PhysicalHistoryAdapter.ViewHolder> {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US);

    private final List<RegistroFisico> registros;

    public PhysicalHistoryAdapter(List<RegistroFisico> registros) {
        this.registros = registros;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registro_fisico, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegistroFisico registro = registros.get(position);

        LocalDate fecha = LocalDate.parse(registro.getFecha());
        holder.fecha.setText(fecha.format(FORMATO_FECHA));

        holder.pesoAltura.setText(String.format(Locale.US, "Peso: %s · Altura: %s",
                formatearValor(registro.getPesoKg(), " kg"), formatearValor(registro.getAlturaCm(), " cm")));

        holder.imc.setText(String.format(Locale.US, "IMC: %.1f (%s)", registro.getImc(),
                PerfilFisicoUtil.clasificarImc(registro.getImc())));
        holder.imc.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                PerfilFisicoUtil.colorParaImc(registro.getImc())));

        holder.medidas.setText(String.format(Locale.US,
                "Pecho: %s · Cintura: %s · Cadera: %s\nBrazo: %s · Piernas: %s · %s",
                formatearValor(registro.getPechoCm(), " cm"), formatearValor(registro.getCinturaCm(), " cm"),
                formatearValor(registro.getCaderaCm(), " cm"), formatearValor(registro.getBrazoCm(), " cm"),
                formatearValor(registro.getPiernaCm(), " cm"), registro.getTipoCuerpo()));
    }

    @Override
    public int getItemCount() {
        return registros.size();
    }

    private static String formatearValor(double valor, String unidad) {
        if (valor == Math.floor(valor)) {
            return (long) valor + unidad;
        }
        return String.format(Locale.US, "%.1f%s", valor, unidad);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView fecha;
        private final TextView pesoAltura;
        private final TextView imc;
        private final TextView medidas;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            fecha = itemView.findViewById(R.id.text_fecha_registro_fisico);
            pesoAltura = itemView.findViewById(R.id.text_peso_altura_registro);
            imc = itemView.findViewById(R.id.text_imc_registro);
            medidas = itemView.findViewById(R.id.text_medidas_registro);
        }
    }
}
