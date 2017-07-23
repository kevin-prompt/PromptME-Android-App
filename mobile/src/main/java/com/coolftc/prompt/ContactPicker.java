package com.coolftc.prompt;

import static com.coolftc.prompt.utility.Constants.*;

import android.app.Fragment;
import android.app.FragmentManager;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.coolftc.prompt.service.SendInviteThread;
import com.coolftc.prompt.source.FriendDB;
import com.coolftc.prompt.source.WebServices;
import com.coolftc.prompt.utility.ExpClass;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The Contact Picker is used to select whom is to be sent a prompt .  The list contains
 * all people who can be sent a prompt (connections), then a delimiter, followed by a
 * list of all people invited as connections but who have not accepted.  Finally another
 * delimiter and all other people in the local contacts.  The idea is that the last group
 * is available to invite to the network.
 *
 * The first person on the list is the primary user.
 */
public class ContactPicker extends AppCompatActivity implements FragmentTalkBack {

    // The contact list.
    ListView mListView;
    // The search box.
    EditText mContactSearch;
    // The contact permission is stored to reduce management overhead.
    int contactPermissionCheck;
    // The "mAccounts" collect all the possible people to display.
    List<Account> mAccounts = new ArrayList< >();
    // This is the mapping of the detail map to each specific person.
    String[] StatusMapFROM = {CP_PER_ID, CP_TYPE, CP_NAME, CP_EXTRA, CP_FACE};
    int[] StatusMapTO = {R.id.rowp_Id, R.id.rowpType, R.id.rowpContactName, R.id.rowpContactExtra, R.id.rowpFacePic};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI
        setContentView(R.layout.contactpicker);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mContactSearch = (EditText) findViewById(R.id.txtSearch_CP);
        mListView = (ListView) findViewById(R.id.listContacts_CP);
        contactPermissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS);
        ShowDetails("");

        // As a user types in characters, trim the contact list.
        mContactSearch.addTextChangedListener(new TextWatcher() {
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
        mAccounts = new ArrayList<>();

        // First Step is to grab the primary user, which is stored in the preferences.
        AddDelimitRow(R.string.contact_pri);
        mAccounts.add(new Actor(this));

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

        // Once the mAccounts are populated, go ahead and use the cache processor.
        ShowDetailsCache(search);
    }

    /*
     *  This is an optimization on the ShowDetails(), in that it just uses the existing
     *  list of mAccounts.  This make the type ahead search much quicker, but will not
     *  pick up any changes to the list.
     */
    private void ShowDetailsCache(String search) {
        // The "uniques" hold all the unique names that are on the list
        Map<String, Boolean> uniques = new HashMap<>();

        // The "detail" hold the final data sent to the display list.
        List<Map<String, String>> details = new ArrayList<>();

        // Move the account data into the desired detail format.
        for(Account acct : mAccounts) {
            // Sometimes we want to skip displaying records.
            if (uniques.containsKey(acct.bestName())) continue;
            if (!acct.Found(search)) continue;

            Map<String, String> hold = new TreeMap<>();

            // Specify which display format to use and save off the unique.
            if (acct.tag.equalsIgnoreCase(TITLE_ROW)) {
                hold.put(CP_PER_ID, "0");
            } else {
                hold.put(CP_PER_ID, acct.bestId());
                uniques.put(acct.bestName(), true);
            }
            // Copy the data to display.
            hold.put(CP_TYPE, acct.localId);
            hold.put(CP_NAME, acct.bestName());
            hold.put(CP_EXTRA, acct.bestNameAlt());
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
        mAccounts.add(delimtFriend);
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

    /*
     *  Clicking on a delimiter row, just ignore.
     */
    public void delimitClick(View view){
        switch (view.getId()) {
            case R.id.rowpItem:
                break;
        }
    }

    /*
     *  If the user want to invite someone that is not in contacts, this is the manual way.
     */
    public void JumpInvite(View view){
        Intent intent = new Intent(this, Invite.class);
        startActivity(intent);
    }

    /*
     *  This is where the action is.  For existing friends, go to the Entry screen.  For waiting
     *  friends, send the acceptance.  For potential friends, send the invitation.
     */
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
                    for(Account acct : mAccounts) {
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


                        }else{ // Send an invitation or something.
                            String [] addresses = GetContactAddresses(holdAcct.bestName());
                            if (addresses.length > 0) {
                                // This will require the network, so check it
                                WebServices ws = new WebServices();
                                if(!ws.IsNetwork(this)) {
                                    Toast.makeText(this, R.string.msgNoNet, Toast.LENGTH_LONG).show();
                                    break;
                                }

                                // Create a dialog to allow selection of address.
                                FragmentManager mgr = getFragmentManager();
                                Fragment frag = mgr.findFragmentByTag(KY_ADDR_FRAG);
                                if (frag != null) {
                                    mgr.beginTransaction().remove(frag).commit();
                                }
                                ContactPickerDialog invited = new ContactPickerDialog();
                                invited.setInvites(addresses);
                                invited.show(mgr, KY_ADDR_FRAG);
                            } else {
                                Toast.makeText(this, R.string.ctp_no_addresses, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                    break;
            }
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".pickOnClick");
        }
    }

    /*
     *  Find all the associated email addresses and phone numbers for a contact.
     */
    private String [] GetContactAddresses(String anchor) {
        List<String> ali = new ArrayList<>();
        for (Account acct : mAccounts) {
            if (acct.bestName().equalsIgnoreCase(anchor)) {
                ali.add(acct.unique);
            }
        }
        return ali.toArray(new String[0]);
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
                mAccounts.add(local);
            }
            cursor.close();
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".LoadFriends"); }
        finally { social.close(); }
    }

    /*
     * This reads all the contacts with a phone number or email address from the data table.  For a good layout of the data see:
     * http://androidexample.com/Get_Contact_Emails_By_Content_Provider_-_Android_Example/index.php?view=article_discription&aid=121
     * The phone numbers get a little formatting help.  With all the data we want in memory, specific data is selectively displayed.
     */
    private void LoadContacts(){
        List<Account> contacts = new ArrayList< >();
        String sortEnd = getString(R.string.zzzzz);
        Cursor contact = null;
        String[] selection = {ContactsContract.Data.CONTACT_ID, ContactsContract.Data.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts.Data.DATA1, ContactsContract.Data.MIMETYPE, ContactsContract.Contacts.PHOTO_THUMBNAIL_URI};
        try{
            contact = getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    selection,
                    ContactsContract.Contacts.Data.MIMETYPE + "=? OR " + ContactsContract.Contacts.Data.MIMETYPE + "=?",
                    new String[]{ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE},
                    ContactsContract.Data.DISPLAY_NAME_PRIMARY);

            if(contact == null || contact.getCount() == 0) return;
            PhoneNumberUtil phoneHelper = PhoneNumberUtil.getInstance();
            while (contact.moveToNext()) {
                try {
                    Account holdAcct = new Account();
                    Long id = contact.getLong(contact.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                    holdAcct.contactId = id.toString();
                    holdAcct.contactName = contact.getString(contact.getColumnIndex(ContactsContract.Data.DISPLAY_NAME_PRIMARY));
                    holdAcct.contactSur = holdAcct.contactName.substring(holdAcct.contactName.lastIndexOf(" ")+1);
                    if (holdAcct.contactSur.length() > 0 && Character.isDigit(holdAcct.contactSur.charAt(0))) holdAcct.contactSur = sortEnd; // sort these to the end
                    String mimetype = contact.getString(contact.getColumnIndex(ContactsContract.Data.MIMETYPE));
                    holdAcct.unique = contact.getString(contact.getColumnIndex(ContactsContract.Contacts.Data.DATA1));
                    if (mimetype.equalsIgnoreCase(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                        Phonenumber.PhoneNumber fullNbr = phoneHelper.parse(holdAcct.unique, "US");
                        String holdnbr = phoneHelper.format(fullNbr, PhoneNumberUtil.PhoneNumberFormat.E164);
                        holdAcct.unique = holdnbr.replace("+", "");
                    }
                    holdAcct.contactPic = contact.getString(contact.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
                    contacts.add(holdAcct);
                } catch (Exception ex) {
                    /* If a single contact has an issue, just move past it. Specifically, the phone
                       number parser might barf if the user has some non-number in that field. We do
                       not want to use it either in that case. */
                }
            }
            contact.close();
            // The contacts are sorted by first name already, so only sort if need to switch to last name sorting.
            if(Settings.isSortByLastName(getApplicationContext())){
                Collections.sort(contacts, Account.ByLastFirstName);
            }
            for (Account ali : contacts) {
                mAccounts.add(ali);
            }
        }catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".GetContactList");
            if(contact != null) contact.close();
        }
    }


    /* The Options Menu works closely with the ActionBar.  It can show useful menu items on the bar
     * while hiding less used ones on the traditional menu.  The xml configuration determines how they
     * are shown. The system will call the onCreate when the user presses the menu button.
     * Note: Android refuses to show icon+text on the ActionBar in portrait, so deal with it. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contactpicker_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuSort:
                // Create a dialog to allow changes to sorting.
                FragmentManager mgr = getFragmentManager();
                Fragment frag = mgr.findFragmentByTag(KY_CNTC_FRAG);
                if (frag != null) {
                    mgr.beginTransaction().remove(frag).commit();
                }
                ContactPickerSortDialog sorter = new ContactPickerSortDialog();
                sorter.show(mgr, KY_CNTC_FRAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setDate(String date)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTime(String time)  {
        throw new UnsupportedOperationException();
    }

    /*
     *  The dialog has adjusted the sort parameters, resort and redisplay the data.
     */
    @Override
    public void newSort() {
        if(mContactSearch != null) mContactSearch.setText("");
        ShowDetails("");
    }

    /*
     *  The invites have been selected, but better to call the thread from here.
     */
    @Override
    public void newInvite(String [] addresses, boolean mirror) {
        SendInviteThread smt = new SendInviteThread(getApplicationContext(), addresses, mirror);
        smt.start();
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
            // See https://github.com/lopspower/CircularImageView for information about CircularImageView
            CircularImageView holdPic;
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
                                holdPic = (CircularImageView) convertView.findViewById(R.id.rowpFacePic);
                                holdPic.setImageURI(Uri.parse(holdData.get(CP_FACE)));
                            } else {
                                holdPic = (CircularImageView) convertView.findViewById(R.id.rowpFacePic);
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
}
