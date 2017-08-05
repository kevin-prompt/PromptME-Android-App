package com.coolftc.prompt;

import java.io.Serializable;
import java.util.Comparator;

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
    public String contactSur = "";  // The local contact last (sur) name.
    public String tag = "";         // Area to store temporary identifier.
    public boolean mirror = false;  // The friend is a special mirror type.
    public boolean pending = false; // When true, it means pending confirmation from local user.
    public boolean confirmed = false;  // If confirmed, reminders can be sent.
    public boolean primary = false; // When true, this account is the primary user.

    // Constructors
    public Account(){}

    // Helper methods
    public String acctIdStr(){ return Long.toString(acctId); }
    public String contactPicUri(){ return contactPic!=null ? contactPic : ""; }
    public String bestName(){ return display.length()>0 ? display : contactName; }
    public String bestNameAlt(){ return confirmed && contactName.length() > 0 ? contactName : unique; }
    public String bestId(){ return localId.length()>0 ? localId : contactId; }
    public boolean isEmail(){ return unique.contains("@"); }
    public String sleepcycleStr(){ return Integer.toString(sleepcycle);}
    public String cleanUnique(){ return isEmail() ? unique : unique.replaceAll("[^0-9]", ""); }

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
     *  Helps sort by sur name.
     *  The comparison returns a 1 if r1 is earlier in the alphabet (A to Z value),
     *  and -1 if r1 is later. By convention, a sur name that starts with a digit
     *  is sorted to the end.
     */
    public static Comparator<Account> ByLastFirstName = new Comparator<Account>() {
        public int compare(Account r1, Account r2) {
            if (r1 == null || r2 == null) return 0;
            if (r1.contactName != null && r1.contactSur != null && r2.contactName != null && r2.contactSur != null) {
                if (r2.contactSur.length() == 0 || Character.isDigit(r2.contactSur.charAt(0))) return -1;
                //if (r1.contactSur.length() == 0 || Character.isDigit(r1.contactSur.charAt(0))) return -1;
                //ascending order
                return (r1.contactSur+r1.contactName).compareTo(r2.contactSur+r2.contactName);
            }
            return 0;  // indeterminate
        }
    };
}
