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
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.codingblocks.chatter.fragments.RoomsFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity {


    OkHttpClient client = new OkHttpClient();
    SharedPreferences sharedPreferences;
    //Database
    RoomsDatabase roomdb;


    MessagesDatabase messagesDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        ButterKnife.bind(this);

        //for deleting db on SignOut
        roomdb = RoomsDatabase.getInstance(this);
        messagesDatabase = MessagesDatabase.getInstance(this);

        /* Some usefull things */
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
                            Log.i("TAG", "onResponseofUser: " + responseText);
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

    public void openRoom(String id, String roomName, int userCount) {
        Bundle bundle = new Bundle();
        bundle.putString("RoomId", id);
        bundle.putString("RoomName", roomName);
        bundle.putInt("userCount", userCount);
        Intent roomIntent = new Intent(DashboardActivity.this, RoomActivity.class);
        roomIntent.putExtras(bundle);
        startActivity(roomIntent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.preferences:
                startActivity(new Intent(DashboardActivity.this, SettingsActivity.class));
                break;
            case R.id.signOut:
                signOut();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        AlertDialog dialog = new AlertDialog.Builder(this)
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
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.clear();
                                editor.commit();
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
}