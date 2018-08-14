package com.elirex.fayeclient;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * @author Sheng-Yuan Wang (2015/9/4).
 */
public class FayeServiceConnection implements ServiceConnection {

    private static final String LOG_TAG =
            FayeServiceConnection.class.getSimpleName();

    private FayeService mService;
    private FayeServiceListener mListener;

    public FayeServiceConnection(FayeServiceListener listener) {
       mListener = listener;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        FayeService.FayeServiceBinder binder = ((FayeService.FayeServiceBinder) iBinder);
        mService = binder.getService();
        mService.addListener(mListener);
        Log.i(LOG_TAG, "Faye Service connected.");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
       if(mService != null)  {
           mService.removeListener(mListener);
           mService = null;
           Log.i(LOG_TAG, "Faye service disconnected.");
       }
    }

}
