package com.example.receitas.model;

public class Recipe {
    private int id;
    private String name;
    private String ingredients;
    private String instructions;
    private String type;
    private int userId;

    // 🔹 Novos campos para imagens (opcionais)
    private String mainImageUri;   // foto final da receita
    private String stepImagesJson; // fotos dos passos (armazenadas como JSON)

    public Recipe() {} // Construtor vazio para SQLite

    public Recipe(int id, String name, String ingredients, String instructions,
                  String type, int userId, String mainImageUri, String stepImagesJson) {
        this.id = id;
        this.name = name;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.type = type;
        this.userId = userId;
        this.mainImageUri = mainImageUri;
        this.stepImagesJson = stepImagesJson;
    }

    // ==================== Getters e Setters ====================

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMainImageUri() { return mainImageUri; }
    public void setMainImageUri(String mainImageUri) { this.mainImageUri = mainImageUri; }

    public String getStepImagesJson() { return stepImagesJson; }
    public void setStepImagesJson(String stepImagesJson) { this.stepImagesJson = stepImagesJson; }
}
