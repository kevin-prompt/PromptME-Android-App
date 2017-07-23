package com.coolftc.prompt.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.coolftc.prompt.Actor;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.source.MessageDB;
import com.coolftc.prompt.Reminder;
import com.coolftc.prompt.source.WebServices;

import static com.coolftc.prompt.utility.Constants.*;

/**
 *  This thread is used to:
    a. Removed a prompt from the table.
    b. Cancel a prompt on the server with the web service, if it is still pending.
 */

public class CancelMessageThread extends Thread {
    private Reminder mData;
    private MessageDB mMessage;
    private Context mContext;

    public CancelMessageThread(Context activity, Reminder msg) {
        mData = msg;
        mContext = activity;
    }

    /*
     *  This tries to delete the message off the server, and if that works, deletes
     *  it locally.  If the message cannot be deleted off the server for good reason,
     *  e.g. its time has past, still delete it locally.
     */
    @Override
    public void run() {
        try {
            mMessage = new MessageDB(mContext);  // Be sure to close this before leaving the thread.
            Actor sender = new Actor(mContext);
            WebServices ws = new WebServices();

            // For Recurring notes, we want to always try to remove from the server but still
            // delete them locally if there is a failure to do so (since they might not be there).
            // It is possible to snooze a recurring message, so also try to delete that, which
            // also might not exist any longer.
            if (mData.IsRecurring()) {
                if (ws.IsNetwork(mContext)) {
                    ws.DelPrompt(sender.ticket, sender.acctIdStr(), mData.ServerIdStr());
                    if(mData.snoozeId > 0) {
                        ws.DelPrompt(sender.ticket, sender.acctIdStr(), mData.SnoozeIdStr());
                    }
                    DelMessage(mData.id);
                } else {
                    updFailure(mData.id, NETWORK_DOWN);
                }
            } else {
                // If message delivery time has passed, there is no message on the server to delete.
                if (mData.IsPast()) {
                    DelMessage(mData.id);
                } else {
                    // Check if this is snoozed, as we will need to use the snooze id.  If the call
                    // fails for some reason, we will leave the record but update the status so they
                    // can try again.
                    if (ws.IsNetwork(mContext)) {
                        String realID = mData.snoozeId > 0 ? mData.SnoozeIdStr() : mData.ServerIdStr();
                        int actual = ws.DelPrompt(sender.ticket, sender.acctIdStr(), realID);
                        if (actual >= 200 && actual < 300) {
                            DelMessage(mData.id);
                        } else {
                            updFailure(mData.id, actual);
                        }
                    } else {
                        updFailure(mData.id, NETWORK_DOWN);
                    }
                }
            }

            // Trigger the Refresh to update the Pending count.
            Intent intent = new Intent(mContext, Refresh.class);
            mContext.startService(intent);

        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".run");
        } finally {
            mMessage.close();
        }
    }

    /*
     *  Delete the record locally.
     */
    private void DelMessage(long id){
        SQLiteDatabase db = mMessage.getWritableDatabase();

        String where = "_ID=" + Long.toString(id);
        db.delete(MessageDB.MESSAGE_TABLE, where, null);
    }

    /*
     *  Change an existing record to reflect message failed to send. Mark as processed.
     */
    private void updFailure(long id, long status) {
        SQLiteDatabase db = mMessage.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_STATUS, status);
        values.put(MessageDB.MESSAGE_PROCESSED, MessageDB.SQLITE_TRUE);

        String where = DB_Table_ID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }

}
