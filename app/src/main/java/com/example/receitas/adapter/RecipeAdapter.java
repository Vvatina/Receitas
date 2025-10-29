package com.example.receitas.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receitas.R;
import com.example.receitas.model.Recipe;

import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private List<Recipe> recipes;
    private OnRecipeClickListener listener;

    // Interface para cliques
    public interface OnRecipeClickListener {
        void onViewClick(Recipe recipe);
        void onEditClick(Recipe recipe);
        void onDeleteClick(Recipe recipe);
    }

    // Construtor
    public RecipeAdapter(List<Recipe> recipes, OnRecipeClickListener listener) {
        this.recipes = recipes;
        this.listener = listener;
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
        holder.tvType.setText(recipe.getType()); // exibe o tipo da receita

        // Clique no botão "Visualizar"
        holder.btnView.setOnClickListener(v -> listener.onViewClick(recipe));

        // Clique no botão "Editar"
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(recipe));

        // Clique no botão "Excluir"
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(recipe));
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    // Atualiza a lista
    public void updateList(List<Recipe> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }

    // ViewHolder
    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvType;
        TextView btnView, btnEdit, btnDelete;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvType = itemView.findViewById(R.id.tvType); // novo campo
            btnView = itemView.findViewById(R.id.btnView);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
