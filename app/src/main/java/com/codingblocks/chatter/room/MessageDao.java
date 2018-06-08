package com.codingblocks.chatter.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.codingblocks.chatter.MessagesTable;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface MessageDao {
    @Query("SELECT * FROM MessagesTable")
    List<MessagesTable> getAll();

    @Query("SELECT * FROM MessagesTable WHERE uId = :uId")
    MessagesTable getById(String uId);

    @Query("SELECT * FROM MessagesTable WHERE roomId = :roomId ORDER BY id")
    List<MessagesTable> getByRoomId(String roomId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(MessagesTable messages);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll (ArrayList<MessagesTable> messages);

    @Delete
    void delete(MessagesTable message);

    @Query("SELECT MAX(id) FROM MessagesTable")
    Integer getMaxId();

    @Query("SELECT * FROM MessagesTable WHERE NOT sentStatus")
    List<MessagesTable> getUnSentMessages();

    @Query("SELECT * FROM MessagesTable WHERE (roomId = :roomId AND unread) ORDER BY id LIMIT 1")
    MessagesTable getFistUnReadByRoomId(String roomId);
}
