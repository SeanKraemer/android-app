package edu.uiuc.cs427app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.location.Geocoder;
import android.location.Address;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.widget.Toolbar;

import java.util.LinkedHashSet;

/**
 * MainActivity serves as the main hub of the application.
 *
 * This activity manages the user's list of saved cities and provides navigation to:
 * - Weather details for each city (DetailsActivity)
 * - Map view for each city (MapActivity)
 * - User logout functionality
 *
 * Features:
 * - Loads and displays per-user city list from ContentProvider
 * - Add/remove cities with geocoding validation
 * - Swipe-to-delete gesture for removing cities
 * - Dynamic theme support based on user preferences
 * - Search suggestions with live geocoding
 */
public class MainActivity extends AppCompatActivity {
    // SharedPreferences for user data
    private SharedPreferences sharedPreferences;
    private UserDatabaseHelper dbHelper;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_CURRENT_USER = "current_user";

    private LinearLayout locationsContainer;
    private Button buttonAddLocation;
    private final List<String> locations = new ArrayList<>();
    private Set<String> locationSet = new HashSet<>(); // normalized names for duplicate checks

    // Theme colors parsed once for reuse
    private int themeBackgroundColor;
    private int themeTextColor;
    private int themeAccentColor;
    private int themeButtonColor;
    private int themeToolbarColor;

    // Holds a reference to the currently shown Add City dialog so we can dismiss it on selection
    private AlertDialog currentDialog;

