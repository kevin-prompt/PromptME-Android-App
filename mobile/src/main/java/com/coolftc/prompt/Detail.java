package com.coolftc.prompt;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static com.coolftc.prompt.Constants.*;

public class Detail extends AppCompatActivity {

    private Reminder mPrompt = new Reminder();
    private Actor mUser = null;
    private FriendDB mSocial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail);

        // Initialize default values.
        Bundle extras = getIntent().getExtras();
        if (savedInstanceState != null) {
            mPrompt = (Reminder) savedInstanceState.getSerializable(IN_MESSAGE);
        } else {
            if (extras != null) {
                mPrompt = (Reminder) extras.getSerializable(IN_MESSAGE);
            }
        }

        // If the incoming data is properly populated, it will be marked as processed.  In
        // that case we do not need to load it from the database. Otherwise we do need to
        // get more information.
        mUser = new Actor(getApplicationContext());
        if(!mPrompt.isProcessed) { BuildPrompt(); }
        ShowDetails();
    }

    /*
     *  Since this is a data entry screen, with some data collection in dialogs,
     *  we need to persist that extra data in the case of Activity resets.  Make
     *  sure to call the super as the last thing done.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(IN_MESSAGE, mPrompt);
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
     *  This deletes the Prompt from the local data store, and if it has not yet
     *  been processed, it is also removed from the server pending queue.
     */
    public void CancelPrompt(View view){
        WebServices ws = new WebServices();

        if(!mPrompt.IsPast() && !ws.IsNetwork(this)) {
            Toast.makeText(this, R.string.msgNoNet, Toast.LENGTH_LONG).show();
            return;
        }

        CancelMessageThread cmt = new CancelMessageThread(getApplicationContext(), mPrompt);
        cmt.start();

        Intent intent = new Intent(this, History.class);
        startActivity(intent);
    }

    /*
     *  This allows a navigation to the Entry screen, with some data populated,
     *  specifically, who will get the message, and sometimes the message itself.
     */
    public void CopyPrompt(View view) {

        try {

            Bundle mBundle = new Bundle();
            // If the Prompt is created by the user, then they can copy that Prompt.
            // A "copy" passes along the target address as the target address and
            // makes the message available to use as a default.
            if (mUser.unique.equalsIgnoreCase(mPrompt.from.unique)) {
                mBundle.putSerializable(IN_USER_ACCT, mUser);
                mBundle.putSerializable(IN_MESSAGE, mPrompt);
            }
            // If the Prompt came from a friend, then the user can reply to it.
            // A reply passes along the from address as the target address.
            // We need to fill out the information about the account for Entry to use it.
            else { // Reply
                mSocial = new FriendDB(getApplicationContext());
                mBundle.putSerializable(IN_USER_ACCT, GetAccountByName(mPrompt.from.unique));
            }

            Intent intent = new Intent(this, Entry.class);
            intent.putExtras(mBundle);
            startActivity(intent);
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".CopyPrompt");
        } finally {
            if(mSocial != null) mSocial.close();
        }
    }

    /*
     *  Fill out a Reminder to display based on input data.  This is a best effort,
     *  if there is no further data the display will have to deal with it.
     */
    private void BuildPrompt() {

        // Check if we a local db id.
        if(mPrompt.id > 0) {
            Reminder holdPrompt = GetMessageByLocal(mPrompt.id);
            if(holdPrompt.isProcessed) mPrompt = holdPrompt;
        }
        // Maybe we have the server id
        if(!mPrompt.isProcessed && mPrompt.serverId > 0) {
            Reminder holdPrompt = GetMessageByServer(mPrompt.serverId);
            if(holdPrompt.isProcessed) mPrompt = holdPrompt;
        }
    }

    /*
     *  The raw data for this screen is provided as a parameter, or via the local
     *  database, what is shown to the user goes through a bit of transformation
     *  to make it look reasonable.
     */
    private void ShowDetails() {
        TextView holdText;
        Button holdPush;
        String holdFormatted;

        // If there is no time, then things have not gone well.
        if(mPrompt.targetTime == null || mPrompt.targetTime.length() == 0) {
            holdText = (TextView) findViewById(R.id.dtlNoteTime);
            if(holdText != null) holdText.setText(R.string.err_no_message);
            return;
        }

        // The time label depends on if the prompt is still in the future.
        int res = mPrompt.IsPast() ? R.string.arrived : R.string.scheduled;
        holdFormatted =  getResources().getString(res);
        holdFormatted += " " + mPrompt.GetPromptTimeRev(getApplicationContext());
        holdText = (TextView) findViewById(R.id.dtlNoteTime);
        if(holdText != null) { holdText.setText(holdFormatted); }

        // The recurring label includes the end date, if applicable.
        if(mPrompt.IsRecurring()){
            if(mPrompt.recurEnd.length() > 0) {
                holdFormatted = getResources().getString(R.string.is_recurring_end) + " " + mPrompt.GetRecurringTime(getApplicationContext());
            } else {
                holdFormatted = getResources().getString(R.string.is_recurring);
            }
        } else {
            holdFormatted = "";
        }
        holdText = (TextView) findViewById(R.id.dtlRecurring);
        if (holdText != null) holdText.setText(holdFormatted);

        // The message
        holdText = (TextView) findViewById(R.id.dtlMessage);
        if (holdText != null) holdText.setText(mPrompt.message);

        // The author or more relevant data.
        holdFormatted = "";
        if(mPrompt.IsSelfie()) {
            holdFormatted = getString(R.string.personal_prompt);
        } else {
            if(!mUser.unique.equalsIgnoreCase(mPrompt.target.unique)){
                holdFormatted = getString(R.string.target_prompt) + mPrompt.target.display;
            }
            if(!mUser.unique.equalsIgnoreCase(mPrompt.from.unique)){
                holdFormatted = getString(R.string.from_prompt) + mPrompt.from.display;
            }
        }
        holdText = (TextView) findViewById(R.id.dtlWho);
        if (holdText != null) holdText.setText(holdFormatted);

        // Create date
        holdFormatted = getString(R.string.created) + " " + mPrompt.GetCreatedTime(getApplicationContext());
        holdText = (TextView) findViewById(R.id.dtlCreated);
        if (holdText != null) holdText.setText(holdFormatted);

        // Status message
        if(mPrompt.status != 0){
            holdFormatted = getString(R.string.status) + ": " + Integer.toString(mPrompt.status);
            holdText = (TextView) findViewById(R.id.dtlStatus);
            if (holdText != null) holdText.setText(holdFormatted);
        }

        if(mPrompt.IsPast()) {
            holdFormatted = getString(R.string.delete);
        } else {
            holdFormatted = getString(R.string.cancel);
        }
        holdPush = (Button) findViewById(R.id.dtlCancel);
        if(holdPush != null) holdPush.setText(holdFormatted);

        if(mUser.unique.equalsIgnoreCase(mPrompt.from.unique)) {
            holdFormatted = getString(R.string.copy);
        } else {
            holdFormatted = getString(R.string.reply);
        }
        holdPush = (Button) findViewById(R.id.dtlCopy);
        if(holdPush != null) holdPush.setText(holdFormatted);

    }

    /*
     *  Read a specific message out of the local storage and return it as a Reminder.
     */
    private Reminder GetMessage(String query){
        MessageDB message = new MessageDB(this);  // Be sure to close this before leaving the thread.
        SQLiteDatabase db = message.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(query, filler);
        Reminder local;
        local = new Reminder();
        local.target = new Account();
        local.from = new Account();
        try{
            // Return null if no record found.
            if(!cursor.moveToNext()) { return local; }
            local.id = cursor.getLong(cursor.getColumnIndex(MessageDB.MESSAGE_ID));
            local.serverId = cursor.getLong(cursor.getColumnIndex(MessageDB.MESSAGE_SRVR_ID));
            local.targetTime = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TIME));
            local.targetTimeNameId = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_TIMENAME));
            local.targetTimeAdjId = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_TIMEADJ));
            local.recurUnit = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_UNIT));
            local.recurPeriod = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_PERIOD));
            local.recurNumber = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_NUMBER));
            local.recurEnd = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_R_END));
            local.message = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_MSG));
            local.isProcessed = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_PROCESSED))==MessageDB.SQLITE_TRUE;
            local.status = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_STATUS));
            local.created = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_CREATE));
            local.from.unique = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_SOURCE));
            local.from.display = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_FROM));
            local.target.unique = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TARGET));
            local.target.display = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_NAME));
            cursor.close();
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".GetMessages"); }
        finally { message.close(); }
        return local;
    }

    private Reminder GetMessageByServer(Long id) {
        return GetMessage(DB_MessageByServer.replace(SUB_ZZZ, id.toString()));
    }

    private Reminder GetMessageByLocal(Long id){
        return GetMessage(DB_MessageByLocal.replace(SUB_ZZZ, id.toString()));
    }

    /*
     *  Get a filled out Account for the Entry screen.  The Message only has
     *  the unique and display names for the accounts.
     */
    private Account GetAccountByName(String uname){
        SQLiteDatabase db = mSocial.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_FriendByName.replace(SUB_ZZZ, uname), filler);
        try{
            Account local = new Account();
            if(cursor.moveToNext()) {
                local.localId = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_ID));
                local.acctId = cursor.getLong(cursor.getColumnIndex(FriendDB.FRIEND_ACCT_ID));
                local.unique = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_UNIQUE));
                local.display = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_DISPLAY));
                local.timezone = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_TIMEZONE));
                local.sleepcycle = cursor.getInt(cursor.getColumnIndex(FriendDB.FRIEND_SCYCLE));
                local.contactId = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_CONTACT_ID));
                local.contactName = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_CONTACT_NAME));
                local.contactPic = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_CONTACT_PIC));
                local.pending = cursor.getInt(cursor.getColumnIndex(FriendDB.FRIEND_PENDING)) == FriendDB.SQLITE_TRUE;
                local.mirror = cursor.getInt(cursor.getColumnIndex(FriendDB.FRIEND_MIRROR)) == FriendDB.SQLITE_TRUE;
                local.confirmed = cursor.getInt(cursor.getColumnIndex(FriendDB.FRIEND_CONFIRM)) == FriendDB.SQLITE_TRUE;
            }
            cursor.close();
            return local;
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".GetAccountByName"); return new Account(); }
    }
}
