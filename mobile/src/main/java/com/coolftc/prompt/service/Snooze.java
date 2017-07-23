package com.coolftc.prompt.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.format.DateFormat;

import com.coolftc.prompt.Actor;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.KTime;
import com.coolftc.prompt.source.MessageDB;
import com.coolftc.prompt.Reminder;
import com.coolftc.prompt.Settings;
import com.coolftc.prompt.source.WebServiceModels;
import com.coolftc.prompt.source.WebServices;

import java.util.Calendar;

import static com.coolftc.prompt.utility.Constants.*;

/**
 *  The Snooze service is used by the Notification snooze buttons to push the Prompt
    into the future a bit.  This calls the web service and then updates the MessageDB.
    Since a person may not get to a notification right away, we want to based the
    snooze offset on the current time (when it was requested) and not the time of the
    original prompt.
 */
public class Snooze extends IntentService {
    private static final String SRV_NAME = "SnoozeService";  // Name can be used for debugging.
    private MessageDB mMessage;

    public Snooze() {
        super(SRV_NAME);
    }

    @Override
    protected void onHandleIntent(Intent hint) {

        Reminder mPrompt;
        Bundle extras = hint.getExtras();
        if (extras != null) {
            mPrompt = (Reminder) extras.getSerializable(IN_MESSAGE);
        } else {
            return; // If there is no message, nothing to do.
        }

        // Just in case.
        if(mPrompt == null) return;

        try {
            mMessage = new MessageDB(getApplicationContext());  // Be sure to close this before leaving the thread.

            // Tell the server to snooze this message (really it just creates a special Prompt).
            WebServiceModels.PromptResponse actual = sendSnooze(getNewSnoozeTime(), mPrompt);

            // Update the local record with future time or status, and mark as processed.
            if(actual.response >= 200 && actual.response < 300) {
                updSnooze(mPrompt.serverId, actual.noteTime, actual.noteId);

                // Clear the notification.
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel((int) mPrompt.serverId);
            }

        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".onHandleIntent");
        } finally {
            mMessage.close();
        }
    }

    /*
     *  Add the number of minutes to snooze, from settings, to the current time.
     */
    private String getNewSnoozeTime() {
        Calendar snoozeTime = Calendar.getInstance();
        int snoozeLength = Settings.getSnooze(getApplicationContext());
        snoozeTime.add(Calendar.MINUTE, snoozeLength);
        String outFormat = KTime.KT_fmtDate3339fk_xS; // fractional seconds required by web service
        return DateFormat.format(outFormat, snoozeTime).toString();
    }

    /*
     *  Snooze a prompt to the server. If things work out, the server returns
     *  the actual time that the prompt will be (again) generated.
     */
    private WebServiceModels.PromptResponse sendSnooze(String snoozeTime, Reminder msg){
        Actor user = new Actor(getApplicationContext());
        WebServiceModels.PromptResponse realTime;
        WebServices ws = new WebServices();
        if(ws.IsNetwork(getApplicationContext())) {
            WebServiceModels.SnoozeRequest rData = new WebServiceModels.SnoozeRequest();
            rData.when = snoozeTime;
            rData.timezone = msg.target.timezone;
            rData.message = msg.message;
            rData.snoozeId = msg.serverId;
            rData.senderId = msg.from.acctId;

            realTime = ws.SnoozePrompt(user.ticket, user.acctIdStr(), rData);
        } else {
            realTime = new WebServiceModels.PromptResponse();
            realTime.response = NETWORK_DOWN;
        }
        return realTime;
    }

    // Change an existing record to hold the server time and id.  Mark as processed.
    private void updSnooze(long id, String timeExact, long snoozeId) {
        SQLiteDatabase db = mMessage.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_TIME, timeExact);
        values.put(MessageDB.MESSAGE_SNOOZE_ID, snoozeId);

        String where = DB_Table_ServerID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }


}
