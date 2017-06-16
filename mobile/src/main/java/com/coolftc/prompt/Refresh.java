package com.coolftc.prompt;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import static com.coolftc.prompt.Constants.*;
import com.google.firebase.iid.FirebaseInstanceId;
import com.coolftc.prompt.WebServiceModels.*;
import java.util.ArrayList;
import java.util.List;

/**
 *  This service is used to update various local data with any changed server data.
    For now this primarily means checking on the connections and occasionally doing
    a token refresh.

   NOTE: Android works to keep itself operating smoothly by aggressively cleaning
    up tasks that do not seem to be getting used.  This means Services/Threads
    cannot expect to be long running, even if they do not use up a lot of resources.
    Services/Threads should do something and then exit.  If that thing needs to be
    done periodically, use the AlarmManager or JobScheduler to schedule calls to it.
*/
public class Refresh extends IntentService {
    private static final String SRV_NAME = "RefreshService";  // Name can be used for debugging.
    private FriendDB social;

    public Refresh() {
        super(SRV_NAME);
    }

    @Override
    protected void onHandleIntent(Intent hint) {
        Account ghost = new Account(this, true);
        String timeNow = KTime.ParseNow(KTime.KT_fmtDate3339k).toString();

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
                ghost.token = "";
                ghost.device = FirebaseInstanceId.getInstance().getId();
            }

