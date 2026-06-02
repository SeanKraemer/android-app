package com.weatherplanner.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * LoginActivity handles user authentication.
 * Users can sign in with existing credentials stored in the SQLite database.
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    // UI components
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText themeDescriptionInput;

    // Database helper
    private UserDatabaseHelper dbHelper;

    // SharedPreferences for storing current logged-in user
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_CURRENT_USER = "current_user";

    /**
     * Lifecycle callback invoked when the activity is being created.
     * Initializes the UI, database helper, and SharedPreferences.
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize database helper
        dbHelper = new UserDatabaseHelper(this);
        dbHelper.getWritableDatabase();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize UI components
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        Button signInButton = findViewById(R.id.signInButton);
        TextView signUpLink = findViewById(R.id.signUpLink);
        themeDescriptionInput = findViewById(R.id.themeDescriptionInput);

        // Make the Sign Up link underlined and clickable
        signUpLink.setPaintFlags(signUpLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        signUpLink.setClickable(true);

        // Set click listeners for buttons
        signInButton.setOnClickListener(this);
        signUpLink.setOnClickListener(this);
    }

    /**
     * Handles button clicks for sign in and navigation to sign up.
     *
     * @param view The view that was clicked
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.signInButton) {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            // Validate input
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            handleSignIn(username, password);
        } else if (view.getId() == R.id.signUpLink) {
            // Navigate to SignUpActivity when sign up link is clicked
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        }
    }

    /**
     * Handles the sign in process.
     * Verifies that the username and password match a record in the database.
     * If successful, stores the current user in SharedPreferences and redirects to MainActivity.
     *
     * @param username The username entered by the user
     * @param password The password entered by the user
     */
    private void handleSignIn(String username, String password) {
        // Validate credentials against database
        if (!dbHelper.validateUser(username, password)) {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
            return;
        }
        String themePrompt = themeDescriptionInput.getText().toString().trim();

        if (themePrompt.isEmpty()) {
            // No new theme selected — just log in normally
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_CURRENT_USER, username);
            editor.apply();

            Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show();
            redirectToMainActivity();
            return;
        }

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
                    String promptToSave;

                    if (themeSpec == null || themeSpec.backgroundColor == null) {
                        themeSpec = new ThemeGenerator.ThemeSpec();
                        themeSpec.backgroundColor = "#FFFFFF";
                        themeSpec.textColor = "#000000";
                        themeSpec.accentColor = "#2196F3";
                        themeSpec.buttonColor = "#4CAF50";
                        themeSpec.toolbarColor = "#00EEFF";
                        promptToSave = "default";
                    } else {
                        promptToSave = themePrompt;
                    }

                    // Update DB
                    dbHelper.updateUserTheme(username,
                            themeSpec.backgroundColor,
                            themeSpec.textColor,
                            themeSpec.accentColor,
                            themeSpec.buttonColor,
                            themeSpec.toolbarColor,
                            promptToSave
                    );

                    // Save current user & theme locally
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_CURRENT_USER, username);
                    editor.apply();

                    Toast.makeText(LoginActivity.this, "Theme updated! Logging in…", Toast.LENGTH_SHORT).show();
                    redirectToMainActivity();
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
                    Toast.makeText(LoginActivity.this, "Login succeeded but theme generation failed. Using previous theme.", Toast.LENGTH_LONG).show();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_CURRENT_USER, username);
                    editor.apply();

                    redirectToMainActivity();
                });
            }
        });
    }

    /**
     * Redirects the user to MainActivity after successful authentication.
     * This method finishes the current activity to prevent going back to the login screen.
     */
    private void redirectToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Prevents user from going back to login screen
    }
}