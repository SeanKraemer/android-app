package com.weatherplanner.app;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * WeatherInsightsActivity provides an LLM-powered Q&A interface for weather insights.
 *
 * This activity:
 * 1. Receives weather data from the previous activity via Intent extras
 * 2. Uses Gemini LLM to generate context-relevant questions
 * 3. Displays questions as interactive buttons
 * 4. Generates and displays personalized answers when questions are clicked
 *
 * All questions and answers are dynamically generated - nothing is hardcoded.
 */
public class WeatherInsightsActivity extends AppCompatActivity {

    private static final String TAG = "WeatherInsightsActivity";

    // UI Components
    private TextView cityNameText;
    private TextView weatherSummaryText;
    private LinearLayout questionsContainer;
    private TextView answerTitleText;
    private TextView answerText;
    private ScrollView answerScrollView;
    private ProgressBar loadingSpinner;
    private TextView loadingText;
    private TextView questionText;

    // Weather data received from previous activity
    private String cityName;
    private String temperature;
    private String weatherCondition;
    private String humidity;
    private String windCondition;

    // Theme colors
    private String buttonColor;
    private String accentColor;
    private String textColor;
    private String backgroundColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_insights);

        // Initialize UI components
        initializeViews();

        // Load theme colors
        loadThemeColors();

        // Apply theme
        applyTheme();

        // Get weather data from Intent
        retrieveWeatherData();

        // Display weather summary
        displayWeatherSummary();

        // Generate questions using LLM
        generateWeatherQuestions();
    }

    /**
     * Initializes all UI components by finding them in the layout.
     * This method should be called first in onCreate().
     */
    private void initializeViews() {
        cityNameText = findViewById(R.id.insightsCityName);
        weatherSummaryText = findViewById(R.id.weatherSummary);
        questionsContainer = findViewById(R.id.questionsContainer);
        answerTitleText = findViewById(R.id.answerTitle);
        answerText = findViewById(R.id.answerText);
        answerScrollView = findViewById(R.id.answerScrollView);
        loadingSpinner = findViewById(R.id.loadingSpinner);
        loadingText = findViewById(R.id.loadingText);
        questionText = findViewById(R.id.questionText);

        // Initially hide answer section
        answerScrollView.setVisibility(View.GONE);
        answerTitleText.setVisibility(View.GONE);
    }

    /**
     * Loads the user's theme colors from SharedPreferences.
     * These colors were set during login/signup with LLM theme generation.
     */
    private void loadThemeColors() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("current_user", "User");

        // Initialize database helper to get theme from database
        UserDatabaseHelper dbHelper = new UserDatabaseHelper(this);
        UserDatabaseHelper.User user = dbHelper.getUserTheme(username);

        // Load theme from database or use defaults
        if (user != null) {
            backgroundColor = user.themeBackground != null ? user.themeBackground : "#FFFFFF";
            textColor = user.themeText != null ? user.themeText : "#000000";
            accentColor = user.themeAccent != null ? user.themeAccent : "#2196F3";
            buttonColor = user.themeButton != null ? user.themeButton : "#4CAF50";
        } else {
            // Fallback to defaults if user not found
            backgroundColor = "#FFFFFF";
            textColor = "#000000";
            accentColor = "#2196F3";
            buttonColor = "#4CAF50";
        }

    }

    /**
     * Applies the loaded theme colors to all UI components.
     */
    private void applyTheme() {
        // Apply background color to main view
        getWindow().getDecorView().setBackgroundColor(Color.parseColor(backgroundColor));

        // Apply text colors
        if (cityNameText != null) cityNameText.setTextColor(Color.parseColor(textColor));
        if (weatherSummaryText != null) weatherSummaryText.setTextColor(Color.parseColor(textColor));
        if (answerTitleText != null) answerTitleText.setTextColor(Color.parseColor(accentColor));
        if (answerText != null) answerText.setTextColor(Color.parseColor(textColor));
        if (loadingText != null) loadingText.setTextColor(Color.parseColor(textColor));
        if (answerText != null) answerText.setBackgroundColor(Color.parseColor(backgroundColor));
        if (questionText != null) questionText.setTextColor(Color.parseColor(textColor));
    }

    /**
     * Retrieves weather data from the Intent extras.
     * This data is passed from the WeatherActivity (or DetailsActivity).
     */
    private void retrieveWeatherData() {
        cityName = getIntent().getStringExtra("cityName");
        temperature = getIntent().getStringExtra("temperature");
        weatherCondition = getIntent().getStringExtra("weatherCondition");
        humidity = getIntent().getStringExtra("humidity");
        windCondition = getIntent().getStringExtra("windCondition");


        // Validate that we received the necessary data
        if (cityName == null || temperature == null || weatherCondition == null) {
            Log.e(TAG, "Missing required weather data!");
            Toast.makeText(this, "Error: Missing weather data", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if data is missing
        }
    }

    /**
     * Displays a summary of the current weather at the top of the screen.
     */
    private void displayWeatherSummary() {
        cityNameText.setText(cityName);

        String summary = String.format("Current Weather: %s, %s\nHumidity: %s | Wind: %s",
                weatherCondition, temperature, humidity, windCondition);
        weatherSummaryText.setText(summary);
    }

    /**
     * Generates weather-related questions using the LLM.
     * Shows a loading spinner while questions are being generated.
     * Once generated, displays questions as clickable buttons.
     */
    private void generateWeatherQuestions() {
        // Show loading state
        loadingSpinner.setVisibility(View.VISIBLE);
        loadingText.setVisibility(View.VISIBLE);
        loadingText.setText("Generating questions...");
        questionsContainer.setVisibility(View.GONE);


        // Call LLM to generate questions
        WeatherInsightsGenerator.generateQuestions(
                BuildConfig.GEMINI_API_KEY,
                cityName,
                temperature,
                weatherCondition,
                humidity != null ? humidity : "N/A",
                windCondition != null ? windCondition : "N/A",
                new WeatherInsightsGenerator.QuestionCallback() {
                    /**
                     * Called when LLM successfully generates weather-related questions.
                     * Updates UI on main thread to display question buttons.
                     * @param questions QuestionSet containing list of generated questions
                     */
                    @Override
                    public void onQuestionsGenerated(WeatherInsightsGenerator.QuestionSet questions) {
                        // Run on UI thread since callback may be on background thread
                        runOnUiThread(() -> {

                            // Hide loading state
                            loadingSpinner.setVisibility(View.GONE);
                            loadingText.setVisibility(View.GONE);
                            questionsContainer.setVisibility(View.VISIBLE);

                            // Display the generated questions as buttons
                            displayQuestionButtons(questions.questions);
                        });
                    }

                    /**
                     * Called when question generation fails due to API errors or network issues.
                     * Displays error message and hides loading state.
                     * @param e Exception describing the failure
                     */
                    @Override
                    public void onError(Exception e) {
                        // Handle error on UI thread
                        runOnUiThread(() -> {
                            Log.e(TAG, "Failed to generate questions", e);

                            // Hide loading state
                            loadingSpinner.setVisibility(View.GONE);
                            loadingText.setVisibility(View.GONE);

                            // Show error message
                            Toast.makeText(WeatherInsightsActivity.this,
                                    "Failed to generate questions. Please try again.",
                                    Toast.LENGTH_LONG).show();

                            // Show fallback message
                            loadingText.setText("Unable to generate questions at this time.");
                            loadingText.setVisibility(View.VISIBLE);
                        });
                    }
                }
        );
    }

    /**
     * Creates and displays buttons for each generated question.
     * Each button, when clicked, will generate an answer for that question.
     *
     * @param questions List of question strings to display
     */
    private void displayQuestionButtons(List<String> questions) {
        // Clear any existing buttons
        questionsContainer.removeAllViews();


        // Create a button for each question
        for (int i = 0; i < questions.size(); i++) {
            final String question = questions.get(i);
            final int questionNumber = i + 1;

            // Create button
            Button questionButton = new Button(this);
            questionButton.setText(question);
            questionButton.setAllCaps(false); // Keep original text case

            // Apply theme colors to button
            questionButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(buttonColor)));
            questionButton.setTextColor(Color.parseColor(accentColor));

            // Set button layout parameters
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16); // Add bottom margin between buttons
            questionButton.setLayoutParams(params);

            // Set click listener to generate answer for this question
            questionButton.setOnClickListener(v -> {
                generateAnswerForQuestion(question, questionNumber);
            });

            // Add button to container
            questionsContainer.addView(questionButton);
        }
    }

    /**
     * Generates an answer for the selected question using LLM.
     * Displays a loading state while the answer is being generated.
     *
     * @param question The question text
     * @param questionNumber The question number (for display purposes)
     */
    private void generateAnswerForQuestion(String question, int questionNumber) {
        // Show loading state in answer area
        answerTitleText.setVisibility(View.VISIBLE);
        answerTitleText.setText("Question " + questionNumber);
        answerScrollView.setVisibility(View.VISIBLE);
        answerText.setText("Generating answer...");


        // Call LLM to generate answer
        WeatherInsightsGenerator.generateAnswer(
                BuildConfig.GEMINI_API_KEY,
                cityName,
                temperature,
                weatherCondition,
                humidity != null ? humidity : "N/A",
                windCondition != null ? windCondition : "N/A",
                question,
                new WeatherInsightsGenerator.AnswerCallback() {
                    /**
                     * Called when LLM successfully generates an answer to the question.
                     * Formats and displays the Q&A pair in the answer TextView.
                     * @param answer Answer object containing the generated response text
                     */
                    @Override
                    public void onAnswerGenerated(WeatherInsightsGenerator.Answer answer) {
                        // Display answer on UI thread
                        runOnUiThread(() -> {

                            // Format and display the answer
                            String formattedAnswer = "Q: " + question + "\n\nA: " + answer.answer;
                            answerText.setText(formattedAnswer);
                        });
                    }

                    /**
                     * Called when answer generation fails due to API errors or network issues.
                     * Displays error message in answer area and shows toast notification.
                     * @param e Exception describing the failure
                     */
                    @Override
                    public void onError(Exception e) {
                        // Handle error on UI thread
                        runOnUiThread(() -> {
                            Log.e(TAG, "Failed to generate answer", e);

                            // Show error message in answer area
                            answerText.setText("Unable to generate answer at this time. Please try again.");

                            Toast.makeText(WeatherInsightsActivity.this,
                                    "Failed to generate answer",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }
}