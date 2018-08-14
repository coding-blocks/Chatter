package com.elirex.fayeclient;

/**
 * @author Sheng-Yuan Wang (2015/9/7).
 */
public interface FayeClientListener {

    public void onConnectedServer(FayeClient fc);
    public void onDisconnectedServer(FayeClient fc);
    public void onReceivedMessage(FayeClient fc, String msg);

}
