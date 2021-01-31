package com.coolftc.prompt;

import static com.coolftc.prompt.utility.Constants.*;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.coolftc.prompt.service.SendMessageThread;
import com.coolftc.prompt.source.WebServicesOld;
import com.coolftc.prompt.utility.ExpParseToCalendar;
import com.coolftc.prompt.utility.KTime;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Arrays;
import java.util.List;

/**
 *  The Entry screen lets a person compose a message, as well as specify
 *  the time settings.  Composition of the message is just entering text.
 *
 *  Selecting the delivery time is more complex.  It can be done using
 *  the Simple Time settings (name + adjustment) or by choosing a specific
 *  date/time using the date/time dialog.  Additionally, a repeating
 *  attribute can be added to a message, again using another dialog.
 *
 *  This screen requires no data* or network access. It gets the "who"
 *  from welcome (if primary) or the contact picker, and sends the
 *  message by starting a thread.  After starting the thread, the
 *  history screen is shown, where the status of a message can be
 *  shown (e.g. sending, etc.).
 *  The thread handles writing to the DB and calling the web service.
 *  *If no data is passed in, it does read from SharedPreferences.
 */
public class Entry extends AppCompatActivity {

    // Data needed to create a message
    private Account mTarget;                     // Who is getting this message
    private Reminder mPrompt;                    // Working copy of the message
    private Spinner mTimename;                   // Simple time - name
    private List<String> mTimeadjData;           // Holds the raw data for mTimeadj
    private SeekBar mTimeadj;                    // Simple time - adjustment

    // Useful information
    private int mDefaultTimeName = 1;           // Today
    private int mDefaultTimeAdj = 47;           // Mid point between early and late
    private String mDefaultMessage = "";
    private static final int SEEK_MARK = 16;    // The seek bar has a range of 0 - 95, includes 5 marks
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        boolean firstRun = false;
        // When saved data is also passed in normally, it needs to be restored here.
        if (savedInstanceState != null) {
            mTarget = (Account) savedInstanceState.getSerializable(IN_USER_ACCT);
            mPrompt = (Reminder) savedInstanceState.getSerializable(IN_MESSAGE);
        }else {
            firstRun = true;
            if (extras != null) {
                mTarget = (Account) extras.getSerializable(IN_USER_ACCT);
                if(mTarget == null) {
                    mTarget = new Actor(this);  // Default to self-message.
                }
                // This passed-in message data is used when making a copy.
                // Since exact time is not useful, just default to Today.
                mPrompt = (Reminder) extras.getSerializable(IN_MESSAGE);
                if(mPrompt != null){
                    mDefaultTimeName = mPrompt.GetTargetTimeNameIdDsply();
                    if(mDefaultTimeName == 0) ++mDefaultTimeName;
                    mDefaultTimeAdj = mPrompt.GetTargetTimeAdjIdDsply() * SEEK_MARK;
                    mDefaultMessage = mPrompt.message;
                    mPrompt.target = mTarget;   // Just to be complete
                } else {
                    mPrompt = new Reminder();
                }
            } else {
                // if nothing passed in, just make self-message with defaults.
                mTarget = new Actor(this);
                mPrompt = new Reminder();
            }
        }

        // Set up main view and menu.
        setContentView(R.layout.entry);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configure the Spinners, set defaults, add listeners.
        if(firstRun) {
            TextView holdText = (TextView) findViewById(R.id.sendMessage);
            if (holdText != null && mDefaultMessage.length() > 0) {
                holdText.setText(mDefaultMessage);
            }
            CheckBox holdChkBox = (CheckBox) findViewById(R.id.sendRecure);
            if (holdChkBox != null && mPrompt.recurUnit != RECUR_INVALID) {
                holdChkBox.setChecked(true);
            }
        }

        mTimename = (Spinner) findViewById(R.id.sendTimeName);
        mTimename.setSelection(mDefaultTimeName);

        mTimeadj = (SeekBar) findViewById(R.id.sendTimeAdj);
        mTimeadj.setProgress(mDefaultTimeAdj);
        mTimeadjData = Arrays.asList(getResources().getStringArray(R.array.time_adj));

