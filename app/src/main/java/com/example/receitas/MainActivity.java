package com.example.receitas;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receitas.adapter.RecipeAdapter;
import com.example.receitas.database.DatabaseHelper;
import com.example.receitas.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;

    // FIREBASE
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    // SQLite (Apenas para migração)
    private DatabaseHelper dbHelper;

    private List<Recipe> allRecipesList = new ArrayList<>(); // Lista completa baixada
    private TextView tvEmptyList;
    private Spinner spinnerTypeFilter;

    private final String[] types = {"Filtrar por tipos", "Todos", "Prato Principal", "Sobremesa", "Entrada", "Bebida"};
    private final String ALL_TYPE = "Todos";
    private final String FILTER_PROMPT = "Filtrar por tipos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // INICIALIZA FIREBASE
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Se não tiver logado, volta pro Login
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class)); // Certifique-se de ter LoginActivity
            finish();
            return;
        }

        // Inicializa SQLite apenas para checar migração
        dbHelper = new DatabaseHelper(this);

        recyclerView = findViewById(R.id.recyclerViewRecipes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        tvEmptyList = findViewById(R.id.tvEmptyList);
        spinnerTypeFilter = findViewById(R.id.spinnerTypeFilter);

        setupSpinner();

        // 1. Tenta Migrar dados antigos (Só roda na primeira vez)
        checkAndMigrateLocalData();

        // 2. Carrega dados da Nuvem
        loadRecipesFromCloud();

        findViewById(R.id.btnAddRecipe).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            // Não precisamos mais passar USER_ID via intent, o FirebaseAuth já tem.
            startActivity(intent);
        });
    }

    // ==================================================================
    // 1. MIGRAÇÃO (SQLite -> Firebase)
    // ==================================================================
    private void checkAndMigrateLocalData() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isMigrated = prefs.getBoolean("IS_DATA_MIGRATED", false);

        if (!isMigrated) {
            List<Recipe> localRecipes = dbHelper.getAllRecipes();

            if (localRecipes.isEmpty()) {
                prefs.edit().putBoolean("IS_DATA_MIGRATED", true).apply();
                return;
            }

            Toast.makeText(this, "A sincronizar receitas antigas...", Toast.LENGTH_LONG).show();

            int totalToMigrate = localRecipes.size();
            final int[] migratedCount = {0};

            String myId = currentUser.getUid(); // ID do utilizador logado

            for (Recipe localRecipe : localRecipes) {
                // 1. Define o dono
                localRecipe.setOwnerId(myId);

                // A LINHA COM ERRO FOI REMOVIDA.
                // O DatabaseHelper já preencheu o sqliteId quando fez 'getAllRecipes'.
                // Se quisesse acessar, seria localRecipe.getSqliteId(), mas não é necessário mudar nada aqui.

                // --- APLICANDO A FEATURE DE PARTILHA (CRUCIAL) ---
                // Adiciona o dono à lista de quem pode ver
                List<String> shares = new ArrayList<>();
                shares.add(myId);
                localRecipe.setSharedWith(shares);

                // Adiciona permissão de edição para o dono
                Map<String, Boolean> perms = new HashMap<>();
                perms.put(myId, true);
                localRecipe.setPermissions(perms);
                // -------------------------------------------------

                // Envia para a nuvem
                db.collection("recipes")
                        .add(localRecipe)
                        .addOnSuccessListener(docRef -> {
                            migratedCount[0]++;
                            if (migratedCount[0] == totalToMigrate) {
                                prefs.edit().putBoolean("IS_DATA_MIGRATED", true).apply();
                                Toast.makeText(this, "Sincronização concluída!", Toast.LENGTH_SHORT).show();
                                loadRecipesFromCloud(); // Recarrega a lista
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Migracao", "Erro ao migrar receita: " + localRecipe.getName());
                        });
            }
        }
    }

    // ==================================================================
    // 2. LEITURA DA NUVEM
    // ==================================================================
    private void loadRecipesFromCloud() {
        // Busca receitas onde SOU O DONO
        db.collection("recipes")
                .whereEqualTo("ownerId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRecipesList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Recipe r = doc.toObject(Recipe.class);
                            if (r != null) {
                                r.setFirestoreId(doc.getId()); // Salva o ID do documento
                                allRecipesList.add(r);
                            }
                        }
                    }

                    // Aplica o filtro atual (ou mostra tudo)
                    applyFilter(spinnerTypeFilter.getSelectedItem().toString());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao carregar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firebase", "Erro loadRecipes", e);
                });
    }

    // ==================================================================
    // LÓGICA DE FILTRO (Agora feita na memória, não no banco)
    // ==================================================================
    private void applyFilter(String type) {
        List<Recipe> filteredList = new ArrayList<>();

        if (type.equals(FILTER_PROMPT) || type.equals(ALL_TYPE)) {
            filteredList.addAll(allRecipesList);
        } else {
            for (Recipe r : allRecipesList) {
                if (r.getType() != null && r.getType().equalsIgnoreCase(type)) {
                    filteredList.add(r);
                }
            }
        }

        updateRecyclerView(filteredList);
    }

    private void updateRecyclerView(List<Recipe> list) {
        updateEmptyState(list);
        if (adapter == null) {
            adapter = new RecipeAdapter(list, this);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateList(list);
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

    // ==================================================================
    // INTERAÇÕES
    // ==================================================================
    @Override
    protected void onResume() {
        super.onResume();
        loadRecipesFromCloud(); // Recarrega sempre que voltar à tela
    }

    @Override
    public void onViewClick(Recipe recipe) {
        Intent intent = new Intent(this, ViewRecipeActivity.class);
        // Passamos o ID do Firestore (String) e não mais o ID int
        intent.putExtra("firestore_id", recipe.getFirestoreId());
        intent.putExtra("recipe_name", recipe.getName()); // Backup visual
        startActivity(intent);
    }

    @Override
    public void onEditClick(Recipe recipe) {
        Intent intent = new Intent(this, AddRecipeActivity.class);
        intent.putExtra("firestore_id", recipe.getFirestoreId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Recipe recipe) {
        if (recipe.getFirestoreId() == null) return;

        db.collection("recipes").document(recipe.getFirestoreId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Receita excluída!", Toast.LENGTH_SHORT).show();
                    loadRecipesFromCloud(); // Atualiza lista
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao excluir.", Toast.LENGTH_SHORT).show();
                });
    }

    // Configuração visual do Spinner (igual ao anterior)
    private void setupSpinner() {
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
                applyFilter(types[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
}