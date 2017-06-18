package com.coolftc.prompt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static com.coolftc.prompt.Constants.*;
import static com.coolftc.prompt.Settings.PREF_SETTINGS;
import static com.coolftc.prompt.Settings.PREF_VIBRATEON;

/**
 *  The very basic fragment to display settings.  See the Settings
 *  activity for all the important stuff.  Note that you need to
 *  place the listener here in the fragment to pick up changes.
 */
public class SettingsBasic extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        // Do not bother showing the showing the selection if the device does not support it.
        final Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (!v.hasVibrator()) {
            PreferenceCategory cat = (PreferenceCategory) findPreference(PREF_SETTINGS);
            Preference pref = findPreference(PREF_VIBRATEON);
            cat.removePreference(pref);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences shared, String key) {
        // Update the Account and mark for update for the next time Refresh runs.
        if (key.equals(SP_REG_DISPLAY) || key.equals(SP_REG_SCYCLE)) {
            Account ali = new Account(getActivity(),true);
            ali.display = shared.getString(SP_REG_DISPLAY, ali.display);
            try { ali.sleepcycle = Integer.parseInt(shared.getString(SP_REG_SCYCLE, ali.sleepcycleStr())); }
            catch (NumberFormatException ex){ /*skip it*/}
            ali.force = true;
            ali.SyncPrime(false, getActivity());
        }
    }
}
