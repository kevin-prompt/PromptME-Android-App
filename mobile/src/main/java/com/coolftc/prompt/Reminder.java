package com.coolftc.prompt;

import android.content.Context;
import android.text.format.DateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.TimeZone;

import static com.coolftc.prompt.Constants.DATE_TIME_FMT_SHORT;

/**
 *  Representation of the message, including who, when and what. This is useful for passing
 *  around both messages to be (sent) and messages that have been (in history).
 */
public class Reminder {

    public Account target;              // The person getting the message.
    public long id = 0;                 // Prompt Key

    // When a message is sent, the server provides the exact time that it will ultimately be sent.
    // It is stored in KTime.KT_fmtDate3339f for use with API.
    public String targetTime = "";

    // Simplified time.
    public String targetTimeName = "";  // Time = name.
    public int targetTimeNameId = 0;    // Time name code.
    public String targetTimeAdj = "";   // Time = adjustment.
    public int targetTimeAdjId = 0;     // Time adjustment code.

    // Recurrence
    public int recurUnit = 0;           // If set to RECUR_INVALID then no recurrence.
    public int recurPeriod = 0;
    public int recurNumber = 0;
    public String recurEnd = "";

    // Message
    public String message = "";
    public boolean isProcessed = false; // Has the message been sent to the server.
    public int status = 0;              // If there are any issues, the code is stored here.
    public String created = "";         // The timestamp of when the message was created.

    public String idStr(){ return Long.toString(id); }


    /*
     *  This method will search a few of the fields to determine if the
     *  search "term" is contained in it.  It is a helper to aid in the
     *  determination if the message is of interest. Using toLowerCase
     *  make it case insensitive.
     */
    public boolean Found(String term){
        String lowTerm = term.toLowerCase();
        return (term.length() == 0 || target.display.toLowerCase().contains(lowTerm) || message.toLowerCase().contains(lowTerm));
    }

    /*
     *  This provides a human readable timestamp based on date / time
     *  formatting selected in Settings.  It assumes caller wants to
     *  see the date in the local timezone.
     */
    public String GetPromptTime(Context context){
        Calendar delivery = null;
        String dateTimeFmt = Settings.getDateDisplayFormat(context, DATE_TIME_FMT_SHORT);
        try {
            delivery = KTime.ConvertTimezone(KTime.ParseToCalendar(targetTime, KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE), TimeZone.getDefault().getID());
            return DateFormat.format(dateTimeFmt, delivery).toString();
        } catch (ExpParseToCalendar expParseToCalendar) {
            return context.getResources().getString(R.string.unknown);
        }
    }

    /*
     *  This is used to determine if the prompt time has passed.
     */
    public String IsPromptPast(){
        if(!isProcessed) return "";
        try {
            if(KTime.IsPast(targetTime, KTime.KT_fmtDate3339fk))
                return "past";
            else
                return "";
        } catch (ExpParseToCalendar expParseToCalendar) {
            return "";
        }
    }

    // Helps sort by prompt create Date
    public static Comparator<Reminder> ByCreateDate = new Comparator<Reminder>() {
        public int compare(Reminder r1, Reminder r2) {
            if(r1 != null && r2 != null) {
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
            if(r1 == null || r2 == null) return 0;
            if(!r1.isProcessed) return -1;
            if(!r2.isProcessed) return 1;
            if(r1.targetTime != null && r2.targetTime != null) {
                //descending order
                return r2.targetTime.compareTo(r1.targetTime);
            }
            return 0;  // indeterminate
        }
    };
}
