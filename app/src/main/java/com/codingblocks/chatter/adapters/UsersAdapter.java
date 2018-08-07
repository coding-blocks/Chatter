package com.codingblocks.chatter.adapters;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.codingblocks.chatter.R;
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
        Users users = mUsers.get(position);
        Log.i("TAG", "onBindViewHolder: " + mUsers.size());
        Picasso.get().load(users.getUrl()).into(holder.userImage);
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
