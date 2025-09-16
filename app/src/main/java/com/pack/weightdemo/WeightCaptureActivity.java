package com.pack.weightdemo;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class WeightCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1001;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice selectedDevice;
    private TextView tvWeight;
    private Spinner deviceSpinner;
    private Button btnConnect;

     private BluetoothHelperOld bluetoothHelperOld;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_weight_capture);
        /*ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });*/


        tvWeight = findViewById(R.id.tvWeight);
        /*deviceSpinner = findViewById(R.id.deviceSpinner);
        btnConnect = findViewById(R.id.btnConnect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        checkBluetoothPermissions();

        btnConnect.setOnClickListener(v -> connectToSelectedDevice());*/
        bluetoothHelperOld = new BluetoothHelperOld(this, new WeightListener() {
            @Override
            public void onWeightReceived(String weight) {
                tvWeight.setText("Weight: " + weight + " kg");
            }

            @Override
            public void onConnectionSuccess() {
                Toast.makeText(WeightCaptureActivity.this, "Essae connected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onConnectionFailed(String reason) {
                Toast.makeText(WeightCaptureActivity.this, "Connection failed: " + reason, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected() {
                Toast.makeText(WeightCaptureActivity.this, "Disconnected, retrying...", Toast.LENGTH_SHORT).show();
            }
        });

        //bluetoothHelper.start();

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, bluetoothHelper.getPermissionRequestCode());
        } else {
            Log.e("TAG",      "Permission already granted");
            bluetoothHelper.startConnection(); // Safe to call now
        }*/

        bluetoothHelperOld.startConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothHelperOld.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            bluetoothHelperOld.startConnection();
        }
    }


    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                }, REQUEST_BLUETOOTH_PERMISSIONS);
            } else {
                listPairedDevices();
            }
        } else {
            listPairedDevices(); // No runtime permissions needed below Android 12
        }
    }

    private void listPairedDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        for (BluetoothDevice device : pairedDevices) {
            adapter.add(device.getName() + "\n" + device.getAddress());
        }

        deviceSpinner.setAdapter(adapter);
    }


    private void connectToSelectedDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return;
        }
        int selectedIndex = deviceSpinner.getSelectedItemPosition();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        selectedDevice = (BluetoothDevice) pairedDevices.toArray()[selectedIndex];

        new Thread(() -> {
            try {
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SPP UUID
                bluetoothSocket = selectedDevice.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();

                runOnUiThread(() -> Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show());

                InputStream inputStream = bluetoothSocket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    Log.e("TAG","line :: "+ line);
                    if (line.contains("kg")) {
                        String weight = line.replaceAll("[^0-9.]", "");
                        runOnUiThread(() -> tvWeight.setText("Weight: " + weight + " kg"));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Connection Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                listPairedDevices();
            } else {
                Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }*/



}