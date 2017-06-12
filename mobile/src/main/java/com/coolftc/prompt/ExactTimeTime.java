package com.coolftc.prompt;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import java.util.Calendar;

/**
 *  This fragment is used to select the time.  It can also
 *  navigate to the date picking fragment.  In either case
 *  the owning activity is appraised of any time change by
 *  the listener event.
 */
public class ExactTimeTime extends Fragment {
    // the fragment initialization parameters.
    private static final String IN_EX_TIME = "ExactTimeTime.fragtime";
    private String mTimeStamp;
    private FragmentTalkBack mActivity;

    // Required empty public constructor
    public ExactTimeTime() { }

    // This is a way to pass data into fragment.
    public static ExactTimeTime newInstance(String ali) {
        ExactTimeTime fragment = new ExactTimeTime();
        Bundle args = new Bundle();
        args.putString(IN_EX_TIME, ali);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentTalkBack) {
            mActivity = (FragmentTalkBack) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement FragmentTalkBack");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTimeStamp = getArguments().getString(IN_EX_TIME);
        } else {
            mTimeStamp = KTime.ParseNow(KTime.KT_fmtDate3339k).toString();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.exacttime_time, container, false);

        TimePicker timePicker = (TimePicker) view.findViewById(R.id.exactTimePicker);
        Calendar timeset;
        try {
            timeset = KTime.ParseToCalendar(mTimeStamp, KTime.KT_fmtDate3339fk);
        } catch (ExpParseToCalendar expParseToCalendar) {
            timeset = Calendar.getInstance();
        }
        // Note: Using deprecated timePicker methods while min SDK is lower than the new methods.
        int holdTm = Integer.parseInt(android.text.format.DateFormat.format("HH",timeset).toString());
        timePicker.setCurrentHour(holdTm);
        holdTm = Integer.parseInt(android.text.format.DateFormat.format("mm",timeset).toString());
        timePicker.setCurrentMinute(holdTm);
        timePicker.setOnTimeChangedListener(timeListener);

        return view;
    }

    /*
     *  Listener that receives the selected time.
     *  The formatting is a bit odd, but the picker is just picking two integers,
     *  so to fill out the rest of the time we just steal from the input timestamp.
     */
    private TimePicker.OnTimeChangedListener timeListener = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            if (view.isShown()) {
                Integer hour = hourOfDay;
                Integer min = minute;
                String holdTime = (hour < 10 ? "0" + hour.toString() : hour.toString()) + ":" + (min < 10 ? "0" + min.toString() : min.toString()) + mTimeStamp.substring(16);
                if (mActivity != null) {
                    mActivity.setTime(holdTime);
                }
            }
        }
    };

}
