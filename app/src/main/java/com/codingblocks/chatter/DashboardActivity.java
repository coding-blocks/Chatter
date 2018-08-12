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
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.codingblocks.chatter.db.RoomsTable;
import com.codingblocks.chatter.fragments.RoomsFragment;
import com.codingblocks.chatter.models.RoomsDao;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    OkHttpClient client = new OkHttpClient();
    SharedPreferences sharedPreferences;
    //Database
    RoomsDatabase roomdb;
    MessagesDatabase messagesDatabase;

    private View navHeader;
    private ImageView imgNavHeaderBg, imgProfile;
    private TextView txtName, txtDisplayName;
    String username, accessToken, idOfUser, displayName, userUrl, avatarUrl;
    NavigationView navigationView;
    Menu navMenu;
    List<RoomsTable> suggested = new ArrayList<>();
    RoomsDao roomdao;
    List<RoomsTable> favRoomsTableList;
    SubMenu favChannelMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.app_name, R.string.app_name);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView = findViewById(R.id.nav_view);
        navMenu = navigationView.getMenu();

        navigationView.setNavigationItemSelectedListener(this);
        // Navigation view header
        navHeader = navigationView.getHeaderView(0);
        txtName = navHeader.findViewById(R.id.name);
        txtDisplayName = navHeader.findViewById(R.id.displayName);
        imgNavHeaderBg = navHeader.findViewById(R.id.img_header_bg);
        imgProfile = navHeader.findViewById(R.id.img_profile);
        //for deleting db on SignOut
        roomdb = RoomsDatabase.getInstance(this);
        roomdao = roomdb.roomsDao();
        messagesDatabase = MessagesDatabase.getInstance(this);

        /* Some usefull things */
        sharedPreferences =
                this.getSharedPreferences("UserPreferences", 0);

        /* Useful data from Shared Preferences */
        accessToken = sharedPreferences.getString("accessToken", "");
        username = sharedPreferences.getString("username", "");
        idOfUser = sharedPreferences.getString("idOfUser", "");
        displayName = sharedPreferences.getString("displayName", "");
        userUrl = sharedPreferences.getString("userUrl", "");
        avatarUrl = sharedPreferences.getString("avatarUrl", "");

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
                                    txtName.setText(username);
                                    txtDisplayName.setText(displayName);
                                    Picasso.get().load(avatarUrl).into(imgProfile);
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
        } else {
            txtName.setText(username);
            txtDisplayName.setText(displayName);
            Picasso.get().load(avatarUrl).into(imgProfile);
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        RoomsFragment roomsFragment = new RoomsFragment();
        transaction.replace(R.id.fragment_holder, RoomsFragment.newInstance("All"), "Room");
        transaction.commit();
        getSuggestedRooms();
        updateNavFavourites();
    }

    @SuppressLint("StaticFieldLeak")
    private void updateNavFavourites() {
        new AsyncTask<Void, Void, List<RoomsTable>>() {

            @Override
            protected List<RoomsTable> doInBackground(Void... voids) {
                return roomdao.getAllRooms();
            }

            @Override
            protected void onPostExecute(List<RoomsTable> roomsTableList) {
                super.onPostExecute(roomsTableList);
                favRoomsTableList = roomsTableList;
                if(favChannelMenu == null){
                    favChannelMenu = navMenu.addSubMenu("My Favourites");
                }
                favChannelMenu.clear();
                for(int i=0; i < favRoomsTableList.size(); ++i) {
                    RoomsTable roomsTable = favRoomsTableList.get(i);
                    if (roomsTable.getFavourite() != null) {
                        favChannelMenu.add(Menu.NONE, 100 + i, Menu.NONE, favRoomsTableList.get(i).getRoomName());
                    }
                }
            }
        }.execute();
    }

    private void getSuggestedRooms() {
        if (isNetworkAvailable()) {
            Request request = new Request.Builder()
                    .url("https://api.gitter.im/v1/"
                            + "user/"
                            + idOfUser +
                            "/suggestedRooms"
                    )
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String responseText = "{\"rooms\":" + response.body().string() + "}";

                    try {
                        JSONObject JObject = new JSONObject(responseText);
                        JSONArray JArray = JObject.getJSONArray("rooms");
                        int i;
                        for (i = 0; i < JArray.length(); i++) {
                            JSONObject dynamicJObject = JArray.getJSONObject(i);
                            String githubType = dynamicJObject.getString("githubType");
                            final String uId = dynamicJObject.getString("id");
                            String name = dynamicJObject.getString("name");
                            int userCount = 0;
                            if (!githubType.equals("ONETWOONE")) {
                                userCount = dynamicJObject.getInt("userCount");
                            }

                            String favourite = null;
                            if (!dynamicJObject.isNull("favourite"))
                                favourite = (dynamicJObject.getString("favourite"));
                            final RoomsTable room = new RoomsTable();
                            room.setuId(uId);
                            room.setRoomName(name);
                            room.setUserCount(userCount);
                            room.setFavourite(favourite);
                            suggested.add(room);

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                SubMenu topChannelMenu = navMenu.addSubMenu("Suggested Rooms");
                                for (int i = 1; i <= 5; i++) {
                                    try {
                                        topChannelMenu.add(Menu.NONE, i, Menu.NONE, suggested.get(i).getRoomName());
                                    } catch (Exception e) {

                                    }
                                }


                            }
                        });

                    }


                }
            });
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void openRoom(String id, String roomName, int userCount, String favourtie, boolean roomMember) {
        Bundle bundle = new Bundle();
        bundle.putString("RoomId", id);
        bundle.putString("RoomName", roomName);
        bundle.putInt("userCount", userCount);
        bundle.putBoolean("roomMember", roomMember);
        bundle.putString("favourite", favourtie);
        Intent roomIntent = new Intent(DashboardActivity.this, RoomActivity.class);
        roomIntent.putExtras(bundle);
        startActivity(roomIntent);

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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_all_conv) {
            changeFragment("all");
        } else if (id == R.id.nav_people) {
            changeFragment("oneToone");
        } else if (id == R.id.nav_signOut)
            signOut();
        else if (id == R.id.nav_prefences)
            startActivity(new Intent(DashboardActivity.this, SettingsActivity.class));
        else if(id == 1 ||id == 2 ||id == 3 ||id == 4 ||id == 5){
            openRoom(suggested.get(id).getuId(),
                    suggested.get(id).getRoomName(),
                    suggested.get(id).getUserCount(),
                    suggested.get(id).getFavourite(),
                    false
            );
        } else if (id >=100) {
            int roomId = id - 100;
            openRoom(favRoomsTableList.get(roomId).getuId(),
                    favRoomsTableList.get(roomId).getRoomName(),
                    favRoomsTableList.get(roomId).getUserCount(),
                    favRoomsTableList.get(roomId).getFavourite(),
                    false
            );
        }

        return false;
    }

    @Override
    protected void onResume() {
        updateNavFavourites();
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    void changeFragment(String filter) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_holder, RoomsFragment.newInstance(filter), "Room");
        transaction.commit();
        onBackPressed();
    }

}