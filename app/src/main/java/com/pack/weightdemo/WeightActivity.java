package com.pack.weightdemo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Set;

public class WeightActivity extends AppCompatActivity {

    private Spinner deviceSpinner;
    private Button btnConnect;
    private TextView tvWeight;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter<String> deviceAdapter;
    private ArrayList<BluetoothDevice> deviceList;

    private BluetoothHelper bluetoothHelper;

    private final int PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight);

        deviceSpinner = findViewById(R.id.spinnerDevices);
        btnConnect = findViewById(R.id.btnConnect);
        tvWeight = findViewById(R.id.tvWeight);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothHelper = new BluetoothHelper(WeightActivity.this, new BluetoothHelper.Callback() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> Toast.makeText(WeightActivity.this, "Connected", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onConnectionFailed(String reason) {
                runOnUiThread(() -> Toast.makeText(WeightActivity.this, "Failed: " + reason, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> Toast.makeText(WeightActivity.this, "Disconnected", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onWeightReceived(String weight) {

                tvWeight.setText("Weight: " + weight + " kg");
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT}, PERMISSION_REQUEST_CODE);
        } else {
            populateDeviceList();
        }

        btnConnect.setOnClickListener(v -> {
            int selectedPosition = deviceSpinner.getSelectedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < deviceList.size()) {
                BluetoothDevice device = deviceList.get(selectedPosition);
                //bluetoothHelper.connectToDevice(device);


                if (device.getName() != null && (
                        device.getName().startsWith("ESSAE WS") ||
                                device.getName().toUpperCase().contains("ESSAE WS"))) {
                    // A bit more flexible but with controlled scope
                    Log.e("TAG","NAME MATCH ::"+ device.getName());
                    bluetoothHelper.connectToDevice(device);
                } else {
                    Toast.makeText(WeightActivity.this, "Select weighing machine to connect: Ex. ESSAE WS", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void populateDeviceList() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        deviceList = new ArrayList<>(bondedDevices);

        ArrayList<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : bondedDevices) {
            deviceNames.add(device.getName() + "\n" + device.getAddress());
            Log.e("TAG","NAME ::"+ device.getName());
            Log.e("TAG","Alias ::"+ device.getAlias());
        }

        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, deviceNames);
        deviceSpinner.setAdapter(deviceAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothHelper.stop();
    }

    // Permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            populateDeviceList();
        }
    }
}
