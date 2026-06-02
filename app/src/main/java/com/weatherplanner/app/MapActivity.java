package com.weatherplanner.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * MapActivity displays an interactive Google Map for a selected city.
 *
 * This activity:
 * 1. Receives city name, latitude, and longitude from MainActivity via Intent
 * 2. Displays city information in TextViews
 * 3. Shows an interactive Google Map centered on the city coordinates
 * 4. Adds a marker at the city location
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    // City information
    private String cityName;
    private double latitude;
    private double longitude;

    // Database helper for user themes
    private UserDatabaseHelper dbHelper;

    // Theme colors
    private int themeBackgroundColor;
    private int themeTextColor;
    private int themeToolbarColor;

    // SharedPreferences constants
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_CURRENT_USER = "current_user";

    // UI Components
    private TextView cityNameTextView;
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView mapDemoMessageTextView;

    // Google Map
    private GoogleMap googleMap;

    /**
     * Lifecycle callback invoked when the activity is being created.
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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

        String backgroundColor = user != null && user.themeBackground != null ? user.themeBackground : "#FFFFFF";
        String textColor = user != null && user.themeText != null ? user.themeText : "#000000";
        String toolbarColor = user != null && user.themeToolbar != null ? user.themeToolbar : "#4CAF50";

        themeBackgroundColor = Color.parseColor(backgroundColor);
        themeTextColor = Color.parseColor(textColor);
        themeToolbarColor = Color.parseColor(toolbarColor);

        // Get city information from Intent
        Intent intent = getIntent();
        cityName = intent.getStringExtra("city");
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);

        if (cityName == null) {
            cityName = "Unknown City";
        }

        // Set action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(cityName + " Map");
        }

        // Initialize UI elements
        initializeViews();

        // Apply theme
        applyTheme(toolbar);

        if (hasMapsApiKey()) {
            initializeLiveMap();
        } else {
            showMapFallback();
        }
    }

    /**
     * Initializes all UI components by finding them in the layout.
     */
    private void initializeViews() {
        cityNameTextView = findViewById(R.id.cityNameTextView);
        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        mapDemoMessageTextView = findViewById(R.id.mapDemoMessage);

        // Display city information
        cityNameTextView.setText(getString(R.string.map_city_format, cityName));
        latitudeTextView.setText(getString(R.string.map_latitude_format, latitude));
        longitudeTextView.setText(getString(R.string.map_longitude_format, longitude));
    }

    /**
     * Applies the user's theme to all UI components.
     * @param toolbar The toolbar to apply theme to
     */
    private void applyTheme(Toolbar toolbar) {
        cityNameTextView.setTextColor(themeTextColor);
        latitudeTextView.setTextColor(themeTextColor);
        longitudeTextView.setTextColor(themeTextColor);
        if (mapDemoMessageTextView != null) {
            mapDemoMessageTextView.setTextColor(themeTextColor);
        }
        toolbar.setBackgroundColor(themeToolbarColor);
        getWindow().getDecorView().setBackgroundColor(themeBackgroundColor);
    }

    private boolean hasMapsApiKey() {
        return BuildConfig.MAPS_API_KEY != null && !BuildConfig.MAPS_API_KEY.trim().isEmpty();
    }

    private void initializeLiveMap() {
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.map, mapFragment)
                .commitNow();
        mapFragment.getMapAsync(this);
    }

    private void showMapFallback() {
        if (mapDemoMessageTextView != null) {
            mapDemoMessageTextView.setText(getString(R.string.map_demo_message));
            mapDemoMessageTextView.setVisibility(TextView.VISIBLE);
        }
    }

    /**
     * Callback invoked when the Google Map is ready to be used.
     * Centers the map on the city coordinates and adds a marker.
     *
     * @param map The GoogleMap instance
     */
    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;

        // Create a LatLng object for the city coordinates
        LatLng cityLocation = new LatLng(latitude, longitude);

        // Add a marker at the city location
        googleMap.addMarker(new MarkerOptions()
                .position(cityLocation)
                .title(cityName));

        // Move camera to the city location with appropriate zoom level
        // Zoom level 10 provides a good city-level view
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityLocation, 10));

        // Enable zoom controls
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    /**
     * Handles the back button being pressed.
     * @return true if the back button was pressed
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