            // This is to make sure we stay in sync with the server.  At one point Azure had a 90 life for
            // token storage, so the date based cycle was to be a keep-alive, but now that is not the case.
            // Leaving in a once a month upload for now.
            if (ghost.token.length() == 0 || ghost.force ||
                    KTime.CalcDateDifference(ghost.tokenAge, timeNow, KTime.KT_fmtDate3339k, KTime.KT_DAYS) > 30) {
                // Get the latest token and save it locally (probably has not changed).
                ghost.token = FirebaseInstanceId.getInstance().getToken();
                ghost.tokenAge = timeNow;

                // Commit the changes and try to tell the server.
                ghost.force = false;
                ghost.SyncPrime(true, this);
            }
        } catch (ExpParseToCalendar ex) {
            // If there is a date problem, clear out the old value.
            ghost.tokenAge = timeNow;
            ghost.SyncPrime(false, this);
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".onHandleIntent");
        }

        // Check if the user is signed up yet.
        if (ghost.ticket.length() == 0 || ghost.acctId == 0) return;

        /*  Sync the social graph.  This will grab data off the server and then clean up
            the local database with any Deletes, Changes, Adds.  In that order.  For Adds,
            check if there is local contact information that can supplement the data.
         */
        try {
            social = new FriendDB(this);  // Be sure to close this before leaving the thread.
            // Check that valid account and not updating too often.
            if (ghost.ticket.length() > 0 || KTime.CalcDateDifference(ghost.friendAge, timeNow, KTime.KT_fmtDate3339k, KTime.KT_MINUTES) > 13) {
                WebServices ws = new WebServices();
                if (ws.IsNetwork(this)) {
                    Invitations invites = ws.GetFriends(ghost.ticket, ghost.acctIdStr());
                    if(invites.friends.size() == 0) return; // There is always 1 friend (yourself), if not something is wrong.
                    Account[] inviteStore = queryFriends();
                    CheckForDeletes(invites, inviteStore);
                    CheckForUpdates(invites, inviteStore);
                    CheckForAdditions(invites,  inviteStore);
                    UpdateContactInfo(inviteStore);
                    ghost.friendAge = timeNow;
                    ghost.SyncPrime(false, this);
                }
            }
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".onHandleIntent");
            // If there is a date problem, clear out the old value.
            ghost.tokenAge = timeNow;
            ghost.SyncPrime(false, this);
        } finally {
            social.close();
        }
    }

    /*
     *  The server side is authoritative, so check to see if any data has
     *  been added, so it can also be stored locally.
     *  Check each of the Invitations against all Accounts.
     */
    private void CheckForAdditions(Invitations server, Account[] local){
        List<Account> toAdd = new ArrayList<>();

        localLoop: for(InviteResponse invite : server.friends){
            for(Account acct : local){
                if(invite.friendId == acct.acctId)
                    continue localLoop;
            }
            toAdd.add(CopyInviteAcct(invite, true, false));
        }
        localLoop: for(InviteResponse invite : server.rsvps){
            for(Account acct : local){
                if(invite.friendId == acct.acctId)
                    continue localLoop;
            }
            toAdd.add(CopyInviteAcct(invite, false, true));
        }
        localLoop: for(InviteResponse invite : server.invites){
            for(Account acct : local){
                if(invite.friendId == acct.acctId)
                    continue localLoop;
            }
            toAdd.add(CopyInviteAcct(invite, false, false));
        }
        addFriends(toAdd);
    }

    // Copy server invite to build an account.
    private Account CopyInviteAcct(InviteResponse invite, boolean confirm, boolean sent){
        Account hold = new Account();
        hold.acctId = invite.friendId;
        hold.timezone = invite.timezone;
        hold.sleepcycle = invite.scycle;
        hold.unique = invite.fname;
        hold.display = invite.fdisplay;
        hold.mirror = invite.mirror;
        hold.pending = sent;
        hold.confirmed = confirm;
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
            for(InviteResponse invite : server.friends){
                if(acct.acctId == invite.friendId) {
                    if (invite.fname.equalsIgnoreCase(acct.unique) &&
                        invite.timezone.equalsIgnoreCase(acct.timezone) &&
                        invite.scycle == acct.sleepcycle &&
                        invite.fdisplay.equalsIgnoreCase(acct.display) &&
                        invite.mirror == acct.mirror && acct.confirmed) {
                        continue localLoop;
                    }
                    acct.unique = invite.fname;
                    acct.timezone = invite.timezone;
                    acct.sleepcycle = invite.scycle;
                    acct.display = invite.fdisplay;
                    acct.mirror = invite.mirror;
                    acct.confirmed = true;
                    acct.pending = false;
                    toChg.add(acct);
                }
            }
            for(InviteResponse invite : server.rsvps){
                if(acct.acctId == invite.friendId) {
                    if (invite.fname.equalsIgnoreCase(acct.unique) &&
                        invite.timezone.equalsIgnoreCase(acct.timezone) &&
                        invite.scycle == acct.sleepcycle &&
                        invite.fdisplay.equalsIgnoreCase(acct.display) &&
                        invite.mirror == acct.mirror && !acct.confirmed) {
                        continue localLoop;
                    }
                    acct.unique = invite.fname;
                    acct.timezone = invite.timezone;
                    acct.sleepcycle = invite.scycle;
                    acct.display = invite.fdisplay;
                    acct.mirror = invite.mirror;
                    acct.confirmed = false;
                    acct.pending = true;
                    toChg.add(acct);
                }
            }
            for(InviteResponse invite : server.invites){
                if(acct.acctId == invite.friendId) {
                    if (invite.fname.equalsIgnoreCase(acct.unique) &&
                        invite.timezone.equalsIgnoreCase(acct.timezone) &&
                        invite.scycle == acct.sleepcycle &&
                        invite.fdisplay.equalsIgnoreCase(acct.display) &&
                        invite.mirror == acct.mirror && !acct.confirmed) {
                        continue localLoop;
                    }
                    acct.unique = invite.fname;
                    acct.timezone = invite.timezone;
                    acct.sleepcycle = invite.scycle;
                    acct.display = invite.fdisplay;
                    acct.mirror = invite.mirror;
                    acct.confirmed = false;
                    acct.pending = false;
                    toChg.add(acct);
                }
            }
        }
        chgFriends(toChg);
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
            for(InviteResponse invite : server.friends){
                if(acct.acctId == invite.friendId)
                    continue localLoop;
            }
            for(InviteResponse invite : server.rsvps){
                if(acct.acctId == invite.friendId)
                    continue localLoop;
            }
            for(InviteResponse invite : server.invites){
                if(acct.acctId == invite.friendId)
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
    private boolean UpdateContactInfo(Account[] local){
        List<Account> toChg = new ArrayList<>();

        for(Account acct : local){
            if(acct.contactId.length() == 0){
                Account holdContact;
                if(acct.isEmail()) {
                    holdContact = getContactByEmail(acct.unique);
                }else{
                    holdContact = getContactByPhone(acct.unique);
                }
                if(holdContact.contactName.length() > 0) {
                    acct.contactId = holdContact.contactId;
                    acct.contactName = holdContact.contactName;
                    acct.contactPic = holdContact.contactPic;
                    toChg.add(acct);
                }
            }
        }
        chgFriends(toChg);

        return (toChg.size()>0);
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
                Long id = contact.getLong(contact.getColumnIndex(ContactsContract.PhoneLookup._ID));
                holdContact.contactId = id.toString();
                holdContact.contactName = contact.getString(contact.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                holdContact.contactPic = contact.getString(contact.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI));
            }
            contact.close();
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".getContactByPhone");
            if (contact != null) contact.close();
        }
        return holdContact;
    }

    // Find the contact information for the email address.
    private Account getContactByEmail(String email) {
        Account holdContact = new Account();
        Cursor contact = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Email.CONTENT_FILTER_URI, Uri.encode(email));
        String[] selection = {ContactsContract.Data.CONTACT_ID, ContactsContract.Data.DISPLAY_NAME, ContactsContract.Data.PHOTO_THUMBNAIL_URI};

        try {
            contact = getContentResolver().query(uri, selection, null, null, null);
            if (contact == null || contact.getCount() == 0) return holdContact;
            while (contact.moveToNext()) {
                Long id = contact.getLong(contact.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                holdContact.contactId = id.toString();
                holdContact.contactName = contact.getString(contact.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                holdContact.contactPic = contact.getString(contact.getColumnIndex(ContactsContract.Data.PHOTO_THUMBNAIL_URI));
            }
            contact.close();
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".getContactByEmail");
            if (contact != null) contact.close();
        }
        return holdContact;
    }

    // Retrieve any known friends and invites from the local DB.
    private Account[] queryFriends(){
        SQLiteDatabase db = social.getReadableDatabase();
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
                dataPoints[i++] = local;
            }
            cursor.close();
            return dataPoints;
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".queryFriends"); return new Account[0]; }
    }

    // Retrieve a specific friend/invite from the local DB.
    private Account queryFriend(String id){
        // Check if the local user
        if(id.equalsIgnoreCase(Owner_DBID)) return new Account(this, true);

        // Look up the friend
        FriendDB social = new FriendDB(this);  // Be sure to close this before leaving the thread.
        SQLiteDatabase db = social.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_FriendExact.replace(SUB_ZZZ, id), filler);
        try{
            Account local = new Account();
            if(cursor.getCount() > 0) {
                cursor.moveToNext();
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
            }
            cursor.close();
            return local;
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".queryFriends"); return new Account(); }
        finally { social.close(); }
    }

    // Add any new friends and invitations.
    private void addFriends(List<Account> items) {
        SQLiteDatabase db = social.getWritableDatabase();

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
        SQLiteDatabase db = social.getWritableDatabase();

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
        SQLiteDatabase db = social.getWritableDatabase();

        for(String key : keys) {
            String where = "_id = " + key;
            String[] filler = {};
            db.delete(FriendDB.FRIEND_TABLE, where, filler);
        }
    }

}


