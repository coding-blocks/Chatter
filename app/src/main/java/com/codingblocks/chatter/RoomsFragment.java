package com.codingblocks.chatter;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RoomsFragment extends Fragment {

    public RoomsFragment() {
        // Required empty public constructor
    }

    private OkHttpClient client = new OkHttpClient();

    @BindView(R.id.recycler_view) RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_rooms, container, false);
        ButterKnife.bind(this,view);

        Realm.init(getActivity().getApplicationContext());
        Realm realm = Realm.getDefaultInstance();

        final RealmResults<RoomsTable> rooms = realm
                .where(RoomsTable.class)
                .greaterThan("id", 0)
                .findAll();

        /* Add on change listener for rooms so that we can get live results  */
        rooms.addChangeListener(new RealmChangeListener<RealmResults<RoomsTable>>() {
            @Override
            public void onChange(RealmResults<RoomsTable> rooms) {
                // Update the Recycler View
                displayRooms(rooms);
            }
        });

        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        displayRooms(rooms);

        return view;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void displayRooms(RealmResults<RoomsTable> rooms){
        /* No rooms, let's get them first */
        if(rooms.size() == 0){
            /* Internet is needed for sure to get the rooms */
            getRooms(1);
        }

        RecyclerView.Adapter adapter =
                new RoomsAdapter(rooms, getActivity().getApplicationContext());
        recyclerView.setAdapter(adapter);

        /* Get rooms if network is available
           [we have old ones but checking for updates] */
        getRooms(0);
    }

    public void getRooms(int severity){
        if(isNetworkAvailable()) {
            /* Display a toast to inform the user that we are syncing */
            Toast.makeText(
                    getActivity(), "Syncing data", Toast.LENGTH_SHORT
            ).show();
            String accessToken = getActivity()
                    .getSharedPreferences("UserPreferences", 0)
                    .getString("accessToken", "");
            if (accessToken.equals("")) {
                Intent intent = new Intent(getActivity(), SplashActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }
            Request request = new Request.Builder()
                    .url("https://gitter.im/v1/rooms")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization:", "Bearer " + accessToken)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    /* Simple hack for compatibility as API 19 is required for
                       new JSONArray */
                    final String responseText = "{\"rooms\":"+response.toString()+"}";
                    // We will move to UI Thread
                    if(getActivity()!=null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    JSONObject JObject = new JSONObject(responseText);
                                    JSONArray JArray = JObject.getJSONArray("rooms");
                                    int i;
                                    for (i = 0; i < JArray.length(); i++) {
                                        // Initialize Realm
                                        Realm.init(getActivity().getApplicationContext());
                                        // Get a Realm instance for this thread
                                        Realm realm = Realm.getDefaultInstance();

                                        JSONObject dynamicJObject = JArray.getJSONObject(i);
                                        String githubType = dynamicJObject.getString("githubType");
                                        String uId = dynamicJObject.getString("id");
                                        String name = dynamicJObject.getString("name");
                                        // userCount = 0 == user to user room since ONETWOONE doesnot have it
                                        int userCount = 0;
                                        if (!githubType.equals("ONETWOONE")) {
                                            userCount = dynamicJObject.getInt("userCount");
                                        }
                                        int unreadItems = dynamicJObject.getInt("unreadItems");
                                        int mentions = dynamicJObject.getInt("mentions");

                                        RealmResults<RoomsTable> containedRoom =
                                                realm.where(RoomsTable.class)
                                                        .equalTo("uId", uId)
                                                        .findAllSorted("id", Sort.DESCENDING);

                                        // Get the current max id in the EntityName table
                                        Number maxId = realm.where(MessagesTable.class).max("id");
                                        // If id is null, set it to 1, else set increment it by 1
                                        int nextId = (maxId == null) ? 1 : maxId.intValue() + 1;

                                        if (containedRoom.size() == 1) {
                                            // Save the id so that if when we delete, we can insert it into the empty slot
                                            // since we are sorting by id
                                            nextId = containedRoom.get(0).getId();
                                            // Delete that room, so you can push an update ;)
                                            realm.beginTransaction();
                                            containedRoom.deleteFirstFromRealm();
                                            realm.commitTransaction();
                                        }
                                        RoomsTable room = new RoomsTable();
                                        room.setId(nextId);
                                        room.setuId(uId);
                                        room.setRoomName(name);
                                        room.setUserCount(userCount);
                                        room.setUnreadItems(unreadItems);
                                        room.setMentions(mentions);

                                        // Begin, copy and commit
                                        realm.beginTransaction();
                                        realm.copyToRealm(room);
                                        realm.commitTransaction();
                                    }
                                    if (i == 0) {
                                        Toast.makeText(
                                                getActivity(),
                                                "There seems to be no rooms, please try again later",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            });
        /* Prompt user to turn on internet only if we have no rooms */
        } else if(severity == 1){
            Intent intent = new Intent(getActivity(), NoNetworkActivity.class);
            intent.putExtra("calledFrom", "DashboardActivity");
            getActivity().startActivity(intent);
            getActivity().finish();
        }
    }
}
