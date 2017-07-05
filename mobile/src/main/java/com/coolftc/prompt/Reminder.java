package com.coolftc.prompt;

import android.content.Context;
import android.text.format.DateFormat;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Comparator;
import java.util.TimeZone;
import static com.coolftc.prompt.Constants.*;

/**
 *  Representation of the message, including who, when and what. This is useful for passing
 *  around both messages to be (sent) and messages that have been (in history).
 */
public class Reminder  implements Serializable {

    private static final long serialVersionUID = 6234225499461379739L; // Required for serialization.
    public Account from;                // The person sending the message.
    public Account target;              // The person getting the message.
    public long id = 0;                 // Prompt Key stored locally.
    public long serverId = 0;           // Prompt Key on the server.

    // When a message is sent, the server provides the exact time that it will ultimately be sent.
    // It is stored in KTime.KT_fmtDate3339f for use with API.
    public String targetTime = "";
    private boolean mTartgetTimePast = false;    // used as a cache since once in the past, always...

    // Simplified time.
    public int targetTimeNameId = 0;        // Time name code.
    public int targetTimeAdjId = 0;         // Time adjustment code.

    // Recurrence
    public int recurUnit = RECUR_INVALID;   // If set to RECUR_INVALID then no recurrence.
    public int recurPeriod = 0;
    public int recurNumber = 0;
    public String recurEnd = "";

    // Message
    public String message = "";
    public boolean isProcessed = false; // Has the message been sent to the server.
    public int status = 0;              // If there are any issues, the code is stored here.
    public String created = "";         // The timestamp of when the message was created.

    // Use constants instead of enums in java to save on resources.
    private static final int PROMPT = 1;
    private static final int RECURRING = 2;
    private static final int CREATED = 3;
    private static final int DETAIL = 4;

    // Some simple formatting methods.
    public String IdStr() { return Long.toString(id); }
    public String ServerIdStr() { return Long.toString(serverId); }
    public boolean IsSelfie() { return target.unique.equalsIgnoreCase(from.unique); }
    public boolean IsRecurring() { return recurUnit != RECUR_INVALID; }
    public String GetPromptTime(Context context) { return GetFormattedTime(context,  PROMPT); }
    public String GetPromptTimeRev(Context context) { return GetFormattedTime(context, DETAIL); }
    public String GetRecurringTime(Context context) { return GetFormattedTime(context,  RECURRING); }
    public String GetCreatedTime(Context context) { return GetFormattedTime(context,  CREATED); }
    //  The actual value is not of particular importance, just needs a String where false is zero length.
    public String IsPromptPast() { return IsPast()? "past" : ""; }

    /*
     *  This method will search a few of the fields to determine if the
     *  search "term" is contained in it.  It is a helper to aid in the
     *  determination if the message is of interest. Using toLowerCase
     *  make it case insensitive.
     */
    public boolean Found(String term) {
        String lowTerm = term.toLowerCase();
        return (term.length() == 0 || target.display.toLowerCase().contains(lowTerm) || message.toLowerCase().contains(lowTerm));
    }

    /*
     *  This provides a human readable timestamp based on date / time
     *  formatting selected in Settings.  It assumes caller wants to
     *  see the date in the local timezone.
     */
    private String GetFormattedTime(Context context, int ts){
        String holdTimeStamp;
        int holdFormat;
        switch (ts){
            case PROMPT:
                holdTimeStamp = targetTime;
                holdFormat = DATE_TIME_FMT_SHORT;
                break;
            case RECURRING:
                holdTimeStamp = recurEnd;
                holdFormat = DATE_TIME_FMT_SHORT;
                break;
            case CREATED:
                holdTimeStamp = created;
                holdFormat = DATE_TIME_FMT_SHORT;
                break;
            case DETAIL:
                holdTimeStamp = targetTime;
                holdFormat = DATE_TIME_FMT_REV;
                break;
            default:
                return context.getResources().getString(R.string.unknown);
        }
        Calendar delivery;
        String dateTimeFmt = Settings.getDateDisplayFormat(context, holdFormat);
        try {
            delivery = KTime.ConvertTimezone(KTime.ParseToCalendar(holdTimeStamp, KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE), TimeZone.getDefault().getID());
            return DateFormat.format(dateTimeFmt, delivery).toString();
        } catch (ExpParseToCalendar expParseToCalendar) {
            return context.getResources().getString(R.string.unknown);
        }
    }

    /*
     *  Helper to indicate if the prompt is past or future.
     *  Generally the assumption will be the prompt is not in the past.
     */
    public boolean IsPast() {
        if (!isProcessed) return false;
        if (mTartgetTimePast) return true;
        try {
            if (KTime.IsPast(targetTime, KTime.KT_fmtDate3339fk)) {
                mTartgetTimePast = true;
                return true;
            } else {
                return false;
            }
        } catch (ExpParseToCalendar expParseToCalendar) {
            return false;
        }

    }

    // Helps sort by prompt create Date
    public static Comparator<Reminder> ByCreateDate = new Comparator<Reminder>() {
        public int compare(Reminder r1, Reminder r2) {
            if (r1 != null && r2 != null) {
                //descending order
                return r2.created.compareTo(r1.created);
            }
            return 0;  // indeterminate
        }
    };

    /*
     *  Helps sort by prompt delivery Date
     *  If the the prompt is in the process of being created, it will not yet
     *  have a date, but we want to sort it to the top so the person can see
     *  what they entered is getting worked on.
     *  The comparison returns a 1 if r2 is newer (larger time value), and -1
     *  if r2 is older (smaller time value).  So to bubble the not yet processed
     *  reminder to the top, we just simulate it being larger (far in the future).
     */
    public static Comparator<Reminder> ByDeliveryDate = new Comparator<Reminder>() {
        public int compare(Reminder r1, Reminder r2) {
            if (r1 == null || r2 == null) return 0;
            if (!r1.isProcessed) return -1;
            if (!r2.isProcessed) return 1;
            if (r1.targetTime != null && r2.targetTime != null) {
                //descending order
                return r2.targetTime.compareTo(r1.targetTime);
            }
            return 0;  // indeterminate
        }
    };
}
