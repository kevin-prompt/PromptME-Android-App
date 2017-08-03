package com.coolftc.prompt.service;

import android.content.Context;
import android.content.Intent;

import com.coolftc.prompt.Account;
import com.coolftc.prompt.Actor;
import com.coolftc.prompt.source.WebServices;
import com.coolftc.prompt.utility.ExpClass;

/**
 *  This thread is used to:
    a. Remove a friend relationship.
    b. Reject an invitation to connect.
 */

public class CancelFriendThread extends Thread {
   private Context mContext;

    Account mData;

    public CancelFriendThread(Context activity, Account friend) {
        mData = friend;
        mContext = activity;
    }

    /*
     *  This tries to delete the relationship off the server and then
     *  requests the Refresh check for new data.
     */
    @Override
    public void run() {
        Actor sender = new Actor(mContext);
        WebServices ws = new WebServices();

        try {
            if (ws.IsNetwork(mContext)) {
                // There is not currently any recourse if this fails, other than the user
                // can just retry after seeing the connection still exists.
                ws.DelInvite(sender.ticket, sender.acctIdStr(), mData.acctIdStr());
            }

            // Trigger the Refresh to update the Pending count.
            Intent intent = new Intent(mContext, Refresh.class);
            mContext.startService(intent);

        } catch (Exception ex) {
            ExpClass.LogEX(ex, this.getClass().getName() + ".run");
        }
    }
}