        mTimename.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
           @Override
           // Can call getItemAtPosition(position) if need to access the selected item.
           public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
               ShowDetails();
           }
           @Override
           public void onNothingSelected(AdapterView<?> arg0) {  }
        });
        mTimeadj.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ShowDetails();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Set up analytics.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        ShowDetails();
    }

    /*
     *  Since this is a data entry screen, with some data collection in dialogs,
     *  we need to persist that extra data in the case of Activity resets.  Make
     *  sure to call the super as the last thing done.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(IN_USER_ACCT, mTarget);
        outState.putSerializable(IN_MESSAGE, mPrompt);
        super.onSaveInstanceState(outState);
    }
    /*
     *  Restore the state, compliments of onSaveInstanceState().
     *  NOTE: If you do not see something here, often we need to
     *  restore data in the OnCreate().
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /*
     *  Population of the screen.
     */
    private void ShowDetails(){
        ImageView holdImage;
        TextView holdText;
        CheckBox holdChkBox;

        // Who is getting the message (does not really change).
        holdImage = (ImageView) findViewById(R.id.sendFacePic);
        if(holdImage != null && mTarget.contactPicUri().length() > 0) {
            holdImage.setImageURI(Uri.parse(mTarget.contactPicUri()));
        }
        holdText = (TextView) findViewById(R.id.sendContactName);
        if(holdText != null) { holdText.setText(mTarget.bestName());}
        holdText = (TextView) findViewById(R.id.sendContactExtra);
        if(holdText != null) { holdText.setText(!mTarget.primary ? mTarget.bestNameAlt() : "");}

        // When are they getting the message (can change).
        String holdRaw = getResources().getString(R.string.deliver) + " ";
        // Special processing if an exact time is in use
        holdChkBox = (CheckBox) findViewById(R.id.sendExactTime);
        if(holdChkBox != null && holdChkBox.isChecked()) { // Exact time.
            holdRaw += mPrompt.GetPromptTime(getApplicationContext());
            mTimename.setEnabled(false);  // Blank out the time name
            mTimeadj.setEnabled(false);
        } else {
            holdRaw += mTimename.getSelectedItem().toString();
            if(mTimename.getSelectedItemPosition() > 0) {
                holdRaw += ", " + GetTimeAdjustment(mTimename.getSelectedItemPosition());
                mTimeadj.setEnabled(true);
            } else {
                mTimeadj.setEnabled(false);
            }
        }
        holdText = (TextView) findViewById(R.id.sendTargeTime);
        if(holdText != null) { holdText.setText(holdRaw);}

        // How often will they get the message (if once, hide)
        holdChkBox = (CheckBox) findViewById(R.id.sendRecure);
        if(holdChkBox == null || !holdChkBox.isChecked()) {
            holdText = (TextView) findViewById(R.id.sendRecurTime);
            if (holdText != null) { holdText.setVisibility(View.INVISIBLE); }
        }else{
            holdText = (TextView) findViewById(R.id.sendRecurTime);
            if (holdText != null) {
                holdText.setText(mPrompt.GetRecurringVerb(getApplicationContext()));
                holdText.setVisibility(View.VISIBLE);
            }
        }
    }

    /*
     *  The time adjustments sometimes need to be finessed for time names before tomorrow.
     *  This will force all adjustments for Tonight to avoid morning or afternoon.
     */
    private String GetTimeAdjustment(int name){
        if(name == 2){  // tonight
            if(mTimeadj.getProgress() < 49){
                return mTimeadjData.get(0);
            }
        }

        return mTimeadjData.get(mTimeadj.getProgress() / SEEK_MARK);
    }

    /*
     *  This brings up the recurring prompt dialog, feeding in default values if needed.
     *  See onActivityResult for the reply.
     */
    public void ShowRecurring(View view) {
        CheckBox holdChk = (CheckBox) view;
        if(holdChk != null) {
            if (holdChk.isChecked()) {
                Intent recurring = new Intent(this, Recurrence.class);
                if (mPrompt.recurUnit == RECUR_INVALID) {
                    mPrompt.recurUnit = RECUR_UNIT_DEFAULT;
                }
               if (mPrompt.recurEnd == null) {
                   mPrompt.recurEnd = RECUR_END_DEFAULT;
                }
                String displayTime = "";
                TextView holdText = (TextView) findViewById(R.id.sendTargeTime);
                if (holdText != null) {
                    displayTime = holdText.getText().toString();
                }
                recurring.putExtra(IN_UNIT, mPrompt.recurUnit);
                recurring.putExtra(IN_PERIOD, mPrompt.recurPeriod);
                recurring.putExtra(IN_ENDTIME, mPrompt.recurEnd);
                recurring.putExtra(IN_ENDNBR, mPrompt.recurNumber);
                recurring.putExtra(IN_DISP_TIME, displayTime);
                startActivityForResult(recurring, KY_RECURE);
            }else{ // unchecked
                ShowDetails();
            }
        }
    }

    /*
     *  Called by clicking on the exact time checkbox.
     *  - True: This means it was false, so can just go to the date picker.
     *      See onActivityResult for the reply.
     *  - False: This means we need to clean things up and return to a default time name.
     */
    public void ShowExactTime(View view){
        CheckBox holdChkBox = (CheckBox) view;
        if(holdChkBox != null){
            if(holdChkBox.isChecked()) {
                try {
                    if (KTime.IsPast(mPrompt.targetTime, KTime.KT_fmtDate3339fk)) {
                        mPrompt.targetTime = KTime.ParseNow(KTime.KT_fmtDate3339fk).toString();
                    }
                } catch (ExpParseToCalendar expParseToCalendar) {
                    mPrompt.targetTime = KTime.ParseNow(KTime.KT_fmtDate3339fk).toString();
                }
                Intent timestamp = new Intent(this, ExactTime.class);
                timestamp.putExtra(IN_TIMESTAMP, mPrompt.targetTime);
                startActivityForResult(timestamp, KY_DATETIME);
            }else { // unchecked
                mTimename.setEnabled(true);
                mTimeadj.setEnabled(true);
                ShowDetails();
            }
        }
    }

    /*
     *  This method processes any Activity responses.  Generally this is when some page
     *  has been navigated to, and upon its return it has data that needs to be processed.
     *  Probably want to reload the screen at that point.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case KY_DATETIME:     // Returning from the datetime picker.
                if (resultCode == RESULT_OK) {
                    mPrompt.targetTime = data.getExtras().getString(IN_TIMESTAMP);
                    ShowDetails();
                }else{
                    CheckBox holdChkbox = (CheckBox) findViewById(R.id.sendExactTime);
                    if(holdChkbox != null) {holdChkbox.setChecked(false);}
                }
                break;
            case KY_RECURE:     // Returning from recurrence picker.
                if (resultCode == RESULT_OK) {
                    mPrompt.recurUnit = data.getExtras().getInt(IN_UNIT);
                    mPrompt.recurPeriod = data.getExtras().getInt(IN_PERIOD);
                    mPrompt.recurNumber = data.getExtras().getInt(IN_ENDNBR);
                    mPrompt.recurEnd = data.getExtras().getString(IN_ENDTIME);
                    ShowDetails();
                } else {        // Not OK, set check box to false and reset local data
                    CheckBox holdChkBox = (CheckBox) findViewById(R.id.sendRecure);
                    if(holdChkBox!=null) { holdChkBox.setChecked(false); }
                    mPrompt.recurUnit = RECUR_INVALID;
                    mPrompt.recurPeriod = RECUR_INVALID;
                    mPrompt.recurNumber = RECUR_INVALID;
                    mPrompt.recurEnd = "";
                }
        }
    }

    /*
     *  Create the notification by:
     *  1: Collecting the desired information from the GUI.
     *  2: Checking the network is available.
     *  3: Starting a thread to:
     *      a. Save data to table with status = sending.
     *      b. Send data to web service.
     *      c. Update status on table to sent or failed, along with key and specific time.
     *  4: Reset UI for next message.
     *  5: Launch the history Activity (to see messages & status)
     */
    public void SendMessage(View view) {
        TextView holdText;

        Reminder ali = new Reminder();
        WebServicesOld ws = new WebServicesOld();

        if(!ws.IsNetwork(this)) {
            Toast.makeText(this, R.string.msgNoNet, Toast.LENGTH_LONG).show();
            return;
        }

        ali.target = mTarget;
        ali.from = new Actor(this);
        holdText = (TextView) findViewById(R.id.sendMessage);
        if(holdText!=null) { ali.message = holdText.getText().toString(); }
        if(ali.message.length()==0) { ali.message = getResources().getString(R.string.ent_DefaulMsg); }
        if(ali.message.length() > MSG_MAX_LENGTH) { ali.message = ali.message.substring(0, MSG_MAX_LENGTH); }
        ali.targetTime = mPrompt.targetTime;
        // Listbox index is one less that the value we need for the time name.
        CheckBox holdChkBox = (CheckBox) findViewById(R.id.sendExactTime);
        if(holdChkBox != null && holdChkBox.isChecked()) { // Exact time.
            ali.targetTimeNameId = 0;
        } else {
            ali.SetTargetTimeNameIdDsply(mTimename.getSelectedItemPosition());
        }
        // Want a number from 0 to 5, so do an integer division to truncate fraction.
        ali.SetTargetTimeAdjIdDsply(mTimeadj.getProgress() / SEEK_MARK);
        ali.recurUnit = mPrompt.recurUnit;
        ali.recurPeriod = mPrompt.recurPeriod;
        ali.recurNumber = mPrompt.recurNumber;
        ali.recurEnd = mPrompt.recurEnd;

        SendMessageThread smt = new SendMessageThread(getApplicationContext(), ali);
        smt.start();

        Intent intent = new Intent(this, History.class);
        startActivity(intent);

        // Let Analytics know we tried to send a prompt.
        Bundle params = new Bundle();
        params.putString(AV_PM_SEND_WHO, ali.from.unique);
        params.putString(AV_PM_SEND_WHEN, KTime.ParseNow(KTime.KT_fmtDate3339fk).toString());
        mFirebaseAnalytics.logEvent(AN_EV_SEND, params);
    }
}
