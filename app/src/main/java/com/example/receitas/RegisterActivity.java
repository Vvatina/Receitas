package com.example.receitas;

import android.content.Intent;
import android.graphics.Color; // ADICIONADO
import android.graphics.Typeface; // ADICIONADO
import android.graphics.drawable.GradientDrawable; // ADICIONADO
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat; // ADICIONADO

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button registerButton;
    private TextView loginTextView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);

        // --- APLICAR ESTILO PADRONIZADO AO BOTÃO ---
        estilizarBotao(registerButton);

        // --- OPCIONAL: APLICAR FONTE TANGERINE ---
        // Descomenta as linhas abaixo se quiseres a fonte igual à da AddRecipeActivity
        /*
        Typeface tangerine = ResourcesCompat.getFont(this, R.font.tangerine_regular);
        usernameEditText.setTypeface(tangerine, Typeface.BOLD);
        emailEditText.setTypeface(tangerine, Typeface.BOLD);
        passwordEditText.setTypeface(tangerine, Typeface.BOLD);
        registerButton.setTypeface(tangerine, Typeface.BOLD);
        usernameEditText.setTextSize(25f);
        emailEditText.setTextSize(25f);
        passwordEditText.setTextSize(25f);
        */

        // Botão de cadastro
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEditText.getText().toString().trim();
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(RegisterActivity.this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "A senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show();
                    return;
                }

                registerUser(username, email, password);
            }
        });

        // Link para login
        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Apenas fecha esta tela para voltar ao Login (se veio de lá)
            }
        });
    }

    /**
     * Método adicionado para aplicar a cor #BAB095 e bordas arredondadas ao botão.
     */
    private void estilizarBotao(Button btn) {
        btn.setBackgroundTintList(null);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor("#BAB095"));
        drawable.setCornerRadius(14f);

        btn.setBackground(drawable);
        btn.setTextColor(Color.WHITE);
        btn.setAllCaps(false);
        btn.setTextSize(30f);
    }

    private void registerUser(String username, String email, String password) {
        registerButton.setEnabled(false);
        registerButton.setText("A criar conta...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            saveUserToFirestore(user, username);
                        } else {
                            registerButton.setEnabled(true);
                            registerButton.setText("CADASTRAR");
                            Toast.makeText(RegisterActivity.this, "Erro ao criar conta: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String username) {
        if (user == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("username", username);
        userData.put("email", user.getEmail());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    registerButton.setEnabled(true);
                    registerButton.setText("CADASTRAR");
                    Toast.makeText(RegisterActivity.this, "Erro ao salvar perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}