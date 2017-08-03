package com.coolftc.prompt.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.coolftc.prompt.Account;
import com.coolftc.prompt.utility.ExpClass;

import static com.coolftc.prompt.utility.Constants.*;


/**
 *  The AcceptInvite service is used by the Notification Accept button to sent out a
    a matching friend request and complete the connection.  This calls the SendInviteThread
    and so does not do much processing here.
 */
public class AcceptInvite extends IntentService {
    private static final String SRV_NAME = "AcceptInviteService";  // Name can be used for debugging.

    public AcceptInvite() {
        super(SRV_NAME);
    }

    @Override
    protected void onHandleIntent(Intent hint) {

        Account mAccount;
        Bundle extras = hint.getExtras();
        if (extras != null) {
            mAccount = (Account) extras.getSerializable(IN_DSPL_TGT);
        } else {
            return; // If there is no friend, nothing to do.
        }

        // Just in case.
        if(mAccount == null) return;

        try {
            // Clear the notification.
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel((int) mAccount.acctId);

            // By sending an invitation back, the connection is consummated.  Mirrors are not handled in this service.
            String [] addresses = new String[1];
            addresses[0] = mAccount.unique;
            SendInviteThread smt = new SendInviteThread(getApplicationContext(), addresses, false);
            smt.start();

        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".onHandleIntent");
        }
    }
}
