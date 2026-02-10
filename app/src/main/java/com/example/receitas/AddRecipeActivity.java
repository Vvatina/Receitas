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

import com.example.receitas.model.Recipe;
import com.example.receitas.model.Step; // Certifique-se que a classe Step existe
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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

    // FIREBASE
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // VARIÁVEIS DE CONTROLE
    private String firestoreId = null; // ID da receita se estivermos a editar
    private boolean isEdit = false;
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

        // Inicializar Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Inicializar Views
        etName = findViewById(R.id.etName);
        etIngredients = findViewById(R.id.etIngredients);
        spinnerType = findViewById(R.id.spinnerType);
        imgMainRecipe = findViewById(R.id.imgMainRecipe);
        btnSelectMainImage = findViewById(R.id.btnSelectMainImage);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddStep = findViewById(R.id.btnAddStep);
        layoutInstructionsContainer = findViewById(R.id.layoutInstructionsContainer);

        setupSpinner();

        // Verifica se é edição
        checkEditMode();

        // Se for nova receita, adiciona o primeiro passo vazio
        if (!isEdit && steps.isEmpty()) {
            addStepFieldToUI(null, null);
        }

        // Listeners
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
        // Agora recebemos um ID String do Firestore
        if (getIntent().hasExtra("firestore_id")) {
            firestoreId = getIntent().getStringExtra("firestore_id");
            isEdit = true;
            setTitle("Editar Receita");
            loadRecipeData(firestoreId);
        }
    }

    private void loadRecipeData(String id) {
        db.collection("recipes").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Recipe recipe = documentSnapshot.toObject(Recipe.class);
                    if (recipe != null) {
                        populateUI(recipe);
                    }
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
            try {
                imgMainRecipe.setImageURI(Uri.parse(mainImagePath));
            } catch (Exception e) {
                // Imagem pode não carregar se for de outro dispositivo (pois é URI local)
                Log.e("ImgLoad", "Erro imagem local");
            }
        }

        // Carregar Passos
        if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
            try {
                Gson gson = new Gson();
                Type stepListType = new TypeToken<ArrayList<Step>>(){}.getType();
                List<Step> loadedSteps = gson.fromJson(recipe.getInstructions(), stepListType);

                steps.clear();
                layoutInstructionsContainer.removeAllViews(); // Limpa views antigas

                for (Step step : loadedSteps) {
                    addStepFieldToUI(step.getInstructionText(), step.getImageUri());
                }
            } catch (Exception e) {
                Log.e("AddRecipeActivity", "Erro JSON: " + e.getMessage());
            }
        }

        // Setar Spinner
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
            // Usamos o requestCode base + index para saber qual passo chamou
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

        // Permissão persistente para ler a imagem depois (apenas localmente)
        try {
            getContentResolver().takePersistableUriPermission(selectedImage, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            Log.e("Permissions", "Falha na permissão persistente: " + e.getMessage());
        }

        if (requestCode == PICK_IMAGE_MAIN) {
            mainImagePath = imagePath;
            imgMainRecipe.setImageURI(selectedImage);
        } else if (requestCode >= PICK_IMAGE_STEP) {
            int stepIndex = requestCode - PICK_IMAGE_STEP;

            // Atualiza o objeto na lista
            if (stepIndex >= 0 && stepIndex < steps.size()) {
                Step stepToUpdate = steps.get(stepIndex);
                stepToUpdate.setImageUri(imagePath);

                // Atualiza a visualização na tela
                LinearLayout stepLayout = (LinearLayout) layoutInstructionsContainer.getChildAt(stepIndex);
                if (stepLayout != null) {
                    // O layout interno tem hierarquia, precisamos achar a imagem pela Tag
                    // A tag foi definida no método addStepFieldToUI como "imgStep_" + index
                    // Nota: O método findViewWithTag procura em todos os filhos
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
        final int stepIndex = steps.size(); // O índice será o tamanho atual antes de adicionar

        // Cria o objeto Step se não vier preenchido
        Step newStep = new Step(instructionText, imageUri);
        // Só adiciona na lista se não estivermos a repopular a UI (evita duplicação no loop do populateUI)
        // Mas como limpamos a lista no populateUI, podemos adicionar sempre aqui.
        // CUIDADO: Se chamarmos isso no clique do botão, o step é novo.
        // Se chamarmos no populateUI, o step já existe.

        // Lógica simplificada: Se instructionText for null, é um novo passo via botão.
        // Se não for null, estamos carregando.
        if (instructionText == null && imageUri == null) {
            steps.add(newStep);
        } else {
            // Se estamos carregando, o objeto já deveria estar na lista?
            // Não, no loop do populateUI nós limpamos a lista 'steps' e vamos adicionando de volta.
            // Então:
            if (!steps.contains(newStep)) {
                steps.add(newStep);
            }
        }

        LinearLayout stepLayout = new LinearLayout(this);
        stepLayout.setOrientation(LinearLayout.VERTICAL);
        stepLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        stepLayout.setPadding(0, 8, 0, 8);

        // Linha separadora
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
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
        etStep.setBackgroundResource(R.drawable.bg_edittext); // Certifique-se de ter este drawable ou remova
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
            @Override public void afterTextChanged(Editable s) {
                // Atualiza o texto na lista em memória
                if (stepIndex < steps.size()) {
                    steps.get(stepIndex).setInstructionText(s.toString());
                }
            }
        });
        stepLayout.addView(etStep);

        // Container Imagem + Botão
        LinearLayout imageControlLayout = new LinearLayout(this);
        imageControlLayout.setOrientation(LinearLayout.HORIZONTAL);
        imageControlLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        imageControlLayout.setPadding(0, 8, 0, 8);

        // ImageView
        ImageView imgStep = new ImageView(this);
        imgStep.setTag("imgStep_" + stepIndex); // Tag para encontrar depois
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

        // Botão Selecionar Imagem
        Button btnSelectImage = new Button(this);
        btnSelectImage.setText("Selecionar Foto");
        btnSelectImage.setTextColor(Color.WHITE);
        btnSelectImage.setAllCaps(false);
        btnSelectImage.setTextSize(14f);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor("#6A1B9A"));
        drawable.setCornerRadius(16f);
        btnSelectImage.setBackground(drawable);

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
            Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Serializa passos para JSON
        Gson gson = new Gson();
        String stepsJson = gson.toJson(steps);

        // Prepara objeto Recipe
        Recipe recipeToSave = new Recipe();
        recipeToSave.setName(name);
        recipeToSave.setIngredients(ingredients);
        recipeToSave.setInstructions(stepsJson);
        recipeToSave.setType(type);
        recipeToSave.setMainImageUri(mainImagePath);

        // Define o Dono (Importante para o Firebase)
        if (auth.getCurrentUser() != null) {
            recipeToSave.setOwnerId(auth.getCurrentUser().getUid());
        }

        // SALVA NO FIREBASE
        btnSave.setEnabled(false); // Evita duplo clique
        btnSave.setText("A guardar...");

        if (isEdit && firestoreId != null) {
            // ATUALIZAÇÃO
            db.collection("recipes").document(firestoreId)
                    .set(recipeToSave) // .set() sobrescreve o documento
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Receita atualizada!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Erro ao atualizar.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // NOVA RECEITA
            db.collection("recipes")
                    .add(recipeToSave)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Receita criada!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Erro ao criar.", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}