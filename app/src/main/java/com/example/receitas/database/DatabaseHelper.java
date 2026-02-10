package com.example.receitas.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.receitas.model.Recipe;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "recipes.db";
    private static final int DATABASE_VERSION = 5;

    // Tabela de receitas
    private static final String TABLE_RECIPES = "recipes";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_INGREDIENTS = "ingredients";
    private static final String COLUMN_INSTRUCTIONS = "instructions";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_USER_ID_FK = "user_id";
    private static final String COLUMN_MAIN_IMAGE = "main_image";
    private static final String COLUMN_STEP_IMAGES = "step_images";

    // Tabela de usuários (Mantida apenas para leitura legada)
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_PASSWORD = "password";

    // SQL Criação (Mantido igual)
    private static final String CREATE_USERS_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT NOT NULL, " +
                    COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_PASSWORD + " TEXT NOT NULL);";

    private static final String CREATE_RECIPES_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_RECIPES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME + " TEXT NOT NULL, " +
                    COLUMN_INGREDIENTS + " TEXT, " +
                    COLUMN_INSTRUCTIONS + " TEXT, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_MAIN_IMAGE + " TEXT, " +
                    COLUMN_STEP_IMAGES + " TEXT, " +
                    COLUMN_USER_ID_FK + " INTEGER, " +
                    "FOREIGN KEY(" + COLUMN_USER_ID_FK + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USER_ID + "));";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_RECIPES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COLUMN_MAIN_IMAGE + " TEXT;");
                db.execSQL("ALTER TABLE " + TABLE_RECIPES + " ADD COLUMN " + COLUMN_STEP_IMAGES + " TEXT;");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ===================== USUÁRIOS =====================

    public long registerUser(String username, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS +
                        " WHERE " + COLUMN_EMAIL + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{email, password});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    public int getUserId(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_USER_ID +
                        " FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + "=?",
                new String[]{email});
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
        }
        cursor.close();
        db.close();
        return userId;
    }

    // NOVO MÉTODO: Busca o nome do usuário pelo ID (Usado no PDF)
    public String getUserName(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String name = "";
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_USERNAME + " FROM " + TABLE_USERS + " WHERE " + COLUMN_USER_ID + "=?", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            name = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return name;
    }

    // ===================== RECEITAS =====================

    public void addRecipe(Recipe recipe, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, recipe.getName());
        values.put(COLUMN_INGREDIENTS, recipe.getIngredients());
        values.put(COLUMN_INSTRUCTIONS, recipe.getInstructions());
        values.put(COLUMN_TYPE, recipe.getType());
        values.put(COLUMN_USER_ID_FK, userId);
        values.put(COLUMN_MAIN_IMAGE, recipe.getMainImageUri());

        db.insert(TABLE_RECIPES, null, values);
        db.close();
    }

    public void updateRecipe(Recipe recipe) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, recipe.getName());
        values.put(COLUMN_INGREDIENTS, recipe.getIngredients());
        values.put(COLUMN_INSTRUCTIONS, recipe.getInstructions()); // JSON de Passos
        values.put(COLUMN_TYPE, recipe.getType());
        values.put(COLUMN_MAIN_IMAGE, recipe.getMainImageUri());

        // CORREÇÃO AQUI: Use getSqliteId() em vez de getId()
        db.update(TABLE_RECIPES, values, COLUMN_ID + " = ?", new String[]{String.valueOf(recipe.getSqliteId())});

        db.close();
    }

    public void deleteRecipe(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_RECIPES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES, null);

        if (cursor.moveToFirst()) {
            do {
                recipes.add(buildRecipeFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return recipes;
    }

    public Recipe getRecipeById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Recipe recipe = null;
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES + " WHERE " + COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (cursor != null && cursor.moveToFirst()) {
            recipe = buildRecipeFromCursor(cursor);
        }
        if (cursor != null) cursor.close();
        db.close();
        return recipe;
    }



    public List<Recipe> getRecipesByTypeAndUser(String type, int userId) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + TABLE_RECIPES +
                " WHERE " + COLUMN_TYPE + " = ?" +
                " AND " + COLUMN_USER_ID_FK + " = ?";

        Cursor cursor = db.rawQuery(selectQuery, new String[]{type, String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                recipes.add(buildRecipeFromCursor(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return recipes;
    }

    public List<Recipe> getRecipesByType(String type) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES + " WHERE " + COLUMN_TYPE + " = ?", new String[]{type});
        if (cursor.moveToFirst()) {
            do {
                recipes.add(buildRecipeFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return recipes;
    }

    public List<Recipe> getRecipesByUser(int userId) {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_RECIPES + " WHERE " + COLUMN_USER_ID_FK + " = ?", new String[]{String.valueOf(userId)});
        if (cursor.moveToFirst()) {
            do {
                recipes.add(buildRecipeFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return recipes;
    }

    private Recipe buildRecipeFromCursor(Cursor cursor) {
        Recipe recipe = new Recipe();

        // CORREÇÃO 1: Usar setSqliteId (pois getId não existe mais ou é string)
        recipe.setSqliteId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));

        recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
        recipe.setIngredients(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INGREDIENTS)));
        recipe.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_INSTRUCTIONS)));
        recipe.setType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)));

        // CORREÇÃO 2: Ignoramos o setUserId antigo, pois agora usamos ownerId (String).
        // A MainActivity vai definir o ownerId atual ao migrar.

        recipe.setMainImageUri(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MAIN_IMAGE)));
        recipe.setStepImagesJson(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STEP_IMAGES)));

        return recipe;
    }
}