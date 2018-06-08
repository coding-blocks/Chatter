package com.codingblocks.chatter.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.codingblocks.chatter.MessagesTable;
import com.codingblocks.chatter.RoomsTable;

@Database(entities = {MessagesTable.class, RoomsTable.class}, version = 1)
public abstract class ChatterDatabase extends RoomDatabase {
    public  abstract RoomDao roomDao();
    public  abstract MessageDao messageDao();

    private static ChatterDatabase DB_INSTANCE;
    public static ChatterDatabase getInstance(Context context){
        if (DB_INSTANCE == null){
            DB_INSTANCE = Room.databaseBuilder(context, ChatterDatabase.class, "com.codingblocks.chatter.database").build();
        }
        return DB_INSTANCE;
    }
}
