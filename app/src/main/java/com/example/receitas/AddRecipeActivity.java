package com.example.receitas;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.receitas.database.DatabaseHelper;
import com.example.receitas.model.Recipe;
import com.example.receitas.model.Step;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AddRecipeActivity extends AppCompatActivity {

    private EditText etName, etIngredients;
    private Spinner spinnerType;
    private ImageView imgMainRecipe;
    private Button btnSelectMainImage, btnSave, btnCancel, btnAddStep;
    private LinearLayout layoutInstructionsContainer;

    private DatabaseHelper dbHelper;
    private Recipe recipe;
    private boolean isEdit = false;
    private int userId;
    private String mainImagePath = null;
    private final List<Step> steps = new ArrayList<>();

    private final int PICK_IMAGE_MAIN = 100;
    private final int PICK_IMAGE_STEP = 200;

    private final String[] types = {"Filtro", "Prato Principal", "Sobremesa", "Entrada", "Bebida"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        etName = findViewById(R.id.etName);
        etIngredients = findViewById(R.id.etIngredients);
        spinnerType = findViewById(R.id.spinnerType);
        imgMainRecipe = findViewById(R.id.imgMainRecipe);
        btnSelectMainImage = findViewById(R.id.btnSelectMainImage);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddStep = findViewById(R.id.btnAddStep);
        layoutInstructionsContainer = findViewById(R.id.layoutInstructionsContainer);

        dbHelper = new DatabaseHelper(this);
        userId = getIntent().getIntExtra("USER_ID", -1);

        setupSpinner();
        checkEditMode();
        if (steps.isEmpty()) addStepFieldToUI(null, null);

        btnSelectMainImage.setOnClickListener(v -> pickImage(PICK_IMAGE_MAIN, -1));
        btnAddStep.setOnClickListener(v -> addStepFieldToUI(null, null));
        btnSave.setOnClickListener(v -> saveRecipe());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, types) {
            @Override public boolean isEnabled(int position) { return position != 0; }
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
    }

    private void checkEditMode() {
        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        if (recipeId == -1) return;

        isEdit = true;
        recipe = dbHelper.getRecipeById(recipeId);
        if (recipe == null) return;

        etName.setText(recipe.getName());
        etIngredients.setText(recipe.getIngredients());
        mainImagePath = recipe.getMainImageUri();
        if (mainImagePath != null) imgMainRecipe.setImageURI(Uri.parse(mainImagePath));

        // Carrega passos existentes
        if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
            try {
                Gson gson = new Gson();
                Type stepListType = new TypeToken<ArrayList<Step>>(){}.getType();
                List<Step> loadedSteps = gson.fromJson(recipe.getInstructions(), stepListType);
                steps.clear();
                for (Step step : loadedSteps) addStepFieldToUI(step.getInstructionText(), step.getImageUri());
            } catch (Exception e) {
                Log.e("AddRecipeActivity", "Erro ao carregar passos/JSON: " + e.getMessage());
                Toast.makeText(this, "Erro ao carregar passos. O formato pode estar desatualizado.", Toast.LENGTH_LONG).show();
            }
        }

        int typePosition = ((ArrayAdapter<String>)spinnerType.getAdapter()).getPosition(recipe.getType());
        if (typePosition >= 0) spinnerType.setSelection(typePosition);
        setTitle("Editar Receita");
    }

    private void pickImage(int requestCode, int stepIndex) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        if (requestCode == PICK_IMAGE_STEP) startActivityForResult(intent, PICK_IMAGE_STEP + stepIndex);
        else startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri selectedImage = data.getData();
        String imagePath = selectedImage.toString();
        getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (requestCode == PICK_IMAGE_MAIN) {
            mainImagePath = imagePath;
            imgMainRecipe.setImageURI(selectedImage);
        } else if (requestCode >= PICK_IMAGE_STEP) {
            int stepIndex = requestCode - PICK_IMAGE_STEP;
            if (stepIndex >= 0 && stepIndex < steps.size()) {
                Step stepToUpdate = steps.get(stepIndex);
                stepToUpdate.setImageUri(imagePath);

                LinearLayout stepLayout = (LinearLayout) layoutInstructionsContainer.getChildAt(stepIndex);
                if (stepLayout != null) {
                    ImageView imgStep = stepLayout.findViewWithTag("imgStep_" + stepIndex);
                    if (imgStep != null) {
                        imgStep.setImageURI(selectedImage);
                        imgStep.clearColorFilter();
                    }
                }
            }
        }
    }

    private void addStepFieldToUI(String instructionText, String imageUri) {
        final int stepIndex = steps.size();
        final Step newStep = new Step(instructionText, imageUri);
        steps.add(newStep);

        LinearLayout stepLayout = new LinearLayout(this);
        stepLayout.setOrientation(LinearLayout.VERTICAL);
        stepLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        stepLayout.setPadding(0, 8, 0, 8);

        // Linha separadora
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        stepLayout.addView(divider);

        // Título Passo
        TextView stepNumber = new TextView(this);
        stepNumber.setText("Passo " + (stepIndex + 1));
        stepNumber.setTextSize(18);
        stepNumber.setTextColor(Color.parseColor("#333333"));
        stepNumber.setTypeface(null, android.graphics.Typeface.BOLD);
        stepNumber.setGravity(Gravity.CENTER_HORIZONTAL);
        stepNumber.setPadding(0, 16, 0, 8);
        stepLayout.addView(stepNumber);

        // EditText instrução
        EditText etStep = new EditText(this);
        etStep.setHint("Instrução do Passo " + (stepIndex + 1));
        etStep.setText(instructionText != null ? instructionText : "");
        etStep.setBackgroundResource(R.drawable.bg_edittext);
        etStep.setPadding(24, 24, 24, 24);
        etStep.setTextColor(Color.parseColor("#000000"));
        etStep.setHintTextColor(Color.parseColor("#888888"));
        etStep.setMinLines(3);
        etStep.setGravity(Gravity.TOP);
        etStep.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        etStep.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { newStep.setInstructionText(s.toString()); }
        });
        stepLayout.addView(etStep);

        // Container horizontal para imagem + botão
        LinearLayout imageControlLayout = new LinearLayout(this);
        imageControlLayout.setOrientation(LinearLayout.HORIZONTAL);
        imageControlLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        imageControlLayout.setPadding(0, 8, 0, 8);

        // ImageView
        ImageView imgStep = new ImageView(this);
        imgStep.setTag("imgStep_" + stepIndex);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(150, 150);
        imgParams.setMargins(0, 0, 8, 0);
        imgStep.setLayoutParams(imgParams);
        imgStep.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgStep.setBackgroundColor(Color.parseColor("#EEEEEE"));
        if (imageUri != null) {
            imgStep.setImageURI(Uri.parse(imageUri));
            imgStep.clearColorFilter();
        } else {
            imgStep.setImageResource(android.R.drawable.ic_menu_gallery);
            imgStep.setColorFilter(Color.parseColor("#888888"));
        }
        imageControlLayout.addView(imgStep);

        // Botão estilizado
        Button btnSelectImage = new Button(this);
        btnSelectImage.setText("Selecionar Foto");
        btnSelectImage.setTextColor(Color.WHITE);
        btnSelectImage.setAllCaps(false);
        btnSelectImage.setTextSize(14f);

        // Borda arredondada cinza escura
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#6A1B9A")); // cor de fundo
        drawable.setCornerRadius(16f); // borda arredondada
        btnSelectImage.setBackground(drawable);

        // Ícone de galeria
        btnSelectImage.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_gallery, 0, 0, 0);
        btnSelectImage.setCompoundDrawablePadding(8);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnSelectImage.setLayoutParams(btnParams);
        btnSelectImage.setOnClickListener(v -> pickImage(PICK_IMAGE_STEP, stepIndex));
        imageControlLayout.addView(btnSelectImage);

        stepLayout.addView(imageControlLayout);
        layoutInstructionsContainer.addView(stepLayout);
    }

    private void saveRecipe() {
        String name = etName.getText().toString().trim();
        String ingredients = etIngredients.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();

        if (name.isEmpty() || ingredients.isEmpty() || spinnerType.getSelectedItemPosition() == 0 || steps.isEmpty()) {
            Toast.makeText(this, "Preencha o nome, ingredientes, tipo e adicione pelo menos um passo.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Step step : steps) {
            if (step.getInstructionText() == null || step.getInstructionText().trim().isEmpty()) {
                Toast.makeText(this, "A instrução de um passo não pode estar vazia.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Gson gson = new Gson();
        String stepsJson = gson.toJson(steps);

        if (isEdit && recipe != null) {
            recipe.setName(name);
            recipe.setIngredients(ingredients);
            recipe.setInstructions(stepsJson);
            recipe.setType(type);
            recipe.setMainImageUri(mainImagePath);
            dbHelper.updateRecipe(recipe);
            Toast.makeText(this, "Receita atualizada!", Toast.LENGTH_SHORT).show();
        } else {
            recipe = new Recipe(0, name, ingredients, stepsJson, type, userId, mainImagePath, null);
            dbHelper.addRecipe(recipe, userId);
            Toast.makeText(this, "Receita salva!", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
