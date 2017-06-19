package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 *  The Welcome screen is the entry point into the application.  Upon first
    use it will notice the user needs to proceed to the signup process.  If
    the user is returning, this screen provides a path to direct entry of a
    prompt (for themselves), or navigation to a list of contacts that could
    be sent a prompt.  It also does a quick check to validate connectivity.

 */
public class Welcome extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up main view and menu.
        setContentView(R.layout.welcome);

        // In order to skip signup, use LoadUser(), otherwise comment out.
        Actor acct = new Actor(this); //LoadUser();

        // Google Play?
        isGooglePlayServicesAvailable(this);

        /* The Refresh service is important as it is the primary way
         * most of the data (SQL and Web Service) is refreshed. For
         * now this only exists in Welcome, but is run upon each creation.
         * It may end up needing to be called more often.
         */
        Intent sIntent = new Intent(this, Refresh.class);
        startService(sIntent);

        // Check if user needs to sign up.
        if (acct.ticket.length() == 0) {
            Intent intent = new Intent(this, Signup.class);
            startActivity(intent);
        }
        else {
            // Check that system is up.
            new CheckRegistryTask(this).execute(acct.ticket);
        }

        DisplayAccount();
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         *  It is a good idea to trigger this service each time Welcome comes up.
         */
        Intent sIntent = new Intent(this, Refresh.class);
        startService(sIntent);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void DisplayAccount(){
        TextView holdView;

        Actor acct = new Actor(this);

        holdView = (TextView) this.findViewById(R.id.entryTicket);
        if(holdView != null) { holdView.setText(acct.ticket); }
        holdView = (TextView) this.findViewById(R.id.entryAcctId);
        if(holdView != null) { holdView.setText(acct.acctIdStr()); }
        holdView = (TextView) this.findViewById(R.id.entryName);
        if(holdView != null) { holdView.setText(acct.display); }
        holdView = (TextView) this.findViewById(R.id.entryUnique);
        if(holdView != null) { holdView.setText(acct.unique); }
        holdView = (TextView) this.findViewById(R.id.entryPhoto);
        if(holdView != null) { holdView.setText(acct.contactPicUri()); }
    }

    private Account LoadUser(){
        Actor acct = new Actor();

        acct.acctId = 1;
        acct.ticket = "13393021-fb05-4383-b8aa-7f5208129ab7";
        acct.display = "KevinOne";
        acct.unique = "kevinone@kafekevin.com";
        acct.localId = Owner_DBID;
        acct.confirmed = true;
        acct.sleepcycle = 2;

        acct.SyncPrime(false, this);
        return acct;
    }

    public void JumpConnections(View view) {
        Intent intent = new Intent(this, ContactPicker.class);
        startActivity(intent);
    }

    public void JumpEntry(View view) {
        Intent intent = new Intent(this, Entry.class);
        startActivity(intent);
    }

    private void SystemCheck(boolean isOk) {
        TextView holdView;
        String holdLabel;

        holdView = (TextView) this.findViewById(R.id.isRegistered);
        holdLabel = getResources().getText(R.string.isRegistered) + (isOk ? "YES" : "NO");
        if (holdView != null) { holdView.setText(holdLabel); }
    }

    /*
        This is a quick check to see that there is a network, our server is
        reachable and the ticket they are using is viable.
     */
    private class CheckRegistryTask extends AsyncTask<String, Void, Boolean> {
        private Context context;

        public CheckRegistryTask(AppCompatActivity activity) {
            context = activity;
        }

        @Override
        protected Boolean doInBackground(String... criteria) {
            try {
                WebServices stat = new WebServices();
                String ticket = criteria[0];

                return !stat.IsNetwork(context) || stat.CheckRegistration(ticket);

            } catch (Exception ex) {
                return true;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            SystemCheck(result);
        }
    }

    /*
        The notification functionality relies upon the Google Play Services, which
        on some implementations of Android might might not be present, just letting
        the person know.
     */
    public boolean isGooglePlayServicesAvailable(AppCompatActivity main) {
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

}
