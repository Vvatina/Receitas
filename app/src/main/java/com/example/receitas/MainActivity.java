package com.example.receitas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receitas.adapter.RecipeAdapter;
import com.example.receitas.database.DatabaseHelper;
import com.example.receitas.model.Recipe;

import java.util.List;

public class MainActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<Recipe> recipes;

    private TextView tvEmptyList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().hide();

        // Inicializa banco e RecyclerView
        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerViewRecipes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmptyList = findViewById(R.id.tvEmptyList);


        // Carrega receitas do banco
        loadRecipes();

        // Botão para adicionar nova receita
        findViewById(R.id.btnAddRecipe).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });
    }

    /** Carrega todas as receitas e configura o adapter */
    private void loadRecipes() {
        recipes = dbHelper.getAllRecipes();

        if (recipes.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }

        if (adapter == null) {
            adapter = new RecipeAdapter(recipes, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(recipes);
        }
    }


    /** Recarrega a lista ao voltar da outra Activity */
    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }

    /** Clique em "Visualizar" */
    @Override
    public void onViewClick(Recipe recipe) {
        Intent intent = new Intent(this, ViewRecipeActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        startActivity(intent);
    }

    /** Clique em "Editar" */
    @Override
    public void onEditClick(Recipe recipe) {
        Intent intent = new Intent(this, AddRecipeActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        startActivity(intent);
    }

    /** Clique em "Excluir" */
    @Override
    public void onDeleteClick(Recipe recipe) {
        dbHelper.deleteRecipe(recipe.getId());
        loadRecipes();
    }

}
