package com.weatherplanner.app;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.startsWith;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Root;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Full Espresso test suite for LoginActivity, aligned with UserDatabaseHelper.
 */
@RunWith(AndroidJUnit4.class)
public class LoginActivityTest {

    private Context context;
    private UserDatabaseHelper dbHelper;
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_CURRENT_USER = "current_user";

    @Before
    public void setUp() {
        context = getApplicationContext();
        dbHelper = new UserDatabaseHelper(context);

        // Wipe DB table
        dbHelper.getWritableDatabase().delete("users", null, null);

        // Reset SharedPreferences
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    @After
    public void tearDown() {
        prefs.edit().clear().apply();
        dbHelper.close();
    }

    // Helper to add users cleanly
    private void addTestUser(String username, String password) {
        dbHelper.addUser(
                username,
                password,
                "#FFFFFF",    // theme_bg
                "#000000",    // theme_text
                "#FF0000",    // theme_accent
                "#00FF00",    // theme_button
                "#0000FF",    // theme_toolbar
                "default theme" // theme_prompt
        );
    }

    // ============================================================================
    // 1. Successful login → no theme change → navigates to MainActivity
    // ============================================================================
    @Test
    public void testValidLogin_NoTheme_NavigatesToMainActivity() throws Exception {

        addTestUser("validuser", "valid1234");

        ActivityScenario.launch(LoginActivity.class);

        Thread.sleep(1000);

        onView(withId(R.id.usernameInput)).perform(typeText("validuser"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.passwordInput)).perform(typeText("valid1234"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.signInButton)).perform(click());
        Thread.sleep(1500);

        // Verify SharedPreferences saved user
        String savedUser = prefs.getString(KEY_CURRENT_USER, null);
        assert savedUser.equals("validuser");

        // Verify MainActivity (adjust text to whatever your MainActivity shows)
        onView(withText(startsWith("Weather Planner"))).check(matches(isDisplayed()));

    }

    // ============================================================================
    // 2. Invalid username
    // ============================================================================
    @Test
    public void testInvalidUsername_ShowsToast() throws Exception {

        ActivityScenario.launch(LoginActivity.class);

        Thread.sleep(1000);

        onView(withId(R.id.usernameInput)).perform(typeText("nonexistent"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.passwordInput)).perform(typeText("whatever"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.signInButton)).perform(click());
        Thread.sleep(1500);

        onView(withText("Invalid username or password"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));

        // Still on LoginActivity
        onView(withId(R.id.signInButton)).check(matches(isDisplayed()));
    }

    // ============================================================================
    // 3. Invalid password
    // ============================================================================
    @Test
    public void testInvalidPassword_ShowsToast() throws Exception {
        addTestUser("testuser", "correct123");

        ActivityScenario.launch(LoginActivity.class);
        Thread.sleep(1000);

        onView(withId(R.id.usernameInput)).perform(typeText("testuser"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.passwordInput)).perform(typeText("wrongpassword"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.signInButton)).perform(click());
        Thread.sleep(1500);

        onView(withText("Invalid username or password"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    // ============================================================================
    // 4. Empty username
    // ============================================================================
    @Test
    public void testEmptyUsername_ShowsRequiredToast() throws Exception {

        ActivityScenario.launch(LoginActivity.class);
        Thread.sleep(1000);

        onView(withId(R.id.passwordInput)).perform(typeText("abc"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.signInButton)).perform(click());
        Thread.sleep(1500);

        onView(withText("Please enter both username and password"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    // ============================================================================
    // 5. Empty password
    // ============================================================================
    @Test
    public void testEmptyPassword_ShowsRequiredToast() throws Exception {

        ActivityScenario.launch(LoginActivity.class);
        Thread.sleep(1000);

        onView(withId(R.id.usernameInput)).perform(typeText("testuser"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.signInButton)).perform(click());
        Thread.sleep(1500);

        onView(withText("Please enter both username and password"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    // ============================================================================
    // 6. Both fields empty
    // ============================================================================
    @Test
    public void testBothEmpty_ShowsRequiredToast() throws Exception {

        ActivityScenario.launch(LoginActivity.class);
        Thread.sleep(1000);

        onView(withId(R.id.signInButton)).perform(click());
        Thread.sleep(1500);

        onView(withText("Please enter both username and password"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }

    // ============================================================================
    // 7. Sign-up link opens SignUpActivity
    // ============================================================================
    @Test
    public void testSignUpLink_NavigatesToSignUpActivity() throws Exception {

        ActivityScenario.launch(LoginActivity.class);
        Thread.sleep(1000);

        onView(withId(R.id.signUpLink)).perform(click());
        Thread.sleep(1500);

        // Adjust text to your SignUpActivity title
        onView(withId(R.id.signupTitle)).check(matches(withText("Create Account")));

    }

    // ============================================================================
    // 8. SharedPreferences stores correct username
    // ============================================================================
    @Test
    public void testSharedPreferencesStores_CurrentUser() throws Exception {
        addTestUser("sharedpreftest", "test5678");

        ActivityScenario.launch(LoginActivity.class);
        Thread.sleep(1000);

        onView(withId(R.id.usernameInput)).perform(typeText("sharedpreftest"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.passwordInput)).perform(typeText("test5678"));
        closeSoftKeyboard();
        Thread.sleep(1000);

        onView(withId(R.id.signInButton)).perform(click());
        Thread.sleep(1500);

        String saved = prefs.getString(KEY_CURRENT_USER, null);
        assert saved.equals("sharedpreftest");
    }

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
