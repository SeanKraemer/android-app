package edu.uiuc.cs427app;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import android.view.View;
import org.hamcrest.Matcher;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;

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
 * Espresso instrumented tests for MainActivity.
 * Tests the addition and removal of cities from the user's location list.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private UserDatabaseHelper dbHelper;
    private SharedPreferences prefs;
    private ContentResolver contentResolver;

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpass123";

    /**
     * Setup method that runs before each test.
     * Clears the user database, clears SharedPreferences, clears location data,
     * creates a test user, and logs in that user.
     */
    @Before
    public void setUp() {
        Context context = getApplicationContext();
        dbHelper = new UserDatabaseHelper(context);
        contentResolver = context.getContentResolver();

        // Clear database and SharedPreferences
        dbHelper.getWritableDatabase().delete("users", null, null);
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Clear all location data from ContentProvider
        contentResolver.delete(LocationContract.LocationEntry.CONTENT_URI, null, null);

        // Create a test user
        dbHelper.addUser(
                TEST_USERNAME,
                TEST_PASSWORD,
                "#FFFFFF",    // theme_bg
                "#000000",    // theme_text
                "#FF0000",    // theme_accent
                "#00FF00",    // theme_button
                "#0000FF",    // theme_toolbar
                "default theme"     // theme_prompt
        );

        // Simulate logged in user by setting SharedPreferences
        prefs.edit().putString(KEY_CURRENT_USER, TEST_USERNAME).apply();
    }

    /**
     * Teardown method that runs after each test.
     * Cleans up SharedPreferences, database, and ContentProvider data.
     */
    @After
    public void tearDown() {
        prefs.edit().clear().apply();
        contentResolver.delete(LocationContract.LocationEntry.CONTENT_URI, null, null);
        dbHelper.close();
    }

    /**
     * Test for adding a new city to the location list.
     *
     * This test:
     * 1. Launches MainActivity
     * 2. Clicks "Add a location" button
     * 3. Types a city name (Champaign) in the dialog
     * 4. Clicks "Add" button
     * 5. Verifies the city appears in the location list
     * 6. Verifies the city is saved in the ContentProvider database
     *
     * Assertion: The city name should be visible in the UI and stored in the database
     * for the current user.
     */
    @Test
    public void testAddNewCity() throws Exception {
        // Launch MainActivity
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Thread.sleep(1000);

            // Click "Add a location" button
            onView(withId(R.id.buttonAddLocation)).perform(click());
            Thread.sleep(1000);

            // Type city name in the dialog input field
            // The dialog uses a dynamically created EditText with hint "Search city"
            // We need to find it using a custom matcher for EditText with the specific hint
            onView(withHint("Search city")).perform(typeText("Champaign"));
            closeSoftKeyboard();
            Thread.sleep(1500);

            // Click "Add" button in the dialog
            onView(withText("Add")).inRoot(isDialog()).perform(click());
            Thread.sleep(2000); // Wait for geocoding to complete

            // Verify the city appears in the location list
            // The city name is displayed in a TextView that is part of the dynamically created row
            onView(withText("Champaign")).check(matches(isDisplayed()));

            // Verify the city is saved in the ContentProvider
            String selection = LocationContract.LocationEntry.COLUMN_USER + "=? AND " +
                    LocationContract.LocationEntry.COLUMN_CITY + "=?";
            String[] selectionArgs = new String[]{TEST_USERNAME, "Champaign"};
            Cursor cursor = contentResolver.query(
                    LocationContract.LocationEntry.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    null
            );

            assert cursor != null;
            assert cursor.getCount() > 0 : "City should be saved in ContentProvider";
            cursor.close();
        }
    }

    /**
     * Test for removing an existing city from the location list.
     *
     * This test:
     * 1. Adds a city (Chicago) to the location list first
     * 2. Launches MainActivity to display the added city
     * 3. Performs a swipe left gesture on the city row
     * 4. Clicks "Delete" button in the confirmation dialog
     * 5. Verifies the city is removed from the UI
     * 6. Verifies the city is deleted from the ContentProvider database
     *
     * Assertion: The city should no longer be visible in the UI and should be
     * removed from the database for the current user.
     */
    @Test
    public void testRemoveExistingCity() throws Exception {
        // First, add a city to the database directly so we have something to delete
        String testCity = "Chicago";
        android.content.ContentValues cv = new android.content.ContentValues();
        cv.put(LocationContract.LocationEntry.COLUMN_USER, TEST_USERNAME);
        cv.put(LocationContract.LocationEntry.COLUMN_CITY, testCity);
        contentResolver.insert(LocationContract.LocationEntry.CONTENT_URI, cv);
        Thread.sleep(500);

        // Launch MainActivity - it should load and display the city
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Thread.sleep(1500);

            // Verify the city is displayed
            onView(withText(testCity)).check(matches(isDisplayed()));
            Thread.sleep(500);

            // Perform swipe left gesture on the city row to trigger delete
            // The row has a tag equal to the city name, so we can find it by tag
            // We use a custom action that simulates touch events to trigger the OnTouchListener
            onView(withTagValue(is(testCity))).perform(swipeLeftToDelete());
            Thread.sleep(1500);

            // Click "Delete" button in the confirmation dialog
            // Use allOf to match both the text and Button class to avoid ambiguity with the title
            onView(allOf(
                    withText("Delete"),
                    withClassName(endsWith("Button"))
            )).inRoot(isDialog()).perform(click());
            Thread.sleep(2000); // Wait for animation and deletion to complete

            // Verify the city is no longer visible in the UI
            // After removal, the entire row is removed from the container, so we check it doesn't exist
            onView(withTagValue(is(testCity))).check(doesNotExist());

            // Verify the city is deleted from the ContentProvider
            String selection = LocationContract.LocationEntry.COLUMN_USER + "=? AND " +
                    LocationContract.LocationEntry.COLUMN_CITY + "=?";
            String[] selectionArgs = new String[]{TEST_USERNAME, testCity};
            Cursor cursor = contentResolver.query(
                    LocationContract.LocationEntry.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    null
            );

            assert cursor != null;
            assert cursor.getCount() == 0 : "City should be deleted from ContentProvider";
            cursor.close();
        }
    }

    /**
     * Custom ViewAction that simulates a left swipe gesture using MotionEvents.
     * This properly triggers the custom OnTouchListener in MainActivity.
     *
     * The action simulates:
     * - ACTION_DOWN at the center-right of the view
     * - Multiple ACTION_MOVE events moving left (exceeding 33% threshold)
     * - ACTION_UP to complete the gesture
     *
     * @return ViewAction that performs the swipe left gesture
     */
    private static ViewAction swipeLeftToDelete() {
        return new ViewAction() {
            /**
             * Returns the constraints for this ViewAction.
             * The view must be displayed on screen to perform the swipe gesture.
             *
             * @return Matcher that ensures the view is displayed
             */
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            /**
             * Returns a description of this ViewAction for debugging and error messages.
             *
             * @return String description of the action
             */
            @Override
            public String getDescription() {
                return "Swipe left to trigger delete";
            }

            /**
             * Performs the swipe left gesture on the given view.
             * Creates and dispatches MotionEvents to simulate a touch swipe from right to left,
             * exceeding the 33% threshold required by MainActivity's OnTouchListener.
             *
             * @param uiController Controller for coordinating with the UI thread
             * @param view The view on which to perform the swipe gesture
             */
            @Override
            public void perform(UiController uiController, View view) {
                // Get view location on screen to calculate raw coordinates
                int[] viewLocation = new int[2];
                view.getLocationOnScreen(viewLocation);

                // Start from 75% of width, end at 20% of width (more than 33% threshold)
                float viewStartX = view.getWidth() * 0.75f;
                float viewEndX = view.getWidth() * 0.2f;
                float viewY = view.getHeight() / 2f;

                // Raw coordinates (screen coordinates)
                float rawStartX = viewLocation[0] + viewStartX;
                float rawEndX = viewLocation[0] + viewEndX;
                float rawY = viewLocation[1] + viewY;

                long downTime = android.os.SystemClock.uptimeMillis();

                // ACTION_DOWN
                android.view.MotionEvent downEvent = createMotionEvent(
                        downTime, downTime,
                        android.view.MotionEvent.ACTION_DOWN,
                        rawStartX, rawY
                );
                view.dispatchTouchEvent(downEvent);
                downEvent.recycle();
                uiController.loopMainThreadForAtLeast(50);

                // ACTION_MOVE events - interpolate between start and end
                int numSteps = 10;
                for (int i = 1; i <= numSteps; i++) {
                    float fraction = (float) i / numSteps;
                    float currentRawX = rawStartX + fraction * (rawEndX - rawStartX);

                    long eventTime = downTime + i * 10L;
                    android.view.MotionEvent moveEvent = createMotionEvent(
                            downTime, eventTime,
                            android.view.MotionEvent.ACTION_MOVE,
                            currentRawX, rawY
                    );
                    view.dispatchTouchEvent(moveEvent);
                    moveEvent.recycle();
                    uiController.loopMainThreadForAtLeast(10);
                }

                // ACTION_UP
                long upTime = downTime + (numSteps + 1) * 10L;
                android.view.MotionEvent upEvent = createMotionEvent(
                        downTime, upTime,
                        android.view.MotionEvent.ACTION_UP,
                        rawEndX, rawY
                );
                view.dispatchTouchEvent(upEvent);
                upEvent.recycle();

                uiController.loopMainThreadForAtLeast(200);
            }

            /**
             * Creates a MotionEvent with raw (screen) coordinates.
             * This is necessary because the OnTouchListener uses getRawX() which returns screen coordinates.
             *
             * @param downTime The time when the gesture started
             * @param eventTime The time when this event occurred
             * @param action The action code (DOWN, MOVE, UP)
             * @param rawX The raw X screen coordinate
             * @param rawY The raw Y screen coordinate
             * @return A MotionEvent with the specified properties
             */
            private android.view.MotionEvent createMotionEvent(
                    long downTime, long eventTime, int action,
                    float rawX, float rawY) {

                android.view.MotionEvent.PointerProperties[] properties =
                    new android.view.MotionEvent.PointerProperties[1];
                properties[0] = new android.view.MotionEvent.PointerProperties();
                properties[0].id = 0;
                properties[0].toolType = android.view.MotionEvent.TOOL_TYPE_FINGER;

                android.view.MotionEvent.PointerCoords[] coords =
                    new android.view.MotionEvent.PointerCoords[1];
                coords[0] = new android.view.MotionEvent.PointerCoords();
                coords[0].x = rawX;  // Use raw coordinates as the primary coordinates
                coords[0].y = rawY;
                coords[0].pressure = 1.0f;
                coords[0].size = 1.0f;

                return android.view.MotionEvent.obtain(
                        downTime, eventTime, action,
                        1, properties, coords,
                        0, 0, 1.0f, 1.0f, 0, 0,
                        android.view.InputDevice.SOURCE_TOUCHSCREEN, 0
                );
            }
        };
    }

    /**
     * Custom TypeSafeMatcher for matching Toast messages.
     * This matcher identifies Toast windows by checking the window type and tokens.
     */
    public static class ToastMatcher extends TypeSafeMatcher<Root> {

        /**
         * Describes what this matcher matches.
         * @param description The description to be built
         */
        @Override
        public void describeTo(Description description) {
            description.appendText("is toast");
        }

        /**
         * Checks if the given Root represents a Toast window.
         * Uses a safer approach by checking if the window token equals the application token,
         * which is the reliable way to identify Toast windows across different Android versions.
         *
         * @param root The Root to check
         * @return true if the Root is a Toast window, false otherwise
         */
        @Override
        public boolean matchesSafely(Root root) {
            // Check if the window token equals the application token
            // This is the reliable way to identify Toast windows without using deprecated APIs
            IBinder windowToken = root.getDecorView().getWindowToken();
            IBinder appToken = root.getDecorView().getApplicationWindowToken();
            return windowToken == appToken;
        }
    }
}

