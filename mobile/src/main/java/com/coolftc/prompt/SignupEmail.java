package com.coolftc.prompt;

import static com.coolftc.prompt.utility.Constants.*;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.coolftc.prompt.source.RegisterRequest;
import com.coolftc.prompt.source.RegisterResponse;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import java.util.TimeZone;

/**
 *  The Signup process is a minimal account setup to establish a unique
    contact ID/method using either SMS or email.  At the same time, a
    non-unique name is also created to allow some personalization.
    For email, all the processing is done internally.  The create account
    generates an email and then the SignupConfirm screen performs
    the verification.

    The Signup screens are set to only be portrait to avoid some UI and
    Technical complications. Given the one time use of the screen, it
    should not be a big deal. Preventing a portrait/landscape context
    switch should help avoid problems with the AsynchTask used in the
    account creation/verification. Other context changes are much less
    common.
 */
public class SignupEmail extends AppCompatActivity {

    private String mDsplName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mDsplName = getResources().getString(R.string.mysteryme);
        if(extras != null){
            mDsplName = extras.getString(IN_DSPL_NAME, getResources().getString(R.string.mysteryme));
        }

        setContentView(R.layout.signupemail);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /*
        Ready to go, try and create the account.
     */
    public void EmailCreateAcct(View view) {
        TextView holdView = findViewById(R.id.txtEmailAddr_SE);
        String txtEmailAddr = "";
        if(holdView!=null) txtEmailAddr = holdView.getText().toString();
        if(txtEmailAddr.length()==0) return;

        // If the user is a Solo, passing in the existing unique name as the
        // custom name will trigger the server to match up the accounts.
        Actor user = new Actor(getApplicationContext());
        String soloName = user.solo ? user.unique : "";

        // Create an account, then move on to verify page
        new AcctCreateTask(SignupEmail.this, getResources().getString(R.string.app_name)).execute(
                txtEmailAddr,
                mDsplName,
                getResources().getString(R.string.prf_SleepCycleDefault),
                soloName);
    }

    /*
        Once created, the email with the verification code should be on
        its way, so go to the next step.
     */
    private void  AccountCreated(Actor acct){
        if(acct.ticket.length() > 0) {
            // Verify Screen - this is the normal path.
            Intent confirmScreen = new Intent(this, SignupConfirm.class);
            confirmScreen.putExtra(IN_DSPL_TGT, acct.unique);
            confirmScreen.putExtra(IN_CONFIRM_TYPE, EMAIL_SIGNUP);
            startActivity(confirmScreen);
        }
        else {
            // Problem creating account.
            TextView holdView = findViewById(R.id.lblError_SE);
            if (holdView != null)
                holdView.setTextColor(ContextCompat.getColor(this, R.color.promptwhite));
            if (acct.tag.length() > 0){
                holdView = findViewById(R.id.lblEmailStatus);
                if (holdView != null) {
                    holdView.setVisibility(View.VISIBLE);
                    String holdStat = getString(R.string.status) + ":" + acct.tag;
                    holdView.setText(holdStat);
                }
            }
        }
    }

    /**
     * The nested AsyncTask class is used to off-load the network call to a separate
     * thread but allow quick feedback to the user.
     * Considerations:  Memory can leak as an inner class holds a reference to outer.
     * 1) Create as an explicit inner class, not an antonymous one.
     * 2) Pass in the Application context, not an Activity context.
     * 3) Make the work in the background single pass and likely to complete (quickly).
     * 4) If possible prevent the most common Activity killer by locking into portrait.
     * 5) Avoid use in parts of the App that get used a lot, e.g. lazy data refresh design.
     */
    private class AcctCreateTask extends AsyncTask<String, Boolean, Actor> {
        private ProgressDialog progressDialog;
        private final Context context;
        private final String title;

        public AcctCreateTask(AppCompatActivity activity, String name){
            context = activity;
            title = name;
        }

        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, title, context.getResources().getString(R.string.processing), true, true);
            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) { cancel(true); } });
        }

        protected void onPostExecute(Actor result) {
            AccountCreated(result);
            progressDialog.dismiss();
        }

        protected void onCancelled() {
            AccountCreated(new Actor(context));
        }

        protected void onProgressUpdate(Boolean... values) {
            if(!values[0]) Toast.makeText(context, R.string.msgNoNet, Toast.LENGTH_LONG).show();
        }

        /*
         * This assumes verification will come later and just creates the account
         * here. If the account is created, we also sync the local account data.
         * Input criteria:
         ** Creation **
         * 0 - Unique Name (SMS/email)
         * 1 - Display Name
         * 2 - Sleep Cycle
         * 3 - Custom Data (e.g. external id)
         */
        protected Actor doInBackground(String... criteria) {

            Actor acct = new Actor(context);
            try (Connection net = new Connection(context)) {
                if (net.isOnline()) {
                    WebServices ws = new WebServices(new Gson());
                    acct.unique = criteria[0];
                    acct.timezone = TimeZone.getDefault().getID();
                    acct.display = criteria[1].length() > 0 ? criteria[1] : acct.unique;
                    acct.sleepcycle = Integer.parseInt(criteria[2]);
                    acct.custom = criteria[3];

                    RegisterRequest data = new RegisterRequest(
                            acct.unique,
                            true,
                            acct.timezone,
                            acct.display,
                            acct.sleepcycle,
                            acct.custom,
                            acct.device,
                            acct.token,
                            FTI_TYPE_ANDROID);
                    String realPath = ws.baseUrl(context) + FTI_Register;
                    RegisterResponse user = ws.callPostApi(realPath, data, RegisterResponse.class, acct.ticket);
                    if (user != null) {
                        acct.ticket = user.getTicket();
                        acct.acctId = user.getId();
                        acct.solo = false;
                        acct.confirmed = false;
                        acct.SyncPrime(false, context);
                    }

                } else {
                    publishProgress(false);
                    acct.tag = Integer.toString(NETWORK_DOWN);
                }
            } catch (Exception ex) {
                ExpClass.Companion.logEX(ex, this.getClass().getName() + ".run");
            }
            return acct;
        }
    }
}
