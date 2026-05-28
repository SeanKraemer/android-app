package edu.uiuc.cs427app;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper for the app's locations database.
 * Responsible for creating the schema on first run and handling upgrades.
 */
public class LocationDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "locations.db";
    private static final int DATABASE_VERSION = 1;

    /**
     * Creates a new DB helper instance for the locations database.
     *
     * @param context an application or provider context used to open/create the DB
     */
    public LocationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * Creates the locations table with a unique constraint on (username, city).
     *
     * @param db an open, writable database
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_LOCATIONS_TABLE =
                "CREATE TABLE " + LocationContract.LocationEntry.TABLE_NAME + " ("
                        + LocationContract.LocationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + LocationContract.LocationEntry.COLUMN_USER + " TEXT NOT NULL, "
                        + LocationContract.LocationEntry.COLUMN_CITY + " TEXT NOT NULL, "
                        + "UNIQUE (" + LocationContract.LocationEntry.COLUMN_USER + ", " + LocationContract.LocationEntry.COLUMN_CITY + ") ON CONFLICT IGNORE"
                        + ");";
        db.execSQL(SQL_CREATE_LOCATIONS_TABLE);
    }

    /**
     * Called when the database needs to be upgraded.
     * Current strategy: drop the existing locations table and recreate it.
     *
     * @param db the database
     * @param oldVersion the previous database version
     * @param newVersion the new database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LocationContract.LocationEntry.TABLE_NAME);
        onCreate(db);
    }
}
