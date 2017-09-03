package com.coolftc.prompt;

import android.content.Context;
import android.text.format.DateFormat;

import com.coolftc.prompt.utility.ExpParseToCalendar;
import com.coolftc.prompt.utility.KTime;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Comparator;
import java.util.TimeZone;
import static com.coolftc.prompt.utility.Constants.*;

/**
 *  Representation of the message, including who, when and what. This is useful for passing
 *  around both messages to be (sent) and messages that have been (history or notification).
 */
public class Reminder  implements Serializable {

    private static final long serialVersionUID = 6234225499461379739L; // Required for serialization.
    public Account from;                // The person sending the message.
    public Account target;              // The person getting the message.
    public long id = 0;                 // Prompt Key stored locally.
    public long serverId = 0;           // Prompt Key on the server.
    public long snoozeId = 0;           // Prompt Key on the server for a snoozed message (need to use this to cancel).
    public int type = 0;                // 1 = NOTE, 2 = INVITE

    // When a message is sent, the server provides the exact time that it will ultimately be sent.
    // It is stored in KTime.KT_fmtDate3339f for use with API.
    public String targetTime = "";
    private boolean mTartgetTimePast = false;    // used as a cache since once in the past, always...

    // Simplified time.
    public int targetTimeNameId = 0;        // Time name code.
    public int targetTimeAdjId = 0;         // Time adjustment code.
    public int sleepCycle = -1;             // The sleep cycle used for the Prompt.
    public String timezone = "";            // The time zone used for the Prompt.

    // Recurrence
    public int recurUnit = RECUR_INVALID;   // If set to RECUR_INVALID then no recurrence.
    public int recurPeriod = 0;
    public int recurNumber = 0;
    public String recurEnd = "";

    // Message
    public String message = "";
    public boolean processed = false;   // Has the message been sent to the server.
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
    public String SnoozeIdStr() { return Long.toString(snoozeId); }
    public boolean IsSelfie() { return target.unique.equalsIgnoreCase(from.unique); }
    public boolean IsRecurring() { return recurUnit != RECUR_INVALID; }
    public String GetPromptTime(Context context) { return GetFormattedTime(context,  PROMPT); }
    public String GetPromptTimeRev(Context context) { return GetFormattedTime(context, DETAIL); }
    public String GetRecurringTime(Context context) { return GetFormattedTime(context,  RECURRING); }
    public String GetCreatedTime(Context context) { return GetFormattedTime(context,  CREATED); }
    //  The actual value is not of particular importance, just needs a String where false is zero length.
    public String IsPromptPast() { return IsPast()? "past" : ""; }

