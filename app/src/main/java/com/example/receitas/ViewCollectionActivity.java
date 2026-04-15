package com.example.receitas;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receitas.adapter.RecipeAdapter;
import com.example.receitas.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ViewCollectionActivity extends AppCompatActivity implements RecipeAdapter.OnRecipeClickListener {

    private String collectionId;
    private String collectionName;
    private String currentUserId;

    private TextView tvCollectionTitle, tvEmptyCollection;
    private RecyclerView recyclerView;
    private RecipeAdapter recipeAdapter;
    private FirebaseFirestore db;
    private List<Recipe> collectionRecipes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_collection);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        collectionId = getIntent().getStringExtra("collection_id");
        collectionName = getIntent().getStringExtra("collection_name");
// Adiciona isto logo a seguir a receber os extras do Intent
        Log.d("DEBUG_COLLECTION", "ID Recebido: " + collectionId);

        if (collectionId == null) {
            Toast.makeText(this, "Erro: ID da coleção é nulo!", Toast.LENGTH_LONG).show();
        }
        tvCollectionTitle = findViewById(R.id.tvCollectionTitle);
        tvEmptyCollection = findViewById(R.id.tvEmptyCollection);
        recyclerView = findViewById(R.id.recyclerViewCollectionRecipes);
        ImageButton btnBack = findViewById(R.id.btnBack);

        if (collectionName != null) {
            tvCollectionTitle.setText(collectionName);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recipeAdapter = new RecipeAdapter(collectionRecipes, currentUserId, this);
        recyclerView.setAdapter(recipeAdapter);

        btnBack.setOnClickListener(v -> finish());

        // CLIQUE NO BOTÃO +
        findViewById(R.id.btnAddRecipeToCollection).setOnClickListener(v -> mostrarOpcoesAdicionar());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipesForThisCollection();
    }

    private void loadRecipesForThisCollection() {
        if (collectionId == null) return;

        // Mudamos a lógica: Pedimos todas as receitas desta coleção.
        // O Firebase filtrará automaticamente o que o utilizador tem permissão para ver.
        db.collection("recipes")
                .whereEqualTo("collectionId", collectionId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    collectionRecipes.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null) {
                            r.setFirestoreId(doc.getId());
                            collectionRecipes.add(r);
                        }
                    }
                    recipeAdapter.updateList(collectionRecipes);

                    if (collectionRecipes.isEmpty()) {
                        tvEmptyCollection.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        tvEmptyCollection.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewCollection", "Erro ao carregar receitas: ", e);
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // =========================================================================
    // LÓGICA DO BOTÃO + (CRIAR OU ADICIONAR EXISTENTE)
    // =========================================================================
    private void mostrarOpcoesAdicionar() {
        int corTitulo = Color.parseColor("#4E342E");
        int corFundoPapel = Color.parseColor("#FCF9F2");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        TextView title = new TextView(this);
        title.setText("Adicionar a este Livro");
        title.setPadding(50, 40, 50, 20);
        title.setTextSize(22);
        title.setTextColor(corTitulo);
        title.setTypeface(null, Typeface.BOLD);
        builder.setCustomTitle(title);

        String[] opcoes = {"📝 Criar Nova Receita", "📖 Adicionar Receita Existente"};

        builder.setItems(opcoes, (dialog, which) -> {
            if (which == 0) {
                // Vai criar uma nova receita e passa-lhe o ID do livro!
                Intent intent = new Intent(this, AddRecipeActivity.class);
                intent.putExtra("collection_id", collectionId);
                startActivity(intent);
            } else if (which == 1) {
                // Vai buscar receitas já existentes
                mostrarListaReceitasExistentes();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable bgDrawable = new android.graphics.drawable.GradientDrawable();
            bgDrawable.setColor(corFundoPapel);
            bgDrawable.setCornerRadius(30f);
            dialog.getWindow().setBackgroundDrawable(bgDrawable);
        }
    }

    private void mostrarListaReceitasExistentes() {
        if (currentUserId == null) return;

        db.collection("recipes")
                .whereEqualTo("ownerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> receitasDisponiveis = new ArrayList<>();
                    List<String> nomesReceitas = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null) {
                            r.setFirestoreId(doc.getId());
                            // Só permite adicionar se ainda não estiver num livro
                            if (r.getCollectionId() == null || !r.getCollectionId().equals(collectionId)) {
                                receitasDisponiveis.add(r);
                                nomesReceitas.add(r.getName());
                            }
                        }
                    }

                    if (receitasDisponiveis.isEmpty()) {
                        Toast.makeText(this, "Não tens outras receitas disponíveis!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] arrayNomes = nomesReceitas.toArray(new String[0]);
                    new AlertDialog.Builder(this)
                            .setTitle("Escolhe uma Receita")
                            .setItems(arrayNomes, (dialog, which) -> {
                                Recipe selecionada = receitasDisponiveis.get(which);
                                vincularReceitaAoLivro(selecionada.getFirestoreId());
                            }).show();
                });
    }

    private void vincularReceitaAoLivro(String recipeId) {
        db.collection("recipes").document(recipeId)
                .update("collectionId", collectionId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Receita adicionada ao livro!", Toast.LENGTH_SHORT).show();
                    loadRecipesForThisCollection();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao adicionar.", Toast.LENGTH_SHORT).show());
    }

    // =========================================================================
    // CLIQUES NAS RECEITAS DA LISTA
    // =========================================================================
    @Override
    public void onViewClick(Recipe recipe) {
        Intent intent = new Intent(this, ViewRecipeActivity.class);
        intent.putExtra("firestore_id", recipe.getFirestoreId());
        intent.putExtra("recipe_name", recipe.getName());
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
        // PERGUNTA: Remover do Livro ou Apagar de vez?
        String[] opcoes = {"Remover deste Livro", "Apagar receita para sempre"};

        new AlertDialog.Builder(this)
                .setTitle("O que pretendes fazer?")
                .setItems(opcoes, (dialog, which) -> {
                    if (which == 0) {
                        // Apenas retira do livro (volta a ser receita solta)
                        db.collection("recipes").document(recipe.getFirestoreId())
                                .update("collectionId", FieldValue.delete())
                                .addOnSuccessListener(v -> {
                                    Toast.makeText(this, "Removida do livro!", Toast.LENGTH_SHORT).show();
                                    loadRecipesForThisCollection();
                                });
                    } else if (which == 1) {
                        // Apaga do Firebase
                        db.collection("recipes").document(recipe.getFirestoreId()).delete()
                                .addOnSuccessListener(v -> loadRecipesForThisCollection());
                    }
                }).show();
    }

    @Override
    public void onShareClick(Recipe recipe) {
        Toast.makeText(this, "Para partilhares, usa o ecrã principal!", Toast.LENGTH_SHORT).show();
    }
}