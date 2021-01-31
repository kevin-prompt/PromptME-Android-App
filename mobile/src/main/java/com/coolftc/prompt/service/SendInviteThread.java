package com.coolftc.prompt.service;

import android.content.Context;
import android.content.Intent;

import com.coolftc.prompt.Actor;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.source.WebServiceModelsOld;
import com.coolftc.prompt.source.WebServicesOld;

import static com.coolftc.prompt.utility.Constants.NETWORK_DOWN;

/**
 *  This thread is used to send an invitation to each supplied address.  While the
    web service will return results, it is better if those are just ignored here
    and the Refresh service is allowed to update the local data.
 */
public class SendInviteThread extends Thread {
    private Context mContext;
    private String [] mAddresses;
    private String mDisplay;
    private boolean mMirror;

    public SendInviteThread(Context activity, String [] addresses, String display, boolean mirror) {
        mAddresses = addresses;
        mContext = activity;
        mMirror = mirror;
        mDisplay = display;
    }

    @Override
    public void run() {
        try {
            Actor sender = new Actor(mContext);

            // Skip any empty addresses
            for (String address : mAddresses) {
                if(address.length() > 0) {
                    WebServiceModelsOld.InviteResponse actual = sendInvite(sender, address, mDisplay, mMirror);
                }
            }

            // Since this will change the data, refresh it.
            Intent sIntent = new Intent(mContext, Refresh.class);
            mContext.startService(sIntent);

        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".run");
        }
    }

    /*
     *  Send a new invite to the server.
     */
    private WebServiceModelsOld.InviteResponse sendInvite(Actor from, String unique, String display, boolean mirror){
        WebServiceModelsOld.InviteResponse rtn;
        WebServicesOld ws = new WebServicesOld();
        if(ws.IsNetwork(mContext)) {
            WebServiceModelsOld.InviteRequest rData = new WebServiceModelsOld.InviteRequest();
            rData.fname = unique;
            rData.fdisplay = display;
            rData.message = "";
            rData.mirror = mirror;

            rtn = ws.NewInvite(from.ticket, from.acctIdStr(), rData);
        } else {
            rtn = new WebServiceModelsOld.InviteResponse();
            rtn.response = NETWORK_DOWN;
        }
        return rtn;
    }


}
