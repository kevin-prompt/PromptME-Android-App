package com.coolftc.prompt;

import android.provider.BaseColumns;

public class Constants {

    /*
     *  No one is suppose to actually use this class.  To access the values
     *  use a import static com.coolftc.prompt.Constants.*;
     */
    private Constants(){}

    /* Constants used in dealing with the Shared Preference Database. */
    public static final String SP_REG_STORE = "prompt.registry";
    public static final String SP_REG_ID = "prompt.id";
    public static final String SP_REG_TICKET = "prompt.ticket";
    public static final String SP_REG_GCM = "prompt.gcmtoken";
    public static final String SP_REG_GCM_AGE = "prompt.gcmage";
    public static final String SP_REG_GCM_ID = "prompt.gcmid";
    public static final String SP_REG_UNIQUE = "prompt.unique";
    public static final String SP_REG_DISPLAY = "prompt.display";
    public static final String SP_REG_SCYCLE = "prompt.sleepcycle";
    public static final String SP_REG_CTID = "prompt.contactid";
    public static final String SP_REG_CTNAME = "prompt.contactname";
    public static final String SP_REG_CTFACE = "prompt.contactface";
    public static final String SP_REG_DBID = "prompt.localdb";
    public static final String SP_REG_CONFIRM = "prompt.confirmed";
    public static final String SP_REG_FORCE = "prompt.force";
    public static final long Owner_ID = -1;

    /*
     *  The various display date format templates.  The time has two variations, 24 hour clock and AM/PM.
     *  The dates have three major variations based on cultural ordering of elements, and some additional
     *  play based on separators and how Months are represented.
     */
    public static final String DB_fmtDateShrtMiddle = "MM/dd/yyyy";         // On wikipedia these 3 formats, most commonly
    public static final String DB_fmtDateShrtLittle = "dd/MM/yyyy";         // used across countries, are called
    public static final String DB_fmtDateShrtBig = "yyyy/MM/dd";            // big/little/middle Endian.
    public static final String DB_fmtDateMonthMiddle = "MMM dd, yyyy";      // Middle = m/d/y used in USA
    public static final String DB_fmtDateMonthLittle = "dd MMM, yyyy";      // Little = d/m/y used in India, Russia, South America
    public static final String DB_fmtDateMonthBig = "yyyy, MMM dd";         // Big = y/m/d used in China
    public static final String DB_fmtLongMonthMiddle = "MMMM dd, yyyy";     // Full Month Name = MMMM
    public static final String DB_fmtLongMonthLittle = "dd MMMM, yyyy";     //
    public static final String DB_fmtLongMonthBig = "yyyy, MMMM dd";        //
    public static final String DB_fmtDateNoYearBigMid = "MMM dd";           // Without the year, big & middle the same
    public static final String DB_fmtDateNoYearLittle = "dd MMM";
    public static final String DB_fmtDateTime = "h:mmaa";
    public static final String DB_fmtDateTime24 = "kk:mm  ";
    public static final String DB_fmtDateTimeZone = "h:mmaa z";
    public static final String DB_fmtDayOfWeek = "EEEE";
    public static final String BAD_TIME = "99:99";
    public static final String THE_PAST = "1964-02-06T11:50:25+00:00";      // Handy initialization
    // Date Formatting Constants
    public static final int DATE_FMT_SHORT = 0;             // Short Date Display format

    /* Constants used as codes for cross Activity communications. */
    public static final String KEVIN_SPEAKS = "Kevin Speaks";
    public static final int KY_SIGNUP = 10001;
    public static final int KY_CONTACTPIC = 10002;
    public static final int KY_ENTRY = 10003;
    public static final int KY_DATETIME = 10004;
    public static final int KY_RECURE = 10005;
    public static final int KY_PLAYSTORE = 10006;
    public static final String IN_DSPL_NAME = "DispalyName";
    public static final String IN_DSPL_TGT = "TargetAddr";
    public static final String IN_USER_ACCT = "UserAccount";
    public static final String IN_TIMESTAMP = "timestamp";
    public static final String IN_EXACTPICK = "exactpick";
    public static final String IN_PERIOD = "period";
    public static final String IN_UNIT = "unit";
    public static final String IN_ENDTIME = "endtime";
    public static final String IN_ENDNBR = "endnumber";
    public static final String IN_DISP_TIME = "displaytime";
    public static final int SEC_READ_CONTACTS = 10003;
    public static final int FR_POS_EXDATE = 0;
    public static final int FR_POS_EXTIME = 1;

