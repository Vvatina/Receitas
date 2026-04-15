package com.example.receitas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
import androidx.core.content.res.ResourcesCompat;

import com.example.receitas.model.Recipe;
import com.example.receitas.model.Step;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRecipeActivity extends AppCompatActivity {

    private EditText etName, etIngredients;
    private Spinner spinnerType;
    private ImageView imgMainRecipe;
    private Button btnSelectMainImage, btnSave, btnCancel, btnAddStep;
    private LinearLayout layoutInstructionsContainer;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String firestoreId = null;
    private boolean isEdit = false;
    private String mainImagePath = null;
    private final List<Step> steps = new ArrayList<>();

    private final int PICK_IMAGE_MAIN = 100;
    private final int PICK_IMAGE_STEP = 200;

    private final String[] types = {"Filtro", "Prato Principal", "Sobremesa", "Entrada", "Bebida"};

    private Typeface tangerine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_recipe);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        tangerine = ResourcesCompat.getFont(this, R.font.tangerine_regular);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etName);
        etIngredients = findViewById(R.id.etIngredients);
        spinnerType = findViewById(R.id.spinnerType);
        imgMainRecipe = findViewById(R.id.imgMainRecipe);
        btnSelectMainImage = findViewById(R.id.btnSelectMainImage);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddStep = findViewById(R.id.btnAddStep);
        layoutInstructionsContainer = findViewById(R.id.layoutInstructionsContainer);

        etName.setTypeface(tangerine, Typeface.BOLD);
        etIngredients.setTypeface(tangerine, Typeface.BOLD);
        btnSelectMainImage.setTypeface(tangerine, Typeface.BOLD);
        btnSave.setTypeface(tangerine, Typeface.BOLD);
        btnCancel.setTypeface(tangerine, Typeface.BOLD);
        btnAddStep.setTypeface(tangerine, Typeface.BOLD);

        estilizarBotao(btnSelectMainImage);
        estilizarBotao(btnSave);
        estilizarBotao(btnCancel);
        estilizarBotao(btnAddStep);

        setupSpinner();
        checkEditMode();

        if (!isEdit && steps.isEmpty()) {
            addStepFieldToUI(null, null);
        }

        btnSelectMainImage.setOnClickListener(v -> pickImage(PICK_IMAGE_MAIN, -1));
        btnAddStep.setOnClickListener(v -> addStepFieldToUI(null, null));
        btnSave.setOnClickListener(v -> saveRecipe());

        btnCancel.setOnClickListener(v -> {
            libertarReceita();
            finish();
        });
    }

    private void estilizarBotao(Button btn) {
        btn.setBackgroundTintList(null);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#BAB095"));
        drawable.setCornerRadius(14f);
        btn.setBackground(drawable);
        btn.setTextColor(Color.WHITE);
        btn.setAllCaps(false);
        btn.setTextSize(20f);
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, types) {
            @Override public boolean isEnabled(int position) { return position != 0; }
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTypeface(tangerine, Typeface.BOLD);
                tv.setTextSize(25f);
                tv.setBackgroundColor(Color.parseColor("#FDF5E6"));
                tv.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return tv;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTypeface(tangerine, Typeface.NORMAL);
                tv.setTextSize(25f);
                tv.setTextColor(position == 0 ? Color.parseColor("#888888") : Color.BLACK);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);
        spinnerType.setSelection(0);
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("firestore_id")) {
            firestoreId = getIntent().getStringExtra("firestore_id");
            isEdit = true;
            loadRecipeData(firestoreId);
        }
    }

    private void loadRecipeData(String id) {
        db.collection("recipes").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Recipe recipe = documentSnapshot.toObject(Recipe.class);
                    if (recipe != null) populateUI(recipe);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void populateUI(Recipe recipe) {
        etName.setText(recipe.getName());
        etIngredients.setText(recipe.getIngredients());

        mainImagePath = recipe.getMainImageUri();
        if (mainImagePath != null) {
            try { imgMainRecipe.setImageURI(Uri.parse(mainImagePath)); }
            catch (Exception e) { Log.e("ImgLoad", "Erro imagem local"); }
        }

        if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
            try {
                Gson gson = new Gson();
                Type stepListType = new TypeToken<ArrayList<Step>>(){}.getType();
                List<Step> loadedSteps = gson.fromJson(recipe.getInstructions(), stepListType);

                steps.clear();
                layoutInstructionsContainer.removeAllViews();

                for (Step step : loadedSteps) {
                    addStepFieldToUI(step.getInstructionText(), step.getImageUri());
                }
            } catch (Exception e) {
                Log.e("AddRecipeActivity", "Erro JSON: " + e.getMessage());
            }
        }

        if (recipe.getType() != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerType.getAdapter();
            int position = adapter.getPosition(recipe.getType());
            if (position >= 0) spinnerType.setSelection(position);
        }
    }

    private void pickImage(int requestCode, int stepIndex) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        if (requestCode == PICK_IMAGE_STEP) {
            startActivityForResult(intent, PICK_IMAGE_STEP + stepIndex);
        } else {
            startActivityForResult(intent, requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri selectedImage = data.getData();
        String imagePath = selectedImage.toString();

        try {
            getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            Log.e("Permissions", "Falha na permissão: " + e.getMessage());
        }

        if (requestCode == PICK_IMAGE_MAIN) {
            mainImagePath = imagePath;
            imgMainRecipe.setImageURI(selectedImage);
        } else if (requestCode >= PICK_IMAGE_STEP) {
            int stepIndex = requestCode - PICK_IMAGE_STEP;
            if (stepIndex >= 0 && stepIndex < steps.size()) {
                steps.get(stepIndex).setImageUri(imagePath);

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
        Step newStep = new Step(instructionText, imageUri);

        if (instructionText == null && imageUri == null) steps.add(newStep);
        else if (!steps.contains(newStep)) steps.add(newStep);

        LinearLayout stepLayout = new LinearLayout(this);
        stepLayout.setOrientation(LinearLayout.VERTICAL);
        stepLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        stepLayout.setPadding(0, 8, 0, 8);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(Color.parseColor("#E0E0E0"));
        stepLayout.addView(divider);

        TextView stepNumber = new TextView(this);
        stepNumber.setText("Passo " + (stepIndex + 1));
        stepNumber.setTextSize(30f);
        stepNumber.setTextColor(Color.parseColor("#333333"));
        stepNumber.setTypeface(tangerine, Typeface.BOLD);
        stepNumber.setGravity(Gravity.CENTER_HORIZONTAL);
        stepNumber.setPadding(0, 16, 0, 8);
        stepLayout.addView(stepNumber);

        EditText etStep = new EditText(this);
        etStep.setHint("Instrução do Passo " + (stepIndex + 1));
        etStep.setText(instructionText != null ? instructionText : "");
        etStep.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F6E8D8")));
        etStep.setBackgroundResource(R.drawable.bg_edittext);
        etStep.setPadding(24, 24, 24, 24);
        etStep.setTextColor(Color.parseColor("#000000"));
        etStep.setHintTextColor(Color.parseColor("#4E342E"));
        etStep.setMinLines(3);
        etStep.setGravity(Gravity.TOP);
        etStep.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        etStep.setTypeface(tangerine, Typeface.NORMAL);
        etStep.setTextSize(25f);

        etStep.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (stepIndex < steps.size()) steps.get(stepIndex).setInstructionText(s.toString());
            }
        });
        stepLayout.addView(etStep);

        LinearLayout imageControlLayout = new LinearLayout(this);
        imageControlLayout.setOrientation(LinearLayout.HORIZONTAL);
        imageControlLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        imageControlLayout.setPadding(0, 8, 0, 8);

        ImageView imgStep = new ImageView(this);
        imgStep.setTag("imgStep_" + stepIndex);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(150, 150);
        imgParams.setMargins(0, 0, 8, 0);
        imgStep.setLayoutParams(imgParams);
        imgStep.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgStep.setBackgroundColor(Color.parseColor("#EEEEEE"));
        if (imageUri != null) { imgStep.setImageURI(Uri.parse(imageUri)); imgStep.clearColorFilter(); }
        else { imgStep.setImageResource(android.R.drawable.ic_menu_gallery); imgStep.setColorFilter(Color.parseColor("#888888")); }
        imageControlLayout.addView(imgStep);

        Button btnSelectImage = new Button(this);
        btnSelectImage.setText("Selecionar Foto");
        btnSelectImage.setTextSize(20f);
        btnSelectImage.setTypeface(tangerine, Typeface.BOLD);
        btnSelectImage.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_gallery, 0, 0, 0);
        btnSelectImage.setCompoundDrawablePadding(8);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnSelectImage.setLayoutParams(btnParams);
        estilizarBotao(btnSelectImage);

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
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show();
            return;
        }

        Gson gson = new Gson();
        String stepsJson = gson.toJson(steps);

        Recipe recipeToSave = new Recipe();
        recipeToSave.setName(name);
        recipeToSave.setIngredients(ingredients);
        recipeToSave.setInstructions(stepsJson);
        recipeToSave.setType(type);
        recipeToSave.setMainImageUri(mainImagePath);

        // LÓGICA DE COLEÇÃO: Importante para aparecer na Home ou no Livro
        String collectionIdFromIntent = getIntent().getStringExtra("collection_id");
        recipeToSave.setCollectionId(collectionIdFromIntent);

        String currentUserId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;

        btnSave.setEnabled(false);
        btnSave.setText("A guardar...");

        if (isEdit && firestoreId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("ingredients", ingredients);
            updates.put("instructions", stepsJson);
            updates.put("type", type);
            updates.put("mainImageUri", mainImagePath);

            db.collection("recipes").document(firestoreId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        libertarReceita();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Salvar");
                        Toast.makeText(this, "Erro ao atualizar.", Toast.LENGTH_SHORT).show();
                    });
        } else {// DENTRO DO ELSE (Nova Receita)
            if (currentUserId != null) {
                recipeToSave.setOwnerId(currentUserId);

                // CORREÇÃO: A lista de partilha deve começar VAZIA.
                // Não adiciones o currentUserId aqui!
                List<String> shares = new ArrayList<>();
                recipeToSave.setSharedWith(shares);
            }

            db.collection("recipes")
                    .add(recipeToSave)
                    .addOnSuccessListener(documentReference -> finish())
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Salvar");
                        Toast.makeText(this, "Erro ao criar.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void libertarReceita() {
        if (isEdit && firestoreId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("editadoPorId", com.google.firebase.firestore.FieldValue.delete());
            updates.put("tempoEdicao", com.google.firebase.firestore.FieldValue.delete());
            db.collection("recipes").document(firestoreId).update(updates);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        libertarReceita();
    }
}