package com.coolftc.prompt;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.coolftc.prompt.source.WebServiceModels;
import com.coolftc.prompt.source.WebServices;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        Actor acct = new Actor(this);
        TextView holdData;

        // Initialize the about data
        holdData = (TextView) findViewById(R.id.abtVersion);
        if(holdData != null) holdData.setText(R.string.ver_nbr);
        holdData = (TextView) findViewById(R.id.abtBuildDate);
        if(holdData != null) holdData.setText(R.string.ver_build);
        holdData = (TextView) findViewById(R.id.abtBroadcast);
        if(holdData != null) holdData.setText(acct.isBroadcast(getApplicationContext()));
        holdData = (TextView) findViewById(R.id.abtAds);
        if(holdData != null) holdData.setText(acct.isAds(getApplicationContext()));
        holdData = (TextView) findViewById(R.id.abtWhoAmI);
        if(holdData != null) holdData.setText(acct.unique);
        holdData = (TextView) findViewById(R.id.abtAccount);
        if(holdData != null) holdData.setText("(" + acct.acctIdStr() + ")");

        new CheckSystemTask(getApplicationContext()).execute(acct.ticket);
    }

    /*
     *  This displays the result of a system status call.
     */
    private void SystemCheck(WebServiceModels.PingResponse ver) {
        TextView holdView;
        String holdResult;

        // Success
        if(ver.response >= 200 && ver.response <300){
            holdResult = String.format(getResources().getText(R.string.abt_sysup).toString(), ver.version);
        }else{
            switch (ver.response){
                case 0:
                    holdResult = getResources().getText(R.string.err_cannot_connect).toString();
                    break;
                case 400:
                    holdResult = String.format(getResources().getText(R.string.err_bad_request).toString(), ver.response);
                    break;
                case 401:
                    holdResult = String.format(getResources().getText(R.string.err_bad_auth).toString(), ver.response);
                    break;
                case 404:
                    holdResult = String.format(getResources().getText(R.string.err_bad_request).toString(), ver.response);
                    break;
                default:
                    holdResult = String.format(getResources().getText(R.string.err_bad_server).toString(), ver.response);
                    break;
            }
        }

        holdView = (TextView) this.findViewById(R.id.abtSystemVer);
        if (holdView != null) { holdView.setText(holdResult); }
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
    private class CheckSystemTask extends AsyncTask<String, Void, WebServiceModels.PingResponse> {
        private Context context;

        public CheckSystemTask(Context startup) {
            context = startup;
        }

        /*
        *  This is a quick check to see that there is network, our server is reachable
        *  and the ticket they are using is viable. Returns the server version, too.
        */
        @Override
        protected WebServiceModels.PingResponse doInBackground(String... criteria) {
            try {
                WebServices stat = new WebServices();
                String ticket = criteria[0];

                if(stat.IsNetwork(context)) {
                    return stat.GetVersion(ticket);
                }else {
                    return new WebServiceModels.PingResponse();
                }

            } catch (Exception ex) {
                return new WebServiceModels.PingResponse();
            }
        }

        @Override
        protected void onPostExecute(WebServiceModels.PingResponse result) {
            SystemCheck(result);
        }
    }
}
