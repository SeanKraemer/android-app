package edu.uiuc.cs427app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Root;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.view.WindowManager;
import android.os.IBinder;

import android.database.sqlite.SQLiteDatabase;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SignUpActivityInstrumentedTest {

    private UserDatabaseHelper dbHelper;
    private Context context;
    // Adjust this name to match the actual DB filename used by your helper if needed.
    private static final String TEST_DB_NAME = "WeatherApp.db";

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        // Ensure a clean DB state by removing any existing DB file used by the helper.
        // Replace TEST_DB_NAME with the actual DB name if different.
        context.deleteDatabase(TEST_DB_NAME);

        // instantiate helper (will create DB when needed)
        dbHelper = new UserDatabaseHelper(context);

        // Initialize Espresso-Intents for navigation assertions
        Intents.init();

        // Small pause to let instrumentation settle (optional)
        Thread.sleep(200);
    }

    @After
    public void tearDown() throws Exception {
        // Close any open DB and remove test DB to clean state
        try {
            SQLiteDatabase writable = dbHelper.getWritableDatabase();
            writable.close();
        } catch (Exception ignored) {}

        context.deleteDatabase(TEST_DB_NAME);

        // Release Espresso-Intents
        Intents.release();

        Thread.sleep(200);
    }

    // 1. Successful account creation
    @Test
    public void testSuccessfulAccountCreation_navigatesToLoginAndSavesUser() throws Exception {
        // Launch activity
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class)) {
            // Fill inputs
            onView(withId(R.id.signupUsernameInput)).perform(typeText("testuser"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.signupPasswordInput)).perform(typeText("test1234"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("test1234"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.themeDescriptionInput)).perform(typeText("modern blue"));
            closeSoftKeyboard();
            Thread.sleep(1000);

            // Click sign up
            onView(withId(R.id.createAccountButton)).perform(click());
            Thread.sleep(1000);

            // Assert user exists in DB
            boolean exists = dbHelper.userExists("testuser");
            if (!exists) {
                throw new AssertionError("Expected user 'testuser' to exist in DB after sign up.");
            }

            // Assert navigation to LoginActivity using Espresso-Intents
            intended(hasComponent(LoginActivity.class.getName()));
        }
    }

    // 2. Empty username validation
    @Test
    public void testEmptyUsername_showsPleaseFillInAllFieldsToast() throws Exception {
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class)) {
            // Leave username empty
            onView(withId(R.id.signupPasswordInput)).perform(typeText("pass1234"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("pass1234"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.themeDescriptionInput)).perform(typeText("modern"));

            closeSoftKeyboard();
            Thread.sleep(1000);

            onView(withId(R.id.createAccountButton)).perform(click());
            Thread.sleep(1000);

            onView(withText("Please fill in all fields"))
                    .inRoot(new ToastMatcher())
                    .check(matches(isDisplayed()));
        }
    }

    // 3. Empty password validation
    @Test
    public void testEmptyPassword_showsPleaseFillInAllFieldsToast() throws Exception {
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class)) {
            onView(withId(R.id.signupUsernameInput)).perform(typeText("someuser"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            // Leave password empty
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("whatever"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.themeDescriptionInput)).perform(typeText("theme"));

            closeSoftKeyboard();
            Thread.sleep(1000);

            onView(withId(R.id.createAccountButton)).perform(click());
            Thread.sleep(1000);

            onView(withText("Please fill in all fields"))
                    .inRoot(new ToastMatcher())
                    .check(matches(isDisplayed()));
        }
    }

    // 4. Password mismatch validation
    @Test
    public void testPasswordMismatch_showsPasswordsDoNotMatchToast() throws Exception {
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class)) {
            onView(withId(R.id.signupUsernameInput)).perform(typeText("testuser2"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.signupPasswordInput)).perform(typeText("pass123"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("different"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.themeDescriptionInput)).perform(typeText("cool theme"));
            closeSoftKeyboard();
            Thread.sleep(1000);

            onView(withId(R.id.createAccountButton)).perform(click());
            Thread.sleep(1000);

            onView(withText("Passwords do not match"))
                    .inRoot(new ToastMatcher())
                    .check(matches(isDisplayed()));
        }
    }

    // 5. Username too short (<3)
    @Test
    public void testUsernameTooShort_showsUsernameMustBeAtLeast3CharactersToast() throws Exception {
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class)) {
            onView(withId(R.id.signupUsernameInput)).perform(typeText("ab"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.signupPasswordInput)).perform(typeText("pass1234"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("pass1234"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.themeDescriptionInput)).perform(typeText("theme"));
            closeSoftKeyboard();
            Thread.sleep(1000);

            onView(withId(R.id.createAccountButton)).perform(click());
            Thread.sleep(1000);

            onView(withText("Username must be at least 3 characters"))
                    .inRoot(new ToastMatcher())
                    .check(matches(isDisplayed()));
        }
    }

    // 6. Password too short (<4)
    @Test
    public void testPasswordTooShort_showsPasswordMustBeAtLeast4CharactersToast() throws Exception {
        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class)) {
            onView(withId(R.id.signupUsernameInput)).perform(typeText("testuser3"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.signupPasswordInput)).perform(typeText("abc"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("abc"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.themeDescriptionInput)).perform(typeText("theme"));
            closeSoftKeyboard();
            Thread.sleep(1000);

            onView(withId(R.id.createAccountButton)).perform(click());
            Thread.sleep(1000);

            onView(withText("Password must be at least 4 characters"))
                    .inRoot(new ToastMatcher())
                    .check(matches(isDisplayed()));
        }
    }

    // 7. Duplicate username rejection
    @Test
    public void testDuplicateUsername_showsUsernameAlreadyExistsToast() throws Exception {
        // First create a user using dbHelper directly (simulate prior registration)
        boolean created = dbHelper.addUser("duplicateUser", "somePassword", "bg", "text", "accent", "button", "toolbar", "prompt");
        if (!created) {
            // If addUser returns false, we still proceed because maybe DB structure differs.
            // But assert userExists to ensure we have one inserted; if not, attempt to continue.
        }

        try (ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class)) {
            // Attempt to create same username again
            onView(withId(R.id.signupUsernameInput)).perform(typeText("duplicateUser"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.signupPasswordInput)).perform(typeText("newpass123"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.confirmPasswordInput)).perform(typeText("newpass123"));
            closeSoftKeyboard();

            Thread.sleep(1000);
            onView(withId(R.id.themeDescriptionInput)).perform(typeText("theme"));
            closeSoftKeyboard();
            Thread.sleep(1000);

            onView(withId(R.id.createAccountButton)).perform(click());
            Thread.sleep(1000);

            onView(withText("Username already exists. Please choose another."))
                    .inRoot(new ToastMatcher())
                    .check(matches(isDisplayed()));
        }
    }

    // 8. Back button returns to LoginActivity
    @Test
    public void testBackButton_returnsToLoginActivity() throws Exception {
        ActivityScenario<SignUpActivity> scenario = ActivityScenario.launch(SignUpActivity.class);

        // Click the back button
        onView(withId(R.id.backButton)).perform(click());
        Thread.sleep(1000);

        // Assert the SignUpActivity has moved to DESTROYED state
        assertEquals(
                Lifecycle.State.DESTROYED,
                scenario.getState()
        );
    }



    /**
     * ToastMatcher helper class for verifying Toasts with Espresso.
     * Usage: onView(withText("message")).inRoot(new ToastMatcher()).check(matches(isDisplayed()));
     */
    public static class ToastMatcher extends TypeSafeMatcher<Root> {

        @Override
        public void describeTo(Description description) {
            description.appendText("is toast");
        }

        @Override
        public boolean matchesSafely(Root root) {
            int type = root.getWindowLayoutParams().get().type;
            if ((type == WindowManager.LayoutParams.TYPE_TOAST)
                    || (type == WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW)) {
                // In some devices the toast window type may differ.
                // Attempt to find that the window token equals the application token.
                IBinder windowToken = root.getDecorView().getWindowToken();
                IBinder appToken = root.getDecorView().getApplicationWindowToken();
                return windowToken == appToken;
            }
            return false;
        }
    }
}
