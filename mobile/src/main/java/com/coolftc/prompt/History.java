package com.coolftc.prompt;

import static com.coolftc.prompt.utility.Constants.*;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Handler;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.coolftc.prompt.source.MessageDB;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.ExpParseToCalendar;
import com.coolftc.prompt.utility.KTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *  The History is a display of all Reminders created on the local device. The
    list is shown in reverse chronological order by default, but can be sorted
    by descending create date. If a reminder has not yet been sent to the server,
    a "Processing..." message will also be displayed.
    From this screen one can navigate to the welcome screen by pressing the
    parent navigation or to the contact list with the floating people button. To
    see details of the prompt, tapping on the prompt navigates to detail.
 */
public class History extends AppCompatActivity implements FragmentTalkBack{

    // The message list.
    private ListView mListView;
    // The search box.
    private EditText mHistorySearch;
    // The "reminder" collection of all the possible messages.
    private List<Reminder> mReminders = new ArrayList< >();
    // This is the mapping of the detail map to each specific message.
    private String[] StatusMapFROM = {HS_REM_ID, HS_TIME, HS_RECURS, HS_SNOOZE, HS_LAST_15, HS_WHO_FROM, HS_WHO_TO, HS_MSG};
    private int[] StatusMapTO = {R.id.rowh_Id, R.id.rowhTargetTime, R.id.rowhRecur, R.id.rowhSnooze, R.id.rowhNew, R.id.rowhTargetFrom, R.id.rowhTargetTo, R.id.rowhMessage};

    // Handler used as a timer to trigger updates.
    private Handler hRefresh = new Handler();
    private Integer hRefreshCntr = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up the UI
        setContentView(R.layout.history);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mHistorySearch = (EditText) findViewById(R.id.txtSearch_HS);
        mListView = (ListView) findViewById(R.id.listContacts_HS);
        ShowDetails("");
        hRefresh.postDelayed(rRefresh, UPD_SCREEN_TQ);

