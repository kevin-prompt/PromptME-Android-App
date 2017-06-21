package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 *  This is to manage the incoming prompts.  Mostly it creates a notification
 *  that the user will see and interact with.  Also trigger a data refresh here.
 */
public class NotificationX extends FirebaseMessagingService {

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(RemoteMessage prompt) {
        Map<String, String> holdPrompt = prompt.getData();
        sendNotification(holdPrompt.get(IN_NOTE_MSG));
    }

    // Put the message into a notification and post it.
    private void sendNotification(String msg) {

        NotificationManager notificationMgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Some customization of the notification
        Uri toneUri = Settings.getRingtone(this);
        boolean vibrateon = Settings.getVibrateOn(this);

        // Using FLAG_UPDATE_CURRENT as each new notification will likely refer to a new Prompt.
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, History.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg)
                .setContentIntent(contentIntent);

        if (vibrateon && toneUri != null) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
                    .setSound(toneUri, AudioManager.STREAM_NOTIFICATION);
        } else if (vibrateon) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        } else if (toneUri != null) {
            mBuilder.setSound(toneUri);
        }

        notificationMgr.notify(NOTIFICATION_ID, mBuilder.build());

        /*
         *  Trigger Refresh service to bring the application up to date.
         */
        Intent sIntent = new Intent(this, Refresh.class);
        startService(sIntent);
    }

}
