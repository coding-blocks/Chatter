package com.codingblocks.chatter;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class RoomsTable extends RealmObject {
    @PrimaryKey
    private int id;
    private String uId;
    private String roomName;
    private int userCount;
    private int unreadItems;
    private int mentions;

    public int getId(){return id;}
    public int getUserCount(){return userCount;}
    public int getUnreadItems(){return unreadItems;}
    public int getMentions(){return mentions;}
    public String getRoomName(){return roomName;}
    public String getuId(){return uId;}

    public void setId(int id){this.id = id;}
    public void setuId(String uId){this.uId = uId;}
    public void setRoomName(String roomName){this.roomName = roomName;}
    public void setUserCount(int userCount){this.userCount = userCount;}
    public void setUnreadItems(int unreadItems){this.unreadItems = unreadItems;}
    public void setMentions(int mentions){this.mentions = mentions;}
}
