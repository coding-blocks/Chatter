package com.elirex.fayeclient.rx;

import com.elirex.fayeclient.FayeClient;

/**
 * Created by Wang, Sheng-Yuan (Elirex) on 15/9/29.
 */
public class RxEventMessage extends RxEvent {

    private final Object mMessage;

    public RxEventMessage(FayeClient client, Object message) {
        super(client);
        mMessage = message;
    }

    @SuppressWarnings("unchecke")
    public <T> T message() throws ClassCastException {
        return (T) mMessage;
    }

    @Override
    public String toString() {
        return (String) mMessage;
    }
}
