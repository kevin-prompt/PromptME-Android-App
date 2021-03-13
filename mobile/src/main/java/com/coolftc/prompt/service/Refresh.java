package com.coolftc.prompt.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;

import androidx.core.content.ContextCompat;

import static com.coolftc.prompt.utility.Constants.*;
import static com.coolftc.prompt.utility.KTime.KT_fmtDate3339fk;
import static com.coolftc.prompt.utility.KTime.UTC_TIMEZONE;

import com.coolftc.prompt.Account;
import com.coolftc.prompt.Actor;
import com.coolftc.prompt.R;
import com.coolftc.prompt.Settings;
import com.coolftc.prompt.source.Invitations;
import com.coolftc.prompt.source.InviteResponse;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.source.FriendDB;
import com.coolftc.prompt.utility.KTime;
import com.coolftc.prompt.source.MessageDB;
import com.coolftc.prompt.utility.WebServices;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *  This service is used to update various local data with any changed server data.
    For now this primarily means checking on the connections, occasionally doing
    a token refresh and updating how many pending prompts the user has.

   NOTE: Android works to keep itself operating smoothly by aggressively cleaning
    up tasks that do not seem to be getting used.  This means Services/Threads
    cannot expect to be long running, even if they do not use up a lot of resources.
    Services/Threads should do something and then exit.  If that thing needs to be
    done periodically, use the AlarmManager or JobScheduler to schedule calls to it.
*/
public class Refresh extends IntentService {
    private static final String SRV_NAME = "RefreshService";  // Name can be used for debugging.
    private FriendDB mSocial;
    private MessageDB mMessage;
    // The contact permission is stored to reduce management overhead.
    int mContactPermissionCheck;
    // The friendAge is a debounce value for the friend query.
    private static LocalDateTime friendAge = LocalDateTime.MIN;

    public Refresh() {
        super(SRV_NAME);
    }

