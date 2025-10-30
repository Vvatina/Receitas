package com.example.receitas;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.receitas.database.DatabaseHelper;
import com.example.receitas.model.Recipe;

public class AddRecipeActivity extends AppCompatActivity {
    private EditText etName, etIngredients, etInstructions;
    private Spinner spinnerType;
    private DatabaseHelper dbHelper;
    private Recipe recipe;
    private boolean isEdit = false;
    private int userId; // Id do usuário logado

    private String[] types = {"Selecione o tipo", "Prato Principal", "Sobremesa", "Entrada", "Bebida"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        getSupportActionBar().hide();

        dbHelper = new DatabaseHelper(this);
        etName = findViewById(R.id.etName);
        etIngredients = findViewById(R.id.etIngredients);
        etInstructions = findViewById(R.id.etInstructions);
        spinnerType = findViewById(R.id.spinnerType);

        // Recebe o userId da MainActivity
        userId = getIntent().getIntExtra("USER_ID", -1);

        // Configura Spinner com hint
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, types) {

            @Override
            public boolean isEnabled(int position) { return position != 0; }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return view;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        spinnerType.setSelection(0);

        // Verifica se é edição
        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        if (recipeId != -1) {
            isEdit = true;
            recipe = dbHelper.getRecipeById(recipeId);
            if (recipe != null) {
                etName.setText(recipe.getName());
                etIngredients.setText(recipe.getIngredients());
                etInstructions.setText(recipe.getInstructions());

                int typePosition = adapter.getPosition(recipe.getType());
                if (typePosition >= 0) spinnerType.setSelection(typePosition);
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
        String type = spinnerType.getSelectedItem().toString();

        if (name.isEmpty() || ingredients.isEmpty() || instructions.isEmpty() || spinnerType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEdit && recipe != null) {
            recipe.setName(name);
            recipe.setIngredients(ingredients);
            recipe.setInstructions(instructions);
            recipe.setType(type);
            dbHelper.updateRecipe(recipe);
            Toast.makeText(this, "Receita atualizada!", Toast.LENGTH_SHORT).show();
        } else {
            // 🔹 Associa a receita ao usuário logado
            recipe = new Recipe(0, name, ingredients, instructions, type, userId);
            dbHelper.addRecipe(recipe, userId);
            Toast.makeText(this, "Receita salva!", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}
