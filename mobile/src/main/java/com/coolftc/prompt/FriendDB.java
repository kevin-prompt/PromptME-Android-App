package com.coolftc.prompt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/*
 *  This class allows access to the database & tables that hold the friend information.
 *  NOTE: There are 3 primary objects you will work with.  The FriendDB, Readable or
 *  Writable Databases and Cursors.
 *  Cursor   - Call close() as soon as possible (unless using a managed cursor, then never call close()).
 *  Database - Never call close().  If using multiple FriendDB (SQLiteOpenHelper) across threads,
 *              you may get "database is locked" exceptions on occasion.
 *  FriendDB- Call close() as little as possible, but always before the context ends. Try to create
 *              as few of these as possible. If you just have one, the "database is locked" problems
 *              should be very rare.
 *
 *  Note: Creating the FriendDB does not try to do a create/upgrade.  That only
 *  happens upon first read/write database call.  So having (at least the first)
 *  read/write database call in an asynchronous thread will be good for performance.
 */
public class FriendDB extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "friend.db";
    private static final int DATABASE_VERSION = 1;
    // Status Table & Attributes
    public static final String FRIEND_TABLE = "friend";
    public static final String FRIEND_ID = BaseColumns._ID;
    public static final String FRIEND_ACCT_ID = "idacct";
    public static final String FRIEND_UNIQUE = "uniquename";
    public static final String FRIEND_DISPLAY = "displayname";
    public static final String FRIEND_TIMEZONE = "timezone";
    public static final String FRIEND_SCYCLE = "sleepcycle";
    public static final String FRIEND_CONTACT_ID = "idcontact";
    public static final String FRIEND_CONTACT_NAME = "namecontact";
    public static final String FRIEND_CONTACT_PIC = "piccontact";
    public static final String FRIEND_MIRROR = "mirror";
    public static final String FRIEND_PENDING = "pending";
    public static final String FRIEND_CONFIRM = "confirm";

    // Extra helper data
    // see http://www.sqlite.org/datatype3.html for information about sqlite datatypes.
    private static final String TABLE_TYPE_TEXT = " text";
    private static final String TABLE_TYPE_INT = " integer";
    //private static final String TABLE_TYPE_DATE = " date";    // ends up "numeric" in table, better to just use string
    private static final String TABLE_TYPE_FLOAT = " real";
    private static final String TABLE_TYPE_BOOL = " boolean";   // ends up "numeric" in table
    private static final String TABLE_DELIMIT = ",";
    public static final int SQLITE_TRUE = 1;                    // Boolean is not supported in the
    public static final int SQLITE_FALSE = 0;                   // db, so we have to improvise.

    public FriendDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public FriendDB(Context context, SQLiteDatabase.CursorFactory cf) {
        super(context, DATABASE_NAME, cf, DATABASE_VERSION);
    }

    /* This method is called when a database is not found, so we create the tables here. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Status Table.
        db.execSQL("create table "  + FRIEND_TABLE + " (" +
                FRIEND_ID           + TABLE_TYPE_INT + " primary key autoincrement" + TABLE_DELIMIT +
                FRIEND_ACCT_ID      + TABLE_TYPE_INT + TABLE_DELIMIT +
                FRIEND_UNIQUE       + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                FRIEND_DISPLAY      + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                FRIEND_TIMEZONE     + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                FRIEND_SCYCLE       + TABLE_TYPE_INT  + TABLE_DELIMIT +
                FRIEND_CONTACT_ID   + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                FRIEND_CONTACT_NAME + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                FRIEND_CONTACT_PIC  + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                FRIEND_MIRROR       + TABLE_TYPE_BOOL + TABLE_DELIMIT +
                FRIEND_PENDING      + TABLE_TYPE_BOOL + TABLE_DELIMIT +
                FRIEND_CONFIRM      + TABLE_TYPE_BOOL + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // When there is an older database found, this method is called on startup.
        // The version number presented is the basis for "older".

        // This upgrade will wipe all data.  Remove the tables.
        // NOTE: Recreating the table from scratch is not a big deal, since the data
        //       is recreated from Local Contacts and Saved Friends.
        db.execSQL("DROP TABLE IF EXISTS " + FRIEND_TABLE);

        // Recreates the database with a new version
        onCreate(db);
    }


}