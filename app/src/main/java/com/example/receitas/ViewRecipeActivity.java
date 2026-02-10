package com.example.receitas;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.receitas.database.DatabaseHelper;
import com.example.receitas.model.Recipe;

// JSON
import org.json.JSONArray;

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
import com.itextpdf.layout.Document; // Importante: Deve ser este Document!
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.ListItem;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.properties.AreaBreakType;
// ... outros elementos como Table, Image

public class ViewRecipeActivity extends AppCompatActivity {

    private TextView tvName, tvType, tvIngredients;
    private ImageView imgMainRecipe;
    private LinearLayout layoutStepsContainer;
    private Button btnExportPDF;

    private DatabaseHelper dbHelper;
    private int recipeId;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        tvName = findViewById(R.id.tvRecipeName);
        tvType = findViewById(R.id.tvRecipeType);
        tvIngredients = findViewById(R.id.tvRecipeIngredients);
        imgMainRecipe = findViewById(R.id.imgMainRecipe);
        layoutStepsContainer = findViewById(R.id.layoutStepsContainer);
        btnExportPDF = findViewById(R.id.btnExportPDF);

        dbHelper = new DatabaseHelper(this);

        recipeId = getIntent().getIntExtra("recipe_id", -1);
        userId = getIntent().getIntExtra("USER_ID", -1);

        if (recipeId != -1 && userId != -1) {
            loadRecipe(recipeId);
        } else {
            showRecipeNotFound();
        }

