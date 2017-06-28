package com.codingblocks.chatter;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import butterknife.BindView;

public class NoNetworkActivity extends AppCompatActivity {

    @BindView(R.id.retry_button) Button retryButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Exclude the title bar for this activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.no_network_activity);

        final String calledFrom = getIntent().getStringExtra("calledFrom");

        retryButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If network is available redirect user to splash activity
                if(isNetworkAvailable()){
                    // Default
                    Intent intent = new Intent(NoNetworkActivity.this, SplashActivity.class);
                    if(calledFrom.equals("DashboardActivity")){
                        intent = new Intent(NoNetworkActivity.this, DashboardActivity.class);
                    }
                    NoNetworkActivity.this.startActivity(intent);
                    NoNetworkActivity.this.finish();
                }
            }
        });
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
