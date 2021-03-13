package com.coolftc.prompt;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.coolftc.prompt.service.PingServerThread;
import java.lang.ref.WeakReference;
import static com.coolftc.prompt.utility.Constants.*;

public class About extends AppCompatActivity {
    /*
        This handler is used to as a callback mechanism, such that the Service
         can alert the Activity when the cache has been updated.  This is how
         the "loading" animation, which is shown when the cache is empty, can
         be replaced with a real animation when the cache has data.
     */
    private static class MsgHandler extends Handler {
        private final WeakReference<About> mActivity;

        MsgHandler(About activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            About activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case WHAT_OK_PING:
                        activity.CheckPingOk((String)msg.obj, msg.arg1);
                        break;
                    case WHAT_ERR_PING:
                        activity.CheckPingError(msg.arg1);
                        break;
                }
            }
        }
    }
    private final MsgHandler mHandler = new MsgHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        Actor acct = new Actor(this);
        TextView holdData;

        // Initialize the about data
        holdData = findViewById(R.id.abtVersion);
        if(holdData != null) holdData.setText(R.string.ver_nbr);
        holdData = findViewById(R.id.abtBuildDate);
        if(holdData != null) holdData.setText(R.string.ver_build);
        holdData = findViewById(R.id.abtOsVersion);
        if(holdData != null) holdData.setText(String.format(getResources().getString(R.string.os_version_dtl), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        holdData = findViewById(R.id.abtBroadcast);
        if(holdData != null) holdData.setText(acct.isBroadcast(getApplicationContext()));
        holdData = findViewById(R.id.abtAds);
        if(holdData != null) holdData.setText(getResources().getText(R.string.unknown));
        holdData = findViewById(R.id.abtWhoAmI);
        if(holdData != null) holdData.setText(acct.unique);
        holdData = findViewById(R.id.abtAccount);
        if(holdData != null) holdData.setText(String.format("(%s)", acct.acctIdStr()));

        PingServerThread ping = new PingServerThread(getApplicationContext(), new Messenger(mHandler));
        ping.start();
    }

    @Override
    protected void onStart() {
        /*
         * To get the hyperlinks to work, we have to apply this setting to the
         * textview after it has been created.  That is why it is here.
         * If you need to read the string from the resource and then set the
         * TextView, (e.g. to provide a dynamic link), you need to escape the
         * HTML in the resource (mostly &lt; and &gt;) and then use:
         *  textView.setText(Html.fromHtml(getResources().getText(R.string.SOMETHING).toString()));
         * Could also apply style="@style/ContentText.Link" in the xml, too.
         */
        TextView holdView;
        holdView = findViewById(R.id.abtPrivacyLink);
        if(holdView!=null) {holdView.setMovementMethod(LinkMovementMethod.getInstance());}
        super.onStart();
    }

    private void CheckPingOk(String reply, int ads){

        String holdResult = String.format(getResources().getText(R.string.abt_sysup).toString(), reply);
        TextView holdView = this.findViewById(R.id.abtSystemVer);
        if (holdView != null) { holdView.setText(holdResult); }

        holdView = findViewById(R.id.abtAds);
        holdView.setText(ads==1?getResources().getText(R.string.yes):getResources().getText(R.string.no));
    }

    private void CheckPingError(int status){
        String holdResult;
        switch (status){
            case 99:
                holdResult = getResources().getText(R.string.err_cannot_connect).toString();
                break;
            case 400:
                holdResult = String.format(getResources().getText(R.string.err_bad_request).toString(), status);
                break;
            case 401:
                holdResult = String.format(getResources().getText(R.string.err_bad_auth).toString(), status);
                break;
            case 404:
                holdResult = String.format(getResources().getText(R.string.err_bad_resource).toString(), status);
                break;
            default:
                holdResult = String.format(getResources().getText(R.string.err_bad_server).toString(), status);
                break;
        }

        TextView holdView = this.findViewById(R.id.abtSystemVer);
        if (holdView != null) { holdView.setText(holdResult); }

        holdView = findViewById(R.id.abtAds);
        holdView.setText(getResources().getText(R.string.no));
    }
}
