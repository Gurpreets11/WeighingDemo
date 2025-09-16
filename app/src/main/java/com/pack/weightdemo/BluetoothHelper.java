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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.core.app.ActivityCompat;

public class BluetoothHelper {
    public interface Callback {
        void onConnected();
        void onConnectionFailed(String reason);
        void onDisconnected();
        void onWeightReceived(String weight);
    }

    private BluetoothSocket socket;
    private BluetoothSocket  connectedSocket;
    private boolean reading = false;
    private final Callback callback;

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final Activity activity;
    public BluetoothHelper(Activity activity,Callback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void connectToDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
                return;
            }
        }

        new Thread(() -> {
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                socket.connect();
                this.connectedSocket = socket;
                callback.onConnected();
                startReading(socket.getInputStream());
            } catch (Exception e) {
                Log.e("BT", "Connect failed: " + e.getMessage());
                callback.onConnectionFailed(e.getMessage());
            }
        }).start();
    }

    private void startReading(InputStream inputStream) {
        reading = true;
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder sb = new StringBuilder();

            List<String> lastReadings = new ArrayList<>();
            String lastSentWeight = "";

            try {
                while (reading && (bytes = inputStream.read(buffer)) > 0) {
                    String read = new String(buffer, 0, bytes);
                    //Log.e("TAGG","read :"+ read);
                    sb.append(read);
                    if (read.contains("\n")) {
                        String[] lines = sb.toString().split("\n");
                        for (String line : lines) {
                            line = line.trim();

                            // Remove control characters like STX (ASCII 2)
                            line = line.replaceAll("[^0-9. kg]", "");
                            //Log.e("TAGG","line :"+ line);
                            if (line.toLowerCase().contains("kg")) {
                                String weight = line.replaceAll("[^0-9.]", "");
                                //Log.e("TAGG","weight :"+ weight);
                                // Skip invalid or 0.000 readings
                                if (weight.isEmpty() || weight.equals("0.000")) continue;
                                // Add to buffer
                                lastReadings.add(weight);
                                if (lastReadings.size() > 5)
                                    lastReadings.remove(0);

                                // Check for stability
                                /*if (lastReadings.size() == 3 &&
                                        lastReadings.get(0).equals(lastReadings.get(1)) &&
                                        lastReadings.get(1).equals(lastReadings.get(2))) {

                                    if (!weight.equals(lastSentWeight)) {
                                        lastSentWeight = weight;

                                        String finalWeight = weight;
                                        new Handler(Looper.getMainLooper()).post(() ->
                                                callback.onWeightReceived(finalWeight));
                                    }
                                }*/


                                /*if (lastReadings.size() == 5 && allEqual(lastReadings)) {
                                    if (!weight.equals(lastSentWeight)) {
                                        lastSentWeight = weight;

                                        String finalWeight = weight;
                                        Log.e("TAGG","finalWeight :"+ finalWeight);
                                        new Handler(Looper.getMainLooper()).post(() ->
                                                callback.onWeightReceived(finalWeight));
                                    }
                                }*/

                                new Handler(Looper.getMainLooper()).post(() ->
                                        callback.onWeightReceived(weight));

                            }
                        }
                        sb.setLength(0);
                    }
                }
            } catch (Exception e) {
                callback.onDisconnected();
            }
        }).start();
    }


    private boolean allEqual(List<String> readings) {
        if (readings == null || readings.size() < 2) return false;
        String first = readings.get(0);
        for (String r : readings) {
            if (!r.equals(first)) return false;
        }
        return true;
    }

    public void stop() {
        reading = false;
        try {
            if (socket != null) socket.close();
            this.connectedSocket = null;
        } catch (Exception ignored) {
        }
    }

    public boolean isDeviceConnected(){
        boolean isDeviceConnected ;
        if (connectedSocket != null && connectedSocket.isConnected()) {
            // Read from input stream
            isDeviceConnected= true;
        } else {
            //showToast("Weighing scale not connected!");
            isDeviceConnected= false;
        }
        return isDeviceConnected;
    }
}

