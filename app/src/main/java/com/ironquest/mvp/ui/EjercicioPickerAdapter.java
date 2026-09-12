package com.ironquest.mvp.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ironquest.mvp.R;
import com.ironquest.mvp.model.Ejercicio;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Fila con miniatura, nombre y equipo. Miniatura desde el .jpg estático — el .gif solo se usa en "ver técnica". */
final class EjercicioPickerAdapter extends RecyclerView.Adapter<EjercicioPickerAdapter.ViewHolder> {

    interface Listener {
        void onEjercicioClick(Ejercicio ejercicio);
    }

    private final Context context;
    private final Listener listener;
    private final LruCache<String, Bitmap> cacheMiniaturas;
    private List<Ejercicio> ejercicios = new ArrayList<>();

    EjercicioPickerAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        int maxKb = 3 * 1024;
        cacheMiniaturas = new LruCache<String, Bitmap>(maxKb) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    void submitLista(List<Ejercicio> nuevos) {
        this.ejercicios = nuevos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejercicio_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ejercicio ejercicio = ejercicios.get(position);
        holder.nombre.setText(ejercicio.getNombre());
        if (ejercicio.getEquipo() != null && !ejercicio.getEquipo().isEmpty()) {
            holder.equipo.setVisibility(View.VISIBLE);
            holder.equipo.setText(ejercicio.getEquipo());
        } else {
            holder.equipo.setVisibility(View.GONE);
        }
        cargarMiniatura(holder.miniatura, ejercicio.getImgAsset());
        holder.itemView.setOnClickListener(v -> listener.onEjercicioClick(ejercicio));
    }

    private void cargarMiniatura(ImageView imageView, String imgAsset) {
        if (imgAsset == null || imgAsset.isEmpty()) {
            imageView.setImageDrawable(null);
            return;
        }
        Bitmap cacheado = cacheMiniaturas.get(imgAsset);
        if (cacheado != null) {
            imageView.setImageBitmap(cacheado);
            return;
        }
        try (InputStream inputStream = context.getAssets().open(imgAsset)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                cacheMiniaturas.put(imgAsset, bitmap);
            }
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            imageView.setImageDrawable(null);
        }
    }

    @Override
    public int getItemCount() {
        return ejercicios.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView miniatura;
        private final TextView nombre;
        private final TextView equipo;

        private ViewHolder(@NonNull View itemView) {
            super(itemView);
            miniatura = itemView.findViewById(R.id.image_miniatura_ejercicio);
            nombre = itemView.findViewById(R.id.text_nombre_ejercicio_picker);
            equipo = itemView.findViewById(R.id.text_equipo_ejercicio_picker);
        }
    }
}
