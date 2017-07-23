package com.coolftc.prompt;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;

import com.coolftc.prompt.utility.ExpParseToCalendar;
import com.coolftc.prompt.utility.KTime;

import java.util.Calendar;

/**
 *  This fragment is used to select the date.  It can also
 *  navigate to the time picking fragment.  In either case
 *  the owning activity is appraised of any date change by
 *  the listener event.
 */
public class ExactTimeDate extends Fragment {
    // Fragment initialization parameters.
    private static final String IN_EX_DATE = "ExactTimeDate.fragdate";
    private String mTimeStamp;
    private FragmentTalkBack mActivity;

    // Required empty public constructor
    public ExactTimeDate() { }

    // This is a way to pass data into fragment.
    public static ExactTimeDate newInstance(String ali) {
        ExactTimeDate fragment = new ExactTimeDate();
        Bundle bundle = new Bundle();
        bundle.putString(IN_EX_DATE, ali);
        fragment.setArguments(bundle);
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
            mTimeStamp = getArguments().getString(IN_EX_DATE);
        } else {
            mTimeStamp = KTime.ParseNow(KTime.KT_fmtDate3339fk).toString();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.exacttime_date, container, false);

        CalendarView datePicker = (CalendarView) view.findViewById(R.id.exactCalendar);
        Calendar timeset;
        try {
            timeset = KTime.ParseToCalendar(mTimeStamp, KTime.KT_fmtDate3339fk);
        } catch (ExpParseToCalendar expParseToCalendar) {
            timeset = Calendar.getInstance();
        }
        // Grab today's day as the minimum to avoid times in the past.
        datePicker.setMinDate(Calendar.getInstance().getTimeInMillis()-1);
        datePicker.setDate(timeset.getTimeInMillis());
        datePicker.setOnDateChangeListener(dateListener);

        return view;
    }

    ///listener that receives the selected date
    private CalendarView.OnDateChangeListener dateListener = new CalendarView.OnDateChangeListener() {
        @Override
        public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
            if (view.isShown()) {
                Integer day = dayOfMonth;
                Integer mon = month + 1;
                String holdDate = year + "-" + (mon < 10 ? "0" + mon.toString() : mon.toString()) + "-" + (day < 10 ? "0" + day.toString() : day.toString());
                if (mActivity != null) {
                    mActivity.setDate(holdDate);
                }
            }
        }
    };


}
