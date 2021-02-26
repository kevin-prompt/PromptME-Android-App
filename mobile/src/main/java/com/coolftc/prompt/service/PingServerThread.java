package com.coolftc.prompt.service;

import android.content.Context;
import android.os.Message;
import android.os.Messenger;

import com.coolftc.prompt.Actor;
import com.coolftc.prompt.source.PingResponse;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import static com.coolftc.prompt.utility.Constants.*;
import static com.coolftc.prompt.utility.ExpClass.STATUS_CODE_NETWORK_DOWN;

/**
 *  This thread is used to:
    a. Check if the server is reachable.
    b. See if the app should display ads.
 */
public class PingServerThread extends Thread {
    private final Context mContext;
    private final Messenger mMessage;

    public PingServerThread(Context application, Messenger message) {
        mContext = application;
        mMessage = message;
    }

    @Override
    public void run() {
        try {
            Actor user = new Actor(mContext);
            try (Connection net = new Connection(mContext)) {
                if (net.isOnline()) {
                    WebServices ws = new WebServices(new Gson());
                    // find the server and get proof
                    String realPath = ws.baseUrl(mContext) + FTI_Ping;
                    PingResponse response = ws.callGetApi(realPath, PingResponse.class, user.ticket);
                    // checking for ads
                    user.LoadPrime(true, mContext);
                    returnOK(response.getVersion(), user.ads ? 1 : 0);
                } else {
                    returnErr(STATUS_CODE_NETWORK_DOWN);
                }
            }
        } catch (ExpClass kx) {
            ExpClass.Companion.logEXP(kx, this.getClass().getName() + ".pingServer");
            returnErr(kx.getStatus());
        }
    }

    private void returnOK(String version, int ads){
        try {
            Message msg = Message.obtain();
            msg.what = WHAT_OK_PING;
            msg.arg1 = ads;
            msg.obj = version;
            mMessage.send(msg);
        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, "pingServer");
        }
    }

    private void returnErr(int status){
        try {
            Message msg = Message.obtain();
            msg.what = WHAT_ERR_PING;
            msg.arg1 = status;
            mMessage.send(msg);
        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, "pingServer");
        }
    }

}
