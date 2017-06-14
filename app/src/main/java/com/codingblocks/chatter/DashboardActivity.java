package com.codingblocks.chatter;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DashboardActivity extends AppCompatActivity {
    @BindView(R.id.textView) TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        ButterKnife.bind(this);
        SharedPreferences sharedPreferences =
                this.getApplicationContext().getSharedPreferences("UserPreferences", 0);
        // Mock text view
        textView.setText(sharedPreferences.getString("accessToken", "Access Token not found"));
    }
}
