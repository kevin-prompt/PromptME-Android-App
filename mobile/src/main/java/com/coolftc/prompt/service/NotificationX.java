package com.coolftc.prompt.service;

import static com.coolftc.prompt.utility.Constants.*;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.coolftc.prompt.Account;
import com.coolftc.prompt.Actor;
import com.coolftc.prompt.Detail;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.R;
import com.coolftc.prompt.Reminder;
import com.coolftc.prompt.Settings;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 *  This class manages the incoming prompts (push notifications).  It creates a notification
    that the user sees and interacts with.  The incoming prompt contains the following data:
     IN_NOTE_TYPE      - The type of notification. Possible names are: NOTE – for a prompt, INVITE – for an invitation or confirmation of friendship
     IN_NOTE_KEY       - The original server id of the prompt/invite.
     IN_NOTE_FROM      - The unique name of the sender of the message.
     IN_NOTE_FROMID    - The server id of the sender of the message.
     IN_NOTE_FROMNICE  - The display name of the sender of the message.
     IN_NOTE_PEND      - The scheduled time of delivery for the message.
     IN_NOTE_RECUR     - Set to -1 if not recurring, otherwise it is the recurring Unit. (Only NOTE)
     IN_NOTE_COMPLETE  - Set to 1 if friendship is complete, otherwise zero (0).  (Only INVITE)
     IN_NOTE_MSG       - The message.

 *  This data can be used to find the full prompt on the local data store
    or create a new one for historic review.
 */
public class NotificationX extends FirebaseMessagingService {

    private static final int TYPE_NOTE = 1;
    private static final int TYPE_INVITE = 2;

    @Override
    public void onMessageReceived(RemoteMessage prompt) {

        Reminder note = parseNotification(prompt.getData());

        switch(note.type){
            case TYPE_NOTE:
                promptNotification(note);
                break;
            case TYPE_INVITE:
                inviteNotification(note);
                break;
            default:
                ExpClass.LogIN(KEVIN_SPEAKS, "An unknown notification type came into the Application.");
        }
    }

    /*
     *  This will parse the incoming notification.  The type can be:
     *  0 for an Unknown note.
     *  1 for a NOTE
     *  2 for an INVITE
     */
    private Reminder parseNotification(Map<String, String> note){
        String type = note.get(IN_NOTE_TYPE);
        Reminder holdMsg = new Reminder();
        if(type.equalsIgnoreCase(IN_TYPE_NOTE)){ holdMsg.type = TYPE_NOTE; }
        if(type.equalsIgnoreCase(IN_TYPE_INVITE)){ holdMsg.type = TYPE_INVITE; }

        // If this is a selfie, make the from equal to the target, otherwise, fill out from.
        holdMsg.target = new Actor(getApplicationContext());
        if (holdMsg.target.unique.equalsIgnoreCase(note.get(IN_NOTE_FROM))) {
            holdMsg.from = holdMsg.target;
        } else {
            holdMsg.from = new Account();
            holdMsg.from.unique = note.get(IN_NOTE_FROM);
            holdMsg.from.display = note.get(IN_NOTE_FROMNICE);
            holdMsg.from.acctId = Long.parseLong(note.get(IN_NOTE_FROMID));
        }

        holdMsg.serverId = Long.parseLong(note.get(IN_NOTE_KEY));
        holdMsg.targetTime = note.get(IN_NOTE_PEND);
        holdMsg.recurUnit = Integer.parseInt(note.get(IN_NOTE_RECUR));
        holdMsg.message = note.get(IN_NOTE_MSG);

        return holdMsg;
    }

    /*
     *  Use this method for prompts.
     *  This method creates a local notification that can be interacted with by user.
     */
    private void promptNotification(Reminder msg) {

        NotificationManager notificationMgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Some customization of the notification
        Uri toneUri = Settings.getRingtone(this);
        boolean vibrateon = Settings.getVibrateOn(this);

        // Pass along what information we have
        Intent intentX = new Intent(this, Detail.class);
        Bundle xBundle = new Bundle();
        xBundle.putSerializable(IN_MESSAGE, msg);
        intentX.putExtras(xBundle);
        intentX.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Putting in the server id here to make sure unique intents are created.
        PendingIntent contentIntent = PendingIntent.getActivity(this, (int)msg.serverId, intentX, PendingIntent.FLAG_UPDATE_CURRENT);

        // Support a snooze
        Intent intentS = new Intent(this, Snooze.class);
        Bundle sBundle = new Bundle();
        sBundle.putSerializable(IN_MESSAGE, msg);
        intentS.putExtras(sBundle);
        // Putting in the server id here to make sure unique intents are created.
        PendingIntent snoozeIntent = PendingIntent.getService(this, (int)msg.serverId, intentS, PendingIntent.FLAG_CANCEL_CURRENT);

        /*
         *  The settings for local notifications and why.  Note that if targeting v26+ a
         *  NotificationChannel will need to be created (somewhere else) and then added to
         *  to the notification (either in the constructor or .SetChannelId())
         *  .addAction          Places buttons for snooze/dismiss on
         *  .setAutoCancel      Have notification dismiss after use.
         *  .setContentIntent   Used to navigate to the App when notification touched.
         *  .setContentText     The second row of a notification.
         *  .setContentTitle    The first row of a notification.
         *  .setSmallIcon       The icon that shows up in the notification.
         *  .setStyle           Rich notification style to display more text.
         *  .setWhen            The time of the Prompt, supports ordering of notifications.
         *  The sound and vibration are a little trickier.
         *  .setSound           Sound to play, specify a stream if vibration is on.
         *  .setDefaults        Needed to make the device vibrate (DEFAULT_VIBRATE).
         *  When sending a notification, add a unique number to have each posted separately.
         */
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_snooze_black_18dp, getString(R.string.snooze), snoozeIntent)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setContentText(msg.message)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.prompt_notify)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg.message))
                .setWhen(msg.GetPromptMSec());

        if (vibrateon && toneUri != null) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
                    .setSound(toneUri, AudioManager.STREAM_NOTIFICATION);
        } else if (vibrateon) {
            mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        } else if (toneUri != null) {
            mBuilder.setSound(toneUri);
        }

        // User the serverId to so it is easy to dismiss later.
        notificationMgr.notify((int)msg.serverId, mBuilder.build());

        /*
         *  Trigger Refresh service to bring the application up to date.
         */
        Intent sIntent = new Intent(this, Refresh.class);
        startService(sIntent);
    }

    /*
     *  Use this method for invitations.
     */
    private void inviteNotification(Reminder msg) {

    }
}
