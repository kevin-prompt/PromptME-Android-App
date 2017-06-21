package com.coolftc.prompt;

import android.content.Context;
import android.text.format.DateFormat;
import java.util.Calendar;
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
    public String targetTimename = "";  // Time = name.
    public int targetTimenameId = 0;    // Time name code.
    public String targetTimeadj = "";   // Time = adjustment.
    public int targetTimeadjId = 0;     // Time adjustment code.

    // Recurrence
    public int recureUnit = 0;          // If set to RECUR_INVALID then no recurrence.
    public int recurePeriod = 0;
    public int recureNumber = 0;
    public String recureEnd = "";

    // Message
    public String message = "";
    public boolean processed = false;   // Has the message been sent to the server.
    public int status = 0;              // If there are any issues, the code is stored here.

    public String idStr(){ return Long.toString(id); }


    /*
     *  This method will search a few of the fields to determine if the
     *  search "term" is contained in it.  It is a helper to aid in the
     *  determination if the message is of interest. Using toLowerCase
     *  make it case insensitive.
     */
    public boolean Found(String term){
        String lowterm = term.toLowerCase();
        return (term.length() == 0 || target.display.toLowerCase().contains(lowterm) || message.toLowerCase().contains(lowterm));
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



}
