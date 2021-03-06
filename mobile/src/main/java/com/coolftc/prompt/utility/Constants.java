package com.coolftc.prompt.utility;

import android.provider.BaseColumns;

import com.coolftc.prompt.source.FriendDB;
import com.coolftc.prompt.source.MessageDB;

public class Constants {

    /*
     *  No one is suppose to actually use this class.  To access the values
     *  use a import static com.coolftc.prompt.utility.Constants.*;
     */
    private Constants(){}

    /*  Constants used in dealing with the Shared Preference Database.
     *  This is a different storage area than used by Settings, although
     *  a couple of the names are reused there.
     */
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
    public static final String SP_REG_PEND = "prompt.pending";
    public static final String SP_REG_CONFIRM = "prompt.confirmed";
    public static final String SP_REG_FORCE = "prompt.force";
    public static final String SP_REG_SOLO = "prompt.solo";
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
    // Date Formatting Constants
    public static final int DATE_FMT_SHORT = 0;             // Short Date Display format
    public static final int DATE_TIME_FMT_SHORT = 1;        // Short Date and Time Display format
    public static final int DATE_TIME_FMT_REV = 2;          // Short Time and Date Display format

    /* Constants used as codes for cross Activity communications. */
    public static final String KEVIN_SPEAKS = "Kevin Speaks";
    public static final int KY_SIGNUP = 10001;
    public static final int KY_CONTACTPIC = 10002;
    public static final int KY_ENTRY = 10003;
    public static final int KY_DATETIME = 10004;
    public static final int KY_RECURE = 10005;
    public static final int KY_PLAYSTORE = 10006;
    public static final String IN_CONFIRM_TYPE = "ConfirmType";
    public static final String IN_SMS_VCODE = "VerifyCode";
    public static final String IN_SMS_RESEND = "ResendToken";
    public static final String IN_DSPL_NAME = "DispalyName";
    public static final String IN_DSPL_TGT = "TargetAddr";
    public static final String IN_USER_ACCT = "UserAccount";
    public static final String IN_TIMESTAMP = "timestamp";
    public static final String IN_EXACTPICK = "exactpick";
    public static final String IN_PERIOD = "period";
    public static final String IN_FIRSTDOW = "dayofweek";
    public static final String IN_UNIT = "unit";
    public static final String IN_ENDTIME = "endtime";
    public static final String IN_ENDNBR = "endnumber";
    public static final String IN_DISP_TIME = "displaytime";
    public static final String IN_MESSAGE = "message";
    public static final String IN_ADDRESSES = "addresses";
    public static final String IN_LABELS = "labels";
    public static final String IN_ADDRESSES_TRUE = "addresses.select";
    public static final int SEC_READ_CONTACTS = 10013;
    public static final int SEC_WRITE_STORAGE = 10014;
    public static final int FR_POS_EXDATE = 0;
    public static final int FR_POS_EXTIME = 1;
    public static final String KY_HIST_FRAG = "history.sort";
    public static final String KY_CNTC_FRAG = "contactpicker.sort";
    public static final String KY_ADDR_FRAG = "contactpicker.fragment";
    /* Thread Constants */
    public static final int WHAT_OK_PING = 1000;
    public static final int WHAT_ERR_PING = 2000;
    public static final int WHAT_WS_DOMAIN = 1001;
    public static final int WHAT_WS_DOMAIN_ERR = 2001;

    /* Constants used in dealing with web service calls. */
    public static final int API_TIMEOUT = 30000;
    public static final int API_TIMEOUT_SHORT = 30000;
    public static final String API_HEADER_ACCEPT = "application/json";
    public static final String API_HEADER_CONTENT = "application/json";
    public static final String API_XFORM_CONTENT = "application/x-www-form-urlencoded";
    public static final String API_ENCODING = "UTF-8";
    public static final String API_BEARER = "bearer "; // note the trailing space
    public static final String SUB_ZZZ = "%ZZZ!";
    public static final String SUB_YYY = "%YYY!";     // if there are two substitutions together
    public static final String SP_BASE_URL = "API.baseURL";
    public static final String SP_BASE_URL_LIFE = "API.baseURL.age";

    /* Constants used in dealing with the FutureTell API and other web sites */
    public static final String FTI_BASE_CAMP_URL ="https://www.coolftc.org/Prompt/link/promptme.json";
    public static final String FTI_Status = "/v1/status";
    public static final String FTI_Ping = "/v1/status/ping";
    public static final String FTI_Register = "/v1/user";
    public static final String FTI_RegisterExtra = "/v1/user/"+SUB_ZZZ;
    public static final String FTI_Message = "/v1/user/"+SUB_ZZZ+"/prompt";
    public static final String FTI_Message_Del = "/v1/user/"+SUB_ZZZ+"/prompt/";
    public static final String FTI_Invite = "/v1/user/"+SUB_ZZZ+"/friend";
    public static final String FTI_Invite_Del = "/v1/user/"+SUB_ZZZ+"/friend/";
    public static final String FTI_Friends = "/v1/user/"+SUB_ZZZ+"/friend";
    public static final String FTI_Ticket = "?ticket=";
    public static final int FTI_TIMEOUT = 30000;
    public static final int FTI_TYPE_ANDROID = 3;       // 3 = Android GCM
    public static final String FTI_SOLO_DOMAIN = "@zalicon.com";
    public static final String FTI_DIGIT_VERIFY = "2";      // 2 = Digits
    public static final String FTI_FIREBASE_VERIFY = "3";   // 3 = Firebase
    public static final String FTI_SOLO_VERIFY = "4";       // 4 = Solo
    public static final int NETWORK_DOWN = 99;          // Sometimes it is nice to know the issue was a bad network.

