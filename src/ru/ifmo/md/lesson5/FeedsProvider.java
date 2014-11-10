package ru.ifmo.md.lesson5;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class FeedsProvider extends ContentProvider {

    static final String PROVIDER_NAME = "ru.ifmo.md.lesson5.Feeds";
    static final String URL = "content://" + PROVIDER_NAME + "/feeds";
    static final Uri CONTENT_URI = Uri.parse(URL);

    static final String _ID = "_id";
    static final String NAME = "name";
    static final String TITLE = "title";
    static final String DESC = "desc";
    static final String LINK = "link";

    private static HashMap<String, String> STUDENTS_PROJECTION_MAP;

    static final int FEEDS = 1;
    static final int FEED_ID = 2;

    static final UriMatcher uriMatcher;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "feeds", FEEDS);
        uriMatcher.addURI(PROVIDER_NAME, "feeds/#", FEED_ID);
    }

    private SQLiteDatabase db;
    static final String DATABASE_NAME = "Feeds";
    static final String STUDENTS_TABLE_NAME = "feeds";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_DB_TABLE =
        " CREATE TABLE " + STUDENTS_TABLE_NAME +
                " (" + _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                NAME + " TEXT NOT NULL, " +
                TITLE + " TEXT NOT NULL, " +
                DESC + " TEXT NOT NULL, " +
                LINK + " TEXT NOT NULL);";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " +  STUDENTS_TABLE_NAME);
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        db = dbHelper.getWritableDatabase();
        return (db != null);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long rowID = db.insert(STUDENTS_TABLE_NAME, "", values);
        if (rowID > 0)
        {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(STUDENTS_TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case FEEDS:
                qb.setProjectionMap(STUDENTS_PROJECTION_MAP);
                break;
            case FEED_ID:
                qb.appendWhere( _ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (sortOrder == null || sortOrder.equals("")){
            sortOrder = NAME;
        }
        Cursor c = qb.query(db,	projection,	selection, selectionArgs,
                null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case FEEDS:
                count = db.delete(STUDENTS_TABLE_NAME, selection, selectionArgs);
                break;
            case FEED_ID:
                String id = uri.getPathSegments().get(1);
                count = db.delete( STUDENTS_TABLE_NAME, _ID +  " = " + id +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count = 0;

        switch (uriMatcher.match(uri)){
            case FEEDS:
                count = db.update(STUDENTS_TABLE_NAME, values,
                        selection, selectionArgs);
                break;
            case FEED_ID:
                count = db.update(STUDENTS_TABLE_NAME, values, _ID +
                        " = " + uri.getPathSegments().get(1) +
                        (!TextUtils.isEmpty(selection) ? " AND (" +
                                selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri );
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }
}
