package com.codingblocks.chatter;

import android.accounts.Account;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.codingblocks.chatter.room.ChatterDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private ContentResolver mContentResolver;
    private SharedPreferences sharedPreferences;
    Context context;

    private OkHttpClient client = new OkHttpClient();

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
        sharedPreferences = context.getSharedPreferences("UserPreferences", 0);
        this.context = context;
    }

    /* For backward compatibility to maintain compatiblity with Android 3.0  */
    public SyncAdapter(
            Context context,
            boolean autoInitialize,
            boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
        sharedPreferences = context.getSharedPreferences("UserPreferences", 0);
    }

    @Override
    public void onPerformSync(
            Account account,
            Bundle bundle,
            String s,
            ContentProviderClient contentProviderClient,
            SyncResult syncResult) {
        final String accessToken = sharedPreferences.getString("accessToken", "");
        if (!accessToken.equals("")) {
            /* Sync new messages to server part */
            final ArrayList<MessagesTable> messagesToBeSent = new ArrayList<>(
                    ChatterDatabase.getInstance(context).messageDao().getUnSentMessages());
            for (int i = 0; i < messagesToBeSent.size(); i++) {
                final int k = i; // Simple encapsulation hack
                String messageText = messagesToBeSent.get(i).getText();
                RequestBody requestBody = new FormBody.Builder()
                        .add("text", messageText)
                        .build();
                Request request = new Request.Builder()
                        .url("https://api.gitter.im/v1/rooms/:"
                                + messagesToBeSent.get(i).getRoomId()
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
                    public void onResponse(@NonNull Call call, @NonNull Response response)
                            throws IOException {
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
                            messagesToBeSent.get(k).setUId(uId);
                            messagesToBeSent.get(k).setText(text);
                            messagesToBeSent.get(k).setTimestamp(timestamp);
                            messagesToBeSent.get(k).setUnread(unread);
                            messagesToBeSent.get(k).setDisplayName(displayName);
                            messagesToBeSent.get(k).setUsername(username);
                            messagesToBeSent.get(k).setSentStatus(true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            // Add to database
            ChatterDatabase.getInstance(context).messageDao().insertAll(messagesToBeSent);

            // Sync messages
            final ArrayList<RoomsTable> rooms = new ArrayList<>(
                    ChatterDatabase.getInstance(context).roomDao().getAll());
            // Setup notifications to be displayed asap
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getContext())
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("There are new messages")
                            .setContentText("Click here to expand");
            final int NOTIFICATION_ID = 1234567890;
            Intent intent = new Intent(getContext(), DashboardActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(
                    getContext(),
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
            mBuilder.setContentIntent(contentIntent);
            final NotificationCompat.InboxStyle inboxStyle =
                    new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle("Event tracker details:");
            for (int i = 0; i < rooms.size(); i++) {
                final MessagesTable unreadMessages = ChatterDatabase.getInstance(context)
                        .messageDao()
                        .getFistUnReadByRoomId(rooms.get(i).getuId());
                final RoomsTable room = rooms.get(i);
                Request request = new Request.Builder()
                        .url("https://api.gitter.im/v1/rooms/:"
                                + room.getuId()
                                + "/chatMessages?"
                                + "afterId="
                                + unreadMessages.getuId())
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        e.printStackTrace();
                        call.cancel();
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response)
                            throws IOException {
                        final String responseText = "{\"messages\":" + response.toString() + "}";
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

                                // Get the current max id in the messages table
                                Integer maxId = ChatterDatabase.getInstance(context).messageDao().getMaxId();
                                // If id is null, set it to 1, else set increment it by 1
                                int nextId = (maxId == null) ? 1 : maxId + 1;

                                MessagesTable message = new MessagesTable();
                                message.setId(nextId);
                                message.setUId(uId);
                                message.setText(text);
                                message.setTimestamp(timestamp);
                                message.setUnread(unread);
                                message.setSentStatus(true);
                                message.setDisplayName(displayName);
                                message.setRoomId(room.getuId());
                                message.setUsername(username);
                                // Add to database
                                ChatterDatabase.getInstance(context).messageDao().insert(message);
                            }
                            // Add sub notifications
                            if(i > 0) {
                                if(i == 1) {
                                    inboxStyle.addLine("1 unread message in " + room.getRoomName());
                                } else {
                                    inboxStyle.addLine(i + " unread message in " + room.getRoomName());
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getContext());
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(DashboardActivity.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(intent);
            mBuilder.setStyle(inboxStyle);
            NotificationManager nManager = (NotificationManager)
                    getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nManager != null) {
                nManager.notify(NOTIFICATION_ID, mBuilder.build());
            }
        }
    }
}
