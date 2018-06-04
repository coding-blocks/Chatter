package com.codingblocks.chatter;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import butterknife.ButterKnife;


public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener{

    private Boolean notification;
    private Boolean wifiEnableMessage;
    public SettingsFragment() {
        // Required empty public constructor
    }

    SharedPreferences preferences;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settingpage);
        preferences= PreferenceManager.getDefaultSharedPreferences(getActivity());


    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("notification_key"))
        {
            notification=sharedPreferences.getBoolean("notification_key",false);
            Toast.makeText(getActivity(), "Notification Enable "+notification, Toast.LENGTH_SHORT).show();
        }
        else if(key.equals("user_wifi"))
        {
            notification=sharedPreferences.getBoolean("user_wifi",false);
            Toast.makeText(getActivity(), "Wifi Messaging "+notification, Toast.LENGTH_SHORT).show();
        }
        else
        {

        }

    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
}
