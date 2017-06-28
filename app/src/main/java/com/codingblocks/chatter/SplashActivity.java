package com.codingblocks.chatter;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;


public class SplashActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        SharedPreferences sharedPreferences =
                this.getApplicationContext().getSharedPreferences("UserPreferences", 0);
        String accessToken = sharedPreferences.getString("accessToken", "");

        Intent intent;
        // Check if the token exists and redirect the user to authentication activity if
        // he there is internet connection (else he would be send to NoNetworkActivity)
        // or redirect him to the dashboard activity accordingly.
        if (accessToken.equals("")) {
            if(isNetworkAvailable()){
                intent = new Intent(this, AuthenticationActivity.class);
            } else {
                intent = new Intent(this, NoNetworkActivity.class);
                intent.putExtra("calledFrom", "SplashActivity");
            }
        } else {
            intent = new Intent(this, DashboardActivity.class);
        }
        this.startActivity(intent);
        finish();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
