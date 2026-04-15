package com.example.receitas.model;

import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeCollection {

    private String id; // ID do documento no Firebase
    private String name; // Nome do Livro/Coleção (ex: "Natal 2026")
    private String ownerId; // ID do utilizador que criou a coleção

    // Para o futuro: campos para partilha de coleções inteiras
    private List<String> sharedWith = new ArrayList<>();
    private Map<String, Boolean> permissions = new HashMap<>();

    // OBRIGATÓRIO: Construtor vazio para o Firebase funcionar
    public RecipeCollection() {
    }

    // Construtor principal para criar uma nova coleção
    public RecipeCollection(String name, String ownerId) {
        this.name = name;
        this.ownerId = ownerId;
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================

    @Exclude // Impede que o ID seja guardado em duplicado dentro do documento
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    // Getters seguros para listas e mapas
    public List<String> getSharedWith() {
        return sharedWith != null ? sharedWith : new ArrayList<>();
    }

    public void setSharedWith(List<String> sharedWith) {
        this.sharedWith = sharedWith;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions != null ? permissions : new HashMap<>();
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = permissions;
    }
}