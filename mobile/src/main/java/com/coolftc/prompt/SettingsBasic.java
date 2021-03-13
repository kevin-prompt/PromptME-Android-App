package com.coolftc.prompt;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.OpenableColumns;
import android.text.InputType;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.coolftc.prompt.service.Refresh;

import java.util.Locale;
import java.util.Objects;

import static com.coolftc.prompt.utility.Constants.*;
import static com.coolftc.prompt.Settings.*;

/**
 *  The very basic fragment to display settings.  See the Settings
 *  activity for all the important stuff.  Note that you need to
 *  place the listener here in the fragment to pick up changes.
 *
 *  May need to adjust the Theme
 *  See https://stackoverflow.com/questions/32070670/preferencefragmentcompat-requires-preferencetheme-to-be-set/44236460#44236460
 *
 *  Help with preferences : https://guides.codepath.com/android/settings-with-preferencefragment
 *
 */
public class SettingsBasic extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceManager.OnPreferenceTreeClickListener{

    private SharedPreferences mPrefDB;
    private Preference mActorName;
    private Preference mSleepCycle;
    private Preference mSound;
    private Preference mShortDate;
    private EditTextPreference mSnooze;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings);

        // Get local access to the default shared preference DB
        PreferenceManager pm = this.getPreferenceManager();
        pm.setOnPreferenceTreeClickListener(this);
        mPrefDB = pm.getSharedPreferences();
        mPrefDB.registerOnSharedPreferenceChangeListener(this);

        // Get all the preference rows that will need dynamic updating
        mActorName = findPreference(PREF_DISPNAME);
        mSleepCycle = findPreference(PREF_SCYCLE);
        mSound = findPreference(PREF_SOUND);
        mShortDate = findPreference(PREF_PICKSHORTDATEFMT);
        mSnooze = findPreference(PREF_SNOOZE);

        // Do not bother showing these selections if the device does not support it
        // or it is no longer meaningful.
        // Note: To modify the structure of the page, need to use the category.
        // No vibration choice if device does not vibrate.
        // Notification vibration is managed in the channel for v8.0+.
        final Vibrator v = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (!v.hasVibrator() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            PreferenceCategory cat = findPreference(PREF_SYSTEM);
            Preference vibrate = findPreference(PREF_VIBRATEON);
            Objects.requireNonNull(cat).removePreference(vibrate);
        }
        // Only offer to turn off the nag screen if it is still showing up.
        if(ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED){
            PreferenceCategory cat = findPreference(PREF_SYSTEM);
            Preference contacts = findPreference(PREF_CONTACTS);
            Objects.requireNonNull(cat).removePreference(contacts);
        }
        // Only show the option to verify if they have not yet verified.
        Actor user = new Actor(getActivity());
        if(!user.solo){
            PreferenceCategory cat = findPreference(PREF_SETTINGS);
            Preference verify = findPreference(PREF_VERIFICATION);
            Objects.requireNonNull(cat).removePreference(verify);
        }
        // Only show ringtone picker when they are on a version that will let it work.
        // Otherwise, they must change the sound under notification settings.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PreferenceCategory cat = findPreference(PREF_SYSTEM);
            Preference verify = findPreference(PREF_SOUND);
            Objects.requireNonNull(cat).removePreference(verify);
        }

        // Apply any initialization (mostly summaries)
        mActorName.setSummary(mPrefDB.getString(PREF_DISPNAME, ""));
        mSleepCycle.setSummary(getSleepCycleSummary());
        String defaultRingtone = String.format(Locale.getDefault(), "android.resource://%s/%d", requireContext().getPackageName(), R.raw.promptbeep);
        mSound.setSummary(displayName(Uri.parse(mPrefDB.getString(PREF_SOUND, defaultRingtone))));
        mShortDate.setSummary(getShortDate());
        mSnooze.setSummary(getSnoozeSummary());
        mSnooze.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

        // Check if the user will let us write (the ringtone) to external storage. We will trigger a Refresh
        // to do the write in either case, no harm and less work than implementing a callback in the Activity.
        int storagePermissionCheck = ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (storagePermissionCheck != PackageManager.PERMISSION_GRANTED) {
            // If permission has not been granted, ask for it, unless the user has explicitly said not to.
                ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, SEC_WRITE_STORAGE);
            Intent sIntent = new Intent(getActivity(), Refresh.class);
            requireActivity().startService(sIntent);
        }
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

    /*
        The RingtonePreference was removed from the AndroidX version of preferences.  This
        option was suggested at https://issuetracker.google.com/issues/37057453#comment2.
        This is mostly for picking a different notification sound. I try to make the one
        that comes with the program work without having to explicitly pick it.
     */
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey() != null && preference.getKey().equals(PREF_SOUND)) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);

            Uri existingValue =  Settings.getRingtone(requireContext());
            if (existingValue != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingValue);
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            }

            startActivityForResult(intent, KY_SOUND_PICKER);
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }
    // This is the callback from the onPreferenceTreeClick() for the notification sound.
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == KY_SOUND_PICKER && data != null) {
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            String ringtoneStr = ringtone != null ? ringtone.toString() : "";
            SharedPreferences.Editor ali = mPrefDB.edit();
            ali.putString(PREF_SOUND, ringtoneStr);
            ali.apply();
            mSound.setSummary(displayName(ringtone));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Get a display name for the sound.
    private String displayName(Uri uri) {
        if (uri == null || uri.toString().length() == 0) { return getResources().getString(R.string.none); }
        try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null, null)) {
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    String name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    if (name.equalsIgnoreCase("promptbeep.mp3")) {
                        name = getResources().getString(R.string.prf_NotificationTone);
                    }
                    return name;
                }
            }
            return getResources().getString(R.string.prf_NotificationTone);
        } catch (Exception ex) { return ""; }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences shared, String key) {
        // Update the Account and mark for update for the next time Refresh runs.
        if (key.equals(SP_REG_DISPLAY)) {
            mActorName.setSummary(mPrefDB.getString(PREF_DISPNAME, ""));
            Actor ali = new Actor(getActivity());
            ali.display = shared.getString(SP_REG_DISPLAY, ali.display);
            ali.force = true;
            ali.SyncPrime(false, requireActivity());
        }
        if (key.equals(SP_REG_SCYCLE)) {
            mSleepCycle.setSummary(getSleepCycleSummary());
            Actor ali = new Actor(getActivity());
            try { ali.sleepcycle = Integer.parseInt(Objects.requireNonNull(shared.getString(SP_REG_SCYCLE, ali.sleepcycleStr()))); }
            catch (NumberFormatException ex){ /*skip it*/}
            ali.force = true;
            ali.SyncPrime(false, requireActivity());
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
        try { sc = Integer.parseInt(Objects.requireNonNull(mPrefDB.getString(SP_REG_SCYCLE, "2"))); }
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
        if (sd != null) {
            if (sd.equalsIgnoreCase(SDATE_MID)) sdNdx = 0;
            if (sd.equalsIgnoreCase(SDATE_SML)) sdNdx = 1;
            if (sd.equalsIgnoreCase(SDATE_BIG)) sdNdx = 2;
        }
        String summary = getResources().getString(R.string.prf_PickDateFormatSum);
        String [] dtypes = getResources().getStringArray(R.array.dateorder);

        return String.format(summary, dtypes[sdNdx].substring(0, 12));
    }
}
