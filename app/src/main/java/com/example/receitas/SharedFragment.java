package com.example.receitas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receitas.adapter.RecipeAdapter;
import com.example.receitas.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SharedFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyShared;
    private RecipeAdapter adapter;
    private List<Recipe> sharedList = new ArrayList<>();

    private FirebaseFirestore db;
    private String myId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shared, container, false);

        db = FirebaseFirestore.getInstance();
        myId = FirebaseAuth.getInstance().getUid();

        recyclerView = view.findViewById(R.id.recyclerViewShared);
        tvEmptyShared = view.findViewById(R.id.tvEmptyShared);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSharedRecipes();
    }

    private void loadSharedRecipes() {
        // Busca onde "sharedWith" tem o meu ID
        db.collection("recipes")
                .whereArrayContains("sharedWith", myId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    sharedList.clear();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null) {
                            r.setFirestoreId(doc.getId());
                            // Filtro visual: Garante que não mostro as minhas próprias receitas aqui
                            // (caso eu tenha me adicionado no sharedWith por engano na migração)
                            if (!r.getOwnerId().equals(myId)) {
                                sharedList.add(r);
                            }
                        }
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao carregar.", Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        if (sharedList.isEmpty()) {
            tvEmptyShared.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyShared.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new RecipeAdapter(sharedList, this);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(sharedList);
            }
        }
    }

    // --- Ações (Interfaces) ---

    @Override
    public void onViewClick(Recipe recipe) {
        Intent intent = new Intent(getContext(), ViewRecipeActivity.class);
        intent.putExtra("firestore_id", recipe.getFirestoreId());
        intent.putExtra("recipe_name", recipe.getName());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Recipe recipe) {
        // Verifica se tem permissão (Exemplo simples: se estiver no mapa de permissions)
        // Se não tiver lógica de permissão complexa, bloqueie ou permita tudo.
        // Aqui vou permitir editar:
        Intent intent = new Intent(getContext(), AddRecipeActivity.class);
        intent.putExtra("firestore_id", recipe.getFirestoreId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Recipe recipe) {
        Toast.makeText(getContext(), "Você não pode excluir receitas de outros.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onShareClick(Recipe recipe) {
        Toast.makeText(getContext(), "Apenas o dono pode partilhar.", Toast.LENGTH_SHORT).show();
    }
}