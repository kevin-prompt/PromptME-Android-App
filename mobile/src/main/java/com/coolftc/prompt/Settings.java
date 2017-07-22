package com.coolftc.prompt;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import static com.coolftc.prompt.Constants.*;

/**
 *  The Settings provide a UI to allow for the maintenance of certain locally
    stored data in a system defined Preference table. This is not the only
    data stored in Preferences, see the Actor class.
    In Settings there are some personal items stored, but mostly it is
    general App data like how to format the date. Initially, 2 personal
    items, Display Name and Sleep Cycle, will are editable directly in
    the Settings screen. For the rest of the Settings, this class provides
    a simple wrapper to read them from the Preferences. It also alleviates
    any worries about initialization.

 *  This activity uses a fragment to do all the screen work, including the
    listener that can sync the personal items back to the Account and
    eventually to the server.

 *  To Add New Settings:
    1) Create a node in the settings.xml layout to define the UI.
        Use prf_Title/prf_TitleSum/prf_TitleDefault for resource string names.
    2) Create an id (PREF_*) constant below that matches the settings.xml.
    3) Create a "get" access method.  Add any special formatting as necessary.
    4) Add any code to dynamically adjust the summary (in SettingsBasic) if necessary.
 */
public class Settings extends AppCompatActivity {

