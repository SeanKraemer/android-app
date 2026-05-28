package edu.uiuc.cs427app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * DetailsActivity displays detailed weather information for a selected city.
 *
 * This activity:
 * 1. Receives city name from MainActivity via Intent
 * 2. Displays weather information with theme support
 * 3. Provides navigation to Weather Insights (LLM Q&A)
 */
public class DetailsActivity extends AppCompatActivity implements View.OnClickListener {

    // City information
    private String cityName;

    // Database helper for user themes
    private UserDatabaseHelper dbHelper;

    // Weather API service
    private WeatherApiService weatherApiService;

    // Weather data (these will be populated by weather API integration)
    private String temperature;
    private String weatherCondition;
    private String humidity;
    private String windCondition;
    private String dateTime;

    // Theme colors
    private int themeBackgroundColor;
    private int themeTextColor;
    private int themeButtonColor;
    private int themeToolbarColor;
    private int themeAccentColor;

    // SharedPreferences constants
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_CURRENT_USER = "current_user";

    // UI Components
    private TextView welcomeMessage;
    private TextView cityInfoMessage;
    private TextView dateTimeText;
    private Button buttonWeatherInsights;
    private ImageView weatherImageView;

    /**
     * Lifecycle callback invoked when the activity is being created.
     * Initializes the weather API service, sets up the UI, loads user theme preferences,
     * and fetches weather data for the selected city.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved state,
     *                          or null if this is a newly created activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Weather API service
        weatherApiService = new WeatherApiService(BuildConfig.OPENWEATHERMAP_API_KEY);

        setContentView(R.layout.activity_details);

        // Initialize database helper
        dbHelper = new UserDatabaseHelper(this);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Load user theme
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String username = sharedPreferences.getString(KEY_CURRENT_USER, "User");
        UserDatabaseHelper.User user = dbHelper.getUserTheme(username);

        String backgroundColor = user != null && user.themeBackground != null
                ? user.themeBackground : "#FFFFFF";
        String textColor = user != null && user.themeText != null
                ? user.themeText : "#000000";
        String buttonColor = user != null && user.themeButton != null
                ? user.themeButton : "#4CAF50";
        String toolbarColor = user != null && user.themeToolbar != null
                ? user.themeToolbar : "#4CAF50";
        String accentColor = user != null && user.themeAccent != null
                ? user.themeAccent : "#2196F3";

        themeBackgroundColor = Color.parseColor(backgroundColor);
        themeTextColor = Color.parseColor(textColor);
        themeButtonColor = Color.parseColor(buttonColor);
        themeToolbarColor = Color.parseColor(toolbarColor);
        themeAccentColor = Color.parseColor(accentColor);

        // Get the city name from the Intent that opened this Activity
        cityName = getIntent().getStringExtra("city");
        if (cityName == null) {
            cityName = "";
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(cityName);
            }
        }

        // Initialize UI elements
        initializeViews();

        // Apply theme
        applyTheme(toolbar);

        // Fetch weather data from API
        fetchWeatherData();
    }

    /**
     * Initializes all UI components by finding them in the layout.
     * Sets up click listeners for interactive elements.
     */
    private void initializeViews() {
        welcomeMessage = findViewById(R.id.welcomeText);
        cityInfoMessage = findViewById(R.id.cityInfo);
        dateTimeText = findViewById(R.id.dateTimeText);
        buttonWeatherInsights = findViewById(R.id.weatherInsightsButton);
        weatherImageView = findViewById(R.id.weatherImageView);

        // Set click listeners
        buttonWeatherInsights.setOnClickListener(this);
    }

    /**
     * Applies the user's theme to all UI components.
     * Updates colors for text views, buttons, toolbar, and background based on
     * user preferences loaded from the database.
     *
     * @param toolbar The toolbar to apply theme colors to
     */
    private void applyTheme(Toolbar toolbar) {
        welcomeMessage.setTextColor(themeTextColor);
        cityInfoMessage.setTextColor(themeTextColor);
        if (dateTimeText != null) {
            dateTimeText.setTextColor(themeTextColor);
        }
        buttonWeatherInsights.setBackgroundTintList(ColorStateList.valueOf(themeButtonColor));
        buttonWeatherInsights.setTextColor(themeAccentColor);
        toolbar.setBackgroundColor(themeToolbarColor);
        getWindow().getDecorView().setBackgroundColor(themeBackgroundColor);
    }

