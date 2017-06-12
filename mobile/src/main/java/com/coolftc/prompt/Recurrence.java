package com.coolftc.prompt;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import java.util.Calendar;
import java.util.Locale;

import static com.coolftc.prompt.Constants.*;

/**
 *  When someone wants a prompt to propagate at a periodic
    rate for a certain length of time.  This is handled by
    encoding information about the recurrence pattern in
    the note. Specifically the note contains Units, Period
    and End Date.
    This screen is used to allow entry of such data.
 */
public class Recurrence extends AppCompatActivity {

    private Integer recurUnit = RECUR_INVALID;      // The units used for recurrence.
    private Integer recurPeriod = RECUR_INVALID;    // The period used for recurrence.
    private String recurEnd;                        // The end date used for recurrence.
    private Integer recurNbr = RECUR_INVALID;       // The number of times to recur.
    private String display = "";                    // The deliver time reference

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recurrence);

        // Initialize default values.
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            recurUnit = extras.getInt(IN_UNIT);
            recurPeriod = extras.getInt(IN_PERIOD);
            recurEnd = extras.getString(IN_ENDTIME);
            recurNbr = extras.getInt(IN_ENDNBR);
            display = extras.getString(IN_DISP_TIME);
        }else{
            recurUnit = RECUR_UNIT_DEFAULT;
            recurPeriod = RECUR_PERIOD_DEFAULT;
            recurEnd = RECUR_END_DEFAULT;
            recurNbr = RECUR_END_NBR;
            // if display is not passed in, just don't show anything.
        }

        // Initialize sets up the local controls, so they don't have to be monitored so closely.
        Initialize();
        ShowDetails();
    }

    /*
     *  These methods switch between the recurrence types (day/day of week/month)
     */
    public void ShowDailyMenu(View view) {
        recurUnit = UNIT_TYPE_DAY;
        ShowDetails();
    }
    public void ShowWeeklyMenu(View view) {
        recurUnit = UNIT_TYPE_WEEKDAY;
        ShowDetails();
    }
    public void ShowMonthlyMenu(View view) {
        recurUnit = UNIT_TYPE_MONTH;
        ShowDetails();
    }

    /*
     *  We need to manually set the radio buttons, since we cannot used the
     *  linear view that does it automatically.  Simple task as all we have
     *  to really do is unclick the other ones.
     */
    public void RdoOccurOn(View view) {
        RadioButton holdRdo;
        holdRdo = (RadioButton) findViewById(R.id.reptPickDay);
        if(holdRdo!=null) { holdRdo.setChecked(false); }
        holdRdo = (RadioButton) findViewById(R.id.reptForever);
        if(holdRdo!=null) { holdRdo.setChecked(false); }
    }
    public void RdoForeverOn(View view) {
        RadioButton holdRdo;
        holdRdo = (RadioButton) findViewById(R.id.reptOccur);
        if(holdRdo!=null) { holdRdo.setChecked(false); }
        holdRdo = (RadioButton) findViewById(R.id.reptPickDay);
        if(holdRdo!=null) { holdRdo.setChecked(false); }

    }
    public void PickEndDate(View view) {
        RadioButton holdRdo;
        holdRdo = (RadioButton) findViewById(R.id.reptOccur);
        if(holdRdo!=null) { holdRdo.setChecked(false); }
        holdRdo = (RadioButton) findViewById(R.id.reptForever);
        if(holdRdo!=null) { holdRdo.setChecked(false); }

        // The date picker returns in onActivityResult().
        Intent timestamp = new Intent(this, ExactTime.class);
        timestamp.putExtra(IN_TIMESTAMP, KTime.ParseNow(KTime.KT_fmtDate3339k));
        startActivityForResult(timestamp, KY_DATETIME);
    }

    /*
     *  First go out and get the data entered, then pack in the extras and ship it back.
     */
    public void PickFinishRecur(View view) {
        int problem = 0;
        String holdInt = "";
        boolean testRdo = false;
        RadioButton holdRdo;
        EditText holdEdit;

        //  Get/Check the repeating info
        Intent rtn = new Intent();
        rtn.putExtra(IN_UNIT, recurUnit);
        if(recurUnit == UNIT_TYPE_WEEKDAY) {
            recurPeriod = DayOfWeek();
            if(recurPeriod == 0) problem = R.string.problemDayWeek;
        } else {
            holdEdit = (EditText) findViewById(R.id.reptPeriod);
            if(holdEdit != null) { holdInt = holdEdit.getText().toString(); }
            try { recurPeriod = Integer.parseInt(holdInt);}
            catch (Exception exp) { recurPeriod = 0; }
            if (recurPeriod == 0) problem = R.string.problemOccur;
        }
        rtn.putExtra(IN_PERIOD, recurPeriod);

        // Get/Check the end info
        holdRdo = (RadioButton) findViewById(R.id.reptOccur);
        if(holdRdo!=null) { testRdo = holdRdo.isChecked(); }
        if (testRdo) { // End after # occurrences
            holdEdit = (EditText) findViewById(R.id.reptOccurNbr);
            if(holdEdit != null) { holdInt = holdEdit.getText().toString(); }
            try { recurNbr = Integer.parseInt(holdInt);}
            catch (Exception exp) { recurNbr = 0; }
            rtn.putExtra(IN_ENDNBR, recurNbr);
            rtn.putExtra(IN_ENDTIME, "");
            if (recurNbr == 0) problem = R.string.problemEndRepeat;
        }
        holdRdo = (RadioButton) findViewById(R.id.reptForever);
        if(holdRdo!=null) { testRdo = holdRdo.isChecked(); }
        if (testRdo) { // Never end
            rtn.putExtra(IN_ENDNBR, 0);
            Calendar future = Calendar.getInstance();
            future.add(Calendar.YEAR, FOREVER_MORE);
            rtn.putExtra(IN_ENDTIME, DateFormat.format(KTime.KT_fmtDate3339k, future));
        }
        holdRdo = (RadioButton) findViewById(R.id.reptPickDay);
        if(holdRdo!=null) { testRdo = holdRdo.isChecked(); }
        if (testRdo) { // End at a specific date
            rtn.putExtra(IN_ENDNBR, 0);
            rtn.putExtra(IN_ENDTIME, recurEnd);
            if (recurEnd.length() == 0) problem = R.string.problemEndDate;
        }

        // If some data not valid, let them know, otherwise send them back to main screen.
        if (problem > 0){
            Dialog problemBox = new AlertDialog.Builder(this)
                    .setTitle(R.string.problemTitle)
                    .setMessage(problem)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .create();
            problemBox.show();
        } else {
            setResult(RESULT_OK, rtn);
            finish();
        }
    }
    public void PickCancelRecur(View view) {
        finish();
    }

    /* This method processes any Activity responses.  Generally this is when some page has been navigated to,
     * and upon its return, if it needs to send back data, it will exercise this call back. */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case KY_DATETIME:     // Returning from the datetime picker.
                TextView holdText;
                if (resultCode == RESULT_OK) {
                    recurEnd = data.getExtras().getString(IN_TIMESTAMP);
                    holdText = (TextView) findViewById(R.id.reptEndDate);
                    if(holdText != null) holdText.setText(recurEnd);
                }
                break;
        }
    }

    /*
     *  First see if any of the day of the week checkboxes are checked.
     *  Then find out which radio button for the end is the best choice.
     *  This method should only be called when the screen is first displayed.
     */
    private void Initialize() {
        CheckBox holdChkb;
        RadioButton holdRdo;

        if (recurUnit==UNIT_TYPE_WEEKDAY){
            holdChkb = (CheckBox) findViewById(R.id.reptSun);
            if(holdChkb != null) holdChkb.setChecked((recurPeriod & SUN_FLAG) == SUN_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptMon);
            if(holdChkb != null) holdChkb.setChecked((recurPeriod & MON_FLAG) == MON_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptTue);
            if(holdChkb != null) holdChkb.setChecked((recurPeriod & TUE_FLAG) == TUE_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptWed);
            if(holdChkb != null) holdChkb.setChecked((recurPeriod & WED_FLAG) == WED_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptThr);
            if(holdChkb != null) holdChkb.setChecked((recurPeriod & THU_FLAG) == THU_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptFri);
            if(holdChkb != null) holdChkb.setChecked((recurPeriod & FRI_FLAG) == FRI_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptSat);
            if(holdChkb != null) holdChkb.setChecked((recurPeriod & SAT_FLAG) == SAT_FLAG);
        }

        // The explicit retry number has priority.
        if (recurNbr > 0 || recurNbr < 0){
            holdRdo = (RadioButton) findViewById(R.id.reptOccur);
            if(holdRdo!=null) { holdRdo.setChecked(true); }
        }
        if (recurNbr == 0){
            try {
                long years = KTime.CalcDateDifference(recurEnd, KTime.ParseNow(KTime.KT_fmtDate3339k).toString(), KTime.KT_fmtDate3339k, KTime.KT_YEARS);
                if (years < FOREVER_LESS) {
                    holdRdo = (RadioButton) findViewById(R.id.reptPickDay);
                    if(holdRdo!=null) { holdRdo.setChecked(true); }
                } else {
                    holdRdo = (RadioButton) findViewById(R.id.reptForever);
                    if(holdRdo!=null) { holdRdo.setChecked(true); }
                }
            } catch (Exception exp) {
                // Just don't pick the first button
                holdRdo = (RadioButton) findViewById(R.id.reptOccur);
                if(holdRdo!=null) { holdRdo.setChecked(true); }
            }
        }
    }

    /*
     *  This is mostly for when the type of recurrence changes (day/week/month).
     */
    private void ShowDetails() {
        TextView holdText;
        EditText holdEdit;

        holdText = (TextView) findViewById(R.id.reptDelivery);
        if(holdText!=null) { holdText.setText(display); }

        HideAllDays();

        switch (recurUnit){
            case UNIT_TYPE_DAY:
                StandardPeriod(R.string.day_after);
                break;
            case UNIT_TYPE_WEEKDAY:
                ShowAllDays();
                break;
            case UNIT_TYPE_MONTH:
                StandardPeriod(R.string.month_after);
                break;
            default:
                break;
        }

        holdEdit = (EditText) findViewById(R.id.reptOccurNbr);
        if(holdEdit!=null) { holdEdit.setText(String.format(Locale.getDefault(),"%d", recurNbr)); }
        holdText = (TextView) findViewById(R.id.reptEndDate);
        if(recurNbr == 0 && recurEnd.length() > 0) {
            long years;
            try {
                years = KTime.CalcDateDifference(recurEnd, KTime.ParseNow(KTime.KT_fmtDate3339k).toString(), KTime.KT_fmtDate3339k, KTime.KT_YEARS);
            } catch (Exception exp) {
                years = 0;
            }
            if (years < FOREVER_LESS) {
                if(holdText!=null) { holdText.setText(recurEnd); }
            } else {
                if(holdText!=null) { holdText.setText(R.string.taptochoose); }
            }
        }else{
            if(holdText!=null) { holdText.setText(R.string.taptochoose); }
        }
    }

    /*
     *  When the day of week is selected, this will make them all visible.
     */
    private void ShowAllDays() {
        CheckBox holdChkb;

        holdChkb = (CheckBox) findViewById(R.id.reptSun);
        if(holdChkb != null) { holdChkb.setVisibility(View.VISIBLE); }
        holdChkb = (CheckBox) findViewById(R.id.reptMon);
        if(holdChkb != null) { holdChkb.setVisibility(View.VISIBLE); }
        holdChkb = (CheckBox) findViewById(R.id.reptTue);
        if(holdChkb != null) { holdChkb.setVisibility(View.VISIBLE); }
        holdChkb = (CheckBox) findViewById(R.id.reptWed);
        if(holdChkb != null) { holdChkb.setVisibility(View.VISIBLE); }
        holdChkb = (CheckBox) findViewById(R.id.reptThr);
        if(holdChkb != null) { holdChkb.setVisibility(View.VISIBLE); }
        holdChkb = (CheckBox) findViewById(R.id.reptFri);
        if(holdChkb != null) { holdChkb.setVisibility(View.VISIBLE); }
        holdChkb = (CheckBox) findViewById(R.id.reptSat);
        if(holdChkb != null) { holdChkb.setVisibility(View.VISIBLE); }
    }

    /*
     *  The standard recurring periods of days or months can are made visible here.
     */
    private void StandardPeriod(int desc) {
        TextView holdText;
        EditText holdEdit;

        holdText = (TextView) findViewById(R.id.reptEvery);
        if(holdText != null) holdText.setVisibility(View.VISIBLE);
        holdText = (TextView) findViewById(R.id.reptEveryTime);
        if(holdText != null) {
            holdText.setText(getResources().getString(desc));
            holdText.setVisibility(View.VISIBLE);
        }
        holdEdit = (EditText) findViewById(R.id.reptPeriod);
        if(holdEdit != null) {
            if(holdEdit.getText().length()==0) { holdEdit.setText(String.format(Locale.getDefault(),"%d",recurPeriod)); }
            holdEdit.setVisibility(View.VISIBLE);
        }
    }

    /*
     *  When switching between the types of recurrence, first hide everything.
     */
    private void HideAllDays() {
        TextView holdText;
        EditText holdEdit;
        CheckBox holdChkb;

        holdText = (TextView) findViewById(R.id.reptEvery);
        if(holdText != null) holdText.setVisibility(View.GONE);
        holdText = (TextView) findViewById(R.id.reptEveryTime);
        if(holdText != null) holdText.setVisibility(View.GONE);

        holdEdit = (EditText) findViewById(R.id.reptPeriod);
        if(holdEdit != null) holdEdit.setVisibility(View.GONE);

        holdChkb = (CheckBox) findViewById(R.id.reptSun);
        if(holdChkb != null) holdChkb.setVisibility(View.GONE);
        holdChkb = (CheckBox) findViewById(R.id.reptMon);
        if(holdChkb != null) holdChkb.setVisibility(View.GONE);
        holdChkb = (CheckBox) findViewById(R.id.reptTue);
        if(holdChkb != null) holdChkb.setVisibility(View.GONE);
        holdChkb = (CheckBox) findViewById(R.id.reptWed);
        if(holdChkb != null) holdChkb.setVisibility(View.GONE);
        holdChkb = (CheckBox) findViewById(R.id.reptThr);
        if(holdChkb != null) holdChkb.setVisibility(View.GONE);
        holdChkb = (CheckBox) findViewById(R.id.reptFri);
        if(holdChkb != null) holdChkb.setVisibility(View.GONE);
        holdChkb = (CheckBox) findViewById(R.id.reptSat);
        if(holdChkb != null) holdChkb.setVisibility(View.GONE);
    }

    private int DayOfWeek() {
        CheckBox holdChkb;
        int days = 0;

        holdChkb = (CheckBox) findViewById(R.id.reptSun);
        if(holdChkb != null && holdChkb.isChecked()) { days += SUN_FLAG; }
        holdChkb = (CheckBox) findViewById(R.id.reptMon);
        if(holdChkb != null && holdChkb.isChecked()) { days += MON_FLAG; }
        holdChkb = (CheckBox) findViewById(R.id.reptTue);
        if(holdChkb != null && holdChkb.isChecked()) { days += TUE_FLAG; }
        holdChkb = (CheckBox) findViewById(R.id.reptWed);
        if(holdChkb != null && holdChkb.isChecked()) { days += WED_FLAG; }
        holdChkb = (CheckBox) findViewById(R.id.reptThr);
        if(holdChkb != null && holdChkb.isChecked()) { days += THU_FLAG; }
        holdChkb = (CheckBox) findViewById(R.id.reptFri);
        if(holdChkb != null && holdChkb.isChecked()) { days += FRI_FLAG; }
        holdChkb = (CheckBox) findViewById(R.id.reptSat);
        if(holdChkb != null && holdChkb.isChecked()) { days += SAT_FLAG; }

        return days;
    }
}
