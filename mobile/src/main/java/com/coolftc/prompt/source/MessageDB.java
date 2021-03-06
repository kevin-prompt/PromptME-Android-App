package com.coolftc.prompt.source;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 *  This class allows access to the database & tables that hold the reminder information.
    NOTE: There are 3 primary objects you will work with.  The MessageDB, Readable or
    Writable Databases and Cursors.
    Cursor   - Call close() as soon as possible (unless using a managed cursor, then never call close()).
    Database - Never call close().  If using multiple MessageDB (SQLiteOpenHelper) across threads,
                you may get "database is locked" exceptions on occasion.
    MessageDB- Call close() as little as possible, but always before the context ends. Try to create
                as few of these as possible. If you just have one, the "database is locked" problems
                should be very rare.

 *  Note: Creating the MessageDB does not try to do a create/upgrade.  That only
    happens upon first read/write database call.  So having (at least the first)
    read/write database call in an asynchronous thread will be good for performance.
 */
public class MessageDB extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "message.db";
    private static final int DATABASE_VERSION = 1;
    // Status Table & Attributes
    public static final String MESSAGE_TABLE = "message";           // Table name.
    public static final String MESSAGE_ID = BaseColumns._ID;        // Table key.
    public static final String MESSAGE_TARGET = "uniquename";       // Unique name of the message target.
    public static final String MESSAGE_SOURCE = "sourcename";       // Unique name of the message source.
    public static final String MESSAGE_NAME = "targetname";         // Display name of the message target.
    public static final String MESSAGE_FROM = "fromname";           // Display name of the message source.
    public static final String MESSAGE_TIME = "timeexact";          // The actual expected delivery time (per server).
    public static final String MESSAGE_TIMENAME = "timename";       // The code used for simplified time name.
    public static final String MESSAGE_TIMEADJ = "timeadj";         // The code used for simplified time adjustment.
    public static final String MESSAGE_SLEEP = "sleepcycle";        // The sleep cycle in place at the time of the message.
    public static final String MESSAGE_TIMEZONE = "timezone";       // The time zone in place at the time of the message.
    public static final String MESSAGE_R_UNIT = "recurunit";        // Part of recurring parameters.  Units = 0 means no recurrence.
    public static final String MESSAGE_R_PERIOD = "recurperiod";    // Part of recurring parameters.
    public static final String MESSAGE_R_NUMBER = "recurnbr";       // Part of recurring parameters.
    public static final String MESSAGE_R_END = "recurend";          // Part of recurring parameters.
    public static final String MESSAGE_MSG = "promptmsg";           // The message.
    public static final String MESSAGE_SRVR_ID = "serverid";        // The note id of the message on the server.
    public static final String MESSAGE_STATUS = "status";           // The status of the message wrt the server. Non-zero = non-good.
    public static final String MESSAGE_SNOOZE_ID = "snoozeid";      // The new note id of the message on the server.
    public static final String MESSAGE_CREATE = "created";          // The timestamp of the record in this local database (in UTC).
    public static final String MESSAGE_PROCESSED = "processed";     // True = server has processed the request.  See status for how that went.

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
                MESSAGE_TARGET      + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_SOURCE      + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_NAME        + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_FROM        + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_TIME        + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_TIMENAME    + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_TIMEADJ     + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_SLEEP       + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_TIMEZONE    + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_R_UNIT      + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_R_PERIOD    + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_R_NUMBER    + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_R_END       + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_MSG         + TABLE_TYPE_TEXT + TABLE_DELIMIT +
                MESSAGE_SRVR_ID     + TABLE_TYPE_INT + TABLE_DELIMIT +
                MESSAGE_STATUS      + TABLE_TYPE_INT  + TABLE_DELIMIT +
                MESSAGE_SNOOZE_ID   + TABLE_TYPE_INT + TABLE_DELIMIT +
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
