package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

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
public class SignupConfirm extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            displayTargetAddr(extras.getString(IN_DSPL_TGT));
        }

        setContentView(R.layout.signupconfirm);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

    }

    /*
        Verify the account with the entered code.
     */
    public void Verification(View view) {
        TextView holdView = (TextView)findViewById(R.id.txtConfirmCode_SC);
        String txtCode = "";
        if(holdView!=null) txtCode = holdView.getText().toString();
        if(txtCode.length()==0) return;

        // For internal confirmation, there is no provider/credential.
        new AcctVerifyTask(SignupConfirm.this, getResources().getString(R.string.app_name)).execute(
                txtCode,
                "",
                "");
    }

    /*
        Recreate the account from saved data, it will just generate a new ticket and resend a verification.
     */
    public void ResendCode(View view) {
        new AcctResendTask(SignupConfirm.this, getResources().getString(R.string.app_name)).execute();
    }

    /*
        If everything works out, send along to Welcome screen.
        Also, initialize the Settings with the name.
     */
    private void SignupComplete(Actor acct){

        if(acct.confirmed) {
            // All set... onward to welcome.
            Settings.setDisplayName(this, acct.display);
            Intent intent = new Intent(this, Welcome.class);
            startActivity(intent);
        }else{
            // The error copy pushes the Resend button down when it is made visible.
            // To make sure the button stays visible, hide the input device.
            TextView holdView = (TextView) findViewById(R.id.lblError_SC);
            if (holdView != null) holdView.setVisibility(View.VISIBLE);
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    /*
        Show the email address on the screen.
     */
    private void displayTargetAddr(String target) {
        TextView holdView;

        holdView = (TextView) this.findViewById(R.id.lblTargetAddr_SC);
        if (target != null && holdView != null) holdView.setText(target);
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
    private class AcctVerifyTask extends AsyncTask<String, Boolean, Actor> {
        private ProgressDialog progressDialog;
        private Context context;
        private String title;

        public AcctVerifyTask(AppCompatActivity activity, String name){
            context = activity;
            title = name;
        }

        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, title, context.getResources().getString(R.string.processing), true, true);
            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) { cancel(true); } });
        }

        protected void onPostExecute(Actor result) {
            SignupComplete(result);
            progressDialog.dismiss();
        }

        protected void onCancelled() {
            SignupComplete(new Actor(context));
        }

        protected void onProgressUpdate(Boolean... values) {
            if(!values[0]) Toast.makeText(context, R.string.msgNoNet, Toast.LENGTH_LONG).show();
        }

        /*
         * This tries to verify the account based on the supplied code.
         * This assumes verification has been completed elsewhere, and will create and then
         * verify (or really confirm verification) of the account.  If the account is
         * created, we also sync the local account data.
         * Input criteria:
         ** Verification **
         * 0 - Verify Code (digits, internal)
         * 1 - Provider Key (for external verification)
         * 2 - Credential (for external verification)
         */
        protected Actor doInBackground(String... criteria) {

            Actor acct = new Actor(context);
            WebServices ws = new WebServices();
            if(ws.IsNetwork(context)) {
                WebServiceModels.VerifyRequest confirm = new WebServiceModels.VerifyRequest();
                confirm.code = Long.parseLong(criteria[0]);   // code supplied
                confirm.provider = criteria[1];
                confirm.credential = criteria[2];
                int rtn = ws.Verify(acct.ticket, acct.acctIdStr(), confirm);
                if (rtn >= 200 && rtn < 300) {
                    acct.confirmed = true;
                    acct.SyncPrime(false, context);
                }
            } else {
                publishProgress(false);
            }
            return acct;
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
    private class AcctResendTask extends AsyncTask<Void, Boolean, Actor> {
        private ProgressDialog progressDialog;
        private Context context;
        private String title;

        public AcctResendTask(AppCompatActivity activity, String name){
            context = activity;
            title = name;
        }

        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, title, context.getResources().getString(R.string.processing), true, true);
            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) { cancel(true); } });
        }

        protected void onPostExecute(Actor result) {
            SignupComplete(result);
            progressDialog.dismiss();
        }

        protected void onCancelled() {
            SignupComplete(new Actor(context));
        }

        protected void onProgressUpdate(Boolean... values) {
            if(!values[0]) Toast.makeText(context, R.string.msgNoNet, Toast.LENGTH_LONG).show();
        }

        /*
         * This assumes verification will come later and just creates the account
         * here. If the account is created, we also sync the local account data.
         * Input criteria: This gets all input data from existing Account data.
         */
        protected Actor doInBackground(Void... criteria) {

            Actor acct = new Actor(context);
            WebServices ws = new WebServices();
            if(ws.IsNetwork(context)) {
                WebServiceModels.RegisterRequest regData = new WebServiceModels.RegisterRequest();
                regData.uname = acct.unique;
                regData.dname = acct.display;
                regData.cname = acct.custom;
                regData.timezone = acct.timezone;
                regData.device = acct.device;
                regData.target = acct.token;
                regData.type = FTI_TYPE_ANDROID;
                regData.verify = true; // Send out a verification code

                WebServiceModels.RegisterResponse user = ws.Registration(regData);
                if(user.response >= 200 && user.response < 300) {
                    acct.ticket = user.ticket;
                    acct.SyncPrime(false, context);
                }
            } else {
                publishProgress(false);
            }
            return acct;
        }
    }

}
