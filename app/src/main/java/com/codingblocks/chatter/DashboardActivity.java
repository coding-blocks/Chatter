package com.codingblocks.chatter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.MenuItem;

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

    @BindView(R.id.nav_view) BottomNavigationView bottomNavigationView;

    OkHttpClient client = new OkHttpClient();

    public int savedMenuItemId;

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
        if(username.equals("") ||
                idOfUser.equals("") ||
                displayName.equals("") ||
                userUrl.equals("") ||
                avatarUrl.equals("")) {
            if(isNetworkAvailable()){
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

        if(savedInstanceState == null){
            savedMenuItemId = bottomNavigationView.getMenu().getItem(0).getItemId();
        } else {
            savedMenuItemId = savedInstanceState.getInt("arg_last_menu_item_id");
        }
        selectFragment(savedMenuItemId);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                selectFragment(menuItem.getItemId());
                return true;
            }
        });
    }

    public void selectFragment(int id){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch(id) {
            case R.id.action_dashboard:
                transaction.replace(R.id.fragment_holder, new DashboardFragment());
                break;
            case R.id.action_rooms:
                transaction.replace(R.id.fragment_holder, new RoomsFragment());
                break;
            case R.id.action_settings:
                transaction.replace(R.id.fragment_holder, new SettingsFragment());
                break;
        }
        // Save the menu id and commit the transaction
        savedMenuItemId = id;
        transaction.commit();
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void openRoom(String id){
        Bundle bundle = new Bundle();
        bundle.putString("RoomId", id);
        RoomFragment roomFragment = new RoomFragment();
        roomFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_holder, roomFragment)
                .commit();
    }
}
