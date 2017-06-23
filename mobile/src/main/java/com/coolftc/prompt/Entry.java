package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
    private Spinner mTimename;                   // Simple time - name
    private Spinner mTimeadj;                    // Simple time - adjustment
    private ArrayAdapter<String> mTimeadjAdapter;// Allows changes to mTimeadj spinner.
    private List<String> mTimeadjData;           // Holds the raw data for mTimeadj

    // This data needs explicit persistence
    private String mTargetTime;                  // If Simple time is exact, this is the real time.
    private int mRecurUnit = RECUR_INVALID;      // The units used for recurrence.
    private int mRecurPeriod = RECUR_INVALID;    // The period used for recurrence.
    private int mRecurNbr = RECUR_INVALID;       // The number of times to recur (has priority over mRecurEnd).
    private String mRecurEnd;                    // The end date used for recurrence.

    // Using constants instead of an enum for these Simple time names.
    private static final int EXACT = 1;
    private static final int TODAY = 2;
    private static final int TONIGHT = 3;
    private static final int TOMORROW = 4;
    private static final int DEFAULT_NAME = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        // For data with no impact on the visuals, see onRestoreInstanceState().
        if (savedInstanceState != null) {
            mTarget = (Account) savedInstanceState.getSerializable(IN_USER_ACCT);
        }else {
            if (extras != null) {
                mTarget = (Account) extras.getSerializable(IN_USER_ACCT);
            } else {
                // if nothing passed in, just default self-message.
                mTarget = new Actor(this);
            }
        }

        // Set up main view and menu.
        setContentView(R.layout.entry);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configure the Spinners, set default, add listeners after to speed things up.
        mTimename = (Spinner) findViewById(R.id.sendTimeName);
        mTimeadj = (Spinner) findViewById(R.id.sendTimeAdj);
        mTimeadjData = Arrays.asList(getResources().getStringArray(R.array.time_adj));
        mTimeadjAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        mTimeadjAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ResetSpinAdj();
        mTimeadj.setAdapter(mTimeadjAdapter);
        mTimeadjAdapter.notifyDataSetChanged();
        mTimename.setSelection(DEFAULT_NAME);
        mTimeadj.setSelection(2);

        mTimename.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
           @Override
           // Can call getItemAtPosition(position) if need to access the selected item.
           public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
               switch (pos) {
                   case 0:
                       PopulateSpinAdj(TODAY);
                       break;
                   case 1:
                       PopulateSpinAdj(TONIGHT);
                       break;
                   default:
                       ResetSpinAdj();
                       break;
               }
               ShowDetails();
           }
           @Override
           public void onNothingSelected(AdapterView<?> arg0) {  }
        });
        mTimeadj.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            // Can call getItemAtPosition(position) if need to access the selected item.
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if(mTimename.getSelectedItemPosition() > 0) {// ignore if exact time set in mTimename
                    ShowDetails();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {  }
        });

        ShowDetails();
    }

    /*
     *  Since this is a data entry screen, with some data collection in dialogs,
     *  we need to persist that extra data in the case of Activity resets.  Make
     *  sure to call the super as the last thing done.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mTargetTime == null) mTargetTime = "";
        outState.putString(IN_TIMESTAMP, mTargetTime);
        outState.putInt(IN_UNIT, mRecurUnit);
        outState.putInt(IN_PERIOD, mRecurPeriod);
        outState.putInt(IN_ENDNBR, mRecurNbr);
        if (mRecurEnd == null) mRecurEnd = "";
        outState.putString(IN_ENDTIME, mRecurEnd);
        outState.putSerializable(IN_USER_ACCT, mTarget);
        super.onSaveInstanceState(outState);
    }
    /*
     *  Restore the state.  See onSaveInstanceState().
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTargetTime = savedInstanceState.getString(IN_TIMESTAMP);
        mRecurUnit = savedInstanceState.getInt(IN_UNIT);
        mRecurPeriod = savedInstanceState.getInt(IN_PERIOD);
        mRecurNbr = savedInstanceState.getInt(IN_ENDNBR);
        mRecurEnd = savedInstanceState.getString(IN_ENDTIME);
    }

    /*
     *  When some time names are selected, it is nice to remove any unattainable adjustments
     *  from the selections. Generally the idea is to remove adjustments that are in the past.
     */
    private void PopulateSpinAdj(int name){
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (name == EXACT) {
            mTimeadj.setSelection(0);
            mTimeadjAdapter.clear();
            mTimeadjAdapter.add(mTimeadjData.get(0));
        }

        if (name == TODAY) {
            mTimeadj.setSelection(0);
            mTimeadjAdapter.clear();
            mTimeadjAdapter.add(mTimeadjData.get(0));
            if (hour < 12) mTimeadjAdapter.add(mTimeadjData.get(1)); // Morning 7 - 11:59am
            if (hour < 18) mTimeadjAdapter.add(mTimeadjData.get(2)); // Afternoon 12 - 5pm
            if (hour < 23) mTimeadjAdapter.add(mTimeadjData.get(3)); // Night 5 - 11pm
            if (hour < 10) mTimeadjAdapter.add(mTimeadjData.get(4)); // Early 6 - 9am
            mTimeadjAdapter.add(mTimeadjData.get(5)); // Late 9 - 11:59pm
            mTimeadjAdapter.add(mTimeadjData.get(6)); // Next
        }

        if (name == TONIGHT){
            mTimeadj.setSelection(0);
            mTimeadjAdapter.clear();
            mTimeadjAdapter.add(mTimeadjData.get(0));
            if (hour < 21) mTimeadjAdapter.add(mTimeadjData.get(4)); // Early 5 - 9pm
            mTimeadjAdapter.add(mTimeadjData.get(5)); // Late 9 - 11:59pm
            // Skipping mTimeadjData(6) "Next", because Tomorrow Night is how people say it.
        }

        if (name == TOMORROW) {
            mTimeadj.setSelection(0);
            mTimeadjAdapter.clear();
            mTimeadjAdapter.add(mTimeadjData.get(0));
            mTimeadjAdapter.add(mTimeadjData.get(1));
            mTimeadjAdapter.add(mTimeadjData.get(2));
            mTimeadjAdapter.add(mTimeadjData.get(3));
            mTimeadjAdapter.add(mTimeadjData.get(4));
            mTimeadjAdapter.add(mTimeadjData.get(5));
            // Skipping mTimeadjData(6) "Next" because it is kind of ambiguous.
        }

        mTimeadjAdapter.notifyDataSetChanged();
    }

    /*
     *  Fill out the time adjustments spinner with the full list of items.
     */
    private void ResetSpinAdj(){
        mTimeadj.setSelection(0);
        mTimeadjAdapter.clear();
        for (String adj : mTimeadjData) {
            mTimeadjAdapter.add(adj);
        }
        mTimeadjAdapter.notifyDataSetChanged();
    }

    /*
     *  Since the spinner can be changed, need to check explicitly what is selected based on original data.
     */
    private int GetRealAdj(String adj) {
        int ndx = 0;
        for (String hold : mTimeadjData) {
            if(adj.equalsIgnoreCase(hold)) return ndx;
            ndx++;
        }
        return 0;  // if not found fall back to zero
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
        if(holdText != null) { holdText.setText(mTarget.unique);}

        // When are they getting the message (can change).
        String holdRaw = getResources().getString(R.string.deliver) + " ";
        // Special processing if an exact time is in use
        holdChkBox = (CheckBox) findViewById(R.id.sendExactTime);
        if(holdChkBox != null && holdChkBox.isChecked()) { // Exact time.
            String dTime = "";
            String dateTimeFmt = Settings.getDateDisplayFormat(getApplicationContext(), DATE_TIME_FMT_SHORT);
            try {
                Calendar delivery = KTime.ParseToCalendar(mTargetTime, KTime.KT_fmtDate3339fk);
                dTime = DateFormat.format(dateTimeFmt, delivery).toString();
            } catch (ExpParseToCalendar expParseToCalendar) {
                dTime = KTime.ParseNow(dateTimeFmt).toString();
            }
            holdRaw += dTime;
            mTimename.setEnabled(false);  // Blank out the time name
            mTimeadj.setEnabled(false);
        } else {
            holdRaw += mTimename.getSelectedItem().toString();
            if (mTimeadj.getSelectedItemPosition() > 0)
                holdRaw += ", " + mTimeadj.getSelectedItem().toString();
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
                holdText.setText(GetRecurringVerb());
                holdText.setVisibility(View.VISIBLE);
            }
        }
    }

    /*
     *  Generate the natural language terms for the recurring period.
     */
    private String GetRecurringVerb() {
        int holdResource = 0;
        String weekdays = "";

        // Day
        if(mRecurUnit == UNIT_TYPE_DAY){
            // Daily
            if(mRecurPeriod == 1){
                // Ending
                if(mRecurNbr > 0){
                    holdResource = R.string.recur_daily_nbr;
                }else{
                    if(IsForever()){
                        holdResource = R.string.recur_daily_end;
                    } else {
                        holdResource = R.string.recur_daily_day;
                    }
                }
            } else { // More than 1 day
                // Ending
                if(mRecurNbr > 0){
                    holdResource = R.string.recur_day_nbr;
                }else{
                    if(IsForever()){
                        holdResource = R.string.recur_day_end;
                    } else {
                        holdResource = R.string.recur_day_day;
                    }
                }
            }
        }

        // Week
        if(mRecurUnit == UNIT_TYPE_WEEKDAY){
            if(mRecurNbr > 0){
                holdResource = R.string.recur_wek_nbr;
            }else{
                if(IsForever()){
                    holdResource = R.string.recur_wek_end;
                } else {
                    holdResource = R.string.recur_wek_day;
                }
            }

            // Build out the weekday string, shorted if too many days choosen.
            if((mRecurPeriod & SUN_FLAG) == SUN_FLAG) { weekdays += getResources().getText(R.string.sunday_abbr) + ", "; }
            if((mRecurPeriod & MON_FLAG) == MON_FLAG) { weekdays += getResources().getText(R.string.monday_abbr) + ", "; }
            if((mRecurPeriod & TUE_FLAG) == TUE_FLAG) { weekdays += getResources().getText(R.string.tuesday_abbr) + ", "; }
            if((mRecurPeriod & WED_FLAG) == WED_FLAG) { weekdays += getResources().getText(R.string.wednsday_abbr) + ", "; }
            if((mRecurPeriod & THU_FLAG) == THU_FLAG) { weekdays += getResources().getText(R.string.thursday_abbr) + ", "; }
            if((mRecurPeriod & FRI_FLAG) == FRI_FLAG) { weekdays += getResources().getText(R.string.friday_abbr) + ", "; }
            if((mRecurPeriod & SAT_FLAG) == SAT_FLAG) { weekdays += getResources().getText(R.string.saturday_abbr) + ", "; }
            weekdays = weekdays.substring(0, weekdays.length()-2); // trim off the trailing comma.
            if(weekdays.length() > 13){
                weekdays = weekdays.substring(0, 13) + "...";
            }

        }

        // Month
        if(mRecurUnit == UNIT_TYPE_MONTH){
            if(mRecurPeriod == 1){
                if(mRecurNbr > 0){
                    holdResource = R.string.recur_monthly_nbr;
                }else{
                    if(IsForever()){
                        holdResource = R.string.recur_monthly_end;
                    } else {
                        holdResource = R.string.recur_monthly_day;
                    }
                }
            } else {
                if (mRecurNbr > 0) {
                    holdResource = R.string.recur_mon_nbr;
                } else {
                    if (IsForever()) {
                        holdResource = R.string.recur_mon_end;
                    } else {
                        holdResource = R.string.recur_mon_day;
                    }
                }
            }
        }

        if(holdResource == 0) return "";
        String template = getResources().getText(holdResource).toString();
        return String.format(template, mRecurPeriod, mRecurNbr, mRecurEnd, weekdays);
    }

    /*
     *  Calculate if the date is effectively "forever".
     */
    private boolean IsForever(){
        try {
            return KTime.CalcDateDifference(mRecurEnd, KTime.ParseNow(KTime.KT_fmtDate3339k).toString(), KTime.KT_fmtDate3339k, KTime.KT_YEARS) > FOREVER_LESS;
        } catch (ExpParseToCalendar ex) {
            return false;
        }
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
                if (mRecurUnit == RECUR_INVALID) {
                    mRecurUnit = RECUR_UNIT_DEFAULT;
                }
                if (mRecurPeriod == RECUR_INVALID) {
                    mRecurPeriod = RECUR_PERIOD_DEFAULT;
                }
                if (mRecurEnd == null) {
                    mRecurEnd = RECUR_END_DEFAULT;
                }
                if (mRecurNbr == RECUR_INVALID) {
                    mRecurNbr = RECUR_END_NBR;
                }
                String displayTime = "";
                TextView holdText = (TextView) findViewById(R.id.sendTargeTime);
                if (holdText != null) {
                    displayTime = holdText.getText().toString();
                }
                recurring.putExtra(IN_UNIT, mRecurUnit);
                recurring.putExtra(IN_PERIOD, mRecurPeriod);
                recurring.putExtra(IN_ENDTIME, mRecurEnd);
                recurring.putExtra(IN_ENDNBR, mRecurNbr);
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
                    if (KTime.IsPast(mTargetTime, KTime.KT_fmtDate3339fk)) {
                        mTargetTime = KTime.ParseNow(KTime.KT_fmtDate3339fk).toString();
                    }
                } catch (ExpParseToCalendar expParseToCalendar) {
                    mTargetTime = KTime.ParseNow(KTime.KT_fmtDate3339fk).toString();
                }
                Intent timestamp = new Intent(this, ExactTime.class);
                timestamp.putExtra(IN_TIMESTAMP, mTargetTime);
                startActivityForResult(timestamp, KY_DATETIME);
            }else { // unchecked
                ResetSpinAdj();
                mTimename.setEnabled(true);
                mTimeadj.setEnabled(true);
                mTimename.setSelection(DEFAULT_NAME); // Just return to a know safe state.
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
                    mTargetTime = data.getExtras().getString(IN_TIMESTAMP);
                    PopulateSpinAdj(EXACT);
                    ShowDetails();
                }else{
                    ResetSpinAdj();
                    mTimename.setSelection(DEFAULT_NAME); // Just return to a know safe state.
                }
                break;
            case KY_RECURE:     // Returning from recurrence picker.
                if (resultCode == RESULT_OK) {
                    mRecurUnit = data.getExtras().getInt(IN_UNIT);
                    mRecurPeriod = data.getExtras().getInt(IN_PERIOD);
                    mRecurEnd = data.getExtras().getString(IN_ENDTIME);
                    mRecurNbr = data.getExtras().getInt(IN_ENDNBR);
                    ShowDetails();
                } else {        // Not OK, set check box to false and reset local data
                    CheckBox holdChkBox = (CheckBox) findViewById(R.id.sendRecure);
                    if(holdChkBox!=null) { holdChkBox.setChecked(false); }
                    mRecurUnit = RECUR_INVALID;
                    mRecurPeriod = RECUR_INVALID;
                    mRecurNbr = RECUR_INVALID;
                    mRecurEnd = "";
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
        WebServices ws = new WebServices();

        if(!ws.IsNetwork(this)) {
            Toast.makeText(this, R.string.msgNoNet, Toast.LENGTH_LONG).show();
            return;
        }

        ali.target = mTarget;
        holdText = (TextView) findViewById(R.id.sendMessage);
        if(holdText!=null) { ali.message = holdText.getText().toString(); }
        if(ali.message.length()==0) { ali.message = getResources().getString(R.string.ent_DefaulMsg); }
        ali.targetTime = mTargetTime;
        ali.targetTimenameId = mTimename.getSelectedItemPosition() + 1;
        ali.targetTimename = mTimename.getSelectedItem().toString();
        ali.targetTimeadjId = GetRealAdj(mTimeadj.getSelectedItem().toString());
        ali.targetTimeadj = mTimeadj.getSelectedItem().toString();
        ali.recureUnit = mRecurUnit;
        ali.recurePeriod = mRecurPeriod;
        ali.recureNumber = mRecurNbr;
        ali.recureEnd = mRecurEnd;

        SendMessageThread smt = new SendMessageThread(getApplicationContext(), ali);
        smt.start();

        Intent intent = new Intent(this, History.class);
        startActivity(intent);

    }
}