    /* Constants used for Analytics */
    public static final String AN_UP_TICKET = "ticket";
    public static final String AN_EV_SEND = "prompt_send";
    public static final String AV_PM_SEND_WHO = "send_who";
    public static final String AV_PM_SEND_WHEN = "send_when";

    /* Mapping constants. */
    /* Contact list. */
    public static final String CP_PER_ID = "person";
    public static final String CP_TYPE = "type";
    public static final String CP_NAME = "name";
    public static final String CP_EXTRA = "extra";
    public static final String CP_UNIQUE = "unique";
    public static final String CP_LINKED = "linked";
    public static final String CP_FACE = "face";
    public static final String CP_BUTTON = "button";
    public static final String TITLE_ROW = "AliCon2016";
    /* History list. */
    public static final String HS_REM_ID = "history.reminder";
    public static final String HS_TIME = "history.time";
    public static final String HS_WHO_FROM = "history.source";
    public static final String HS_MSG = "history.message";
    public static final String HS_WHO_TO = "history.target";
    public static final String HS_RECURS = "history.recurs";
    public static final String HS_SNOOZE = "hostory.snooze";
    public static final String HS_LAST_15 = "history.new";
    public static final String HS_TIME_PAST = "history.past";
    /* Recurrence. */
    public static final int UNIT_TYPE_DAY = 4;
    public static final int UNIT_TYPE_MONTH = 6;
    public static final int UNIT_TYPE_WEEKDAY = 100;
    public static final int RECUR_INVALID = -1;         // Values start here.
    public static final int RECUR_UNIT_DEFAULT = UNIT_TYPE_DAY;
    public static final int RECUR_PERIOD_DEFAULT = 0;
    public static final String RECUR_END_DEFAULT = "";  // The date picker will fill this out.
    public static final int RECUR_END_NBR = 3;
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
    public static final String IN_NOTE_TYPE = "name";
    public static final String IN_NOTE_KEY = "noteId";
    public static final String IN_NOTE_FROM = "fromName";
    public static final String IN_NOTE_FROMID = "fromId";
    public static final String IN_NOTE_FROMNICE = "fromDisplay";
    public static final String IN_NOTE_PEND = "time";
    public static final String IN_NOTE_RECUR = "recurUnit";
    public static final String IN_NOTE_MSG = "message";
    public static final String IN_NOTE_MIRROR = "mirror";
    public static final String IN_TYPE_PROMPT = "NOTE";
    public static final String IN_TYPE_INVITE = "INVITE";
    public static final String IN_TYPE_FRIEND = "FRIEND";
    public static final String IN_NOTE_RAW = "line2-notify";
    /* Details */
    public static final int MSG_ID_INVALID = -1;
    /* Entry */
    public static final int MSG_MAX_LENGTH = 1024;
    /* Signup */
    public static final int EMAIL_SIGNUP = 1;
    public static final int SMS_SIGNUP = 2;

    /* Constants used in dealing with the SQLite Database and Shared Preference Database. */
    /* One trick with sqlite is that text columns must be surrounded by single quote in where clause. */
    public static final String Owner_DBID = "-1";
    public static final String ISO3166_Default = "US";
    public static final String DB_FriendsAll = "select * from friend";
    public static final String DB_FriendsWhere = "select * from friend where " + FriendDB.FRIEND_CONFIRM + " = '" + SUB_ZZZ + "' order by " + FriendDB.FRIEND_DISPLAY + " COLLATE NOCASE";
    public static final String DB_FriendByName = "select * from friend where " + FriendDB.FRIEND_UNIQUE + " = '" + SUB_ZZZ + "'";
    public static final String DB_Table_ID = BaseColumns._ID + " = " + SUB_ZZZ;
    public static final String DB_Table_ServerID = MessageDB.MESSAGE_SRVR_ID + " = " + SUB_ZZZ;
    public static final String DB_FriendExact = "select * from friend where " + DB_Table_ID;
    public static final String DB_PendingCnt = "select count(" + MessageDB.MESSAGE_ID + ") from message where " + MessageDB.MESSAGE_TIME + " >= '" + SUB_ZZZ + "'";
    public static final String DB_MessagesAll = "select * from message";
    public static final String DB_MessageByLocal = "select * from message where " + MessageDB.MESSAGE_ID + " = '" + SUB_ZZZ + "'";
    public static final String DB_MessageByServer = "select * from message where " + MessageDB.MESSAGE_SRVR_ID + " = '" + SUB_ZZZ + "'";

    /* Screen update rates */
    public static final int UPD_SCREEN_TQ = 3000;       // How often in msec to update the screen from data base to start
    public static final int UPD_SCREEN_TM = 60000;      // How often in msec to update the screen from data base normally

}
