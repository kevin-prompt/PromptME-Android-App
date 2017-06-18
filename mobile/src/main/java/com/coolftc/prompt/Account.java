package com.coolftc.prompt;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.Serializable;
import java.util.TimeZone;
import static com.coolftc.prompt.Constants.*;
import static com.coolftc.prompt.WebServiceModels.*;

/**
 * Represents information about any user in the application.  Not all fields
 * need apply to all types of users.  For example, there is much less info
 * populated for a connection, and the primary user would not explicitly
 * store a time zone.
 *
 * This class can be used as an in-memory store of any user, but the LoadPrime
 * and SyncPrime methods are only for the actual primary user.
 *
 * This class implements Serializable to allow easy movement between screens
 * using intents.  This means it cannot contain things that are not serializable.
 */
public class Account implements Serializable {

    private static final long serialVersionUID = 5264124489360369638L; // Required for serialization.
    // This group is stored in shared preferences.
    public String ticket = "";      // The key used to access web services.
    public String token = "";       // The push token (GCM).
    public String tokenAge = "";    // The timestamp of the last token check
    public String device = "";      // The unique device identifier.
    public boolean force = false;   // When true the data duplicated on server has changed
    // This subset can also be stored in the Friend DB.
    public String localId = "";     // The local db id of the record.
    public long acctId = 0;         // The id of the user in the Registry (on server).
    public String unique = "";      // The unique name, e.g. phone # / email addr.
    public String display = "";     // The display name.
    public String timezone = "";    // The local timezone.
    public String contactId = "";   // The local contact id of the user.
    public String contactName = ""; // The local contact name of the user.
    public String contactPic = "";  // The local contact thumbnail uri.
    public boolean mirror = false;  // The friend is a special mirror type
    public boolean pending = false; // When true, it means pending confirmation from local user
    public boolean confirmed = false;  // If confirmed, reminders can be sent.

    // This group is stored on the server.
    public String custom = "";      // The custom, third party identifier.
    public String timezoneExt = ""; // The time zone saved on the server.
    public int sleepcycle = 2;      // The chosen sleep cycle of the user.
    public boolean ads = true;      // Should ads be displayed on the app.

    // Constructors
    public Account(){}
    public Account (Context ctx, boolean load) { if(load) LoadPrime(false, ctx); }

    // Helper methods
    public String acctIdStr(){ return Long.toString(acctId); }
    public String sleepcycleStr(){ return Integer.toString(sleepcycle);}
    public String contactPicUri(){ return contactPic!=null ? contactPic : ""; }
    public String bestName(){ return contactName.length()>0 ? contactName : display; }
    public String bestId(){ return localId.length()>0 ? localId : contactId; }
    public boolean isEmail(){ return unique.contains("@"); }

    /*
     *  This method will search a few of the fields to determine if the
     *  search "term" is contained in it.  It is a helper to aid in the
     *  determination if the account is of interest. Using toLowerCase
     *  make it case insensitive.
     */
    public boolean Found(String term){
        String lowterm = term.toLowerCase();
        return (term.length() == 0 || unique.toLowerCase().contains(lowterm) || display.toLowerCase().contains(lowterm) || contactName.toLowerCase().contains(lowterm));
    }

    /*
     * For the primary user, this method loads the relevant data from the
     * local store (shared preferences).  Additionally, we can go to the
     * server as well if full=true.
     * NOTE: Any use of the network would require the calling party to
     * not be on the main thread when called.
     */
    public void LoadPrime(boolean full, Context context){

        // LoadPrime the local data.
        SharedPreferences registered = context.getSharedPreferences(SP_REG_STORE, Context.MODE_PRIVATE);
        acctId = registered.getLong(SP_REG_ID, 0);
        ticket = registered.getString(SP_REG_TICKET, "");
        unique = registered.getString(SP_REG_UNIQUE, "");
        display = registered.getString(SP_REG_DISPLAY, "");
        token = registered.getString(SP_REG_GCM, "");
        tokenAge = registered.getString(SP_REG_GCM_AGE, KTime.ParseNow(KTime.KT_fmtDate3339k).toString());
        device = registered.getString(SP_REG_GCM_ID, "");
        timezone = TimeZone.getDefault().getID();
        sleepcycle = registered.getInt(SP_REG_SCYCLE, 0);
        contactId = registered.getString(SP_REG_CTID, "");
        contactName = registered.getString(SP_REG_CTNAME, "");
        contactPic = registered.getString(SP_REG_CTFACE, "");
        localId = registered.getString(SP_REG_DBID, "");
        confirmed = registered.getBoolean(SP_REG_CONFIRM, false);
        force = registered.getBoolean(SP_REG_FORCE, false);

        if(full) {
            // LoadPrime the server data, too.
            WebServices ws = new WebServices();
            if(ws.IsNetwork(context)) {
                UserResponse user = ws.GetUser(ticket, Long.toString(acctId));
                custom = user.cname;
                ads = user.ads;
                confirmed = user.verified;
                timezoneExt = user.timezone;
                sleepcycle = user.scycle;
            }
            else
                ExpClass.LogIN(KEVIN_SPEAKS, "Account.LoadPrime Network Unavailable");
        }
    }

    /*
     * For the primary user, this method saves the relevant data to the
     * local store (shared preferences).  Additionally, we put it to the
     * server as well if full=true.
     * NOTE: Any use of the network would require the calling party to
     * NOT be on the main thread when called.  This method is synchronized
     * in case it gets called in multiple places in the code.
     */
    public synchronized void SyncPrime(boolean full, Context context) {

        // Save the local data.  It is possible for this method to be called
        SharedPreferences registered = context.getSharedPreferences(SP_REG_STORE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = registered.edit();
        editor.putLong(SP_REG_ID, acctId);
        editor.putString(SP_REG_TICKET, ticket);
        editor.putString(SP_REG_UNIQUE, unique);
        editor.putString(SP_REG_DISPLAY, display);
        editor.putInt(SP_REG_SCYCLE, sleepcycle);
        editor.putString(SP_REG_GCM, token);
        editor.putString(SP_REG_GCM_AGE, tokenAge);
        editor.putString(SP_REG_GCM_ID, device);
        editor.putString(SP_REG_CTID, contactId);
        editor.putString(SP_REG_CTNAME, contactName);
        editor.putString(SP_REG_CTFACE, contactPicUri());
        editor.putString(SP_REG_DBID, localId);
        editor.putBoolean(SP_REG_CONFIRM, confirmed);
        editor.putBoolean(SP_REG_FORCE, force);
        editor.apply();


        if (full && ticket.length() > 0) {
            // Save data to the server, too.
            WebServices ws = new WebServices();
            if (ws.IsNetwork(context)) {
                UserRequest user = new UserRequest();
                user.dname = display;
                user.timezone = TimeZone.getDefault().getID();
                user.target = token;
                user.scycle = sleepcycle;
                user.type = FTI_TYPE_ANDROID;
                UserResponse data = ws.ChgUser(ticket, Long.toString(acctId), user);
                if (data.response < 200 || data.response >= 300)
                    ExpClass.LogIN(KEVIN_SPEAKS, "Account.SyncPrime server fail response = " + Integer.toString(data.response));
            } else
                ExpClass.LogIN(KEVIN_SPEAKS, "Account.SyncPrime Network Unavailable");
        }
    }
}
