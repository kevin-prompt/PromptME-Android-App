package com.coolftc.prompt.service;

import static com.coolftc.prompt.utility.Constants.*;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import androidx.annotation.NonNull;

import com.coolftc.prompt.Account;
import com.coolftc.prompt.Actor;
import com.coolftc.prompt.ContactPicker;
import com.coolftc.prompt.Detail;
import com.coolftc.prompt.Invite;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.R;
import com.coolftc.prompt.Reminder;
import com.coolftc.prompt.Settings;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 *  This class manages the incoming prompts (push notifications).  It creates a notification
    that the user sees and interacts with.  The incoming prompt contains the following data:
     IN_NOTE_TYPE      - The type of notification. Possible names are:
                         NOTE – for a prompt,
                         INVITE – for an invitation of friendship
                         FRIEND - for a confirmation of friendship
     IN_NOTE_KEY       - The original server id of the prompt/invite.
     IN_NOTE_FROM      - The unique name of the sender of the message.
     IN_NOTE_FROMID    - The server id of the sender of the message.
     IN_NOTE_FROMNICE  - The display name of the sender of the message.
     IN_NOTE_PEND      - The scheduled time of delivery for the message.
     IN_NOTE_RECUR     - Set to -1 if not recurring, otherwise it is the recurring Unit. (Only valid for NOTE)
     IN_NOTE_MIRROR    - Set to 1 if friendship is a mirror, otherwise zero (0).  (Only valid for INVITE)
     IN_NOTE_MSG       - The message.

 *  This data can be used to find the full prompt on the local data store
    or create a new one for historic review.
 */
public class NotificationX extends FirebaseMessagingService {

