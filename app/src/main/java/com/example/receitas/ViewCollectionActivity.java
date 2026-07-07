package com.example.receitas;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.res.ResourcesCompat;

import androidx.annotation.NonNull;
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

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, opcoes) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.BLACK); // Força a cor preta
                return view;
            }
        };

        builder.setAdapter(adapter, (dialog, which) -> {
            if (which == 0) {
                Intent intent = new Intent(this, AddRecipeActivity.class);
                intent.putExtra("collection_id", collectionId);
                startActivity(intent);
            } else if (which == 1) {
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

                    mostrarDialogEscolherReceita(receitasDisponiveis, nomesReceitas);
                });
    }

    /**
     * Mostra um dialog elegante com a lista de receitas disponíveis para adicionar ao livro
     */
    private void mostrarDialogEscolherReceita(List<Recipe> receitasDisponiveis, List<String> nomesReceitas) {
        Typeface tangerine = ResourcesCompat.getFont(this, R.font.tangerine_regular);

        // ===== CRIAR SUBTÍTULO COM O NOME DO LIVRO =====
        TextView tvSubtitulo = new TextView(this);
        String nomeExibicao = (collectionName != null) ? collectionName : "esta Coleção";
        tvSubtitulo.setText("Adicionar a \"" + nomeExibicao + "\"");
        tvSubtitulo.setTypeface(tangerine, Typeface.BOLD_ITALIC);
        tvSubtitulo.setTextSize(26f); // A fonte Tangerine costuma precisar de um tamanho maior
        tvSubtitulo.setTextColor(Color.parseColor("#6B5D4F"));
        tvSubtitulo.setPadding(0, 0, 0, 20);
        tvSubtitulo.setGravity(Gravity.CENTER);

        // ===== LISTA DE RECEITAS ESTILIZADA =====
        ListView listView = new ListView(this);

        // Criar um separador elegante entre as receitas
        android.graphics.drawable.GradientDrawable divider = new android.graphics.drawable.GradientDrawable();
        divider.setColor(Color.parseColor("#E0D6C8"));
        divider.setSize(0, 2);
        listView.setDivider(divider);
        listView.setDividerHeight(2);

        // Adaptador para estilizar cada item da lista
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, nomesReceitas) {
            @androidx.annotation.NonNull
            @Override
            public View getView(int position, View convertView, @androidx.annotation.NonNull ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTypeface(tangerine);
                tv.setTextSize(32f);
                tv.setTextColor(Color.parseColor("#2C2416"));
                tv.setPadding(40, 30, 40, 30);

                tv.setText("📖  " + getItem(position));

                return tv;
            }
        };
        listView.setAdapter(adapter);

        // ===== CONTAINER VERTICAL =====
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 20, 50, 10);
        container.addView(tvSubtitulo);
        container.addView(listView);

        // ===== CRIAR E CONFIGURAR O DIALOG =====
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Escolher Receita")
                .setView(container)
                .setNegativeButton("✕ Cancelar", null)
                .create();

        // Ação de clique nos itens da lista
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Recipe selecionada = receitasDisponiveis.get(position);
            vincularReceitaAoLivro(selecionada.getFirestoreId());
            dialog.dismiss(); // Fechar o dialog após escolher
        });

        // ===== APLICAR FUNDO PERSONALIZADO (como no teu código) =====
        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable dialogBg = new android.graphics.drawable.GradientDrawable();
            dialogBg.setColor(Color.parseColor("#FDF8F0"));
            dialogBg.setCornerRadius(30f);
            dialogBg.setStroke(4, Color.parseColor("#D4C4A8"));
            dialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        dialog.show();

        // ===== ESTILIZAR TÍTULO E BOTÃO =====
        int titleId = getResources().getIdentifier("alertTitle", "id", "android");
        TextView tvTitle = dialog.findViewById(titleId);
        if (tvTitle != null) {
            tvTitle.setTypeface(tangerine, Typeface.BOLD);
            tvTitle.setTextSize(42f);
            tvTitle.setTextColor(Color.parseColor("#8B7355"));
            tvTitle.setPadding(50, 40, 50, 10);
            tvTitle.setGravity(Gravity.CENTER);
        }

        Button btnNegativo = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (btnNegativo != null) {
            btnNegativo.setTypeface(tangerine, Typeface.BOLD);
            btnNegativo.setTextSize(22f);
            btnNegativo.setTextColor(Color.parseColor("#FF0000"));
            btnNegativo.setPadding(0, 10, 0, 30);
        }
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
        Typeface tangerine = ResourcesCompat.getFont(this, R.font.tangerine_regular);

        // Paleta de Cores
        int corFundo = Color.parseColor("#FDF8F0");
        int corBorda = Color.parseColor("#D4C4A8");
        int corTitulo = Color.parseColor("#5D4037");
        int corAviso = Color.parseColor("#A64444"); // Vermelho terracota para exclusão

        // 1. Container Principal
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(60, 50, 60, 50);
        root.setGravity(Gravity.CENTER);

        // 2. Título do Dialog
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Gerir Receita");
        tvTitle.setTypeface(tangerine, Typeface.BOLD);
        tvTitle.setTextSize(40f);
        tvTitle.setTextColor(corTitulo);
        tvTitle.setPadding(0, 0, 0, 10);
        root.addView(tvTitle);

        // 3. Subtítulo (Nome da Receita)
        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText(recipe.getName());
        tvSubtitle.setTypeface(tangerine, Typeface.ITALIC);
        tvSubtitle.setTextSize(26f);
        tvSubtitle.setTextColor(Color.parseColor("#8B7355"));
        tvSubtitle.setPadding(0, 0, 0, 40);
        root.addView(tvSubtitle);

        // 4. Estilizar os Botões de Ação (como TextViews clicáveis)
        TextView btnRemoveFromBook = criarBotaoMenu(
                "Remover apenas deste livro",
                Color.parseColor("#4E342E"), 30f, tangerine);

        TextView btnDeleteForever = criarBotaoMenu(
                "  Apagar receita para sempre",
                corAviso, 30f, tangerine);

        TextView btnCancel = criarBotaoMenu(
                "Voltar",
                Color.GRAY, 22f, tangerine);
        btnCancel.setPadding(0, 40, 0, 0);

        root.addView(btnRemoveFromBook);
        root.addView(btnDeleteForever);
        root.addView(btnCancel);

        // 5. Criar o AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setView(root);

        // Ações dos Botões
        btnRemoveFromBook.setOnClickListener(v -> {
            db.collection("recipes").document(recipe.getFirestoreId())
                    .update("collectionId", FieldValue.delete())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Removida do livro!", Toast.LENGTH_SHORT).show();
                        loadRecipesForThisCollection();
                        dialog.dismiss();
                    });
        });

        btnDeleteForever.setOnClickListener(v -> {
            db.collection("recipes").document(recipe.getFirestoreId()).delete()
                    .addOnSuccessListener(aVoid -> {
                        loadRecipesForThisCollection();
                        dialog.dismiss();
                    });
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 6. Fundo Arredondado e Borda
        if (dialog.getWindow() != null) {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(corFundo);
            bg.setCornerRadius(40f);
            bg.setStroke(4, corBorda);
            dialog.getWindow().setBackgroundDrawable(bg);
        }

        dialog.show();
    }

    // Função auxiliar para criar os itens do menu rapidamente
    private TextView criarBotaoMenu(String texto, int cor, float tamanho, Typeface tf) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(cor);
        tv.setTextSize(tamanho);
        tv.setTypeface(tf);
        tv.setPadding(0, 20, 0, 20);
        tv.setGravity(Gravity.CENTER);
        tv.setClickable(true);
        tv.setFocusable(true);

        // Adiciona um efeito visual simples de clique
        android.util.TypedValue outValue = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        tv.setBackgroundResource(outValue.resourceId);

        return tv;
    }

    @Override
    public void onShareClick(Recipe recipe) {
        Toast.makeText(this, "Para partilhares, usa o ecrã principal!", Toast.LENGTH_SHORT).show();
    }
}