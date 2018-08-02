package com.codingblocks.chatter;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.codingblocks.chatter.fragments.RoomFragment;
import com.codingblocks.chatter.models.MessagesDao;
import com.codingblocks.chatter.models.RoomsDao;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RoomActivity extends AppCompatActivity {

    String roomId;
    int usercount;
    private OkHttpClient client = new OkHttpClient();
    //Database
    RoomsDatabase roomdb;
    RoomsDao roomsDao;

    MessagesDatabase messagesDatabase;
    MessagesDao messagesDao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        roomId = (String) bundle.get("RoomId");
        usercount = (int) bundle.get("userCount");
        Log.i("TAG", "onCreate: " + i.getExtras() + i.getBundleExtra("RoomId") + bundle.get("RoomId") + "userCount" + bundle.get("userCount"));
        RoomFragment roomFragment = new RoomFragment();
        roomFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.room_fragment_holder, roomFragment)
                .commit();
        roomdb = RoomsDatabase.getInstance(this);
        roomsDao = roomdb.roomsDao();
        messagesDatabase = MessagesDatabase.getInstance(this);
        messagesDao = messagesDatabase.messagesDao();
    }

    private void leaveRoom(final String roomId) {
        final String accessToken = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        String uid = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("idOfUser", "");
        Log.i("TAG", "onResponse: " + uid + accessToken);

        final Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/rooms/"
                        + roomId
                        + "/users/"
                        + uid)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .delete()
                .build();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to leave this Room?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @SuppressLint("StaticFieldLeak")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            }

                            @SuppressLint("StaticFieldLeak")
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        messagesDao.deleteRoomMessages(roomId);
                                        roomsDao.deleteRoom(roomId);
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        super.onPostExecute(aVoid);
                                        onBackPressed();
                                    }
                                }.execute();

                            }
                        });
                    }

                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_room_options_menu, menu);
        MenuItem item = menu.findItem(R.id.leaveRoom);
        if (usercount == 2) {
            item.setVisible(false);
        } else
            item.setVisible(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.leaveRoom:
                leaveRoom(roomId);
                break;

        }
        return super.onOptionsItemSelected(item);
    }

}
