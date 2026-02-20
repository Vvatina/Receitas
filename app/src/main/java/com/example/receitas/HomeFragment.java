package com.example.receitas;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout; // Importante
import android.widget.RadioButton;  // Importante
import android.widget.RadioGroup;   // Importante
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receitas.adapter.RecipeAdapter;
import com.example.receitas.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch; // Importante

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private TextView tvEmptyList;
    private Spinner spinnerTypeFilter;

    private FirebaseFirestore db;
    private String currentUserId;

    private List<Recipe> allRecipesList = new ArrayList<>();
    private final String[] types = {"Filtrar por tipos", "Todos", "Prato Principal", "Sobremesa", "Entrada", "Bebida"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializa Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Inicializa Componentes Visuais
        recyclerView = view.findViewById(R.id.recyclerViewRecipes);
        tvEmptyList = view.findViewById(R.id.tvEmptyList);
        spinnerTypeFilter = view.findViewById(R.id.spinnerTypeFilter);
        ImageButton btnLogout = view.findViewById(R.id.btnLogout);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupSpinner();

        // Botão Adicionar
        view.findViewById(R.id.btnAddRecipe).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddRecipeActivity.class));
        });

        // Botão Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyRecipes();
    }

    private void loadMyRecipes() {
        // FILTRO: Apenas receitas onde ownerId sou eu
        if (currentUserId == null) return;

        db.collection("recipes")
                .whereEqualTo("ownerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allRecipesList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null) {
                            r.setFirestoreId(doc.getId());
                            allRecipesList.add(r);
                        }
                    }
                    // Aplica filtro do spinner
                    if (spinnerTypeFilter != null && spinnerTypeFilter.getSelectedItem() != null) {
                        applyFilter(spinnerTypeFilter.getSelectedItem().toString());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao carregar.", Toast.LENGTH_SHORT).show());
    }

    private void applyFilter(String type) {
        List<Recipe> filteredList = new ArrayList<>();
        if (type == null || type.equals("Filtrar por tipos") || type.equals("Todos")) {
            filteredList.addAll(allRecipesList);
        } else {
            for (Recipe r : allRecipesList) {
                if (r.getType() != null && r.getType().equalsIgnoreCase(type)) {
                    filteredList.add(r);
                }
            }
        }

        if (filteredList.isEmpty()) {
            tvEmptyList.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyList.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            if (adapter == null) {
                adapter = new RecipeAdapter(filteredList, this);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateList(filteredList);
            }
        }
    }

    private void setupSpinner() {
        // Adapter do Spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, types) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                // Fonte Tangerine
                Typeface tangerine = ResourcesCompat.getFont(requireContext(), R.font.tangerine_regular);
                tv.setTypeface(tangerine, Typeface.NORMAL);
                tv.setTextSize(25f); // tamanho do texto no dropdown
                tv.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return tv;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                Typeface tangerine = ResourcesCompat.getFont(requireContext(), R.font.tangerine_regular);
                tv.setTypeface(tangerine, Typeface.NORMAL);
                tv.setTextSize(25f); // tamanho do texto selecionado
                tv.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return tv;
            }
        };

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTypeFilter.setAdapter(spinnerAdapter);

        // Listener do Spinner
        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter(types[position]); // aplica filtro
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }



    // --- IMPLEMENTAÇÃO DA INTERFACE DO ADAPTER ---

    @Override
    public void onViewClick(Recipe recipe) {
        Intent intent = new Intent(getContext(), ViewRecipeActivity.class);
        intent.putExtra("firestore_id", recipe.getFirestoreId());
        intent.putExtra("recipe_name", recipe.getName());
        startActivity(intent);
    }

    @Override
    public void onEditClick(Recipe recipe) {
        Intent intent = new Intent(getContext(), AddRecipeActivity.class);
        intent.putExtra("firestore_id", recipe.getFirestoreId());
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Recipe recipe) {
        new AlertDialog.Builder(getContext())
                .setTitle("Excluir")
                .setMessage("Apagar esta receita?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    db.collection("recipes").document(recipe.getFirestoreId()).delete()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(getContext(), "Apagada!", Toast.LENGTH_SHORT).show();
                                loadMyRecipes();
                            });
                })
                .setNegativeButton("Não", null)
                .show();
    }

    // --- LÓGICA DE PARTILHA ATUALIZADA ---

    @Override
    public void onShareClick(Recipe recipe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Partilhar: " + recipe.getName());

        // 1. Criar Layout Programaticamente
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // 2. Campo de Email
        final EditText inputEmail = new EditText(getContext());
        inputEmail.setHint("Email do destinatário");
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(inputEmail);

        // 3. Texto de Permissão
        TextView lblPermissao = new TextView(getContext());
        lblPermissao.setText("\nNível de Permissão:");
        lblPermissao.setPadding(0, 20, 0, 10);
        layout.addView(lblPermissao);

        // 4. Radio Group (Visualizar vs Editar)

        final RadioGroup radioGroup = new RadioGroup(getContext());

        final RadioButton rbViewer = new RadioButton(getContext());
        rbViewer.setId(View.generateViewId()); // <--- ADICIONE ISTO (Gera ID único)
        rbViewer.setText("Apenas Visualizar");
        rbViewer.setChecked(true);

        final RadioButton rbEditor = new RadioButton(getContext());
        rbEditor.setId(View.generateViewId()); // <--- ADICIONE ISTO (Gera ID único)
        rbEditor.setText("Pode Editar");

        radioGroup.addView(rbViewer);
        radioGroup.addView(rbEditor);
        layout.addView(radioGroup);

        builder.setView(layout);

        // 5. Ação do Botão Enviar
        builder.setPositiveButton("Enviar", (dialog, which) -> {
            String email = inputEmail.getText().toString().trim();
            boolean isEditor = rbEditor.isChecked(); // Verifica qual está marcado

            if (!email.isEmpty()) {
                shareRecipe(email, recipe, isEditor);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void shareRecipe(String email, Recipe recipe, boolean isEditor) {
        // Busca o utilizador pelo email
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String friendId = querySnapshot.getDocuments().get(0).getId();

                        // --- GRAVAÇÃO EM LOTE (BATCH) ---
                        // Garante que o ID vai para a lista sharedWith E para o mapa permissions ao mesmo tempo
                        WriteBatch batch = db.batch();
                        var docRef = db.collection("recipes").document(recipe.getFirestoreId());

                        // 1. Adiciona à lista geral (sharedWith)
                        batch.update(docRef, "sharedWith", FieldValue.arrayUnion(friendId));

                        // 2. Atualiza o mapa de permissões (permissions.ID_DO_AMIGO = true/false)
                        batch.update(docRef, "permissions." + friendId, isEditor);

                        batch.commit()
                                .addOnSuccessListener(v -> {
                                    String msg = isEditor ? "Partilhado como Editor!" : "Partilhado como Leitor!";
                                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao salvar permissões.", Toast.LENGTH_SHORT).show());

                    } else {
                        Toast.makeText(getContext(), "E-mail não encontrado.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao buscar utilizador.", Toast.LENGTH_SHORT).show());
    }
}