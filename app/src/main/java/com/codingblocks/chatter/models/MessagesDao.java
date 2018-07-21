package com.codingblocks.chatter.models;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.codingblocks.chatter.db.MessagesTable;

import java.util.List;

@Dao
public interface MessagesDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addMessages(MessagesTable messagesTable);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addMultipleMessages(List<MessagesTable> messagesTable);

    @Query("SELECT * FROM messages")
    List<MessagesTable> getAllMessages();

    @Query("SELECT * FROM messages WHERE uId = :uId")
    MessagesTable getById(String uId);

    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY id ASC")
    List<MessagesTable> getRoomMessages(String roomId);

    //Get the current max id
    @Query("SELECT MAX(id) FROM messages")
    Integer getMax();

    @Query("SELECT * FROM messages WHERE id = :nextId")
    List<MessagesTable> getSentMessage(int nextId);

    @Query("SELECT * FROM messages WHERE sentStatus = 0")
    List<MessagesTable> getPendingMessages();

    @Query("SELECT * FROM messages WHERE sentStatus = 1 AND roomId = :roomId ORDER BY id LIMIT 1")
    MessagesTable getUnreadMessages(String roomId);

    @Delete
    void delete(MessagesTable message);
}
