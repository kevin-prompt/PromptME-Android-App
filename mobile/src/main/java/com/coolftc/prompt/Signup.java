package com.coolftc.prompt;

import static com.coolftc.prompt.Constants.*;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.Digits;
import com.digits.sdk.android.DigitsAuthButton;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsOAuthSigning;
import com.digits.sdk.android.DigitsSession;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;
import java.util.Map;
import java.util.TimeZone;

import io.fabric.sdk.android.Fabric;

/**
 *  The Signup process is a minimal account setup to establish a unique
    contact ID/method using either SMS or email.  At the same time, a
    non-unique name is also created to allow some personalization.
    For SMS, Digits handles everything after collection, then verification
    is more a matter of confirming between servers.

   The Signup screens are set to only be portrait to avoid some UI and
   Technical complications. Given the one time use of the screen, it
    should not be a big deal. Preventing a portrait/landscape context
    switch should help avoid problems with the AsynchTask used in the
    account creation/verification. Other context changes are much less
    common.
 */
public class Signup extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Digits
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new TwitterCore(authConfig), new Digits.Builder().build());

        // Set up main view and menu.
        setContentView(R.layout.signup);

        // The Digits button can only be manipulated in code, xml does not work.
        // For the most part, I am trying to get the button to match the one I
        // have for email verification (which is done in layout).
        DigitsAuthButton digitsButton = (DigitsAuthButton) findViewById(R.id.auth_button);
        digitsButton.setAuthTheme(R.style.CustomDigitsTheme);
        digitsButton.setText(R.string.btnPhoneNbr);
        digitsButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        digitsButton.setTypeface(null, Typeface.NORMAL);
        digitsButton.setBackgroundColor(ContextCompat.getColor(this, R.color.promptbluedk));
        digitsButton.setCallback(new AuthCallback() {
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                // Success - Get proof of verification and create account as verified.
                TwitterAuthConfig authConfig = TwitterCore.getInstance().getAuthConfig();
                TwitterAuthToken authToken = session.getAuthToken();
                DigitsOAuthSigning oauthSigning = new DigitsOAuthSigning(authConfig, authToken);
                Map<String, String> holdCredentials = oauthSigning.getOAuthEchoHeadersForVerifyCredentials();
                TextView holdView = (TextView)findViewById(R.id.txtDisplayName);
                String txtDspl = getResources().getString(R.string.mysteryme);
                if(holdView!=null) txtDspl = holdView.getText().toString();
                new AcctCreateVerifyTask(Signup.this, getResources().getString(R.string.app_name)).execute(
                        session.getPhoneNumber(),
                        txtDspl,
                        Long.toString(session.getId()),
                        FTI_DIGIT_VERIFY,
                        holdCredentials.get("X-Auth-Service-Provider"),
                        holdCredentials.get("X-Verify-Credentials-Authorization"));
            }

            @Override
            public void failure(DigitsException ex) {
                 ExpClass.LogEX(ex, this.getClass().getName() + ".DigitsButton");
            }
        });
    }

    /*
     *  If they want to use an email address, switch to that signup process.
     */
    public void EmailVerification(View view) {
        TextView holdView = (TextView)findViewById(R.id.txtDisplayName);
        String txtDspl = getResources().getString(R.string.mysteryme);
        if(holdView!=null) txtDspl = holdView.getText().toString();

        Intent emailConfirm = new Intent(this, SignupEmail.class);
        emailConfirm.putExtra(IN_DSPL_NAME, txtDspl);
        startActivity(emailConfirm);
    }

    /*
        Called at the end of the account creation/verification task.
     */
    private void  SignupComplete(Account acct){
        String holdTicket = acct.ticket;
        long holdID = acct.acctId;
        Intent intent = new Intent(this, Welcome.class);
        startActivity(intent);
    }

    /*
     * The nested AsyncTask class is used to off-load the network call to a separate thread.
     * Considerations:  If the parent activity is destroyed, the code can cause trouble, so
     * try to avoid unnecessary re-creations of the activity, e.g. context switches.
     */
    private class AcctCreateVerifyTask extends AsyncTask<String, Boolean, Account> {
        private ProgressDialog progressDialog;
        private Context context;
        private String title;

        public AcctCreateVerifyTask(AppCompatActivity activity, String name){
            context = activity;
            title = name;
        }

        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, title, context.getResources().getString(R.string.processing), true, true);
            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() { @Override public void onDismiss(DialogInterface dialog) { cancel(true); } });
        }

        protected void onPostExecute(Account result) {
            SignupComplete(result);
            progressDialog.dismiss();
        }

        protected void onCancelled() {
            SignupComplete(new Account(context, true));
        }

        protected void onProgressUpdate(Boolean... values) {
            if(!values[0]) Toast.makeText(context, R.string.msgNoNet, Toast.LENGTH_LONG).show();
        }

        /*
         * This assumes verification has been completed elsewhere, and will create and then
         * verify (or really confirm verification) of the account.  If the account is
         * created, we also sync the local account data.
         * Input criteria:
         ** Creation **
         * 0 - Unique Name (SMS/email)
         * 1 - Display Name
         * 2 - Custom Data (e.g. external id)
         ** Verification **
         * 3 - Verify Code (digits, internal)
         * 4 - Provider Key (for external verification)
         * 5 - Credential (for external verification)
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
                regData.verify = false; // Don't send out a verification code

                WebServiceModels.RegisterResponse user = ws.Registration(regData);
                if(user.response >= 200 && user.response < 300) {
                    acct.ticket = user.ticket;
                    acct.acctId = user.id;
                    if(!regData.verify) {
                        WebServiceModels.VerifyRequest confirm = new WebServiceModels.VerifyRequest();
                        confirm.code = Long.parseLong(criteria[3]);   // code 2 = digits
                        confirm.provider = criteria[4];
                        confirm.credential = criteria[5];
                        int rtn = ws.Verify(acct.ticket, acct.acctIdStr(), confirm);
                        if (rtn >= 200 && rtn < 300) {
                            acct.confirmed = true;
                        }
                    }
                    acct.SyncPrime(false, context);
                }
            }else{
                publishProgress(false);
            }
            return acct;
        }
    }

}
