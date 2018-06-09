package com.codingblocks.chatter;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;

import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface RoomsDao {


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addRooms(RoomsTable roomsTable);


    @Query("SELECT * FROM rooms")
    List<RoomsTable> getAllRooms();

    @Query("SELECT * FROM rooms WHERE uId = :uId ORDER BY id DESC")
    List<RoomsTable> getRoomsWithuId(String uId);

    @Query("SELECT * FROM rooms WHERE uId = :uId ORDER BY id DESC")
    RoomsTable getRoomWithuId(String uId);


    //Get the current max id
    @Query("SELECT MAX(id) FROM rooms")
    int getMax();

    @Delete()
    void delete(RoomsTable roomsTable);

}
