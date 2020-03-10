package com.example.wheelencoder;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button scanButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private boolean deviceListEmpty;
    private boolean isScanning = false;

    ListView deviceListView;
    private ArrayList<String> devicesArray;
    private ArrayList<BluetoothDevice> bluetoothDeviceList;
    ArrayAdapter<String> devListAdapter;
    public static final String EXTRA_MESSAGE = "com.example.wheelencoder.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        deviceListEmpty = true;
        devicesArray = new ArrayList<String>();
        bluetoothDeviceList = new ArrayList<BluetoothDevice>();
        devicesArray.add("No Devices Found");
        devListAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, devicesArray);

        deviceListView = (ListView) findViewById(R.id.deviceListView);
        deviceListView.setAdapter(devListAdapter);
        deviceListView.setOnItemClickListener(messageClickedHandler);

        peripheralTextView = (TextView) findViewById(R.id.deviceTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        scanButton = (Button) findViewById(R.id.bleScanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleBleScanning();
            }
        });

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();


        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }

    // Create a message handling object as an anonymous class.
    private AdapterView.OnItemClickListener messageClickedHandler = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position, long id) {
            // Do something in response to the click
            //String value = (String) deviceListView.getItemAtPosition(position);
            if (bluetoothDeviceList.size() > 0) {
                String value = (String) bluetoothDeviceList.get(position).getAddress();
                EditText editText = (EditText) findViewById(R.id.bdaddr_text);
                editText.setText(value);

                Button button = (Button) findViewById(R.id.button_connect);
                button.setEnabled(true);
            }

        }
    };
        /** Called when the user taps the Send button */
    public void showWheelStatus(View view) {
        Intent intent = new Intent(this, WheelStatusActivity.class);
        EditText editText = (EditText) findViewById(R.id.bdaddr_text);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void toggleBleScanning() {
        if (isScanning == false)
        {
            startScanning();
            scanButton.setText("Stop Scanning");
            isScanning = true;
        }
        else
        {
            stopScanning();
            scanButton.setText("Start Scanning");
            isScanning = false;
        }
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String name = result.getDevice().getName();
            String bdaddr = result.getDevice().getAddress();
            peripheralTextView.append("Device Name: " + name + " rssi: " + result.getRssi() + "\n");

            // auto scroll for text view
            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                peripheralTextView.scrollTo(0, scrollAmount);

            if (!bluetoothDeviceList.contains(result.getDevice()) && name != null) {
                if (name.contains("Wheel")) {
                    if (deviceListEmpty)
                        devListAdapter.clear();
                    devListAdapter.add(name);
                    bluetoothDeviceList.add(result.getDevice());
                    deviceListEmpty = false;
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        peripheralTextView.setText("");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }
}