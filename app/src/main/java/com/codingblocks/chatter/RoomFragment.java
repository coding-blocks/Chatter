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

import com.codingblocks.chatter.room.ChatterDatabase;
import com.codingblocks.chatter.room.MessageDao;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RoomFragment extends Fragment {

    private MessageDao messageDao;

    public RoomFragment() {
        // Required empty public constructor
    }

    private OkHttpClient client = new OkHttpClient();

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    @BindView(R.id.inputMessage)
    EditText inputMessage;
    @BindView(R.id.sendButton)
    ImageButton sendButton;
    String roomId;
    ArrayList<MessagesTable> messages;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_room, container, false);
        ButterKnife.bind(this, view);
        Bundle bundle = this.getArguments();

        roomId = bundle.getString("RoomId", " ");
        Log.e("TAG", "onCreateView: " + roomId);

        if (roomId.equals(" ")) {
            Toast.makeText(
                    this.getContext(),
                    "Error no room id has been passed",
                    Toast.LENGTH_SHORT
            ).show();
        }

        messageDao = ChatterDatabase.getInstance(getContext()).messageDao();
        new Thread(new Runnable() {
            @Override
            public void run() {
                messages = new ArrayList<>(messageDao.getByRoomId(roomId));
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        init();
                        displayMessages(messages, roomId);
                    }
                });
            }
        }).start();

        /* Add on change listener for messages so that we can get live results  */
//        messages.addChangeListener(new RealmChangeListener<RealmResults<MessagesTable>>() {
//            @Override
//            public void onChange(RealmResults<MessagesTable> rooms) {
//                // Update the Recycler View
//                displayMessages(messages, roomId);
//            }
//        });

//        if (!currentRoom.get(0).getDraftMessage().equals("")) {
//            inputMessage.setText(currentRoom.get(0).getDraftMessage());
//        }

        return view;
    }

    private void init() {
        displayMessages(messages, roomId);
        /* Resetting the placeholder text and setting it back if its empty */
        inputMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputMessage.getText().toString().equals("Type in here to sent")) {
                    inputMessage.setHint(R.string.type_in_placeholder);
                }
            }
        });
        inputMessage.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (inputMessage.getText().toString().trim().equals("")) {
                    inputMessage.setHint(R.string.type_in_placeholder);
                } else {
                    // TODO : Read msg from here
                }
            }
        });

        /* Sent message button */
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!inputMessage.getText().toString().equals("Type in here to sent") &&
                        !inputMessage.getText().toString().trim().equals("")) {
                    sendMessage();
                }
            }
        });
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    RecyclerView.Adapter adapter;

    public void displayMessages(ArrayList<MessagesTable> messages, String roomId) {
        /* No messages, let's get them first */
        if (messages.size() == 0) {
            getMessages(1, roomId);
        }

        adapter = new MessagesAdapter(messages, getActivity().getApplicationContext());
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);
        /* Get messages if network is available
           [we have old ones but checking for updates] */
//        getMessages(0, roomId);
    }

    public void getMessages(int severity, final String roomId) {
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
            Request request = new Request.Builder()
                    .url("https://api.gitter.im/v1/rooms/" + roomId + "/chatMessages?access_token=" + accessToken)
//                    .addHeader("Accept", "application/json")
//                    .addHeader("Authorization:", "Bearer " + accessToken)
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
                    Log.e("TAG access", "onResponse: " + accessToken);
//                    Log.e("TAG RESPONSE", "onResponse: " + responseText );
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
//                            Log.e("TAG JSON " , "run: " + i );
//                             If message exists already

                            MessagesTable containedMessage = messageDao.getById(uId);
                            // Get the current max id in the EntityName table
                            Integer maxId = messageDao.getMaxId();
                            // If id is null, set it to 1, else set increment it by 1
                            int nextId = (maxId == null) ? 1 : maxId + 1;

                            if (containedMessage != null) {
                                // Save the id so that if when we delete, we can insert it into the empty slot
                                // since we are sorting by id
                                nextId = containedMessage.getId();
                                // Delete that message, so you can push an update ;)
                                messageDao.delete(containedMessage);
                                messages.remove(containedMessage);
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
                            messages.add(message);
                            messageDao.insert(message);
                        }
                        if (i == 0) {
                            Toast.makeText(
                                    getActivity(),
                                    "There seems to be no rooms, please try again later",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }else {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                displayMessages(messages, roomId);
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

    public void sendMessage() {

        String messageText = inputMessage.getText().toString();
        // Get the current max id in the Messages table
        Integer maxId = messageDao.getMaxId();
        // If id is null, set it to 1, else set increment it by 1
        final int nextId = (maxId == null) ? 1 : maxId + 1;
        final MessagesTable message = new MessagesTable();
        message.setId(nextId);
        message.setUId(String.valueOf(System.currentTimeMillis())); // This will get updated when we sync
        message.setRoomId(roomId);
        message.setText(messageText);
        message.setTimestamp("sending"); // This is a  sample timestamp, it will get updated
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

        // Begin, copy and commit
        messages.add(message);
        adapter.notifyItemInserted(messages.size() - 1);
        messageDao.insert(message);

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
                            +roomId
                            + "/chatMessages")
                    .addHeader("Accept", "application/json")
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
                        final String avatarUrl = userObject.getString("avatarUrlMedium");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // TODO : Do something when message is sent or if error occurs

                                MessagesTable sentMessage = new MessagesTable();
                                sentMessage.setUId(uId);
                                sentMessage.setText(text);
                                sentMessage.setTimestamp(timestamp);
                                sentMessage.setUnread(unread);
                                sentMessage.setDisplayName(displayName);
                                sentMessage.setUsername(username);
                                sentMessage.setSentStatus(true);
                                sentMessage.setUserAvater(avatarUrl);

                                // Add message to database
                                messageDao.insert(sentMessage);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        displayMessages(messages, roomId);
                    }
                }
            });
        }

        inputMessage.setHint(R.string.type_in_placeholder);
    }
}
