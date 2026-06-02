package com.weatherplanner.app;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for the Location Content Provider.
 *
 * This class defines the database schema and URIs used to access location data
 * through the LocationProvider. It follows the Android Content Provider contract
 * pattern to provide a structured way to query, insert, update, and delete
 * location records for different users.
 *
 */
public final class LocationContract {
    private LocationContract() {}

    public static final String AUTHORITY = "com.weatherplanner.app.locationprovider";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static final String PATH_LOCATIONS = "locations";

    public static final class LocationEntry implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_LOCATIONS);

        public static final String TABLE_NAME = "locations";
        public static final String COLUMN_USER = "username"; // owner of the location
        public static final String COLUMN_CITY = "city"; // display name of city
    }
}
