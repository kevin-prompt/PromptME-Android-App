package com.coolftc.prompt;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/*
 *  This class allows access to the database & tables that hold the reminder information.
 *  NOTE: There are 3 primary objects you will work with.  The MessageDB, Readable or
 *  Writable Databases and Cursors.
 *  Cursor   - Call close() as soon as possible (unless using a managed cursor, then never call close()).
 *  Database - Never call close().  If using multiple MessageDB (SQLiteOpenHelper) across threads,
 *              you may get "database is locked" exceptions on occasion.
 *  MessageDB- Call close() as little as possible, but always before the context ends. Try to create
 *              as few of these as possible. If you just have one, the "database is locked" problems
 *              should be very rare.
 *
 *  Note: Creating the MessageDB does not try to do a create/upgrade.  That only
 *  happens upon first read/write database call.  So having (at least the first)
 *  read/write database call in an asynchronous thread will be good for performance.
 */
public class MessageDB  extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "message.db";
    private static final int DATABASE_VERSION = 1;
    // Status Table & Attributes
    public static final String MESSAGE_TABLE = "message";
    public static final String MESSAGE_ID = BaseColumns._ID;
    public static final String MESSAGE_UNIQUE = "uniquename";
    public static final String MESSAGE_NAME = "targetname";
    public static final String MESSAGE_TIME = "timeexact";
    public static final String MESSAGE_TIMENAME = "timename";
    public static final String MESSAGE_TIMEADJ = "timeadj";
    public static final String MESSAGE_R_UNIT = "recurunit";
    public static final String MESSAGE_R_PERIOD = "recurperiod";
    public static final String MESSAGE_R_NUMBER = "recurnbr";
    public static final String MESSAGE_R_END = "recurend";
    public static final String MESSAGE_MSG = "promptmsg";
    public static final String MESSAGE_STATUS = "status";
    public static final String MESSAGE_CREATE = "created";
    public static final String MESSAGE_PROCESSED = "processed";

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

    public MessageDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public MessageDB(Context context, SQLiteDatabase.CursorFactory cf) {
        super(context, DATABASE_NAME, cf, DATABASE_VERSION);
    }

    /* This method is called when a database is not found, so we create the tables here. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Status Table.
        db.execSQL("create table "  + MESSAGE_TABLE + " (" +
                MESSAGE_ID          + TABLE_TYPE_INT + " primary key autoincrement" + TABLE_DELIMIT +
                MESSAGE_UNIQUE      + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_NAME        + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_TIME        + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_TIMENAME    + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_TIMEADJ     + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_R_UNIT      + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_R_PERIOD    + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_R_NUMBER    + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_R_END       + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_MSG         + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_STATUS      + TABLE_TYPE_INT  + TABLE_DELIMIT +
                MESSAGE_CREATE      + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_PROCESSED   + TABLE_TYPE_BOOL + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // When there is an older database found, this method is called on startup.
        // The version number presented is the basis for "older".

        /* This upgrade will wipe all data.  Remove the tables.
         * NOTE: If an upgrade is ever done, probably want to do something other
         *       than drop table since this data is not stored anywhere else.
         *       Until your know what the new table will look like, not much point
         *      in doing anything.
         */
        db.execSQL("DROP TABLE IF EXISTS " + MESSAGE_TABLE);

        // Recreates the database with a new version
        onCreate(db);
    }

}
