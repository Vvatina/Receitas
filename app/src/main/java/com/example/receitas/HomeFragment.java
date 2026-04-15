package com.example.receitas;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.receitas.adapter.*;
import com.example.receitas.model.*;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class HomeFragment extends Fragment implements
        RecipeAdapter.OnRecipeClickListener,
        CollectionAdapter.OnCollectionClickListener {

    private RecyclerView recyclerViewRecipes, recyclerViewCollections;
    private RecipeAdapter recipeAdapter;
    private CollectionAdapter collectionAdapter;

    private TextView tvEmptyList, tvEmptyCollections, tvProfileName, tvProfileBio;
    private Spinner spinnerTypeFilter;
    private LinearLayout layoutRecipes, layoutCollections;
    private TabLayout tabLayout;
    private ImageView imgProfilePhoto;

    private FirebaseFirestore db;
    private String currentUserId;

    private List<Recipe> allRecipesList = new ArrayList<>();
    private List<RecipeCollection> allCollectionsList = new ArrayList<>();

    private final String[] types = {"Filtrar por tipos", "Todos", "Prato Principal", "Sobremesa", "Entrada", "Bebida"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Views
        tabLayout = view.findViewById(R.id.tabLayout);
        layoutRecipes = view.findViewById(R.id.layoutRecipes);
        recyclerViewRecipes = view.findViewById(R.id.recyclerViewRecipes);
        tvEmptyList = view.findViewById(R.id.tvEmptyList);
        spinnerTypeFilter = view.findViewById(R.id.spinnerTypeFilter);

        layoutCollections = view.findViewById(R.id.layoutCollections);
        recyclerViewCollections = view.findViewById(R.id.recyclerViewCollections);
        tvEmptyCollections = view.findViewById(R.id.tvEmptyCollections);

        imgProfilePhoto = view.findViewById(R.id.imgProfilePhoto);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileBio = view.findViewById(R.id.tvProfileBio);

        recyclerViewRecipes.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCollections.setLayoutManager(new LinearLayoutManager(getContext()));

        setupSpinner();
        setupTabs();

        view.findViewById(R.id.btnAddRecipe).setOnClickListener(v -> mostrarOpcoesAdicionar());
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
        view.findViewById(R.id.btnSettings).setOnClickListener(v -> startActivity(new Intent(getActivity(), ProfileActivity.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
        loadRecipes();        // 🔥 APENAS MINHAS RECEITAS
        loadMyCollections();  // 🔥 APENAS MEUS LIVROS
    }

    // =========================================================
    // 🔥 DATA (Lógica original mantida intacta)
    // =========================================================

    private void loadRecipes() {
        if (currentUserId == null) return;

        db.collection("recipes")
                .whereEqualTo("ownerId", currentUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    allRecipesList.clear();

                    for (DocumentSnapshot doc : snap) {
                        Recipe r = doc.toObject(Recipe.class);
                        if (r != null) {
                            r.setFirestoreId(doc.getId());
                            allRecipesList.add(r);
                        }
                    }

                    updateRecipeUI();
                });
    }

    private void loadMyCollections() {
        if (currentUserId == null) return;

        db.collection("recipe_collections")
                .whereEqualTo("ownerId", currentUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    allCollectionsList.clear();

                    for (DocumentSnapshot doc : snap) {
                        RecipeCollection c = doc.toObject(RecipeCollection.class);
                        if (c != null) {
                            c.setId(doc.getId());
                            allCollectionsList.add(c);
                        }
                    }

                    updateCollectionUI();
                });
    }

    // =========================================================
    // UI
    // =========================================================
    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutRecipes.setVisibility(View.VISIBLE);
                    layoutCollections.setVisibility(View.GONE);

                    // 👇 NOVA LINHA: Esconde o texto da aba de Coleções à força!
                    tvEmptyCollections.setVisibility(View.GONE);

                    applyFilter(getSelectedType());
                } else {
                    layoutRecipes.setVisibility(View.GONE);
                    layoutCollections.setVisibility(View.VISIBLE);

                    // 👇 NOVA LINHA: Esconde o texto da aba de Receitas à força!
                    tvEmptyList.setVisibility(View.GONE);

                    toggleEmptyCollections();
                }
            }
            public void onTabUnselected(TabLayout.Tab tab) {}
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private String getSelectedType() {
        return spinnerTypeFilter.getSelectedItem() != null
                ? spinnerTypeFilter.getSelectedItem().toString()
                : "Todos";
    }

    private void updateRecipeUI() {
        if (recipeAdapter == null) {
            recipeAdapter = new RecipeAdapter(allRecipesList, currentUserId, this);
            recyclerViewRecipes.setAdapter(recipeAdapter);
        } else {
            recipeAdapter.updateList(allRecipesList);
        }

        applyFilter(getSelectedType());
    }

    private void updateCollectionUI() {
        if (collectionAdapter == null) {
            collectionAdapter = new CollectionAdapter(allCollectionsList, currentUserId, this);
            recyclerViewCollections.setAdapter(collectionAdapter);
        } else {
            collectionAdapter.updateList(allCollectionsList);
        }

        toggleEmptyCollections();
    }

    private void toggleEmptyCollections() {
        // 1. Verificamos se estamos realmente na aba de Livros/Coleções
        if (tabLayout.getSelectedTabPosition() == 1) {
            // Se sim, aplicamos a sua lógica original
            boolean empty = allCollectionsList.isEmpty();
            tvEmptyCollections.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerViewCollections.setVisibility(empty ? View.GONE : View.VISIBLE);
        } else {
            // 2. Se estivermos na aba de Receitas, garantimos que o texto das coleções fica escondido!
            tvEmptyCollections.setVisibility(View.GONE);
        }
    }

    private void applyFilter(String type) {
        List<Recipe> filtered = new ArrayList<>();

        if (type.equals("Todos") || type.equals("Filtrar por tipos")) {
            filtered.addAll(allRecipesList);
        } else {
            for (Recipe r : allRecipesList) {
                if (r.getType() != null && r.getType().equalsIgnoreCase(type)) {
                    filtered.add(r);
                }
            }
        }

        boolean empty = filtered.isEmpty();
        tvEmptyList.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerViewRecipes.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (recipeAdapter != null) recipeAdapter.updateList(filtered);
    }

    // =========================================================
    // PROFILE
    // =========================================================

    private void loadUserProfile() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    tvProfileName.setText(doc.getString("username"));
                    tvProfileBio.setText(doc.getString("bio"));

                    String base64 = doc.getString("profileImageBase64");
                    if (base64 != null && !base64.isEmpty()) {
                        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                        imgProfilePhoto.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
                    }
                });
    }

    private void setupSpinner() {

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item,
                types
        );

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        spinnerTypeFilter.setAdapter(adapter);

        spinnerTypeFilter.setSelection(1);

        spinnerTypeFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                applyFilter(types[pos]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // =========================================================
    // ACTIONS
    // =========================================================

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(getActivity(), LoginActivity.class));
        getActivity().finish();
    }

    private void mostrarOpcoesAdicionar() {
        new AlertDialog.Builder(getContext())
                .setTitle("O que vais preparar hoje?")
                .setItems(new String[]{"🍳 Nova Receita", "📚 Novo Livro"}, (d, w) -> {
                    if (w == 0) startActivity(new Intent(getActivity(), AddRecipeActivity.class));
                    else startActivity(new Intent(getActivity(), AddCollectionActivity.class));
                }).show();
    }

    // =========================================================
    // CLICKS
    // =========================================================

    @Override public void onViewClick(Recipe r) {
        Intent i = new Intent(getContext(), ViewRecipeActivity.class);
        i.putExtra("firestore_id", r.getFirestoreId());
        startActivity(i);
    }

    @Override public void onEditClick(Recipe r) {
        Intent i = new Intent(getContext(), AddRecipeActivity.class);
        i.putExtra("firestore_id", r.getFirestoreId());
        startActivity(i);
    }

    @Override public void onDeleteClick(Recipe r) {
        db.collection("recipes").document(r.getFirestoreId()).delete()
                .addOnSuccessListener(v -> loadRecipes());
    }

    @Override
    public void onShareClick(Recipe r) {
        if (!r.getOwnerId().equals(currentUserId)) {
            Toast.makeText(getContext(), "Só o dono pode partilhar.", Toast.LENGTH_SHORT).show();
            return;
        }
        mostrarDialogPartilha("Partilhar Receita", r.getName(), email -> shareRecipeWithEmail(r, email));
    }

    private void shareRecipeWithEmail(Recipe recipe, String email) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        String uidToShare = snap.getDocuments().get(0).getId();

                        db.collection("recipes").document(recipe.getFirestoreId())
                                .update("sharedWith", FieldValue.arrayUnion(uidToShare))
                                .addOnSuccessListener(v -> Toast.makeText(getContext(), "Receita partilhada!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao partilhar", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(getContext(), "Utilizador não encontrado", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override public void onCollectionClick(RecipeCollection c) {
        Intent i = new Intent(getActivity(), ViewCollectionActivity.class);
        i.putExtra("collection_id", c.getId());
        startActivity(i);
    }

    @Override public void onDeleteCollectionClick(RecipeCollection c) {
        db.collection("recipe_collections").document(c.getId()).delete()
                .addOnSuccessListener(v -> loadMyCollections());
    }

    @Override
    public void onShareCollectionClick(RecipeCollection c) {
        if (currentUserId == null || !c.getOwnerId().equals(currentUserId)) {
            Toast.makeText(getContext(), "Só o dono pode partilhar.", Toast.LENGTH_SHORT).show();
            return;
        }
        mostrarDialogPartilha("Partilhar Livro", c.getName(), email -> shareCollectionWithEmail(c, email));
    }

    private void shareCollectionWithEmail(RecipeCollection collection, String email) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        String uidToShare = snap.getDocuments().get(0).getId();

                        db.collection("recipe_collections").document(collection.getId())
                                .update("sharedWith", FieldValue.arrayUnion(uidToShare))
                                .addOnSuccessListener(v -> Toast.makeText(getContext(), "Livro partilhado!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Erro ao partilhar", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(getContext(), "Utilizador não encontrado", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================================================
    // 🎨 DIALOG DE PARTILHA MELHORADO E REUTILIZÁVEL
    // =========================================================

    /**
     * Interface funcional para callback de partilha
     */
    private interface OnShareCallback {
        void onShare(String email);
    }

    /**
     * Mostra um dialog elegante e coeso para partilhar receitas/livros
     *
     * @param titulo Título do dialog (ex: "Partilhar Receita")
     * @param itemNome Nome do item a partilhar (para exibir no subtítulo)
     * @param callback Ação a executar quando o utilizador confirmar
     */
    private void mostrarDialogPartilha(String titulo, String itemNome, OnShareCallback callback) {
        Typeface tangerine = ResourcesCompat.getFont(requireContext(), R.font.tangerine_regular);

        // ===== CRIAR CAMPO DE EMAIL ESTILIZADO =====
        final EditText inputEmail = new EditText(getContext());
        inputEmail.setHint("exemplo@email.com");
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setTypeface(tangerine, Typeface.BOLD);
        inputEmail.setTextSize(26f);
        inputEmail.setPadding(40, 30, 40, 30);
        inputEmail.setTextColor(Color.parseColor("#2C2416"));
        inputEmail.setHintTextColor(Color.parseColor("#998F7C"));

        // Background arredondado com borda
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#FFFFFF"));
        inputBg.setCornerRadius(20f);
        inputBg.setStroke(3, Color.parseColor("#BAB095"));
        inputEmail.setBackground(inputBg);

        // ===== CRIAR SUBTÍTULO COM NOME DO ITEM =====
        TextView tvSubtitulo = new TextView(getContext());
        tvSubtitulo.setText("\"" + itemNome + "\"");
        tvSubtitulo.setTypeface(tangerine, Typeface.BOLD_ITALIC);
        tvSubtitulo.setTextSize(22f);
        tvSubtitulo.setTextColor(Color.parseColor("#6B5D4F"));
        tvSubtitulo.setPadding(0, 0, 0, 20);
        tvSubtitulo.setGravity(Gravity.CENTER);

        // ===== CRIAR LABEL DO CAMPO =====
        TextView tvLabel = new TextView(getContext());
        tvLabel.setText("Email do utilizador");
        tvLabel.setTypeface(tangerine, Typeface.BOLD);
        tvLabel.setTextSize(24f);
        tvLabel.setTextColor(Color.parseColor("#5A4A3A"));
        tvLabel.setPadding(10, 0, 0, 10);

        // ===== CONTAINER VERTICAL ORGANIZADO =====
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 20, 50, 30);
        container.addView(tvSubtitulo);
        container.addView(tvLabel);
        container.addView(inputEmail);

        // ===== CRIAR DIALOG =====
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(titulo)
                .setView(container)
                .setPositiveButton("✓ Partilhar", (d, which) -> {
                    String email = inputEmail.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(getContext(), "Por favor insere um email", Toast.LENGTH_SHORT).show();
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(getContext(), "Email inválido", Toast.LENGTH_SHORT).show();
                    } else {
                        callback.onShare(email);
                    }
                })
                .setNegativeButton("✕ Cancelar", null)
                .create();

        // ===== APLICAR FUNDO PERSONALIZADO =====
        if (dialog.getWindow() != null) {
            GradientDrawable dialogBg = new GradientDrawable();
            dialogBg.setColor(Color.parseColor("#FDF8F0"));
            dialogBg.setCornerRadius(30f);
            dialogBg.setStroke(4, Color.parseColor("#D4C4A8"));
            dialog.getWindow().setBackgroundDrawable(dialogBg);
        }

        dialog.show();

        // ===== ESTILIZAR BOTÕES E TÍTULO =====
        TextView tvTitle = dialog.findViewById(getResources().getIdentifier("alertTitle", "id", "android"));
        if (tvTitle != null) {
            tvTitle.setTypeface(tangerine, Typeface.BOLD);
            tvTitle.setTextSize(38f);
            tvTitle.setTextColor(Color.parseColor("#8B7355"));
            tvTitle.setPadding(50, 40, 50, 10);
        }

        Button btnPositivo = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (btnPositivo != null) {
            btnPositivo.setTypeface(tangerine, Typeface.BOLD);
            btnPositivo.setTextSize(18f);
            btnPositivo.setTextColor(Color.parseColor("#8B7355"));
            btnPositivo.setPadding(40, 20, 40, 20);
            GradientDrawable btnBg = new GradientDrawable();
            btnBg.setColor(Color.parseColor("#8B7355"));
            btnBg.setCornerRadius(15f);
            btnPositivo.setBackground(btnBg);
        }

        Button btnNegativo = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (btnNegativo != null) {
            btnNegativo.setTypeface(tangerine, Typeface.BOLD);
            btnNegativo.setTextSize(18f);
            btnNegativo.setTextColor(Color.parseColor("#FF0000"));
        }
    }
}