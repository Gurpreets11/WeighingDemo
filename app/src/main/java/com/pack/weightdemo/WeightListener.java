package com.pack.weightdemo;

public interface WeightListener {
    void onWeightReceived(String weight);
    void onConnectionSuccess();
    void onConnectionFailed(String reason);
    void onDisconnected();
}
