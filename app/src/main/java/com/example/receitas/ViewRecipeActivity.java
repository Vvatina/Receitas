package com.example.receitas;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager; // IMPORTANTE: Para manter o ecrã ligado
import android.widget.Button;
import android.widget.CompoundButton; // IMPORTANTE: Para o Switch
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch; // IMPORTANTE: Para o Switch
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;

import com.example.receitas.model.Recipe;
import com.example.receitas.model.Step;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

// JSON Imports
import org.json.JSONArray;
import org.json.JSONObject;

// iText PDF Imports
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ViewRecipeActivity extends AppCompatActivity {

    private TextView tvName, tvType, tvIngredients;
    private ImageView imgMainRecipe;
    private LinearLayout layoutStepsContainer;
    private Button btnExportPDF;

    // NOVO: Declarar o interrutor do Modo de Cozinha
    private Switch switchModoCozinha;

    // FIREBASE
    private FirebaseFirestore db;
    private String firestoreId;
    private Recipe currentRecipe;
    private String currentAuthorName = "Desconhecido";

    // DESIGN
    private Typeface tangerine; // Fonte vintage

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 1. Carregar Fonte Vintage
        try {
            tangerine = ResourcesCompat.getFont(this, R.font.tangerine_regular);
        } catch (Exception e) {
            Log.e("FontError", "Erro ao carregar fonte tangerine");
            tangerine = Typeface.DEFAULT;
        }

        // 2. Inicializar Views
        tvName = findViewById(R.id.tvRecipeName);
        tvType = findViewById(R.id.tvRecipeType);
        tvIngredients = findViewById(R.id.tvRecipeIngredients);
        imgMainRecipe = findViewById(R.id.imgMainRecipe);
        layoutStepsContainer = findViewById(R.id.layoutStepsContainer);
        btnExportPDF = findViewById(R.id.btnExportPDF);

        // NOVO: Inicializar o interrutor do Modo de Cozinha
        switchModoCozinha = findViewById(R.id.switchModoCozinha);

        // 3. Aplicar Estilo Vintage aos elementos fixos
        if (tangerine != null) {
            tvName.setTypeface(tangerine, Typeface.BOLD);
            tvType.setTypeface(tangerine, Typeface.BOLD);
            tvIngredients.setTypeface(tangerine);

            // O botão também recebe a fonte
            btnExportPDF.setTypeface(tangerine, Typeface.BOLD);
            btnExportPDF.setTextSize(24f);

            // NOVO: Aplicar a fonte vintage ao interrutor
            switchModoCozinha.setTypeface(tangerine, Typeface.BOLD);
        }

        // 4. Aplicar Estilo de Botão (Bege)
        estilizarBotao(btnExportPDF);

        // ==========================================
        // NOVO: LÓGICA DO MODO DE COZINHA
        // ==========================================
        switchModoCozinha.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Adiciona a "flag" para manter o ecrã ligado
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Toast.makeText(ViewRecipeActivity.this, "Modo de Cozinha Ativado \uD83C\uDF73", Toast.LENGTH_SHORT).show();
            } else {
                // Remove a "flag" para o ecrã poder desligar-se
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Toast.makeText(ViewRecipeActivity.this, "Modo de Cozinha Desativado", Toast.LENGTH_SHORT).show();
            }
        });
        // ==========================================

        // Inicializa Firebase
        db = FirebaseFirestore.getInstance();

        // Recupera ID
        if (getIntent().hasExtra("firestore_id")) {
            firestoreId = getIntent().getStringExtra("firestore_id");
            loadRecipeFromCloud(firestoreId);
        } else {
            Toast.makeText(this, "ID da receita não fornecido.", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnExportPDF.setOnClickListener(v -> {
            if (currentRecipe != null) {
                exportRecipeToDownloads(currentRecipe);
            } else {
                Toast.makeText(this, "A aguardar carregamento...", Toast.LENGTH_SHORT).show();
            }
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
    }

    private void loadRecipeFromCloud(String id) {
        db.collection("recipes").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentRecipe = documentSnapshot.toObject(Recipe.class);
                        if (currentRecipe != null) {
                            populateUI(currentRecipe);
                            fetchAuthorName(currentRecipe.getOwnerId());
                        }
                    } else {
                        Toast.makeText(this, "Receita não encontrada.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erro ao carregar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void fetchAuthorName(String ownerId) {
        if (ownerId == null) return;
        db.collection("users").document(ownerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentAuthorName = documentSnapshot.getString("username");
                    }
                });
    }

    private void populateUI(Recipe recipe) {
        tvName.setText(recipe.getName());
        tvType.setText(recipe.getType());
        tvIngredients.setText(recipe.getIngredients());

        if (recipe.getMainImageUri() != null && !recipe.getMainImageUri().isEmpty()) {
            try {
                imgMainRecipe.setImageURI(Uri.parse(recipe.getMainImageUri()));
                imgMainRecipe.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                imgMainRecipe.setVisibility(View.GONE);
            }
        } else {
            imgMainRecipe.setVisibility(View.GONE);
        }

        layoutStepsContainer.removeAllViews();

        if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
            try {
                Gson gson = new Gson();
                Type stepListType = new TypeToken<ArrayList<Step>>(){}.getType();
                List<Step> stepList = gson.fromJson(recipe.getInstructions(), stepListType);

                int count = 1;
                for (Step step : stepList) {
                    addStepToLayout(count, step.getInstructionText(), step.getImageUri());
                    count++;
                }
            } catch (Exception e) {
                Log.e("ViewRecipe", "Erro ao fazer parse dos passos: " + e.getMessage());
            }
        }
    }

    private void addStepToLayout(int number, String text, String imageUri) {
        LinearLayout stepLayout = new LinearLayout(this);
        stepLayout.setOrientation(LinearLayout.VERTICAL);
        stepLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        stepLayout.setPadding(0, 0, 0, 32);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Passo " + number);
        tvTitle.setTextSize(32f);
        tvTitle.setTextColor(Color.parseColor("#4E342E"));
        tvTitle.setTypeface(tangerine, Typeface.BOLD);
        tvTitle.setGravity(Gravity.START);
        stepLayout.addView(tvTitle);

        TextView tvText = new TextView(this);
        tvText.setText(text);
        tvText.setTextSize(26f);
        tvText.setTextColor(Color.parseColor("#3E2723"));
        tvText.setTypeface(tangerine, Typeface.NORMAL);
        tvText.setPadding(8, 4, 8, 16);
        stepLayout.addView(tvText);

        if (imageUri != null && !imageUri.isEmpty()) {
            CardView imgCard = new CardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 500);
            cardParams.setMargins(8, 8, 8, 8);
            imgCard.setLayoutParams(cardParams);
            imgCard.setRadius(12f);
            imgCard.setCardElevation(4f);
            imgCard.setCardBackgroundColor(Color.WHITE);

            ImageView ivStep = new ImageView(this);
            ivStep.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivStep.setScaleType(ImageView.ScaleType.CENTER_CROP);

            try {
                ivStep.setImageURI(Uri.parse(imageUri));
                imgCard.addView(ivStep);
                stepLayout.addView(imgCard);
            } catch (Exception e) {
            }
        }

        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(200, 2);
        divParams.gravity = Gravity.CENTER_HORIZONTAL;
        divParams.setMargins(0, 16, 0, 0);
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor("#BAB095"));
        stepLayout.addView(divider);

        layoutStepsContainer.addView(stepLayout);
    }

    private void exportRecipeToDownloads(Recipe recipe) {
        try {
            String fileName = "Receita_" + recipe.getName().replaceAll("\\W+", "_") + ".pdf";
            Uri pdfUri = null;
            OutputStream outputStream = null;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Receitas");
                pdfUri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (pdfUri != null) outputStream = getContentResolver().openOutputStream(pdfUri);
            } else {
                File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "Receitas");
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File pdfFile = new File(downloadsDir, fileName);
                outputStream = new java.io.FileOutputStream(pdfFile);
                pdfUri = Uri.fromFile(pdfFile);
            }

            if (outputStream == null) return;

            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            DeviceRgb colorDeepPurple = new DeviceRgb(74, 20, 140);
            DeviceRgb colorText = new DeviceRgb(50, 50, 50);

            document.add(new Paragraph("\n"));
            document.add(new Paragraph(recipe.getName().toUpperCase())
                    .setFontSize(24f).setBold().setFontColor(colorDeepPurple).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(recipe.getType())
                    .setFontSize(14f).setItalic().setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(10f));

            if (recipe.getMainImageUri() != null) {
                addImageToPDFStyled(recipe.getMainImageUri(), document);
            }

            document.add(new Paragraph("\nPor: " + currentAuthorName)
                    .setFontSize(10f).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER));

            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            document.add(new Paragraph("INGREDIENTES")
                    .setFontSize(16f).setBold().setFontColor(colorDeepPurple).setUnderline());

            String[] ingredients = recipe.getIngredients().split("\n");
            for(String ing : ingredients) {
                document.add(new Paragraph("• " + ing).setFontSize(12f));
            }

            document.add(new Paragraph("\nPREPARO")
                    .setFontSize(16f).setBold().setFontColor(colorDeepPurple).setUnderline().setMarginTop(20f));

            if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
                Gson gson = new Gson();
                Type stepListType = new TypeToken<ArrayList<Step>>(){}.getType();
                List<Step> stepList = gson.fromJson(recipe.getInstructions(), stepListType);

                int i = 1;
                for (Step step : stepList) {
                    document.add(new Paragraph("Passo " + i).setBold().setFontColor(colorDeepPurple).setMarginTop(10f));
                    document.add(new Paragraph(step.getInstructionText()).setTextAlignment(TextAlignment.JUSTIFIED));

                    if (step.getImageUri() != null) {
                        addImageToPDFStyled(step.getImageUri(), document);
                    }
                    i++;
                }
            }

            document.close();
            Toast.makeText(this, "PDF salvo em Downloads!", Toast.LENGTH_LONG).show();

            if (pdfUri != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(pdfUri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addImageToPDFStyled(String uriString, Document document) {
        try {
            Uri uri = Uri.parse(uriString);
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is != null) {
                byte[] bytes = new byte[is.available()];
                is.read(bytes);
                is.close();
                ImageData imageData = ImageDataFactory.create(bytes);
                Image img = new Image(imageData);
                img.setAutoScale(true);
                img.setMaxHeight(300);
                img.setHorizontalAlignment(HorizontalAlignment.CENTER);
                document.add(img);
            }
        } catch (Exception e) {
        }
    }
}