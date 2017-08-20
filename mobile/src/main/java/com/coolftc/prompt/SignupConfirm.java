package com.coolftc.prompt;

import static com.coolftc.prompt.utility.Constants.*;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;
import com.coolftc.prompt.source.WebServiceModels;
import com.coolftc.prompt.source.WebServices;
import com.coolftc.prompt.utility.ExpClass;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.concurrent.TimeUnit;

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

    private int mConfirmType = 0;
    private String mDsplAddr = "";
    private String mVerificationId = "";
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        // When saved data is also passed in normally, it needs to be restored here.
        if (savedInstanceState != null) {
            mDsplAddr = savedInstanceState.getString(IN_DSPL_TGT);
            mConfirmType = savedInstanceState.getInt(IN_CONFIRM_TYPE);
            if (mConfirmType == SMS_SIGNUP) {
                mVerificationId = savedInstanceState.getString(IN_SMS_VCODE);
                mResendToken = savedInstanceState.getParcelable(IN_SMS_RESEND);
            }
        }else {
            if (extras != null) {
                mDsplAddr = extras.getString(IN_DSPL_TGT);
                mConfirmType = extras.getInt(IN_CONFIRM_TYPE);
                if (mConfirmType == SMS_SIGNUP) {
                    mVerificationId = extras.getString(IN_SMS_VCODE);
                    mResendToken = extras.getParcelable(IN_SMS_RESEND);
                }
            }
        }

        setContentView(R.layout.signupconfirm);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        TextView holdView = (TextView) this.findViewById(R.id.lblTargetAddr_SC);
        if (mDsplAddr != null && holdView != null) holdView.setText(mDsplAddr);

        // Set up actions to perform upon phone number submission.
        mAuth = FirebaseAuth.getInstance();
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.

                // In this case we want to sign into google and then create the account and verify it.
                SignInVerify(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException ex) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                ExpClass.LogEX(ex, this.getClass().getName() + ".onVerificationFailed");

                // Problem creating account.
                DisplayProblem();
            }

            @Override
            public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;
            }
        };
    }

    /*
        In case the user navigates away from the screen to get their code
        we want to save off the important data.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(IN_CONFIRM_TYPE, mConfirmType);
        outState.putString(IN_DSPL_TGT, mDsplAddr);
        outState.putString(IN_SMS_VCODE, mVerificationId);
        outState.putParcelable(IN_SMS_RESEND, mResendToken);
        super.onSaveInstanceState(outState);
    }

    /*
        Recreate the account from saved data, it will just generate a new ticket and resend a verification.
     */
    public void ResendCode(View view) {
        // Clear out the code if any has been entered.
        TextView holdView = (TextView)findViewById(R.id.txtConfirmCode_SC);
        if(holdView!=null) holdView.setText("");

        switch (mConfirmType) {
            case EMAIL_SIGNUP :
                new AcctResendTask(SignupConfirm.this, getResources().getString(R.string.app_name)).execute();
                break;
            case SMS_SIGNUP :
                Actor user = new Actor(this);
                PhoneAuthProvider.getInstance().verifyPhoneNumber(
                        user.unique,        // Phone number to verify
                        60,                 // Timeout duration
                        TimeUnit.SECONDS,   // Unit of timeout
                        this,               // Activity (for callback binding)
                        mCallbacks,         // OnVerificationStateChangedCallbacks
                        mResendToken);      // ForceResendingToken from callbacks
                break;
        }
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
            DisplayProblem();
        }
    }

    /*
        If there is a problem with the resend, show the problem display.
     */
    private void ResendComplete(boolean success) {

        if(!success) {
            DisplayProblem();
        }
    }

    /*
        The error copy pushes the Resend button down when it is made visible.
        To make sure the button stays visible, hide the input device.
     */
    private void DisplayProblem(){
        TextView holdView = (TextView) findViewById(R.id.lblError_SC);
        if (holdView != null) holdView.setVisibility(View.VISIBLE);
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /*
        Verify the account with the entered code.
     */
    public void Verification(View view) {
        TextView holdView = (TextView)findViewById(R.id.txtConfirmCode_SC);
        String txtCode = "";
        if(holdView!=null) txtCode = holdView.getText().toString();
        if(txtCode.length()==0) return;

        switch (mConfirmType) {
            case EMAIL_SIGNUP:
                // For internal confirmation, there is no provider/credential.
                new AcctVerifyTask(SignupConfirm.this, getResources().getString(R.string.app_name)).execute(
                        txtCode,
                        "",
                        "");
                break;
            case SMS_SIGNUP:
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, txtCode);
                SignInVerify(credential);
                break;
        }
    }

    /*
        When verification completes, need to acquire token to verify on the backend.
        This is actually a three step process, but performed with callbacks in separate methods:
        SignInVerify -> PerformFirebaseVerify -> AcctVerifyTask
     */
    private void SignInVerify(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    PerformFirebaseVerify(task.getResult().getUser());
                } else {
                    ExpClass.LogEX(task.getException(), this.getClass().getName() + ".SignInVerify");
                    // Problem creating account.
                    DisplayProblem();
                }
            }
        });
    }

    /*
        Continuation of the SignInVerify chain of events.  The sign-in token (a jwt object)
        is required for the Prompt backend to make sure a person is not faking a firebase login.
     */
    private void PerformFirebaseVerify(final FirebaseUser user){
        user.getIdToken(false).addOnCompleteListener(this, new OnCompleteListener<GetTokenResult>() {
            @Override
            public void onComplete(@NonNull Task<GetTokenResult> task) {
                if (task.isSuccessful()){
                    new AcctVerifyTask(SignupConfirm.this, getResources().getString(R.string.app_name)).execute(
                            FTI_FIREBASE_VERIFY,
                            user.getUid(),
                            task.getResult().getToken());
                }else {
                    ExpClass.LogEX(task.getException(), this.getClass().getName() + ".PerformFirebaseVerify");
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
                confirm.code = Long.parseLong(criteria[0]);   // server knows diff between internal/external codes
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
    private class AcctResendTask extends AsyncTask<Void, Boolean, Boolean> {
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

        protected void onPostExecute(Boolean success) {
            ResendComplete(success);
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
        protected Boolean doInBackground(Void... criteria) {

            Actor acct = new Actor(context);
            WebServices ws = new WebServices();
            boolean success = false;
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
                    acct.acctId = user.id;
                    acct.SyncPrime(false, context);
                    success = true;
                }
            } else {
                publishProgress(false);
            }
            return success;
        }
    }

}
