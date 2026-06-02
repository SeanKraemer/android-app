package com.weatherplanner.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Espresso tests for DetailsActivity.
 * With no OpenWeatherMap key configured, the app uses demo weather data.
 */
@RunWith(AndroidJUnit4.class)
public class DetailsActivityTest {

    @Before
    public void setUp() {
        // Initialize Espresso-Intents before each test
        Intents.init();
    }

    @After
    public void tearDown() {
        // Release Espresso-Intents after each test
        Intents.release();
    }

    // ============================================================================
    // Test with a valid city name, expecting a successful API call
    // ============================================================================
    @Test
    public void testValidCity_DisplaysWeatherData() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), DetailsActivity.class)
                .putExtra("city", "Chicago");

        ActivityScenario.launch(intent);

        // Wait for the network request to complete.
        // NOTE: This is a fixed delay and can be flaky. In a real-world scenario,
        // using Espresso Idling Resources would be the standard practice.
        Thread.sleep(5000);

        // Check that the loading message is gone and some weather data is displayed.
        onView(withId(R.id.cityInfo)).check(matches(not(withText("Loading weather data..."))));
        onView(withId(R.id.cityInfo)).check(matches(withText(startsWith("Weather:"))));

        // Check that the welcome message is correct
        onView(withId(R.id.welcomeText)).check(matches(withText("Welcome to Chicago")));
    }

    // ============================================================================
    // Test that the "Weather Insights" button works after data is loaded
    // ============================================================================
    @Test
    public void testNavigationToWeatherInsights_AfterDataLoad() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), DetailsActivity.class)
                .putExtra("city", "Chicago");

        ActivityScenario.launch(intent);

        // Wait for the network request
        Thread.sleep(5000);

        // Click the button and verify navigation
        onView(withId(R.id.weatherInsightsButton)).perform(click());
        intended(hasComponent(WeatherInsightsActivity.class.getName()));
    }

    // ============================================================================
    // Test with an invalid city name, expecting an error message
    // ============================================================================
    @Test
    public void testInvalidCity_DisplaysErrorMessage() throws InterruptedException {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), DetailsActivity.class)
                .putExtra("city", "InvalidCityName12345");

        ActivityScenario.launch(intent);

        // Wait for the weather request or demo fallback to complete.
        Thread.sleep(5000);

        if (BuildConfig.OPENWEATHERMAP_API_KEY == null || BuildConfig.OPENWEATHERMAP_API_KEY.trim().isEmpty()) {
            onView(withId(R.id.welcomeText)).check(matches(withText("Welcome to InvalidCityName12345")));
            onView(withId(R.id.cityInfo)).check(matches(withText(startsWith("Weather:"))));
        } else {
            onView(withId(R.id.welcomeText)).check(matches(withText("Unable to load weather data")));
            onView(withId(R.id.cityInfo)).check(matches(withText(startsWith("Error:"))));
        }
    }
}
