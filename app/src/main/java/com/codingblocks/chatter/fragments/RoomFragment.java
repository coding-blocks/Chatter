package com.codingblocks.chatter.fragments;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.codingblocks.chatter.MessagesDatabase;
import com.codingblocks.chatter.NoNetworkActivity;
import com.codingblocks.chatter.R;
import com.codingblocks.chatter.RoomsDatabase;
import com.codingblocks.chatter.SplashActivity;
import com.codingblocks.chatter.adapters.MessagesAdapter;
import com.codingblocks.chatter.db.Mentions;
import com.codingblocks.chatter.db.MessagesTable;
import com.codingblocks.chatter.db.RoomsTable;
import com.codingblocks.chatter.models.MessagesDao;
import com.codingblocks.chatter.models.RoomsDao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class RoomFragment extends Fragment {

    private OkHttpClient client = new OkHttpClient();
    @BindView(R.id.recycler_view)
    public RecyclerView recyclerView;
    @BindView(R.id.inputMessage)
    public EditText inputMessage;
    @BindView(R.id.sendButton)
    public ImageButton sendButton;
    @BindView(R.id.joinRoom)
    FloatingActionButton fab;
    MessagesAdapter adapter;
    LinearLayoutManager layoutManager;
    List<MessagesTable> messages = new ArrayList<>();
    String roomId;

    //Database
    RoomsDatabase roomdb;
    RoomsDao roomsDao;

    MessagesDatabase messagesDatabase;
    MessagesDao messagesDao;

    public RoomFragment() {
        // Required empty public constructor
    }


    @SuppressLint("StaticFieldLeak")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room, container, false);
        ButterKnife.bind(this, view);
        final Bundle bundle = this.getArguments();
        if (bundle.getString("RoomName") != null)
            getActivity().setTitle(bundle.getString("RoomName"));
        else {
            getActivity().setTitle("Chatter");
        }
        roomId = bundle.getString("RoomId", " ");
        if (roomId.equals(" ")) {
            Toast.makeText(
                    this.getContext(),
                    "Error no room id has been passed",
                    Toast.LENGTH_SHORT
            ).show();
        }
        init();

        /* Sent message button */

        roomdb = RoomsDatabase.getInstance(getContext());
        roomsDao = roomdb.roomsDao();
        final List<RoomsTable> currentRoom = new ArrayList<>();

        new AsyncTask<Void, Void, List<RoomsTable>>() {

            @Override
            protected List<RoomsTable> doInBackground(Void... voids) {
                return roomsDao.getRoomsWithuId(roomId);
            }

            @Override
            protected void onPostExecute(List<RoomsTable> rooms) {
                currentRoom.clear();
                currentRoom.addAll(rooms);
                if (!bundle.getBoolean("roomMember")) {
                    sendButton.setOnClickListener(null);
                    inputMessage.setEnabled(false);
                    fab.setVisibility(View.VISIBLE);
                    fab.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            joinRoom(view);
                        }
                    });
                }
            }
        }.execute();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!inputMessage.getText().toString().equals("Type in here to sent") &&
                        !inputMessage.getText().toString().trim().equals("")) {
                    sendMessage(currentRoom);
                }
            }
        });

        return view;
    }

    private void joinRoom(final View view) {
        final String accessToken = getActivity()
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        String uid = getActivity()
                .getSharedPreferences("UserPreferences", 0)
                .getString("idOfUser", "");
        Log.i("TAG", "onResponse: " + uid + accessToken);
        RequestBody requestBody = new FormBody.Builder()
                .add("id", roomId)
                .build();
        final Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/user/"
                        + uid
                        + "/rooms")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(view, "Added in the Room", Snackbar.LENGTH_LONG).show();
                            fab.setVisibility(View.GONE);
                            inputMessage.setEnabled(true);
                        }
                    });

                }

            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    private void init() {
        messagesDatabase = MessagesDatabase.getInstance(getContext());
        messagesDao = messagesDatabase.messagesDao();

        new AsyncTask<Void, Void, List<MessagesTable>>() {
            @Override
            protected List<MessagesTable> doInBackground(Void... voids) {
                return messagesDao.getRoomMessages(roomId);
            }

            @Override
            protected void onPostExecute(List<MessagesTable> messagesTables) {
                Log.d("TAG", "onPostExecute: " + messages.size() + messagesTables.size());
                messages.clear();
                messages.addAll(messagesTables);
                adapter.notifyDataSetChanged();
                if (messages.size() == 0)
                    getMessages(1, null);
            }
        }.execute();
        /* No messages, let's get them first */

        adapter = new MessagesAdapter(messages, getContext());
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
                int totalItemCount = layoutManager.getItemCount();
                Log.i("TAG", "onScrolled:     " + firstVisibleItemPosition);
                if (firstVisibleItemPosition + 1 == totalItemCount) {
                    Log.i("TAG", "onScrolled:   " + firstVisibleItemPosition + messages.get(firstVisibleItemPosition).getText());
                    getMessages(0, messages.get(firstVisibleItemPosition).getuId());

                }
            }
        });


    }

    private void getMessages(int severity, final String nextMessageId) {
        final List<MessagesTable> messagesTableList = new ArrayList<>();
        if (isNetworkAvailable()) {
            /* Display a toast to inform the user that we are syncing */
            Toast.makeText(
                    getActivity(), "Syncing data", Toast.LENGTH_SHORT
            ).show();
            final String accessToken = getActivity()
                    .getSharedPreferences("UserPreferences", 0)
                    .getString("accessToken", "");
            if (accessToken.equals("")) {
                Intent intent = new Intent(getActivity(), SplashActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }

            //start fetching Messages
            Request request = new Request.Builder()
                    .url("https://api.gitter.im/v1/rooms/" + roomId + "/chatMessages?access_token=" + accessToken + "&limit=20")
                    .build();
            if (nextMessageId != null) {
                Log.i("TAG", "getMessages: " + nextMessageId);
                request = new Request.Builder()
                        .url("https://api.gitter.im/v1/rooms/" + roomId + "/chatMessages?access_token=" + accessToken + "&limit=20&beforeId=" + nextMessageId)
                        .build();
            }
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @SuppressLint("StaticFieldLeak")
                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseText = "{\"messages\":" + response.body().string() + "}";
                    try {
                        JSONObject JObject = new JSONObject(responseText);
                        JSONArray JArray = JObject.getJSONArray("messages");
                        int i;
                        for (i = 0; i < JArray.length(); i++) {
                            JSONObject dynamicJObject = JArray.getJSONObject(i);
                            String uId = dynamicJObject.getString("id");
                            String text = dynamicJObject.getString("text");
                            String timestamp = dynamicJObject.getString("sent");
                            boolean unread = dynamicJObject.getBoolean("unread");
                            JSONObject userObject = dynamicJObject.getJSONObject("fromUser");
                            String displayName = userObject.getString("displayName");
                            String username = userObject.getString("username");
                            String avatarUrl = userObject.getString("avatarUrlMedium");
                            List<Mentions> mentionId = new ArrayList<>();
                            JSONObject mentions = null;
                            if (!dynamicJObject.isNull("mentions")) {
                                if (dynamicJObject.getString("mentions").length() > 2) {
                                    final String mentionsText = "{\"mentions\":" + dynamicJObject.getString("mentions") + "}";
                                    mentions = new JSONObject(mentionsText);
                                    JSONArray mentionsArray = mentions.getJSONArray("mentions");
                                    for (int j = 0; j < mentionsArray.length(); j++) {
                                        JSONObject mentionObjects = mentionsArray.getJSONObject(j);
                                        if (!mentionObjects.isNull("userId")) {
                                            String id = mentionObjects.getString("userId");
                                            String screenName = mentionObjects.getString("screenName");
                                            mentionId.add(new Mentions(id, screenName));
                                        }
                                    }
                                }
                            }


                            final MessagesTable message = new MessagesTable();
//                            message.setId(nextId);
                            message.setUId(uId);
                            message.setText(text);
                            message.setTimestamp(timestamp);
                            message.setUnread(unread);
                            message.setSentStatus(true);
                            message.setDisplayName(displayName);
                            message.setRoomId(roomId);
                            message.setUsername(username);
                            message.setUserAvater(avatarUrl);
                            message.setMentionsIds(mentionId);
                            messagesTableList.add(message);

                        }
                        if (i == 0) {
                            Looper.prepare();
                            if (getActivity() != null)
                                Toast.makeText(
                                        getActivity(),
                                        "There seem to be no more messages",
                                        Toast.LENGTH_SHORT
                                ).show();
                            Looper.loop();

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        Collections.reverse(messagesTableList);
                        messagesDao.addMultipleMessages(messagesTableList);
                        messages.addAll(messagesTableList);
                        if (getActivity() != null)
                            getActivity().runOnUiThread(new Runnable() {
                                @SuppressLint("StaticFieldLeak")
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                    if (nextMessageId == null) {
                                        recyclerView.scrollToPosition(0);
                                    }
                                }
                            });
                    }
                }
            });

        } else if (severity == 1) {
            Intent intent = new Intent(getActivity(), NoNetworkActivity.class);
            intent.putExtra("calledFrom", "DashboardActivity");
            getActivity().startActivity(intent);
            getActivity().finish();
        }

    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @SuppressLint("StaticFieldLeak")
    public void sendMessage(List<RoomsTable> currentRoom) {

        String messageText = inputMessage.getText().toString();
        final int[] maxId = new int[1];
        final int[] nextId = new int[1];
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    maxId[0] = messagesDao.getMax();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                nextId[0] = (maxId[0] == 0) ? 1 : maxId[0] + 1;
            }
        }.execute();
        // If id is null, set it to 1, else set increment it by 1


        final MessagesTable message = new MessagesTable();
        message.setId(nextId[0]);
        message.setUId("NotSent"); // This will get updated when we sync
        message.setRoomId(currentRoom.get(0).getuId());
        message.setText(messageText);
        // This is a  sample timestamp, it will get updated
        message.setTimestamp("sending");
        message.setSentStatus(false);
        message.setUnread(false);
        final String displayName = getActivity()
                .getSharedPreferences("UserPreferences", 0)
                .getString("displayName", "");
        message.setDisplayName(displayName);
        String username = getActivity()
                .getSharedPreferences("UserPreferences", 0)
                .getString("username", "");
        message.setUsername(username);


        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                messagesDao.addMessages(message);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                adapter.notifyDataSetChanged();
            }
        }.execute();

        if (!isNetworkAvailable()) {
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
                    .addHeader("Authorization", "Bearer " + accessToken)
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
                                final List<MessagesTable> sentMessage = new ArrayList<>();
                                new AsyncTask<Void, Void, List<MessagesTable>>() {

                                    @Override
                                    protected List<MessagesTable> doInBackground(Void... voids) {
                                        return messagesDao.getSentMessage(nextId[0]);
                                    }

                                    @Override
                                    protected void onPostExecute(List<MessagesTable> rooms) {
                                        sentMessage.clear();
                                        sentMessage.addAll(rooms);
                                    }
                                }.execute();

                                sentMessage.get(0).setUId(uId);
                                sentMessage.get(0).setText(text);
                                sentMessage.get(0).setTimestamp(timestamp);
                                sentMessage.get(0).setUnread(unread);
                                sentMessage.get(0).setDisplayName(displayName);
                                sentMessage.get(0).setUsername(username);
                                sentMessage.get(0).setSentStatus(true);

                                new AsyncTask<Void, Void, Void>() {

                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        messagesDao.addMultipleMessages(sentMessage);
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        super.onPostExecute(aVoid);
                                        adapter.notifyDataSetChanged();
                                    }
                                }.execute();
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
//                        displayMessages(messages, roomId);
                    }
                }
            });
        }

        inputMessage.setHint(R.string.type_in_placeholder);

    }
}