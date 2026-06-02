package com.weatherplanner.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * UserDatabaseHelper manages the SQLite database for storing user accounts.
 * This class handles creating the database, adding users, validating credentials,
 * and retrieving user information.
 */
public class UserDatabaseHelper extends SQLiteOpenHelper {

    // Database configuration
    private static final String DATABASE_NAME = "WeatherApp.db";
    private static final int DATABASE_VERSION = 3;

    // Table and column names
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_THEME_BACKGROUND = "theme_background";
    private static final String COLUMN_THEME_TEXT = "theme_text";
    private static final String COLUMN_THEME_ACCENT = "theme_accent";
    private static final String COLUMN_THEME_BUTTON = "theme_button";
    private static final String COLUMN_THEME_TOOLBAR = "theme_toolbar";
    private static final String COLUMN_THEME_PROMPT = "theme_prompt";

    /**
     * Constructor for UserDatabaseHelper.
     *
     * @param context The application context
     */
    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * Creates the users table with all necessary columns.
     *
     * @param db The database to create the table in
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_USERNAME + " TEXT UNIQUE,"
                + COLUMN_PASSWORD + " TEXT,"
                + COLUMN_THEME_BACKGROUND + " TEXT,"
                + COLUMN_THEME_TEXT + " TEXT,"
                + COLUMN_THEME_ACCENT + " TEXT,"
                + COLUMN_THEME_BUTTON + " TEXT,"
                + COLUMN_THEME_TOOLBAR + " TEXT,"
                + COLUMN_THEME_PROMPT + " TEXT"
                + ")";
        db.execSQL(CREATE_USERS_TABLE);
    }

    /**
     * Called when the database needs to be upgraded.
     * Drops the old table and creates a new one.
     *
     * @param db The database to upgrade
     * @param oldVersion The old database version
     * @param newVersion The new database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // If the old database is version 1, it needs all the version 2 changes
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_THEME_TOOLBAR + " TEXT");
            // Add any other changes that were part of version 2
        }

        // If the old database is version 2, it only needs the version 3 changes
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COLUMN_THEME_PROMPT + " TEXT");
        }
    }

    /**
     * Adds a new user to the database.
     * Returns true if successful, false if username already exists.
     *
     * @param username The username for the new account
     * @param password The password for the new account
     * @param themeBackground The background color for the user's theme
     * @param themeText The text color for the user's theme
     * @param themeAccent The accent color for the user's theme
     * @param themeButton The button color for the user's theme
     * @return true if user was added successfully, false if username already exists
     */
    public boolean addUser(String username, String password, String themeBackground,
                           String themeText, String themeAccent, String themeButton,
                           String themeToolbar, String themePrompt) {
        // Check if username already exists
        if (userExists(username)) {
            return false;
        }

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);
        values.put(COLUMN_THEME_BACKGROUND, themeBackground);
        values.put(COLUMN_THEME_TEXT, themeText);
        values.put(COLUMN_THEME_ACCENT, themeAccent);
        values.put(COLUMN_THEME_BUTTON, themeButton);
        values.put(COLUMN_THEME_TOOLBAR, themeToolbar);
        values.put(COLUMN_THEME_PROMPT, themePrompt);

        long result = db.insert(TABLE_USERS, null, values);
        db.close();

        return result != -1; // Returns true if insert was successful
    }

    /**
     * Checks if a username already exists in the database.
     *
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    public boolean userExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USERNAME},
                COLUMN_USERNAME + "=?",
                new String[]{username},
                null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * Validates user credentials.
     * Checks if the provided username and password match a record in the database.
     *
     * @param username The username to validate
     * @param password The password to validate
     * @return true if credentials are valid, false otherwise
     */
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USERNAME},
                COLUMN_USERNAME + "=? AND " + COLUMN_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null);

        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return isValid;
    }

    /**
     * Retrieves a user's theme information from the database.
     *
     * @param username The username whose theme to retrieve
     * @return A User object containing the theme information, or null if user not found
     */
    public User getUserTheme(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COLUMN_USERNAME, COLUMN_THEME_BACKGROUND, COLUMN_THEME_TEXT,
                        COLUMN_THEME_ACCENT, COLUMN_THEME_BUTTON, COLUMN_THEME_TOOLBAR,
                        COLUMN_THEME_PROMPT},
                COLUMN_USERNAME + "=?",
                new String[]{username},
                null, null, null);

        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.username = cursor.getString(0);
            user.themeBackground = cursor.getString(1);
            user.themeText = cursor.getString(2);
            user.themeAccent = cursor.getString(3);
            user.themeButton = cursor.getString(4);
            user.themeToolbar = cursor.getString(5);
            user.themePrompt = cursor.getString(6);
        }

        cursor.close();
        db.close();
        return user;
    }

    /**
     * Updates an existing user's theme colors and theme prompt.
     *
     * @param username The user to update
     * @param background New background color hex
     * @param text New text color hex
     * @param accent New accent color hex
     * @param button New button color hex
     * @param toolbar New toolbar color hex
     * @param themePrompt New theme prompt string
     */
    public void updateUserTheme(String username, String background, String text, String accent, String button, String toolbar, String themePrompt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_THEME_BACKGROUND, background);
        values.put(COLUMN_THEME_TEXT, text);
        values.put(COLUMN_THEME_ACCENT, accent);
        values.put(COLUMN_THEME_BUTTON, button);
        values.put(COLUMN_THEME_TOOLBAR, toolbar);
        values.put(COLUMN_THEME_PROMPT, themePrompt);

        db.update(TABLE_USERS, values, COLUMN_USERNAME + "=?", new String[]{username});
        db.close();
    }


    /**
     * Simple User class to hold user information.
     */
    public static class User {
        public String username;
        public String themeBackground;
        public String themeText;
        public String themeAccent;
        public String themeButton;
        public String themeToolbar;
        public String themePrompt;
    }
}