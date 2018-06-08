package com.codingblocks.chatter.room;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.codingblocks.chatter.RoomsTable;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface RoomDao {
    @Query("SELECT * FROM RoomsTable")
    List<RoomsTable> getAll();

    @Query("SELECT * FROM RoomsTable WHERE uId = :uId")
    RoomsTable getById(String uId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(RoomsTable... users);

    @Delete
    void delete(RoomsTable room);

    @Query("SELECT MAX(id) FROM RoomsTable")
    Integer getMaxId();
}
