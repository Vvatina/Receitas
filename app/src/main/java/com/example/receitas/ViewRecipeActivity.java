package com.example.receitas;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.receitas.database.DatabaseHelper;
import com.example.receitas.model.Recipe;

import org.json.JSONArray;

public class ViewRecipeActivity extends AppCompatActivity {

    private TextView tvName, tvType, tvIngredients;
    private ImageView imgMainRecipe;
    private LinearLayout layoutStepsContainer;

    private DatabaseHelper dbHelper;
    private int recipeId;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        // Remove ActionBar
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Inicializa views
        tvName = findViewById(R.id.tvRecipeName);
        tvType = findViewById(R.id.tvRecipeType);
        tvIngredients = findViewById(R.id.tvRecipeIngredients);
        imgMainRecipe = findViewById(R.id.imgMainRecipe);
        layoutStepsContainer = findViewById(R.id.layoutStepsContainer);

        dbHelper = new DatabaseHelper(this);

        // Recebe dados via Intent
        recipeId = getIntent().getIntExtra("recipe_id", -1);
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (recipeId != -1 && userId != -1) {
            loadRecipe(recipeId);
        } else {
            showRecipeNotFound();
        }
    }

    private void loadRecipe(int id) {
        Recipe recipe = dbHelper.getRecipeById(id);

        if (recipe != null && recipe.getUserId() == userId) {

            // --- Nome e tipo ---
            tvName.setText(recipe.getName());
            tvType.setText("Tipo: " + recipe.getType());
            tvType.setTextSize(16f);
            tvType.setLineSpacing(4f, 1f);
            tvType.setPadding(16, 0, 16, 12);

            // --- Ingredientes ---
            tvIngredients.setText("Ingredientes:\n" + recipe.getIngredients());
            tvIngredients.setTextSize(16f);
            tvIngredients.setLineSpacing(4f, 1f);
            tvIngredients.setPadding(16, 0, 16, 12);

            // --- Imagem principal ---
            if (recipe.getMainImageUri() != null && !recipe.getMainImageUri().isEmpty()) {
                imgMainRecipe.setImageURI(Uri.parse(recipe.getMainImageUri()));
                imgMainRecipe.setVisibility(View.VISIBLE);
            } else {
                imgMainRecipe.setVisibility(View.GONE);
            }

            // --- Passos com fotos ---
            layoutStepsContainer.removeAllViews(); // Limpa antes

            if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
                try {
                    JSONArray stepsArray = new JSONArray(recipe.getInstructions());

                    for (int i = 0; i < stepsArray.length(); i++) {
                        String stepText = stepsArray.optJSONObject(i).optString("instructionText", "");
                        String imageUri = stepsArray.optJSONObject(i).optString("imageUri", null);

                        // --- 1️⃣ Título do passo: "Passo X:" em negrito e centralizado ---
                        TextView tvStepTitle = new TextView(this);
                        tvStepTitle.setText("Passo " + (i + 1) + ":");
                        tvStepTitle.setTextColor(Color.parseColor("#4B0082")); // cor destacada
                        tvStepTitle.setTextSize(22f); // maior
                        tvStepTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvStepTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        titleParams.setMargins(0, 16, 0, 8);
                        tvStepTitle.setLayoutParams(titleParams);

                        layoutStepsContainer.addView(tvStepTitle);

                        // --- 2️⃣ Conteúdo do passo (tamanho menor, padrão 16sp) ---
                        TextView tvStepText = new TextView(this);
                        tvStepText.setText(stepText);
                        tvStepText.setTextColor(Color.parseColor("#444444"));
                        tvStepText.setTextSize(16f); // tamanho padrão menor
                        tvStepText.setLineSpacing(4f, 1f);
                        tvStepText.setPadding(16, 0, 16, 12);

                        layoutStepsContainer.addView(tvStepText);

                        // --- 3️⃣ Imagem do passo (se houver) ---
                        if (imageUri != null && !imageUri.isEmpty()) {
                            ImageView ivStep = new ImageView(this);
                            ivStep.setImageURI(Uri.parse(imageUri));
                            ivStep.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    500 // altura da imagem
                            );
                            imgParams.setMargins(0, 0, 0, 16);
                            ivStep.setLayoutParams(imgParams);
                            ivStep.setBackgroundColor(Color.parseColor("#EEEEEE"));

                            layoutStepsContainer.addView(ivStep);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Erro ao carregar passos", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Nenhuma instrução cadastrada", Toast.LENGTH_SHORT).show();
            }

        }
    }
    private void showRecipeNotFound() {
        tvName.setText("Receita não encontrada");
        tvType.setText("ID inválido ou receita não pertence ao usuário logado.");
        tvIngredients.setText("");
        layoutStepsContainer.removeAllViews();
        imgMainRecipe.setVisibility(View.GONE);

        Toast.makeText(this, "Erro: Receita não pode ser carregada.", Toast.LENGTH_LONG).show();
    }
}
