package com.elirex.fayeclient;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.elirex.fayeclient.rx.RxEvent;
import com.elirex.fayeclient.rx.RxEventConnected;
import com.elirex.fayeclient.rx.RxEventDisconnected;
import com.elirex.fayeclient.rx.RxEventMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.NotYetConnectedException;
import java.util.HashSet;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import rx.Observable;
import rx.Subscriber;

/**
 * @author Sheng-Yuan Wang (2015/9/3).
 */
public class FayeClient {

    private static final String LOG_TAG = FayeClient.class.getSimpleName();

    private WebSocket mWebSocket = null;
    private FayeClientListener mListener = null;
    private HashSet<String> mChannels;
    private String mServerUrl = "";
    private boolean mFayeConnected = false;
    private boolean mIsConnectedServer = false;
    private MetaMessage mMetaMessage;
    private Handler mMessageHandler;

    public FayeClient(String url, MetaMessage meta) {
        mServerUrl = url;
        mMetaMessage = meta;
        mChannels = new HashSet<String>();
    }

    {
        HandlerThread thread = new HandlerThread("FayeHandler");
        thread.start();
        mMessageHandler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch(msg.what) {
                    case WebSocket.ON_OPEN:
                        Log.i(LOG_TAG, "onOpen() executed");
                        mIsConnectedServer = true;
                        handShake();
                        break;
                    case WebSocket.ON_CLOSE:
                        Log.i(LOG_TAG, "onClosed() executed");
                        mIsConnectedServer = false;
                        mFayeConnected = false;
                        if(mListener != null && mListener instanceof FayeClientListener) {
                            mListener.onDisconnectedServer(FayeClient.this);
                        }
                        break;
                    case WebSocket.ON_MESSAGE:
                        try {
                            Log.i(LOG_TAG, "onMessage executed");
                            handleFayeMessage((String) msg.obj);
                        } catch (NotYetConnectedException e) {
                            // Do noting
                        }
                        break;
                }
            }
        };
    }

    /* Public Methods */
    public FayeClientListener getListener() {
        return mListener;
    }

    public void setListener(FayeClientListener listener) {
        mListener = listener;
    }

    public void addChannel(String channel) {
        mChannels.add(channel);
    }

    public boolean isConnectedServer() {
        return mIsConnectedServer;
    }

    public boolean isFayeConnected() {
        return mFayeConnected;
    }

    public void connectServer() {
        openWebSocketConnection();
    }

    public void disconnectServer() {
        for(String channel : mChannels) {
            unsubscribe(channel);
        }
        mChannels.clear();
        disconnect();
    }

    public void subscribeChannel(String channel) {
        mChannels.add(channel);
        subscribe(channel);
    }

    public void subscribeToChannels(String... channels) {
        for(String channel : channels) {
            mChannels.add(channel);
            subscribe(channel);
        }
    }

    public void unsubscribeChannel(String channel) {
        if(mChannels.contains(channel)) {
            unsubscribe(channel);
            mChannels.remove(channel);
        }
    }

    public void unsubscribeChannels(String... channels) {
        for(String channel : channels) {
            unsubscribe(channel);
        }
    }

    public void unsubscribeAll() {
        for(String channel : mChannels) {
            unsubscribe(channel);
        }
    }

    public void publish(String channel, String data) {
        publish(channel, data, null, null);
    }

    public void publish(String channel, String data, String ext, String id) {
        try {
            String publish = mMetaMessage.publish(channel, data, ext, id);
            mWebSocket.send(publish);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Build publish message to JSON error", e);
        }
    }

    /* Private Methods */
    private Socket getSSLWebSocket() {
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, null, null);
            SSLSocketFactory factory = sslContext.getSocketFactory();
            return factory.createSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void openWebSocketConnection() {
        // Clean up any existing socket
        if(mWebSocket != null) {
            mWebSocket.close();
        }
        try {
            URI uri = new URI(mServerUrl);
            mWebSocket = new WebSocket(uri, mMessageHandler);
            if(uri.getScheme().equals("wss")) {
                mWebSocket.setSocket(getSSLWebSocket());
            }
            mWebSocket.connect();
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, "Server URL error", e);
        }
    }

    private void closeWebSocketConnection() {
        if(mWebSocket != null) {
            mWebSocket.close();
        }
        mListener = null;
    }

    private void handShake() {
        try {
            String handshake = mMetaMessage.handShake();
            mWebSocket.send(handshake);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "HandShake message error", e);
        }
    }

    private void subscribe(String channel) {
        try {
            String subscribe = mMetaMessage.subscribe(channel);
            mWebSocket.send(subscribe);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Subscribe message error", e);
        }
    }

    private void unsubscribe(String channel) {
        try {
            String unsubscribe = mMetaMessage.unsubscribe(channel);
            mWebSocket.send(unsubscribe);
            Log.i(LOG_TAG, "UnSubscribe:" + channel);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unsubscribe message error", e);
        }
    }

    private void connect() {
        try {
            String connect = mMetaMessage.connect();
            mWebSocket.send(connect);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Connect message error", e);
        }
    }

    private void disconnect() {
        try {
            String disconnect = mMetaMessage.disconnect();
            mWebSocket.send(disconnect);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Disconnect message error", e);
        }
    }

    private void handleFayeMessage(String message) {
        JSONArray arr = null;
        try {
            arr = new JSONArray(message);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Unknown message type: " + message, e);
        }

        int length = arr.length();
        for(int i = 0; i < length; ++i) {
            JSONObject obj = arr.optJSONObject(i);
            if(obj == null) continue;

            String channel = obj.optString(MetaMessage.KEY_CHANNEL);
            boolean successful = obj.optBoolean("successful");
            if(channel.equals(MetaMessage.HANDSHAKE_CHANNEL)) {
                if(successful) {
                    mMetaMessage.setClient(obj.optString(MetaMessage.KEY_CLIENT_ID));
                    if(mListener != null && mListener instanceof FayeClientListener) {
                        mListener.onConnectedServer(this);
                    }
                    connect();
                } else {
                    Log.e(LOG_TAG, "Handshake Error: " + obj.toString());
                }
                return;
            }

            if(channel.equals(MetaMessage.CONNECT_CHANNEL)) {
                if(successful) {
                    mFayeConnected = true;
                    connect();
                } else {
                    Log.e(LOG_TAG, "Connecting Error: " + obj.toString());
                }
                return;
            }

            if(channel.equals(MetaMessage.DISCONNECT_CHANNEL)) {
                if(successful) {
                    if(mListener != null && mListener instanceof FayeClientListener) {
                        mListener.onDisconnectedServer(this);
                    }
                    mFayeConnected = false;
                    closeWebSocketConnection();
                } else {
                    Log.e(LOG_TAG, "Disconnecting Error: " + obj.toString());
                }
                return;
            }

            if(channel.equals(MetaMessage.SUBSCRIBE_CHANNEL)) {
                String subscription = obj.optString(MetaMessage.KEY_SUBSCRIPTION);
                if(successful) {
                    mFayeConnected = true;
                    Log.i(LOG_TAG, "Subscribed channel " + subscription);
                } else {
                    Log.e(LOG_TAG, "Subscribing channel " + subscription
                            + " Error: " + obj.toString());
                }
                return;
            }

            if(channel.equals(MetaMessage.UNSUBSCRIBE_CHANNEL)) {
                String subscription = obj.optString(MetaMessage.KEY_SUBSCRIPTION);
                if(successful) {
                    Log.i(LOG_TAG, "Unsubscribed channel " + subscription);
                } else {
                    Log.e(LOG_TAG, "Unsubscribing channel " + subscription
                            + " Error: " + obj.toString());
                }
                return;
            }

            if(mChannels.contains(channel)) {
                String data = obj.optString(MetaMessage.KEY_DATA, null);
                if(data != null) {
                    if(mListener != null && mListener instanceof FayeClientListener) {
                        mListener.onReceivedMessage(this, data);
                    }
                }
            } else {
                Log.e(LOG_TAG, "Cannot handle this message: " + obj.toString());
            }
            return;
        }
    }

    public Observable<RxEvent> observable() {
        return Observable.create(new Observable.OnSubscribe<RxEvent>() {
            @Override
            public void call(final Subscriber<? super RxEvent> subscriber) {
                FayeClientListener listener = new FayeClientListener() {
                    @Override
                    public void onConnectedServer(FayeClient fc) {
                        if(subscriber.isUnsubscribed()) {
                            Log.d(LOG_TAG, "1.unsubscribed()");
                            setListener(null);
                        } else {
                            RxEventConnected event = new RxEventConnected(fc);
                            subscriber.onNext(event);
                        }
                    }

                    @Override
                    public void onDisconnectedServer(FayeClient fc) {
                        if(subscriber.isUnsubscribed()) {
                            Log.d(LOG_TAG, "2.unsubscribed()");
                            setListener(null);
                        } else {
                            RxEventDisconnected event = new RxEventDisconnected(fc);
                            subscriber.onNext(event);
                        }
                    }

                    @Override
                    public void onReceivedMessage(FayeClient fc, String msg) {
                        if(subscriber.isUnsubscribed()) {
                            Log.d(LOG_TAG, "3.unsubscribed()");
                            setListener(null);
                        } else {
                            RxEventMessage event = new RxEventMessage(fc, msg);
                            subscriber.onNext(event);
                        }
                    }
                };
                setListener(listener);
                connectServer();
            }
        });
    }

}
