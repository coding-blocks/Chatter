package com.codingblocks.chatter;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.codingblocks.chatter.db.MessagesTable;
import com.codingblocks.chatter.models.MessagesDao;

@Database(entities = {MessagesTable.class}, version = 1, exportSchema = false)
public abstract class MessagesDatabase extends RoomDatabase {
    private static MessagesDatabase INSTANCE;

    private static Object LOCK = new Object();

    public static MessagesDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext()
                            , MessagesDatabase.class, MessagesDatabase.DB_NAME).build();
                }
            }
        }
        return INSTANCE;
    }

    public static final String DB_NAME = "messages_db";

    public abstract MessagesDao messagesDao();
}
