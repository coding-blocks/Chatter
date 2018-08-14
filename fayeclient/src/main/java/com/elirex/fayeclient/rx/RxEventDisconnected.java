package com.elirex.fayeclient.rx;

import com.elirex.fayeclient.FayeClient;

/**
 * Created by Wang, Sheng-Yuan (Elirex) on 15/9/29.
 */
public class RxEventDisconnected extends RxEvent {

    public RxEventDisconnected(FayeClient client) {
        super(client);
    }

    @Override
    public String toString() {
        return "Faye client is disconnected to server";
    }
}
