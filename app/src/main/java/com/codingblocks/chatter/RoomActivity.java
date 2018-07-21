package com.codingblocks.chatter;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.codingblocks.chatter.fragments.RoomFragment;

public class RoomActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        Intent i = getIntent();
        Bundle bundle = i.getExtras();

        Log.i("TAG", "onCreate: "+i.getExtras()+i.getBundleExtra("RoomId")+bundle.get("RoomId"));
        RoomFragment roomFragment = new RoomFragment();
        roomFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.room_fragment_holder, roomFragment)
                .commit();
    }
}
