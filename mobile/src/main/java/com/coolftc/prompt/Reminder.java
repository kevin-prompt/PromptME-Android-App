package com.coolftc.prompt;

/**
 *  Representation of the message, including who, when and what.
 */
public class Reminder {

    public Account target;              // The person getting the message.
                                        // The Account holds info like: name/unique/picture/timezone

    public String targetTime = "";      // If an Exact Time is specified, it is stored in KTime.KT_fmtDate3339f for use with API.
    public String targetTimename = "";  // Simplified Time = name.
    public int targetTimenameId = 0;    // Simplified Time name code.
    public String targetTimeadj = "";   // Simplified Time = adjustment.
    public int targetTimeadjId = 0;     // Simplified Time adjustment code.

    // Recurrence
    public int recureUnit = 0;          // If set to RECUR_INVALID then no recurrence.
    public int recurePeriod = 0;
    public int recureNumber = 0;
    public String recureEnd = "";

    // Message
    public String message = "";         // What to be notified (reminded) about.

    // Key
    public long id = 0;
    public boolean processed = false;   // Has the message been sent to the server.
    public int status = 0;             // If there are any issues, the code is stored here.


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



}
