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

    private int userId; // Id do usuário logado

    private final String[] types = {"Filtrar por tipos", "Todos", "Prato Principal", "Sobremesa", "Entrada", "Bebida"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        dbHelper = new DatabaseHelper(this);
        recyclerView = findViewById(R.id.recyclerViewRecipes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmptyList = findViewById(R.id.tvEmptyList);
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter);

        // Recebe userId do LoginActivity
        userId = getIntent().getIntExtra("USER_ID", -1);

        // Configura Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, types
        ) {
            @Override
            public boolean isEnabled(int position) { return position != 0; }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return view;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return view;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(spinnerAdapter);
        spinnerTypeFilter.setSelection(0);

        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) return;

                String selectedType = parent.getItemAtPosition(position).toString();
                List<Recipe> filtered;

                if (selectedType.equals("Todos")) {
                    filtered = dbHelper.getRecipesByUser(userId);
                } else {
                    filtered = dbHelper.getRecipesByType(selectedType);
                    // Filtra apenas do usuário
                    filtered.removeIf(r -> r.getUserId() != userId);
                }

                adapter.updateList(filtered);
                updateEmptyState(filtered);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.btnAddRecipe).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        loadRecipes();
    }

    private void loadRecipes() {
        recipes = dbHelper.getRecipesByUser(userId);
        updateRecyclerView();
    }

    private void updateRecyclerView() {
        updateEmptyState(recipes);

        if (adapter == null) {
            adapter = new RecipeAdapter(recipes, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(recipes);
        }
    }

    private void updateEmptyState(List<Recipe> list) {
        if (list.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String selectedType = spinnerTypeFilter.getSelectedItem().toString();

        if (selectedType.equals("Todos") || selectedType.equals("Tipos")) {
            recipes = dbHelper.getRecipesByUser(userId);
        } else {
            recipes = dbHelper.getRecipesByType(selectedType);
            recipes.removeIf(r -> r.getUserId() != userId);
        }

        updateRecyclerView();
    }

    @Override
    public void onViewClick(Recipe recipe) {
        Intent intent = new Intent(this, ViewRecipeActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Recipe recipe) {
        Intent intent = new Intent(this, AddRecipeActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        intent.putExtra("USER_ID", userId);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Recipe recipe) {
        dbHelper.deleteRecipe(recipe.getId());

        String selectedType = spinnerTypeFilter.getSelectedItem().toString();
        if (selectedType.equals("Todos") || selectedType.equals("Tipos")) {
            recipes = dbHelper.getRecipesByUser(userId);
        } else {
            recipes = dbHelper.getRecipesByType(selectedType);
            recipes.removeIf(r -> r.getUserId() != userId);
        }

        updateRecyclerView();
    }
}
