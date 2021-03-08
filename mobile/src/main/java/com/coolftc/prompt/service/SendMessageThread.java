package com.coolftc.prompt.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.coolftc.prompt.Actor;
import com.coolftc.prompt.source.PromptRequest;
import com.coolftc.prompt.source.PromptResponse;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.source.MessageDB;
import com.coolftc.prompt.Reminder;
import com.coolftc.prompt.utility.KTime;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import static com.coolftc.prompt.utility.Constants.*;

/**
 *  This thread is used to:
        a. Save prompt to table with status = sending.
        b. Send prompt to server with the web service.
        c. Update status on table to sent or failed, along with key and specific time.
 */
public class SendMessageThread extends Thread {

    private final Reminder prompt;
    private MessageDB promptDb;
    private final Context context;

    public SendMessageThread(Context activity, Reminder msg) {
        prompt = msg;
        context = activity;
    }

    @Override
    public void run() {
        try {
            promptDb = new MessageDB(context);  // Be sure to close this before leaving the thread.

            // Create the record in the local db, but set to not processed.
            long localId = addMessage(prompt, false);

            // Create the record on the server using the web service. Get the real time back.
            try {
                PromptResponse actual = sendMessage(prompt);
                updSuccess(localId, actual.getPromptTime(), actual.getPromptId());
            } catch (ExpClass kx) {
                updFailure(localId, kx.getStatus());
            }

            // Trigger the Refresh to update the Pending count.
            Intent intent = new Intent(context, Refresh.class);
            context.startService(intent);
        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".run");
        } finally {
            promptDb.close();
        }
    }

    /*
     *  Send a new prompt to the server. If things work out, the server returns
     *  the actual time that the prompt will be generated.
     */
    private PromptResponse sendMessage(Reminder msg) throws ExpClass {
        try (Connection net = new Connection(context)) {
            if (net.isOnline()) {
                Actor from = new Actor(context);
                WebServices ws = new WebServices(new Gson());
                PromptRequest message = new PromptRequest(
                        msg.targetTime,
                        msg.target.timezone,
                        msg.targetTimeNameId,
                        msg.targetTimeAdjId,
                        msg.target.sleepcycle,
                        msg.target.acctId,
                        msg.recurUnit,
                        msg.recurPeriod,
                        msg.recurEnd,
                        msg.recurNumber,
                        0,
                        msg.message
                );
                String realPath = ws.baseUrl(context) + FTI_Message.replace(SUB_ZZZ, from.acctIdStr());
                return ws.callPostApi(realPath, message, PromptResponse.class, from.ticket);
            } else {
                throw new ExpClass(99, this.getClass().getName() + ".sendMessage", "offline");
            }
        } catch (ExpClass kx) {
            ExpClass.Companion.logEXP(kx, this.getClass().getName() + ".sendMessage");
            throw kx;
        }
    }

    // Add a new reminder to local DB.
    private long addMessage(Reminder msg, boolean processed) {
        SQLiteDatabase db = promptDb.getWritableDatabase();

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
        values.put(MessageDB.MESSAGE_SRVR_ID, 0);   // No server id yet.
        values.put(MessageDB.MESSAGE_STATUS, 0);    // No status yet.
        values.put(MessageDB.MESSAGE_SNOOZE_ID, 0);
        values.put(MessageDB.MESSAGE_CREATE, KTime.ParseNow(KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE).toString());
        values.put(MessageDB.MESSAGE_PROCESSED, (processed?MessageDB.SQLITE_TRUE:MessageDB.SQLITE_FALSE));

        return db.insert(MessageDB.MESSAGE_TABLE, null, values);  // Returns -1 if there is an error.
    }

    // Change an existing record to hold the server time and id.  Mark as processed.
    private void updSuccess(long id, String timeExact, long serverId) {
        SQLiteDatabase db = promptDb.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_TIME, timeExact);
        values.put(MessageDB.MESSAGE_SRVR_ID, serverId);
        values.put(MessageDB.MESSAGE_PROCESSED, MessageDB.SQLITE_TRUE);

        String where = DB_Table_ID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }

    // Change an existing record to reflect message failed to send. Mark as processed.
    private void updFailure(long id, long status) {
        SQLiteDatabase db = promptDb.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_STATUS, status);
        values.put(MessageDB.MESSAGE_PROCESSED, MessageDB.SQLITE_TRUE);

        String where = DB_Table_ID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }

}
