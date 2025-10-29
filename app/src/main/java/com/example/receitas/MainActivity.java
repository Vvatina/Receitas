package com.example.receitas;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
    private Spinner spinnerTypeFilter;

    // Adiciona "Tipos" como hint inicial
    private final String[] types = {"Filtrar por tipos", "Todos", "Prato Principal", "Sobremesa", "Entrada", "Bebida"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Esconde ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inicializa banco e RecyclerView
        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerViewRecipes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmptyList = findViewById(R.id.tvEmptyList);
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter);

// Configura Spinner com "Tipos" como hint (não selecionável)
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                types
        ) {
            @Override
            public boolean isEnabled(int position) {
                // Desabilita o item "Tipos" (posição 0)
                return position != 0;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;

                // Deixa o "Tipos" com cor cinza, os demais pretos
                if (position == 0) {
                    textView.setTextColor(Color.parseColor("#888888"));
                } else {
                    textView.setTextColor(Color.BLACK);
                }
                return view;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                // 🔹 Aqui define a cor do item exibido no Spinner fechado
                if (position == 0) {
                    textView.setTextColor(Color.parseColor("#888888")); // cinza para hint
                } else {
                    textView.setTextColor(Color.BLACK); // preto para itens normais
                }

                return view;
            }
        };

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(spinnerAdapter);
        spinnerTypeFilter.setSelection(0); // Mostra "Tipos" como hint


        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Ignora se for o hint "Tipos"
                if (position == 0) return;

                String selectedType = parent.getItemAtPosition(position).toString();
                List<Recipe> filtered;

                if (selectedType.equals("Todos")) {
                    filtered = dbHelper.getAllRecipes();
                } else {
                    filtered = dbHelper.getRecipesByType(selectedType);
                }

                adapter.updateList(filtered);
                updateEmptyState(filtered);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nada
            }
        });

        // Botão para adicionar nova receita
        findViewById(R.id.btnAddRecipe).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });

        // Carrega todas as receitas inicialmente
        loadRecipes();
    }

    /** Carrega todas as receitas sem filtro */
    private void loadRecipes() {
        recipes = dbHelper.getAllRecipes();
        updateRecyclerView();
    }

    /** Atualiza RecyclerView e mensagem de lista vazia */
    private void updateRecyclerView() {
        updateEmptyState(recipes);

        if (adapter == null) {
            adapter = new RecipeAdapter(recipes, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(recipes);
        }
    }

    /** Mostra/oculta mensagem de lista vazia */
    private void updateEmptyState(List<Recipe> list) {
        if (list.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /** Recarrega a lista ao voltar da outra Activity */
    @Override
    protected void onResume() {
        super.onResume();
        // Atualiza de acordo com o tipo selecionado
        String selectedType = spinnerTypeFilter.getSelectedItem().toString();

        if (selectedType.equals("Todos")) {
            recipes = dbHelper.getAllRecipes();
        } else if (!selectedType.equals("Tipos")) {
            recipes = dbHelper.getRecipesByType(selectedType);
        } else {
            recipes = dbHelper.getAllRecipes(); // Default
        }

        updateRecyclerView();
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

        // Atualiza lista filtrada
        String selectedType = spinnerTypeFilter.getSelectedItem().toString();

        if (selectedType.equals("Todos")) {
            recipes = dbHelper.getAllRecipes();
        } else if (!selectedType.equals("Tipos")) {
            recipes = dbHelper.getRecipesByType(selectedType);
        } else {
            recipes = dbHelper.getAllRecipes();
        }

        updateRecyclerView();
    }
}
