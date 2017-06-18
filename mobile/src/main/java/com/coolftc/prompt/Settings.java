package com.coolftc.prompt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import static com.coolftc.prompt.Constants.*;

/**
 *  The Settings provide a UI to allow for the maintenance of certain locally
 *  stored data.  This does not represent all the data stored in Preferences.
 *  Since it is an efficient storage area for small amounts of data, the
 *  personal data collected is also keep in a Preferences store for ease of
 *  access by using the Account class.  Just not the default Preference
 *  store like Settings uses.
 *  Initially, 2 of the personal items, Display Name and Sleep Cycle, will
 *  be editable directly in the Settings screen.  Just seemed overkill to
 *  create a new screen to support those 2 things.
 *  For the rest of the Settings, this class provides a simple wrapper to
 *  read them from the Preferences.  That also alleviates any worries about
 *  initialization.
 *  This activity uses a fragment to do all the screen work, including the
 *  listener that can sync the 2 personal items back to the Account and
 *  eventually to the server.
 */
public class Settings extends AppCompatActivity {

    public static final String PREF_SETTINGS = "prompt.settings";
    public static final String PREF_VIBRATEON = "1001";
    public static final boolean DEFAULT_VIBRATEON = false;
    public static final String PREF_USE24CLOCK = "1002";
    private static final boolean DEFAULT_USE24CLOCK = false;
    public static final String PREF_PICKSHORTDATEFMT = "1003";
    private static final String DEFAULT_PICKSHORTDATEFMT = DB_fmtDateShrtMiddle;
    private static final String PREF_DISPNAME = "prompt.display";
    private static final String PREF_SCYCLE = "prompt.sleepcycle";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsBasic())
                .commit();
    }

    // What name is stored in preferences.
    public static String getDisplayName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_DISPNAME, "");
    }

    // Allow name update to be adjusted programmatically (to initialized it).
    // Note: This edit does not trigger the onSharedPreferenceChanged().
    public static void setDisplayName(Context context, String name) {
        SharedPreferences.Editor ali = PreferenceManager.getDefaultSharedPreferences(context).edit();
        ali.putString(PREF_DISPNAME, name);
        ali.apply();
    }

    // What sleep cycle is stored in preferences.
    public static int getSleepCycle(Context context) {
        String holdSleep = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SCYCLE, "2");
        return Integer.parseInt(holdSleep);
    }

    // When true, have the device vibrate on incoming prompts.
    public static boolean getVibrateOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_VIBRATEON, DEFAULT_VIBRATEON);
    }

    // When true, display time in a 24 hour format.
    public static boolean getUse24Clock(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USE24CLOCK, DEFAULT_USE24CLOCK);
    }

    // Select how the day/month/year is ordered in displaying dates.
    public static String getPickShortDateFmt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PICKSHORTDATEFMT, DEFAULT_PICKSHORTDATEFMT);
    }

    /*
     * There are a number of variations on how dates are displayed, based on what the user has specified.
     * This method brings some of those together in one place for ease of management and use around the App.
     * ENUMs generate a lot of overhead in Java, so we will stick to constant int as the date type.
     */
    public static String getDateDisplayFormat(Context context, int datetype) {

        String holdFmt;
        String fmt = getPickShortDateFmt(context);
        //Resources res = context.getResources();

        switch (datetype){

            case DATE_FMT_SHORT:
                holdFmt = DB_fmtDateShrtMiddle;
                if(fmt.equalsIgnoreCase("big")) holdFmt = DB_fmtDateShrtBig;
                if(fmt.equalsIgnoreCase("sml")) holdFmt = DB_fmtDateShrtLittle;
                return holdFmt;

//            case DATE_FMT_OBSERVED:
//                try {
//                    holdFmt = "'" + res.getString(R.string.lblObserved) + " 'E ";
//                    if(fmt.equalsIgnoreCase("big")) holdFmt += DB_fmtDateMonthBig;
//                    if(fmt.equalsIgnoreCase("mid")) holdFmt += DB_fmtDateMonthMiddle;
//                    if(fmt.equalsIgnoreCase("sml")) holdFmt += DB_fmtDateMonthLittle;
//                    holdFmt += "' " + res.getString(R.string.lblAt) + " '";
//                    holdFmt += getUse24Clock(context) ? DB_fmtDateTime24 : DB_fmtDateTime;
//                    return holdFmt;
//                } catch (Exception ex) { ExpClass.LogEX(ex, "Settings.chgStateSlate-DATE_FMT_OBSERVED"); return "'Observed 'E MMM dd, yyyy ' at ' h:mmaa"; }
//
//            case DATE_FMT_ALERT_CURR:
//                try {
//                    holdFmt = "' -:- " + res.getString(R.string.lblUntil) + " '";
//                    holdFmt += getUse24Clock(context) ? DB_fmtDateTime24 : DB_fmtDateTime;
//                    if(fmt.equalsIgnoreCase("sml"))
//                        holdFmt += " " + DB_fmtDateNoYearLittle;
//                    else
//                        holdFmt += " " + DB_fmtDateNoYearBigMid;
//                    return holdFmt;
//                } catch (Exception ex) { ExpClass.LogEX(ex, "Settings.chgStateSlate-DATE_FMT_ALERT_CURR"); return "' -:- Until 'h:mmaa MMM dd"; }
//
//            case DATE_FMT_ALERT_EXP:
//                try {
//                    holdFmt = DB_fmtLongMonthMiddle;
//                    if(fmt.equalsIgnoreCase("big")) holdFmt = DB_fmtLongMonthBig;
//                    if(fmt.equalsIgnoreCase("sml")) holdFmt = DB_fmtLongMonthLittle;
//                    holdFmt += " @ " + (getUse24Clock(context) ? DB_fmtDateTime24 : DB_fmtDateTime);
//                    return holdFmt;
//                } catch (Exception ex) { ExpClass.LogEX(ex, "Settings.chgStateSlate-DATE_FMT_ALERT_EXP"); return "MMMM dd, yyyy @ h:mmaa"; }

            default:
                throw new IllegalArgumentException();

        }
    }



}