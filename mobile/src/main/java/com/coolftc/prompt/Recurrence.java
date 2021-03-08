package com.coolftc.prompt;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.coolftc.prompt.utility.ExpParseToCalendar;
import com.coolftc.prompt.utility.KTime;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static com.coolftc.prompt.utility.Constants.*;

/**
 *  When someone wants a prompt to propagate at a periodic
    rate for a certain length of time.  This is handled by
    encoding information about the recurrence pattern in
    the note. Specifically the note contains Units, Period
    and End Date.
    This screen is used to allow entry of such data.
 */
public class Recurrence extends AppCompatActivity {

    private Integer mRecurUnit = RECUR_INVALID;     // The units used for recurrence, e.g. Daily, Monthly.
    private Integer mRecurPeriod = RECUR_INVALID;   // The period used for recurrence, e.g. Every N units.
    private String mRecurEnd;                       // The end date used for recurrence.
    private Integer mRecurNbr = RECUR_INVALID;      // The number of times to recur, if there is not an end date, it can be a number of periods.
    private String mDisplay = "";                   // The deliver time reference
    private boolean mFirstTimeWeeks = true;         // True until the first time click on weeks

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recurrence);

        // Initialize default values.
        Bundle extras = getIntent().getExtras();
        // When saved data is also passed in normally, it needs to be restored here.
        if (savedInstanceState != null) {
            mDisplay = savedInstanceState.getString(IN_DISP_TIME);
            mRecurUnit = savedInstanceState.getInt(IN_UNIT);
            mRecurPeriod = savedInstanceState.getInt(IN_PERIOD);
            mRecurNbr = savedInstanceState.getInt(IN_ENDNBR);
            mRecurEnd = savedInstanceState.getString(IN_ENDTIME);
            mFirstTimeWeeks = savedInstanceState.getBoolean(IN_FIRSTDOW);
        }else {
            if (extras != null) {
                mRecurUnit = extras.getInt(IN_UNIT);
                mRecurPeriod = extras.getInt(IN_PERIOD);
                mRecurEnd = extras.getString(IN_ENDTIME);
                mRecurNbr = extras.getInt(IN_ENDNBR);
                mDisplay = extras.getString(IN_DISP_TIME);
            } else {
                mRecurUnit = RECUR_UNIT_DEFAULT;
                mRecurPeriod = RECUR_PERIOD_DEFAULT;
                mRecurEnd = RECUR_END_DEFAULT;
                mRecurNbr = RECUR_END_NBR;
                // if mDisplay is not passed in, just don't show anything.
            }
        }

        // Initialize sets up the local controls, so they don't have to be monitored so closely.
        Initialize();
        ShowDetails();
    }

    /*
     *  Since this is a data entry screen, with some data collection in dialogs,
     *  we need to persist that extra data in the case of Activity resets.  Make
     *  sure to call the super as the last thing done.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mDisplay == null) mDisplay = "";
        outState.putString(IN_DISP_TIME, mDisplay);
        outState.putInt(IN_UNIT, mRecurUnit);
        outState.putInt(IN_PERIOD, mRecurPeriod);
        outState.putInt(IN_ENDNBR, mRecurNbr);
        if (mRecurEnd == null) mRecurEnd = "";
        outState.putString(IN_ENDTIME, mRecurEnd);
        outState.putBoolean(IN_FIRSTDOW, mFirstTimeWeeks);
        super.onSaveInstanceState(outState);
    }
    /*
     *  Restore the state.  See onSaveInstanceState().
     *  NOTE: For items that might be provided at startup, need to restore them
     *  in the OnCreate().
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /*
     *  First see if any of the day of the week checkboxes are checked.
     *  Then find out which radio button for the end is the best choice.
     *  This method should only be called when the screen is first displayed.
     */
    private void Initialize() {
        CheckBox holdChkb;
        RadioButton holdRdo;

        // The first time in, we want to initialize the days of the week to today's day of the week.
        // When not the first time in, the existing checkboxes should be correct.
        if(mFirstTimeWeeks) {
            int initDays;
            if (mRecurUnit == UNIT_TYPE_WEEKDAY) {
                initDays = mRecurPeriod;
            } else {
                Calendar holdC = Calendar.getInstance();
                double day = holdC.get(Calendar.DAY_OF_WEEK);
                initDays = (int)Math.pow(2.0, day-1);
            }
            mFirstTimeWeeks = false;
            holdChkb = (CheckBox) findViewById(R.id.reptSun);
            if (holdChkb != null) holdChkb.setChecked((initDays & SUN_FLAG) == SUN_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptMon);
            if (holdChkb != null) holdChkb.setChecked((initDays & MON_FLAG) == MON_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptTue);
            if (holdChkb != null) holdChkb.setChecked((initDays & TUE_FLAG) == TUE_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptWed);
            if (holdChkb != null) holdChkb.setChecked((initDays & WED_FLAG) == WED_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptThr);
            if (holdChkb != null) holdChkb.setChecked((initDays & THU_FLAG) == THU_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptFri);
            if (holdChkb != null) holdChkb.setChecked((initDays & FRI_FLAG) == FRI_FLAG);
            holdChkb = (CheckBox) findViewById(R.id.reptSat);
            if (holdChkb != null) holdChkb.setChecked((initDays & SAT_FLAG) == SAT_FLAG);
        }

        // Expiration
        // The explicit retry number has priority.
        if (mRecurNbr > 0) {
            holdRdo = (RadioButton) findViewById(R.id.reptOccur);
            if (holdRdo != null) {
                holdRdo.setChecked(true);
            }
        } else {
            try {
                long years = KTime.CalcDateDifference(mRecurEnd, KTime.ParseNow(KTime.KT_fmtDate3339fk).toString(), KTime.KT_fmtDate3339fk, KTime.KT_YEARS);
                if (years < FOREVER_LESS) {
                    holdRdo = (RadioButton) findViewById(R.id.reptPickDay);
                    if (holdRdo != null) {
                        holdRdo.setChecked(true);
                    }
                } else {
                    holdRdo = (RadioButton) findViewById(R.id.reptForever);
                    if (holdRdo != null) {
                        holdRdo.setChecked(true);
                    }
                }
            } catch (Exception exp) {
                // Just pick the explicit retry
                holdRdo = (RadioButton) findViewById(R.id.reptOccur);
                if (holdRdo != null) {
                    holdRdo.setChecked(true);
                }
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
        if(holdText!=null) { holdText.setText(mDisplay); }

        HideAllDays();

        switch (mRecurUnit){
            case UNIT_TYPE_DAY:
                SelectButton(R.id.reptBtnDaily);
                StandardPeriod(R.string.day_after);
                break;
            case UNIT_TYPE_WEEKDAY:
                SelectButton(R.id.reptBtnWeekly);
                ShowAllDays();
                break;
            case UNIT_TYPE_MONTH:
                SelectButton(R.id.reptBtnMonthly);
                StandardPeriod(R.string.month_after);
                break;
            default:
                break;
        }

        holdEdit = (EditText) findViewById(R.id.reptPeriod);
        if(holdEdit != null && mRecurPeriod > 0) { holdEdit.setText(String.format(Locale.getDefault(), "%d", mRecurPeriod)); }

        holdEdit = (EditText) findViewById(R.id.reptOccurNbr);
        if(holdEdit != null && mRecurNbr > 0) { holdEdit.setText(String.format(Locale.getDefault(), "%d", mRecurNbr)); }

        holdText = (TextView) findViewById(R.id.reptEndDate);
        if(mRecurNbr <= 0 && mRecurEnd.length() > 0) {
            long years;
            try {
                years = KTime.CalcDateDifference(mRecurEnd, KTime.ParseNow(KTime.KT_fmtDate3339fk).toString(), KTime.KT_fmtDate3339fk, KTime.KT_YEARS);
            } catch (Exception exp) {
                years = 0;
            }
            if (years < FOREVER_LESS) {
                if(holdText!=null) { holdText.setText(GetRecurringTime(mRecurEnd)); }
            } else {
                if(holdText!=null) { holdText.setText(R.string.taptochoose); }
            }
        }else{
            if(holdText!=null) { holdText.setText(R.string.taptochoose); }
        }
    }

    /*
     *  When switching between the types of recurrence, first hide everything.
     *  This also resets the Buttons to be all unselected.
     */
    private void HideAllDays() {
        RelativeLayout holdDayMonth;
        Button control;

        holdDayMonth = (RelativeLayout) findViewById(R.id.grpDayMonth);
        if(holdDayMonth != null) holdDayMonth.setVisibility(View.INVISIBLE);
        holdDayMonth = (RelativeLayout) findViewById(R.id.grpWeekly);
        if(holdDayMonth != null) holdDayMonth.setVisibility(View.INVISIBLE);

        control = (Button) findViewById(R.id.reptBtnDaily);
        if(control != null){
            control.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.promptblack));
            control.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mpromptcomplt));
        }
        control = (Button) findViewById(R.id.reptBtnWeekly);
        if(control != null){
            control.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.promptblack));
            control.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mpromptcomplt));
        }
        control = (Button) findViewById(R.id.reptBtnMonthly);
        if(control != null){
            control.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.promptblack));
            control.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mpromptcomplt));
        }
    }

    private void SelectButton(int id){
        Button control;

        control = (Button) findViewById(id);
        if(control != null){
            control.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.promptwhite));
            control.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.mpromptcompdk));
        }
    }

    /*
     *  The standard recurring periods of days or months can are made visible here.
     */
    private void StandardPeriod(int desc) {
        TextView holdText;
        RelativeLayout holdDayMonth;

        holdDayMonth = (RelativeLayout) findViewById(R.id.grpDayMonth);
        if(holdDayMonth != null) holdDayMonth.setVisibility(View.VISIBLE);

        holdText = (TextView) findViewById(R.id.reptEveryTime);
        if(holdText != null) {
            holdText.setText(getResources().getString(desc));
        }
    }

    /*
     *  When the day of week is selected, this will make them all visible.
     */
    private void ShowAllDays() {
        RelativeLayout holdDayMonth;

        holdDayMonth = (RelativeLayout) findViewById(R.id.grpWeekly);
        if(holdDayMonth != null) holdDayMonth.setVisibility(View.VISIBLE);
    }


    /*
     *  These methods switch between the recurrence types (day/day of week/month)
     */
    public void ShowDailyMenu(View view) {
        mRecurUnit = UNIT_TYPE_DAY;
        ShowDetails();
    }
    public void ShowWeeklyMenu(View view) {
        mRecurUnit = UNIT_TYPE_WEEKDAY;
        ShowDetails();
    }
    public void ShowMonthlyMenu(View view) {
        mRecurUnit = UNIT_TYPE_MONTH;
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
        if(mRecurEnd.equalsIgnoreCase(RECUR_END_DEFAULT) || Reminder.IsForever(mRecurEnd))
            timestamp.putExtra(IN_TIMESTAMP, KTime.ParseNow(KTime.KT_fmtDate3339fk));
        else
            timestamp.putExtra(IN_TIMESTAMP, mRecurEnd);
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
        rtn.putExtra(IN_UNIT, mRecurUnit);
        if(mRecurUnit == UNIT_TYPE_WEEKDAY) {
            mRecurPeriod = DayOfWeek();
            if(mRecurPeriod == 0) problem = R.string.problemDayWeek;
        } else {
            holdEdit = (EditText) findViewById(R.id.reptPeriod);
            if(holdEdit != null) { holdInt = holdEdit.getText().toString(); }
            try { mRecurPeriod = Integer.parseInt(holdInt);}
            catch (Exception exp) { mRecurPeriod = 0; }
            if (mRecurPeriod == 0) problem = R.string.problemOccur;
        }
        rtn.putExtra(IN_PERIOD, mRecurPeriod);

        // Get/Check the end info
        holdRdo = (RadioButton) findViewById(R.id.reptOccur);
        if(holdRdo!=null) { testRdo = holdRdo.isChecked(); }
        if (testRdo) { // End after # occurrences
            holdEdit = (EditText) findViewById(R.id.reptOccurNbr);
            if(holdEdit != null) { holdInt = holdEdit.getText().toString(); }
            try { mRecurNbr = Integer.parseInt(holdInt);}
            catch (Exception exp) { mRecurNbr = 0; }
            rtn.putExtra(IN_ENDNBR, mRecurNbr);
            rtn.putExtra(IN_ENDTIME, "");
            if (mRecurNbr == 0) problem = R.string.problemEndRepeat;
        }
        holdRdo = (RadioButton) findViewById(R.id.reptForever);
        if(holdRdo!=null) { testRdo = holdRdo.isChecked(); }
        if (testRdo) { // Never end
            rtn.putExtra(IN_ENDNBR, 0);
            Calendar future = KTime.ConvertTimezone(Calendar.getInstance(), KTime.UTC_TIMEZONE);
            future.add(Calendar.YEAR, FOREVER_MORE);
            CharSequence reFormatted = DateFormat.format(KTime.KT_fmtDate3339k, future);
            try { // Need to reformat this as DateFormat does not support fractional seconds.
                rtn.putExtra(IN_ENDTIME, KTime.ParseToFormat(reFormatted.toString(), KTime.KT_fmtDate3339k, KTime.UTC_TIMEZONE, KTime.KT_fmtDate3339fk));
            } catch (ExpParseToCalendar expParseToCalendar) {
                rtn.putExtra(IN_ENDTIME, KTime.ParseNow(KTime.KT_fmtDate3339fk));
            }
        }
        holdRdo = (RadioButton) findViewById(R.id.reptPickDay);
        if(holdRdo!=null) { testRdo = holdRdo.isChecked(); }
        if (testRdo) { // End at a specific date
            rtn.putExtra(IN_ENDNBR, 0);
            rtn.putExtra(IN_ENDTIME, mRecurEnd);
            if (mRecurEnd.length() == 0) problem = R.string.problemEndDate;
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

    /*  This method processes any Activity responses.  Generally this is when some page has been
     *  navigated to, and upon its return, if it needs to send back data, it will exercise this
     *  call back.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case KY_DATETIME:     // Returning from the datetime picker.
                TextView holdText;
                if (resultCode == RESULT_OK) {
                    mRecurEnd = data.getExtras().getString(IN_TIMESTAMP);
                    holdText = (TextView) findViewById(R.id.reptEndDate);
                    if (holdText != null) holdText.setText(GetRecurringTime(mRecurEnd));
                }
                break;
        }
    }

    /*
     *  Formats time in an expected way for this application.  We reuse the formatter in the
     *  Reminder class.
     */
    private String GetRecurringTime(String inTime) {
        Reminder holdData = new Reminder();
        holdData.recurEnd = inTime;
        return holdData.GetRecurringTime(getApplicationContext());
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
