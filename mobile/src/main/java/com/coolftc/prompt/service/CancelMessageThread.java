package com.coolftc.prompt.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import com.coolftc.prompt.Actor;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.source.MessageDB;
import com.coolftc.prompt.Reminder;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import static com.coolftc.prompt.utility.Constants.*;

/**
 *  This thread is used to:
    a. Removed a prompt from the table.
    b. Cancel a prompt on the server with the web service, if it is still pending.
 */

public class CancelMessageThread extends Thread {
    private final Context mContext;
    private final Reminder mPrompt;

    public CancelMessageThread(Context application, Reminder msg) {
        mPrompt = msg;
        mContext = application;
    }

    /*
     *  This tries to delete the message off the server, and if that works, deletes
     *  it locally.  If the message cannot be deleted off the server for good reason,
     *  e.g. its time has past, still delete it locally.
     */
    @Override
    public void run() {

        try (Connection net = new Connection(mContext); MessageDB db = new MessageDB(mContext)) {
            Actor sender = new Actor(mContext);
            WebServices ws = new WebServices(new Gson());

            // For Recurring notes, we want to try to remove from the server but always delete
            // them locally, even if the API call fails. It is possible to snooze a recurring
            // message, so also try to delete it.
            if (mPrompt.IsRecurring()) {
                if (net.isOnline()) {
                    try {
                        String realPath = ws.baseUrl(mContext) + FTI_Message_Del.replace(SUB_ZZZ, sender.acctIdStr()) + mPrompt.ServerIdStr();
                        ws.callDeleteApi(realPath, sender.ticket);
                        if (mPrompt.snoozeId > 0) {
                            realPath = ws.baseUrl(mContext) + FTI_Message_Del.replace(SUB_ZZZ, sender.acctIdStr()) + mPrompt.SnoozeIdStr();
                            ws.callDeleteApi(realPath, sender.ticket);
                        }
                    } catch (ExpClass kx) {
                        /* skip api failures and just delete locally. */
                        ExpClass.Companion.logEXP(kx, this.getClass().getName() + ".run");
                    }
                    DelMessage(db, mPrompt.id);
                } else {
                    updFailure(db, mPrompt.id, NETWORK_DOWN);
                }
            } else {
                // If message delivery time has passed, there is no message on the server to delete.
                if (mPrompt.IsPast()) {
                    DelMessage(db, mPrompt.id);
                } else {
                    // Check if this is snoozed, as we will need to use the snooze id.  If the call
                    // fails for some reason, we will leave the record but update the status so they
                    // can try again.
                    if (net.isOnline()) {
                        try {
                            String realID = mPrompt.snoozeId > 0 ? mPrompt.SnoozeIdStr() : mPrompt.ServerIdStr();
                            String realPath = ws.baseUrl(mContext) + FTI_Message_Del.replace(SUB_ZZZ, sender.acctIdStr()) + realID;
                            ws.callDeleteApi(realPath, sender.ticket);
                            DelMessage(db, mPrompt.id);
                        } catch (ExpClass kx) {
                            /* Update the local record. */
                            updFailure(db, mPrompt.id, kx.getStatus());
                        }
                    } else {
                        updFailure(db, mPrompt.id, NETWORK_DOWN);
                    }
                }
            }

            // Trigger the Refresh to update the Pending count.
            Intent intent = new Intent(mContext, Refresh.class);
            mContext.startService(intent);

        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".run");
        }
    }

    /*
     *  Delete the record locally.
     */
    private void DelMessage(MessageDB database, long id){
        SQLiteDatabase db = database.getWritableDatabase();

        String where = "_ID=" + id;
        db.delete(MessageDB.MESSAGE_TABLE, where, null);
    }

    /*
     *  Change an existing record to reflect message failed to send. Mark as processed.
     */
    private void updFailure(MessageDB database, long id, long status) {
        SQLiteDatabase db = database.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(MessageDB.MESSAGE_STATUS, status);
        values.put(MessageDB.MESSAGE_PROCESSED, MessageDB.SQLITE_TRUE);

        String where = DB_Table_ID.replace(SUB_ZZZ, Long.toString(id));
        db.update(MessageDB.MESSAGE_TABLE, values, where, null);
    }

}
