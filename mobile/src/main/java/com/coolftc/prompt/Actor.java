package com.coolftc.prompt;

import android.content.Context;
import android.content.SharedPreferences;

import com.coolftc.prompt.source.WebServices;
import com.coolftc.prompt.utility.ExpClass;

import java.util.TimeZone;
import static com.coolftc.prompt.utility.Constants.*;
import static com.coolftc.prompt.source.WebServiceModels.*;

/**
 * The Actor is the person using the app.  They have many of the same
 * properties as other people that might get referenced in the app, e.g.
 * contacts, but have some special functionality as well.  Specifically,
 * their data is retained in the preference DB for easy and quick access.
 */
public class Actor extends Account {

    // These are items specific to the person using the app.
    public String ticket = "";      // The key used to access web services.
    public String token = "";       // The push token (GCM).
    public String device = "";      // The unique device identifier.
    public int notesWaiting = 0;    // The number of expected future prompts (cached here)
    public boolean force = false;   // When true the data duplicated on server has changed
    // These are also stored on the server.
    public String custom = "";      // The custom, third party identifier.
    public String timezoneExt = ""; // The time zone saved on the server.
    public boolean ads = true;      // Should ads be displayed on the app.
    public boolean broadcast = false; // Is this account broadcast only

    /*
     *  Constructors
     */
    public Actor () {};
    public Actor (Context ctx) { LoadPrime(false, ctx); }

    // Helpers
    public String isBroadcast(Context ctx) { return broadcast ? ctx.getResources().getString(R.string.yes):ctx.getResources().getString(R.string.no);}
    public String isAds(Context ctx) { return ads ? ctx.getResources().getString(R.string.yes):ctx.getResources().getString(R.string.no);}

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
        device = registered.getString(SP_REG_GCM_ID, "");
        timezone = TimeZone.getDefault().getID();
        sleepcycle = registered.getInt(SP_REG_SCYCLE, 0);
        contactId = registered.getString(SP_REG_CTID, "");
        contactName = registered.getString(SP_REG_CTNAME, "");
        contactPic = registered.getString(SP_REG_CTFACE, "");
        localId = registered.getString(SP_REG_DBID, "");
        notesWaiting = registered.getInt(SP_REG_PEND, 0);
        confirmed = registered.getBoolean(SP_REG_CONFIRM, false);
        force = registered.getBoolean(SP_REG_FORCE, false);
        isFriend = true;
        primary = true;

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
                broadcast = user.broadcast;
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
        editor.putString(SP_REG_UNIQUE, cleanUnique());
        editor.putString(SP_REG_DISPLAY, display);
        editor.putInt(SP_REG_SCYCLE, sleepcycle);
        editor.putString(SP_REG_GCM, token);
        editor.putString(SP_REG_GCM_ID, device);
        editor.putString(SP_REG_CTID, contactId);
        editor.putString(SP_REG_CTNAME, contactName);
        editor.putString(SP_REG_CTFACE, contactPicUri());
        editor.putString(SP_REG_DBID, localId);
        editor.putInt(SP_REG_PEND, notesWaiting);
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
                UserResponse data = ws.ChgUser(ticket, acctIdStr(), user);
                if (data.response < 200 || data.response >= 300)
                    ExpClass.LogIN(KEVIN_SPEAKS, "Account.SyncPrime server fail response = " + Integer.toString(data.response));
            } else
                ExpClass.LogIN(KEVIN_SPEAKS, "Account.SyncPrime Network Unavailable");
        }
    }

}
