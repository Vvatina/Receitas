package com.example.receitas;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.receitas.database.DatabaseHelper;
import com.example.receitas.model.Recipe;

public class AddRecipeActivity extends AppCompatActivity {
    private EditText etName, etIngredients, etInstructions;
    private DatabaseHelper dbHelper;
    private Recipe recipe;
    private boolean isEdit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().hide();

        dbHelper = new DatabaseHelper(this);
        etName = findViewById(R.id.etName);
        etIngredients = findViewById(R.id.etIngredients);
        etInstructions = findViewById(R.id.etInstructions);

        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        if (recipeId != -1) {
            isEdit = true;
            recipe = dbHelper.getRecipeById(recipeId);
            if (recipe != null) {
                etName.setText(recipe.getName());
                etIngredients.setText(recipe.getIngredients());
                etInstructions.setText(recipe.getInstructions());
                setTitle("Editar Receita");
            }
        }

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> saveRecipe());

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveRecipe() {
        String name = etName.getText().toString().trim();
        String ingredients = etIngredients.getText().toString().trim();
        String instructions = etInstructions.getText().toString().trim();

        if (name.isEmpty() || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEdit && recipe != null) {
            recipe.setName(name);
            recipe.setIngredients(ingredients);
            recipe.setInstructions(instructions);
            dbHelper.updateRecipe(recipe);
            Toast.makeText(this, "Receita atualizada!", Toast.LENGTH_SHORT).show();
        } else {
            recipe = new Recipe(0, name, ingredients, instructions);
            dbHelper.addRecipe(recipe);
            Toast.makeText(this, "Receita salva!", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}