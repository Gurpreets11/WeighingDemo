package com.pack.weightdemo;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;



public class BluetoothHelperOld {


    private static final String TAG = "BluetoothHelper";
    private static final String TARGET_NAME = "Essae";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final Activity activity;
    private final WeightListener callback;
    private final BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private boolean keepReading = false;

    public BluetoothHelperOld(Activity activity, WeightListener callback) {
        this.activity = activity;
        this.callback = callback;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void startConnection() {
        if (adapter == null || !adapter.isEnabled()) {
            callback.onConnectionFailed("Bluetooth not available or not enabled.");
            return;
        }

        // Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
                return;
            }
        }

        Set<BluetoothDevice> bonded = adapter.getBondedDevices();
        BluetoothDevice essaeDevice = null;
        for (BluetoothDevice device : bonded) {
            if (device.getName() != null && device.getName().toLowerCase().contains(TARGET_NAME.toLowerCase())) {
                essaeDevice = device;
                break;
            }
        }

        if (essaeDevice == null) {
            callback.onConnectionFailed("Essae device not paired.");
            return;
        }

        connectSocket(essaeDevice);
    }

    private void connectSocket(BluetoothDevice device) {
        // Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
                return;
            }
        }
        new Thread(() -> {
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                adapter.cancelDiscovery();
                socket.connect();
                callback.onConnectionSuccess();
                startReading(socket.getInputStream());
            } catch (Exception e) {
                Log.e(TAG, "Socket connection error: " + e.getMessage());
                callback.onConnectionFailed("Connection error: " + e.getMessage());
                retryAfterDelay();
            }
        }).start();
    }

    private void startReading(InputStream inStream) {
        keepReading = true;
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder sb = new StringBuilder();
            try {
                while (keepReading && (bytes = inStream.read(buffer)) > 0) {
                    String read = new String(buffer, 0, bytes);
                    sb.append(read);
                    if (read.contains("\n")) {
                        String[] lines = sb.toString().split("\n");
                        for (String line : lines) {
                            if (line.contains("kg")) {
                                String weight = line.replaceAll("[^0-9.]", "");
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    callback.onWeightReceived(weight);
                                });
                            }
                        }
                        sb.setLength(0);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Disconnected: " + e.getMessage());
                callback.onDisconnected();
                retryAfterDelay();
            }
        }).start();
    }

    public void stop() {
        keepReading = false;
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }
    }

    private void retryAfterDelay() {
        new Handler(Looper.getMainLooper()).postDelayed(this::startConnection, 5000);
    }
}


