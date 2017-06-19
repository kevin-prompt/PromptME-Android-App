package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    private Account target;                     // Who is getting this message
    private Spinner timename;                   // Simple time - name
    private Spinner timeadj;                    // Simple time - adjustment
    private ArrayAdapter<String> timeadjAdapter;// Allows changes to timeadj spinner.
    private List<String> timeadjData;           // Holds the raw data for timeadj

    private String targetTime;                  // If Simple time is exact, this is the real time.
    private int recurUnit = RECUR_INVALID;      // The units used for recurrence.
    private int recurPeriod = RECUR_INVALID;    // The period used for recurrence.
    private int recurNbr = RECUR_INVALID;       // The number of times to recur (has priority over recurEnd).
    private String recurEnd;                    // The end date used for recurrence.

    // Using constants instead of an enum for these Simple time names.
    private static final int EXACT = 1;
    private static final int TODAY = 2;
    private static final int TONIGHT = 3;
    private static final int TOMORROW = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            target = (Account) extras.getSerializable(IN_USER_ACCT);
        } else {
            // if nothing passed in, just default self-message.
            target = new Actor(this);
        }

        // Set up main view and menu.
        setContentView(R.layout.entry);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Configure the Spinners, set default, add listeners after to speed things up.
        timename = (Spinner) findViewById(R.id.sendTimeName);
        timeadj = (Spinner) findViewById(R.id.sendTimeAdj);
        timeadjData = Arrays.asList(getResources().getStringArray(R.array.time_adj));
        timeadjAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<String>());
        timeadjAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        ResetSpinAdj();
        timeadj.setAdapter(timeadjAdapter);
        timeadjAdapter.notifyDataSetChanged();
        timename.setSelection(3);
        timeadj.setSelection(2);

        timename.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
           @Override
           // Can call getItemAtPosition(position) if need to access the selected item.
           public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
               switch (pos) {
                   case 0:
                       Intent timestamp = new Intent(parent.getContext(), ExactTime.class);
                       timestamp.putExtra(IN_TIMESTAMP, KTime.ParseNow(KTime.KT_fmtDate3339fk));
                       startActivityForResult(timestamp, KY_DATETIME);
                       return;
                   case 1:
                       PopulateSpinAdj(TODAY);
                       break;
                   case 2:
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
        timeadj.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            // Can call getItemAtPosition(position) if need to access the selected item.
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if(timename.getSelectedItemPosition() > 0) {// ignore if exact time set in timename
                    ShowDetails();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {  }
        });

        ShowDetails();
    }

    /*
     *  When some time names are selected, it is nice to remove any unattainable adjustments
     *  from the selections. Generally the idea is to remove adjustments that are in the past.
     */
    private void PopulateSpinAdj(int name){
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        if (name == EXACT) {
            timeadj.setSelection(0);
            timeadjAdapter.clear();
            timeadjAdapter.add(timeadjData.get(0));
        }

        if (name == TODAY) {
            timeadj.setSelection(0);
            timeadjAdapter.clear();
            timeadjAdapter.add(timeadjData.get(0));
            if (hour < 12) timeadjAdapter.add(timeadjData.get(1)); // Morning 7 - 11:59am
            if (hour < 18) timeadjAdapter.add(timeadjData.get(2)); // Afternoon 12 - 5pm
            if (hour < 23) timeadjAdapter.add(timeadjData.get(3)); // Night 5 - 11pm
            if (hour < 10) timeadjAdapter.add(timeadjData.get(4)); // Early 6 - 9am
            timeadjAdapter.add(timeadjData.get(5)); // Late 9 - 11:59pm
            timeadjAdapter.add(timeadjData.get(6)); // Next
        }

        if (name == TONIGHT){
            timeadj.setSelection(0);
            timeadjAdapter.clear();
            timeadjAdapter.add(timeadjData.get(0));
            if (hour < 21) timeadjAdapter.add(timeadjData.get(4)); // Early 5 - 9pm
            timeadjAdapter.add(timeadjData.get(5)); // Late 9 - 11:59pm
            // Skipping timeadjData(6) "Next", because Tomorrow Night is how people say it.
        }

        if (name == TOMORROW) {
            timeadj.setSelection(0);
            timeadjAdapter.clear();
            timeadjAdapter.add(timeadjData.get(0));
            timeadjAdapter.add(timeadjData.get(1));
            timeadjAdapter.add(timeadjData.get(2));
            timeadjAdapter.add(timeadjData.get(3));
            timeadjAdapter.add(timeadjData.get(4));
            timeadjAdapter.add(timeadjData.get(5));
            // Skipping timeadjData(6) "Next" because it is kind of ambiguous.
        }

        timeadjAdapter.notifyDataSetChanged();
    }

    /*
     *  Fill out the time adjustments spinner with the full list of items.
     */
    private void ResetSpinAdj(){
        timeadj.setSelection(0);
        timeadjAdapter.clear();
        for (String adj : timeadjData) {
            timeadjAdapter.add(adj);
        }
        timeadjAdapter.notifyDataSetChanged();
    }

    /*
     *  Since the spinner can be changed, need to check explicitly what is selected based on original data.
     */
    private int GetRealAdj(String adj) {
        int ndx = 0;
        for (String hold : timeadjData) {
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
        if(holdImage != null && target.contactPicUri().length() > 0) {
            holdImage.setImageURI(Uri.parse(target.contactPicUri()));
        }
        holdText = (TextView) findViewById(R.id.sendContactName);
        if(holdText != null) { holdText.setText(target.bestName());}
        holdText = (TextView) findViewById(R.id.sendContactExtra);
        if(holdText != null) { holdText.setText(target.unique);}

        // When are they getting the message (can change).
        String holdRaw = getResources().getString(R.string.deliver) + " ";
        if(timename.getSelectedItemPosition() == 0) { // Exact time.
            holdRaw += targetTime;
        } else {
            holdRaw += timename.getSelectedItem().toString();
            if (timeadj.getSelectedItemPosition() > 0)
                holdRaw += ", " + timeadj.getSelectedItem().toString();
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
        if(recurUnit == UNIT_TYPE_DAY){
            // Daily
            if(recurPeriod == 1){
                // Ending
                if(recurNbr > 0){
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
                if(recurNbr > 0){
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
        if(recurUnit == UNIT_TYPE_WEEKDAY){
            if(recurNbr > 0){
                holdResource = R.string.recur_wek_nbr;
            }else{
                if(IsForever()){
                    holdResource = R.string.recur_wek_end;
                } else {
                    holdResource = R.string.recur_wek_day;
                }
            }

            // Build out the weekday string, shorted if too many days choosen.
            if((recurPeriod & SUN_FLAG) == SUN_FLAG) { weekdays += getResources().getText(R.string.sunday_abbr) + ", "; }
            if((recurPeriod & MON_FLAG) == MON_FLAG) { weekdays += getResources().getText(R.string.monday_abbr) + ", "; }
            if((recurPeriod & TUE_FLAG) == TUE_FLAG) { weekdays += getResources().getText(R.string.tuesday_abbr) + ", "; }
            if((recurPeriod & WED_FLAG) == WED_FLAG) { weekdays += getResources().getText(R.string.wednsday_abbr) + ", "; }
            if((recurPeriod & THU_FLAG) == THU_FLAG) { weekdays += getResources().getText(R.string.thursday_abbr) + ", "; }
            if((recurPeriod & FRI_FLAG) == FRI_FLAG) { weekdays += getResources().getText(R.string.friday_abbr) + ", "; }
            if((recurPeriod & SAT_FLAG) == SAT_FLAG) { weekdays += getResources().getText(R.string.saturday_abbr) + ", "; }
            weekdays = weekdays.substring(0, weekdays.length()-2); // trim off the trailing comma.
            if(weekdays.length() > 13){
                weekdays = weekdays.substring(0, 13) + "...";
            }

        }

        // Month
        if(recurUnit == UNIT_TYPE_MONTH){
            if(recurPeriod == 1){
                if(recurNbr > 0){
                    holdResource = R.string.recur_monthly_nbr;
                }else{
                    if(IsForever()){
                        holdResource = R.string.recur_monthly_end;
                    } else {
                        holdResource = R.string.recur_monthly_day;
                    }
                }
            } else {
                if (recurNbr > 0) {
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
        return String.format(template, recurPeriod, recurNbr, recurEnd, weekdays);
    }

    /*
     *  Calculate if the date is effectively "forever".
     */
    private boolean IsForever(){
        try {
            return KTime.CalcDateDifference(recurEnd, KTime.ParseNow(KTime.KT_fmtDate3339k).toString(), KTime.KT_fmtDate3339k, KTime.KT_YEARS) > FOREVER_LESS;
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
                if (recurUnit == RECUR_INVALID) {
                    recurUnit = RECUR_UNIT_DEFAULT;
                }
                if (recurPeriod == RECUR_INVALID) {
                    recurPeriod = RECUR_PERIOD_DEFAULT;
                }
                if (recurEnd == null) {
                    recurEnd = RECUR_END_DEFAULT;
                }
                if (recurNbr == RECUR_INVALID) {
                    recurNbr = RECUR_END_NBR;
                }
                String displayTime = "";
                TextView holdText = (TextView) findViewById(R.id.sendTargeTime);
                if (holdText != null) {
                    displayTime = holdText.getText().toString();
                }
                recurring.putExtra(IN_UNIT, recurUnit);
                recurring.putExtra(IN_PERIOD, recurPeriod);
                recurring.putExtra(IN_ENDTIME, recurEnd);
                recurring.putExtra(IN_ENDNBR, recurNbr);
                recurring.putExtra(IN_DISP_TIME, displayTime);
                startActivityForResult(recurring, KY_RECURE);
            }else{ // unchecked
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
                    targetTime = data.getExtras().getString(IN_TIMESTAMP);
                    PopulateSpinAdj(EXACT);
                    ShowDetails();
                }else{
                    ResetSpinAdj();
                    timename.setSelection(3); // Just return to a know safe state.
                }
                break;
            case KY_RECURE:     // Returning from recurrence picker.
                if (resultCode == RESULT_OK) {
                    recurUnit = data.getExtras().getInt(IN_UNIT);
                    recurPeriod = data.getExtras().getInt(IN_PERIOD);
                    recurEnd = data.getExtras().getString(IN_ENDTIME);
                    recurNbr = data.getExtras().getInt(IN_ENDNBR);
                    ShowDetails();
                } else {        // Not OK, set check box to false and reset local data
                    CheckBox holdChkBox = (CheckBox) findViewById(R.id.sendRecure);
                    if(holdChkBox!=null) { holdChkBox.setChecked(false); }
                    recurUnit = RECUR_INVALID;
                    recurPeriod = RECUR_INVALID;
                    recurNbr = RECUR_INVALID;
                    recurEnd = "";
                }
        }
    }

    /*
        Select a new exact time if upon tapping the value.
     */
    public void NewExactTime(View view) {
        if(timename.getSelectedItemPosition() == 0) {
            Intent timestamp = new Intent(this, ExactTime.class);
            timestamp.putExtra(IN_TIMESTAMP, targetTime);
            startActivityForResult(timestamp, KY_DATETIME);
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

        ali.target = target;
        holdText = (TextView) findViewById(R.id.sendMessage);
        if(holdText!=null) { ali.message = holdText.getText().toString(); }
        if(ali.message.length()==0) { ali.message = "No Message - Just an alert."; }
        ali.targetTime = targetTime;
        ali.targetTimenameId = timename.getSelectedItemPosition();
        ali.targetTimename = timename.getSelectedItem().toString();
        ali.targetTimeadjId = GetRealAdj(timeadj.getSelectedItem().toString());
        ali.targetTimeadj = timeadj.getSelectedItem().toString();
        ali.recureUnit = recurUnit;
        ali.recurePeriod = recurPeriod;
        ali.recureNumber = recurNbr;
        ali.recureEnd = recurEnd;

        SendMessageThread smt = new SendMessageThread(this, ali);
        smt.start();

        Intent intent = new Intent(this, History.class);
        startActivity(intent);

    }
}