    public static final String PREF_SETTINGS = "prompt.settings";
    public static final String PREF_SYSTEM = "prompt.system";
    public static final String PREF_DISPNAME = SP_REG_DISPLAY;
    public static final String PREF_SCYCLE = SP_REG_SCYCLE;
    public static final String PREF_SOUND = "1000";
    public static final String PREF_VIBRATEON = "1001";
    public static final boolean DEFAULT_VIBRATEON = false;
    public static final String PREF_USE24CLOCK = "1002";
    public static final boolean DEFAULT_USE24CLOCK = false;
    public static final String PREF_PICKSHORTDATEFMT = "1003";
    public static final String DEFAULT_PICKSHORTDATEFMT = DB_fmtDateShrtMiddle;
    public static final String PREF_SNOOZE = "1004";
    public static final String PREF_NAMESORT = "name.sortorder";
    public static final String PREF_SORT_ORDER = "prompt.sortorder";
    public static final int DEFAULT_SORT_ORDER = 0;

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
        String holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_DISPNAME, "");
        context = null; // we do not want these context objects to have to hang around forever in these static methods.
        return holdAnswer;
    }

    // Allow name update to be adjusted programmatically (to initialized it).
    // Note: This edit does not trigger the onSharedPreferenceChanged().
    public static void setDisplayName(Context context, String name) {
        SharedPreferences.Editor ali = PreferenceManager.getDefaultSharedPreferences(context).edit();
        ali.putString(PREF_DISPNAME, name);
        ali.apply();
        context = null;
    }

    // What sleep cycle is stored in preferences.
    public static int getSleepCycle(Context context) {
        String holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SCYCLE, "2");
        context = null;
        return Integer.parseInt(holdAnswer);
    }

    // How names should be sorted as stored in preferences.
    public static int getNameSortOrder(Context context) {
        int holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_NAMESORT, DEFAULT_SORT_ORDER);
        context = null;
        return holdAnswer;
    }

    // Shortcut to see if sorting by last name, which is not the default.
    public static boolean isSortByLastName(Context context){
        return getNameSortOrder(context) != DEFAULT_SORT_ORDER;
    }

    // Allow non-settings screen to update this value.
    public static void setNameSortOrder(Context context, int sort){
        SharedPreferences.Editor ali = PreferenceManager.getDefaultSharedPreferences(context).edit();
        ali.putInt(PREF_NAMESORT, sort);
        ali.apply();
        context = null;
    }

    // The ringtone to be used upon notifications.
    public static Uri getRingtone(Context context){
        String holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SOUND, "");
        context = null;
        return Uri.parse(holdAnswer);
    }

    // When true, have the device vibrate on incoming prompts.
    public static boolean getVibrateOn(Context context) {
        boolean holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_VIBRATEON, DEFAULT_VIBRATEON);
        context = null;
        return holdAnswer;
    }

    // What sleep cycle is stored in preferences.
    public static int getSnooze(Context context) {
        String holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SNOOZE, "60");
        context = null;
        return Integer.parseInt(holdAnswer);
    }

    // When true, display time in a 24 hour format.
    public static boolean getUse24Clock(Context context) {
        boolean holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USE24CLOCK, DEFAULT_USE24CLOCK);
        context = null;
        return holdAnswer;
    }

    // Select how the day/month/year is ordered in displaying dates.
    public static String getPickShortDateFmt(Context context) {
        String holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PICKSHORTDATEFMT, DEFAULT_PICKSHORTDATEFMT);
        context = null;
        return holdAnswer;
    }

    // Return which sort order is in effect for prompts.
    public static int getPromptSortOrder(Context context) {
        int holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_SORT_ORDER, DEFAULT_SORT_ORDER);
        context = null;
        return holdAnswer;
    }

    // Allow non-settings screen to update this value.
    public static void setPromptSortOrder(Context context, int sort){
        SharedPreferences.Editor ali = PreferenceManager.getDefaultSharedPreferences(context).edit();
        ali.putInt(PREF_SORT_ORDER, sort);
        ali.apply();
        context = null;
    }

    /*
     * There are a number of variations on how dates are displayed, based on what the user has specified.
     * This method brings some of those together in one place for ease of management and use around the App.
     * ENUMs generate a lot of overhead in Java, so we will stick to constant int as the date type.
     */
    public static String getDateDisplayFormat(Context context, int datetype) {

        String holdFmt;
        String fmt = getPickShortDateFmt(context);
        String tmt = getUse24Clock(context) ? DB_fmtDateTime24 : DB_fmtDateTime;
        context = null;

        switch (datetype){

            case DATE_FMT_SHORT:
                try {
                    holdFmt = DB_fmtDateShrtMiddle;
                    if (fmt.equalsIgnoreCase("big")) holdFmt = DB_fmtDateShrtBig;
                    if (fmt.equalsIgnoreCase("sml")) holdFmt = DB_fmtDateShrtLittle;
                    return holdFmt;
                } catch (Exception ex) {
                    ExpClass.LogEX(ex, "Settings.getDateDisplayFormat-DATE_FMT_SHORT");
                    return DB_fmtDateShrtMiddle;
                }

            case DATE_TIME_FMT_SHORT:
                try {
                    holdFmt = DB_fmtDateShrtMiddle;
                    if (fmt.equalsIgnoreCase("big")) holdFmt = DB_fmtDateShrtBig;
                    if (fmt.equalsIgnoreCase("sml")) holdFmt = DB_fmtDateShrtLittle;
                    holdFmt += " @ " + tmt;
                    return holdFmt;
                } catch (Exception ex) {
                    ExpClass.LogEX(ex, "Settings.getDateDisplayFormat-DATE_TIME_FMT_SHORT");
                    return DB_fmtDateShrtMiddle + " @ " + DB_fmtDateTime;
                }
            case DATE_TIME_FMT_REV:
                try{
                    holdFmt = DB_fmtDateShrtMiddle;
                    if (fmt.equalsIgnoreCase("big")) holdFmt = DB_fmtDateShrtBig;
                    if (fmt.equalsIgnoreCase("sml")) holdFmt = DB_fmtDateShrtLittle;
                    holdFmt = tmt + " on " + holdFmt;
                    return holdFmt;
                } catch (Exception ex) {
                    ExpClass.LogEX(ex, "Settings.getDateDisplayFormat-DATE_TIME_FMT_SHORT");
                    return DB_fmtDateShrtMiddle + " @ " + DB_fmtDateTime;
                }
            default:
                throw new IllegalArgumentException();

        }
    }
}
