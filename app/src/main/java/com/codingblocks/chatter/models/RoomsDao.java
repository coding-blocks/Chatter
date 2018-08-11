package com.codingblocks.chatter.models;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.codingblocks.chatter.db.RoomsTable;

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

    @Query("SELECT * FROM rooms WHERE userCount = 2 ORDER BY id DESC")
    List<RoomsTable> getPeopleRooms();

    @Query("SELECT * FROM rooms WHERE roomName = :name ")
    RoomsTable getRoomWithName(String name);


    //Get the current max id
    @Query("SELECT MAX(id) FROM rooms")
    int getMax();

    @Delete()
    void delete(RoomsTable roomsTable);

    @Query("DELETE FROM ROOMS WHERE  uId = :uId")
    void deleteRoom(String uId);


}
