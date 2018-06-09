package com.codingblocks.chatter;


import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.Room;
import android.support.annotation.NonNull;

@Entity(tableName = "rooms")
public class RoomsTable {
    private int id;
    @PrimaryKey
    @NonNull
    public String uId;

    private String roomName;
    private int userCount;
    private int unreadItems;
    private int mentions;
    private String draftMessage;
    private String roomAvatar;

    public RoomsTable(int id, String uId, String roomName, int userCount, int unreadItems, int mentions, String draftMessage, String roomAvatar) {
        this.id = id;
        this.uId = uId;
        this.roomName = roomName;
        this.userCount = userCount;
        this.unreadItems = unreadItems;
        this.mentions = mentions;
        this.draftMessage = draftMessage;
        this.roomAvatar = roomAvatar;
    }

    @Ignore
    public RoomsTable(){

    }

    public int getId() {
        return id;
    }

    public int getUserCount() {
        return userCount;
    }

    public int getUnreadItems() {
        return unreadItems;
    }

    public int getMentions() {
        return mentions;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getuId() {
        return uId;
    }

    public String getDraftMessage() {
        return draftMessage;
    }

    public String getRoomAvatar() {
        return roomAvatar;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public void setUnreadItems(int unreadItems) {
        this.unreadItems = unreadItems;
    }

    public void setMentions(int mentions) {
        this.mentions = mentions;
    }

    public void setDraftMessage(String draftMessage) {
        this.draftMessage = draftMessage;
    }

    public void setRoomAvatar(String avatar) {
        this.roomAvatar = avatar;
    }
}
