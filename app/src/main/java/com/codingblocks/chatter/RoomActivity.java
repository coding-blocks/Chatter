package com.codingblocks.chatter;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.codingblocks.chatter.db.RoomsTable;
import com.codingblocks.chatter.fragments.BottomSheetGroupFragment;
import com.codingblocks.chatter.fragments.RoomFragment;
import com.codingblocks.chatter.models.MessagesDao;
import com.codingblocks.chatter.models.RoomsDao;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomActivity extends AppCompatActivity {

    String roomId;
    int usercount;
    private OkHttpClient client = new OkHttpClient();
    //Database
    RoomsDatabase roomdb;
    RoomsDao roomsDao;
    private Menu menu;


    MessagesDatabase messagesDatabase;
    MessagesDao messagesDao;
    String accessToken;
    String uid;
    String status = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        accessToken = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        uid = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("idOfUser", "");
        Log.i("TAG", "onResponse: " + uid + accessToken);
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        roomId = (String) bundle.get("RoomId");
        usercount = (int) bundle.get("userCount");
        status = bundle.getString("favourite");
        Log.i("TAG", "onCreate: " + status);
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
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void leaveRoom(final String roomId) {


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
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_room_options_menu, menu);
        MenuItem item1 = menu.findItem(R.id.leaveRoom);
        MenuItem item2 = menu.findItem(R.id.aboutRoom);
        MenuItem menuItem = menu.findItem(R.id.favourite);
        if (usercount == 2) {
            item1.setVisible(false);
            item2.setVisible(false);
        } else {
            item1.setVisible(true);
            item2.setVisible(true);
        }
        if (status != null) {
            menuItem.setTitle("Remove from Favourites");
        } else {
            menuItem.setTitle("Add to Favourites");
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.leaveRoom:
                leaveRoom(roomId);
                break;
            case R.id.aboutRoom:
                roominfo(roomId);
                break;
            case R.id.favourite:
                addtofav(roomId);
                break;
            case R.id.markRead:
                markRed(roomId);
                break;
            case android.R.id.home:
                onBackPressed();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void markRed(String roomId) {
        final Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/"
                        + "user/"
                        + uid +
                        "/rooms/"
                        + roomId +
                        "/unreadItems/all"
                )
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .delete()
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });

    }

    private void roominfo(String roomId) {
        BottomSheetGroupFragment bottomSheetFragment = new BottomSheetGroupFragment();
        BottomSheetGroupFragment.newInstance(roomId).show(this.getSupportFragmentManager(), bottomSheetFragment.getTag());
    }

    private void addtofav(final String roomId) {
        RequestBody requestBody;
        Request request;
        if (status == null) {
            requestBody = new FormBody.Builder()
                    .add("favourite", roomId)
                    .build();
            request = new Request.Builder()
                    .url("https://api.gitter.im/v1/"
                            + "user/"
                            + uid +
                            "/rooms/"
                            + roomId
                    )
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .put(requestBody)
                    .build();
        } else {
            requestBody = new FormBody.Builder()
                    .add("favourite", roomId)
                    .build();
            request = new Request.Builder()
                    .url("https://api.gitter.im/v1/"
                            + "user/"
                            + uid +
                            "/rooms/"
                            + roomId
                    )
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .delete(requestBody)
                    .build();
        }

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("TAG", "onResponse: " + response.body().string());
                if (response.isSuccessful()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MenuItem menuItem = menu.findItem(R.id.favourite);
                            if (status != null) {
                                menuItem.setTitle("Add to Favourites");
                                status = null;
                            } else {
                                menuItem.setTitle("Remove from Favourites");
                                status = roomId;
                            }
                        }
                    });

                    RoomsTable roomsTable = roomsDao.getRoomWithuId(roomId);
                    roomsDao.deleteRoom(roomId);
                    if(status != null) {
                        roomsTable.setFavourite(null);
                    } else {
                        roomsTable.setFavourite("1");
                    }
                    roomsDao.addRooms(roomsTable);
                }
            }
        });

    }

}


