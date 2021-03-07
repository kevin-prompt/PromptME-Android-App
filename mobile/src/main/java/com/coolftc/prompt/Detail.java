package com.coolftc.prompt;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.coolftc.prompt.service.CancelMessageThread;
import com.coolftc.prompt.source.FriendDB;
import com.coolftc.prompt.source.MessageDB;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.KTime;

import java.util.Arrays;

import static com.coolftc.prompt.utility.Constants.*;

/**
 *  The Detail screen shows a little bit more information that the history
    and provides a navigation endpoint for the notification and history. It
    also supports a copy/reply function as well as a delete.
 */
public class Detail extends AppCompatActivity {

    private Reminder mPrompt = new Reminder();
    private Actor mUser = null;
    private MessageDB mMessage;

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
        if(!mPrompt.processed) { BuildPrompt(); }
        ShowDetails();
    }

    /*
     *  We may need data not shown on the screen to navigate intelligently, so
     *  make sure we keep that in memory in the case of Activity resets.  Make
     *  sure to call the super as the last thing done in onSaveInstanceState().
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(IN_MESSAGE, mPrompt);
        super.onSaveInstanceState(outState);
    }
    /*
     *  Restore the state, compliments of onSaveInstanceState().
     *  NOTE: If you do not see something here, often we need to
     *  restore data in the OnCreate().
     */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    /*
     *  This deletes the Prompt from the local data store, and if it has not
     *  yet been processed or is a recurring prompt, it also removes it from
     *  the server pending queue.
     */
    public void CancelPrompt(final View view){

        // If we use the web serivce, we need the network.
        try (Connection net = new Connection(getApplicationContext())) {
            if (!net.isOnline() && (!mPrompt.IsPast() || mPrompt.IsRecurring())) {
                Toast.makeText(this, R.string.msgNoNet, Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception ex) { /* Just exit */ return; }

        // Delete nag dialog.  To launch another activity inside a listener, we save off the main activity.
        final Activity holdAct = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Prompt")
                .setMessage("Confirm you wish to delete the Prompt.")
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Push possible network (and database) actions off the main thread.
                        CancelMessageThread cmt = new CancelMessageThread(getApplicationContext(), mPrompt);
                        cmt.start();

                        // Return to the History, it will reflect the deleted data after a short interval.
                        Intent intent = new Intent(holdAct, History.class);
                        holdAct.startActivity(intent);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*
     *  This allows navigation to the Entry screen, with some data populated,
     *  specifically, who gets the message, and sometimes the message itself (copy).
     */
    public void CopyPrompt(View view) {

        try {

            Bundle mBundle = new Bundle();
            // If the Prompt is created by the user, then they can copy that Prompt.
            // A "copy" passes along the target address, but we need to fill out the
            // target account a little bit so it is usable by the Entry screen.
            if (mUser.unique.equalsIgnoreCase(mPrompt.from.unique)) {
                if(mUser.unique.equalsIgnoreCase(mPrompt.target.unique))
                    mBundle.putSerializable(IN_USER_ACCT, mUser);
                else
                    mBundle.putSerializable(IN_USER_ACCT, GetAccountByName(mPrompt.target.unique));
                mBundle.putSerializable(IN_MESSAGE, mPrompt);
            }
            // If the Prompt came from a friend, then the user can "reply" to it.
            // A reply passes along the from address as the target address.
            // Lets try and get a bit more information on the Friend, as the
            // message only has the name.
            else {
                mBundle.putSerializable(IN_USER_ACCT, GetAccountByName(mPrompt.from.unique));
            }

            Intent intent = new Intent(this, Entry.class);
            intent.putExtras(mBundle);
            startActivity(intent);
        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".CopyPrompt");
        }
    }

    /*
     *  Fill out a Reminder to display based on input data.  This is a best effort,
     *  if there is no further data the display will have to deal with it.  If the
     *  data provided is more than is stored in the local DB, add in a new record.
     */
    private void BuildPrompt() {

        try {
            mMessage = new MessageDB(getApplicationContext());  // Be sure to close this before leaving the thread.

            // Check if we have a local DB id to look up the record.
            if (mPrompt.id > 0) {
                Reminder holdPrompt = GetMessageByLocal(mPrompt.id);
                if (holdPrompt.processed) mPrompt = holdPrompt;
            }
            // Still not processed? Maybe we have the server id do do a look up.
            if (!mPrompt.processed && mPrompt.serverId > 0) {
                Reminder holdPrompt = GetMessageByServer(mPrompt.serverId);
                if (holdPrompt.processed) mPrompt = holdPrompt;
            }

            // Looks like this message is not in the local store, add it.  If we get
            // back a db id, mark as processed.
            if (!mPrompt.processed){
                mPrompt.status = (int)AddMessage(mPrompt);
                if(mPrompt.status > 0) {
                    mPrompt.processed = true;
                    mPrompt.created = KTime.ParseNow(KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE).toString();
                }
            }

        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".BuildPrompt");
        } finally {
            mMessage.close();
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
            holdFormatted = "*" + mPrompt.GetRecurringVerb(getApplicationContext());
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
                holdFormatted = getString(R.string.target_prompt) + " " + mPrompt.target.display;
            }
            if(!mUser.unique.equalsIgnoreCase(mPrompt.from.unique)){
                holdFormatted = getString(R.string.from_prompt) + " " + mPrompt.from.display;
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

        // Simple time message
        if(mPrompt.IsExactTime()){
            holdFormatted = getString(R.string.scheduled) + ": " + mPrompt.GetPromptTime(getApplicationContext());
        }else {
            holdFormatted = getString(R.string.scheduled) + ": " + Arrays.asList(getResources().getStringArray(R.array.time_name)).get(mPrompt.GetTargetTimeNameIdDsply());
            holdFormatted += ", " + Arrays.asList(getResources().getStringArray(R.array.time_adj)).get(mPrompt.GetTargetTimeAdjIdDsply());
        }
        if(mPrompt.snoozeId > 0) { holdFormatted += " -" + getString(R.string.img_snooze); }
        holdText = (TextView) findViewById(R.id.dtlSimpleTime);
        if (holdText != null) holdText.setText(holdFormatted);

        // Sleep cycle
        if(mPrompt.sleepCycle >= 0) {
            holdFormatted = getString(R.string.prf_SleepCycle) + ": " + Arrays.asList(getResources().getStringArray(R.array.sleepcycle)).get(mPrompt.sleepCycle);
        } else {
            holdFormatted = getString(R.string.prf_SleepCycle) + ": " + getString(R.string.unknown);
        }
        holdText = (TextView) findViewById(R.id.dtlSleepCycle);
        if (holdText != null) holdText.setText(holdFormatted);

        if(!mPrompt.IsPast() || mPrompt.IsRecurring()) {
            holdFormatted = getString(R.string.cancel);
        } else {
            holdFormatted = getString(R.string.delete);
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
        SQLiteDatabase db = mMessage.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(query, filler);
        Reminder local;
        local = new Reminder();
        local.target = new Account();
        local.from = new Account();
        try{
            // Return null if no record found.
            if(cursor.moveToNext()) {
                local.id = cursor.getLong(cursor.getColumnIndex(MessageDB.MESSAGE_ID));
                local.serverId = cursor.getLong(cursor.getColumnIndex(MessageDB.MESSAGE_SRVR_ID));
                local.targetTime = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TIME));
                local.targetTimeNameId = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_TIMENAME));
                local.targetTimeAdjId = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_TIMEADJ));
                local.sleepCycle = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_SLEEP));
                local.timezone = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TIMEZONE));
                local.recurUnit = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_UNIT));
                local.recurPeriod = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_PERIOD));
                local.recurNumber = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_NUMBER));
                local.recurEnd = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_R_END));
                local.message = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_MSG));
                local.processed = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_PROCESSED)) == MessageDB.SQLITE_TRUE;
                local.status = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_STATUS));
                local.snoozeId = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_SNOOZE_ID));
                local.created = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_CREATE));
                local.from.unique = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_SOURCE));
                local.from.display = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_FROM));
                local.target.unique = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TARGET));
                local.target.display = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_NAME));
            }
            cursor.close();
        } catch(Exception ex){ cursor.close(); ExpClass.Companion.logEX(ex, this.getClass().getName() + ".GetMessages"); }
        return local;
    }

    private Reminder GetMessageByServer(Long id) {
        return GetMessage(DB_MessageByServer.replace(SUB_ZZZ, id.toString()));
    }

    private Reminder GetMessageByLocal(Long id){
        return GetMessage(DB_MessageByLocal.replace(SUB_ZZZ, id.toString()));
    }

    /*
     *  Add a new reminder to local DB.  Usually due to a friend generated prompt
     *  coming in via notification.  Not all the usually information is available
     *  so some assumptions are made, e.g. sleep cycle and time zone are not part
     *  of the data included in a Prompt.
     */
    private long AddMessage(Reminder msg) {
        SQLiteDatabase db = mMessage.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_TARGET, msg.target.unique);
        values.put(MessageDB.MESSAGE_SOURCE, msg.from.unique);
        values.put(MessageDB.MESSAGE_NAME, msg.target.bestName());
        values.put(MessageDB.MESSAGE_FROM, msg.from.bestName());
        values.put(MessageDB.MESSAGE_TIME, msg.targetTime);
        values.put(MessageDB.MESSAGE_TIMENAME, msg.targetTimeNameId);
        values.put(MessageDB.MESSAGE_TIMEADJ, msg.targetTimeAdjId);
        values.put(MessageDB.MESSAGE_SLEEP, msg.target.sleepcycle);
        values.put(MessageDB.MESSAGE_TIMEZONE, msg.target.timezone);
        values.put(MessageDB.MESSAGE_R_UNIT, msg.recurUnit);
        values.put(MessageDB.MESSAGE_R_PERIOD, msg.recurPeriod);
        values.put(MessageDB.MESSAGE_R_NUMBER, msg.recurNumber);
        values.put(MessageDB.MESSAGE_R_END, msg.recurEnd);
        values.put(MessageDB.MESSAGE_MSG, msg.message);
        values.put(MessageDB.MESSAGE_SRVR_ID, msg.serverId);
        values.put(MessageDB.MESSAGE_STATUS, 0);
        values.put(MessageDB.MESSAGE_SNOOZE_ID, 0);
        values.put(MessageDB.MESSAGE_CREATE, KTime.ParseNow(KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE).toString());
        values.put(MessageDB.MESSAGE_PROCESSED, MessageDB.SQLITE_TRUE);

        return db.insert(MessageDB.MESSAGE_TABLE, null, values);  // Returns -1 if there is an error.
    }

    /*
     *  Get a filled out Account for the Entry screen.  The Message DB only has the
     *  unique and display names for the mAccounts, so this is used to suppliment it.
     */
    private Account GetAccountByName(String uname){
        FriendDB social = new FriendDB(getApplicationContext());  // Be sure to close this before leaving the thread.
        SQLiteDatabase db = social.getReadableDatabase();
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
                local.isFriend = true;
            }
            cursor.close();
            return local;
        } catch(Exception ex){ cursor.close(); ExpClass.Companion.logEX(ex, this.getClass().getName() + ".GetAccountByName"); return new Account(); }
        finally { social.close(); }
    }
}
