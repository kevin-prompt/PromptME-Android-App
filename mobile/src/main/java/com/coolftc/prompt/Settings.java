package com.coolftc.prompt;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.coolftc.prompt.utility.ExpClass;
import java.util.Locale;
import static com.coolftc.prompt.utility.Constants.*;

/**
 *  The Settings provide a UI to allow for the maintenance of certain locally
    stored data in a system defined Preference table.
    Settings usually store general App configuration data, e.g. how to format the
    date, but also can give some access to personalization (if there is not
    a dedicated screen for that). Here 2 personal items, Display Name and Sleep
    Cycle, are editable directly in Settings.
    This class provides a simple wrapper to read data from the Preferences and
    optionally format it in some desired way. It also alleviates worries about
    initialization.

 *  This activity uses a fragment to do all the screen work, including the
    listener that can sync the personal items back to the Account and
    eventually to the server.  Some items are only shown if certain conditions
    are met, e.g. vibration is not shown if the device does not support
    vibration.

 *  To Add New Settings:
    1) Create a node in the settings.xml layout to define the UI.
        Use prf_Title/prf_TitleSum/prf_TitleDefault for resource string names.
    2) Create an id (PREF_*) constant below that matches the key in settings.xml.
    3) Create a "get" access method.  Add any special formatting as necessary.
    4) Add any code to dynamically adjust the summary (in SettingsBasic) if necessary.

 * Dependencies
    Requires an res/xml/settings.xml file with correct PreferenceScreen layout.
    implementation 'androidx.preference:preference-ktx:1.1.1'

 */
public class Settings extends AppCompatActivity {

    public static final String PREF_SETTINGS = "prompt.settings";
    public static final String PREF_SYSTEM = "prompt.system";
    public static final String PREF_DISPNAME = SP_REG_DISPLAY;
    public static final String PREF_SCYCLE = SP_REG_SCYCLE;
    public static final String PREF_SOUND = "setting.sound";
    public static final String PREF_VIBRATEON = "settings.vibrate";
    public static final boolean DEFAULT_VIBRATEON = false;
    public static final String PREF_USE24CLOCK = "settings.clock24";
    public static final boolean DEFAULT_USE24CLOCK = false;
    public static final String PREF_PICKSHORTDATEFMT = "settings.shortdate";
    public static final String DEFAULT_PICKSHORTDATEFMT = DB_fmtDateShrtMiddle;
    public static final String PREF_SNOOZE = "settings.snooze";
    public static final String PREF_CONTACTS = "settings.contacts";
    public static final boolean DEFAULT_CONTACTS = true;
    public static final String PREF_SOUND_AVAILABLE = "settings.is.sound";
    public static final int KY_SOUND_PICKER = 1200;
    public static final boolean DEFAULT_SOUND_AVAILABLE = false;
    public static final String PREF_VERIFICATION = "settings.verify";
    public static final String PREF_NAMESORT = "name.sortorder";
    public static final String PREF_SORT_ORDER = "prompt.sortorder";
    public static final int DEFAULT_SORT_ORDER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getSupportFragmentManager().beginTransaction()
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
        String holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SCYCLE, "2");
        return holdAnswer != null ? Integer.parseInt(holdAnswer) : 0;
    }

    // How names should be sorted as stored in preferences.
    public static int getNameSortOrder(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_NAMESORT, DEFAULT_SORT_ORDER);
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
    }

    // The ringtone to be used for notifications, but only matters below Android v8.
    public static Uri getRingtone(Context context){
        String defaultRingtone = String.format(Locale.getDefault(), "android.resource://%s/%d",context.getPackageName(),R.raw.promptbeep);
        String holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SOUND, defaultRingtone);
        return Uri.parse(holdAnswer);
    }

    // When true, have the device vibrate on incoming prompts.
    public static boolean getVibrateOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_VIBRATEON, DEFAULT_VIBRATEON);
    }

    // When true, continue to ask to use the Contacts.
    public static boolean getContactsOk(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_CONTACTS, DEFAULT_CONTACTS);
    }

    // What snooze lenght is stored in preferences.
    public static int getSnooze(Context context) {
        String holdAnswer = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_SNOOZE, "60");
        return holdAnswer != null ? Integer.parseInt(holdAnswer) : 60;
    }

    // When true, display time in a 24 hour format.
    public static boolean getUse24Clock(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_USE24CLOCK, DEFAULT_USE24CLOCK);
    }

    // Select how the day/month/year is ordered in displaying dates.
    public static String getPickShortDateFmt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_PICKSHORTDATEFMT, DEFAULT_PICKSHORTDATEFMT);
    }

    // Return which sort order is in effect for prompts.
    public static int getPromptSortOrder(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(PREF_SORT_ORDER, DEFAULT_SORT_ORDER);
    }

    // Allow non-settings screen to update this value.
    public static void setPromptSortOrder(Context context, int sort){
        SharedPreferences.Editor ali = PreferenceManager.getDefaultSharedPreferences(context).edit();
        ali.putInt(PREF_SORT_ORDER, sort);
        ali.apply();
    }

    // Check if the default notification sound has been copied locally to the device.
    public static boolean isSoundCopied(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SOUND_AVAILABLE, DEFAULT_SOUND_AVAILABLE);
    }

    // Called after a successful copy of the notification sound, so we do not bother with it again.
    public static void setSoundCopied(Context context, boolean done){
        SharedPreferences.Editor ali = PreferenceManager.getDefaultSharedPreferences(context).edit();
        ali.putBoolean(PREF_SOUND_AVAILABLE, done);
        ali.apply();
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

        switch (datetype){

            case DATE_FMT_SHORT:
                try {
                    holdFmt = DB_fmtDateShrtMiddle;
                    if (fmt.equalsIgnoreCase("big")) holdFmt = DB_fmtDateShrtBig;
                    if (fmt.equalsIgnoreCase("sml")) holdFmt = DB_fmtDateShrtLittle;
                    return holdFmt;
                } catch (Exception ex) {
                    ExpClass.Companion.logEX(ex, "Settings.getDateDisplayFormat-DATE_FMT_SHORT");
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
                    ExpClass.Companion.logEX(ex, "Settings.getDateDisplayFormat-DATE_TIME_FMT_SHORT");
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
                    ExpClass.Companion.logEX(ex, "Settings.getDateDisplayFormat-DATE_TIME_FMT_SHORT");
                    return DB_fmtDateShrtMiddle + " @ " + DB_fmtDateTime;
                }
            default:
                throw new IllegalArgumentException();

        }
    }
}
