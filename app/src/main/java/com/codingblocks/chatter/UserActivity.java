package com.codingblocks.chatter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserActivity extends AppCompatActivity {
    private OkHttpClient client = new OkHttpClient();
    private String userId;
    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        userId = (String) bundle.get("userId");
        userName = (String) bundle.get("userName");
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setTitle(userName);
        String accessToken = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/"
                        + "users/" +
                        userName)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("TAG", "UseronResponse: " + response.body().string());

            }
        });
    }
}
