package com.coolftc.prompt;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;


import static com.coolftc.prompt.Constants.*;
import static com.coolftc.prompt.Settings.PREF_DISPNAME;
import static com.coolftc.prompt.Settings.PREF_PICKSHORTDATEFMT;
import static com.coolftc.prompt.Settings.PREF_SCYCLE;
import static com.coolftc.prompt.Settings.PREF_SETTINGS;
import static com.coolftc.prompt.Settings.PREF_SNOOZE;
import static com.coolftc.prompt.Settings.PREF_SOUND;
import static com.coolftc.prompt.Settings.PREF_SYSTEM;
import static com.coolftc.prompt.Settings.PREF_VIBRATEON;

/**
 *  The very basic fragment to display settings.  See the Settings
 *  activity for all the important stuff.  Note that you need to
 *  place the listener here in the fragment to pick up changes.
 */
public class SettingsBasic extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences mPrefDB;
    private PreferenceCategory mCategory;
    private Preference mActorName;
    private Preference mSleepCycle;
    private RingtonePreference mSound;
    private Preference mShortDate;
    private Preference mSnooze;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        // Get local access to the default shared preference DB
        PreferenceManager pm = this.getPreferenceManager();
        mPrefDB = pm.getSharedPreferences();
        mPrefDB.registerOnSharedPreferenceChangeListener(this);

        // Get all the preference rows that will need dynamic updating
        mActorName = findPreference(PREF_DISPNAME);
        mSleepCycle = findPreference(PREF_SCYCLE);
        mSound = (RingtonePreference)findPreference(PREF_SOUND);
        mShortDate = findPreference(PREF_PICKSHORTDATEFMT);
        mSnooze = findPreference(PREF_SNOOZE);

        // Do not bother showing the selection if the device does not support it.
        // Note: To modify the structure of the page, need to use the category.
        final Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (!v.hasVibrator()) {
            PreferenceCategory cat = (PreferenceCategory) findPreference(PREF_SYSTEM);
            Preference mVibrate = findPreference(PREF_VIBRATEON);
            cat.removePreference(mVibrate);
        }

        // Apply any initialization (mostly summaries)
        mActorName.setSummary(mPrefDB.getString(PREF_DISPNAME, ""));
        mSleepCycle.setSummary(getSleepCycleSummary());
        mSound.setSummary(getRingtoneSummary());
        mShortDate.setSummary(getShortDate());
        mSnooze.setSummary(getSnoozeSummary());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*
         *  Remove listener
         *  Unlike in the Android docs, where register and unregister are done in the
         *  onResume and onPause, that fails if the selection of a setting involves
         *  something that triggers the onPause. As the listener will not trigger since
         *  it was unregistered.  This is the case for the ring tone selector.
         */
        mPrefDB.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences shared, String key) {
        // Update the Account and mark for update for the next time Refresh runs.
        if (key.equals(SP_REG_DISPLAY)) {
            mActorName.setSummary(mPrefDB.getString(PREF_DISPNAME, ""));
            Actor ali = new Actor(getActivity());
            ali.display = shared.getString(SP_REG_DISPLAY, ali.display);
            ali.force = true;
            ali.SyncPrime(false, getActivity());
        }
        if (key.equals(SP_REG_SCYCLE)) {
            mSleepCycle.setSummary(getSleepCycleSummary());
            Actor ali = new Actor(getActivity());
            try { ali.sleepcycle = Integer.parseInt(shared.getString(SP_REG_SCYCLE, ali.sleepcycleStr())); }
            catch (NumberFormatException ex){ /*skip it*/}
            ali.force = true;
            ali.SyncPrime(false, getActivity());
        }
        if (key.equals(PREF_SOUND)) {
            mSound.setSummary(getRingtoneSummary());
        }
        if (key.equals(PREF_PICKSHORTDATEFMT)){
            mShortDate.setSummary(getShortDate());
        }
        if (key.equals(PREF_SNOOZE)){
            mSnooze.setSummary(getSnoozeSummary());
        }
    }

    private String getSleepCycleSummary(){
        int sc = 2;
        try { sc = Integer.parseInt(mPrefDB.getString(SP_REG_SCYCLE, "2")); }
        catch (NumberFormatException ex){ /*skip it*/}

        String [] cycles = getResources().getStringArray(R.array.sleepcycle);
        return cycles[sc];
    }

    private String getSnoozeSummary(){
        String sd = mPrefDB.getString(PREF_SNOOZE, "60");
        String summary = getResources().getString(R.string.prf_SnoozeSum);
        return String.format(summary, sd);
    }

    private String getShortDate(){
        String SDATE_MID = "mid";
        String SDATE_BIG = "big";
        String SDATE_SML = "sml";
        int sdNdx = 0; // middle(US)

        String sd = mPrefDB.getString(PREF_PICKSHORTDATEFMT, "");
        if(sd.equalsIgnoreCase(SDATE_MID)) sdNdx = 0;
        if(sd.equalsIgnoreCase(SDATE_SML)) sdNdx = 1;
        if(sd.equalsIgnoreCase(SDATE_BIG)) sdNdx = 2;
        String summary = getResources().getString(R.string.prf_PickDateFormatSum);
        String [] dtypes = getResources().getStringArray(R.array.dateorder);

        return String.format(summary, dtypes[sdNdx].substring(0, 12));
    }

    private String getRingtoneSummary() {
        final String path = mPrefDB.getString(PREF_SOUND, "");
        if (!path.isEmpty()) {
            final Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), Uri.parse(path));
            return ringtone.getTitle(getActivity().getApplicationContext());
        }
        return getResources().getString(R.string.silent);
    }

}
