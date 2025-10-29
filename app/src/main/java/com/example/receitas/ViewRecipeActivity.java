package com.example.receitas;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.receitas.database.DatabaseHelper;
import com.example.receitas.model.Recipe;

public class ViewRecipeActivity extends AppCompatActivity {

    private TextView tvName, tvType, tvIngredients, tvInstructions;
    private DatabaseHelper dbHelper;
    private int recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().hide();

        // Inicializa views
        tvName = findViewById(R.id.tvRecipeName);
        tvType = findViewById(R.id.tvRecipeType);
        tvIngredients = findViewById(R.id.tvRecipeIngredients);
        tvInstructions = findViewById(R.id.tvRecipeInstructions);

        dbHelper = new DatabaseHelper(this);

        recipeId = getIntent().getIntExtra("recipe_id", -1);
        if (recipeId != -1) {
            loadRecipe(recipeId);
        } else {
            tvName.setText("Receita não encontrada");
            tvType.setText("");
        }
    }

    /** Carrega a receita do banco e exibe */
    private void loadRecipe(int id) {
        Recipe recipe = dbHelper.getRecipeById(id);
        if (recipe != null) {
            tvName.setText(recipe.getName());
            tvType.setText("Tipo: " + recipe.getType());
            tvIngredients.setText("Ingredientes:\n" + recipe.getIngredients());
            tvInstructions.setText("Instruções:\n" + recipe.getInstructions());
        } else {
            tvName.setText("Receita não encontrada");
            tvType.setText("");
            tvIngredients.setText("");
            tvInstructions.setText("");
        }
    }
}
