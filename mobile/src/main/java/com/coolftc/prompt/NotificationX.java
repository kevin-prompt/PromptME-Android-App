package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 *  This class manages the incoming prompts.  It creates a notification that the
    user sees and interacts with.  TThe incoming prompt contains the following data:
     IN_NOTE_TYPE      - The type of notification. Possible names are: NOTE – for a prompt, INVITE – for an invitation or confirmation of friendship
     IN_NOTE_KEY       - The original server id of the prompt.
     IN_NOTE_FROM      - The unique name of the sender of the message.
     IN_NOTE_FROMID    - The server id of the sender of the message.
     IN_NOTE_FROMNICE  - The display name of the sender of the message.
     IN_NOTE_PEND      - The scheduled time of delivery for the message.
     IN_NOTE_RECUR     - Set to -1 if not recurring, otherwise it is the recurring Unit.
     IN_NOTE_MSG       - The message.

 *  This data can be used to find the full prompt on the local data store
    or create a new one for historic review.
 */
public class NotificationX extends FirebaseMessagingService {

    public static final int NOTIFICATION_ID = 1;
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
     */
    private void promptNotification(Reminder msg) {

        NotificationManager notificationMgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Some customization of the notification
        Uri toneUri = Settings.getRingtone(this);
        boolean vibrateon = Settings.getVibrateOn(this);

        // Pass along what information we have
        Intent intentX = new Intent(this, Detail.class);
        Bundle mBundle = new Bundle();
        mBundle.putSerializable(IN_MESSAGE, msg);
        intentX.putExtras(mBundle);
        // Using FLAG_UPDATE_CURRENT as each new notification will likely refer to a new Prompt.
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intentX, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg.message))
                .setContentText(msg.message)
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

    /*
     *  Use this method for invitations.
     */
    private void inviteNotification(Reminder msg) {

    }
}
