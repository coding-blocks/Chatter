package com.elirex.fayeclient;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Sheng-Yuan Wang (2015/9/3).
 */
public class FayeService extends Service {

    private static final String LOG_TAG = FayeService.class.getSimpleName();

    private static String sServerUrl;
    private static MetaMessage sMetaMessage;
    private static HashSet<String> sChannels;

    private FayeServiceBinder mBinder = new FayeServiceBinder();

    private FayeClient mFayeClient;
    private ArrayList<FayeServiceListener> mListeners = new ArrayList<FayeServiceListener>();

    @Override
    public void onCreate() {
        super.onCreate();
        startFayeClient();
        Log.i(LOG_TAG, "Faye Service Starts: " + this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopFayeClient();
        Log.i(LOG_TAG, "Faye Service Stopped: " + this);
    }

    public static void initFayeService(String url, MetaMessage meta) {
        initFayeService(url, meta, "");
    }

    public static void initFayeService(String url, MetaMessage meta,
                                       String... channels) {
        sServerUrl = url;
        sMetaMessage = meta;
        if(sChannels == null)  {
            sChannels = new HashSet<String>();
        }
        for(String channel : channels) {
            sChannels.add(channel);
        }
    }

    public static ServiceConnection bind(Context context,
                                         final FayeServiceListener listener) {
        ActivityManager manager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if(FayeService.class.getName()
                    .equals(service.service.getClassName())) {
                break;
            }
        }

        // ServiceConnection connection = new ServiceConnection() {

        //     private FayeService service;

        //     @Override
        //     public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        //         FayeServiceBinder binder = ((FayeServiceBinder) iBinder);
        //         service = binder.getService();
        //         service.addListener(listener);
        //         Log.i(LOG_TAG, "Faye Service connected.");
        //     }

        //     @Override
        //     public void onServiceDisconnected(ComponentName componentName) {
        //         if(service != null)  {
        //             service.removeListener(listener);
        //             service = null;
        //             Log.i(LOG_TAG, "Faye service disconnected.");
        //         }
        //     }
        // };

        FayeServiceConnection connection = new FayeServiceConnection(listener);
        context.bindService(new Intent(context, FayeService.class),
                connection, Context.BIND_AUTO_CREATE);
        return connection;
    }

    public static void unbind(Context context, ServiceConnection connection) {
        connection.onServiceDisconnected(null);
        context.unbindService(connection);
    }

    private FayeClientListener mFayeClientListener = new FayeClientListener() {
        @Override
        public void onConnectedServer(FayeClient fc) {
            Log.i(LOG_TAG, "Connect to server");
            for(FayeServiceListener listener : mListeners) {
                listener.onConnectedToServer(fc);
            }
            Log.d(LOG_TAG, "FayeService channels.size() = " + sChannels.size());
            for(String channel : sChannels) {
                Log.d(LOG_TAG, "Channel: " + channel);
                fc.subscribeChannel(channel);
            }
        }

        @Override
        public void onDisconnectedServer(FayeClient fc) {
            Log.i(LOG_TAG, "Disconnected form server");
            fc.connectServer();
        }

        @Override
        public void onReceivedMessage(FayeClient fc, String msg) {
            Log.i(LOG_TAG, "Message from server: " + msg);
            for(FayeServiceListener listener : mListeners) {
                listener.onMessageReceived(fc, msg);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        if(mBinder == null) {
            mBinder = new FayeServiceBinder();
        }
        return mBinder;
    }

    protected void addListener(FayeServiceListener listener) {
        mListeners.add(listener);
    }

    protected void removeListener(FayeServiceListener listener) {
        mListeners.remove(listener);
        if(mListeners.size() == 0) {
            stopSelf();
        }
    }

    private void startFayeClient() {
        mFayeClient = new FayeClient(sServerUrl, sMetaMessage);
        mFayeClient.setListener(mFayeClientListener);
        mFayeClient.connectServer();
    }

    private void stopFayeClient() {
        HandlerThread thread = new HandlerThread("FayeTerminateHandlerThread");
        thread.start();
        new Handler(thread.getLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (mFayeClient.isConnectedServer()) {
                    mFayeClient.disconnectServer();
                    sChannels.clear();
                }
            }
        });
    }

    public class FayeServiceBinder extends Binder {
        public FayeService getService() {
            return FayeService.this;
        }

        public FayeClient getClient() {
            return mFayeClient;
        }
    }

}
