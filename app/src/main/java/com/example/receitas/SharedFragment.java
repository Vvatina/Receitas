package com.example.receitas;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receitas.adapter.CollectionAdapter;
import com.example.receitas.adapter.RecipeAdapter;
import com.example.receitas.model.Recipe;
import com.example.receitas.model.RecipeCollection;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SharedFragment extends Fragment implements
        RecipeAdapter.OnRecipeClickListener,
        CollectionAdapter.OnCollectionClickListener {

    // Views Receitas
    private RecyclerView recyclerViewSharedRecipes;
    private RecipeAdapter recipeAdapter;
    private TextView tvEmptySharedRecipes;
    private LinearLayout layoutSharedRecipes;

    // Views Coleções
    private RecyclerView recyclerViewSharedCollections;
    private CollectionAdapter collectionAdapter;
    private TextView tvEmptySharedCollections;
    private LinearLayout layoutSharedCollections;

    private TabLayout tabLayoutShared;

    private FirebaseFirestore db;
    private String currentUserId;

    private List<Recipe> sharedRecipesList = new ArrayList<>();
    private List<RecipeCollection> sharedCollectionsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shared, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Inicializar Views
        tabLayoutShared = view.findViewById(R.id.tabLayoutShared);

        layoutSharedRecipes = view.findViewById(R.id.layoutSharedRecipes);
        recyclerViewSharedRecipes = view.findViewById(R.id.recyclerViewSharedRecipes);
        tvEmptySharedRecipes = view.findViewById(R.id.tvEmptySharedRecipes);

        layoutSharedCollections = view.findViewById(R.id.layoutSharedCollections);
        recyclerViewSharedCollections = view.findViewById(R.id.recyclerViewSharedCollections);
        tvEmptySharedCollections = view.findViewById(R.id.tvEmptySharedCollections);

        // 🔥 ESTILIZAR OS TEXTOS DE LISTAS VAZIAS (Fonte Tangerine)
        Typeface tangerine = ResourcesCompat.getFont(requireContext(), R.font.tangerine_regular);

        // Texto para Receitas Partilhadas vazias
        tvEmptySharedRecipes.setText("🍳\nAinda não partilharam receitas contigo!");
        tvEmptySharedRecipes.setTypeface(tangerine);
        tvEmptySharedRecipes.setTextSize(30f);
        tvEmptySharedRecipes.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvEmptySharedRecipes.setTextColor(Color.parseColor("#333333"));

        // Texto para Coleções Partilhadas vazias
        tvEmptySharedCollections.setText("📚\nAinda não partilharam livros contigo!");
        tvEmptySharedCollections.setTypeface(tangerine);
        tvEmptySharedCollections.setTextSize(30f);
        tvEmptySharedCollections.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvEmptySharedCollections.setTextColor(Color.parseColor("#333333"));

        // Setup RecyclerViews
        recyclerViewSharedRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSharedCollections.setLayoutManager(new LinearLayoutManager(getContext()));

        setupTabs();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSharedRecipes();
        loadSharedCollections();
    }

    private void setupTabs() {
        tabLayoutShared.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    // Aba 0: Receitas
                    layoutSharedRecipes.setVisibility(View.VISIBLE);
                    layoutSharedCollections.setVisibility(View.GONE);

                    // 🛑 Ordem de expulsão: Esconde o texto da aba de coleções à força!
                    tvEmptySharedCollections.setVisibility(View.GONE);

                    boolean empty = sharedRecipesList.isEmpty();
                    tvEmptySharedRecipes.setVisibility(empty ? View.VISIBLE : View.GONE);
                    recyclerViewSharedRecipes.setVisibility(empty ? View.GONE : View.VISIBLE);

                } else {
                    // Aba 1: Coleções
                    layoutSharedRecipes.setVisibility(View.GONE);
                    layoutSharedCollections.setVisibility(View.VISIBLE);

                    // 🛑 Ordem de expulsão: Esconde o texto da aba de receitas à força!
                    tvEmptySharedRecipes.setVisibility(View.GONE);

                    boolean empty = sharedCollectionsList.isEmpty();
                    tvEmptySharedCollections.setVisibility(empty ? View.VISIBLE : View.GONE);
                    recyclerViewSharedCollections.setVisibility(empty ? View.GONE : View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadSharedRecipes() {
        if (currentUserId == null) return;

        db.collection("recipes")
                .whereArrayContains("sharedWith", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sharedRecipesList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null) {
                            r.setFirestoreId(doc.getId());
                            sharedRecipesList.add(r);
                        }
                    }

                    if (recipeAdapter == null) {
                        recipeAdapter = new RecipeAdapter(sharedRecipesList, currentUserId, this);
                        recyclerViewSharedRecipes.setAdapter(recipeAdapter);
                    } else {
                        recipeAdapter.updateList(sharedRecipesList);
                    }

                    // 🔥 O "RESTO DO CÓDIGO": Verifica se estamos na aba de receitas
                    if (tabLayoutShared != null && tabLayoutShared.getSelectedTabPosition() == 0) {
                        boolean empty = sharedRecipesList.isEmpty();
                        tvEmptySharedRecipes.setVisibility(empty ? View.VISIBLE : View.GONE);
                        recyclerViewSharedRecipes.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e("FIREBASE", "Erro a carregar receitas partilhadas: ", e));
    }

    private void loadSharedCollections() {
        if (currentUserId == null) return;

        db.collection("recipe_collections")
                .whereArrayContains("sharedWith", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sharedCollectionsList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        RecipeCollection c = doc.toObject(RecipeCollection.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            sharedCollectionsList.add(c);
                        }
                    }

                    if (collectionAdapter == null) {
                        collectionAdapter = new CollectionAdapter(sharedCollectionsList, currentUserId, this);
                        recyclerViewSharedCollections.setAdapter(collectionAdapter);
                    } else {
                        collectionAdapter.updateList(sharedCollectionsList);
                    }

                    // Verifica se a aba de "Livros/Coleções" (posição 1) está selecionada
                    if (tabLayoutShared != null && tabLayoutShared.getSelectedTabPosition() == 1) {
                        boolean empty = sharedCollectionsList.isEmpty();
                        tvEmptySharedCollections.setVisibility(empty ? View.VISIBLE : View.GONE);
                        recyclerViewSharedCollections.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> Log.e("FIREBASE", "Erro a carregar coleções partilhadas: ", e));
    }

    // =========================================================================
    // CLIQUES EM RECEITAS
    // =========================================================================
    @Override
    public void onViewClick(Recipe recipe) {
        Intent intent = new Intent(getContext(), ViewRecipeActivity.class);
        intent.putExtra("firestore_id", recipe.getFirestoreId());
        intent.putExtra("recipe_name", recipe.getName());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Recipe recipe) {
        Toast.makeText(getContext(), "Apenas o dono pode editar esta receita.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(Recipe recipe) {
        Toast.makeText(getContext(), "Não podes apagar uma receita partilhada contigo.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShareClick(Recipe recipe) {
        Toast.makeText(getContext(), "Apenas o dono pode partilhar esta receita.", Toast.LENGTH_SHORT).show();
    }

    // =========================================================================
    // CLIQUES EM COLEÇÕES
    // =========================================================================
    @Override
    public void onCollectionClick(RecipeCollection collection) {
        Intent intent = new Intent(getActivity(), ViewCollectionActivity.class);
        intent.putExtra("collection_id", collection.getId());
        intent.putExtra("collection_name", collection.getName());
        startActivity(intent);
    }

    @Override
    public void onDeleteCollectionClick(RecipeCollection collection) {
        Toast.makeText(getContext(), "Não podes apagar um livro partilhado contigo.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShareCollectionClick(RecipeCollection collection) {
        Toast.makeText(getContext(), "Apenas o dono pode partilhar este livro.", Toast.LENGTH_SHORT).show();
    }
}