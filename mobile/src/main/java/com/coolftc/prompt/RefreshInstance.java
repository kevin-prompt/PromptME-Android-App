package com.coolftc.prompt;

import android.content.Intent;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class RefreshInstance extends FirebaseInstanceIdService {
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is also called
     * when the InstanceID token is initially generated, so this is where
     * you retrieve the token.
     */
    @Override
    public void onTokenRefresh() {

        // Clear out the locally stored token and kick off the service to update it.
        Account acct = new Account(this, true);
        acct.device = "";
        acct.SyncPrime(false, this);
        Intent intent = new Intent(this, Refresh.class);
        startService(intent);
    }
}

