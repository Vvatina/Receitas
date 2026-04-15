package com.example.receitas.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.receitas.R;
import com.example.receitas.model.Recipe;
import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipeList;
    private OnRecipeClickListener listener;
    private String currentUserId;

    public interface OnRecipeClickListener {
        void onViewClick(Recipe recipe);
        void onEditClick(Recipe recipe);
        void onDeleteClick(Recipe recipe);
        void onShareClick(Recipe recipe);
    }

    public RecipeAdapter(List<Recipe> recipeList, String currentUserId, OnRecipeClickListener listener) {
        this.recipeList = recipeList != null ? recipeList : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Usa o teu ficheiro item_recipe.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);

        // Preencher os dados
        holder.tvName.setText(recipe.getName());
        if (recipe.getType() != null && !recipe.getType().isEmpty()) {
            holder.tvType.setText("Tipo: " + recipe.getType());
        } else {
            holder.tvType.setText("Tipo: Sem categoria");
        }

        // =========================================================
        // LÓGICA DE PERMISSÕES
        // =========================================================
        if (recipe.getOwnerId() != null && recipe.getOwnerId().equals(currentUserId)) {
            // É o dono: Mostra Excluir e Partilhar
            if (holder.btnDelete != null) holder.btnDelete.setVisibility(View.VISIBLE);
            if (holder.btnShare != null) holder.btnShare.setVisibility(View.VISIBLE);
        } else {
            // Não é o dono (partilhado): Esconde Excluir e Partilhar
            if (holder.btnDelete != null) holder.btnDelete.setVisibility(View.GONE);
            if (holder.btnShare != null) holder.btnShare.setVisibility(View.GONE);
        }
        // =========================================================

        // Configurar os Cliques
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onViewClick(recipe); });
        if (holder.btnView != null) holder.btnView.setOnClickListener(v -> { if (listener != null) listener.onViewClick(recipe); });
        if (holder.btnEdit != null) holder.btnEdit.setOnClickListener(v -> { if (listener != null) listener.onEditClick(recipe); });
        if (holder.btnDelete != null) holder.btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDeleteClick(recipe); });
        if (holder.btnShare != null) holder.btnShare.setOnClickListener(v -> { if (listener != null) listener.onShareClick(recipe); });
    }

    @Override
    public int getItemCount() { return recipeList.size(); }

    public void updateList(List<Recipe> newList) {
        this.recipeList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        // IDs mapeados exatamente como no teu XML
        TextView tvName, tvType;
        View btnView, btnEdit, btnDelete, btnShare;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvType = itemView.findViewById(R.id.tvType);
            btnView = itemView.findViewById(R.id.btnView);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
}