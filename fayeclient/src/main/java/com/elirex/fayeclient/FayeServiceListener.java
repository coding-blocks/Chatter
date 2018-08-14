package com.elirex.fayeclient;

/**
 * @author Sheng-Yuan Wang (2015/9/7).
 */
public interface FayeServiceListener {
    public void onMessageReceived(FayeClient fc, String msg);
    public void onConnectedToServer(FayeClient fc);
}
