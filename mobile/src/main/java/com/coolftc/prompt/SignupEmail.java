package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

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

    private String dsplName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        dsplName = getResources().getString(R.string.mysteryme);
        if(extras != null){
            dsplName = extras.getString(IN_DSPL_NAME, getResources().getString(R.string.mysteryme));
        }

        setContentView(R.layout.signupemail);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /*
        Ready to go, try and create the account.
     */
    public void EmailCreateAcct(View view) {
        TextView holdView = (TextView)findViewById(R.id.txtEmailAddr_SE);
        String txtEmailAddr = "";
        if(holdView!=null) txtEmailAddr = holdView.getText().toString();
        if(txtEmailAddr.length()==0) return;

        // Create an account, then move on to verify page
        new AcctCreateTask(SignupEmail.this, getResources().getString(R.string.app_name)).execute(
                txtEmailAddr,
                dsplName,
                "");
    }

    /*
        Once created, the email with the verification code should be on
        its way, so go to the next step.
     */
    private void  AccountCreated(Account acct){
        if(acct.ticket.length() > 0) {
            if (!acct.confirmed) {
                // Verify Screen
                Intent confirmScreen = new Intent(this, SignupConfirm.class);
                confirmScreen.putExtra(IN_DSPL_TGT, acct.unique);
                startActivity(confirmScreen);
            }
            else {
                // Already Verified? OK...send to welcome.
            }
        }
        else {
            // Problem creating account
            TextView holdView = (TextView) findViewById(R.id.lblError_SE);
            if (holdView != null)
                holdView.setTextColor(ContextCompat.getColor(this, R.color.promptproblem));
        }
    }

    /*
     * The nested AsyncTask class is used to off-load the network call to a separate thread.
     * Considerations:  If the parent activity is destroyed, the code can cause trouble, so
     * try to avoid unnecessary re-creations of the activity, e.g. context switches.
     * Input criteria:
     ** Creation **
     * 0 - Unique Name (SMS/email)
     * 1 - Display Name
     * 2 - Custom Data (e.g. external id)
     */
    private class AcctCreateTask extends AsyncTask<String, Boolean, Account> {
        private ProgressDialog progressDialog;
        private Context context;
        private String title;

        public AcctCreateTask(AppCompatActivity activity, String name){
            context = activity;
            title = name;
        }

        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, title, context.getResources().getString(R.string.processing), true, true);
            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) { cancel(true); } });
        }

        protected void onPostExecute(Account result) {
            AccountCreated(result);
            progressDialog.dismiss();
        }

        protected void onCancelled() {
            //Toast.makeText(context, R.string.msgUserCancel, Toast.LENGTH_LONG).show();
            AccountCreated(new Account(context, true));
        }

        protected void onProgressUpdate(Boolean... values) {
            if(!values[0]) Toast.makeText(context, R.string.msgNoNet, Toast.LENGTH_LONG).show();
        }

        /*
         * This assumes verification will come later and just creates the account
         * here. If the account is created, we also sync the local account data.
         */
        protected Account doInBackground(String... criteria) {

            Account acct = new Account(context, true);
            WebServices ws = new WebServices();
            if(ws.IsNetwork(context)) {
                WebServiceModels.RegisterRequest regData = new WebServiceModels.RegisterRequest();
                regData.uname = criteria[0];
                acct.unique = regData.uname;
                regData.dname = criteria[1];
                acct.display = regData.dname;
                regData.cname = criteria[2];
                acct.custom = regData.cname;
                regData.timezone = TimeZone.getDefault().getID();
                acct.timezone = regData.timezone;
                regData.device = acct.device;
                regData.target = acct.token;
                regData.type = FTI_TYPE_ANDROID;
                regData.verify = true; // Send out a verification code

                WebServiceModels.RegisterResponse user = ws.Registration(regData);
                if(user.response >= 200 && user.response < 300) {
                    acct.ticket = user.ticket;
                    acct.acctId = user.id;
                    acct.SyncPrime(false, context);
                }
            } else {
                publishProgress(false);
            }
            return acct;
        }
    }

}
