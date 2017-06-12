package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 *  This is to manage the incoming prompts.  Mostly it creates a notification
 *  that the user will see and interact with.
 */
public class Notification extends FirebaseMessagingService {

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(RemoteMessage prompt) {
        Map<String, String> holdPrompt = prompt.getData();
        sendNotification(holdPrompt.get(IN_NOTE_MSG));
    }

    // Put the message into a notification and post it.
    private void sendNotification(String msg) {

        NotificationManager notificationMgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, History.class), 0);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(msg))
                .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        notificationMgr.notify(NOTIFICATION_ID, mBuilder.build());
    }

}
