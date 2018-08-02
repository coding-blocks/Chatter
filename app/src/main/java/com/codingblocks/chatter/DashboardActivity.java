package com.codingblocks.chatter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.codingblocks.chatter.fragments.CommunityFragment;
import com.codingblocks.chatter.fragments.RoomFragment;
import com.codingblocks.chatter.fragments.RoomsFragment;
import com.codingblocks.chatter.fragments.SettingsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity {

    @BindView(R.id.navigationView) BottomNavigationView mBottomNav;

    OkHttpClient client = new OkHttpClient();

    //Database
    RoomsDatabase roomdb;
    MessagesDatabase messagesDatabase;

    /* Some usefull things */
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        ButterKnife.bind(this);

        roomdb = RoomsDatabase.getInstance(this);
        messagesDatabase = MessagesDatabase.getInstance(this);

        sharedPreferences =
                this.getSharedPreferences("UserPreferences", 0);

        /* Useful data from Shared Preferences */
        String accessToken = sharedPreferences.getString("accessToken", "");
        String username = sharedPreferences.getString("username", "");
        String idOfUser = sharedPreferences.getString("idOfUser", "");
        String displayName = sharedPreferences.getString("displayName", "");
        String userUrl = sharedPreferences.getString("userUrl", "");
        String avatarUrl = sharedPreferences.getString("avatarUrl", "");

        /* Get the data from the Gitter API if they are not avaiable if
           internet is also not available redirect to NoNetwirk Activity */
        if (username.equals("") ||
                idOfUser.equals("") ||
                displayName.equals("") ||
                userUrl.equals("") ||
                avatarUrl.equals("")) {
            if (isNetworkAvailable()) {
                Request request = new Request.Builder()
                        .url("https://api.gitter.im/v1/user")
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try {
                            String responseText = response.body().string();
                            JSONArray jsonarray = new JSONArray(responseText);
                            JSONObject Jobject = jsonarray.getJSONObject(0);

                            final String username = Jobject.getString("username");
                            final String idOfUser = Jobject.getString("id");
                            final String displayName = Jobject.getString("displayName");
                            final String userUrl = Jobject.getString("url");
                            final String avatarUrl = Jobject.getString("avatarUrl");
                            DashboardActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sharedPreferences.edit()
                                            .putString("username", username)
                                            .putString("idOfUser", idOfUser)
                                            .putString("displayName", displayName)
                                            .putString("userUrl", userUrl)
                                            .putString("avatarUrl", avatarUrl)
                                            .apply();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                Intent intent = new Intent(
                        DashboardActivity.this, NoNetworkActivity.class
                );
                intent.putExtra("calledFrom", "DashboardActivity");
                DashboardActivity.this.startActivity(intent);
                DashboardActivity.this.finish();
            }
        }

        mBottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment fragment = null;
                switch (item.getItemId()){
                    case R.id.navigation_messages:
                        fragment = new RoomsFragment();
                        break;
                    case R.id.navigation_communities:
                        fragment = new CommunityFragment();
                        break;
                    case R.id.navigation_settings:
                        fragment = new SettingsFragment();
                        break;
                    default:
                        fragment = null;
                }
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_holder, fragment)
                        .commit();
                return true;
            }
        });
        /* Default fragment for messages*/
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_holder, new RoomsFragment());
        transaction.commit();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void openRoom(String id, String roomName) {
        DashboardActivity.this
                .getSharedPreferences("UserPreferences", 0)
                .edit()
                .putString("CurrentRoodId", id)
                .apply();
        Bundle bundle = new Bundle();
        bundle.putString("RoomId", id);
        bundle.putString("RoomName", roomName);
        Fragment roomFragment = new RoomFragment();
        roomFragment.setArguments(bundle);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_holder, roomFragment);
        transaction.commit();
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_options_menu, menu);
        FragmentManager f = this.getSupportFragmentManager();
        if(f.findFragmentById(R.id.fragment_holder) instanceof RoomFragment){
            menu.findItem(R.id.leaveRoom).setVisible(false);
        } else {
            menu.findItem(R.id.leaveRoom).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signOut:
                signOut();
                break;
            case R.id.leaveRoom:
                String roomId = sharedPreferences.getString("CurrentRoomID", "0");
                leaveRoom(roomId);
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {

        AlertDialog dialog = new AlertDialog.Builder(DashboardActivity.this)
                .setMessage("Are you sure you want to Sign Out?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @SuppressLint("StaticFieldLeak")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                messagesDatabase.clearAllTables();
                                roomdb.clearAllTables();
                                return null;
                            }
                            @Override
                            protected void onPostExecute(Void aVoid) {
                                SharedPreferences.Editor editor = DashboardActivity.this
                                        .getSharedPreferences("UserPreferences", 0)
                                        .edit();
                                editor.clear();
                                editor.apply();
                                Intent intent = new Intent(DashboardActivity.this, SplashActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        }.execute();
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

    private void leaveRoom(final String roomId) {
        roomdb = RoomsDatabase.getInstance(DashboardActivity.this);
        messagesDatabase = MessagesDatabase.getInstance(DashboardActivity.this);
        sharedPreferences =
                DashboardActivity.this.getSharedPreferences("UserPreferences", 0);
        String idOfUser = sharedPreferences.getString("idOfUser", null);
        String accessToken = sharedPreferences.getString("accessToken", null);
        final Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/rooms/"+roomId+"/users/" + idOfUser)
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
                            public void onFailure(@NonNull Call call, @NonNull IOException e) { }

                            @SuppressLint("StaticFieldLeak")
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        roomdb.roomsDao().deleteRoom(roomId);
                                        return null;
                                    }
                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        super.onPostExecute(aVoid);
                                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                                        transaction.replace(R.id.fragment_holder, new RoomsFragment());
                                        transaction.commit();
                                        invalidateOptionsMenu();
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
}
