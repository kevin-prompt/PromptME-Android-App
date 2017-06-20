package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.content.Intent;
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
    be sent a prompt.  This is also the entry point to the Settings.

 */
public class Welcome extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up main view and menu.
        setContentView(R.layout.welcome);

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

    private void Display(){
        TextView holdView;

     }

    public void JumpConnections(View view) {
        Intent intent = new Intent(this, ContactPicker.class);
        startActivity(intent);
    }

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

}
