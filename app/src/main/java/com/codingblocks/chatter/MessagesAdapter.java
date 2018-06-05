package com.codingblocks.chatter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.realm.RealmResults;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MyViewHolder> {

    private RealmResults<MessagesTable> messages;
    private Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView username;
        public TextView time;
        public TextView message;

        public MyViewHolder(View view) {
            super(view);
            username = (TextView) view.findViewById(R.id.username);
            time = (TextView) view.findViewById(R.id.time);
            message = (TextView) view.findViewById(R.id.message);
        }
    }

    public MessagesAdapter(RealmResults<MessagesTable> messages, Context context) {
        this.messages = messages;
        this.context = context;
    }

    @Override
    public MessagesAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View itemView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item, parent, false);
        return new MessagesAdapter.MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MessagesAdapter.MyViewHolder myViewHolder, int i) {
        MessagesTable message = messages.get(i);
        myViewHolder.username.setText(message.getDisplayName());
        // A timestamp looks like this 2014-03-25T11:51:32.289Z
        String timestamp = message.getTimestamp();
        Log.e("TAG", "onBindViewHolder: " + message.getText());
        if (!timestamp.equals("sending")) {
            timestamp = timestamp.substring(0, timestamp.indexOf('T')) + " " +
                    timestamp.substring(timestamp.indexOf('T') + 1,
                            timestamp.indexOf('T') + 6);
            //= 11:51 2014-03-25
        }
        myViewHolder.time.setText(timestamp); // or sending
        myViewHolder.message.setText(message.getText());
    }

    @Override
    public int getItemCount() {
        Log.e("TAG", "getItemCount: " + messages.size());
        return messages.size();
    }
}
