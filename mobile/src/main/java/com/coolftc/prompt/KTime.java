package com.coolftc.prompt;

import android.text.format.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * If you already have a Calendar/Date object and just need formatting, use
   DateFormat or SimpleDateFormat directly.  Of course you may need to later
   use ParseToFormat() to translate between what DateFormat can do and what
   is desired.

 * The primary customization offered is for parsing some string formatted dates.
   The expected formats are:
    RFC 822 = "EEE, dd MMM yyyy HH:mm:ss Z";
    RFC 3339 = "yyyy-MM-ddTHH:mm:ssZ";
    RFC 3339f = "yyyy-MM-ddTHH:mm:ss.SSSZ"; (f = full)
    RFC 8601 = "yyyy-MM-dd HH:mm:ss.SSSZ";
   Others will work if they use the same general formatting characters. Note this is
   a POSITIONAL parse.  The date string has to match the template EXACTLY. For example,
   date strings without zero padding will not work.

 * Timezone is the tricky item.  When explicitly passed in with the Parse methods, it is
   just applied to the information. If you want to convert a time in one zone to what it
   is in another zone, use the ConvertTimezone method. All timezones explicitly passed in
   are expected to be a long timezone id, e.g. "America/Los_Angeles". In the date strings
   themselves timezones are "sort of" implied by the offsets to UTC, although this is not
   really the same thing.  That is, the "Z" really means UTC Offset, not timezone.
   To get the long timezone id use TimeZone.getDefault().getID().
   To avoid some timezone issues, just manage all internal dates as UTC and display them
   using the default timezone.  To get a Calendar object with the current UTC time:
   Calendar utcNow = KTime.ConvertTimezone(Calendar.getInstance(), KTime.UTC_TIMEZONE);

   -------------------------------------------------------------------------------
 * Output Formatting template for Android's DateFormat:
    AM_PM               'a' :: corresponds to the AM_PM field.
    DATE                'd' :: corresponds to the DATE field.
    DAY_OF_WEEK         'E' :: corresponds to the DAY_OF_WEEK field.
    DAY_OF_WEEK_IN_MONTH'F' :: corresponds to the DAY_OF_WEEK_IN_MONTH field.
    DAY_OF_YEAR         'D' :: corresponds to the DAY_OF_YEAR field.
    ERA                 'G' :: corresponds to the ERA field.
    HOUR0               'K' :: corresponding to the 24 HOUR field.
    HOUR1               'h' :: corresponding to the 12 HOUR field.
    HOUR_OF_DAY0        'H' :: corresponds to the 12 HOUR_OF_DAY field.
    HOUR_OF_DAY1        'k' :: corresponds to the 24 HOUR_OF_DAY field.
    *MILLISECOND         'S' :: corresponds to the MILLISECOND field.
    MINUTE              'm' :: corresponds to the MINUTE field.
    MONTH               'M' :: corresponds to the MONTH field.
    SECOND              's' :: corresponds to the SECOND field.
    TIMEZONE            'z' :: corresponds to the ZONE_OFFSET and DST_OFFSET fields.
    WEEK_OF_MONTH       'W' :: corresponds to the WEEK_OF_MONTH field.
    WEEK_OF_YEAR        'w' :: corresponds to the WEEK_OF_YEAR field.
    YEAR                'y' :: corresponds to the YEAR field.
    *Not Supported.
 *
 * Finally...
 * The old Java Calendar and Date classes have been supplanted in Java 8 and later by the
 * java.time (http://docs.oracle.com/javase/8/docs/api/java/time/package-summary.html)
 * framework. The new classes are inspired by the highly successful Joda-Time
 * (http://www.joda.org/joda-time/) framework, intended as its successor, similar in concept
 * but re-architected. Defined byJSR 310 (http://jcp.org/en/jsr/detail?id=310). Extended by
 * the ThreeTen-Extra (http://www.threeten.org/threeten-extra/) project. See the Tutorial
 * (http://docs.oracle.com/javase/tutorial/datetime/TOC.html).
 * Hard to say when/if this will ever be viable in Android.
 */
public class KTime {

    /*  Time Formats. Most of the output is done by the android.DateFormat, which is a slightly
     *  different template than the old ctime used by other formatters like SimpleDateFormat.
     *  Specifically, android.DateFormat does not support fractions of a second, and uses the kk
     *  instead of the hh/HH for hours.  Newer versions will support hh/HH, but not there yet.
     *  Suggest always using the Special "k" formats when using KTime to reduce confusion.
     */
    // Special "k" formats (used by android.Dateformat)
    public static final String KT_fmtDate822k = "EEE, dd MMM yyyy kk:mm:ss z";
    public static final String KT_fmtDate3339k = "yyyy-MM-ddTkk:mm:ssz";
    public static final String KT_fmtDate3339fk = "yyyy-MM-ddTkk:mm:ss.SSSz";
    public static final String KT_fmtDate3339fk_xS = "yyyy-MM-ddTkk:mm:ss.000z";// Android does not support fractions, so just force zeros
    public static final String KT_fmtDate8601k = "yyyy-MM-dd kk:mm:ss.SSSz";
    // Standard formats
    public static final String KT_fmtDate822 = "EEE, dd MMM yyyy HH:mm:ss Z";
    public static final String KT_fmtDate3339 = "yyyy-MM-ddTHH:mm:ssZ";
    public static final String KT_fmtDate3339f = "yyyy-MM-ddTHH:mm:ss.SSSZ";    // f = fractional seconds
    public static final String KT_fmtDate8601 = "yyyy-MM-dd HH:mm:ss.SSSZ";
    // Other formats
    public static final String KT_fmtDate3339s = "yyyy-MM-dd'T'HH:mm:ssZ";      // used by SimpleDateFormat
    public static final String KT_fmtDateEUAlert = "yyyy-MM-dd HH:mm:ss Z";     // format used by Alerts from Europe
    /*
     * The various display date format templates.  The time has two variations, 24 hour clock and 
     * AM/PM. The dates have three major variations based on cultural ordering of elements, and 
     * some additional play based on separators and how Months are represented.
     */
    public static final String KT_fmtDateShrtMiddle = "MM/dd/yyyy";             // on wikipedia they call these 3 date formats,
    public static final String KT_fmtDateShrtLittle = "dd/MM/yyyy";             // big/little/middle Endian,  
    public static final String KT_fmtDateShrtBig = "yyyy/MM/dd";                // they are most commonly used across countries,
    public static final String KT_fmtDateMonthMiddle = "MMM dd, yyyy";          // Middle = m/d/y used in USA
    public static final String KT_fmtDateMonthLittle = "dd MMM, yyyy";          // Little = d/m/y used in India, Russia, South America
    public static final String KT_fmtDateMonthBig = "yyyy, MMM dd";             // Big = y/m/d used in China
    public static final String KT_fmtLongMonthMiddle = "MMMM dd, yyyy";         // Full Month Name with MMMM
    public static final String KT_fmtLongMonthLittle = "dd MMMM, yyyy";         // 
    public static final String KT_fmtLongMonthBig = "yyyy, MMMM dd";            // 
    public static final String KT_fmtDateNoYearBigMid = "MMM dd";               // Without the year, big & middle the same
    public static final String KT_fmtDateNoYearLittle = "dd MMM";               // 
    public static final String KT_fmtDateTime = "h:mmaa";
    public static final String KT_fmtDateTime24 = "kk:mm  ";
    public static final String KT_fmtDateTimeZone = "h:mmaa z";
    public static final String KT_fmtDayOfWeek = "EEEE";
    public static final String UTC_TIMEZONE = "UTC"; // The UTC timezone
    /*
     * The time scale constants.  These can be used to indicate input or output preference.
     * Skipping Months, as they are tend to have a lot of variation compared to the others.
     */
    public static final int KT_MILLISECONDS = 0;
    public static final int KT_SECONDS = 1;
    public static final int KT_MINUTES = 2;
    public static final int KT_HOURS = 3;
    public static final int KT_DAYS = 4;
    public static final int KT_WEEKS = 5;
    public static final int KT_YEARS = 6;


    /* Some helpful constants for parsing the date */
    private static final int MISSING = -1;
    private static final String DAY_dd = "dd";      // Day of the week - numeric
    private static final String DAY_MM = "MM";      // Month of the year - numeric
    private static final String DAY_MMM = "MMM";    // Month of the year - string
    private static final String DAY_yyyy = "yyyy";  // Year - numeric
    private static final String DAY_HH = "HH";      // 24 hour of day - numeric
    private static final String DAY_kk = "kk";      // 24 hour of day - numeric
    private static final String DAY_hh = "hh";      // 12 hour of day - numeric
    private static final String DAY_aa = "aa";      // symbol for AM/PM - string
    private static final String DAY_mm = "mm";      // minute of hour - numeric
    private static final String DAY_ss = "ss";      // second of minute - numeric
    private static final String DAY_SSS = "SSS";    // milliseconds past last second - numeric
    private static final String DAY_Z = "Z";        // UTC Offset
    private static final String DAY_z = "z";        // UTC Offset
    private static final String MONTH_FIXED = "NUL JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC";

    /*
     *  Use the current time to produce a formatted date. If the timezone is supplied,
     *  it will first convert it to that local time before formatting.  For example,
     *  to create the current time in UTC, supply the UTC_TIMEZONE.
     */
    public static CharSequence ParseNow(String outFormat){ return ParseNow(outFormat, ""); }
    public static CharSequence ParseNow(String outFormat, String timezone){
        Calendar work = Calendar.getInstance();
        if (timezone.length() > 0) { work = ConvertTimezone(work, timezone); }
        outFormat = outFormat.replace("S","0"); // Milliseconds are not supported by DateFormat, so just improvise.
        return DateFormat.format(outFormat, work);
    }

    /*
     *  Calculates the absolute difference between 2 dates, supplied as formatted strings.
     *  The formatting strings are expected to be in the same timezone.
     *  Given the available output formats are not granular, truncation can occur.
     */
    public static long CalcDateDifference(String tndxOne, String tndxTwo, String inFormat, int outFormat) throws ExpParseToCalendar{
        Calendar time1 = ParseToCalendar(tndxOne, inFormat);
        Calendar time2 = ParseToCalendar(tndxTwo, inFormat);
        long holdDiff = Math.abs(time1.getTimeInMillis() - time2.getTimeInMillis());
        if (holdDiff == 0) return 0;
        switch (outFormat){
            case KT_MILLISECONDS:
                return holdDiff;
            case KT_SECONDS :
                return holdDiff / 1000;
            case KT_MINUTES :
                return holdDiff / (60 * 1000);
            case KT_HOURS:
                return holdDiff / (60 * 60 * 1000);
            case KT_DAYS:
                return holdDiff / (24 * 60 * 60 * 1000);
            case KT_WEEKS:
                return holdDiff / (7 * 24 * 60 * 60 * 1000);
            case KT_YEARS: // lint checker does not like putting too many numbers in denominator.
                return (holdDiff / 52) / (7 * 24 * 60 * 60 * 1000);
            default:
                return holdDiff;
        }
    }

    /*
     *  Is the date supplied in the past? Requires a date and the format of
     *  the data be passed in.  The input date must be UTC.
     */
    public static boolean IsPast(String inTime, String inFormat)  throws ExpParseToCalendar{
        String now = ParseNow(inFormat, UTC_TIMEZONE).toString();
        Calendar inTimeC = ParseToCalendar(inTime, inFormat);
        Calendar nowC = ParseToCalendar(now, inFormat);
        return 0 > inTimeC.getTimeInMillis() - nowC.getTimeInMillis();
    }

    /*
     *  Convert a time from one timezone to another.
     */
    public static Calendar ConvertTimezone(Calendar inTime, String timezone){
        Date holdDate = inTime.getTime();
        Calendar outTime = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        outTime.setTime(holdDate);
        return outTime;
    }

    /*
     *  This will take an Epoch (Unix) time and output it in the desired format.
     *  Epoch does assumes UTC as the time zone, but if a timezone is supplied
     *  the Calendar is converted to use that timezone.
     */
    public static CharSequence ParseEpochToFormat(long secs, String timezone, String outFormat){
        long msecs = secs * 1000;  // Calendar like msecs
        if(timezone.length() == 0) timezone = "UTC";
        Calendar newday = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        newday.setTimeInMillis(msecs);
        outFormat = outFormat.replace("S","0"); // Milliseconds are not supported by DateFormat, so just improvise.
        return DateFormat.format(outFormat, newday);
    }

    /*
     *  Shorthand method to allow both string dates as input and output fields.
     *  Note that the DateFormat uses slightly different characters to define
     *  output than the RFC formats. :(  **The use of "k" was fixed in Android 17.
     */
    public static CharSequence ParseToFormat(String inTime, String inFormat, String timezone, String outFormat) throws ExpParseToCalendar {
        Calendar work = ParseToCalendar(inTime, inFormat, timezone);
        outFormat = outFormat.replace("S","0"); // Milliseconds are not supported by DateFormat, so just improvise.
        return DateFormat.format(outFormat, work);
    }

    /*
     *  Take a string based time and its format description, then generate a
     *  Calendar object.  If a timezone is not supplied the local one is used.
     */
    public static Calendar ParseToCalendar(String inTime, String inFormat) throws ExpParseToCalendar { return ParseToCalendar(inTime,  inFormat, ""); }
    public static Calendar ParseToCalendar(String inTime, String inFormat, String timezone) throws ExpParseToCalendar{
        try {
            int ndx;
            boolean monthFound = false; // if the MMM is found, don't bother looking for the MM
            TimeZone tz = timezone.length() == 0 ? TimeZone.getDefault() : TimeZone.getTimeZone(timezone);
            Calendar work = Calendar.getInstance(tz);

            ndx=inFormat.indexOf(DAY_dd);
            if(ndx != MISSING){
                work.set(Calendar.DAY_OF_MONTH, Integer.parseInt(inTime.substring(ndx, ndx+2)));
            }

            ndx=inFormat.indexOf(DAY_MMM);
            if(ndx != MISSING){
                int holdMonth = MONTH_FIXED.indexOf(inTime.substring(ndx, ndx+3).toUpperCase()) / 4;
                if(holdMonth > 0){
                    work.set(Calendar.MONTH, holdMonth - 1); // Note Calendar month fields are zero indexed.
                    monthFound = true;
                }
            }

            ndx=inFormat.indexOf(DAY_MM);
            if(ndx != MISSING && !monthFound){
                work.set(Calendar.MONTH, Integer.parseInt(inTime.substring(ndx, ndx+2)) - 1);   // Note Calendar month fields are zero indexed.
            }

            ndx=inFormat.indexOf(DAY_yyyy);
            if(ndx != MISSING){
                work.set(Calendar.YEAR, Integer.parseInt(inTime.substring(ndx, ndx+4)));
            }

            ndx=inFormat.indexOf(DAY_HH);
            if(ndx != MISSING){
                work.set(Calendar.HOUR_OF_DAY, Integer.parseInt(inTime.substring(ndx, ndx+2)));
            }

            ndx=inFormat.indexOf(DAY_kk);
            if(ndx != MISSING){
                work.set(Calendar.HOUR_OF_DAY, Integer.parseInt(inTime.substring(ndx, ndx+2)));
            }

            ndx=inFormat.indexOf(DAY_hh);
            if(ndx != MISSING){
                work.set(Calendar.HOUR_OF_DAY, Integer.parseInt(inTime.substring(ndx, ndx+2)));
                ndx=inFormat.indexOf(DAY_aa);
                if(ndx != MISSING){
                    work.set(Calendar.AM_PM, inTime.substring(ndx, ndx+2).toLowerCase().equals("am")?Calendar.AM:Calendar.PM);
                }
            }

            ndx=inFormat.indexOf(DAY_mm);
            if(ndx != MISSING){
                work.set(Calendar.MINUTE, Integer.parseInt(inTime.substring(ndx, ndx+2)));
            }

            ndx=inFormat.indexOf(DAY_ss);
            if(ndx != MISSING){
                work.set(Calendar.SECOND, Integer.parseInt(inTime.substring(ndx, ndx+2)));
            }

            ndx=inFormat.indexOf(DAY_SSS);
            if(ndx != MISSING){
                work.set(Calendar.MILLISECOND, Integer.parseInt(inTime.substring(ndx, ndx+3)));
            }

            ndx=inFormat.indexOf(DAY_Z);
            if(ndx == MISSING) ndx=inFormat.indexOf(DAY_z);
            if(ndx != MISSING){
                int tmzoff = 0;
                int tmzsign = 1;
                String tmz = inTime.substring(ndx).trim(); // time zone is the final element in these formats
                if(!(tmz.equalsIgnoreCase("Z") || tmz.equalsIgnoreCase("GMT") || tmz.equalsIgnoreCase("UTC"))){
                    tmz = tmz.replace(":", "");
                    if(tmz.contains("-")) tmzsign = -1;
                    tmz = tmz.replace("-", ""); tmz = tmz.replace("+", "");
                    int tmzHours = Integer.parseInt(tmz.substring(0, 2));
                    int tmzMins = Integer.parseInt(tmz.substring(2));
                    tmzoff = (tmzHours * 60 * 60) + (tmzMins * 60);
                    tmzoff = tmzoff * 1000 * tmzsign;
                }
                work.set(Calendar.ZONE_OFFSET, tmzoff);
            }

            return work;

        }catch(Exception ex){
            throw new ExpParseToCalendar(ex.getMessage(), ex);
        }
    }

}
