package com.example.receitas;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color; // ADICIONADO
import android.graphics.drawable.GradientDrawable; // ADICIONADO
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText editUsername, editBio, editEmail, editPassword;
    private ImageView imgProfile;
    private Button btnGuardar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    private Uri imagemSelecionadaUri = null;
    private String imagemBase64Atual = "";

    private final ActivityResultLauncher<Intent> escolherImagemLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imagemSelecionadaUri = result.getData().getData();
                    if (imagemSelecionadaUri != null) {
                        // Carregar a imagem diretamente no ImageView (Quadrada)
                        imgProfile.setImageURI(imagemSelecionadaUri);
                        imgProfile.setPadding(0, 0, 0, 0);
                        imgProfile.setColorFilter(null);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editUsername = findViewById(R.id.editUsername);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editBio = findViewById(R.id.editBio); // 👈 ADICIONE ESTA LINHA!
        imgProfile = findViewById(R.id.imgProfile);
        btnGuardar = findViewById(R.id.btnSaveProfile);

        // --- APLICAR ESTILO PADRONIZADO (Cor #BAB095) ---
        estilizarBotao(btnGuardar);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            carregarDadosDoUtilizador(currentUser);
        } else {
            Toast.makeText(this, "Erro de sessão.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imgProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            escolherImagemLauncher.launch(intent);
        });

        btnGuardar.setOnClickListener(v -> guardarDados(currentUser));
    }

    /**
     * Método adicionado para aplicar a cor #BAB095 e bordas arredondadas ao botão.
     */
    private void estilizarBotao(Button btn) {
        // Remove a cor de "tint" padrão do Android
        btn.setBackgroundTintList(null);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);

        // Define a cor solicitada: #BAB095
        drawable.setColor(Color.parseColor("#BAB095"));

        // Define bordas arredondadas
        drawable.setCornerRadius(14f);

        btn.setBackground(drawable);
        btn.setTextColor(Color.WHITE); // Texto branco para contraste
        btn.setAllCaps(false); // Mantém o estilo da fonte (se aplicável)
        btn.setTextSize(20f);
    }

    private void carregarDadosDoUtilizador(FirebaseUser user) {
        editEmail.setText(user.getEmail());

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nome = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");
                        imagemBase64Atual = documentSnapshot.getString("profileImageBase64");

                        if (nome != null) editUsername.setText(nome);
                        if (bio != null) editBio.setText(bio);

                        if (imagemBase64Atual != null && !imagemBase64Atual.isEmpty()) {
                            try {
                                String textoLimpo = imagemBase64Atual.replaceAll("\\s+", "");
                                byte[] imageByteArray = Base64.decode(textoLimpo, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);

                                if (bitmap != null) {
                                    // Garantir que corre na thread principal e limpar filtros antigos
                                    runOnUiThread(() -> {
                                        imgProfile.setPadding(0, 0, 0, 0);
                                        imgProfile.setColorFilter(null);
                                        imgProfile.setImageBitmap(bitmap);

                                        System.out.println("Imagem carregada com sucesso!");
                                    });
                                } else {
                                    System.out.println("Erro: Bitmap veio nulo");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private void guardarDados(FirebaseUser user) {
        String novoUsername = editUsername.getText().toString().trim();
        String novaBio = editBio.getText().toString().trim();

        if (novoUsername.isEmpty()) {
            Toast.makeText(this, "O Nome é obrigatório!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnGuardar.setEnabled(false);
        btnGuardar.setText("A Guardar...");

        String textoDaFotoParaGuardar = imagemBase64Atual;

        if (imagemSelecionadaUri != null) {
            String fotoConvertida = converterImagemParaTexto(imagemSelecionadaUri);
            if (fotoConvertida != null) {
                textoDaFotoParaGuardar = fotoConvertida;
            }
        }

        Map<String, Object> dadosAtualizados = new HashMap<>();
        dadosAtualizados.put("username", novoUsername);
        dadosAtualizados.put("bio", novaBio);
        dadosAtualizados.put("profileImageBase64", textoDaFotoParaGuardar);

        db.collection("users").document(currentUserId)
                .update(dadosAtualizados)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Perfil guardado!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnGuardar.setEnabled(true);
                    btnGuardar.setText("Guardar Alterações");
                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String converterImagemParaTexto(Uri uri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

            // Mantemos o redimensionamento para não sobrecarregar a base de dados
            Bitmap bitmapRedimensionado = Bitmap.createScaledBitmap(bitmap, 400, 400, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmapRedimensionado.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] imageBytes = baos.toByteArray();

            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}