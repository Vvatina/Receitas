package com.example.receitas;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.receitas.model.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

// JSON
import org.json.JSONArray;
import org.json.JSONObject;

// Java IO
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

// iText PDF Imports
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
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

public class ViewRecipeActivity extends AppCompatActivity {

    private TextView tvName, tvType, tvIngredients, tvAuthorName;
    private ImageView imgMainRecipe;
    private LinearLayout layoutStepsContainer;
    private Button btnExportPDF;

    // FIREBASE
    private FirebaseFirestore db;
    private String firestoreId;
    private Recipe currentRecipe; // Armazena a receita carregada
    private String currentAuthorName = "Desconhecido"; // Armazena o nome do autor

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Inicializa UI
        tvName = findViewById(R.id.tvRecipeName);
        tvType = findViewById(R.id.tvRecipeType);
        tvIngredients = findViewById(R.id.tvRecipeIngredients);
        imgMainRecipe = findViewById(R.id.imgMainRecipe);
        layoutStepsContainer = findViewById(R.id.layoutStepsContainer);
        btnExportPDF = findViewById(R.id.btnExportPDF);

        // Adicione um TextView no seu XML para mostrar o autor se quiser, ou use Toast
        // tvAuthorName = findViewById(R.id.tvAuthorName);

        // Inicializa Firebase
        db = FirebaseFirestore.getInstance();

