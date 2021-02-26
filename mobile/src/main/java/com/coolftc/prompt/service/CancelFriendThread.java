package com.coolftc.prompt.service;

import android.content.Context;
import android.content.Intent;

import com.coolftc.prompt.Account;
import com.coolftc.prompt.Actor;
import com.coolftc.prompt.utility.Connection;
import com.coolftc.prompt.utility.ExpClass;
import com.coolftc.prompt.utility.WebServices;
import com.google.gson.Gson;

import static com.coolftc.prompt.utility.Constants.FTI_Invite_Del;
import static com.coolftc.prompt.utility.Constants.SUB_ZZZ;

/**
 *  This thread is used to:
    a. Remove a friend relationship.
    b. Reject an invitation to connect.
 */
public class CancelFriendThread extends Thread {
    private final Context mContext;

    Account mFriend;

    public CancelFriendThread(Context application, Account friend) {
        mFriend = friend;
        mContext = application;
    }

    /*
     *  This tries to delete the relationship off the server and then
     *  requests the Refresh check for new data.
     */
    @Override
    public void run() {

        try (Connection net = new Connection(mContext)) {

            Actor sender = new Actor(mContext);
            WebServices ws = new WebServices(new Gson());

            if (net.isOnline()) {
                // There is not currently any recourse if this fails, other than the user
                // can just retry after seeing the connection still exists.
                String realPath = ws.baseUrl(mContext) + FTI_Invite_Del.replace(SUB_ZZZ, sender.acctIdStr()) + mFriend.acctIdStr();
                ws.callDeleteApi(realPath, sender.ticket);
            }

            // Trigger the Refresh to update the Pending count.
            Intent intent = new Intent(mContext, Refresh.class);
            mContext.startService(intent);

        } catch (Exception ex) {
            ExpClass.Companion.logEX(ex, this.getClass().getName() + ".run");
        }
    }
}
