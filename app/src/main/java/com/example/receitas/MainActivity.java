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
    private final String FILTER_PROMPT = types[0]; // "Filtrar por tipos"
    private final String ALL_TYPE = types[1]; // "Todos"

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
            public boolean isEnabled(int position) {
                // Permite seleção de todos exceto o prompt
                return position != 0;
            }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                // Deixa o prompt cinza
                textView.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return view;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                // Deixa o prompt cinza
                textView.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return view;
            }
        };
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(spinnerAdapter);
        // Garante que o item 0 (prompt) é selecionado inicialmente
        spinnerTypeFilter.setSelection(0);

        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Se selecionar o item 0 (prompt), carrega todas as receitas do usuário
                if (position == 0) {
                    loadRecipes();
                    return;
                }

                String selectedType = parent.getItemAtPosition(position).toString();
                List<Recipe> filtered;

                if (selectedType.equals(ALL_TYPE)) {
                    // Carrega todas as receitas do usuário
                    filtered = dbHelper.getRecipesByUser(userId);
                } else {
                    // *** CORREÇÃO: Utiliza o novo método getRecipesByTypeAndUser ***
                    // Busca receitas filtradas pelo tipo E pelo ID do usuário
                    filtered = dbHelper.getRecipesByTypeAndUser(selectedType, userId);
                }

                adapter.updateList(filtered);
                updateEmptyState(filtered);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Não é necessário implementar, mas garante que se nada for selecionado,
                // a lista completa do usuário é carregada.
                loadRecipes();
            }
        });

        findViewById(R.id.btnAddRecipe).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        loadRecipes();
    }

    private void loadRecipes() {
        // Carrega todas as receitas pertencentes ao usuário logado
        recipes = dbHelper.getRecipesByUser(userId);
        updateRecyclerView();
    }

    // ... (updateRecyclerView e updateEmptyState permanecem iguais)

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

        // Recarrega a lista baseada no filtro que estava ativo antes de pausar
        if (selectedType.equals(ALL_TYPE) || selectedType.equals(FILTER_PROMPT)) {
            recipes = dbHelper.getRecipesByUser(userId);
        } else {
            // *** CORREÇÃO: Usa o método otimizado ***
            recipes = dbHelper.getRecipesByTypeAndUser(selectedType, userId);
        }

        updateRecyclerView();
    }

    @Override
    public void onViewClick(Recipe recipe) {
        Intent intent = new Intent(this, ViewRecipeActivity.class);
        intent.putExtra("recipe_id", recipe.getId());

        // CORREÇÃO ESSENCIAL: O USER_ID deve ser passado para ViewRecipeActivity
        // para que ela possa verificar a propriedade da receita.
        intent.putExtra("USER_ID", userId);

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

        // Recarrega a lista mantendo o filtro ativo
        if (selectedType.equals(ALL_TYPE) || selectedType.equals(FILTER_PROMPT)) {
            recipes = dbHelper.getRecipesByUser(userId);
        } else {
            // *** CORREÇÃO: Usa o método otimizado ***
            recipes = dbHelper.getRecipesByTypeAndUser(selectedType, userId);
        }

        updateRecyclerView();
    }
}