    @Override
    protected void onHandleIntent(Intent hint) {
        Actor ghost = new Actor(this);

        /*
         *  Check the notification token. The signup process needs the push notification
         *  information.  The push notification info is usually stored in the account, but
         *  there is no account until after signup.  To side-step this, we save off what we
         *  have along with defaults. The signup process knows to not expect more than this.
         *
         *  This section also periodically refreshes the token to make sure both the app
         *  and the server are using fresh push notification data.
         */
        try {
            // This mostly is for first spin up of app when device is empty.
            if (ghost.device.length() == 0) {
                ghost.device = ghost.identifier();
                ghost.SyncPrime(false, this);
            }

            // Check for domain updates once a day (at most).
            WebServices ws = new WebServices(new Gson());
            if(LocalDate.now().isAfter(ws.baseUrlAge(getApplicationContext()))) {
                try {
                    DomainThread domainThread = new DomainThread(getApplicationContext(), null);
                    domainThread.start();
                } catch (Exception ex) {
                    ExpClass.Companion.logEX(ex, "API Error cannot find domain target.");
                }
            }

            // Generally the token should not be blank, as NotificationX has a listener to get it.
            if (ghost.token.length() == 0) {
                FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        ghost.token = task.getResult();
                        ghost.SyncPrime(true, getApplicationContext());
                    }
                });
            }

            // Somewhere in the App, someone decided the local data changed and now needs
            // to be synchronized with the server.
            if (ghost.force) {
                ghost.force = false;
                ghost.SyncPrime(true, this);
            }

            // If possible, we want to copy the default notification sound.  While we cannot ask
            // for the permission here, we do ask if the user goes to settings.  Until then the
            // default sound will be used for Notification sounds.
            // ** This sound is only of concern to Apps running below Android v8 (API 26). **
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            if(!Settings.isSoundCopied(getApplicationContext())
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Settings.setSoundCopied(
                            getApplicationContext(),
                            InstallNotificationSound(R.raw.promptbeep, Environment.DIRECTORY_NOTIFICATIONS)
                    );
            }

        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".onHandleIntentA");
        }

        // Check if the user is signed up yet.
        if (ghost.ticket.length() == 0 || ghost.acctId == 0) return;

        /*
         *  Sync the mSocial graph.  This will grab data off the server and then clean up
         *  the local database with any Deletes, Changes, Adds.  In that order.  For Adds,
         *  check if there is local contact information that can supplement the data.
         */
        try (Connection net = new Connection(getApplicationContext())){
            mSocial = new FriendDB(getApplicationContext());  // Be sure to close this before leaving the thread.
            // Check that valid account and not updating too often.
            if (ghost.ticket.length() > 0 || LocalDateTime.now().isAfter(friendAge)) {
                WebServices ws = new WebServices(new Gson());
                if (net.isOnline()) {
                    String realPath = ws.baseUrl(getApplicationContext()) + FTI_Friends.replace(SUB_ZZZ, ghost.acctIdStr());
                    Invitations invites = ws.callGetApi(realPath, Invitations.class, ghost.ticket);
                    if (invites != null && (invites.getFriends() == null || invites.getFriends().size() == 0))
                        return; // There is always 1 friend (yourself), if not something is wrong.
                    mContactPermissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS);
                    Account[] inviteStore = queryFriends();
                    CheckForDeletes(invites, inviteStore);
                    CheckForUpdates(invites, inviteStore);
                    CheckForAdditions(invites, inviteStore);
                    UpdateContactInfo(inviteStore);
                    CheckForUserDate(ghost, inviteStore);
                    friendAge = LocalDateTime.now().plus(15, ChronoUnit.MINUTES);
                }
            }
        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".onHandleIntentB");
            // If there is a date problem, update and see if it works next time.
            friendAge = LocalDateTime.now().plus(15, ChronoUnit.MINUTES);
        } finally {
            mSocial.close();
        }

        /*
         *  Check for pending prompts (these are just ones sent by this user). We want to
         *  cache this value so the main thread has easy access.  Also update any server
         *  specific user data.
         */
        try {
            mMessage = new MessageDB(getApplicationContext()); // Be sure to close this before leaving the thread.
            if (ghost.ticket.length() > 0) {
                int holdPend = getPendPromptCnt();
                if (ghost.notesWaiting != holdPend){
                    ghost.notesWaiting = holdPend;
                    ghost.SyncPrime(false,this);
                }
            }
        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".onHandleIntentC");
            // If there is a date problem, update and see if it works next time.
            friendAge = LocalDateTime.now().plus(15, ChronoUnit.MINUTES);
        } finally {
            mMessage.close();
        }
    }

    /*
     *  The server side is authoritative, so check to see if any data has
     *  been added, so it can also be stored locally.
     *  Check each of the Invitations against all Accounts.
     */
    private void CheckForAdditions(Invitations server, Account[] local){
        List<Account> toAdd = new ArrayList<>();

        localLoop: for(InviteResponse invite : Objects.requireNonNull(server.getFriends())){
            for(Account acct : local){
                if(invite.getFriendId() == acct.acctId)
                    continue localLoop;
            }
            toAdd.add(CopyInviteAcct(invite, true, false));
        }
        localLoop: for(InviteResponse invite : Objects.requireNonNull(server.getRsvps())){
            for(Account acct : local){
                if(invite.getFriendId() == acct.acctId)
                    continue localLoop;
            }
            toAdd.add(CopyInviteAcct(invite, false, true));
        }
        localLoop: for(InviteResponse invite : Objects.requireNonNull(server.getInvites())){
            for(Account acct : local){
                if(invite.getFriendId() == acct.acctId)
                    continue localLoop;
            }
            toAdd.add(CopyInviteAcct(invite, false, false));
        }
        addFriends(toAdd);
    }

    // Copy server invite to build an account.
    private Account CopyInviteAcct(InviteResponse invite, boolean confirm, boolean sent){
        Account hold = new Account();
        hold.acctId = invite.getFriendId();
        hold.timezone = invite.getTimezone();
        hold.sleepcycle = invite.getScycle();
        hold.unique = invite.getFname();
        hold.display = invite.getFdisplay();
        hold.mirror = invite.getMirror();
        hold.pending = sent;
        hold.confirmed = confirm;
        hold.isFriend = true;
        return hold;
    }

    /*
     *  The server side is authoritative, so check to see if any data has
     *  gotten out of sync with the existing local data.  Since "pending"
     *  should only change if confirmed changes, not need to check for it.
     *  Check each Account against all Friends/RSVPs/Invitations.
     */
    private void CheckForUpdates(Invitations server, Account[] local) {
        List<Account> toChg = new ArrayList<>();

        // The localLoop will cut short the iterations by moving on when found
        localLoop: for(Account acct : local){
            for(InviteResponse invite : Objects.requireNonNull(server.getFriends())){
                if(acct.acctId == invite.getFriendId()) {
                    if (Objects.requireNonNull(invite.getFname()).equalsIgnoreCase(acct.unique) &&
                        Objects.requireNonNull(invite.getTimezone()).equalsIgnoreCase(acct.timezone) &&
                        invite.getScycle() == acct.sleepcycle &&
                        Objects.requireNonNull(invite.getFdisplay()).equalsIgnoreCase(acct.display) &&
                        invite.getMirror() == acct.mirror && acct.confirmed) {
                        continue localLoop;
                    }
                    acct.unique = invite.getFname();
                    acct.timezone = invite.getTimezone();
                    acct.sleepcycle = invite.getScycle();
                    acct.display = invite.getFdisplay();
                    acct.mirror = invite.getMirror();
                    acct.confirmed = true;
                    acct.pending = false;
                    acct.isFriend = true;
                    toChg.add(acct);
                }
            }
            for(InviteResponse invite : Objects.requireNonNull(server.getRsvps())){
                if(acct.acctId == invite.getFriendId()) {
                    if (Objects.requireNonNull(invite.getFname()).equalsIgnoreCase(acct.unique) &&
                        Objects.requireNonNull(invite.getTimezone()).equalsIgnoreCase(acct.timezone) &&
                        invite.getScycle() == acct.sleepcycle &&
                        Objects.requireNonNull(invite.getFdisplay()).equalsIgnoreCase(acct.display) &&
                        invite.getMirror() == acct.mirror && !acct.confirmed) {
                        continue localLoop;
                    }
                    acct.unique = invite.getFname();
                    acct.timezone = invite.getTimezone();
                    acct.sleepcycle = invite.getScycle();
                    acct.display = invite.getFdisplay();
                    acct.mirror = invite.getMirror();
                    acct.confirmed = false;
                    acct.pending = true;
                    acct.isFriend = true;
                    toChg.add(acct);
                }
            }
            for(InviteResponse invite : Objects.requireNonNull(server.getInvites())){
                if(acct.acctId == invite.getFriendId()) {
                    if (Objects.requireNonNull(invite.getFname()).equalsIgnoreCase(acct.unique) &&
                        Objects.requireNonNull(invite.getTimezone()).equalsIgnoreCase(acct.timezone) &&
                        invite.getScycle() == acct.sleepcycle &&
                        Objects.requireNonNull(invite.getFdisplay()).equalsIgnoreCase(acct.display) &&
                        invite.getMirror() == acct.mirror && !acct.confirmed) {
                        continue localLoop;
                    }
                    acct.unique = invite.getFname();
                    acct.timezone = invite.getTimezone();
                    acct.sleepcycle = invite.getScycle();
                    acct.display = invite.getFdisplay();
                    acct.mirror = invite.getMirror();
                    acct.confirmed = false;
                    acct.pending = false;
                    acct.isFriend = true;
                    toChg.add(acct);
                }
            }
        }
        chgFriends(toChg);
    }

    /*
     *  This applies the contact name and picture to the user (if available).
     */
    private void CheckForUserDate(Actor user, Account[] local){
        for(Account acct : local){
            if(user.acctId == acct.acctId){
                if(!user.contactPic.equalsIgnoreCase(acct.contactPic)){
                    user.contactPic = acct.contactPic;
                    user.SyncPrime(false, getApplicationContext());
                }
                if(!user.contactName.equalsIgnoreCase(acct.contactName)){
                    user.contactName = acct.contactName;
                    user.SyncPrime(false, getApplicationContext());
                }
            }
        }
    }

    /*
     *  The server side is authoritative, so any local records not in
     *  the server list should be deleted.  If there are any deletes,
     *  let the caller know (might want to refresh the display).
     *  Check each Account against all Invitations.
     */
    private void CheckForDeletes(Invitations server, Account[] local) {
        List<String> toDel = new ArrayList<>();

        // The localLoop will cut short the iterations by moving on when found
        localLoop: for(Account acct : local){
            for(InviteResponse invite : Objects.requireNonNull(server.getFriends())){
                if(acct.acctId == invite.getFriendId())
                    continue localLoop;
            }
            for(InviteResponse invite : Objects.requireNonNull(server.getRsvps())){
                if(acct.acctId == invite.getFriendId())
                    continue localLoop;
            }
            for(InviteResponse invite : Objects.requireNonNull(server.getInvites())){
                if(acct.acctId == invite.getFriendId())
                    continue localLoop;
            }
            toDel.add(acct.localId);
        }
        delFriends(toDel);
    }

    /*
     *  This focuses on trying to find friends that are in the local contact
     *  store, so the image + name can be borrowed. Specifically, if a local
     *  account does not have a value for the contact id, this tries to find
     *  one.  Updates to friend records when contacts change will be done in
     *  a different place.
     *  Returns true if anything was updated.
     */
    private void UpdateContactInfo(Account[] local){
        List<Account> toChg = new ArrayList<>();
        if(mContactPermissionCheck == PackageManager.PERMISSION_GRANTED) {
            for (Account acct : local) {
                if (acct.contactId.length() == 0) {
                    Account holdContact;
                    if (acct.isEmail()) {
                        holdContact = getContactByEmail(acct.unique);
                    } else {
                        holdContact = getContactByPhone(acct.unique);
                    }
                    if (holdContact.contactName.length() > 0) {
                        acct.contactId = holdContact.contactId;
                        acct.contactName = holdContact.contactName;
                        acct.contactPic = holdContact.contactPic;
                        toChg.add(acct);
                    }
                }
            }
            chgFriends(toChg);
        }
    }

    // Find the contact information for the phone number.  This seems to work well enough,
    // even with the number supplied not matching exactly what is seen in contacts.
    private Account getContactByPhone(String number) {
        Account holdContact = new Account();
        Cursor contact = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] selection = {ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI};

        try {
            contact = getContentResolver().query(uri, selection, null, null, null);
            if (contact == null || contact.getCount() == 0) return holdContact;
            while (contact.moveToNext()) {
                long id = contact.getLong(contact.getColumnIndex(ContactsContract.PhoneLookup._ID));
                holdContact.contactId = Long.toString(id);
                holdContact.contactName = contact.getString(contact.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                holdContact.contactPic = contact.getString(contact.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));
            }
            contact.close();
        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".getContactByPhone");
            if (contact != null) contact.close();
        }
        return holdContact;
    }

    // Find the contact information for the email address.
    private Account getContactByEmail(String email) {
        Account holdContact = new Account();
        Cursor contact = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(email));
        String[] selection = {ContactsContract.Data.CONTACT_ID, ContactsContract.Data.DISPLAY_NAME_PRIMARY, ContactsContract.Data.PHOTO_THUMBNAIL_URI};

        try {
            contact = getContentResolver().query(uri, selection, null, null, null);
            if (contact == null || contact.getCount() == 0) return holdContact;
            while (contact.moveToNext()) {
                long id = contact.getLong(contact.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                holdContact.contactId = Long.toString(id);
                holdContact.contactName = contact.getString(contact.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY));
                holdContact.contactPic = contact.getString(contact.getColumnIndex(ContactsContract.Data.PHOTO_THUMBNAIL_URI));
            }
            contact.close();
        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".getContactByEmail");
            if (contact != null) contact.close();
        }
        return holdContact;
    }

    // Retrieve any known friends and invites from the local DB.
    private Account[] queryFriends(){
        SQLiteDatabase db = mSocial.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_FriendsAll, filler);
        try{
            Account[] dataPoints = new Account[cursor.getCount()];
            int i = 0;
            while(cursor.moveToNext()) {
                Account local = new Account();
                local.localId = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_ID));
                local.acctId = cursor.getLong(cursor.getColumnIndex(FriendDB.FRIEND_ACCT_ID));
                local.unique = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_UNIQUE));
                local.display = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_DISPLAY));
                local.timezone = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_TIMEZONE));
                local.sleepcycle = cursor.getInt(cursor.getColumnIndex(FriendDB.FRIEND_SCYCLE));
                local.contactId = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_CONTACT_ID));
                local.contactName = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_CONTACT_NAME));
                local.contactPic = cursor.getString(cursor.getColumnIndex(FriendDB.FRIEND_CONTACT_PIC));
                local.pending = cursor.getInt(cursor.getColumnIndex(FriendDB.FRIEND_PENDING)) == FriendDB.SQLITE_TRUE;
                local.mirror = cursor.getInt(cursor.getColumnIndex(FriendDB.FRIEND_MIRROR)) == FriendDB.SQLITE_TRUE;
                local.confirmed = cursor.getInt(cursor.getColumnIndex(FriendDB.FRIEND_CONFIRM)) == FriendDB.SQLITE_TRUE;
                local.isFriend = true;
                dataPoints[i++] = local;
            }
            cursor.close();
            return dataPoints;
        } catch(Exception ex){ cursor.close(); ExpClass.Companion.logEX(ex, this.getClass().getName() + ".queryFriends"); return new Account[0]; }
    }

    // Add any new friends and invitations.
    private void addFriends(List<Account> items) {
        SQLiteDatabase db = mSocial.getWritableDatabase();

        for(Account acct : items) {
            ContentValues values = new ContentValues();
            values.put(FriendDB.FRIEND_ACCT_ID, acct.acctId);
            values.put(FriendDB.FRIEND_UNIQUE, acct.unique);
            values.put(FriendDB.FRIEND_DISPLAY, acct.display);
            values.put(FriendDB.FRIEND_TIMEZONE, acct.timezone);
            values.put(FriendDB.FRIEND_SCYCLE, acct.sleepcycle);
            values.put(FriendDB.FRIEND_CONTACT_ID, acct.contactId);
            values.put(FriendDB.FRIEND_CONTACT_NAME, acct.contactName);
            values.put(FriendDB.FRIEND_CONTACT_PIC, acct.contactPic);
            values.put(FriendDB.FRIEND_MIRROR, acct.mirror);
            values.put(FriendDB.FRIEND_PENDING, acct.pending);
            values.put(FriendDB.FRIEND_CONFIRM, acct.confirmed);
            db.insert(FriendDB.FRIEND_TABLE, null, values);
        }
    }

    // Change any changes to the list of friends and invitations.
    private void chgFriends(List<Account> items) {
        SQLiteDatabase db = mSocial.getWritableDatabase();

        for(Account acct : items){
            ContentValues values = new ContentValues();
            values.put(FriendDB.FRIEND_ACCT_ID, acct.acctId);
            values.put(FriendDB.FRIEND_UNIQUE, acct.unique);
            values.put(FriendDB.FRIEND_DISPLAY, acct.display);
            values.put(FriendDB.FRIEND_TIMEZONE, acct.timezone);
            values.put(FriendDB.FRIEND_SCYCLE, acct.sleepcycle);
            values.put(FriendDB.FRIEND_CONTACT_ID, acct.contactId);
            values.put(FriendDB.FRIEND_CONTACT_NAME, acct.contactName);
            values.put(FriendDB.FRIEND_CONTACT_PIC, acct.contactPic);
            values.put(FriendDB.FRIEND_MIRROR, acct.mirror);
            values.put(FriendDB.FRIEND_PENDING, acct.pending);
            values.put(FriendDB.FRIEND_CONFIRM, acct.confirmed);
            String where = "_id = " + acct.localId;
            String[] filler = {};
            db.update(FriendDB.FRIEND_TABLE, values, where, filler);
        }
    }

    // Delete any old friends or invitations that are no longer
    private void delFriends(List<String> keys) {
        SQLiteDatabase db = mSocial.getWritableDatabase();

        for(String key : keys) {
            String where = "_id = " + key;
            String[] filler = {};
            db.delete(FriendDB.FRIEND_TABLE, where, filler);
        }
    }

    /*
     *  Get the count of messages of a certain age.  The messages are stored in the local
     *  DB with a timestamp in UTC, so we just want to get what time it is now in the UTC
     *  timezone.  SQLite stores dates as strings and this comparison seems to work. We
     *  then can cache this value for later display to the user.
     */
    private int getPendPromptCnt() {
        SQLiteDatabase db = mMessage.getReadableDatabase();
        String[] filler = {};
        String holdNowUTC = KTime.ParseNow(KT_fmtDate3339fk, UTC_TIMEZONE).toString();
        Cursor cursor = db.rawQuery(DB_PendingCnt.replace(SUB_ZZZ, holdNowUTC), filler);
        int count = 0;
        // If empty, cursor returns false.
        if(cursor.moveToFirst()) { count = cursor.getInt(0); }
        cursor.close();
        return count;
    }

    /*
     *  This copies a supplied MP3 file from the raw App storage area to the global media
     *  region specified for a sound, e.g. Ringtone, Notification, etc.
     *  eDir:   This should be a system label like Environment.DIRECTORY_NOTIFICATIONS
     *  I believe just copying the file is all that is needed to have it put in use
     *  by the system.  No special media settings, but make sure the MP3 has the "title"
     *  field of the metadata filled in, as that is what is displayed to the user.
     */
    private boolean InstallNotificationSound(int resId, String eDir) {
        try {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                return false; // no storage available.

            // Copy the file
            File path = Environment.getExternalStoragePublicDirectory(eDir);
            //noinspection ResultOfMethodCallIgnored
            path.mkdirs();  // if the path is not there, create it
            String filename = getResources().getResourceEntryName(resId) + ".mp3";
            File outFile = new File(path, filename);
            copyResource(resId, outFile);

            // Not sure needed, but this should let the system know there is a new file.
            String mimeType = "audio/mpeg";
            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{outFile.getPath()}, new String[]{mimeType}, null);
            return true;

        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".InstallNotificationSound");
            return false;
        }
    }

    /*
     *  File copy for android.  Once min version 19 reached, can use  automatic resource management.
     */
    public void copyResource(int resourceId, File dst) throws IOException {
        try (InputStream in = getResources().openRawResource(resourceId)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}


