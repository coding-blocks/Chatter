package com.codingblocks.chatter;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;

import com.codingblocks.chatter.adapters.UsersAdapter;
import com.codingblocks.chatter.db.Users;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RoomUsersActivity extends AppCompatActivity {

    private OkHttpClient client = new OkHttpClient();
    private String roomId;
    private boolean addUser = false;
    String accessToken;
    List<Users> mUsers = new ArrayList<>();
    UsersAdapter adapter;
    @BindView(R.id.usersRecyclerView)
    RecyclerView mUserRecyclerView;
    LinearLayoutManager layoutManager;
    int skip = 1;
    Request request;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_users);
        ButterKnife.bind(this);
        Intent i = getIntent();
        Bundle bundle = i.getExtras();
        roomId = (String) bundle.get("roomId");
        addUser = bundle.getBoolean("addUser");
        accessToken = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        if (!addUser)
            adapter = new UsersAdapter(mUsers, this, 2);
        else
            adapter = new UsersAdapter(mUsers, this, 3, roomId);
        mUserRecyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        mUserRecyclerView.setLayoutManager(layoutManager);
        mUserRecyclerView.setAdapter(adapter);
        mUserRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {


            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
                int totalItemCount = layoutManager.getItemCount();
                if (firstVisibleItemPosition + 1 == totalItemCount) {
                    if (!addUser)
                        getUsers(30 * skip++);

                }
            }
        });
        if (!addUser)
            getUsers(0);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void getUsers(int num) {
        String uid = this
                .getSharedPreferences("UserPreferences", 0)
                .getString("idOfUser", "");
        Log.i("TAG", "onResponse: " + uid + roomId);
        request = new Request.Builder()
                .url("https://api.gitter.im/v1/rooms/"
                        + roomId
                        + "/users?skip=" + num)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                inflatedata(response);
            }
        });

    }

    private void inflatedata(Response response) throws IOException {
                            /* Simple hack for compatibility as API 19 is required for
                       new JSONArray */
        final String responseText = response.body().string();
        // We will move to UI Thread
        Thread thread = new Thread(new Runnable() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void run() {
                try {
                    JSONArray JArray = null;
                    if (!addUser) {

                        JArray = new JSONArray(responseText);
                    } else {
                        JSONObject JObject = new JSONObject(responseText);
                        JArray = JObject.getJSONArray("results");
                    }
                    int i;
                    for (i = 0; i < JArray.length(); i++) {
                        JSONObject dynamicJObject = JArray.getJSONObject(i);
                        Log.i("TAG", "roomuser: " + dynamicJObject.toString());
                        String id = dynamicJObject.getString("id");
                        String name = dynamicJObject.getString("username");
                        String displayName = dynamicJObject.getString("displayName");
                        String url = dynamicJObject.getString("avatarUrl");
                        String avatarUrlSmall = dynamicJObject.getString("avatarUrlSmall");
//                        String role = dynamicJObject.getString("role");
                        Users user = new Users();
                        user.setId(id);
//                        user.setRole(role);
                        user.setUrl(url);
                        user.setAvatarUrlSmall(avatarUrlSmall);
                        user.setDisplayName(displayName);
                        user.setUsername(name);
                        mUsers.add(user);
                    }
                } catch (
                        JSONException e)

                {
                    e.printStackTrace();
                } finally

                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        thread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_options_menu, menu);
        MenuItem item = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mUsers.clear();
                adapter.notifyDataSetChanged();
                if (!addUser)
                    getUsers(0);
                skip = 1;
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mUsers.clear();

                searchRooms(newText);
                return true;
            }
        });
        return true;
    }

    private void searchRooms(String newText) {
        /* Display a toast to inform the user that we are syncing */

        if (!addUser) {
            request = new Request.Builder()
                    .url("https://api.gitter.im/v1/rooms/" +
                            roomId + "/users?q=" + newText)
                    .build();


        } else {
            request = new Request.Builder()
                    .url("https://api.gitter.im/v1/user?q=" +
                            newText
                            + "&limit=" + 25 + "&type=gitter")
                    .build();
        }
        HttpUrl url = request.url().newBuilder()
                .addQueryParameter("access_token", accessToken)
                .build();
        request = request.newBuilder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                inflatedata(response);
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

}
