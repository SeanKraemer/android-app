package com.weatherplanner.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * SignUpActivity handles the user account creation process.
 * Users can enter their desired username and password, select a UI layout preference,
 * and create a new account. Accounts are stored in an SQLite database.
 */
public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    // UI components
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button signUpButton;
    private Button backButton;
    private EditText themeDescriptionInput;

    // Database helper
    private UserDatabaseHelper dbHelper;

    // SharedPreferences for storing current logged-in user
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";

    /**
     * Lifecycle callback invoked when the activity is being created.
     * Initializes the UI, database helper, and SharedPreferences.
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize database helper
        dbHelper = new UserDatabaseHelper(this);
        dbHelper.getWritableDatabase();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize UI components
        usernameInput = findViewById(R.id.signupUsernameInput);
        passwordInput = findViewById(R.id.signupPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        themeDescriptionInput = findViewById(R.id.themeDescriptionInput);
        signUpButton = findViewById(R.id.createAccountButton);
        backButton = findViewById(R.id.backButton);

        // Set click listeners
        signUpButton.setOnClickListener(this);
        backButton.setOnClickListener(this);
    }

    /**
     * Handles click events for the Sign Up and Back buttons.
     *
     * @param view The view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.createAccountButton) {
            handleCreateAccount();
        } else if (view.getId() == R.id.backButton) {
            // Return to LoginActivity
            finish();
        }
    }

    /**
     * Handles the account creation process.
     * Validates input, checks if account already exists in database, and stores credentials.
     * After successful account creation, redirects to LoginActivity.
     */
    private void handleCreateAccount() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        String themePrompt = themeDescriptionInput.getText().toString().trim();

        // Validate that all fields are filled
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate username length (at least 3 characters)
        if (username.length() < 3) {
            Toast.makeText(this, "Username must be at least 3 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password length (at least 4 characters)
        if (password.length() < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if account already exists in database
        if (dbHelper.userExists(username)) {
            Toast.makeText(this, "Username already exists. Please choose another.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate theme and create account
        ThemeGenerator.generateTheme(BuildConfig.GEMINI_API_KEY, themePrompt, new ThemeGenerator.ThemeCallback() {

            /**
             * Handles the completion of theme generation.
             * If generation is successful, adds the user to the database.
             *
             * @param genTheme
             */
            @Override
            public void onThemeGenerated(ThemeGenerator.ThemeSpec genTheme) {
                runOnUiThread(() -> {
                    ThemeGenerator.ThemeSpec themeSpec = genTheme;

                    // Use default theme if generation failed
                    if (themeSpec == null || themeSpec.backgroundColor == null) {
                        themeSpec = new ThemeGenerator.ThemeSpec();
                        themeSpec.backgroundColor = "#FFFFFF";
                        themeSpec.textColor = "#000000";
                        themeSpec.accentColor = "#2196F3";
                        themeSpec.buttonColor = "#4CAF50";
                        themeSpec.toolbarColor = "#00EEFF";
                        Log.d("SignUpActivity", "Using default theme values");
                    }

                    // Add user to database
                    boolean success = dbHelper.addUser(
                            username,
                            password,
                            themeSpec.backgroundColor,
                            themeSpec.textColor,
                            themeSpec.accentColor,
                            themeSpec.buttonColor,
                            themeSpec.toolbarColor,
                            themePrompt
                    );

                    if (success) {
                        Log.d("SignUpActivity", "User created successfully: " + username);
                        Toast.makeText(SignUpActivity.this, "Account created successfully! Please sign in.", Toast.LENGTH_SHORT).show();

                        // Redirect back to LoginActivity
                        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this, "Error creating account. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            /**
             * Handles errors during theme generation.
             * If generation fails, uses default theme and still creates the account.
             *
             * @param e Exception
             */
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Log.e("SignUpActivity", "Error generating theme", e);

                    // Use default theme and still create account
                    boolean success = dbHelper.addUser(
                            username,
                            password,
                            "#FFFFFF", // Default background
                            "#000000", // Default text
                            "#2196F3", // Default accent
                            "#4CAF50",  // Default button
                            "#00EEFF",
                            "default"
                    );

                    if (success) {
                        Toast.makeText(SignUpActivity.this, "Account created with default theme. Please sign in.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this, "Error creating account. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}