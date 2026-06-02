package com.weatherplanner.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented tests for logout functionality in MainActivity.
 * Tests that users can successfully log out via the options menu and return to LoginActivity.
 */
@RunWith(AndroidJUnit4.class)
public class LogoutTest {

    private UserDatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_CURRENT_USER = "current_user";
    private Context context;

    /**
     * Sets up the test environment before each test.
     * Creates a clean database, initializes SharedPreferences, and creates a test user.
     * Simulates a logged-in user by storing username in SharedPreferences.
     */
    @Before
    public void setUp() throws InterruptedException {
        // Get application context
        context = ApplicationProvider.getApplicationContext();

        // Initialize database helper
        dbHelper = new UserDatabaseHelper(context);
        dbHelper.getWritableDatabase();

        // Clear any existing test data
        context.deleteDatabase("WeatherApp.db");
        dbHelper = new UserDatabaseHelper(context);

        // Initialize SharedPreferences
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();

        Thread.sleep(500);

        // Create a test user in the database with theme
        boolean userCreated = dbHelper.addUser(
                "logoutTestUser",
                "password123",
                "#FFFFFF",  // background
                "#000000",  // text
                "#2196F3",  // accent
                "#4CAF50",  // button
                "#00EEFF",  // toolbar
                "test theme"
        );

        Thread.sleep(500);

        // Verify user was created
        if (!userCreated) {
            throw new RuntimeException("Failed to create test user");
        }

        // Simulate user being logged in by storing username in SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CURRENT_USER, "logoutTestUser");
        editor.commit();

        Thread.sleep(500);
    }

    /**
     * Cleans up the test environment after each test.
     * Deletes the test database and clears SharedPreferences.
     */
    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(500);

        // Clean up database
        if (dbHelper != null) {
            context.deleteDatabase("WeatherApp.db");
            dbHelper.close();
        }

        // Clean up SharedPreferences
        if (sharedPreferences != null) {
            sharedPreferences.edit().clear().commit();
        }

        Thread.sleep(500);
    }

    /**
     * Test that the logout menu item is visible and clickable in the options menu.
     * Verifies that users can access the logout option from the three-dot menu.
     */
    @Test
    public void testLogoutMenuItem_isVisibleAndClickable() throws InterruptedException {
        // Launch MainActivity (user should already be logged in from setUp)
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Wait for activity to fully load
        Thread.sleep(1500);

        // Open the overflow menu (three dots)
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());

        // Wait for menu to appear
        Thread.sleep(1000);

        // Verify that "Logout" menu item is displayed and clickable
        onView(withText("Logout"))
                .check(matches(isDisplayed()))
                .perform(click());

        Thread.sleep(1000);

        scenario.close();
    }

    /**
     * Test that clicking logout successfully clears the user session and navigates to LoginActivity.
     * This comprehensive test verifies:
     * 1. User can access and click the logout menu item
     * 2. Session data (username) is cleared from SharedPreferences
     * 3. User is redirected to LoginActivity
     * 4. All login screen elements are visible
     */
    @Test
    public void testLogout_clearsSessionAndNavigatesToLogin() throws InterruptedException {
        // Launch MainActivity (user should already be logged in from setUp)
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Wait for activity to fully load
        Thread.sleep(1500);

        // Open the overflow menu (three dots)
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());

        // Wait for menu to appear
        Thread.sleep(1000);

        // Click on the "Logout" menu item
        onView(withText("Logout"))
                .perform(click());

        // Wait for navigation to complete
        Thread.sleep(1500);

        // Assertion 1: Verify navigation to LoginActivity - check sign in button
        onView(withId(R.id.signInButton))
                .check(matches(isDisplayed()));

        Thread.sleep(500);

        // Assertion 2: Verify login screen elements are visible
        onView(withId(R.id.usernameInput))
                .check(matches(isDisplayed()));

        Thread.sleep(500);

        onView(withId(R.id.passwordInput))
                .check(matches(isDisplayed()));

        Thread.sleep(500);

        // Assertion 3: Verify that current user was removed from SharedPreferences
        String currentUser = sharedPreferences.getString(KEY_CURRENT_USER, null);
        assertNull("Current user should be null after logout", currentUser);

        Thread.sleep(500);

        scenario.close();
    }

    /**
     * Test that logout only clears the session, not the user account from database.
     * Verifies that user can log back in after logging out because their account
     * still exists in the database with valid credentials.
     */
    @Test
    public void testLogout_userAccountPersistsInDatabase() throws InterruptedException {
        // Launch MainActivity
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        Thread.sleep(1500);

        // Perform logout
        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().getTargetContext());
        Thread.sleep(1000);
        onView(withText("Logout")).perform(click());
        Thread.sleep(1500);

        // Assertion 1: Verify user account still exists in database
        boolean userExists = dbHelper.userExists("logoutTestUser");
        assertTrue("User account should still exist in database after logout", userExists);

        Thread.sleep(500);

        // Assertion 2: Verify user credentials are still valid (can be used to log back in)
        boolean canValidate = dbHelper.validateUser("logoutTestUser", "password123");
        assertTrue("User credentials should still be valid after logout", canValidate);

        Thread.sleep(500);

        // Assertion 3: Verify session is cleared
        String currentUser = sharedPreferences.getString(KEY_CURRENT_USER, null);
        assertNull("Session should be cleared after logout", currentUser);

        Thread.sleep(500);

        scenario.close();
    }
}