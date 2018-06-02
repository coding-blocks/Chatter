package com.codingblocks.chatter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        ButterKnife.bind(this);

        /* Some usefull things */
        final SharedPreferences sharedPreferences =
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
                        .url("https://gitter.im/v1/user/me")
                        .addHeader("Accept", "application/json")
                        .addHeader("Authorization:", "Bearer " + accessToken)
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
                            JSONObject Jobject = new JSONObject(responseText);
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

    public void openRoom(String id) {
        Bundle bundle = new Bundle();
        bundle.putString("RoomId", id);
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
                startActivity(new Intent(DashboardActivity.this,SettingsActivity.class));
                return true;

        }
        return super.onOptionsItemSelected(item);
    }
}