        btnExportPDF.setOnClickListener(v -> {
            Recipe recipe = dbHelper.getRecipeById(recipeId);
            if (recipe != null) {
                exportRecipeToDownloads(recipe);
            } else {
                Toast.makeText(this, "Receita não encontrada", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecipe(int id) {
        Recipe recipe = dbHelper.getRecipeById(id);

        if (recipe != null && recipe.getUserId() == userId) {
            tvName.setText(recipe.getName());
            tvType.setText("Tipo: " + recipe.getType());
            tvIngredients.setText("Ingredientes:\n" + recipe.getIngredients());

            if (recipe.getMainImageUri() != null && !recipe.getMainImageUri().isEmpty()) {
                imgMainRecipe.setImageURI(Uri.parse(recipe.getMainImageUri()));
                imgMainRecipe.setVisibility(ImageView.VISIBLE);
            } else {
                imgMainRecipe.setVisibility(ImageView.GONE);
            }

            layoutStepsContainer.removeAllViews();

            if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
                try {
                    JSONArray stepsArray = new JSONArray(recipe.getInstructions());
                    for (int i = 0; i < stepsArray.length(); i++) {
                        String stepText = stepsArray.optJSONObject(i).optString("instructionText", "");
                        String imageUri = stepsArray.optJSONObject(i).optString("imageUri", null);

                        // --- Título do Passo (Ex: Passo 1) ---
                        TextView tvStepTitle = new TextView(this);
                        tvStepTitle.setText("Passo " + (i + 1) + ":");
                        tvStepTitle.setTextColor(getColor(R.color.purple_700));
                        tvStepTitle.setTextSize(22f);
                        tvStepTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvStepTitle.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                        layoutStepsContainer.addView(tvStepTitle);

                        // --- Texto da Instrução (Ex: Misture a farinha...) ---
                        TextView tvStepText = new TextView(this);
                        tvStepText.setText(stepText);
                        tvStepText.setTextSize(16f);

                        // CORREÇÃO: Define a cor do texto para PRETO explicitamente
                        tvStepText.setTextColor(android.graphics.Color.BLACK);

                        // Opcional: Adiciona um pouco de margem/padding para não colar na borda
                        tvStepText.setPadding(0, 5, 0, 20);

                        layoutStepsContainer.addView(tvStepText);

                        // --- Imagem do Passo ---
                        if (imageUri != null && !imageUri.isEmpty()) {
                            ImageView ivStep = new ImageView(this);
                            ivStep.setImageURI(Uri.parse(imageUri));
                            ivStep.setScaleType(ImageView.ScaleType.CENTER_CROP);

                            // Ajuste de layout da imagem no app para não ficar gigante
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    600 // Altura fixa em pixels (ajuste conforme necessário)
                            );
                            params.setMargins(0, 10, 0, 30);
                            ivStep.setLayoutParams(params);

                            layoutStepsContainer.addView(ivStep);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            showRecipeNotFound();
        }
    }

    private void showRecipeNotFound() {
        Toast.makeText(this, "Erro: Receita não pode ser carregada.", Toast.LENGTH_LONG).show();
        finish();
    }

    // =====================================
    // EXPORTAR PDF (Corrigido e Bonito)
    // =====================================
    // =====================================
    // EXPORTAR PDF - ESTRUTURA LIVRO (COM NOME DO UTILIZADOR)
    // =====================================
    private void exportRecipeToDownloads(Recipe recipe) {
        try {
            String fileName = "Receita_" + recipe.getName().replaceAll("\\W+", "_") + ".pdf";
            Uri pdfUri;

            // Configuração do Arquivo
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Receitas");
                pdfUri = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                File downloadsDir = new File(android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS), "Receitas");
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                File pdfFile = new File(downloadsDir, fileName);
                pdfUri = Uri.fromFile(pdfFile);
            }

            if (pdfUri == null) return;

            // Inicialização do PDF
            PdfWriter writer = new PdfWriter(getContentResolver().openOutputStream(pdfUri));
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // --- DEFINIÇÃO DAS CORES (VARIAÇÕES DE ROXO) ---
            DeviceRgb colorDeepPurple = new DeviceRgb(74, 20, 140);   // Roxo Escuro (Títulos Principais)
            DeviceRgb colorBrightPurple = new DeviceRgb(156, 39, 176); // Roxo Vibrante (Destaques/Passos)
            DeviceRgb colorText = new DeviceRgb(50, 50, 50);          // Cinza Escuro (Texto comum)

            // ---------------------------------------------------------
            // PÁGINA 1: CAPA
            // ---------------------------------------------------------
            document.add(new Paragraph("\n\n")); // Espaço no topo

            document.add(new Paragraph(recipe.getName().toUpperCase())
                    .setFontSize(26f)
                    .setBold()
                    .setFontColor(colorDeepPurple) // Roxo Escuro
                    .setTextAlignment(TextAlignment.CENTER));

            document.add(new Paragraph(recipe.getType())
                    .setFontSize(14f)
                    .setItalic()
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f));

            if (recipe.getMainImageUri() != null && !recipe.getMainImageUri().isEmpty()) {
                addImageToPDFStyled(recipe.getMainImageUri(), document, true);
            }

            // Busca o nome e adiciona ao PDF
            String userName = dbHelper.getUserName(recipe.getUserId());

            document.add(new Paragraph("\n\nReceita por " + userName + " no app Receitaria")
                    .setFontSize(12f)
                    .setFontColor(ColorConstants.DARK_GRAY)
                    .setTextAlignment(TextAlignment.CENTER));

            // FORÇA PÁGINA 2
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // ---------------------------------------------------------
            // PÁGINA 2: INGREDIENTES
            // ---------------------------------------------------------
            document.add(new Paragraph("LISTA DE INGREDIENTES")
                    .setFontSize(18f)
                    .setBold()
                    .setFontColor(colorDeepPurple) // Roxo Escuro
                    .setUnderline()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f));

            com.itextpdf.layout.element.Table ingredTable = new com.itextpdf.layout.element.Table(1)
                    .setWidth(UnitValue.createPercentValue(100));

            String[] ingredients = recipe.getIngredients().split("\n");
            for (String item : ingredients) {
                if (!item.trim().isEmpty()) {
                    com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph("•  " + item.trim()).setFontSize(12f).setFontColor(colorText))
                            .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
                            .setPaddingBottom(5f);
                    ingredTable.addCell(cell);
                }
            }
            document.add(ingredTable);

            // FORÇA PÁGINA 3
            document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

            // ---------------------------------------------------------
            // PÁGINA 3+: MODO DE PREPARO
            // ---------------------------------------------------------
            document.add(new Paragraph("MODO DE PREPARO")
                    .setFontSize(18f)
                    .setBold()
                    .setFontColor(colorDeepPurple) // Roxo Escuro
                    .setUnderline()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f));

            if (recipe.getInstructions() != null && !recipe.getInstructions().isEmpty()) {
                JSONArray stepsArray = new JSONArray(recipe.getInstructions());
                for (int i = 0; i < stepsArray.length(); i++) {
                    String stepText = stepsArray.optJSONObject(i).optString("instructionText", "");
                    String imageUri = stepsArray.optJSONObject(i).optString("imageUri", null);

                    // Título do Passo (Usando a variação Roxo Vibrante)
                    document.add(new Paragraph("PASSO " + (i + 1))
                            .setFontSize(14f)
                            .setBold()
                            .setFontColor(colorBrightPurple) // <--- Roxo mais claro/vibrante
                            .setMarginTop(15f));

                    // Texto do Passo
                    document.add(new Paragraph(stepText)
                            .setFontSize(12f)
                            .setTextAlignment(TextAlignment.JUSTIFIED)
                            .setFontColor(colorText)
                            .setMarginBottom(10f));

                    // Imagem do Passo (Se houver)
                    if (imageUri != null && !imageUri.isEmpty()) {
                        addImageToPDFStyled(imageUri, document, false);
                    }

                    // Linha Separadora
                    SolidLine line = new SolidLine(0.5f);
                    line.setColor(ColorConstants.LIGHT_GRAY); // Mantive cinza para não poluir, mas pode por roxo claro se quiser
                    LineSeparator ls = new LineSeparator(line);
                    ls.setMarginTop(10f);
                    document.add(ls);
                }
            }

            document.close();
            Toast.makeText(this, "PDF salvo em Downloads!", Toast.LENGTH_LONG).show();

            // Abrir PDF
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    // Método para adicionar imagem mantendo proporção e qualidade
    // Método CORRIGIDO para imagens proporcionais
    // Substitua este método no seu código
// Método de Imagem com Tamanho Reduzido para os Passos
    private void addImageToPDFStyled(String imageUri, Document document, boolean isMainImage) {
        if (imageUri == null || imageUri.isEmpty()) return;

        try {
            byte[] imageBytes = getBytesFromUri(Uri.parse(imageUri));
            if (imageBytes == null) return;

            ImageData data = ImageDataFactory.create(imageBytes);
            Image pdfImage = new Image(data);

            if (isMainImage) {
                // CAPA: Mantém 50% da largura (Tamanho médio/grande)
                pdfImage.setWidth(UnitValue.createPercentValue(50));
                pdfImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
            } else {
                // PASSOS: MODO MINIATURA
                // 50f = aprox 1.7cm. É bem pequeno.
                pdfImage.setWidth(50f);

                pdfImage.setHorizontalAlignment(HorizontalAlignment.CENTER);

                // Remove margens extras para não ocupar espaço em branco
                pdfImage.setMarginTop(2f);
                pdfImage.setMarginBottom(2f);
            }

            // Garante que a altura acompanhe a largura sem distorcer
            pdfImage.setAutoScaleHeight(true);

            document.add(pdfImage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Auxiliar para ler bytes do arquivo
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