        // As a user types in characters, trim the reminder list.
        mHistorySearch.addTextChangedListener(new TextWatcher() {
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
     *  Go to the contact picker to create more prompts.
     */
    public void NewMessage(View view) {
        Intent intent = new Intent(this, ContactPicker.class);
        startActivity(intent);
    }

    /*
     *  Go to the message detail screen, send along the selected reminder to avoid DB read.
     */
    public void OldMessage(View view) {
        try {
            switch (view.getId()) {
                case R.id.rowhItem:
                    // Get the important data out of the row.
                    TextView holdView;
                    holdView = (TextView) view.findViewById(R.id.rowh_Id);
                    long uSelect = Long.parseLong(holdView.getText().toString());

                    // Now find the Reminder data and send it along (to save a trip to the database.
                    for(Reminder msg : mReminders) {
                        if (msg.id == uSelect) {
                            Intent intent = new Intent(this, Detail.class);
                            Bundle mBundle = new Bundle();
                            mBundle.putSerializable(IN_MESSAGE, msg);
                            intent.putExtras(mBundle);
                            startActivity(intent);
                            break;
                        }
                    }
                    break;
            }
        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".pickOnClick");
        }
    }

    /* The Options Menu works closely with the ActionBar.  It can show useful menu items on the bar
     * while hiding less used ones on the traditional menu.  The xml configuration determines how they
     * are shown. The system will call the onCreate when the user presses the menu button.
     * Note: Android refuses to show icon+text on the ActionBar in portrait, so deal with it. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnuSort:
                // Create a dialog to allow changes to sorting.
                FragmentManager mgr = getFragmentManager();
                Fragment frag = mgr.findFragmentByTag(KY_HIST_FRAG);
                if (frag != null) {
                    mgr.beginTransaction().remove(frag).commit();
                }
                HistorySortDialog sorter = new HistorySortDialog();
                sorter.show(mgr, KY_HIST_FRAG);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     *  Load up the messages into the global list, then parse them out to the list view.
     */
    private void ShowDetails(String search) {
        // Load up the messages from the database
        GetMessages();
        if(Settings.getPromptSortOrder(getApplicationContext()) == Settings.DEFAULT_SORT_ORDER)
            Collections.sort(mReminders, Reminder.ByDeliveryDate);
        else
            Collections.sort(mReminders, Reminder.ByCreateDate);
        ShowDetailsCache(search);
    }

    /*
     *  This is an optimization on the ShowDetails(), in that it just uses the existing
     *  list of messages.  This makes the type ahead search much quicker, but will not
     *  pick up any changes to the list.
     */
    private void ShowDetailsCache(String search) {
        // The "detail" hold the final data sent to the display list.
        List<Map<String, String>> details = new ArrayList<>();
        String waiting = getResources().getString(R.string.processing);

        // Move the account data into the desired detail format.
        for(Reminder msg : mReminders) {
            Map<String, String> hold = new TreeMap<>();
            if (!msg.Found(search)) continue;

            // Hidden message id
            hold.put(HS_REM_ID, msg.IdStr());
            // Delivery time and indication if past.  If not processed, assume not in past.
            hold.put(HS_TIME_PAST, msg.IsPromptPast());
            if (msg.processed) {
                hold.put(HS_TIME, msg.GetPromptTime(getApplicationContext()));
            } else {
                hold.put(HS_TIME, waiting);
            }
            // If message to self, skip it, otherwise show who created it.
            if (!msg.IsSelfie()) {
                hold.put(HS_WHO_FROM, msg.from.bestName());
                hold.put(HS_WHO_TO, msg.target.bestName());
            } else {
                hold.put(HS_WHO_FROM, "");
                hold.put(HS_WHO_TO, "");
            }
            // Check if recurring, just put anything in the value.
            if (msg.recurUnit != RECUR_INVALID) {
                hold.put(HS_RECURS, "X");
            } else {
                hold.put(HS_RECURS, "");
            }
            // Check if this note was ever snoozed.
            if(msg.snoozeId > 0) {
                hold.put(HS_SNOOZE, "X");
            } else {
                hold.put(HS_SNOOZE, "");
            }
            // Check if new (created in last x minutes)
            hold.put(HS_LAST_15, "");
            try {
                if(KTime.CalcDateDifference(msg.created, KTime.ParseNow(KTime.KT_fmtDate3339fk, KTime.UTC_TIMEZONE).toString(), KTime.KT_fmtDate3339fk, KTime.KT_MINUTES) < 15){
                    hold.put(HS_LAST_15, "X");
                }
            } catch (ExpParseToCalendar expParseToCalendar) {
                /* Just skip it */
            }
            // The actual message
            hold.put(HS_MSG, msg.message);
            details.add(hold);
        }

        // Try to keep the listbox from scrolling on its own.
        // See https://stackoverflow.com/questions/3014089/maintain-save-restore-scroll-position-when-returning-to-a-listview/5688490#5688490
        int index = mListView.getFirstVisiblePosition();
        View v = mListView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
        HistoryAdapter adapter = new HistoryAdapter(this, details, R.layout.contactpicker_row, StatusMapFROM, StatusMapTO);
        mListView.setAdapter(adapter);
        mListView.setSelectionFromTop(index, top);
    }

   /*
    *  Read a large number of messages into an array.
    *  This can be used later for the search
    */
    private void GetMessages(){
        MessageDB message = new MessageDB(getApplicationContext());  // Be sure to close this before leaving the thread.
        SQLiteDatabase db = message.getReadableDatabase();
        String[] filler = {};
        Cursor cursor = db.rawQuery(DB_MessagesAll, filler);
        try{
            mReminders.clear();
            while(cursor.moveToNext()) {
                Reminder local = new Reminder();
                local.target = new Account();
                local.from = new Account();
                local.id = cursor.getLong(cursor.getColumnIndex(MessageDB.MESSAGE_ID));
                local.serverId = cursor.getLong(cursor.getColumnIndex(MessageDB.MESSAGE_SRVR_ID));
                local.targetTime = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TIME));
                local.targetTimeNameId = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_TIMENAME));
                local.targetTimeAdjId = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_TIMEADJ));
                local.sleepCycle = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_SLEEP));
                local.timezone = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TIMEZONE));
                local.recurUnit = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_UNIT));
                local.recurPeriod = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_PERIOD));
                local.recurNumber = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_R_NUMBER));
                local.recurEnd = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_R_END));
                local.message = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_MSG));
                local.processed = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_PROCESSED))==MessageDB.SQLITE_TRUE;
                local.status = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_STATUS));
                local.snoozeId = cursor.getInt(cursor.getColumnIndex(MessageDB.MESSAGE_SNOOZE_ID));
                local.created = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_CREATE));
                local.from.unique = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_SOURCE));
                local.from.display = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_FROM));
                local.target.unique = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_TARGET));
                local.target.display = cursor.getString(cursor.getColumnIndex(MessageDB.MESSAGE_NAME));
                mReminders.add(local);
            }
            cursor.close();
        } catch(Exception ex){ cursor.close(); ExpClass.LogEX(ex, this.getClass().getName() + ".GetMessages"); }
        finally { message.close(); }

    }

    @Override
    public void setDate(String date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTime(String time) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void newInvite(String [] addresses, boolean mirror) { throw new UnsupportedOperationException(); }

    /*
     *  The dialog has adjusted the sort parameters, resort and redisplay the data.
     */
    @Override
    public void newSort() {
        if(Settings.getPromptSortOrder(getApplicationContext()) == Settings.DEFAULT_SORT_ORDER)
            Collections.sort(mReminders, Reminder.ByDeliveryDate);
        else
            Collections.sort(mReminders, Reminder.ByCreateDate);
        ShowDetailsCache("");
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
            ImageView holdImage;

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
                holdView.setText(holdData.get(HS_REM_ID));
                switch (type) {
                    case TYPE_ITEM:
                        holdView = (TextView) convertView.findViewById(R.id.rowhTargetTime);
                        holdView.setText(holdData.get(HS_TIME));
                        if(holdData.get(HS_TIME_PAST).length() == 0) {
                            holdView.setTypeface(null, Typeface.BOLD);  // not in the past, so bold
                        } else { // Always need to reset these, since they are reused.
                            holdView.setTypeface(null, Typeface.NORMAL);
                        }
                        holdImage = (ImageView) convertView.findViewById(R.id.rowhRecur);
                        if(holdData.get(HS_RECURS) != null && holdData.get(HS_RECURS).length() > 0) {
                            holdImage.setVisibility(View.VISIBLE);
                        } else { // Always need to reset these, since they are reused.
                            holdImage.setVisibility(View.INVISIBLE);
                        }
                        holdImage = (ImageView) convertView.findViewById(R.id.rowhSnooze);
                        if(holdData.get(HS_SNOOZE) != null && holdData.get(HS_SNOOZE).length() > 0) {
                            holdImage.setVisibility(View.VISIBLE);
                        } else { // Always need to reset these, since they are reused.
                            holdImage.setVisibility(View.INVISIBLE);
                        }
                        holdView = (TextView) convertView.findViewById(R.id.rowhNew);
                        if(holdData.get(HS_LAST_15) != null && holdData.get(HS_LAST_15).length() > 0) {
                            holdView.setVisibility(View.VISIBLE);
                        }else{ // Always need to reset these, since they are reused.
                            holdView.setVisibility(View.INVISIBLE);
                        }
                        holdView = (TextView) convertView.findViewById(R.id.rowhTargetFrom);
                        holdView.setText(holdData.get(HS_WHO_FROM));
                        holdView = (TextView) convertView.findViewById(R.id.rowhTargetTo);
                        holdView.setText(holdData.get(HS_WHO_TO));
                        holdView = (TextView) convertView.findViewById(R.id.rowhMessage);
                        holdView.setText(holdData.get(HS_MSG));
                        break;
                }
            }
            return convertView;
        }
    }

    // Non-Thread Timer used to periodically refresh the display list. SendMessageThread updates
    // the DB with completed messages delivery time, typically after the user first sees this screen.
    // For the first minute we want to use the faster refresh rate of TQ and do a full reload of the
    // screen.  This should cover the time a person would actually be looking at the screen,
    // then back off and don't reload the messages to save battery.
    private Runnable rRefresh = new Runnable() {
        public void run() {

            String holdSearch = mHistorySearch.getText().toString();
            hRefreshCntr += UPD_SCREEN_TQ;
            if(hRefreshCntr < UPD_SCREEN_TQ*10){
                ShowDetails(holdSearch);
                hRefresh.postDelayed(this, UPD_SCREEN_TQ);
            } else {
                ShowDetailsCache(holdSearch);
                hRefresh.postDelayed(this, UPD_SCREEN_TM);
            }
        }

    };
}