    /**
     * Fetches weather data for the selected city using OpenWeatherMap API.
     * Displays loading state while fetching, then updates UI with results or error message.
     * Also triggers weather image generation upon successful data retrieval.
     */
    private void fetchWeatherData() {
        // Show loading state
        cityInfoMessage.setText("Loading weather data...");

        // Fetch weather data from API
        weatherApiService.getCurrentWeather(cityName, new WeatherApiService.WeatherCallback() {
            /**
             * Callback invoked when the WeatherApiService successfully fetches and parses
             * weather data from the OpenWeatherMap API.
             * Extracts weather values from the WeatherData object and updates the UI display.
             * Also triggers the generation of an AI-generated weather image.
             *
             * @param weatherData The weather data object containing temperature, conditions,
             *                   humidity, wind speed, and local date/time for the requested city
             */
            @Override
            public void onSuccess(WeatherData weatherData) {
                temperature = weatherData.getTemperatureFahrenheit();
                weatherCondition = weatherData.getWeatherCondition();
                humidity = weatherData.getHumidity();
                windCondition = weatherData.getWindCondition();
                dateTime = weatherData.getLocalDateTime();

                // Update UI with fetched data
                updateWeatherDisplay();
                // Generate and display weather image
                generateAndDisplayWeatherImage();
            }

            /**
             * Callback invoked when the WeatherApiService encounters an error while fetching
             * or parsing weather data from the OpenWeatherMap API.
             * Displays error message to user via toast notification and updates UI with
             * error information and troubleshooting guidance.
             *
             * @param errorMessage Description of the error that occurred, such as network
             *                    connectivity issues, API errors, invalid city name, or
             *                    JSON parsing failures
             */
            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DetailsActivity.this,
                        "Failed to fetch weather data: " + errorMessage,
                        Toast.LENGTH_LONG).show();

                // Display error message in UI
                welcomeMessage.setText("Unable to load weather data");
                cityInfoMessage.setText("Error: " + errorMessage
                        + "\n\nPlease check your internet connection and try again.");
                dateTimeText.setText("");
            }
        });
    }

    /**
     * Generates and displays an AI-generated weather image based on current weather conditions.
     * Uses the Gemini API to create a contextual image representing the weather in the city.
     * Updates the ImageView with the generated image or displays an error toast on failure.
     */
    private void generateAndDisplayWeatherImage() {
        WeatherImageGenerator.generateImage(
                BuildConfig.GEMINI_API_KEY,
                dateTime,
                cityName,
                temperature,
                weatherCondition,
                humidity,
                windCondition,
                new WeatherImageGenerator.ImageCallback() {
                    /**
                     * Callback invoked when the WeatherImageGenerator successfully generates
                     * an AI-powered weather image using the Gemini API.
                     * Decodes the Base64-encoded image bytes and displays the bitmap in the
                     * weather ImageView on the UI thread.
                     *
                     * @param image The WeatherImage object containing the generated image as
                     *             a byte array in Base64 format, representing the current
                     *             weather conditions visually
                     */
                    @Override
                    public void onImageGenerated(WeatherImageGenerator.WeatherImage image) {
                        runOnUiThread(() -> {
                            // Decode Base64 string to Bitmap
                            Bitmap decodedByte = BitmapFactory.decodeByteArray(
                                    image.imageBytes, 0, image.imageBytes.length);
                            weatherImageView.setImageBitmap(decodedByte);
                        });
                    }

                    /**
                     * Callback invoked when the WeatherImageGenerator encounters an error
                     * while generating the AI-powered weather image. Displays an error.
                     *
                     * @param e The exception that occurred during image generation, which may
                     *         include API errors, network failures, or image processing issues
                     */
                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(DetailsActivity.this,
                                    "Failed to generate weather image",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * Updates the UI to display the current weather information.
     * Formats and displays city name, date/time, temperature, weather conditions,
     * humidity, and wind information in the appropriate TextViews.
     */
    private void updateWeatherDisplay() {
        // Update city name in welcome message
        String welcome = "Welcome to " + cityName;
        welcomeMessage.setText(welcome);

        // Update date and time
        if (dateTimeText != null && dateTime != null) {
            dateTimeText.setText(dateTime);
        }

        // Format and display weather data
        String weatherInfo = String.format(
                "Weather: %s\nTemperature: %s\nHumidity: %s\nWind: %s",
                weatherCondition, temperature, humidity, windCondition
        );
        cityInfoMessage.setText(weatherInfo);
    }

    /**
     * Handles button click events throughout the activity.
     * Routes to appropriate actions based on which button was clicked.
     *
     * @param view The view (button) that was clicked by the user
     */
    @Override
    public void onClick(View view) {
        int viewId = view.getId();

        if (viewId == R.id.weatherInsightsButton) {
            // Navigate to Weather Insights activity
            openWeatherInsights();
        }
    }

    /**
     * Opens the Weather Insights activity and passes current weather data.
     * This enables the LLM to generate context-relevant questions and answers
     * based on the current weather conditions in the selected city.
     * Validates that weather data is available before navigating.
     */
    private void openWeatherInsights() {
        // Validate that we have weather data before navigating
        if (temperature == null || weatherCondition == null) {
            Toast.makeText(this, "Weather data not available yet",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Create Intent to open WeatherInsightsActivity
        Intent intent = new Intent(this, WeatherInsightsActivity.class);

        // Pass all weather data as Intent extras
        intent.putExtra("cityName", cityName);
        intent.putExtra("temperature", temperature);
        intent.putExtra("weatherCondition", weatherCondition);
        intent.putExtra("humidity", humidity);
        intent.putExtra("windCondition", windCondition);

        // Start the Weather Insights activity
        startActivity(intent);
    }

    /**
     * Handles the toolbar back button being pressed.
     * Navigates back to the previous activity (MainActivity).
     *
     * @return true to indicate that the navigation was handled successfully
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * Lifecycle callback invoked when the activity is being destroyed.
     * Performs cleanup operations including shutting down the weather API service
     * to prevent memory leaks and release network resources.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (weatherApiService != null) {
            weatherApiService.shutdown();
        }
    }
}