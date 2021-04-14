package com.coolftc.prompt;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.coolftc.prompt.utility.KTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FragmentTalkBack) {
            mActivity = (FragmentTalkBack) context;
        } else {
            throw new RuntimeException(context.toString() + context.getResources().getString(R.string.err_no_fragmenttalkback));
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
            mTimeStamp = KTime.ParseNow(KTime.KT_fmtDate3339fk).toString();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.exacttime_time, container, false);
        TimePicker timePicker = view.findViewById(R.id.exactTimePicker);
        ZonedDateTime zdt;  // using this to validate the input

        try {
            DateTimeFormatter zdtFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
            zdt = ZonedDateTime.parse(mTimeStamp, zdtFormat);
        } catch (Exception ex) {
            zdt = ZonedDateTime.now();
        }
        // Note: Using deprecated timePicker methods while min SDK is lower than the new methods.
        timePicker.setIs24HourView(Settings.getUse24Clock(requireActivity().getApplicationContext()));
        timePicker.setCurrentHour(zdt.toLocalTime().getHour());
        timePicker.setCurrentMinute(zdt.toLocalTime().getMinute());
        timePicker.setOnTimeChangedListener(timeListener);

        return view;
    }

    /*
     *  Listener that receives the selected time.
     *  The formatting is a bit odd, but the picker is just picking two integers,
     *  so to fill out the rest of the time we just steal from the input timestamp.
     */
    private final TimePicker.OnTimeChangedListener timeListener = new TimePicker.OnTimeChangedListener() {
        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            if (view.isShown()) {
                String holdTime = (hourOfDay < 10 ? "0" + Integer.toString(hourOfDay) : Integer.toString(hourOfDay)) + ":" + (minute < 10 ? "0" + minute : Integer.toString(minute)) + mTimeStamp.substring(16);
                if (mActivity != null) {
                    mActivity.setTime(holdTime);
                }
            }
        }
    };

}
