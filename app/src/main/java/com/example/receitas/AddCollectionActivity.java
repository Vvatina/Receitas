package com.example.receitas;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.receitas.model.RecipeCollection;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddCollectionActivity extends AppCompatActivity {

    private EditText editCollectionName;
    private Button btnSaveCollection;
    private ImageButton btnBack;

    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_collection);

        // Esconde a barra superior do Android
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        editCollectionName = findViewById(R.id.editCollectionName);
        btnSaveCollection = findViewById(R.id.btnSaveCollection);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnSaveCollection.setOnClickListener(v -> {
            String name = editCollectionName.getText().toString().trim();
            if (name.isEmpty()) {
                editCollectionName.setError("O nome da coleção é obrigatório!");
                return;
            }
            saveCollection(name);
        });
    }

    private void saveCollection(String name) {
        if (currentUserId == null) return;

        // Desativar o botão para não clicar duas vezes sem querer
        btnSaveCollection.setEnabled(false);
        btnSaveCollection.setText("A criar livro...");

        // 1. Criar o objeto vazio
        RecipeCollection newCollection = new RecipeCollection();

        // 2. Preencher os dados dos nomes
        newCollection.setName(name);
        newCollection.setOwnerId(currentUserId);
        newCollection.setSharedWith(new java.util.ArrayList<>()); // INICIALIZA A LISTA VAZIA!

        // Guardar no Firestore na pasta "recipe_collections"
        db.collection("recipe_collections")
                .add(newCollection)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Coleção '" + name + "' criada com sucesso!", Toast.LENGTH_SHORT).show();
                    finish(); // Fecha a página e volta à Home (aba das coleções)
                })
                .addOnFailureListener(e -> {
                    btnSaveCollection.setEnabled(true);
                    btnSaveCollection.setText("Criar Coleção");
                    Toast.makeText(this, "Erro ao criar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}