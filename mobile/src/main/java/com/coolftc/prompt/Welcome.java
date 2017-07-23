package com.coolftc.prompt;

import static com.coolftc.prompt.utility.Constants.*;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.coolftc.prompt.service.Refresh;
import com.coolftc.prompt.source.FriendDB;
import com.coolftc.prompt.utility.ExpClass;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *  The Welcome screen is the entry point into the application.  Upon first
    use it will notice the user needs to proceed to the signup process.  If
    the user is returning, this screen provides a path to direct entry of a
    prompt (for themselves), or navigation to a list of contacts that could
    be sent a prompt.  This is also the entry point to the Settings.

 */
public class Welcome extends AppCompatActivity {
    // The contact list.
    ListView mListView;
    // The "mAccounts" collect all the possible people to display.
    List<Account> mAccounts = new ArrayList< >();
    // This is the mapping of the detail map to each specific person.
    String[] StatusMapFROM = {CP_PER_ID, CP_TYPE, CP_NAME, CP_EXTRA, CP_FACE};
    int[] StatusMapTO = {R.id.rowp_Id, R.id.rowpType, R.id.rowpContactName, R.id.rowpContactExtra, R.id.rowpFacePic};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up main view and menu.
        setContentView(R.layout.welcome);
        mListView = (ListView) findViewById(R.id.welConnections);

        // Lets see if what we know about this person.
        Actor acct = new Actor(this);

        // Google Play?  They will need it.
        isGooglePlayServicesAvailable(this);

        /* The Refresh service is important as it is the primary way
         * most of the data (SQL and Web Service) is refreshed. For
         * now is only triggered in Welcome, but we do trigger it
         * every time this screen shows up.
         */
        Intent sIntent = new Intent(this, Refresh.class);
        startService(sIntent);

        // Check if user needs to sign up.
        if (acct.ticket.length() == 0) {
            Intent intent = new Intent(this, Signup.class);
            startActivity(intent);
        }

        Display();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         *  Trigger Refresh service each time Welcome comes up.
         */
        Intent sIntent = new Intent(this, Refresh.class);
        startService(sIntent);

        /*
         *  Make sure someone did not navigate back here un prepared.
         */
        Actor acct = new Actor(this);
        if (acct.ticket.length() == 0) {
            Intent intent = new Intent(this, Signup.class);
            startActivity(intent);
        }

    }

    /* The Options Menu works closely with the ActionBar.  It can show useful menu items on the bar
     * while hiding less used ones on the traditional menu.  The xml configuration determines how they
     * are shown. The system will call the onCreate when the user presses the menu button.
     * Note: Android refuses to show icon+text on the ActionBar in portrait, so deal with it. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.welcome_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuWelcomeSettings:
                startActivity(new Intent(this, Settings.class));
                return true;
            case R.id.mnuHistory:
                startActivity(new Intent(this, History.class));
                return true;
            case R.id.mnuConnections:
                startActivity(new Intent(this, ContactPicker.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void Display(){
        // This is a new account list from scratch.  This is a global list (for this class), so
        // it can be used later by other methods.
        mAccounts = new ArrayList<>();
        // This will come in handy a couple times.
        Actor actor = new Actor(getApplicationContext());

        // Have the Title match the larger contacts screen.
        AddDelimitRow(R.string.contact_pri);
        // Grab the primary user, which is stored in the preferences.
        mAccounts.add(actor);
        //Get all friends stored locally.
        LoadFriends(FriendDB.SQLITE_TRUE);
        // Add a title to suggest adding new connections
        AddDelimitRow(R.string.contact_more);

        // The "uniques" hold all the unique names that are on the list
        Map<String, Boolean> uniques = new HashMap<>();

        // The "detail" hold the final data sent to the display list.
        List<Map<String, String>> details = new ArrayList<>();

        // Move the account data into the desired detail format.
        for(Account acct : mAccounts) {
            // Sometimes we want to skip displaying records.
            if (uniques.containsKey(acct.unique)) continue;

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

        WelcomeAdapter adapter = new WelcomeAdapter(this, details, R.layout.contactpicker_row, StatusMapFROM, StatusMapTO);
        mListView.setAdapter(adapter);

        // Just having this check should be good enough, as Refresh is called
        // upon incoming notifications as well as entry into this screen.
        TextView holdView = (TextView)findViewById(R.id.welPending);
        if(holdView != null) { holdView.setText(String.format(getResources().getString(R.string.wel_Pending), actor.notesWaiting)); }
    }

    // Used by floating action button to directly enter a prompt for the user.
    public void JumpEntry(View view) {
        Intent intent = new Intent(this, Entry.class);
        startActivity(intent);
    }

    /*
        The notification functionality relies upon the Google Play Services, which
        on some implementations of Android might might not be present, just letting
        the person know.
     */
    private boolean isGooglePlayServicesAvailable(AppCompatActivity main) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(main);
        if(status != ConnectionResult.SUCCESS) {
            if(googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(main, status, KY_PLAYSTORE).show();
            }
            return false;
        }
        return true;
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

    public void delimitClick(View view){
        switch (view.getId()) {
            case R.id.rowpItem:
                TextView holdView;
                holdView = (TextView) view.findViewById(R.id.rowpDelimitName);
                if(holdView != null) {
                    if (holdView.getText().toString().equalsIgnoreCase(getResources().getString(R.string.contact_more))) {
                        startActivity(new Intent(this, ContactPicker.class));
                    }
                }
                break;
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

    /*
     * This provides a custom handling of the list of connections.  This is a simpler version of
     * the Contact Picker listbox.
     * NOTE: Be sure to use match_parent (or specific values) for the height and width of the ListView
     * and rows. Otherwise the getView is called A LOT! since it has to guess at sizing.
     */
    private class WelcomeAdapter extends SimpleAdapter {
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_SEPARATOR = 1;
        private static final int TYPE_MAX_COUNT = 2;

        private LayoutInflater mInflater;

        public WelcomeAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
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
