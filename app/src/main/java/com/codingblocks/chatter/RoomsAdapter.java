package com.codingblocks.chatter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.realm.RealmResults;

public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.MyViewHolder> {

    private RealmResults<RoomsTable> rooms;
    private Context context;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView roomName;
        public TextView roomUnread;

        public MyViewHolder(View view) {
            super(view);
            roomName = (TextView) view.findViewById(R.id.room_name);
            roomUnread = (TextView) view.findViewById(R.id.room_unread);
        }
    }

    public RoomsAdapter(RealmResults<RoomsTable> rooms, Context context) {
        this.rooms = rooms;
        this.context = context;
    }

    @Override
    public RoomsAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View itemView =
                LayoutInflater.from(parent.getContext())
                .inflate(R.layout.room_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final RoomsAdapter.MyViewHolder myViewHolder, int i) {
        RoomsTable room = rooms.get(i);
        myViewHolder.roomName.setText(room.getRoomName());
        if(room.getMentions() > 0){
            myViewHolder.roomUnread.setText("@");
            myViewHolder.roomUnread.setBackgroundColor(
                    context.getResources().getColor(R.color.colorAccent)
            );
            myViewHolder.roomUnread.setBackgroundColor(
                    context.getResources().getColor(R.color.white)
            );
        } else {
            myViewHolder.roomUnread.setText(room.getUnreadItems());
        }
        final String roomId = room.getuId();
        myViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((DashboardActivity) myViewHolder.itemView.getContext())
                        .openRoom(roomId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }
}
