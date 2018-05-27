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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomFragment extends Fragment {

    public RoomFragment() {
        // Required empty public constructor
    }
    private MessagesAdapter adapter;

    private OkHttpClient client = new OkHttpClient();

    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.inputMessage) EditText inputMessage;
    @BindView(R.id.sendButton) ImageButton sendButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room, container, false);
        ButterKnife.bind(this,view);
        Bundle bundle = this.getArguments();

        final String roomId = bundle.getString("RoomId", " ");
        if(roomId.equals(" ")) {
            Toast.makeText(
                    this.getContext(),
                    "Error no room id has been passed",
                    Toast.LENGTH_SHORT
            ).show();
        }

        Realm.init(getActivity().getApplicationContext());
        final Realm realm = Realm.getDefaultInstance();

        final RealmResults<MessagesTable> messages =
                realm.where(MessagesTable.class)
                .greaterThan("id", 0)
                .findAllSorted("id", Sort.ASCENDING);

        final RealmResults<RoomsTable> currentRoom =
                realm.where(RoomsTable.class)
                .equalTo("uId", roomId)
                .findAll();
        /* Add on change listener for messages so that we can get live results  */
        messages.addChangeListener(new RealmChangeListener<RealmResults<MessagesTable>>() {
            @Override
            public void onChange(RealmResults<MessagesTable> rooms) {
                // Update the Recycler View
                //displayMessages(messages, roomId);
                adapter.notifyDataSetChanged();
            }
        });

        RecyclerView.LayoutManager layoutManager =
                new LinearLayoutManager(getActivity().getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        displayMessages(messages, roomId);
        recyclerView.scrollToPosition(messages.size()-1);

        if(currentRoom.get(0).getDraftMessage() !=null){
            inputMessage.setText(currentRoom.get(0).getDraftMessage());
        }

        /* Resetting the placeholder text and setting it back if its empty */
        inputMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(inputMessage.getText().toString().equals("Type in here to sent")){
                    inputMessage.setHint(R.string.type_in_placeholder);
                }
            }
        });
        inputMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(inputMessage.getText().toString().trim().equals("")){
                    inputMessage.setHint(R.string.type_in_placeholder);
                } else {
                    realm.executeTransaction(new Realm.Transaction() {
                        @Override
                        public void execute(Realm realm) {
                            currentRoom.get(0).setDraftMessage(inputMessage.getText().toString());
                        }
                    });
                }
            }
        });

        /* Sent message button */
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!inputMessage.getText().toString().equals("Type in here to sent") &&
                        !inputMessage.getText().toString().trim().equals("")){
                    sendMessage(currentRoom);
                }
            }
        });
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
       adapter = new MessagesAdapter(messages, getActivity().getApplicationContext());
        recyclerView.setAdapter(adapter);
        /* Get messages if network is available
           [we have old ones but checking for updates] */
        getMessages(0, roomId);
    }

    public void getMessages(int severity, final String roomId){
        if(isNetworkAvailable()) {
        /* Display a toast to inform the user that we are syncing */
            Toast.makeText(
                    getActivity(), "Syncing data", Toast.LENGTH_SHORT
            ).show();

            // Initialize Realm
            Realm.init(getActivity().getApplicationContext());
            // Get a Realm instance to delete the previous message
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransactionAsync(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.deleteAll();
                }
            }, new Realm.Transaction.OnSuccess() {
                @Override
                public void onSuccess() {
                    Log.d("Rishabh", "data deleted");
                }
            }, new Realm.Transaction.OnError() {
                @Override
                public void onError(Throwable error) {
                    Log.d("Rishabh","Data error occur");
                }
            });

            String accessToken = getActivity()
                    .getSharedPreferences("UserPreferences", 0)
                    .getString("accessToken", "");
            if (accessToken.equals("")) {
                Intent intent = new Intent(getActivity(), SplashActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }

            Request request = new Request.Builder()
                    .url("https://api.gitter.im/v1/rooms/" + roomId + "/chatMessages?" +"access_token=" + accessToken)
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
                    final String responseText = "{\"messages\":" + response.body().string() + "}";
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
                                    String avatarUrl=userObject.getString("avatarUrlMedium");

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
                                    message.setSentStatus(true);
                                    message.setDisplayName(displayName);
                                    message.setRoomId(roomId);
                                    message.setUsername(username);
                                    message.setUserAvater(avatarUrl);
                                    // Begin, copy and commit
                                    realm.beginTransaction();
                                    realm.copyToRealm(message);
                                    realm.commitTransaction();

                                    final RealmResults<MessagesTable> messages =
                                            realm.where(MessagesTable.class)
                                                    .greaterThan("id", 0)
                                                    .findAllSorted("id", Sort.ASCENDING);
                                    adapter.notifyDataSetChanged();
                                    recyclerView.smoothScrollToPosition(messages.size()-1);


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

    public void sendMessage(RealmResults<RoomsTable> currentRoom){

        String messageText = inputMessage.getText().toString();
        // Initialize Realm
        Realm.init(getActivity().getApplicationContext());
        // Get a Realm instance for this thread
        Realm realm = Realm.getDefaultInstance();
        // Get the current max id in the Messages table
        Number maxId = realm.where(MessagesTable.class).max("id");
        // If id is null, set it to 1, else set increment it by 1
        final int nextId = (maxId == null) ? 1 : maxId.intValue() + 1;
        MessagesTable message = new MessagesTable();
        message.setId(nextId);
        message.setUId("NotSent"); // This will get updated when we sync
        message.setRoomId(currentRoom.get(0).getuId());
        message.setText(messageText);
        // This is a  sample timestamp, it will get updated
        message.setTimestamp("sending");
        message.setSentStatus(false);
        message.setUnread(false);
        String displayName = getActivity()
                .getSharedPreferences("UserPreferences", 0)
                .getString("displayName", "");
        message.setDisplayName(displayName);
        String username = getActivity()
                .getSharedPreferences("UserPreferences", 0)
                .getString("username", "");
        message.setUsername(username);

        // Begin, copy and commit
        realm.beginTransaction();
        realm.copyToRealm(message);
        realm.commitTransaction();

        if(!isNetworkAvailable()){
            Toast.makeText(getContext(), "Message will be send when you become online", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Sending message", Toast.LENGTH_SHORT).show();

            String accessToken = getActivity()
                    .getSharedPreferences("UserPreferences", 0)
                    .getString("accessToken", "");
            RequestBody requestBody = new FormBody.Builder()
                    .add("text", messageText)
                    .build();
            Request request = new Request.Builder()
                    .url("https://api.gitter.im/v1/rooms/"
                            + currentRoom.get(0).getuId()
                            + "/chatMessages")
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization:", "Bearer " + accessToken)
                    .post(requestBody)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    e.printStackTrace();
                    call.cancel();
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try {
                        String responseText = response.body().string();
                        JSONObject dynamicJObject = new JSONObject(responseText);
                        final String uId = dynamicJObject.getString("id");
                        final String text = dynamicJObject.getString("text");
                        final String timestamp = dynamicJObject.getString("sent");
                        final boolean unread = dynamicJObject.getBoolean("unread");
                        JSONObject userObject = dynamicJObject.getJSONObject("fromUser");
                        final String displayName = userObject.getString("displayName");
                        final String username = userObject.getString("username");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Initialize Realm
                                Realm.init(getActivity().getApplicationContext());
                                // Get a Realm instance for this thread
                                Realm realm = Realm.getDefaultInstance();

                                RealmResults<MessagesTable> sentMessage =
                                        realm.where(MessagesTable.class)
                                        .equalTo("id", nextId)
                                        .findAll();
                                sentMessage.get(0).setUId(uId);
                                sentMessage.get(0).setText(text);
                                sentMessage.get(0).setTimestamp(timestamp);
                                sentMessage.get(0).setUnread(unread);
                                sentMessage.get(0).setDisplayName(displayName);
                                sentMessage.get(0).setUsername(username);
                                sentMessage.get(0).setSentStatus(true);

                                // Begin, copy and commit
                                realm.beginTransaction();
                                realm.copyToRealm(sentMessage);
                                realm.commitTransaction();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
