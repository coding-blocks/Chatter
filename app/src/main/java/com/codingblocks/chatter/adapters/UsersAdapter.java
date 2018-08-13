package com.codingblocks.chatter.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.codingblocks.chatter.R;
import com.codingblocks.chatter.UserActivity;
import com.codingblocks.chatter.db.Users;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UsersAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Users> mUsers;
    private Context context;
    private int type;
    private String roomId;

    public UsersAdapter(List<Users> mUsers, Context context, int type) {
        this.mUsers = mUsers;
        this.context = context;
        this.type = type;
    }

    public UsersAdapter(List<Users> mUsers, Context context, int type, String roomId) {
        this.mUsers = mUsers;
        this.context = context;
        this.type = type;
        this.roomId = roomId;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (type) {
            case 1:
                View currentUserView = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_user, parent, false);
                viewHolder = new MyViewHolder(currentUserView); // view holder for normal items
                break;
            case 2:
                View otherUserView = LayoutInflater.from(parent.getContext()).inflate(R.layout.room_item, parent, false);
                viewHolder = new RoomsAdapter.MyViewHolder(otherUserView); // view holder for normal items
                break;
            case 3:
                View otherUserView2 = LayoutInflater.from(parent.getContext()).inflate(R.layout.room_item, parent, false);
                viewHolder = new RoomsAdapter.MyViewHolder(otherUserView2); // view holder for normal items
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        final Users users = mUsers.get(position);
        if (type == 1) {
            MyViewHolder holder = (MyViewHolder) viewHolder;
            Picasso.get().load(users.getUrl()).into(holder.userImage);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", users.getId());
                    bundle.putString("userName", users.getUsername());
                    Intent userIntent = new Intent(v.getContext(), UserActivity.class);
                    userIntent.putExtras(bundle);
                    v.getContext().startActivity(userIntent);
                }
            });
        } else if (type == 2) {
            RoomsAdapter.MyViewHolder holder = (RoomsAdapter.MyViewHolder) viewHolder;
            holder.roomName.setText(users.getDisplayName());
            Picasso.get().load(users.getAvatarUrlSmall()).into(holder.avatarImage);
            holder.roomUnread.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", users.getId());
                    bundle.putString("userName", users.getUsername());
                    Intent userIntent = new Intent(v.getContext(), UserActivity.class);
                    userIntent.putExtras(bundle);
                    v.getContext().startActivity(userIntent);
                }
            });
        } else if (type == 3) {
            RoomsAdapter.MyViewHolder holder = (RoomsAdapter.MyViewHolder) viewHolder;
            holder.roomName.setText(users.getDisplayName());
            Picasso.get().load(users.getAvatarUrlSmall()).into(holder.avatarImage);
            holder.roomUnread.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addUser(v, users);
                }
            });
        }
    }

    private void addUser(View v, Users users) {
        final OkHttpClient client = new OkHttpClient();
        String accessToken = context
                .getSharedPreferences("UserPreferences", 0)
                .getString("accessToken", "");
        RequestBody requestBody = new FormBody.Builder()
                .add("username", users.getUsername())
                .build();
        final Request request = new Request.Builder()
                .url("https://api.gitter.im/v1/rooms/"
                        + roomId
                        + "/users")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(requestBody)
                .build();

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setMessage("Are you sure you want to add this person to Room?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @SuppressLint("StaticFieldLeak")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                Log.i("TAG", "onResponse: "+response.body().string());
                                if (response.isSuccessful()) {
                                    ((Activity) context).finish();

                                }

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


    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public ImageView userImage;

        public MyViewHolder(View view) {
            super(view);
            userImage = view.findViewById(R.id.avatar);
        }
    }
}