    private static final int TYPE_PROMPT = 1;
    private static final String CHANNEL_PROMPT_ID = "channel.prompt";
    private static final int TYPE_INVITE = 2;
    private static final String CHANNEL_INVITE_ID = "channel.invite";
    private static final int TYPE_FRIEND = 3;
    private static final String CHANNEL_FRIEND_ID = "channel.friend";
    private static final String CHANNEL_GROUP_ID = "channel.groupid";
    private static final String NOTIFY_BASE_STORE = "channel.storage";
    private static final String NOTIFY_BASE_CHANNEL = "channel.setup";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage prompt) {
        try {

            Reminder note = parseNotification(prompt.getData());
            if(!getChannelComplete()) { setupNotificationChannels(); }

            switch (note.type) {
                case TYPE_PROMPT:
                    promptNotification(note);
                    break;
                case TYPE_INVITE:
                    inviteNotification(note);
                    break;
                case TYPE_FRIEND:
                    friendNotification(note);
                    break;
                default:
                    ExpClass.Companion.logINFO(KEVIN_SPEAKS, "An unknown notification type came into the Application.");
            }
        } catch (Exception ex) {
            // This is a general catch to avoid showing a crash screen to the user.
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".onMessageReceived");
        }
    }

    /*
     * Called if FCM token is updated. This may occur if the security of the
     * previous token had been compromised.  Also called when the FCM token
     * is initially generated.
     * This token is the same as the one retrieved by FirebaseMessaging.getInstance().getToken().
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Actor acct = new Actor(this);
        acct.force = true; // This will trigger a refresh of the backend db in the service.
        acct.token = token;
        acct.SyncPrime(false, this);
        Intent intent = new Intent(this, Refresh.class);
        startService(intent);
        // This only really has to happen once, so here seems like a good place to start.
        setupNotificationChannels();
    }

    /*
     *  This will parse the incoming notification.  The type can be:
     *  0 for an Unknown note.
     *  1 for a PROMPT
     *  2 for an INVITE
     */
    private Reminder parseNotification(Map<String, String> note){
        String type = note.get(IN_NOTE_TYPE);
        String holdNoteVal;
        Reminder holdMsg = new Reminder();
        if(type != null) {
            if (type.equalsIgnoreCase(IN_TYPE_PROMPT)) { holdMsg.type = TYPE_PROMPT; }
            if (type.equalsIgnoreCase(IN_TYPE_INVITE)) { holdMsg.type = TYPE_INVITE; }
            if (type.equalsIgnoreCase(IN_TYPE_FRIEND)) { holdMsg.type = TYPE_FRIEND; }
        }

        // If this is a selfie, make the from equal to the target, otherwise, fill out from.
        holdMsg.target = new Actor(getApplicationContext());
        if (holdMsg.target.unique.equalsIgnoreCase(note.get(IN_NOTE_FROM))) {
            holdMsg.from = holdMsg.target;
        } else {
            holdMsg.from = new Account();
            holdMsg.from.unique = note.get(IN_NOTE_FROM);
            holdMsg.from.display = note.get(IN_NOTE_FROMNICE);
            holdNoteVal = note.get(IN_NOTE_FROMID);
            holdMsg.from.acctId = holdNoteVal != null ? Long.parseLong(holdNoteVal) : 0L;
            holdNoteVal = note.get(IN_NOTE_MIRROR);
            holdMsg.from.mirror = holdNoteVal != null && holdNoteVal.equalsIgnoreCase("1");
        }

        holdNoteVal = note.get(IN_NOTE_KEY);
        holdMsg.serverId = holdNoteVal != null ? Long.parseLong(holdNoteVal) : 0L;
        holdMsg.targetTime = note.get(IN_NOTE_PEND);
        holdNoteVal = note.get(IN_NOTE_RECUR);
        holdMsg.recurUnit =  holdNoteVal != null ? Integer.parseInt(holdNoteVal) : 0;
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

        // Support viewing of the Prompt - This brings up the detail screen.
        // This requires creation of a fake stack so if the user hits back, they end up in an expected location.
        // Also, put a server id in pending intent to make sure unique intents are created for each Prompt.
        Intent intentX = new Intent(this, Detail.class);
        Bundle xBundle = new Bundle();
        xBundle.putSerializable(IN_MESSAGE, msg);
        intentX.putExtras(xBundle);
        intentX.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intentX);
        PendingIntent contentIntent = stackBuilder.getPendingIntent((int)msg.serverId, PendingIntent.FLAG_UPDATE_CURRENT);

        // Support a snooze - This starts a service to create a snoozed message.
        // Put a server id in pending intent to make sure unique intents are created for each Prompt.
        Intent intentS = new Intent(this, Snooze.class);
        Bundle sBundle = new Bundle();
        sBundle.putSerializable(IN_MESSAGE, msg);
        intentS.putExtras(sBundle);
        PendingIntent snoozeIntent = PendingIntent.getService(this, (int)msg.serverId, intentS, PendingIntent.FLAG_CANCEL_CURRENT);

        /*
         *  The settings for local notifications and why.  Note that if targeting v26+ a
         *  NotificationChannel will need to be created (somewhere else) and then added to
         *  to the notification (either in the Builder constructor or .SetChannelId())
         *  .addAction          Places buttons for snooze/dismiss on
         *  .setAutoCancel      Have notification dismiss after use.
         *  .setContentIntent   Used to navigate to the App when notification touched.
         *  .setContentText     The second row of a notification.
         *  .setContentTitle    The first row of a notification.
         *  .setSmallIcon       The icon that shows up in the notification.
         *  .setStyle           Rich notification style to display more text.
         *  .setWhen            The time of the Prompt, supports ordering of notifications.
         *  The sound and vibration are a little trickier.  They are also only valid for
         *  Android OS below v8 (see setupNotificationChannels()).
         *  .setSound           Sound to play, specify a stream if vibration is on.
         *  .setDefaults        Needed to make the device vibrate (DEFAULT_VIBRATE).
         *  When sending a notification, add a unique number to have each posted separately.
         */
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_PROMPT_ID)
                .addAction(R.drawable.ic_snooze_black_18dp, getString(R.string.snooze), snoozeIntent)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setContentText(msg.message)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setSmallIcon(R.drawable.prompt_notify)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg.message))
                .setWhen(msg.GetPromptMSec());

        // Notification Sound/Vibration is managed in the channel for v8.0+
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (vibrateon && toneUri != null) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
                        .setSound(toneUri, AudioManager.STREAM_NOTIFICATION);
            } else if (vibrateon) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else if (toneUri != null) {
                mBuilder.setSound(toneUri);
            }
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
        NotificationManager notificationMgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Some customization of the notification
        Uri toneUri = Settings.getRingtone(this);
        boolean vibrateon = Settings.getVibrateOn(this);

        // Support viewing of the Invite - This brings up the raw invite screen.
        // This requires creation of a fake stack so if the user hits back, they end up in an expected location.
        // Also, put a server id in pending intent to make sure unique intents are created for each Prompt.
        Intent intentI = new Intent(this, Invite.class);
        Bundle xBundle = new Bundle();
        xBundle.putSerializable(IN_DSPL_TGT, msg.from);
        intentI.putExtras(xBundle);
        intentI.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TaskStackBuilder stackBuilderX = TaskStackBuilder.create(this);
        stackBuilderX.addNextIntentWithParentStack(intentI);
        PendingIntent inviteIntent = stackBuilderX.getPendingIntent((int)msg.from.acctId, PendingIntent.FLAG_UPDATE_CURRENT);

        // Support a direct accept - This triggers the thread to invite, which in this case is to accept.
        // Put a server id in pending intent to make sure unique intents are created for each Prompt.
        Intent intentA = new Intent(this, AcceptInvite.class);
        Bundle aBundle = new Bundle();
        aBundle.putSerializable(IN_DSPL_TGT, msg.from);
        intentA.putExtras(aBundle);
        PendingIntent acceptIntent = PendingIntent.getService(this, (int)msg.from.acctId, intentA, PendingIntent.FLAG_CANCEL_CURRENT);

        // Support ignoring of the notification.
        // Put a server id in pending intent to make sure unique intents are created for each Prompt.
        Intent intentD = new Intent(this, IgnoreNotification.class);
        Bundle dBundle = new Bundle();
        dBundle.putSerializable(IN_DSPL_TGT, msg.from);
        intentD.putExtras(dBundle);
        PendingIntent ignoreIntent = PendingIntent.getService(this, (int)msg.from.acctId, intentD, PendingIntent.FLAG_CANCEL_CURRENT);

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
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_INVITE_ID)
                .setAutoCancel(true)
                .setContentIntent(inviteIntent)
                .setContentText(msg.message)
                .setContentTitle(getResources().getString(R.string.inv_name))
                .setSmallIcon(R.drawable.prompt_notify)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg.message))
                .setWhen(msg.GetPromptMSec());

        // Notification Sound/Vibration is managed in the channel for v8.0+
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (vibrateon && toneUri != null) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
                        .setSound(toneUri, AudioManager.STREAM_NOTIFICATION);
            } else if (vibrateon) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else if (toneUri != null) {
                mBuilder.setSound(toneUri);
            }
        }

        // The accept button has slightly different behavior based on if it is a mirror request.
        if (msg.from.mirror){
            mBuilder.addAction(R.drawable.ic_person_add_black_18dp, getString(R.string.accept), inviteIntent)
                    .addAction(R.drawable.ic_notifications_off_black_18dp, getString(R.string.ignore), ignoreIntent);
        } else {
            mBuilder.addAction(R.drawable.ic_person_add_black_18dp, getString(R.string.accept), acceptIntent)
                    .addAction(R.drawable.ic_notifications_off_black_18dp, getString(R.string.ignore), ignoreIntent);
        }

        // User the serverId to so it is easy to dismiss later.
        notificationMgr.notify((int)msg.from.acctId, mBuilder.build());

        /*
         *  Trigger Refresh service to bring the application up to date.
         */
        Intent sIntent = new Intent(this, Refresh.class);
        startService(sIntent);
    }

    /*
     *  Use this method to alert a user their invitation has been accepted.
     */
    private void friendNotification(Reminder msg) {
        NotificationManager notificationMgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        // Some customization of the notification
        Uri toneUri = Settings.getRingtone(this);
        boolean vibrateon = Settings.getVibrateOn(this);

        // Support navigation to Contact list - Where the new friend can be found.
        // This requires creation of a fake stack so if the user hits back, they end up in an expected location.
        // Also, put a server id in pending intent to make sure unique intents are created for each Prompt.
        Intent intentF = new Intent(this, ContactPicker.class);
        intentF.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(intentF);
        PendingIntent contentIntent = stackBuilder.getPendingIntent((int)msg.from.acctId, PendingIntent.FLAG_UPDATE_CURRENT);

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
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_FRIEND_ID)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setContentText(msg.message)
                .setContentTitle(getResources().getString(R.string.friend_accept))
                .setSmallIcon(R.drawable.prompt_notify)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg.message))
                .setWhen(msg.GetPromptMSec());

        // Notification Sound/Vibration is managed in the channel for v8.0+
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (vibrateon && toneUri != null) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
                        .setSound(toneUri, AudioManager.STREAM_NOTIFICATION);
            } else if (vibrateon) {
                mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
            } else if (toneUri != null) {
                mBuilder.setSound(toneUri);
            }
        }

        // User the serverId to so it is easy to dismiss later.
        notificationMgr.notify((int)msg.from.acctId, mBuilder.build());

        /*
         *  Trigger Refresh service to bring the application up to date.
         */
        Intent sIntent = new Intent(this, Refresh.class);
        startService(sIntent);
    }

    /*
     *  This can be used to create all the notification channels required to put notifications
     *  in the action bar. Android v8 requires each notification type to have a set of user
     *  customizable settings. This is required before a notification can be posted, otherwise
     *  the system will just eat anything you try and post (on systems at or above v8).
     *  Creating a notification channel with its original values performs a no-op, so its
     *  safe to call multiple times. Once data is added to a channel, it cannot be changed
     *  programmatically, ever!! :(
     * See overview: https://itnext.io/android-notification-channel-as-deep-as-possible-1a5b08538c87
     * A note on DND.  The default for notifications is to not allow them while the device is DND.
     * One could change that here with NotificationChannel.setBypassDnd(true), but it requires the
     * user to approve policy access to the app (and for that to be done before this is set up).
     * This requires the below system activity, but really you would want to explain it before
     * calling the activity. Might be worth it for this App, but probably not since it has to happen
     * prior to the channel getting setup.  Best to maybe direct the user to allow prompts while in
     * DND manually via settings.
     * Add to Manifest: <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
     *      if(!mgr.isNotificationPolicyAccessGranted()) {
     *          Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
     *          intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
     *          startActivity(intent); }
     * If the user was to adjust the setting above, it would generate a system broadcast that could
     * be caught with: android.app.action.NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED
     */
    public void setupNotificationChannels(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // Set up the Grouping of notifications.
            NotificationChannelGroup group = new NotificationChannelGroup(CHANNEL_GROUP_ID, getString(R.string.ntf_channel_group_name));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                group.setDescription(getString(R.string.ntf_channel_group_desc));
            }
            mgr.createNotificationChannelGroup(group);
            // Set up the Channels for notifications.
            List<NotificationChannel> allNc = new ArrayList<>();
            allNc.add(createNotificationChannel(
                    CHANNEL_GROUP_ID,
                    CHANNEL_PROMPT_ID,
                    getString(R.string.ntf_channel_name_prompt),
                    getString(R.string.ntf_channel_desc_prompt),
                    NotificationManager.IMPORTANCE_HIGH,
                    R.raw.promptbeep,
                    true,
                    true,
                    Notification.VISIBILITY_PUBLIC));
            allNc.add(createNotificationChannel(
                    CHANNEL_GROUP_ID,
                    CHANNEL_INVITE_ID,
                    getString(R.string.ntf_channel_name_invite),
                    getString(R.string.ntf_channel_desc_invite),
                    NotificationManager.IMPORTANCE_DEFAULT,
                    R.raw.promptbeep,
                    false,
                    false,
                    Notification.VISIBILITY_PRIVATE));
            allNc.add(createNotificationChannel(
                    CHANNEL_GROUP_ID,
                    CHANNEL_FRIEND_ID,
                    getString(R.string.ntf_channel_name_friend),
                    getString(R.string.ntf_channel_desc_friend),
                    NotificationManager.IMPORTANCE_DEFAULT,
                    0,
                    false,
                    false,
                    Notification.VISIBILITY_SECRET));

            mgr.createNotificationChannels(allNc);
            setChannelComplete();
        }
    }

    private boolean getChannelComplete() {
        SharedPreferences preference = getSharedPreferences(NOTIFY_BASE_STORE, MODE_PRIVATE);
        return preference.getBoolean(NOTIFY_BASE_CHANNEL, false);
    }
    private void setChannelComplete() {
        SharedPreferences registered = getSharedPreferences(NOTIFY_BASE_STORE, MODE_PRIVATE);
        SharedPreferences.Editor editor = registered.edit();
        editor.putBoolean(NOTIFY_BASE_CHANNEL, true);
        editor.apply();
    }

    /*
     *  Many of the attributes of a Notification have been moved into a "channel" as of
     *  Android v8.  This allow the user to adjust these settings, but does require that
     *  the settings only be created once by the system.  If the user wishes to change
     *  them later, the supplied system UI needs to be used.  Most of the settings are
     *  provided here (LED and DND are not).
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel createNotificationChannel(String groupId, String channelId, String name, String desc, int important, int soundRes, boolean vib, boolean badge, int lockscreen){
        NotificationChannel nc = new NotificationChannel(channelId, name, important);
        nc.setDescription(desc);
        nc.setGroup(groupId);
        if(soundRes > 0) {
            Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(getResources().getResourcePackageName(soundRes))
                    .appendPath(getResources().getResourceTypeName(soundRes))
                    .appendPath(getResources().getResourceEntryName(soundRes))
                    .build();
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build();
            nc.setSound(uri, aa);
        }
        nc.enableVibration(vib);
        if(vib) { nc.setVibrationPattern(new long[] {2000L, 1000L}); }
        nc.setLockscreenVisibility(lockscreen);
        nc.setShowBadge(badge);

        return nc;
    }
}
