package com.example.receitas.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receitas.R;
import com.example.receitas.model.Recipe;
import com.google.firebase.auth.FirebaseAuth; // IMPORTANTE

import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private OnRecipeClickListener listener;
    private String currentUserId; // Armazena o ID de quem está a usar a app

    // Interface para cliques
    public interface OnRecipeClickListener {
        void onViewClick(Recipe recipe);
        void onEditClick(Recipe recipe);
        void onDeleteClick(Recipe recipe);
        void onShareClick(Recipe recipe);
    }

    // Construtor
    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes != null ? recipes : new ArrayList<>();
        this.listener = listener;

        // Obtém o ID do utilizador atual para verificar permissões
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        // Preenche os dados
        holder.tvName.setText(recipe.getName());

        if (recipe.getType() != null) {
            holder.tvType.setText(recipe.getType()); // Removi o prefixo "Tipo:" para ficar mais limpo, opcional
        } else {
            holder.tvType.setText("");
        }

        // ==================================================================
        // LÓGICA DE PERMISSÕES (VISIBILIDADE DOS BOTÕES)
        // ==================================================================

        // 1. BOTÃO EDITAR
        // Usa o método inteligente do seu Modelo para decidir
        if (recipe.canEdit(currentUserId)) {
            holder.btnEdit.setVisibility(View.VISIBLE);
            // Configura o clique apenas se estiver visível
            holder.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(recipe);
            });
        } else {
            holder.btnEdit.setVisibility(View.GONE); // Esconde se for apenas leitor
        }

        // 2. BOTÕES DE DONO (APAGAR E PARTILHAR)
        // Geralmente, apenas o dono pode apagar a receita ou partilhar com mais pessoas
        boolean isOwner = recipe.getOwnerId() != null && recipe.getOwnerId().equals(currentUserId);

        if (isOwner) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnShare.setVisibility(View.VISIBLE);

            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(recipe);
            });

            holder.btnShare.setOnClickListener(v -> {
                if (listener != null) listener.onShareClick(recipe);
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
            holder.btnShare.setVisibility(View.GONE);
        }

        // 3. BOTÃO VISUALIZAR (Sempre visível para todos)
        holder.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onViewClick(recipe);
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public void updateList(List<Recipe> newRecipes) {
        this.recipes = newRecipes != null ? newRecipes : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ViewHolder
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvType;
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