package com.codingblocks.chatter.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.codingblocks.chatter.Authenticator;

public class AuthenticatorService extends Service {

    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}


