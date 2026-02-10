package com.example.receitas.model;

import com.google.firebase.firestore.Exclude;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Recipe {
    // -----------------------------------------------------------
    // CAMPOS DE IDENTIFICAÇÃO
    // -----------------------------------------------------------
    private String firestoreId; // ID gerado pelo Firebase (String)
    private int sqliteId;       // ID local (mantido apenas se ainda usar SQLite híbrido)

    private String ownerId;     // UID do dono (String no Firebase)
    private String ownerName;   // Opcional: Nome do dono para exibir fácil

    // -----------------------------------------------------------
    // DADOS DA RECEITA
    // -----------------------------------------------------------
    private String name;
    private String ingredients;
    private String instructions; // JSON dos passos
    private String type;

    // Imagens
    private String mainImageUri;
    private String stepImagesJson;

    // -----------------------------------------------------------
    // CAMPOS PARA COMPARTILHAMENTO (NOVIDADE)
    // -----------------------------------------------------------
    // Lista de IDs para consulta fácil (ex: buscar "receitas compartilhadas comigo")
    private List<String> sharedWith = new ArrayList<>();

    // Mapa para saber o nível de permissão: "ID_USUARIO" -> true (pode editar) / false (só ver)
    private Map<String, Boolean> permissions = new HashMap<>();

    // -----------------------------------------------------------
    // CONSTRUTORES
    // -----------------------------------------------------------

    // OBRIGATÓRIO: Construtor vazio para o Firebase conseguir ler os dados
    public Recipe() {}

    // Construtor completo auxiliar
    public Recipe(String name, String ingredients, String instructions, String type, String ownerId) {
        this.name = name;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.type = type;
        this.ownerId = ownerId;
    }

    // -----------------------------------------------------------
    // LÓGICA DE PERMISSÃO (Helper)
    // -----------------------------------------------------------

    // Método inteligente para saber se um utilizador pode editar
    @Exclude // @Exclude faz com que este método não seja salvo como dado no banco
    public boolean canEdit(String currentUserId) {
        if (currentUserId == null) return false;

        // 1. O dono sempre pode editar
        if (currentUserId.equals(ownerId)) return true;

        // 2. Verifica se está na lista de permissões com valor TRUE
        if (permissions != null && permissions.containsKey(currentUserId)) {
            return Boolean.TRUE.equals(permissions.get(currentUserId));
        }

        return false; // Se não for dono nem tiver permissão explícita
    }

    // -----------------------------------------------------------
    // GETTERS E SETTERS
    // -----------------------------------------------------------

    public String getFirestoreId() { return firestoreId; }
    public void setFirestoreId(String firestoreId) { this.firestoreId = firestoreId; }

    public int getSqliteId() { return sqliteId; }
    public void setSqliteId(int sqliteId) { this.sqliteId = sqliteId; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMainImageUri() { return mainImageUri; }
    public void setMainImageUri(String mainImageUri) { this.mainImageUri = mainImageUri; }

    public String getStepImagesJson() { return stepImagesJson; }
    public void setStepImagesJson(String stepImagesJson) { this.stepImagesJson = stepImagesJson; }

    public List<String> getSharedWith() { return sharedWith; }
    public void setSharedWith(List<String> sharedWith) { this.sharedWith = sharedWith; }

    public Map<String, Boolean> getPermissions() { return permissions; }
    public void setPermissions(Map<String, Boolean> permissions) { this.permissions = permissions; }
}