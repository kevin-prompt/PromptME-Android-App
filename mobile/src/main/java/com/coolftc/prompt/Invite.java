package com.coolftc.prompt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.coolftc.prompt.service.CancelFriendThread;
import com.coolftc.prompt.service.SendInviteThread;
import static com.coolftc.prompt.utility.Constants.*;


/**
 *  The Invite screen is for those occasions when the contact is not readily available in the
    contact list
    -or- when the Mirror attribute needs to be part of the invitation process
    -or- when a user wants to manually reply to a friend request.

 *  The idea that a person may want to send an invitation to someone who is not in their
    contacts is straight forward.

 *  The reason this screen is require for creating/confirming a Mirror is that such activity
    should be uncommon and is hard to explain, so it is buried in this more explicit
    invitation path.  Also, we want to make harder to create/accept a Mirror because if
    that is done inadvertently, the behavior would be unexpected.
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

        // Change the fields around a bit if data is passed in.
        if(mAccount != null){
            EditText holdName = (EditText) findViewById(R.id.contactUnique);
            if (holdName != null) holdName.setText(mAccount.unique);
            CheckBox holdMirror = (CheckBox) findViewById(R.id.chkMirror);
            if (holdMirror != null) holdMirror.setChecked(mAccount.mirror);
            Button holdAccept = (Button) findViewById(R.id.invInvite);
            if (holdAccept != null) holdAccept.setText(R.string.accept);
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
     *  After than we will just simulate a back button press.
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
        boolean mirror = false;
        EditText holdName = (EditText) findViewById(R.id.contactUnique);
        if (holdName != null) addresses[0] = holdName.getText().toString();
        CheckBox holdMirror = (CheckBox) findViewById(R.id.chkMirror);
        if (holdMirror != null) mirror = holdMirror.isChecked();

        if (addresses[0].length() > 0) {
            SendInviteThread smt = new SendInviteThread(getApplicationContext(), addresses, mirror);
            smt.start();
            finish();
        } else {
            Toast.makeText(getApplicationContext(), R.string.err_no_contact, Toast.LENGTH_LONG).show();
        }
    }
}
