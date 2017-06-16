package com.coolftc.prompt;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import static com.coolftc.prompt.Constants.*;

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
            ali.sleepcycle = shared.getInt(SP_REG_SCYCLE, ali.sleepcycle);
            ali.force = true;
            ali.SyncPrime(false, getActivity());
        }
    }
}
