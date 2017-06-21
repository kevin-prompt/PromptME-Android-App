package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Contact Picker is used to select who a prompt is to be sent to, at the
 * appropriate time.  The list contains first, all people who can be sent a
 * prompt (connections), then a delimiter, followed by a list of all people invited as
 * connections but who have not accepted.  Finally another delimiter and all other
 * people in the local contacts.  The idea is that the last group is available
 * to invite to the network.
 *
 * The first person on the list is the primary.
 */
public class ContactPicker extends AppCompatActivity {

    // The contact list.
    ListView mListView;
    // The search box.
    EditText contactSearch;
    // The contact permission is stored to reduce management overhead.
    int contactPermissionCheck;
    // The "accounts" collect all the possible people to display.
    List<Account> accounts = new ArrayList< >();
    // This is the mapping of the details map to each specific person.
    String[] StatusMapFROM = {CP_PER_ID, CP_TYPE, CP_NAME, CP_EXTRA, CP_FACE};
    int[] StatusMapTO = {R.id.rowp_Id, R.id.rowpType, R.id.rowpContactName, R.id.rowpContactExtra, R.id.rowpFacePic};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI
        setContentView(R.layout.contactpicker);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        contactSearch = (EditText) findViewById(R.id.txtSearch_CP);
        mListView = (ListView) findViewById(R.id.listContacts_CP);
        contactPermissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS);
        ShowDetails("");

        // As a user types in characters, trim the contact list.
        contactSearch.addTextChangedListener(new TextWatcher() {
            // The "filter" contains all that has been typed into search.  Might want a debounce on this, too.
            // For example, ignore this event if 1 second has not elapsed since the last time it was fired.
            @Override
            public void onTextChanged(CharSequence filter, int arg1, int arg2, int arg3) { ShowDetailsCache(filter.toString()); }
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
            @Override
            public void afterTextChanged(Editable arg0) { }
        });

    }

    /*
     *  This is a process of collecting all the possible contacts to display,
     *  then filtering those into 4 sections.  Duplicates are removed and if
     *  a search term is supplied, that is applied as well (although for
     *  search it is better to use ShowDetailsCache).
     */
    private void ShowDetails(String search) {
        // This is a new list from scratch.  This is a global list (for this class), so
        // it can be used later by other methods.
        accounts = new ArrayList<>();

        // First Step is to grab the primary user, which is stored in the preferences.
        AddDelimitRow(R.string.contact_pri);
        accounts.add(new Actor(this));

        // Second Step is to get all friends and invites, stored locally (with periodic updates).
        LoadFriends(FriendDB.SQLITE_TRUE);
        AddDelimitRow(R.string.contact_inv);
        LoadFriends(FriendDB.SQLITE_FALSE);

        // Final Step is to collect all the existing contacts on the device.
        if (contactPermissionCheck == PackageManager.PERMISSION_GRANTED) {
            AddDelimitRow(R.string.contact_all);
            LoadContacts();
        } else {
            // If permission has not been granted, ask for it.  Continue to display what you have.
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS}, SEC_READ_CONTACTS);
        }

        // Once the accounts are populated, go ahead and use the cache processor.
        ShowDetailsCache(search);
    }

    /*
     *  This is an optimization on the ShowDetails(), in that it just uses the existing
     *  list of accounts.  This make the type ahead search much quicker, but will not
     *  pick up any changes to the list.
     */
    private void ShowDetailsCache(String search) {
        // The "uniques" hold all the unique names that are on the list
        Map<String, Boolean> uniques = new HashMap<>();

        // The "details" hold the final data sent to the display list.
        List<Map<String, String>> details = new ArrayList<>();

        // Move the account data into the desired details format.
        for(Account acct : accounts) {
            // Sometimes we want to skip displaying records.
            if (uniques.containsKey(acct.unique)) continue;
            if (!acct.Found(search)) continue;

            Map<String, String> hold = new TreeMap<>();

            // Specify which display format to use and save off the unique.
            if (acct.tag.equalsIgnoreCase(TITLE_ROW)) {
                hold.put(CP_PER_ID, "0");
            } else {
                hold.put(CP_PER_ID, acct.bestId());
                uniques.put(acct.unique, true);
            }
            // Copy the data to display.
            hold.put(CP_TYPE, acct.localId);
            hold.put(CP_NAME, acct.bestName());
            hold.put(CP_EXTRA, acct.unique);
            hold.put(CP_FACE, acct.contactPicUri());
            details.add(hold);
        }

        ContactAdapter adapter = new ContactAdapter(this, details, R.layout.contactpicker_row, StatusMapFROM, StatusMapTO);
        mListView.setAdapter(adapter);
    }

    /*
     *  Adds a descriptive row to the list
     */
    private void AddDelimitRow(int resourceId){
        Account delimtFriend = new Account();
        delimtFriend.display = getResources().getString(resourceId);
        delimtFriend.tag = TITLE_ROW;
        accounts.add(delimtFriend);
    }

    /*
     * This reads all the local friends/invites into an account list.
     */
    private void LoadFriends(Integer friendType){
        FriendDB social = new FriendDB(this);  // Be sure to close this before leaving the thread.
        SQLiteDatabase db = social.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_FriendsWhere.replace(SUB_ZZZ, friendType.toString()), filler);
        try{
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
                accounts.add(local);
            }
            cursor.close();
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".queryFriends"); }
        finally { social.close(); }
    }

    /*
     * This reads all the contacts and grabs the desired fields.  Phone numbers need a little extra love.
     */
    private void LoadContacts(){
        Cursor contact = null;
        String[] selection = {ContactsContract.Data.CONTACT_ID, ContactsContract.Data.DISPLAY_NAME, ContactsContract.Contacts.Data.DATA1, ContactsContract.Data.MIMETYPE, ContactsContract.Contacts.PHOTO_THUMBNAIL_URI};
        try{
            contact = getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    selection,
                    ContactsContract.Contacts.Data.MIMETYPE + "=? OR " + ContactsContract.Contacts.Data.MIMETYPE + "=?",
                    new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
                    ContactsContract.Data.CONTACT_ID);

            if(contact == null || contact.getCount() == 0) return;
            PhoneNumberUtil phoneHelper = PhoneNumberUtil.getInstance();
            while (contact.moveToNext()) {
                Account holdAcct = new Account();
                Long id = contact.getLong(contact.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                holdAcct.contactId = id.toString();
                holdAcct.contactName = contact.getString(contact.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String mimetype = contact.getString(contact.getColumnIndex(ContactsContract.Data.MIMETYPE));
                holdAcct.unique = contact.getString(contact.getColumnIndex(ContactsContract.Contacts.Data.DATA1));
                if(mimetype.equalsIgnoreCase(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)){
                    Phonenumber.PhoneNumber fullNbr = phoneHelper.parse(holdAcct.unique, "US");
                    String holdnbr = phoneHelper.format(fullNbr, PhoneNumberUtil.PhoneNumberFormat.E164);
                    holdAcct.unique = holdnbr.replace("+", "");
                }
                holdAcct.contactPic = contact.getString(contact.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
                accounts.add(holdAcct);
            }
            contact.close();
        }catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".GetContactList");
            if(contact != null) contact.close();
        }
    }

    /*
     * This provides a custom handling of the list of contacts.  For example, the list separators have a
     * different layout than the actual contacts and some contacts are intentionally skipped.
     * NOTE: Be sure to use match_parent (or specific values) for the height and width of the ListView
     * and rows. Otherwise the getView is called A LOT! since it has to guess at sizing.
     */
    private class ContactAdapter extends SimpleAdapter {
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_SEPARATOR = 1;
        private static final int TYPE_MAX_COUNT = 2;

        private LayoutInflater mInflater;

        public ContactAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
            mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_MAX_COUNT;
        }

        @Override
        public int getItemViewType(int position) {
            try {
                // NOTE: Unchecked cast is ok if you use the data close by,
                // since that is what will trigger the runtime exception.
                @SuppressWarnings("unchecked")
                Map<String, String> holdData = (Map<String, String>) getItem(position);
                switch (holdData.get(CP_PER_ID)) {
                    case "0": // As a convention, the separators are given a zero (0) id.
                        return TYPE_SEPARATOR;
                    default:
                        return TYPE_ITEM;
                }
            } catch (Exception ex) {
                ExpClass.LogEX(ex, this.getClass().getName() + ".GetContactList");
                return TYPE_SEPARATOR; // safest option
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            TextView holdView;
            ImageView holdPic;
            // ***May want to try "parent, false" instead of "null" to remove lint warning***
            try {
                if (convertView == null) {
                    switch (type) {
                        case TYPE_ITEM:
                            convertView = mInflater.inflate(R.layout.contactpicker_row, null);
                            break;
                        case TYPE_SEPARATOR:
                            convertView = mInflater.inflate(R.layout.contactdelimit_row, null);
                            break;
                    }
                }

                if (convertView != null) {
                    // NOTE: Unchecked cast is ok if you use the data close by,
                    // since that is what will trigger the runtime exception.
                    @SuppressWarnings("unchecked")
                    Map<String, String> holdData = (Map<String, String>) getItem(position);
                    holdView = (TextView) convertView.findViewById(R.id.rowp_Id);
                    holdView.setText(holdData.get(CP_PER_ID));
                    switch (type) {
                        case TYPE_ITEM:
                            holdView = (TextView) convertView.findViewById(R.id.rowpType);
                            holdView.setText(holdData.get(CP_TYPE));
                            holdView = (TextView) convertView.findViewById(R.id.rowpContactName);
                            holdView.setText(holdData.get(CP_NAME));
                            holdView = (TextView) convertView.findViewById(R.id.rowpContactExtra);
                            holdView.setText(holdData.get(CP_EXTRA));
                            if (holdData.get(CP_FACE).length() > 0) {
                                holdPic = (ImageView) convertView.findViewById(R.id.rowpFacePic);
                                holdPic.setImageURI(Uri.parse(holdData.get(CP_FACE)));
                            } else {
                                holdPic = (ImageView) convertView.findViewById(R.id.rowpFacePic);
                                holdPic.setImageResource(R.drawable.contactdoe_26);
                            }
                            break;
                        case TYPE_SEPARATOR:
                            holdView = (TextView) convertView.findViewById(R.id.rowpDelimitName);
                            holdView.setText(holdData.get(CP_NAME));
                            break;
                    }
                }
                return convertView;
            }catch(Exception ex) {
                ExpClass.LogEX(ex, this.getClass().getName() + ".GetContactList");
                return null;
            }
        }
    }

    /*
     * Reload the Contacts, now that we can use all of them, since used agreed to permission.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case SEC_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    contactPermissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS);
                    ShowDetails("");
                }
            }
        }
    }

    public void pickOnClick(View view) {
        try {
            switch (view.getId()) {
                case R.id.rowpItem:
                    // Get the important data out of the row.
                    TextView holdView;
                    holdView = (TextView) view.findViewById(R.id.rowpContactExtra);
                    String uSelect = holdView.getText().toString();

                    // Find the matching Account.
                    Account holdAcct = null;
                    for(Account acct : accounts) {
                        if (acct.unique.equalsIgnoreCase(uSelect)) {
                            holdAcct = acct;
                            break;
                        }
                    }
                    if(holdAcct == null) break;

                    // Navigate to message entry
                    if(holdAcct.confirmed){
                        Intent intent = new Intent(this, Entry.class);
                        Bundle mBundle = new Bundle();
                        mBundle.putSerializable(IN_USER_ACCT, holdAcct);
                        intent.putExtras(mBundle);
                        startActivity(intent);
                    }

                    // Accept invitation request or send out an invitation.
                    if(!holdAcct.confirmed){
                        if(holdAcct.pending){
                            // This means you need to accept them as connection


                        }else{
                            // Send an invitation or something.
                            // - add a dialog to confirm
                            // - kick off a thread for this
                            // - to update contact, will need service to have found invite data and contacts to have reloaded it.
                        }
                    }
                    break;
            }
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".pickOnClick");
        }
    }
}
