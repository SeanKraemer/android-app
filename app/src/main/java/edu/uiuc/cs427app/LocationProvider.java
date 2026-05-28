package edu.uiuc.cs427app;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ContentProvider that exposes CRUD operations for per-user cities.
 * Backed by a SQLite database managed by {@link LocationDbHelper} and the schema
 * defined in {@link LocationContract}.
 *
 * Supported URIs:
 * - content://edu.uiuc.cs427app.locationprovider/locations   (directory)
 * - content://edu.uiuc.cs427app.locationprovider/locations/# (single item by _ID)
 */
public class LocationProvider extends ContentProvider {
    private static final int LOCATIONS = 100;
    private static final int LOCATION_ID = 101;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private LocationDbHelper dbHelper;

    /**
     * Builds the UriMatcher for routing incoming content URIs to handler codes.
     *
     * @return a configured UriMatcher supporting collection and item URIs
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(LocationContract.AUTHORITY, LocationContract.PATH_LOCATIONS, LOCATIONS);
        matcher.addURI(LocationContract.AUTHORITY, LocationContract.PATH_LOCATIONS + "/#", LOCATION_ID);
        return matcher;
    }

    /**
     * Initializes the provider and its database helper.
     *
     * @return true when successfully initialized
     */
    @Override
    public boolean onCreate() {
        dbHelper = new LocationDbHelper(getContext());
        return true;
    }

    /**
     * Queries the locations table. Supports both directory and single-item lookups.
     *
     * @param uri the content URI to query
     * @param projection the list of columns to return
     * @param selection a filter declaring which rows to return, formatted as an SQL WHERE clause
     * @param selectionArgs arguments for the selection placeholders
     * @param sortOrder how to order the rows, formatted as an SQL ORDER BY clause
     * @return a Cursor over the result set
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        final SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor retCursor;
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS: {
                retCursor = db.query(LocationContract.LocationEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            }
            case LOCATION_ID: {
                String id = uri.getLastPathSegment();
                String sel = LocationContract.LocationEntry._ID + "=?";
                retCursor = db.query(LocationContract.LocationEntry.TABLE_NAME, projection, sel, new String[]{id}, null, null, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (getContext() != null && retCursor != null) {
            retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return retCursor;
    }

    /**
     * Returns the MIME type for the given URI, indicating directory or single item.
     *
     * @param uri the content URI
     * @return a MIME type string for the provided URI
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                return "vnd.android.cursor.dir/" + LocationContract.AUTHORITY + "." + LocationContract.PATH_LOCATIONS;
            case LOCATION_ID:
                return "vnd.android.cursor.item/" + LocationContract.AUTHORITY + "." + LocationContract.PATH_LOCATIONS;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /**
     * Inserts a new row into the locations table.
     *
     * @param uri the target content URI (must be the collection URI)
     * @param values key-value pairs representing the row to insert
     * @return the URI of the newly inserted row, or null if insertion failed
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS: {
                long id = db.insert(LocationContract.LocationEntry.TABLE_NAME, null, values);
                if (id > 0) {
                    Uri newUri = ContentUris.withAppendedId(LocationContract.LocationEntry.CONTENT_URI, id);
                    if (getContext() != null) {
                        getContext().getContentResolver().notifyChange(newUri, null);
                    }
                    return newUri;
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        return null;
    }

    /**
     * Deletes rows from the locations table.
     *
     * @param uri the content URI (collection or item)
     * @param selection SQL WHERE clause without 'WHERE'
     * @param selectionArgs arguments for the selection placeholders
     * @return the number of rows deleted
     */
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted;
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                rowsDeleted = db.delete(LocationContract.LocationEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case LOCATION_ID:
                String id = uri.getLastPathSegment();
                rowsDeleted = db.delete(LocationContract.LocationEntry.TABLE_NAME, LocationContract.LocationEntry._ID + "=?", new String[]{id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsDeleted != 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    /**
     * Updates rows in the locations table.
     *
     * @param uri the content URI (collection or item)
     * @param values new values to apply
     * @param selection SQL WHERE clause without 'WHERE'
     * @param selectionArgs arguments for the selection placeholders
     * @return the number of rows updated
     */
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated;
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                rowsUpdated = db.update(LocationContract.LocationEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case LOCATION_ID:
                String id = uri.getLastPathSegment();
                rowsUpdated = db.update(LocationContract.LocationEntry.TABLE_NAME, values, LocationContract.LocationEntry._ID + "=?", new String[]{id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        if (rowsUpdated != 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
}
