package com.coolftc.prompt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import static com.coolftc.prompt.utility.Constants.*;

/**
 *  This provides a simple popup that allows the user to select the phone numbers and/or
    email addresses to which they wish to send invites.  Since a person might sign up on either
    email or phone number, it is safest to send invites to all (which will be the default).
    NOTE: The Mirror is a complicated, little used option, so it has been excluded from here.
 */
public class ContactPickerDialog extends DialogFragment {
    private FragmentTalkBack mActivity;
    private String [] mAddresses;
    private String [] mLabels;
    private boolean [] mSelected;
    private String mDisplayName;

    // Use this to initialize the displayed data.
    public void setInvites(String display, String [] contacts, String [] labels){
        mDisplayName = display;
        mAddresses = contacts;
        mLabels = new String[labels.length];
        for(int i = 0; i < contacts.length; ++i) mLabels[i] = mAddresses[i] + "  " + labels[i];
        mSelected = new boolean[mAddresses.length];
        for(int i = 0; i < mSelected.length; ++i) { mSelected[i] = true; }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // When saved data is also passed in normally, it needs to be restored here.
        if (savedInstanceState != null) {
            mAddresses = savedInstanceState.getStringArray(IN_ADDRESSES);
            mLabels = savedInstanceState.getStringArray(IN_LABELS);
            mSelected = savedInstanceState.getBooleanArray(IN_ADDRESSES_TRUE);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.ctp_Addresses)
                .setMultiChoiceItems(mLabels, mSelected, new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        mSelected[which] = isChecked;
                    }
                })
                .setPositiveButton(R.string.invite, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Blank out any addresses that are not selected.
                        for(int i = 0; i < mSelected.length; ++i) { if (!mSelected[i]) { mAddresses[i] = ""; } }
                        mActivity.newInvite(mAddresses, mDisplayName, false);
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.dismiss, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /*
     *  Since this is a data entry screen, with some data collection in dialogs,
     *  we need to persist that extra data in the case of Activity resets.  Make
     *  sure to call the super as the last thing done.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putCharSequenceArray(IN_ADDRESSES, mAddresses);
        outState.putCharSequenceArray(IN_LABELS, mLabels);
        outState.putBooleanArray(IN_ADDRESSES_TRUE, mSelected);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Context context) {
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
}