    // The simple time settings as displayed are not always exact match for what is used by the
    // server.  To keep things manageable we use a couple of extra methods to access the data.
    public int GetTargetTimeNameIdDsply(){ return targetTimeNameId > 0 ? targetTimeNameId : 0; }
    public void SetTargetTimeNameIdDsply(int value){ targetTimeNameId = value; if(value == 0) targetTime = "";}
    public int GetTargetTimeAdjIdDsply(){ return targetTimeAdjId > 0 ? targetTimeAdjId - 1 : 0; }
    public void SetTargetTimeAdjIdDsply(int value){ targetTimeAdjId = value + 1; }
    public boolean IsExactTime(){ return targetTimeNameId == 0 && targetTime.length() > 0; }

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
     *  This provides a human readable timestamp based on date / time formatting
     *  selected in Settings.  The API likes to provide dates in UTC, while the
     *  app will generally be generating dates in the local time zone.  As long
     *  as it is only 2 options, we can treat a time with a zero offset as UTC
     *  and convert those to display in the local time zone.  If the time has a
     *  non-zero offset, we will just format it as is.
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
        if (holdTimeStamp.length() == 0) return context.getResources().getString(R.string.unknown);
        Calendar delivery;
        String dateTimeFmt = Settings.getDateDisplayFormat(context, holdFormat);
        boolean isUTC = KTime.ParseOffset(holdTimeStamp, KTime.KT_fmtDate3339fk, KTime.KT_SECONDS) == 0;
        try {
            if(isUTC) {
                delivery = KTime.ConvertTimezone(KTime.ParseToCalendar(holdTimeStamp, KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE), TimeZone.getDefault().getID());
            } else {
                delivery = KTime.ParseToCalendar(holdTimeStamp, KTime.KT_fmtDate3339fk);
            }
            return DateFormat.format(dateTimeFmt, delivery).toString();
        } catch (ExpParseToCalendar expParseToCalendar) {
            return context.getResources().getString(R.string.unknown);
        }
    }

    /*
     *  Return the Prompt time as an epoch number.
     */
    public long GetPromptMSec() {
        Calendar ali;
        boolean isUTC = KTime.ParseOffset(targetTime, KTime.KT_fmtDate3339fk, KTime.KT_SECONDS) == 0;
        try {
            if(isUTC) {
                ali = KTime.ConvertTimezone(KTime.ParseToCalendar(targetTime, KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE), TimeZone.getDefault().getID());
            } else {
                ali = KTime.ParseToCalendar(targetTime, KTime.KT_fmtDate3339fk);
            }
            return ali.getTimeInMillis();
        } catch (ExpParseToCalendar expParseToCalendar) {
            return Calendar.getInstance().getTimeInMillis();
        }
    }

    /*
     *  Helper to indicate if the prompt time is past or future.
     *  Generally the assumption will be the prompt is not in the past.
     */
    public boolean IsPast() {
        if (!processed) return false;
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

    /*
     *  Calculate if the recurring end date is effectively "forever".
     */
    private boolean IsForever(){ return IsForever(recurEnd); }

    public static boolean IsForever(String when){
        try {
            return  (when.length() != 0) &&
                    (KTime.CalcDateDifference(when, KTime.ParseNow(KTime.KT_fmtDate3339fk).toString(), KTime.KT_fmtDate3339fk, KTime.KT_YEARS) > FOREVER_LESS);
        } catch (ExpParseToCalendar ex) {
            return false;
        }
    }

    /*
     *  Generate the natural language terms for the recurring period.
     */
    public String GetRecurringVerb(Context context) {
        int holdResource = 0;
        String weekdays = "";

        // Day
        if(recurUnit == UNIT_TYPE_DAY){
            // Daily
            if(recurPeriod == 1){
                // Ending
                if(recurNumber > 0){
                    holdResource = R.string.recur_daily_nbr;
                }else{
                    if(IsForever()){
                        holdResource = R.string.recur_daily_end;
                    } else {
                        holdResource = R.string.recur_daily_day;
                    }
                }
            } else { // More than 1 day
                // Ending
                if(recurNumber > 0){
                    holdResource = R.string.recur_day_nbr;
                }else{
                    if(IsForever()){
                        holdResource = R.string.recur_day_end;
                    } else {
                        holdResource = R.string.recur_day_day;
                    }
                }
            }
        }

        // Week
        if(recurUnit == UNIT_TYPE_WEEKDAY){
            if(recurNumber > 0){
                holdResource = R.string.recur_wek_nbr;
            }else{
                if(IsForever()){
                    holdResource = R.string.recur_wek_end;
                } else {
                    holdResource = R.string.recur_wek_day;
                }
            }

            // Build out the weekday string, shorted if too many days choosen.
            if((recurPeriod & SUN_FLAG) == SUN_FLAG) { weekdays += context.getResources().getText(R.string.sunday_abbr) + ", "; }
            if((recurPeriod & MON_FLAG) == MON_FLAG) { weekdays += context.getResources().getText(R.string.monday_abbr) + ", "; }
            if((recurPeriod & TUE_FLAG) == TUE_FLAG) { weekdays += context.getResources().getText(R.string.tuesday_abbr) + ", "; }
            if((recurPeriod & WED_FLAG) == WED_FLAG) { weekdays += context.getResources().getText(R.string.wednsday_abbr) + ", "; }
            if((recurPeriod & THU_FLAG) == THU_FLAG) { weekdays += context.getResources().getText(R.string.thursday_abbr) + ", "; }
            if((recurPeriod & FRI_FLAG) == FRI_FLAG) { weekdays += context.getResources().getText(R.string.friday_abbr) + ", "; }
            if((recurPeriod & SAT_FLAG) == SAT_FLAG) { weekdays += context.getResources().getText(R.string.saturday_abbr) + ", "; }
            weekdays = weekdays.substring(0, weekdays.length()-2); // trim off the trailing comma.
            if(weekdays.length() > 13){
                weekdays = weekdays.substring(0, 13) + "...";
            }
        }

        // Month
        if(recurUnit == UNIT_TYPE_MONTH){
            if(recurPeriod == 1){
                if(recurNumber > 0){
                    holdResource = R.string.recur_monthly_nbr;
                }else{
                    if(IsForever()){
                        holdResource = R.string.recur_monthly_end;
                    } else {
                        holdResource = R.string.recur_monthly_day;
                    }
                }
            } else {
                if (recurNumber > 0) {
                    holdResource = R.string.recur_mon_nbr;
                } else {
                    if (IsForever()) {
                        holdResource = R.string.recur_mon_end;
                    } else {
                        holdResource = R.string.recur_mon_day;
                    }
                }
            }
        }

        if(holdResource == 0) return "";
        String template = context.getResources().getText(holdResource).toString();
        String formattedDate = IsForever() ? context.getResources().getString(R.string.forever) : GetRecurringTime(context);
        return String.format(template, recurPeriod, recurNumber, formattedDate, weekdays);
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
            if (!r1.processed) return -1;
            if (!r2.processed) return 1;
            if (r1.targetTime != null && r2.targetTime != null) {
                //descending order
                return r2.targetTime.compareTo(r1.targetTime);
            }
            return 0;  // indeterminate
        }
    };
}
