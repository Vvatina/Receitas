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
    private String firestoreId; // ID do documento no Firebase
    private int sqliteId;       // ID local (caso use banco híbrido)
    private String ownerId;     // UID de quem criou
    private String ownerName;   // Nome de quem criou (opcional)

    // -----------------------------------------------------------
    // DADOS DA RECEITA
    // -----------------------------------------------------------
    private String name;
    private String ingredients;
    private String instructions; // JSON ou Texto dos passos
    private String type;         // Ex: Sobremesa, Prato Principal

    // Imagens (URIs ou URLs)
    private String mainImageUri;
    private String stepImagesJson;

    // -----------------------------------------------------------
    // CAMPOS PARA PARTILHA E PERMISSÕES
    // -----------------------------------------------------------

    // Lista simples de IDs para fazer queries rápidas (ex: array-contains)
    private List<String> sharedWith = new ArrayList<>();

    // Mapa detalhado: "ID_USUARIO" -> true (Editor) / false (Visualizador)
    private Map<String, Boolean> permissions = new HashMap<>();

    // -----------------------------------------------------------
    // CONSTRUTORES
    // -----------------------------------------------------------

    // OBRIGATÓRIO: Construtor vazio para o Firebase funcionar
    public Recipe() {}

    // Construtor auxiliar para criação rápida
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

    /**
     * Verifica se o utilizador atual tem permissão de edição.
     * Retorna TRUE se for o dono OU se tiver permissão de editor no mapa.
     */
    @Exclude // @Exclude impede que este método seja interpretado como um campo do banco
    public boolean canEdit(String currentUserId) {
        if (currentUserId == null) return false;

        // 1. O Dono tem sempre permissão total
        if (ownerId != null && ownerId.equals(currentUserId)) return true;

        // 2. Verifica se o mapa de permissões existe e se o utilizador é true (Editor)
        if (permissions != null && permissions.containsKey(currentUserId)) {
            return Boolean.TRUE.equals(permissions.get(currentUserId));
        }

        return false; // Se não for dono nem editor, retorna false (apenas leitura)
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

    // --- GETTERS SEGUROS PARA LISTAS E MAPAS ---
    // (Evitam erros se o Firebase retornar nulo)

    public List<String> getSharedWith() {
        return sharedWith != null ? sharedWith : new ArrayList<>();
    }
    public void setSharedWith(List<String> sharedWith) { this.sharedWith = sharedWith; }

    public Map<String, Boolean> getPermissions() {
        return permissions != null ? permissions : new HashMap<>();
    }
    public void setPermissions(Map<String, Boolean> permissions) { this.permissions = permissions; }
}