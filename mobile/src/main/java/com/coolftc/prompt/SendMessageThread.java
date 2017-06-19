package com.coolftc.prompt;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import java.util.Calendar;
import static com.coolftc.prompt.Constants.*;

/**
 *  This thread is used to:
        a. Save data to table with status = sending.
        b. Send data to web service.
        c. Update status on table to sent or failed, along with key and specific time.
 */
public class SendMessageThread extends Thread {

    private Reminder data;
    private MessageDB message;
    private Context context;

    SendMessageThread(AppCompatActivity activity, Reminder msg) {
        data = msg;
        context = activity;
    }

    @Override
    public void run() {
        try {
            message = new MessageDB(context);  // Be sure to close this before leaving the thread.
            // Create the record in the local db, but set to not processed.
            long localId = addMessage(data, false);
            // Create the record on the server using the web service. Get the real time back.
            Actor sender = new Actor(context);
            SendMessageOut actual = sendMessage(sender, data);
            // Update the local record with future time or status, and mark as processed.
            if(actual.status == 0) updSuccess(localId, actual.finalTime, true);
            if(actual.status != 0) updFailure(localId, actual.status);
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".run");
        } finally {
            message.close();
        }
    }

    // Send a new prompt to the server.
    private SendMessageOut sendMessage(Actor from, Reminder msg){
        SendMessageOut rtn = new SendMessageOut();
        WebServices ws = new WebServices();
        if(ws.IsNetwork(context)) {
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

            WebServiceModels.PromptResponse realTime = ws.NewPrompt(from.ticket, from.acctIdStr(), rData);
            if (realTime.response >= 200 && realTime.response < 300) {
                try {
                    Calendar delivery = KTime.ConvertTimezone(KTime.ParseToCalendar(realTime.noteTime, KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE), from.timezone);
                    rtn.finalTime = DateFormat.format(KTime.KT_fmtDateShrtMiddle, delivery).toString() + " @ " +
                            DateFormat.format(KTime.KT_fmtDateTime, delivery).toString();
                } catch (ExpParseToCalendar ex) {
                    // The web service worked, so the likely message is fine, just date problem, so use current time.
                    Calendar delivery = Calendar.getInstance();
                    rtn.finalTime = DateFormat.format(KTime.KT_fmtDateShrtMiddle, delivery).toString() + " @ " +
                            DateFormat.format(KTime.KT_fmtDateTime, delivery).toString();
                }
            }
            else{
                rtn.status = realTime.response;
            }
        } else {
            rtn.status = NETWORK_DOWN;
        }
        return rtn;
    }

    // Add a new reminder to local DB.
    private long addMessage(Reminder msg, boolean processed) {
        SQLiteDatabase db = message.getWritableDatabase();

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
        values.put(MessageDB.MESSAGE_STATUS, 0);    // No status yet.
        values.put(MessageDB.MESSAGE_CREATE, KTime.ParseNow(KTime.KT_fmtDate3339k).toString());
        values.put(MessageDB.MESSAGE_PROCESSED, (processed?MessageDB.SQLITE_TRUE:MessageDB.SQLITE_FALSE));

        return db.insert(MessageDB.MESSAGE_TABLE, null, values);  // Returns -1 if there is an error.
    }

    // Change an existing record to hold the actual future time and mark as processed.
    private void updSuccess(long id, String timeExact, boolean processed) {
        SQLiteDatabase db = message.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_TIME, timeExact);
        values.put(MessageDB.MESSAGE_PROCESSED, (processed?MessageDB.SQLITE_TRUE:MessageDB.SQLITE_FALSE));

        String where = DB_Table_ID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }

    // Change an existing record to reflect message failed to send.
    private void updFailure(long id, long status) {
        SQLiteDatabase db = message.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_STATUS, status);
        values.put(MessageDB.MESSAGE_PROCESSED, MessageDB.SQLITE_TRUE);

        String where = DB_Table_ID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }

    /*  This is a simple class to wrap multivalue output from SendMessage.
        Default values are status of ok and no final time.
     */
    class SendMessageOut { public int status = 0; public String finalTime; }
}
