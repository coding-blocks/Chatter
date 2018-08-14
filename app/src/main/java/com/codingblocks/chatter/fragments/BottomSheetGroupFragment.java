package com.codingblocks.chatter.fragments;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.codingblocks.chatter.MessagesDatabase;
import com.codingblocks.chatter.R;
import com.codingblocks.chatter.RoomUsersActivity;
import com.codingblocks.chatter.RoomsDatabase;
import com.codingblocks.chatter.adapters.UsersAdapter;
import com.codingblocks.chatter.db.RoomsTable;
import com.codingblocks.chatter.db.Users;
import com.codingblocks.chatter.models.MessagesDao;
import com.codingblocks.chatter.models.RoomsDao;
import com.squareup.picasso.Picasso;

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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BottomSheetGroupFragment extends BottomSheetDialogFragment {
    @BindView(R.id.group_image)
    ImageView mGroupImage;
    @BindView(R.id.group_displayname)
    TextView mGroupName;
    @BindView(R.id.group_backedby)
    TextView mGroupTopic;
    @BindView(R.id.usercount)
    TextView userCount;
    @BindView(R.id.usersRecyclerView)
    RecyclerView mUserRecyclerView;
    @BindView(R.id.deleteRoomBtn)
    Button deleteRoom;
    @BindView(R.id.removeUserBtn)
    Button removeUser;
    private OkHttpClient client = new OkHttpClient();
    private String room_id;
    //Database
    RoomsDatabase roomdb;
    RoomsDao roomsDao;
    RoomsTable roomsTable;
    MessagesDatabase messagesDatabase;
    MessagesDao messagesDao;
    UsersAdapter adapter;
    List<Users> mUsers = new ArrayList<>();
    private String uid;
    String accessToken;

    public static BottomSheetGroupFragment newInstance(String room_id) {
        BottomSheetGroupFragment bottomSheetFragment = new BottomSheetGroupFragment();
        Bundle bundle = new Bundle();
        bundle.putString("user_id", room_id);
        Log.d("TAG", "newInstance: " + room_id);
        bottomSheetFragment.setArguments(bundle);
        return bottomSheetFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.bottom_sheet_info, container, false);
        room_id = getArguments().getString("user_id");
        ButterKnife.bind(this, root);
        roomdb = RoomsDatabase.getInstance(getContext());
        roomsDao = roomdb.roomsDao();
        messagesDatabase = MessagesDatabase.getInstance(getContext());
        messagesDao = messagesDatabase.messagesDao();
        adapter = new UsersAdapter(mUsers, getContext(), 1);
        mUserRecyclerView.setHasFixedSize(true);
        mUserRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 6));
        mUserRecyclerView.setAdapter(adapter);
        accessToken = getContext()
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        uid = getContext()
                .getSharedPreferences("UserPreferences", 0)
                .getString("idOfUser", "");
        Log.i("TAG", "onResponse: " + uid + accessToken);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Picasso.get().load(roomsTable.getRoomAvatar()).into(mGroupImage);
                mGroupName.setText(roomsTable.getRoomName());
                mGroupTopic.setText(roomsTable.getTopic());
                userCount.setText("People (" + roomsTable.getUserCount() + ")");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                roomsTable = roomsDao.getRoomWithuId(room_id);
                return null;
            }
        }.execute();
        final Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/rooms/"
                        + room_id
                        + "/users")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        fetchRoomUsers(request);
        return root;
    }

    private void fetchRoomUsers(Request request) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = "{\"users\":" + response.body().string() + "}";
                try {
                    JSONObject JObject = new JSONObject(responseText);
                    JSONArray JArray = JObject.getJSONArray("users");
                    int i;
                    for (i = 0; i < JArray.length(); i++) {
                        Log.i("TAG", "onResponse: ");
                        JSONObject dynamicJObject = JArray.getJSONObject(i);
                        Log.i("TAG", "onResponse: " + dynamicJObject);
                        String id = dynamicJObject.getString("id");
                        String name = dynamicJObject.getString("username");
                        String displayName = dynamicJObject.getString("displayName");
                        String url = dynamicJObject.getString("avatarUrl");
                        String avatarUrlSmall = dynamicJObject.getString("avatarUrlSmall");
                        String role = null;
                        if (!dynamicJObject.isNull("role")) {
                            role = dynamicJObject.getString("role");
                            Log.i("TAG", "onResponse: " + role + id + "  " + uid);
                            if (role.equals("admin") && id.equals(uid)) {
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        deleteRoom.setVisibility(View.VISIBLE);
                                        deleteRoom.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                deleteRoom();
                                            }
                                        });
                                        removeUser.setVisibility(View.VISIBLE);
                                        removeUser.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Bundle bundle = new Bundle();
                                                bundle.putString("roomId", room_id);
                                                bundle.putBoolean("removeUser", true);
                                                Intent roomIntent = new Intent(getActivity(), RoomUsersActivity.class);
                                                roomIntent.putExtras(bundle);
                                                startActivity(roomIntent);
                                            }
                                        });
                                    }
                                });

                            }
                        }
                        Users user = new Users();
                        user.setId(id);
//                        user.setRole(role);
                        Log.i("TAG", "roleresponse: " + role);
                        user.setUrl(url);
                        user.setAvatarUrlSmall(avatarUrlSmall);
                        user.setDisplayName(displayName);
                        user.setUsername(name);
                        mUsers.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } finally {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }

    private void deleteRoom() {
        final Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/rooms/"
                        + room_id)
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .delete()
                .build();

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setMessage("Are you sure you want to delete this Room? \n")
                .setTitle("Delete Room")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @SuppressLint("StaticFieldLeak")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            }

                            @SuppressLint("StaticFieldLeak")
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                new AsyncTask<Void, Void, Void>() {
                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        messagesDao.deleteRoomMessages(room_id);
                                        roomsDao.deleteRoom(room_id);
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        super.onPostExecute(aVoid);
                                        getActivity().onBackPressed();
                                    }
                                }.execute();

                            }
                        });
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
}