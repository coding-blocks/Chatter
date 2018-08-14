package com.elirex.fayeclient.rx;

import com.elirex.fayeclient.FayeClient;

/**
 * Created by Wang, Sheng-Yuan (Elirex) on 15/9/28.
 */
public abstract class RxEvent {

    private final FayeClient mClient;

    public RxEvent(FayeClient client) {
        mClient = client;
    }

    @Override
    public abstract String toString();
}
