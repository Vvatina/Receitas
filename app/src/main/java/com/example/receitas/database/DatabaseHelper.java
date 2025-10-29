package com.example.receitas.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.receitas.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "recipes.db";
    private static final int DATABASE_VERSION = 3;


    // Tabela e colunas
    private static final String TABLE_RECIPES = "recipes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_INGREDIENTS = "ingredients";
    private static final String COLUMN_INSTRUCTIONS = "instructions";
    private static final String COLUMN_TYPE = "type";

    // SQL para criar tabela
    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_RECIPES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_INGREDIENTS + " TEXT, " +
                    COLUMN_INSTRUCTIONS + " TEXT, " +
                    COLUMN_TYPE + " TEXT);"; // adiciona tipo

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        onCreate(db);
    }

    public void addRecipe(Recipe recipe) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, recipe.getName());
        values.put(COLUMN_INGREDIENTS, recipe.getIngredients());
        values.put(COLUMN_INSTRUCTIONS, recipe.getInstructions());
        values.put(COLUMN_TYPE, recipe.getType()); // novo campo
        db.insert(TABLE_RECIPES, null, values);
        db.close();
    }

    public void updateRecipe(Recipe recipe) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, recipe.getName());
        values.put(COLUMN_INGREDIENTS, recipe.getIngredients());
        values.put(COLUMN_INSTRUCTIONS, recipe.getInstructions());
        values.put(COLUMN_TYPE, recipe.getType()); // novo campo
        db.update(TABLE_RECIPES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(recipe.getId())});
        db.close();
    }


    // Deletar receita
    public void deleteRecipe(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECIPES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Listar todas as receitas
    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES, null);

        if (cursor.moveToFirst()) {
            do {
                Recipe recipe = new Recipe();
                recipe.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                recipe.setIngredients(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTS)));
                recipe.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTIONS)));
                recipe.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))); // 👈 faltava isso!
                recipes.add(recipe);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return recipes;
    }


    // Buscar receita por ID
    public Recipe getRecipeById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Recipe recipe = null;

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES + " WHERE " + COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (cursor != null && cursor.moveToFirst()) {
            recipe = new Recipe();
            recipe.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
            recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            recipe.setIngredients(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTS)));
            recipe.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTIONS)));
        }

        if (cursor != null) cursor.close();
        db.close();
        return recipe;
    }
    public List<Recipe> getRecipesByType(String type) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES + " WHERE " + COLUMN_TYPE + " = ?", new String[]{type});

        if (cursor.moveToFirst()) {
            do {
                Recipe recipe = new Recipe();
                recipe.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                recipe.setIngredients(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTS)));
                recipe.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTIONS)));
                recipe.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE))); // novo campo
                recipes.add(recipe);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return recipes;
    }

}