    /**
     * Lifecycle callback invoked when the activity is being created.
     * Initializes theme colors from SharedPreferences, binds views, styles the UI,
     * loads the current user's saved locations from the ContentProvider, renders the list,
     * and wires up UI handlers (e.g., Add Location button).
     *
     * @param savedInstanceState previously saved state if re-created after process death; null on first launch
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        dbHelper = new UserDatabaseHelper(this);

        String username = sharedPreferences.getString(KEY_CURRENT_USER, "User");
        UserDatabaseHelper.User user = dbHelper.getUserTheme(username);

        String backgroundColor = user != null && user.themeBackground != null ? user.themeBackground : "#FFFFFF";
        String textColor = user != null && user.themeText != null ? user.themeText : "#000000";
        String accentColor = user != null && user.themeAccent != null ? user.themeAccent : "#2196F3";
        String buttonColor = user != null && user.themeButton != null ? user.themeButton : "#4CAF50";
        String toolbarColor = user != null && user.themeToolbar != null ? user.themeToolbar : "#4CAF50";

        themeBackgroundColor = Color.parseColor(backgroundColor);
        themeTextColor = Color.parseColor(textColor);
        themeAccentColor = Color.parseColor(accentColor);
        themeButtonColor = Color.parseColor(buttonColor);
        themeToolbarColor = Color.parseColor(toolbarColor);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Apply background
        getWindow().getDecorView().setBackgroundColor(themeBackgroundColor);

        displayUsername();

        locationsContainer = findViewById(R.id.locationsContainer);
        buttonAddLocation = findViewById(R.id.buttonAddLocation);

        // Apply button theme
        if (buttonAddLocation != null) {
            buttonAddLocation.setBackgroundTintList(ColorStateList.valueOf(themeButtonColor));
            buttonAddLocation.setTextColor(themeAccentColor);
        }

        // Apply text colors
        TextView title = findViewById(R.id.textView3);
        if (title != null) title.setTextColor(themeTextColor);

        TextView listLabel = findViewById(R.id.textView4);
        if (listLabel != null) listLabel.setTextColor(themeTextColor);
        toolbar.setBackgroundColor(themeToolbarColor);

        TextView themePromptText = findViewById(R.id.themePromptText);
        if (themePromptText != null) {
            String prompt = (user != null && user.themePrompt != null) ? user.themePrompt : "default";
            themePromptText.setText("Theme: " + prompt);
            themePromptText.setTextColor(themeTextColor);
        }

        loadLocations();
        renderLocations();

        // Invoked when the user taps the "Add Location" button.
        // Opens the Add City dialog to let the user search for and add a new city.
        buttonAddLocation.setOnClickListener(view -> showAddLocationDialog());
    }


    /**
     * Displays the username at the top of the MainActivity.
     * Updates the action bar title to show "Team # - Username".
     */
    private void displayUsername() {
        String username = sharedPreferences.getString(KEY_CURRENT_USER, "User");
        String userDisplayText = "Team 425 - " + username;

        // Set the action bar title (the header at the top)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(userDisplayText);
        }
    }

    /**
     * Creates the options menu (three dots) for the action bar.
     * Includes the menu_main.xml layout which contains the logout option.
     *
     * @param menu
     * @return true if the menu was created successfully
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles menu item selections.
     * Currently handles the logout option from the options menu.
     *
     * @param item The menu item that was selected
     * @return true if the menu item was handled, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            handleLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the logout process.
     * Clears the user session data from SharedPreferences and redirects to LoginActivity.
     */
    private void handleLogout() {
        // Clear the stored username from SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CURRENT_USER);
        editor.apply();

        // Redirect to LoginActivity
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Shows the themed Add City dialog.
     * Builds a vertical layout with a title and input, applies user theme colors,
     * spaces the action buttons, and triggers validation on Add.
     */
    private void showAddLocationDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);
        container.setPadding(pad, pad, pad, pad);
        container.setBackgroundColor(themeBackgroundColor);

        // Title inside content for alignment and theming
        TextView titleView = new TextView(this);
        titleView.setText("Add Location");
        titleView.setTextSize(20f);
        titleView.setTextColor(themeTextColor);
        titleView.setPadding(0, 0, 0, (int) (8 * density));
        container.addView(titleView);

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Search city");
        input.setTextColor(themeTextColor);
        int hintColor = Color.argb(160, Color.red(themeTextColor), Color.green(themeTextColor), Color.blue(themeTextColor));
        input.setHintTextColor(hintColor);
        if (Build.VERSION.SDK_INT >= 21) {
            input.setBackgroundTintList(ColorStateList.valueOf(themeAccentColor));
        }
        input.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.addView(input);

        // Suggestions list below the input with fixed height to keep dialog stable
        final ListView suggestionsList = new ListView(this);
        int listHeightPx = (int) (240 * density); // ~240dp fixed height
        suggestionsList.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                listHeightPx
        ));
        suggestionsList.setDivider(null);
        suggestionsList.setVerticalScrollBarEnabled(true);
        suggestionsList.setBackgroundColor(themeBackgroundColor);
        container.addView(suggestionsList);

        final ArrayList<String> suggestionItems = new ArrayList<>();
        final ArrayAdapter<String> suggestionsAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, suggestionItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                TextView tv = v.findViewById(android.R.id.text1);
                if (tv != null) {
                    tv.setTextColor(themeTextColor);
                    tv.setTextSize(16f);
                }
                return v;
            }
        };
        suggestionsList.setAdapter(suggestionsAdapter);

        /**
         * Callback invoked when the user taps a suggestion in the Add Location dialog.
         * Selects the city, adds it to the list, and dismisses the dialog.
         *
         * @param parent The AdapterView where the click occurred
         * @param view The view within the AdapterView that was clicked
         * @param pos The position of the view in the adapter
         * @param id The row id of the item that was clicked
         */
        suggestionsList.setOnItemClickListener((parent, view, pos, id) -> {
            String selection = suggestionItems.get(pos);
            String cityNameOnly = selection.split(",")[0].trim();
            String key = cityNameOnly.toLowerCase(Locale.ROOT);
            if (locationSet.contains(key)) {
                Toast.makeText(MainActivity.this, "City already added", Toast.LENGTH_SHORT).show();
                return;
            }
            addLocation(cityNameOnly);
            if (currentDialog != null && currentDialog.isShowing()) {
                currentDialog.dismiss();
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(container)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    /**
                     * Callback invoked when the user presses the 'Add' button on the Add Location dialog.
                     * Validates that the city name is not empty and calls validateAndAddCity to geocode it.
                     *
                     * @param dialogInterface The dialog interface that received the click
                     * @param which The button that was clicked
                     */
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        final String city = input.getText().toString().trim();
                        if (city.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Validate the city using Geocoder
                        validateAndAddCity(city);
                    }
                })
                .create();

        this.currentDialog = dialog;
        // Keep a reference to dismiss when selecting a suggestion
        dialog.setOnDismissListener(d -> currentDialog = null);

        // Query geocoder as user types
        input.addTextChangedListener(new TextWatcher() {
            /**
             * Callback invoked before text is changed in the input field.
             * Empty implementation - required by TextWatcher interface.
             *
             * @param s The text before it is changed
             * @param start The starting position of the changed text
             * @param count The number of characters to be replaced
             * @param after The number of characters after the change
             */
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            /**
             * Callback invoked when text is being changed in the input field.
             * Empty implementation - required by TextWatcher interface.
             *
             * @param s The text being changed
             * @param start The starting position of the changed text
             * @param before The number of characters before the change
             * @param count The number of characters after the change
             */
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            /**
             * Callback invoked after text has changed in the input field.
             * Performs live geocoding and updates the suggestion list with matching cities.
             *
             * @param s The text after it has been changed
             */
            @Override
            public void afterTextChanged(Editable s) {
                final String q = s.toString().trim();
                if (q.length() < 2) {
                    suggestionItems.clear();
                    suggestionsAdapter.notifyDataSetChanged();
                    return;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Geocoder with callback
                    Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

                    geocoder.getFromLocationName(q, 5, new Geocoder.GeocodeListener() {
                        /**
                         * Callback invoked by geocoder when async geocoding completes successfully.
                         * Runs on a background thread provided by Geocoder; marshals results to UI thread
                         * to update the suggestion list.
                         *
                         * @param addresses List of addresses matching the geocoding query
                         */
                        @Override
                        public void onGeocode(List<Address> addresses) {
                            // Runs on a background thread provided by Geocoder; marshal to UI thread inside helper
                            updateSuggestionsFromAddresses(addresses, suggestionItems, suggestionsAdapter);
                        }

                        /**
                         * Callback invoked when geocoder encounters an error during async geocoding.
                         * Clears the suggestion list on the UI thread.
                         *
                         * @param errorMessage Description of the error that occurred
                         */
                        @Override
                        public void onError(String errorMessage) {
                            // If geocoding fails, clear suggestions on UI thread
                            runOnUiThread(() -> {
                                suggestionItems.clear();
                                suggestionsAdapter.notifyDataSetChanged();
                            });
                        }
                    });
                } else {
                    // Fallback run geocoding on a background thread and update UI when done
                    new Thread(() -> {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = geocoder.getFromLocationName(q, 5);
                        } catch (IOException ignored) {
                        }
                        // updateSuggestionsFromAddresses marshals to UI thread internally
                        updateSuggestionsFromAddresses(addresses, suggestionItems, suggestionsAdapter);
                    }).start();
                }
            }
        });

        /**
         * Callback invoked when the Add Location dialog is shown.
         * Themes the dialog buttons (positive/negative) with user colors and prevents layout
         * jumps when the keyboard appears by adjusting soft input mode.
         *
         * @param di The dialog interface that was shown
         */
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface di) {
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(themeBackgroundColor));
                    // Prevent dialog from resizing/moving when keyboard appears
                    dialog.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING |
                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                    );
                }
                Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (positive != null) {
                    positive.setTextColor(themeAccentColor);
                    if (Build.VERSION.SDK_INT >= 21) {
                        positive.setBackgroundTintList(ColorStateList.valueOf(themeButtonColor));
                    }
                }
                if (negative != null) {
                    negative.setTextColor(themeAccentColor);
                    if (Build.VERSION.SDK_INT >= 21) {
                        negative.setBackgroundTintList(ColorStateList.valueOf(themeButtonColor));
                    }
                }

                // Add spacing between Cancel and Add buttons
                int gap = (int) (16 * density);
                if (positive != null && positive.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) positive.getLayoutParams();
                    lp.setMarginStart(gap);
                    lp.topMargin = Math.max(lp.topMargin, gap / 2);
                    positive.setLayoutParams(lp);
                }
                if (negative != null && negative.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) negative.getLayoutParams();
                    lp.setMarginEnd(gap);
                    lp.topMargin = Math.max(lp.topMargin, gap / 2);
                    negative.setLayoutParams(lp);
                }
            }
        });

        dialog.show();

        // Ensure window background is set immediately to match theme
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(themeBackgroundColor));
        }

        input.requestFocus();
    }

    // Helper to transform addresses to displayable suggestions and update adapter on UI thread
    private void updateSuggestionsFromAddresses(List<Address> addresses, final ArrayList<String> items, final ArrayAdapter<String> adapter) {
        final LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (addresses != null) {
            for (Address a : addresses) {
                if (a == null) continue;
                String primary = a.getLocality();
                if (primary == null || primary.isEmpty()) primary = a.getFeatureName();
                String admin = a.getAdminArea();
                String country = a.getCountryName();
                ArrayList<String> parts = new ArrayList<>();
                if (primary != null && !primary.isEmpty()) parts.add(primary);
                if (admin != null && !admin.isEmpty()) parts.add(admin);
                if (country != null && !country.isEmpty()) parts.add(country);
                if (!parts.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(parts.get(i));
                    }
                    unique.add(sb.toString());
                }
            }
        }
        runOnUiThread(() -> {
            items.clear();
            items.addAll(unique);
            adapter.notifyDataSetChanged();
        });
    }

    /**
     * Validates a user-entered city name using Android's Geocoder on a background thread.
     * Contract:
     * - Input: non-null, non-empty free-form city text (trimmed by caller).
     * - Duplicate handling: quickly rejects duplicates on the UI thread using a
     *   case-insensitive set before doing any network work.
     * - Behavior: performs a Geocoder lookup (up to 5 results). If at least one
     *   reasonable match is found, selects a display name by preferring
     *   Address.getLocality(), then getFeatureName(), then getAdminArea(), and
     *   finally falls back to the original input.
     * - Threading: network-bound geocoding runs on a background thread; UI updates
     *   (toasts and calling addLocation) are marshaled back via runOnUiThread.
     * - Error handling: IO/network failures or no results are treated as invalid
     *   and reported to the user with a toast (no insertion performed).
     * - Side effects: on success, calls addLocation(normalizedName) to persist the
     *   city via the ContentProvider and update the on-screen list.
     *
     * @param city The free-form city text entered by the user.
     */
    private void validateAndAddCity(final String city) {
        // Duplicate check
        final String key = city.trim().toLowerCase(Locale.ROOT);
        if (locationSet.contains(key)) {
            Toast.makeText(this, "City already added", Toast.LENGTH_SHORT).show();
            return;
        }

        // Run Geocoder lookup in background since it may perform network I/O
        /**
         * Background thread Runnable that performs geocoding to validate the user-entered city name.
         * Fetches address data from Geocoder and normalizes the city name, then posts results
         * back to the UI thread.
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocationName(city, 5);
                } catch (IOException e) {
                    addresses = null;
                }

                final boolean valid;
                final String normalizedName;
                if (addresses != null && !addresses.isEmpty()) {
                    // Find the first address that has a locality/admin area/country
                    Address match = null;
                    for (Address a : addresses) {
                        if (a == null) continue;
                        String locality = a.getLocality();
                        String admin = a.getAdminArea();
                        String country = a.getCountryName();
                        if ((locality != null && !locality.isEmpty()) || (admin != null && !admin.isEmpty()) || (country != null && !country.isEmpty())) {
                            match = a;
                            break;
                        }
                    }
                    if (match != null) {
                        // Use locality if available, otherwise fall back to feature name / admin
                        String use = match.getLocality();
                        if (use == null || use.isEmpty()) {
                            use = match.getFeatureName();
                        }
                        if (use == null || use.isEmpty()) {
                            use = match.getAdminArea();
                        }
                        if (use == null || use.isEmpty()) {
                            use = city; // fallback to input
                        }
                        normalizedName = use;
                        valid = true;
                    } else {
                        normalizedName = city;
                        valid = false;
                    }
                } else {
                    normalizedName = city;
                    valid = false;
                }

                final boolean isValidFinal = valid;
                final String nameToAdd = normalizedName;
                /**
                 * UI thread Runnable that processes geocoding results after background validation.
                 * Adds the location if valid, or shows an error toast if geocoding failed.
                 */
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isValidFinal) {
                            addLocation(nameToAdd);
                        } else {
                            Toast.makeText(MainActivity.this, "City not found. Try a more specific name (e.g. 'Champaign, IL')", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * Inserts a validated city for the current user via the ContentProvider and updates the UI.
     * Prevents duplicates by checking a normalized in-memory set before inserting.
     *
     * @param city The city display name to save for the signed-in user.
     */
    private void addLocation(String city) {
        String username = sharedPreferences.getString(KEY_CURRENT_USER, "User");
        String key = city.trim().toLowerCase(Locale.ROOT);
        if (locationSet.contains(key)) {
            Toast.makeText(this, "City already added", Toast.LENGTH_SHORT).show();
            return;
        }

        // insert into ContentProvider for this user
        ContentValues cv = new ContentValues();
        cv.put(LocationContract.LocationEntry.COLUMN_USER, username);
        cv.put(LocationContract.LocationEntry.COLUMN_CITY, city);
        Uri newUri = getContentResolver().insert(LocationContract.LocationEntry.CONTENT_URI, cv);
        if (newUri != null) {
            locations.add(city);
            locationSet.add(key);
            addLocationRow(city);
        } else {
            Toast.makeText(this, "Failed to save location", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Deletes the given city for the current user via the ContentProvider and updates UI.
     *
     * @param city the city to delete
     * @param rowToRemove the row view to remove from the container upon success
     */
    private void deleteLocation(String city, View rowToRemove) {
        String username = sharedPreferences.getString(KEY_CURRENT_USER, "User");
        String selection = LocationContract.LocationEntry.COLUMN_USER + "=? AND " + LocationContract.LocationEntry.COLUMN_CITY + "=?";
        String[] selArgs = new String[]{username, city};
        int deleted = getContentResolver().delete(LocationContract.LocationEntry.CONTENT_URI, selection, selArgs);
        if (deleted > 0) {
            locations.remove(city);
            locationSet.remove(city.trim().toLowerCase(Locale.ROOT));
            locationsContainer.removeView(rowToRemove);
            Toast.makeText(this, "Location removed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to remove location", Toast.LENGTH_SHORT).show();
            // reset any translation if deletion failed
            rowToRemove.animate().translationX(0f).setDuration(150).start();
        }
    }

    /**
     * Adds a horizontal row view for the given city containing the city name,
     * a "Weather" button, and a "Map" button, styled using the current theme colors.
     *
     * @param city The city display name to render.
     */
    private void addLocationRow(final String city) {
        // horizontal row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rowParams);
        row.setPadding(8, 12, 8, 12);
        row.setTag(city);

        // TextView for city name
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);
        tv.setText(city);
        tv.setTextColor(themeTextColor);
        tv.setTextSize(16);
        tv.setPadding(8, 0, 8, 0);
        row.addView(tv);

        // Weather button
        Button weatherBtn = new Button(this);
        LinearLayout.LayoutParams weatherBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        weatherBtnParams.setMargins(0, 0, 8, 0); // Add right margin
        weatherBtn.setLayoutParams(weatherBtnParams);
        weatherBtn.setBackgroundTintList(ColorStateList.valueOf(themeButtonColor));
        weatherBtn.setTextColor(themeAccentColor);
        weatherBtn.setText("Weather");
        weatherBtn.setAllCaps(true);
        /**
         * Callback invoked when the user taps the Weather button for this city.
         * Opens DetailsActivity to display weather information for the selected city.
         *
         * @param view The Weather button that was clicked
         */
        weatherBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DetailsActivity.class);
                intent.putExtra("city", city);
                startActivity(intent);
            }
        });
        row.addView(weatherBtn);

        // Map button
        Button mapBtn = new Button(this);
        LinearLayout.LayoutParams mapBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mapBtn.setLayoutParams(mapBtnParams);
        mapBtn.setBackgroundTintList(ColorStateList.valueOf(themeButtonColor));
        mapBtn.setTextColor(themeAccentColor);
        mapBtn.setText("Map");
        mapBtn.setAllCaps(true);
        /**
         * Callback invoked when the user taps the Map button for this city.
         * Opens MapActivity with geocoded coordinates for the selected city.
         *
         * @param view The Map button that was clicked
         */
        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openMapActivity(city);
            }
        });
        row.addView(mapBtn);

        /**
         * Callback invoked when the user touches the location row.
         * Implements swipe-to-delete gesture by detecting left swipe beyond threshold
         * and showing the delete confirmation dialog.
         *
         * @param v The row view that was touched
         * @param event The motion event describing the touch interaction
         * @return true if the touch event was handled, false otherwise
         */
        row.setOnTouchListener(new View.OnTouchListener() {
            float downX = 0f;
            boolean swiping = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        swiping = false;
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float deltaX = event.getRawX() - downX;
                        // Only allow left swipe visuals; clamp to <= 0 (left direction)
                        float clamped = Math.min(0f, deltaX);
                        v.setTranslationX(clamped);
                        if (clamped < -v.getWidth() * 0.1f) {
                            swiping = true;
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        float deltaX = event.getRawX() - downX;
                        float threshold = v.getWidth() * 0.33f;
                        // Trigger delete only on a left swipe beyond threshold
                        if (deltaX <= -threshold && swiping) {
                            showDeleteLocationDialog(v, city);
                        } else {
                            v.animate().translationX(0f).setDuration(150).start();
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        locationsContainer.addView(row);
    }

    /**
     * Re-renders the on-screen list of locations from the in-memory list by
     * clearing the container and adding a row for each city.
     */
    private void renderLocations() {
        locationsContainer.removeAllViews();
        for (String loc : locations) {
            addLocationRow(loc);
        }
    }

    /**
     * Loads all cities for the current user from the ContentProvider into memory,
     * normalizing names to enforce case-insensitive de-duplication.
     */
    private void loadLocations() {
        locations.clear();
        locationSet.clear();
        String username = sharedPreferences.getString(KEY_CURRENT_USER, "User");
        ContentResolver cr = getContentResolver();
        String[] projection = new String[]{LocationContract.LocationEntry._ID, LocationContract.LocationEntry.COLUMN_CITY};
        String selection = LocationContract.LocationEntry.COLUMN_USER + "=?";
        String[] selArgs = new String[]{username};
        Cursor c = cr.query(LocationContract.LocationEntry.CONTENT_URI, projection, selection, selArgs, null);
        if (c != null) {
            while (c.moveToNext()) {
                String city = c.getString(c.getColumnIndexOrThrow(LocationContract.LocationEntry.COLUMN_CITY));
                if (city != null) {
                    locations.add(city);
                    locationSet.add(city.trim().toLowerCase(Locale.ROOT));
                }
            }
            c.close();
        }
    }

    /**
     * Shows a themed confirmation dialog for deleting a city, matching the Add Location dialog theme.
     * The dialog uses a custom title and message with themed text color, a themed background,
     * and tinted buttons. On confirm, the row animates out to the left and is deleted.
     *
     * @param rowView the row view to animate and remove upon deletion
     * @param city the city name to delete
     */
    private void showDeleteLocationDialog(final View rowView, final String city) {
        // Container with vertical layout similar to Add Location dialog
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, pad);
        container.setBackgroundColor(themeBackgroundColor);

        // Title
        TextView titleView = new TextView(this);
        titleView.setText("Delete");
        titleView.setTextSize(20f);
        titleView.setTextColor(themeTextColor);
        titleView.setPadding(0, 0, 0, (int) (8 * getResources().getDisplayMetrics().density));
        container.addView(titleView);

        // Message
        TextView messageView = new TextView(this);
        messageView.setText("Delete this city?");
        messageView.setTextColor(themeTextColor);
        messageView.setTextSize(16f);
        container.addView(messageView);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(container)
                .setNegativeButton("Cancel", (d, which) -> rowView.animate().translationX(0f).setDuration(150).start())
                .setPositiveButton("Delete", (d, which) -> {
                    float target = -rowView.getWidth();
                    rowView.animate().translationX(target).setDuration(150).withEndAction(() -> deleteLocation(city, rowView)).start();
                })
                .create();

        dialog.setOnCancelListener(d -> rowView.animate().translationX(0f).setDuration(150).start());

        /**
         * Callback invoked when the delete confirmation dialog is shown.
         * Themes the dialog buttons and adds spacing between Cancel and Delete buttons.
         *
         * @param di The dialog interface that was shown
         */
        dialog.setOnShowListener(di -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(themeBackgroundColor));
            }
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (positive != null) {
                positive.setTextColor(themeAccentColor);
                if (Build.VERSION.SDK_INT >= 21) {
                    positive.setBackgroundTintList(ColorStateList.valueOf(themeButtonColor));
                }
            }
            if (negative != null) {
                negative.setTextColor(themeAccentColor);
                if (Build.VERSION.SDK_INT >= 21) {
                    negative.setBackgroundTintList(ColorStateList.valueOf(themeButtonColor));
                }
            }

            // Add spacing between Cancel and Delete buttons
            int gap = (int) (16 * getResources().getDisplayMetrics().density);
            if (positive != null && positive.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) positive.getLayoutParams();
                lp.setMarginStart(gap);
                lp.topMargin = Math.max(lp.topMargin, gap / 2);
                positive.setLayoutParams(lp);
            }
            if (negative != null && negative.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) negative.getLayoutParams();
                lp.setMarginEnd(gap);
                lp.topMargin = Math.max(lp.topMargin, gap / 2);
                negative.setLayoutParams(lp);
            }
        });

        dialog.show();

        // Ensure window background is set immediately to match theme
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(themeBackgroundColor));
        }
    }

    /**
     * Opens MapActivity for the given city.
     * Geocodes the city name to get coordinates and passes them to MapActivity via Intent.
     *
     * @param city the city name to display on the map
     */
    private void openMapActivity(final String city) {
        // Run geocoding in background thread since it may perform network I/O
        /**
         * Background thread Runnable that geocodes the city name to obtain coordinates.
         * Posts the geocoding results (latitude, longitude, success status) to the UI thread
         * for launching MapActivity or showing an error.
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocationName(city, 1);
                } catch (IOException e) {
                    addresses = null;
                }

                final double latitude;
                final double longitude;
                final boolean success;

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    latitude = address.getLatitude();
                    longitude = address.getLongitude();
                    success = true;
                } else {
                    latitude = 0.0;
                    longitude = 0.0;
                    success = false;
                }

                /**
                 * UI thread Runnable that processes geocoding results.
                 * If geocoding was successful, creates Intent to launch MapActivity with city coordinates.
                 * Otherwise, displays an error toast to the user.
                 */
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            // Create Intent to open MapActivity
                            Intent intent = new Intent(MainActivity.this, MapActivity.class);
                            intent.putExtra("city", city);
                            intent.putExtra("latitude", latitude);
                            intent.putExtra("longitude", longitude);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to find location for " + city, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }
}