    /* Constants used in dealing with the FutureTell API and other web sites */
    public static final String SUB_ZZZ = "ZZZ";
    public static final String FTI_BaseURL = "http://d9f51b5b1b3c412bb637cb5fca495d3e.cloudapp.net";
    public static final String FTI_Status = "/v1/status";
    public static final String FTI_Ping = "/v1/status/ping";
    public static final String FTI_Register = "/v1/user";
    public static final String FTI_RegisterExtra = "/v1/user/"+SUB_ZZZ;
    public static final String FTI_Message = "/v1/user/"+SUB_ZZZ+"/note";
    public static final String FTI_Friends = "/v1/user/"+SUB_ZZZ+"/friend";
    public static final String FTI_Ticket = "?ticket=";
    public static final int FTI_TIMEOUT = 30000;
    public static final int FTI_TYPE_ANDROID = 3;       // 3 = Android GCM
    public static final String FTI_DIGIT_VERIFY = "2";  // 2 = Digits
    public static final int NETWORK_DOWN = 99;          // Sometimes it is nice to know the issue was a bad network.

    /* Third Party connection information (Obfuscation recommended). */
    /* Twitter - Fabric - Digits user secrets */
    /* If the TWITTER_KEY is replaced here, it must also be replaced on the server (since it is used for verification). */
    public static final String TWITTER_KEY = "IbUE8sGOxs3nDsWAVh537VyH2";
    public static final String TWITTER_SECRET = "CPktuMWtbgLAIH0GNHUpTz6dgVOs8VT4l8XCdcwaHWNjytFSab";
    /* The Google GCM account (called Project number in console) */
    public static final String SENDER_ID = "800396557293";

    /* Mapping constants. */
    /* Contact list. */
    public static final String CP_PER_ID = "person";
    public static final String CP_TYPE = "type";
    public static final String CP_NAME = "name";
    public static final String CP_EXTRA = "extra";
    public static final String CP_FACE = "face";
    public static final String TITLE_ROW = "AliCon2016";
    /* History list. */
    public static final String HS_REM_ID = "reminder";
    public static final String HS_TIME = "time";
    public static final String HS_WHO = "who";
    public static final String HS_MSG = "message";
    /* Recurrence. */
    public static final int UNIT_TYPE_DAY = 4;
    public static final int UNIT_TYPE_MONTH = 6;
    public static final int UNIT_TYPE_WEEKDAY = 100;
    public static final int RECUR_INVALID = -1;         // Values start here.
    public static final int RECUR_UNIT_DEFAULT = UNIT_TYPE_DAY;
    public static final int RECUR_PERIOD_DEFAULT = 1;
    public static final String RECUR_END_DEFAULT = "";  // The date picker will fill this out.
    public static final int RECUR_END_NBR = 4;
    public static final int SUN_FLAG = 1;
    public static final int MON_FLAG = 2;
    public static final int TUE_FLAG = 4;
    public static final int WED_FLAG = 8;
    public static final int THU_FLAG = 16;
    public static final int FRI_FLAG = 32;
    public static final int SAT_FLAG = 64;
    public static final int FOREVER_MORE = 1000;
    public static final int FOREVER_LESS = 500;
    /* Notification */
    public static final String IN_NOTE_KEY = "noteId";
    public static final String IN_NOTE_MSG = "message";
    public static final String IN_NOTE_FROM = "fromName";
    public static final String IN_NOTE_FROMID = "fromId";
    public static final String IN_NOTE_RAW = "line2-notify";

    /* Constants used in dealing with the SQLite Database and Shared Preference Database. */
    public static final String Owner_DBID = "-1";
    public static final String ISO3166_Default = "US";
    public static final String DB_FriendsAll = "select * from friend";
    public static final String DB_FriendsWhere = "select * from friend where confirm = " + SUB_ZZZ;
    public static final String DB_Table_ID = BaseColumns._ID + " = " + SUB_ZZZ;
    public static final String DB_FriendExact = "select * from friend where " + DB_Table_ID;
    public static final String DB_MessagesAll = "select " + MessageDB.MESSAGE_ID + "," + MessageDB.MESSAGE_NAME + "," +
            MessageDB.MESSAGE_TIME + "," + MessageDB.MESSAGE_MSG + "," + MessageDB.MESSAGE_STATUS + "," + MessageDB.MESSAGE_PROCESSED +
            " from message order by " + MessageDB.MESSAGE_ID + " desc";
    public static final int UPD_SCREEN_TM = 30000;                  // How often in msec to update the screen from data base
    public static final int UPD_SCREEN_TQ = 10000;                  // How often in msec to update the screen from data base (first minute)



}
