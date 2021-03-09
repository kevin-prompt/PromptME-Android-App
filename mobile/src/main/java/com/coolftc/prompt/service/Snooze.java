package com.coolftc.prompt.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import com.coolftc.prompt.Actor;
import com.coolftc.prompt.source.PromptResponse;
import com.coolftc.prompt.source.SnoozeRequest;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.source.MessageDB;
import com.coolftc.prompt.Reminder;
import com.coolftc.prompt.Settings;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import java.time.ZonedDateTime;
import static com.coolftc.prompt.utility.Constants.*;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 *  The Snooze service is used by the Notification snooze buttons to push the Prompt
    into the future a bit.  This calls the web service and then updates the MessageDB.
    Since a person may not get to a notification right away, we want to base the
    snooze offset on the current time (when it was requested) and not the time of the
    original prompt.
 */
public class Snooze extends IntentService {
    private static final String SRV_NAME = "SnoozeService";  // Name can be used for debugging.

    public Snooze() {
        super(SRV_NAME);
    }

    @Override
    protected void onHandleIntent(Intent hint) {

        if(hint == null || hint.getExtras() == null) return; // If there is no message, nothing to do.
        Bundle extras = hint.getExtras();
        Reminder prompt = (Reminder) extras.getSerializable(IN_MESSAGE);

        // Just in case.
        if(prompt == null) return;

        // Clear the notification.
        // While this risks not doing the snooze, the annoyance of not dismissing the notification
        // when touching snooze if far more of a problem.  Once Prompts (and snoozes) can be queued,
        // dismissing prior to success will be even less of an issue.
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel((int) prompt.serverId);

        try (MessageDB mDB = new MessageDB(getApplicationContext())){

            // Get a new time to send the prompt.
            String snoozeTime = ZonedDateTime.now()
                    .plusMinutes(Settings.getSnooze(getApplicationContext())).format(ISO_OFFSET_DATE_TIME);

            // Tell the server to snooze this prompt.
            Actor user = new Actor(getApplicationContext());
            PromptResponse actual = sendSnooze(user, snoozeTime, prompt);

            // Update the local record with future time and update new (snooze) id.
            updSnooze(mDB, prompt.serverId, actual.getPromptTime(), actual.getPromptId());

        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".onHandleIntent");
        }
    }

    /*
     *  Snooze a prompt to the server. If things work out, the server returns
     *  the actual time that the prompt will be (again) generated.
     */
    private PromptResponse sendSnooze(Actor from, String snoozeTime, Reminder msg) throws ExpClass {
        try (Connection net = new Connection(getApplicationContext())) {
            WebServices ws = new WebServices(new Gson());
            if (net.isOnline()) {
                SnoozeRequest message = new SnoozeRequest(
                        snoozeTime,
                        msg.target.timezone,
                        msg.serverId,
                        msg.from.acctId,
                        msg.message
                );
                String realPath = ws.baseUrl(getApplicationContext()) + FTI_Message.replace(SUB_ZZZ, from.acctIdStr());
                return ws.callPutApi(realPath, message, PromptResponse.class, from.ticket);
            } else {
                throw new ExpClass(99, this.getClass().getName() + ".sendSnooze", "offline");
            }
            } catch (ExpClass kx) {
                ExpClass.Companion.logEXP(kx, this.getClass().getName() + ".sendSnooze");
                throw kx;
            }
    }

    // Change an existing record to hold the server time and id.  Mark as processed.
    private void updSnooze(MessageDB messageDB, long id, String timeExact, long snoozeId) {
        SQLiteDatabase db = messageDB.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_TIME, timeExact);
        values.put(MessageDB.MESSAGE_SNOOZE_ID, snoozeId);

        String where = DB_Table_ServerID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }


}
