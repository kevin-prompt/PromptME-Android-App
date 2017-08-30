package com.coolftc.prompt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.coolftc.prompt.service.CancelFriendThread;
import com.coolftc.prompt.service.SendInviteThread;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import static com.coolftc.prompt.utility.Constants.*;


/**
 *  The Invite screen is for those occasions...
         when the contact is not readily available in the contact list
    -or- when the Mirror attribute needs to be part of the invitation process
    -or- when a user wants to manually reply to a friend request.

 *  Primarily, the idea that a person may want to send an invitation to someone
    who is not in their contacts.  This screen provides a place for manual input
    of either an email address or phone number.

 *  The reason this screen is require for creating/confirming a Mirror is that such activity
    should be uncommon and is hard to explain, so it is buried in this more explicit
    invitation path.  Also, we want to make harder to create/accept a Mirror because if
    that is done inadvertently, the behavior would be unexpected.

 *  Finally, manual acceptance (or general rejection) of a connection request is
    managed with this screen.
 */
public class Invite extends AppCompatActivity {

    private Account mAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        // When saved data is also passed in normally, it needs to be restored here.
        if (savedInstanceState != null) {
            mAccount = (Account) savedInstanceState.getSerializable(IN_DSPL_TGT);
        } else {
            if (extras != null) {
                mAccount = (Account) extras.getSerializable(IN_DSPL_TGT);
            }
        }

        setContentView(R.layout.invite);

        // Change the fields around a bit if data is passed in.  The title is the primary
        // method of informing the user what will happen:
        // 1) confirmed(true): already a friend, can only reject at this point.
        // not confirmed and...
        // 2) pending(true):   someone wants to be your friend, accept is primary action.
        // 3) pending(false):  you sent this, so you must want to cancel it now.
        // is already a friend (confirmed = true) we disable the Accept button.
        if(mAccount != null) {
            int lblKey = R.string.inv_title_make;
            if(mAccount.confirmed) {
                lblKey = R.string.inv_title_reject;
            } else {
                lblKey = mAccount.pending ? R.string.inv_title_accept : R.string.inv_title_cancel;
            }
            TextView holdTitle = (TextView) findViewById(R.id.lblInviteTitle);
            if(holdTitle != null){ holdTitle.setText(lblKey); }

            EditText holdName = (EditText) findViewById(R.id.contactUnique);
            if (holdName != null) holdName.setText(mAccount.unique);
            CheckBox holdMirror = (CheckBox) findViewById(R.id.chkMirror);
            if (holdMirror != null) holdMirror.setChecked(mAccount.mirror);

            // Label Buttons
            Button holdAccept = (Button) findViewById(R.id.invInvite);
            if (holdAccept != null) {
                holdAccept.setText(R.string.accept);
                holdAccept.setEnabled(!(mAccount.confirmed || (mAccount.isFriend && !mAccount.pending)));
            }
            Button holdReject = (Button) findViewById(R.id.invCancel);
            if (holdReject != null) holdReject.setText(R.string.reject);
        }
    }

    /*
     *  Since this is a data entry screen, with some data collection in dialogs,
     *  we need to persist that extra data in the case of Activity resets.  Make
     *  sure to call the super as the last thing done.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(IN_DSPL_TGT, mAccount);
        super.onSaveInstanceState(outState);
    }
    /*
     *  Restore the state, compliments of onSaveInstanceState().
     *  NOTE: If you do not see something here, often we need to
     *  restore data in the OnCreate().
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /*
     *  If this is an active invitation, we want to cancel it.
     *  After that we will just simulate a back button press.
     *  While we could just call finish() here, in the case of a program using fragments,
     *  there is more going on, so better to always really just use onBackPressed().
     */
    public void PickCancelInvite(View view) {
        if(mAccount != null){
            CancelFriendThread cft = new CancelFriendThread(getApplicationContext(), mAccount);
            cft.start();
        }

        onBackPressed();
    }

    /*
     *  Send the invitation.  There must be something in the address field.
     */
    public void PickSendInvite(View view) {
        String [] addresses = new String[] {""};
        String display = "";    // Not currently asking for a display name, but could add it in the future.
        boolean mirror = false;

        EditText holdName = (EditText) findViewById(R.id.contactUnique);
        if (holdName != null) addresses[0] = holdName.getText().toString();
        CheckBox holdMirror = (CheckBox) findViewById(R.id.chkMirror);
        if (holdMirror != null) mirror = holdMirror.isChecked();

        // If a phone number, do a little cleanup
        if(!addresses[0].contains("@")) {
            PhoneNumberUtil phoneHelper = PhoneNumberUtil.getInstance();
            try {
                Phonenumber.PhoneNumber fullNbr = phoneHelper.parse(addresses[0], "US");
                String holdnbr = phoneHelper.format(fullNbr, PhoneNumberUtil.PhoneNumberFormat.E164);
                addresses[0] = holdnbr.replace("+", "");
            } catch (NumberParseException e) {
                addresses[0] = "";
            }
        }

        if (addresses[0].length() > 0) {
            SendInviteThread smt = new SendInviteThread(getApplicationContext(), addresses, display, mirror);
            smt.start();
            finish();
        } else {
            Toast.makeText(getApplicationContext(), R.string.err_no_contact, Toast.LENGTH_LONG).show();
        }
    }
}
