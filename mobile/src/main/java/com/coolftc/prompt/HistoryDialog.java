package com.coolftc.prompt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 *  This provides a simple popup that allows the user to select the order of display for Prompts
    in the History screen.  The sort order is actually a setting, but it seemed better to offer
    maintenance closer to the actual list.
 */
public class HistoryDialog extends DialogFragment {
    private FragmentTalkBack mActivity;
    private int mSortBy = 0;
    private boolean mCompromised = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mSortBy = Settings.getPromptSortOrder(getActivity().getApplicationContext());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.his_SortOrder)
                .setSingleChoiceItems(R.array.sortByTime, mSortBy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSortBy = which;
                        mCompromised = true;
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Save the new configuration if it changed, and let Activity know.
                        if(mCompromised) {
                            Settings.setPromptSortOrder(getActivity().getApplicationContext(), mSortBy);
                            mActivity.newSort();
                        }
                        dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
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
