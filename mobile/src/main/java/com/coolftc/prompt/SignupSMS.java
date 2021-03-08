package com.coolftc.prompt;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.coolftc.prompt.source.RegisterRequest;
import com.coolftc.prompt.source.RegisterResponse;
import com.coolftc.prompt.source.VerifyRequest;
import com.coolftc.prompt.source.VerifyResponse;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.WebServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.gson.Gson;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.coolftc.prompt.utility.Constants.*;

/**
 *  The Signup process is a minimal account setup to establish a unique
    contact ID/method using either SMS or email.  At the same time, a
    non-unique name is also created to allow some personalization.
    For SMS, the Google Firebase authentication service is used. After
    triggering an SMS code to the entered number, an account is created.
    If Firebase determines the user does not need to perform a confirmation,
    a Prompt verification is also performed. Otherwise, the SignupConfirm
    screen performs the verification.

    The Signup screens are set to only be portrait to avoid some UI and
    Technical complications. Given the one time use of the screen, it
    should not be a big deal. Preventing a portrait/landscape context
    switch should help avoid problems with the AsynchTask used in the
    account creation/verification. Other context changes are much less
    common.
 */
public class SignupSMS extends AppCompatActivity {

    private String mDsplName = "";
    private String mPhoneNbr = "";
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        mDsplName = getResources().getString(R.string.mysteryme);
        if(extras != null){
            mDsplName = extras.getString(IN_DSPL_NAME, getResources().getString(R.string.mysteryme));
        }

        setContentView(R.layout.signupsms);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        // Set up actions to perform upon phone number submission.
        mAuth = FirebaseAuth.getInstance();
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.

                // In this case we want to sign into google and then create the account and verify it.
                SignInCreateVerify(credential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException ex) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                ExpClass.Companion.logEX(ex, this.getClass().getName() + ".onVerificationFailed");

                // Problem creating account.
                TextView holdView = findViewById(R.id.lblError_SE);
                if (holdView != null)
                    holdView.setTextColor(ContextCompat.getColor(SignupSMS.this, R.color.promptwhite));
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                // If the user is a Solo, passing in the existing unique name as the
                // custom name will trigger the server to match up the accounts.
                Actor user = new Actor(getApplicationContext());
                String soloName = user.solo ? user.unique : "";

                // Create the account, then verify on next step.
                new AcctCreateTask(SignupSMS.this, getResources().getString(R.string.app_name)).execute(
                        mPhoneNbr,
                        mDsplName,
                        getResources().getString(R.string.prf_SleepCycleDefault),
                        soloName);
            }
        };
    }

    /*
        Ready to go, trigger the verification and try and create the account.
     */
    public void PhoneCreateAcct(View view) {
        TextView holdView = findViewById(R.id.txtPhoneAddr_SE);
        if (holdView != null) mPhoneNbr = holdView.getText().toString();
        PhoneNumberUtil phoneHelper = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber fullNbr = phoneHelper.parse(mPhoneNbr, "US");
            String holdnbr = phoneHelper.format(fullNbr, PhoneNumberUtil.PhoneNumberFormat.E164);
            mPhoneNbr = holdnbr.replace("+", "");
        } catch (NumberParseException e) {
            mPhoneNbr = "";
            ExpClass.Companion.logEX(e, this.getClass().getName() + "phoneNumberFormatter");
        }
        if (mPhoneNbr.length() == 0) return;
/*
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                mPhoneNbr,          // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
*/
        PhoneAuthProvider.verifyPhoneNumber(
                new PhoneAuthOptions.Builder(FirebaseAuth.getInstance())
                .setPhoneNumber(mPhoneNbr)                  // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS)   // Timeout duration & Unit of timeout
                .setCallbacks(mCallbacks)                   // OnVerificationStateChangedCallbacks
                .build());
    }

    /*
        Once created, the SMS with the verification code should be on
        its way, so go to the next step.  In some circumstances, the
        verification has taken place already (so skip confirm).
     */
    private void  AccountCreated(Actor acct){
        if(acct.ticket.length() > 0) {
            if (!acct.confirmed) {
                // Verify Screen - this is the normal path.
                Intent confirmScreen = new Intent(this, SignupConfirm.class);
                confirmScreen.putExtra(IN_DSPL_TGT, acct.unique);
                confirmScreen.putExtra(IN_SMS_VCODE, mVerificationId);
                confirmScreen.putExtra(IN_SMS_RESEND, mResendToken);
                confirmScreen.putExtra(IN_CONFIRM_TYPE, SMS_SIGNUP);
                startActivity(confirmScreen);
            }
            else {
                // Already Verified? OK...send to welcome.
                Settings.setDisplayName(this, acct.display);
                Intent intent = new Intent(this, Welcome.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }
        else {
            // Problem creating account.
            TextView holdView = findViewById(R.id.lblError_SE);
            if (holdView != null)
                holdView.setTextColor(ContextCompat.getColor(this, R.color.promptwhite));
            if (acct.tag.length() > 0){
                holdView = findViewById(R.id.lblSmsStatus);
                if (holdView != null) {
                    holdView.setVisibility(View.VISIBLE);
                    String holdStat = getString(R.string.status) + ":" + acct.tag;
                    holdView.setText(holdStat);
                }
            }
        }
    }

    /*
        The error copy pushes the Resend button down when it is made visible.
        To make sure the button stays visible, hide the input device.
     */
    private void DisplayProblem(){
        TextView holdView = findViewById(R.id.lblError_SE);
        if (holdView != null)
            holdView.setTextColor(ContextCompat.getColor(SignupSMS.this, R.color.promptwhite));
    }

    /*
        When verification completes, need to acquire token to verify on the backend.
        This is actually a three step process, but performed with callbacks in separate methods:
        SignInVerify -> PerformFirebaseVerify -> AcctVerifyTask
     */
    private void SignInCreateVerify(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    PerformFirebaseVerify(task.getResult().getUser());
                } else {
                    ExpClass.Companion.logEX(task.getException(), this.getClass().getName() + ".SignInCreateVerify");
                    // Problem creating account.
                    DisplayProblem();
                }
            }
        });
    }

    /*
        Continuation of the SignInVerify chain of events.
     */
    private void PerformFirebaseVerify(final FirebaseUser device){
        device.getIdToken(false).addOnCompleteListener(this, new OnCompleteListener<GetTokenResult>() {
            @Override
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                if (task.isSuccessful()){
                    // If the user is a Solo, passing in the existing unique name as the
                    // custom name will trigger the server to match up the accounts.
                    Actor user = new Actor(getApplicationContext());
                    String soloName = user.solo ? user.unique : "";

                    new AcctCreateVerifyTask(SignupSMS.this, getResources().getString(R.string.app_name)).execute(
                            mPhoneNbr,
                            mDsplName,
                            getResources().getString(R.string.prf_SleepCycleDefault),
                            soloName,
                            FTI_FIREBASE_VERIFY,
                            device.getUid(),
                            task.getResult().getToken());
                }else {
                    ExpClass.Companion.logEX(task.getException(), this.getClass().getName() + ".PerformFirebaseVerify");
                    DisplayProblem();
                }
            }
        });
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
         * 4 - Verify Code (firebase, internal)
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
                        acct.solo = false;
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
