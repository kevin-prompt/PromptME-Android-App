package com.coolftc.prompt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coolftc.prompt.source.RegisterRequest;
import com.coolftc.prompt.source.RegisterResponse;
import com.coolftc.prompt.source.VerifyRequest;
import com.coolftc.prompt.source.VerifyResponse;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import java.util.TimeZone;
import static com.coolftc.prompt.utility.Constants.*;

/**
 *  Some people may not want to offer up their email or phone number, but we can
 *  give them a limited version of the functionality that is safe.  Specifically,
 *  removing access to invites, and linking to others, is really the only abusive
 *  action available, therefore, this just removes the opportunity to do that.
 *  The primary functionality of the App, self Prompts, is fully available.  To
 *  support this in the overall framework, an email address is generated using
 *  the unique device name added to the zalicon.com domain.  Then by not asking
 *  to trigger a verification, but doing a special verification, the server magically
 *  creates a verified user without the ability to be social.
 */
public class SignupSolo extends AppCompatActivity {

    private String mDsplName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mDsplName = getResources().getString(R.string.mysteryme);
        if(extras != null){
            mDsplName = extras.getString(IN_DSPL_NAME, getResources().getString(R.string.mysteryme));
        }

        setContentView(R.layout.signupsolo);
    }

    public void SignupReturn(View view){
        onBackPressed(); // Only path here is from Signup, so this seems reasonable.
    }

    public void SoleSignup(View view) {
        String mEmailAddr;
        Actor user = new Actor(this);

        if (user.device.length() > 0)
            mEmailAddr = user.device + FTI_SOLO_DOMAIN;
        else
            mEmailAddr = user.identifier() + FTI_SOLO_DOMAIN;

        if(mEmailAddr.equalsIgnoreCase(FTI_SOLO_DOMAIN)){
            DisplayProblem();
            return;
        }

        new AcctCreateVerifyTask(SignupSolo.this, getResources().getString(R.string.app_name)).execute(
                mEmailAddr,
                mDsplName,
                getResources().getString(R.string.prf_SleepCycleDefault),
                mEmailAddr,
                FTI_SOLO_VERIFY,
                "",
                "");
    }

    /*
        The error copy.
     */
    private void DisplayProblem(){
        TextView holdView = findViewById(R.id.lblError_SO);
        if (holdView != null) holdView.setVisibility(View.VISIBLE);
    }

    /*
        Once created, the Account should be ready to go.
     */
    private void  AccountCreated(Actor acct){
        if(acct.ticket.length() > 0) {
            if (!acct.confirmed) {
                DisplayProblem();
            }
            else {
                // Worked...send to welcome.
                Settings.setDisplayName(this, acct.display);
                Intent intent = new Intent(this, Welcome.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }
        else {
            // Problem creating account.
            DisplayProblem();
        }
    }


    /**
     * The nested AsyncTask class is used to off-load the network call to a separate
     * thread but allow quick feedback to the user.
     * Considerations:  Memory can leak as an inner class holds a reference to outer.
     * 1) Create as an explicit inner class, not an antonymous one.
     * 2) Pass in the Application context if possible, not an Activity context. In that
     *      case you cannot use the UI, e.g. no progressDialog, but you can call a method.
     * 3) Make the work in the background single pass and likely to complete (quickly).
     * 4) If possible prevent the most common Activity killer by locking into portrait.
     * 5) Avoid use in parts of the App that get used a lot, e.g. use lazy data refresh design.
     */
    private class AcctCreateVerifyTask extends AsyncTask<String, Boolean, Actor> {
        private ProgressDialog progressDialog;
        private final Context context;
        private final String title;

        public AcctCreateVerifyTask(AppCompatActivity activity, String name) {
            context = activity;
            title = name;
        }

        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(context, title, context.getResources().getString(R.string.processing), true, true);
            progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    cancel(true);
                }
            });
        }

        protected void onPostExecute(Actor result) {
            AccountCreated(result);
            progressDialog.dismiss();
        }

        protected void onCancelled() {
            AccountCreated(new Actor(context));
        }

        protected void onProgressUpdate(Boolean... values) {
            if (!values[0]) Toast.makeText(context, R.string.msgNoNet, Toast.LENGTH_LONG).show();
        }

        /*
         * This assumes verification has been completed elsewhere, and will create and then
         * verify (or really confirm verification) of the account.  If the account is
         * created, we also sync the local account data.
         * Input criteria:
         ** Creation **
         * 0 - Unique Name (SMS/email)
         * 1 - Display Name
         * 2 - Sleep Cycle
         * 3 - Custom Data (e.g. external id)
         ** Verification **
         * 4 - Verify Code (firebase, internal, solo)
         * 5 - Provider Key (for external verification)
         * 6 - Credential (for external verification)
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
                            false,  // This is different than Email
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
                        acct.confirmed = false;
                        if (!data.getVerify()) {    // Seems to always execute here (i.e. verify always false).
                            VerifyRequest confirm = new VerifyRequest(
                                    Long.parseLong(criteria[4]),
                                    criteria[5],
                                    criteria[6]);
                            realPath = ws.baseUrl(context) + FTI_RegisterExtra.replace(SUB_ZZZ, acct.acctIdStr());
                            VerifyResponse rtn = ws.callPutApi(realPath, confirm, VerifyResponse.class, acct.ticket);
                            if (rtn != null) {
                                acct.confirmed = rtn.getVerified();
                                acct.solo = true;
                            }
                        }
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