        // Recupera ID passado pela Intent
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
                Toast.makeText(this, "A aguardar carregamento da receita...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecipeFromCloud(String id) {
        db.collection("recipes").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentRecipe = documentSnapshot.toObject(Recipe.class);
                        if (currentRecipe != null) {
                            populateUI(currentRecipe);
                            // Busca o nome do autor baseado no ID do dono
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
                        // Se tiver um TextView para o autor, atualize aqui:
                        // tvAuthorName.setText("Por: " + currentAuthorName);
                    }
                });
    }

    private void populateUI(Recipe recipe) {
        tvName.setText(recipe.getName());
        tvType.setText("Tipo: " + recipe.getType());
        tvIngredients.setText("Ingredientes:\n" + recipe.getIngredients());

        // Carrega Imagem Principal
        if (recipe.getMainImageUri() != null && !recipe.getMainImageUri().isEmpty()) {
            try {
                imgMainRecipe.setImageURI(Uri.parse(recipe.getMainImageUri()));
                imgMainRecipe.setVisibility(ImageView.VISIBLE);
            } catch (Exception e) {
                // Se a imagem for local de outro aparelho, não vai carregar
                imgMainRecipe.setVisibility(ImageView.GONE);
            }
        } else {
            imgMainRecipe.setVisibility(ImageView.GONE);
        }

        layoutStepsContainer.removeAllViews();

        if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
            try {
                JSONArray stepsArray = new JSONArray(recipe.getInstructions());
                for (int i = 0; i < stepsArray.length(); i++) {
                    JSONObject stepObj = stepsArray.optJSONObject(i);
                    String stepText = stepObj.optString("instructionText", "");
                    String imageUri = stepObj.optString("imageUri", null);

                    // --- Título do Passo ---
                    TextView tvStepTitle = new TextView(this);
                    tvStepTitle.setText("Passo " + (i + 1) + ":");
                    tvStepTitle.setTextColor(getColor(R.color.purple_700)); // Certifique-se que essa cor existe ou use Color.parseColor("#7B1FA2")
                    tvStepTitle.setTextSize(22f);
                    tvStepTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvStepTitle.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                    layoutStepsContainer.addView(tvStepTitle);

                    // --- Texto da Instrução ---
                    TextView tvStepText = new TextView(this);
                    tvStepText.setText(stepText);
                    tvStepText.setTextSize(16f);
                    tvStepText.setTextColor(android.graphics.Color.BLACK);
                    tvStepText.setPadding(0, 5, 0, 20);
                    layoutStepsContainer.addView(tvStepText);

                    // --- Imagem do Passo ---
                    if (imageUri != null && !imageUri.isEmpty()) {
                        ImageView ivStep = new ImageView(this);
                        try {
                            ivStep.setImageURI(Uri.parse(imageUri));
                            ivStep.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 600);
                            params.setMargins(0, 10, 0, 30);
                            ivStep.setLayoutParams(params);
                            layoutStepsContainer.addView(ivStep);
                        } catch (Exception e) {
                            // Ignora imagem quebrada
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // =====================================
    // EXPORTAR PDF
    // =====================================
    private void exportRecipeToDownloads(Recipe recipe) {
        try {
            String fileName = "Receita_" + recipe.getName().replaceAll("\\W+", "_") + ".pdf";
            Uri pdfUri;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Receitas");
                pdfUri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "Receitas");
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File pdfFile = new File(downloadsDir, fileName);
                pdfUri = Uri.fromFile(pdfFile);
            }

            if (pdfUri == null) return;

            PdfWriter writer = new PdfWriter(getContentResolver().openOutputStream(pdfUri));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            DeviceRgb colorDeepPurple = new DeviceRgb(74, 20, 140);
            DeviceRgb colorBrightPurple = new DeviceRgb(156, 39, 176);
            DeviceRgb colorText = new DeviceRgb(50, 50, 50);

            // CAPA
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph(recipe.getName().toUpperCase())
                    .setFontSize(26f).setBold().setFontColor(colorDeepPurple).setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(recipe.getType())
                    .setFontSize(14f).setItalic().setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20f));

            if (recipe.getMainImageUri() != null && !recipe.getMainImageUri().isEmpty()) {
                addImageToPDFStyled(recipe.getMainImageUri(), document, true);
            }

            // USA O NOME CARREGADO DO FIREBASE
            document.add(new Paragraph("\n\nReceita por " + currentAuthorName + " no app Receitaria")
                    .setFontSize(12f).setFontColor(ColorConstants.DARK_GRAY).setTextAlignment(TextAlignment.CENTER));

            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // INGREDIENTES
            document.add(new Paragraph("LISTA DE INGREDIENTES")
                    .setFontSize(18f).setBold().setFontColor(colorDeepPurple).setUnderline()
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20f));

            com.itextpdf.layout.element.Table ingredTable = new com.itextpdf.layout.element.Table(1)
                    .setWidth(UnitValue.createPercentValue(100));

            String[] ingredients = recipe.getIngredients().split("\n");
            for (String item : ingredients) {
                if (!item.trim().isEmpty()) {
                    com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph("•  " + item.trim()).setFontSize(12f).setFontColor(colorText))
                            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setPaddingBottom(5f);
                    ingredTable.addCell(cell);
                }
            }
            document.add(ingredTable);

            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // MODULO DE PREPARO
            document.add(new Paragraph("MODO DE PREPARO")
                    .setFontSize(18f).setBold().setFontColor(colorDeepPurple).setUnderline()
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20f));

            if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
                JSONArray stepsArray = new JSONArray(recipe.getInstructions());
                for (int i = 0; i < stepsArray.length(); i++) {
                    String stepText = stepsArray.optJSONObject(i).optString("instructionText", "");
                    String imageUri = stepsArray.optJSONObject(i).optString("imageUri", null);

                    document.add(new Paragraph("PASSO " + (i + 1))
                            .setFontSize(14f).setBold().setFontColor(colorBrightPurple).setMarginTop(15f));

                    document.add(new Paragraph(stepText)
                            .setFontSize(12f).setTextAlignment(TextAlignment.JUSTIFIED)
                            .setFontColor(colorText).setMarginBottom(10f));

                    if (imageUri != null && !imageUri.isEmpty()) {
                        addImageToPDFStyled(imageUri, document, false);
                    }

                    SolidLine line = new SolidLine(0.5f);
                    line.setColor(ColorConstants.LIGHT_GRAY);
                    LineSeparator ls = new LineSeparator(line);
                    ls.setMarginTop(10f);
                    document.add(ls);
                }
            }

            document.close();
            Toast.makeText(this, "PDF salvo em Downloads!", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addImageToPDFStyled(String imageUri, Document document, boolean isMainImage) {
        if (imageUri == null || imageUri.isEmpty()) return;

        try {
            byte[] imageBytes = getBytesFromUri(Uri.parse(imageUri));
            if (imageBytes == null) return;

            ImageData data = ImageDataFactory.create(imageBytes);
            Image pdfImage = new Image(data);

            if (isMainImage) {
                pdfImage.setWidth(UnitValue.createPercentValue(50));
                pdfImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
            } else {
                pdfImage.setWidth(50f);
                pdfImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
                pdfImage.setMarginTop(2f);
                pdfImage.setMarginBottom(2f);
            }
            pdfImage.setAutoScaleHeight(true);
            document.add(pdfImage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] getBytesFromUri(Uri uri) {
        try (InputStream iStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = iStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}