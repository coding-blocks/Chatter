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

public class RoomFragment extends Fragment {


    public RoomFragment() {
        // Required empty public constructor
    }

    private OkHttpClient client = new OkHttpClient();

    @BindView(R.id.recycler_view) RecyclerView recyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room, container, false);
        ButterKnife.bind(view);
        Bundle bundle = this.getArguments();

        final String roomId = bundle.getString("RoomId", "");
        if(roomId.equals("")) {
            Toast.makeText(
                    this.getContext(),
                    "Error no room id has been passed",
                    Toast.LENGTH_SHORT
            ).show();
        }

        Realm.init(getActivity().getApplicationContext());
        Realm realm = Realm.getDefaultInstance();

        final RealmResults<MessagesTable> messages = realm
                .where(MessagesTable.class)
                .greaterThan("id", 0)
                .findAllSorted("id", Sort.DESCENDING);

        /* Add on change listener for messages so that we can get live results  */
        messages.addChangeListener(new RealmChangeListener<RealmResults<MessagesTable>>() {
            @Override
            public void onChange(RealmResults<MessagesTable> rooms) {
                // Update the Recycler View
                displayMessages(messages, roomId);
            }
        });

        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        displayMessages(messages, roomId);

        return view;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void displayMessages(RealmResults<MessagesTable> messages, String roomId){
        /* No messages, let's get them first */
        if(messages.size() == 0){
            getMessages(1, roomId);
        }
        RecyclerView.Adapter adapter =
                new MessagesAdapter(messages, getActivity().getApplicationContext());
        recyclerView.setAdapter(adapter);
        /* Get messages if network is available
           [we have old ones but checking for updates] */
        getMessages(0, roomId);
    }

    public void getMessages(int severity, String roomId){
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
                    .url("https://gitter.im/v1/rooms/:" + roomId + "/chatMessages")
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
                    final String responseText = "{\"messages\":" + response.toString() + "}";
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject JObject = new JSONObject(responseText);
                                JSONArray JArray = JObject.getJSONArray("messages");
                                int i;
                                for (i = 0; i < JArray.length(); i++) {
                                    // Initialize Realm
                                    Realm.init(getActivity().getApplicationContext());
                                    // Get a Realm instance for this thread
                                    Realm realm = Realm.getDefaultInstance();

                                    JSONObject dynamicJObject = JArray.getJSONObject(i);
                                    String uId = dynamicJObject.getString("id");
                                    String text = dynamicJObject.getString("text");
                                    String timestamp = dynamicJObject.getString("sent");
                                    boolean unread = dynamicJObject.getBoolean("unread");
                                    JSONObject userObject = dynamicJObject.getJSONObject("fromUser");
                                    String displayName = userObject.getString("displayName");
                                    String username = userObject.getString("username");

                                    // If message exists already
                                    final RealmResults<MessagesTable> containedMessage =
                                            realm.where(MessagesTable.class)
                                                    .equalTo("uId", uId)
                                            .findAllSorted("id", Sort.DESCENDING);

                                    // Get the current max id in the EntityName table
                                    Number maxId = realm.where(MessagesTable.class).max("id");
                                    // If id is null, set it to 1, else set increment it by 1
                                    int nextId = (maxId == null) ? 1 : maxId.intValue() + 1;

                                    if(containedMessage.size() == 1) {
                                        // Save the id so that if when we delete, we can insert it into the empty slot
                                        // since we are sorting by id
                                        nextId = containedMessage.get(0).getId();
                                        // Delete that message, so you can push an update ;)
                                        realm.beginTransaction();
                                        containedMessage.deleteFirstFromRealm();
                                        realm.commitTransaction();
                                    }

                                    MessagesTable message = new MessagesTable();
                                    message.setId(nextId);
                                    message.setUId(uId);
                                    message.setText(text);
                                    message.setTimestamp(timestamp);
                                    message.setUnread(unread);
                                    message.setDisplayName(displayName);
                                    message.setUsername(username);

                                    // Begin, copy and commit
                                    realm.beginTransaction();
                                    realm.copyToRealm(message);
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
            });
        } else if(severity == 1){
            Intent intent = new Intent(getActivity(), NoNetworkActivity.class);
            intent.putExtra("calledFrom", "DashboardActivity");
            getActivity().startActivity(intent);
            getActivity().finish();
        }
    }
}
