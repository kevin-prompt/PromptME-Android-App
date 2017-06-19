package com.coolftc.prompt;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.Serializable;
import java.util.TimeZone;
import static com.coolftc.prompt.Constants.*;
import static com.coolftc.prompt.WebServiceModels.*;

/**
 * Represents information about any people in the application.  Not all fields
 * need apply to all types of users.  For example, there is much less info
 * populated for a connection, and the primary user would not explicitly
 * store a time zone.  To help separate these differences out, the Actor
 * class does subclass from here.
 *
 * This class can be used as an in-memory store of any user, but the LoadPrime
 * and SyncPrime methods are only for the actual primary user.
 *
 * This class implements Serializable to allow easy movement between screens
 * using intents.  This means it cannot contain things that are not serializable.
 */
public class Account implements Serializable {

    private static final long serialVersionUID = 5264124489360369638L; // Required for serialization.
    // This subset can also be stored in the Friend DB.
    public String localId = "";     // The local db id of the record.
    public long acctId = 0;         // The id of the user in the Registry (on server).
    public String unique = "";      // The unique name, e.g. phone # / email addr.
    public String display = "";     // The display name.
    public int sleepcycle = 2;      // The chosen sleep cycle of the user.
    public String timezone = "";    // The local timezone.
    public String contactId = "";   // The local contact id of the user.
    public String contactName = ""; // The local contact name of the user.
    public String contactPic = "";  // The local contact thumbnail uri.
    public String tag = "";         // Area to store temporary identifier
    public boolean mirror = false;  // The friend is a special mirror type
    public boolean pending = false; // When true, it means pending confirmation from local user
    public boolean confirmed = false;  // If confirmed, reminders can be sent.

    // Constructors
    public Account(){}

    // Helper methods
    public String acctIdStr(){ return Long.toString(acctId); }
    public String contactPicUri(){ return contactPic!=null ? contactPic : ""; }
    public String bestName(){ return contactName.length()>0 ? contactName : display; }
    public String bestId(){ return localId.length()>0 ? localId : contactId; }
    public boolean isEmail(){ return unique.contains("@"); }
    public String sleepcycleStr(){ return Integer.toString(sleepcycle);}

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

}
