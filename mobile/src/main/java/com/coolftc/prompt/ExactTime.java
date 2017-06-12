package com.coolftc.prompt;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import static com.coolftc.prompt.Constants.*;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.View;

import java.util.Calendar;

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
        if(extras != null){
            mTimeStamp = extras.getString(IN_TIMESTAMP);
        }else{
            mTimeStamp = KTime.ParseNow(KTime.KT_fmtDate3339fk).toString();
        }
        if(mTimeStamp != null) {
            mDatePicked = mTimeStamp.substring(0, 10);
            mTimePicked = mTimeStamp.substring(11);
        }

        PageFragmentMgr mPageFragmentMgr = new PageFragmentMgr(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        if(mViewPager != null){ mViewPager.setAdapter(mPageFragmentMgr);}
    }

    @Override
    public void setDate(String date) {
        mDatePicked = date;
    }

    @Override
    public void setTime(String time) {
        mTimePicked = time;
    }

    /*
     *  The navigation buttons for the fragments
     */
    public void PickTime(View view){
        mViewPager.setCurrentItem(FR_POS_EXTIME, true);
    }
    public void PickDate(View view){
        mViewPager.setCurrentItem(FR_POS_EXDATE, true);
    }

    /*
     *  Check the time selected is in the future and then return it.
     */
    public void PickFinish(View view){
        boolean past = false;
        String holdpick = mDatePicked + "T" + mTimePicked;

        try {
            Calendar present = Calendar.getInstance();
            Calendar picked = KTime.ParseToCalendar(holdpick, KTime.KT_fmtDate3339fk);
            past = present.getTimeInMillis() > picked.getTimeInMillis();
        } catch (ExpParseToCalendar expParseToCalendar) { /* just ignore. */ }

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
            if (holdpick.length() > 0) {
                Intent rtn = new Intent(IN_EXACTPICK);
                rtn.putExtra(IN_TIMESTAMP, holdpick);
                setResult(RESULT_OK, rtn);
                finish();
            }
        }
    }


    /* Maybe add these later...
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.exacttime_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    /*
     *  This manages the fragments such that they can be navigated using swipe.
     *  That is to say, that the base class does all that for you.
     */
    public class PageFragmentMgr extends FragmentPagerAdapter {

        private static final int FRAGMENT_COUNT = 2;

        public PageFragmentMgr(FragmentManager fm) { super(fm); }

        @Override
        public Fragment getItem(int position) {
            switch (position){
                case FR_POS_EXDATE:
                    return ExactTimeDate.newInstance(mTimeStamp);
                case FR_POS_EXTIME:
                    return ExactTimeTime.newInstance(mTimeStamp);
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
