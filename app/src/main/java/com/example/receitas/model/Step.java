package com.example.receitas.model;

public class Step {
    private String instructionText;
    private String imageUri; // Pode ser null se não houver imagem

    // Construtor padrão necessário para desserialização do Gson (JSON para Objeto)
    public Step() {}

    // Construtor com campos
    public Step(String instructionText, String imageUri) {
        this.instructionText = instructionText;
        this.imageUri = imageUri;
    }

    // Getters e Setters
    public String getInstructionText() {
        return instructionText;
    }

    public void setInstructionText(String instructionText) {
        this.instructionText = instructionText;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}