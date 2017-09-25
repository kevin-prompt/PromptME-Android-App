package com.coolftc.prompt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.coolftc.prompt.source.WebServiceModels;
import com.coolftc.prompt.source.WebServices;
import com.google.firebase.iid.FirebaseInstanceId;
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
            mEmailAddr = FirebaseInstanceId.getInstance().getId();

        if(mEmailAddr == null || mEmailAddr.length() == 0){
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
        TextView holdView = (TextView) findViewById(R.id.lblError_SO);
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
        private Context context;
        private String title;

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
            WebServices ws = new WebServices();
            if (ws.IsNetwork(context)) {
                WebServiceModels.RegisterRequest regData = new WebServiceModels.RegisterRequest();
                regData.uname = criteria[0];
                acct.unique = regData.uname;
                regData.dname = criteria[1];
                acct.display = regData.dname.length() > 0 ? regData.dname : getString(R.string.mysteryme);
                regData.scycle = Integer.parseInt(criteria[2]);
                acct.sleepcycle = regData.scycle;
                regData.cname = criteria[3];
                acct.custom = regData.cname;
                regData.timezone = TimeZone.getDefault().getID();
                acct.timezone = regData.timezone;
                regData.device = acct.device;
                regData.target = acct.token;
                regData.type = FTI_TYPE_ANDROID;
                regData.verify = false; // Don't send out a verification code

                WebServiceModels.RegisterResponse user = ws.Registration(regData);
                if (user.response >= 200 && user.response < 300) {
                    acct.ticket = user.ticket;
                    acct.acctId = user.id;
                    if (!regData.verify) {
                        WebServiceModels.VerifyRequest confirm = new WebServiceModels.VerifyRequest();
                        confirm.code = Long.parseLong(criteria[4]);
                        confirm.provider = criteria[5];
                        confirm.credential = criteria[6];
                        int rtn = ws.Verify(acct.ticket, acct.acctIdStr(), confirm);
                        if (rtn >= 200 && rtn < 300) {
                            acct.confirmed = true;
                            acct.solo = true;
                        }
                    }
                    acct.SyncPrime(false, context);
                } else {
                    acct.tag = Integer.toString(user.response);
                }
            } else {
                publishProgress(false);
                acct.tag = Integer.toString(NETWORK_DOWN);
            }
            return acct;
        }
    }

}
