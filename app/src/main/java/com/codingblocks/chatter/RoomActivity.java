package com.codingblocks.chatter;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.codingblocks.chatter.fragments.RoomFragment;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RoomActivity extends AppCompatActivity {

    String roomId;
    private OkHttpClient client = new OkHttpClient();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        roomId = (String) bundle.get("RoomId");
        Log.i("TAG", "onCreate: " + i.getExtras() + i.getBundleExtra("RoomId") + bundle.get("RoomId"));
        RoomFragment roomFragment = new RoomFragment();
        roomFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.room_fragment_holder, roomFragment)
                .commit();
        leaveRoom(roomId);
    }

    private void leaveRoom(String roomId) {
        String accessToken = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        String uid = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("id", "");
        Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/rooms/"
                        + roomId
                        + "/users"
                        + uid)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization:", "Bearer " + accessToken)
                .delete()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

            }
        });

    }

}
