package com.coolftc.prompt.service;

import android.content.Context;
import android.content.Intent;

import com.coolftc.prompt.Actor;
import com.coolftc.prompt.source.InviteRequest;
import com.coolftc.prompt.source.InviteResponse;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import static com.coolftc.prompt.utility.Constants.FTI_Invite;
import static com.coolftc.prompt.utility.Constants.SUB_ZZZ;

/**
 *  This thread is used to send an invitation to each supplied address.  While the
    web service will return results, it is better if those are just ignored here
    and the Refresh service is allowed to update the local data.
 */
public class SendInviteThread extends Thread {
    private final Context mContext;
    private final String [] mAddresses;
    private final String mDisplay;
    private final boolean mMirror;

    public SendInviteThread(Context activity, String [] addresses, String display, boolean mirror) {
        mAddresses = addresses;
        mContext = activity;
        mMirror = mirror;
        mDisplay = display;
    }

    @Override
    public void run() {
        try {
            // Skip any empty addresses
            for (String address : mAddresses) {
                if(address.length() > 0) {
                    sendInvite(address, mDisplay, mMirror);
                }
            }

            // Since this will change the data, refresh it.
            Intent sIntent = new Intent(mContext, Refresh.class);
            mContext.startService(sIntent);

        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".run");
        }
    }

    /*
     *  Send a new invite to the server.
     */
    private void sendInvite(String unique, String display, boolean mirror){
        try (Connection net = new Connection(mContext)) {
            if (net.isOnline()) {
                Actor from = new Actor(mContext);
                WebServices ws = new WebServices(new Gson());
                InviteRequest invite = new InviteRequest(unique, display, "", mirror);
                String realPath = ws.baseUrl(mContext) + FTI_Invite.replace(SUB_ZZZ, from.acctIdStr());
                ws.callPostApi(realPath, invite, InviteResponse.class, from.ticket);
            }
        } catch (ExpClass kx) {
            ExpClass.Companion.logEXP(kx, this.getClass().getName() + ".run");
        }
    }
}