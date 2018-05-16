package com.codingblocks.chatter.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.codingblocks.chatter.SyncAdapter;

public class SyncService extends Service {
    // Storage for an instance of the sync adapter
    private SyncAdapter sSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSyncAdapter.getSyncAdapterBinder();
    }
}
