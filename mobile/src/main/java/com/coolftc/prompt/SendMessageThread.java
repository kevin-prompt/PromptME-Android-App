package com.coolftc.prompt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import static com.coolftc.prompt.Constants.*;

/**
 *  This thread is used to:
        a. Save prompt to table with status = sending.
        b. Send prompt to server with the web service.
        c. Update status on table to sent or failed, along with key and specific time.
 */
public class SendMessageThread extends Thread {

    private Reminder mData;
    private MessageDB mMessage;
    private Context mContext;

    SendMessageThread(Context activity, Reminder msg) {
        mData = msg;
        mContext = activity;
    }

    @Override
    public void run() {
        try {
            mMessage = new MessageDB(mContext);  // Be sure to close this before leaving the thread.
            // Create the record in the local db, but set to not processed.
            long localId = addMessage(mData, false);
            // Create the record on the server using the web service. Get the real time back.
            Actor sender = new Actor(mContext);
            WebServiceModels.PromptResponse actual = sendMessage(sender, mData);
            // Update the local record with future time or status, and mark as processed.
            if(actual.response >= 200 && actual.response < 300) {
                updSuccess(localId, actual.noteTime, actual.noteId);
            }else {
                updFailure(localId, actual.response);
            }
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".run");
        } finally {
            mMessage.close();
        }
    }

    /*
     *  Send a new prompt to the server. If things work out, the server returns
     *  the actual time that the prompt will be generated.
     */
    private WebServiceModels.PromptResponse sendMessage(Actor from, Reminder msg){
        WebServiceModels.PromptResponse realTime;
        WebServices ws = new WebServices();
        if(ws.IsNetwork(mContext)) {
            WebServiceModels.PromptRequest rData = new WebServiceModels.PromptRequest();
            rData.when = msg.targetTime;
            rData.timezone = msg.target.timezone;
            rData.timename = msg.targetTimenameId;
            rData.timeadj = msg.targetTimeadjId;
            rData.scycle = msg.target.sleepcycle;
            rData.receiveId = msg.target.acctId;
            rData.units = msg.recureUnit;
            rData.period = msg.recurePeriod;
            rData.recurs = msg.recureNumber;
            rData.end = msg.recureEnd;
            rData.message = msg.message;

            realTime = ws.NewPrompt(from.ticket, from.acctIdStr(), rData);
        } else {
            realTime = new WebServiceModels.PromptResponse();
            realTime.response = NETWORK_DOWN;
        }
        return realTime;
    }

    // Add a new reminder to local DB.
    private long addMessage(Reminder msg, boolean processed) {
        SQLiteDatabase db = mMessage.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_UNIQUE, msg.target.unique);
        values.put(MessageDB.MESSAGE_NAME, msg.target.bestName());
        values.put(MessageDB.MESSAGE_TIME, msg.targetTime);
        values.put(MessageDB.MESSAGE_TIMENAME, msg.targetTimenameId);
        values.put(MessageDB.MESSAGE_TIMEADJ, msg.targetTimeadjId);
        values.put(MessageDB.MESSAGE_R_UNIT, msg.recureUnit);
        values.put(MessageDB.MESSAGE_R_PERIOD, msg.recurePeriod);
        values.put(MessageDB.MESSAGE_R_NUMBER, msg.recureNumber);
        values.put(MessageDB.MESSAGE_R_END, msg.recureEnd);
        values.put(MessageDB.MESSAGE_MSG, msg.message);
        values.put(MessageDB.MESSAGE_SRVR_ID, 0);   // No server id yet.
        values.put(MessageDB.MESSAGE_STATUS, 0);    // No status yet.
        values.put(MessageDB.MESSAGE_CREATE, KTime.ParseNow(KTime.KT_fmtDate3339k).toString());
        values.put(MessageDB.MESSAGE_PROCESSED, (processed?MessageDB.SQLITE_TRUE:MessageDB.SQLITE_FALSE));

        return db.insert(MessageDB.MESSAGE_TABLE, null, values);  // Returns -1 if there is an error.
    }

    // Change an existing record to hold the server time and id.  Mark as processed.
    private void updSuccess(long id, String timeExact, long serverId) {
        SQLiteDatabase db = mMessage.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_TIME, timeExact);
        values.put(MessageDB.MESSAGE_SRVR_ID, serverId);
        values.put(MessageDB.MESSAGE_PROCESSED, MessageDB.SQLITE_TRUE);

        String where = DB_Table_ID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }

    // Change an existing record to reflect message failed to send. Mark as processed.
    private void updFailure(long id, long status) {
        SQLiteDatabase db = mMessage.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_STATUS, status);
        values.put(MessageDB.MESSAGE_PROCESSED, MessageDB.SQLITE_TRUE);

        String where = DB_Table_ID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }

}
