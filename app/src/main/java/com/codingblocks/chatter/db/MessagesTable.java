package com.codingblocks.chatter.db;


import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.codingblocks.chatter.ListTypeConvertor;

import java.util.List;


@Entity(tableName = "messages")
@TypeConverters({ListTypeConvertor.class})
public class MessagesTable {
    private int id;
    @PrimaryKey
    @NonNull
    public String uId;
    private String text;
    private String timestamp;
    private String roomId;
    private String username;
    private String displayName;
    private boolean unread;
    private boolean sentStatus;
    private String userAvater;

    @Nullable
    public List<Mentions> getMentionsIds() {
        return mentionsIds;
    }

    public void setMentionsIds(@Nullable List<Mentions> mentionsIds) {
        this.mentionsIds = mentionsIds;
    }

    @Nullable
    private List<Mentions> mentionsIds;


    public MessagesTable(int id, @NonNull String uId, String text, String timestamp, String roomId, String username, String displayName, boolean unread, boolean sentStatus, String userAvater, List<Mentions> mentionsIds) {
        this.id = id;
        this.uId = uId;
        this.text = text;
        this.timestamp = timestamp;
        this.roomId = roomId;
        this.username = username;
        this.displayName = displayName;
        this.unread = unread;
        this.sentStatus = sentStatus;
        this.userAvater = userAvater;
        this.mentionsIds = mentionsIds;
    }

    @Ignore
    public MessagesTable() {
    }

    public int getId() {
        return id;
    }

    public String getuId() {
        return uId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean getUnread() {
        return unread;
    }

    public boolean getSentStatus() {
        return sentStatus;
    }

    public String getUserAvater() {
        return userAvater;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setUId(String uId) {
        this.uId = uId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }

    public void setSentStatus(boolean sentStatus) {
        this.sentStatus = sentStatus;
    }

    public void setUserAvater(String userAvater) {
        this.userAvater = userAvater;
    }

}
