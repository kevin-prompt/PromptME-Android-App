package com.coolftc.prompt.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.coolftc.prompt.Account;

import static com.coolftc.prompt.utility.Constants.*;

/**
 *  The IgnoreNotification service is used by the Notification Ignore button to do nothing
    except dismiss the notification.
 */
public class IgnoreNotification extends IntentService {
    private static final String SRV_NAME = "IgnoreNotificationService";  // Name can be used for debugging.

    public IgnoreNotification() {
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

        // Clear the notification.
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel((int) mAccount.acctId);
    }
}
