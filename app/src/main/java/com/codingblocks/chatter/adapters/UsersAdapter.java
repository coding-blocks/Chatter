package com.codingblocks.chatter.adapters;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import java.util.List;
public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.MyViewHolder> {
    private List<Users> mUsers;
    private Context context;
    public UsersAdapter(List<Users> mUsers, Context context) {
        this.mUsers = mUsers;
        this.context = context;
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_user, parent, false);
        return new UsersAdapter.MyViewHolder(itemView);
    }
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final Users users = mUsers.get(position);
        Log.i("TAG", "onBindViewHolder: " + mUsers.size());
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
