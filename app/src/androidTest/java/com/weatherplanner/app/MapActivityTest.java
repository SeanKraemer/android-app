package com.weatherplanner.app;

import android.content.Intent;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MapActivityTest {

    // Tolerance for floating point comparison
    private static final float DELTA = 0.01f;

    // --- TEST 1: CHAMPAIGN ---
    @Test
    public void testMapActivity_displaysChampaign() {
        // Mock Data for City 1
        String city = "Champaign";
        double lat = 40.1163;
        double lon = -88.2435;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent.putExtra("city", city);
        intent.putExtra("latitude", lat);
        intent.putExtra("longitude", lon);

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(2000); // Wait for map to load

            if (BuildConfig.MAPS_API_KEY == null || BuildConfig.MAPS_API_KEY.trim().isEmpty()) {
                onView(withId(R.id.mapDemoMessage))
                        .check(matches(isDisplayed()))
                        .check(matches(withText(containsString("Demo mode"))));
                return;
            }

            scenario.onActivity(activity -> {
                SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.map);

                if (mapFragment != null) {
                    mapFragment.getMapAsync(googleMap -> {
                        LatLng cameraTarget = googleMap.getCameraPosition().target;
                        assertEquals("Latitude should match Champaign", lat, cameraTarget.latitude, DELTA);
                        assertEquals("Longitude should match Champaign", lon, cameraTarget.longitude, DELTA);
                    });
                }
            });
        }
    }

    // --- TEST 2: CHICAGO ---
    @Test
    public void testMapActivity_displaysChicago() {
        // Mock Data for City 2
        String city = "Chicago";
        double lat = 41.8832;
        double lon = -87.6323;

        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MapActivity.class);
        intent.putExtra("city", city);
        intent.putExtra("latitude", lat);
        intent.putExtra("longitude", lon);

        try (ActivityScenario<MapActivity> scenario = ActivityScenario.launch(intent)) {
            SystemClock.sleep(2000); // Wait for map to load

            if (BuildConfig.MAPS_API_KEY == null || BuildConfig.MAPS_API_KEY.trim().isEmpty()) {
                onView(withId(R.id.mapDemoMessage))
                        .check(matches(isDisplayed()))
                        .check(matches(withText(containsString("Demo mode"))));
                return;
            }

            scenario.onActivity(activity -> {
                SupportMapFragment mapFragment = (SupportMapFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.map);

                if (mapFragment != null) {
                    mapFragment.getMapAsync(googleMap -> {
                        LatLng cameraTarget = googleMap.getCameraPosition().target;
                        assertEquals("Latitude should match Chicago", lat, cameraTarget.latitude, DELTA);
                        assertEquals("Longitude should match Chicago", lon, cameraTarget.longitude, DELTA);
                    });
                }
            });
        }
    }
}
