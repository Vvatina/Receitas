package com.example.receitas;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    private void registerUser(String username, String email, String password) {
        registerButton.setEnabled(false);
        registerButton.setText("A criar conta...");

        // 1. Criar utilizador na Autenticação do Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sucesso na Autenticação
                            FirebaseUser user = mAuth.getCurrentUser();
                            saveUserToFirestore(user, username);
                        } else {
                            // Falha
                            registerButton.setEnabled(true);
                            registerButton.setText("CADASTRAR");

                            String error = "Erro ao cadastrar.";
                            if (task.getException() != null) {
                                error = task.getException().getMessage();
                            }
                            Toast.makeText(RegisterActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser user, String username) {
        if (user == null) return;

        // Criar um mapa com os dados do utilizador
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("username", username);
        userData.put("email", user.getEmail());

        // 2. Salvar na coleção "users" usando o UID como ID do documento
        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Conta criada com sucesso!", Toast.LENGTH_SHORT).show();

                    // Vai direto para o App (MainActivity) pois o Firebase já faz o login automático após o registo
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    // Limpa a stack para o utilizador não voltar ao registo se clicar em voltar
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