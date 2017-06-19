package com.coolftc.prompt;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        Actor me = new Actor(this);
        TextView holdData;
        // Initialize the about data
        holdData = (TextView) findViewById(R.id.abtVersion);
        if(holdData != null) holdData.setText(R.string.ver_nbr);
        holdData = (TextView) findViewById(R.id.abtBuildDate);
        if(holdData != null) holdData.setText(R.string.ver_build);
        holdData = (TextView) findViewById(R.id.abtBroadcast);
        if(holdData != null) holdData.setText(me.isBroadcast(getApplicationContext()));
        holdData = (TextView) findViewById(R.id.abtAds);
        if(holdData != null) holdData.setText(me.isAds(getApplicationContext()));
        holdData = (TextView) findViewById(R.id.abtWhoAmI);
        if(holdData != null) holdData.setText(me.unique);
        holdData = (TextView) findViewById(R.id.abtAccount);
        if(holdData != null) holdData.setText("(" + me.acctIdStr() + ")");

    }
}
