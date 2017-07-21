package com.coolftc.prompt;

import android.content.Context;

/**
 *  This thread is used to send an invitation to each supplied address.
 *
 */
public class SendInviteThread extends Thread {
    private Context mContext;
    private String [] mAddresses;

    SendInviteThread(Context activity, String [] addresses) {
        mAddresses = addresses;
        mContext = activity;
    }

    @Override
    public void run() {

    }

}
