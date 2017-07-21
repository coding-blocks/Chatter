package com.codingblocks.chatter;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmResults;
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
        /* Sync new messages to server part */
        Realm.init(context);
        Realm realm = Realm.getDefaultInstance();
        final RealmResults<MessagesTable> messagesToBeSent =
                realm.where(MessagesTable.class)
                        .equalTo("sentStatus", false)
                        .findAll();
        for(int i = 0; i < messagesToBeSent.size(); i++){
            final int k = i; // Simple encapsulation hack
            String accessToken = sharedPreferences.getString("accessToken", "");
            if (!accessToken.equals("")) {
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
        }
        // Begin, copy and commit
        realm.beginTransaction();
        realm.copyToRealm(messagesToBeSent);
        realm.commitTransaction();
    }
}
