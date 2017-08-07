package com.coolftc.prompt;

import static com.coolftc.prompt.utility.Constants.*;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


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

        // Set up main view and menu.
        setContentView(R.layout.signup);
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
     *  If they want to use an phone number, switch to that signup process.
     */
    public void SMSVerification(View view) {
        TextView holdView = (TextView)findViewById(R.id.txtDisplayName);
        String txtDspl = getResources().getString(R.string.mysteryme);
        if(holdView!=null) txtDspl = holdView.getText().toString();

        Intent phoneConfirm = new Intent(this, SignupSMS.class);
        phoneConfirm.putExtra(IN_DSPL_NAME, txtDspl);
        startActivity(phoneConfirm);
    }

}
