package com.coolftc.prompt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.coolftc.prompt.source.UserRequest;
import com.coolftc.prompt.source.UserResponse;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import static com.coolftc.prompt.utility.Constants.*;

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
    public boolean solo = false;    // Account limited to self-reminders

    /*
     *  Constructors
     */
    public Actor () {}
    public Actor (Context ctx) { LoadPrime(false, ctx); }

    // Helpers
    public String isBroadcast(Context ctx) { return broadcast ? ctx.getResources().getString(R.string.yes):ctx.getResources().getString(R.string.no);}
    public String isAds(Context ctx) { return ads ? ctx.getResources().getString(R.string.yes):ctx.getResources().getString(R.string.no);}
    public String identifier() {    // Google keeps dumping their own unique ids, so build one.
        final String DEVICE_LAYOUT = "%s::%s::%s::%s::%d";
        return String.format(Locale.US, DEVICE_LAYOUT,
                max50(Build.MANUFACTURER), max50(Build.MODEL), max50(Build.DEVICE), max50(Build.ID), someNbr())
                .replaceAll("\\s", "_");
    }
    private String max50(String in) { return in.length() > 50 ? in.substring(0, 50) : in.trim(); }
    private int someNbr() { return new Random().nextInt(1000000000 - 100000000) + 100000000; }
    /*
     * For the primary user, this method loads the relevant data from the local
     * store (shared preferences).  Additionally, it can go to the server, although
     * that should not be used to change local users settings like sleep cycle.
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
        solo = registered.getBoolean(SP_REG_SOLO, false);
        isFriend = true;
        primary = true;

        if(full) {
            // LoadPrime the server data, too.
            try (Connection net = new Connection(context)) {
                WebServices ws = new WebServices(new Gson());
                if (net.isOnline()) {
                    String realPath = ws.baseUrl(context) + FTI_RegisterExtra.replace(SUB_ZZZ, acctIdStr());
                    UserResponse user = ws.callGetApi(realPath, UserResponse.class, ticket);
                    if(user != null) {
                        custom = user.getCname();
                        ads = user.getAds();
                        confirmed = user.getVerified();
                        timezoneExt = user.getTimezone();
                        sleepcycle = user.getScycle();
                        broadcast = user.getBroadcast();
                    }
                }
            else
                ExpClass.Companion.logINFO(KEVIN_SPEAKS, "Account.LoadPrime Network Unavailable");
            } catch (Exception ex) {
                ExpClass.Companion.logEX(ex, this.getClass().getName() + ".LoadPrime");
            }
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
        editor.putBoolean(SP_REG_SOLO, solo);
        editor.apply();

        if (full && ticket.length() > 0) {
            // Save data to the server, too.
            try (Connection net = new Connection(context)) {
                WebServices ws = new WebServices(new Gson());
                if (net.isOnline()) {
                    UserRequest user = new UserRequest(
                            TimeZone.getDefault().getID(),
                            display,
                            sleepcycle,
                            token,
                            FTI_TYPE_ANDROID
                    );
                    String realPath = ws.baseUrl(context) + FTI_RegisterExtra.replace(SUB_ZZZ, acctIdStr());
                    ws.callPostApi(realPath, user, UserResponse.class, ticket);
                } else
                    ExpClass.Companion.logINFO(KEVIN_SPEAKS, "Account.SyncPrime Network Unavailable");
            } catch (Exception ex) {
                ExpClass.Companion.logEX(ex, this.getClass().getName() + ".SyncPrime");
            }
        }
    }
}
