package com.example.receitas.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.receitas.R;
import com.example.receitas.model.RecipeCollection;
import java.util.ArrayList;
import java.util.List;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder> {

    private List<RecipeCollection> collections;
    private OnCollectionClickListener listener;
    private String currentUserId;

    public interface OnCollectionClickListener {
        void onCollectionClick(RecipeCollection collection);
        void onDeleteCollectionClick(RecipeCollection collection);
        void onShareCollectionClick(RecipeCollection collection);
    }

    public CollectionAdapter(List<RecipeCollection> collections, String currentUserId, OnCollectionClickListener listener) {
        this.collections = collections != null ? collections : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CollectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_collection, parent, false);
        return new CollectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionViewHolder holder, int position) {
        RecipeCollection collection = collections.get(position);
        holder.tvCollectionName.setText(collection.getName());

        // LÓGICA DE PERMISSÕES
        if (collection.getOwnerId() != null && collection.getOwnerId().equals(currentUserId)) {
            if (holder.btnDeleteCollection != null) holder.btnDeleteCollection.setVisibility(View.VISIBLE);
            if (holder.btnShareCollection != null) holder.btnShareCollection.setVisibility(View.VISIBLE);
        } else {
            if (holder.btnDeleteCollection != null) holder.btnDeleteCollection.setVisibility(View.GONE);
            if (holder.btnShareCollection != null) holder.btnShareCollection.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onCollectionClick(collection); });
        holder.btnViewCollection.setOnClickListener(v -> { if (listener != null) listener.onCollectionClick(collection); });

        if (holder.btnDeleteCollection != null) holder.btnDeleteCollection.setOnClickListener(v -> { if (listener != null) listener.onDeleteCollectionClick(collection); });
        if (holder.btnShareCollection != null) holder.btnShareCollection.setOnClickListener(v -> { if (listener != null) listener.onShareCollectionClick(collection); });
    }

    @Override
    public int getItemCount() { return collections.size(); }

    public void updateList(List<RecipeCollection> newList) {
        this.collections = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class CollectionViewHolder extends RecyclerView.ViewHolder {
        TextView tvCollectionName;
        View btnViewCollection, btnDeleteCollection, btnShareCollection;

        public CollectionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCollectionName = itemView.findViewById(R.id.tvCollectionName);
            btnViewCollection = itemView.findViewById(R.id.btnViewCollection);
            btnDeleteCollection = itemView.findViewById(R.id.btnDeleteCollection); // Confirma os IDs no teu item_collection.xml
            btnShareCollection = itemView.findViewById(R.id.btnShareCollection);   // Confirma os IDs no teu item_collection.xml
        }
    }
}