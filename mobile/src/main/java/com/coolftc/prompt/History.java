package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/*
 * The History Display shows all reminders created on the local device. The
 * list is shown in reverse chronological order.  If a reminder has not yet
 * been sent to the server, a "processing..." message will also be displayed.
 * From this screen one can navigate to the welcome screen by pressing the
 * floating add button (to add another prompt).
 */
public class History extends AppCompatActivity {

     // The message list.
     ListView mListView;
     // The search box.
     EditText historySearch;
     // The "reminder" collection of all the possible messages.
     List<Reminder> reminders = new ArrayList< >();
     // This is the mapping of the details map to each specific message.
     String[] StatusMapFROM = {HS_REM_ID, HS_TIME, HS_WHO, HS_MSG};
     int[] StatusMapTO = {R.id.rowh_Id, R.id.rowhTargetTime, R.id.rowhTargetName, R.id.rowhMessage};

    // Handler used as a timer to trigger updates.
    private Handler hRefresh = new Handler();
    private Integer hRefreshCntr = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI
        setContentView(R.layout.history);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        historySearch = (EditText) findViewById(R.id.txtSearch_HS);
        mListView = (ListView) findViewById(R.id.listContacts_HS);
        ShowDetails("");
        hRefresh.postDelayed(rRefresh, 5000);

        // As a user types in characters, trim the reminder list.
        historySearch.addTextChangedListener(new TextWatcher() {
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
     *  Load up the messages into the global list, then parse them out to the list view.
     */
    private void ShowDetails(String search) {
        // Load up the messages from the database
        GetMessages();
        ShowDetailsCache(search);
    }

    /*
     *  This is an optimization on the ShowDetails(), in that it just uses the existing
     *  list of messages.  This makes the type ahead search much quicker, but will not
     *  pick up any changes to the list.
     */
    private void ShowDetailsCache(String search) {
        // The "details" hold the final data sent to the display list.
        List<Map<String, String>> details = new ArrayList<>();
        String waiting = getResources().getString(R.string.processing);

        // Move the account data into the desired details format.
        for(Reminder msg : reminders) {
            Map<String, String> hold = new TreeMap<>();
            if (!msg.Found(search)) continue;

            // Copy the data to display.
            hold.put(HS_REM_ID, msg.idStr());
            if(msg.processed) {
                hold.put(HS_TIME, msg.GetPromptTime(getApplicationContext()));
            }else{
                hold.put(HS_TIME, waiting);
            }
            hold.put(HS_WHO, msg.target.bestName());
            hold.put(HS_MSG, msg.message);
            details.add(hold);
        }

        HistoryAdapter adapter = new HistoryAdapter(this, details, R.layout.contactpicker_row, StatusMapFROM, StatusMapTO);
        mListView.setAdapter(adapter);
    }

   /*
    *  Read a large number of messages into an array.
    *  This can be used later for the search
    */
    private void GetMessages(){
        MessageDB message = new MessageDB(this);  // Be sure to close this before leaving the thread.
        SQLiteDatabase db = message.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_MessagesAll, filler);
        try{
            reminders.clear();
            while(cursor.moveToNext()) {
                Account acct = new Account();
                Reminder local = new Reminder();
                local.target = acct;
                local.id = cursor.getLong(cursor.getColumnIndex(MessageDB.MESSAGE_ID));
                local.target.display = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_NAME));
                local.targetTime = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TIME));
                local.message = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_MSG));
                local.status = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_STATUS));
                local.processed = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_PROCESSED))==MessageDB.SQLITE_TRUE;
                reminders.add(local);
            }
            cursor.close();
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".GetMessages"); }
        finally { message.close(); }

    }

   /*
    * This provides a custom handling of the list of messages.  Other that formatting the displayed
    * values, there is not too much special going on here.
    * NOTE: Be sure to use match_parent (or specific values) for the height and width of the ListView
    * and rows. Otherwise the getView is called A LOT! since it has to guess at sizing.
    */
    private class HistoryAdapter extends SimpleAdapter {
        private static final int TYPE_ITEM = 0;
        private static final int TYPE_MAX_COUNT = 1;

        private LayoutInflater mInflater;

        public HistoryAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_MAX_COUNT;
        }

        @Override
        public int getItemViewType(int position) { return TYPE_ITEM; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            TextView holdView;

            if (convertView == null) {
                switch (type) {
                    case TYPE_ITEM:
                        convertView = mInflater.inflate(R.layout.history_row, null);
                        break;
                }
            }

            if (convertView != null) {
                // NOTE: Unchecked cast is ok if you use the data close by,
                // since that is what will trigger the runtime exception.
                @SuppressWarnings("unchecked")
                Map<String, String> holdData = (Map<String, String>) getItem(position);
                holdView = (TextView) convertView.findViewById(R.id.rowh_Id);
                holdView.setText(holdData.get(CP_PER_ID));
                switch (type) {
                    case TYPE_ITEM:
                        holdView = (TextView) convertView.findViewById(R.id.rowhTargetTime);
                        holdView.setText(holdData.get(HS_TIME));
                        holdView = (TextView) convertView.findViewById(R.id.rowhTargetName);
                        holdView.setText(holdData.get(HS_WHO));
                        holdView = (TextView) convertView.findViewById(R.id.rowhMessage);
                        holdView.setText(holdData.get(HS_MSG));
                        break;
                }
            }
            return convertView;
        }
    }

    public void NewMessage(View view) {
        Intent intent = new Intent(this, ContactPicker.class);
        startActivity(intent);
    }

    // Non-Thread Timer used to periodically refresh the display list. SendMessageThread updates
    // the DB with completed messages delivery time, typically after the user first sees this screen.
    // For the first 5 iterations we want to use the faster refresh rate of TQ.  This should cover
    // the time a person would actually be looking at the screen, then back off to save battery.
    private Runnable rRefresh = new Runnable() {
        public void run() {

            ShowDetails("");
            hRefreshCntr += UPD_SCREEN_TQ;
            if(hRefreshCntr < UPD_SCREEN_TQ*5){
                hRefresh.postDelayed(this, UPD_SCREEN_TQ);
            } else {
                hRefresh.postDelayed(this, UPD_SCREEN_TM);
            }
        }

    };
}
