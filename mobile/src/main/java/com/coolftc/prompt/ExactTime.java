package com.coolftc.prompt;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import static com.coolftc.prompt.utility.Constants.*;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 *  The top level container screen for picking the exact time and date. Upon
 *  first call, they will just use the current time, but after that it will
 *  be supplied a timestamp. The timestamp is then split into a date / time
 *  and respectfully used to initialize the fragment views that allow
 *  maintenance of the date and time.
 *
 *  When the fragments register a change in the date or time, they feed it
 *  back to this high level manager.  Once a user is finished, this high
 *  level manager checks the data for reasonability and sends it back to
 *  the calling screen.
 */
public class ExactTime extends AppCompatActivity implements FragmentTalkBack {

    private ViewPager mViewPager;
    private String mTimeStamp;
    private String mDatePicked;
    private String mTimePicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exacttime);

        // Initialize default values.
        Bundle extras = getIntent().getExtras();
        DateTimeFormatter zdtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        ZonedDateTime zdt;  // using this to validate the input

        if(extras != null) {
            mTimeStamp = extras.getString(IN_TIMESTAMP);
            try {
                zdt = ZonedDateTime.parse(mTimeStamp, zdtFormat);
            } catch (Exception ex) {
                zdt = ZonedDateTime.now();
                mTimeStamp = zdt.format(zdtFormat);
            }
        } else {
            zdt = ZonedDateTime.now();
            mTimeStamp = zdt.format(zdtFormat);
        }

        if(mTimeStamp != null) {
            mDatePicked = mTimeStamp.substring(0, 10);
            mTimePicked = mTimeStamp.substring(11);
        }

        PageFragmentMgr mPageFragmentMgr = new PageFragmentMgr(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        // At some point consider ViewPager2 see https://developer.android.com/training/animation/vp2-migration
        mViewPager = findViewById(R.id.container);
        if(mViewPager != null){ mViewPager.setAdapter(mPageFragmentMgr); }
    }

    private void setTimeAgain(View view){
        // The Timepicker has a bug with not calling its click listener for AM/PM
        // see https://issuetracker.google.com/issues/36931448
        // As it happens, we can just get the final time on exit and live with that.
        if(view == null) return;
        RelativeLayout holdParent = (RelativeLayout) view.getParent();
        if(holdParent == null) return;
        TimePicker timePicker = holdParent.findViewById(R.id.exactTimePicker);
        int hour; int min;
        if(timePicker != null) {
            hour = timePicker.getCurrentHour();
            min = timePicker.getCurrentMinute();
            setTime((hour < 10 ? "0" + hour : Integer.toString(hour)) + ":" + (min < 10 ? "0" + min : Integer.toString(min)) + mTimeStamp.substring(16));
        }
    }

    @Override
    public void setDate(String date) { mDatePicked = date; }

    @Override
    public void setTime(String time) {
        mTimePicked = time;
    }

    @Override
    public void newInvite(String [] addresses, String display, boolean mirror) { throw new UnsupportedOperationException(); }

    @Override
    public void newSort() { throw new UnsupportedOperationException(); }

    /*
     *  The navigation buttons for the fragments
     */
    public void PickTime(View view){
        setTimeAgain(view);
        mViewPager.setCurrentItem(FR_POS_EXTIME, true);
    }
    public void PickDate(View view){
        mViewPager.setCurrentItem(FR_POS_EXDATE, true);
    }

    /*
     *  Check the time selected is in the future and then return it.
     *  NOTE: The time picker does not ask for seconds, fractions there of, or an offset.
     *  For simplicity, these values are just copied from whatever exists prior
     *  to the time getting picked (i.e. the initial time value).
     */
    public void PickFinish(View view){
        boolean past = false;
        setTimeAgain(view);  // Double check on the date.
        String holdPick = mDatePicked + "T" + mTimePicked;

        try {
            DateTimeFormatter zdtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            ZonedDateTime zdt = ZonedDateTime.parse(holdPick, zdtFormat);
            past = zdt.isBefore(ZonedDateTime.now());
        } catch (Exception ex) { /* just ignore. */ }

        if (past) {
            Dialog problemBox = new AlertDialog.Builder(this)
                    .setTitle(R.string.problemTitle)
                    .setMessage(R.string.problemDatePastAM)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                        }
                    })
                    .create();
            problemBox.show();
        } else {
            Intent rtn = new Intent(IN_EXACTPICK);
            rtn.putExtra(IN_TIMESTAMP, holdPick);
            setResult(RESULT_OK, rtn);
            finish();
        }
    }

    /*
     *  This manages the fragments such that they can be navigated using swipe.
     *  That is to say, that the base class does all that for you.
     */
    public class PageFragmentMgr extends FragmentPagerAdapter {

        private static final int FRAGMENT_COUNT = 2;

        public PageFragmentMgr(FragmentManager fm) { super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT ); }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position){
                case FR_POS_EXDATE:
                    return ExactTimeDate.newInstance(mTimeStamp);
                case FR_POS_EXTIME:
                default:
                    return ExactTimeTime.newInstance(mTimeStamp);
            }
        }

        @Override
        public int getCount() { return FRAGMENT_COUNT; }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case FR_POS_EXDATE:
                    return getResources().getString(R.string.pickdate);
                case FR_POS_EXTIME:
                    return getResources().getString(R.string.picktime);
            }
            return null;
        }
    }
}
