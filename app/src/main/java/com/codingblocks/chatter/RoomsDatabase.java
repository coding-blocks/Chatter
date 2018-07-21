package com.codingblocks.chatter;


import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.codingblocks.chatter.db.RoomsTable;
import com.codingblocks.chatter.models.RoomsDao;

@Database(entities = {RoomsTable.class},version = 1,exportSchema = false)
public abstract class RoomsDatabase extends RoomDatabase {
    private static RoomsDatabase INSTANCE;

    private static Object LOCK = new Object();

    public static RoomsDatabase getInstance(Context context){
        if(INSTANCE == null){
            synchronized (LOCK){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext()
                            ,RoomsDatabase.class,RoomsDatabase.DB_NAME).build();
                }
            }
        }
        return INSTANCE;
    }

    public static final String DB_NAME = "rooms_db";

    abstract public RoomsDao roomsDao();
}
