package com.example.wheelencoder;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.UUID;

public class WheelStatusActivity extends AppCompatActivity {

    private final static String TAG = WheelStatusActivity.class.getSimpleName();

    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_WHEEL_COUNT_SERVICE =
            UUID.fromString("162524BA-544F-4639-8A46-30BC444833D7");
    public final static UUID UUID_CPR_CHAR =
            UUID.fromString("162524BB-544F-4639-8A46-30BC444833D7");
    public final static UUID UUID_WHEEL_COUNT_CHAR =
            UUID.fromString("162524BC-544F-4639-8A46-30BC444833D7");
    public final static UUID UUID_DIAMETER_CHAR =
            UUID.fromString("162524BD-544F-4639-8A46-30BC444833D7");

    //CCCD is BT SIG defined. Where is this constant in Android?
    public final static UUID UUID_BT_CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    BluetoothGattCharacteristic wheelCountCharacteristic = null;
    BluetoothGattCharacteristic cprCharacteristic = null;
    BluetoothGattCharacteristic diameterCharacteristic = null;

    int displayCPR = 30;
    int displayCount = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wheel_status);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.status_textView);
        textView.setText(message);

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        BluetoothDevice btDevice = btAdapter.getRemoteDevice(message);

        btDevice.connectGatt(this, false, gattCallback);
    }

    private int mod(int x, int y)
    {
        int result = x % y;
        return result < 0 ? result + y : result;
    }

    public void updateCountDisplay(int count) {
        displayCount = mod(count, displayCPR);
        updateChart();
    }

    public void updateCPRDisplay(int CPR) {
        displayCPR = CPR;
        displayCount = mod(displayCount, displayCPR);
        updateChart();
    }

    public void updateChart(){
        // Update the text in a center of the chart:
        TextView numberOfCals = findViewById(R.id.wheel_count_progress);
        numberOfCals.setText(String.valueOf(displayCount) + " / " + displayCPR);

        // Calculate the slice size and update the pie chart:
        ProgressBar pieChart = findViewById(R.id.stats_progressbar);
        double d = (double) displayCount / (double) displayCPR;
        int progress = (int) (d * 100);
        pieChart.setProgress(progress);
    }

    /** Called when the user taps the read button */
    public void readWheelCount(View view) {
        Log.i(TAG, "Read button pressed");
        if ((connectionState == STATE_CONNECTED) && (wheelCountCharacteristic != null))
        {
            bluetoothGatt.readCharacteristic(wheelCountCharacteristic);
        }
    }

    /** Called when the user taps the read button */
    public void readCPR(View view) {
        Log.i(TAG, "Read CPR button pressed");
        if ((connectionState == STATE_CONNECTED) && (cprCharacteristic != null))
        {
            bluetoothGatt.readCharacteristic(cprCharacteristic);
        }
    }
    /** Called when the user taps the write button */
    public void writeCPR(View view) {
        Log.i(TAG, "Read CPR button pressed");
        if ((connectionState == STATE_CONNECTED) && (cprCharacteristic != null))
        {
            EditText cprEditText = findViewById((R.id.cpr_value));
            String temp = cprEditText.getText().toString();
            int value=0;
            if (!temp.isEmpty()) {
            value=Integer.parseInt(temp);
            }
            cprCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            bluetoothGatt.writeCharacteristic(cprCharacteristic);
        }
    }

    /** Called when the user taps the read diameter button */
    public void readWheelDiameter(View view) {
        Log.i(TAG, "Read diam button pressed");
        if ((connectionState == STATE_CONNECTED) && (diameterCharacteristic != null))
        {
            bluetoothGatt.readCharacteristic(diameterCharacteristic);
        }
    }

    /**Called when user taps the write diameter button*/
    public void writeWheelDiameter(View view) {
        Log.i(TAG, "write wheel diameter");
        if ((connectionState == STATE_CONNECTED) && (diameterCharacteristic != null))
        {
            EditText diamEditText = findViewById((R.id.diam_value));
            String temp = diamEditText.getText().toString();
            int value=0;
            if (!temp.isEmpty()) {
                value=Integer.parseInt(temp);
            }
            diameterCharacteristic.setValue(value, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            bluetoothGatt.writeCharacteristic(diameterCharacteristic);
        }

    }

    /** Called when the user taps the read button */
    public void enableWheelCountNotify(View view) {
        Log.i(TAG, "Enabled button pressed.");
        if (wheelCountCharacteristic != null) {
            boolean enabled = true;
            bluetoothGatt.setCharacteristicNotification(wheelCountCharacteristic, enabled);
            BluetoothGattDescriptor descriptor = wheelCountCharacteristic.getDescriptor(UUID_BT_CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        connectionState = STATE_CONNECTED;

                        TextView textView = findViewById(R.id.status_textView);
                        textView.setText("Connected");

                        //broadcastUpdate(intentAction);
                        Log.i(TAG, "Connected to GATT server.");
                        //Log.i(TAG, "Attempting to start service discovery:" +
                        //        bluetoothGatt.discoverServices());
                        bluetoothGatt = gatt;
                        gatt.discoverServices();

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        connectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        //broadcastUpdate(intentAction);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.i(TAG, "Services discovered." + status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                    BluetoothGattService service = gatt.getService(UUID_WHEEL_COUNT_SERVICE);
                    wheelCountCharacteristic = service.getCharacteristic(UUID_WHEEL_COUNT_CHAR);
                    cprCharacteristic = service.getCharacteristic(UUID_CPR_CHAR);
                    diameterCharacteristic = service.getCharacteristic(UUID_DIAMETER_CHAR);

                    TextView textView = findViewById(R.id.status_textView);
                    textView.setText("Found Wheel Counter Service");


                    if (wheelCountCharacteristic != null) {
                        boolean enabled = true;
                        bluetoothGatt.setCharacteristicNotification(wheelCountCharacteristic, enabled);
                        BluetoothGattDescriptor descriptor = wheelCountCharacteristic.getDescriptor(UUID_BT_CLIENT_CHARACTERISTIC_CONFIG);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(descriptor);
                    }

                    if (cprCharacteristic != null) {
                        textView = (TextView)findViewById(R.id.cpr_value);
                        String value = cprCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString();
                        textView.setText(value);
                    }

                    if (diameterCharacteristic != null)
                        bluetoothGatt.readCharacteristic(diameterCharacteristic);

                    if (wheelCountCharacteristic != null)
                        bluetoothGatt.readCharacteristic(wheelCountCharacteristic);
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Log.i(TAG, "Char Read.");
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                        if (characteristic.getUuid().equals(UUID_WHEEL_COUNT_CHAR))
                        {
                            TextView textView = findViewById(R.id.counter_value);
                            String value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0).toString();
                            textView.setText(value);
                            updateCountDisplay(Integer.parseInt(value));
                        }
                        else if (characteristic.getUuid().equals(UUID_CPR_CHAR))
                        {
                            TextView textView = findViewById(R.id.cpr_value);
                            String value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0).toString();
                            textView.setText(value);
                            updateCPRDisplay(Integer.parseInt(value));
                        }
                        else if (characteristic.getUuid().equals(UUID_DIAMETER_CHAR))
                        {
                            TextView textView = findViewById(R.id.diam_value);
                            String value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0).toString();
                            textView.setText(value);
                        }
                    }
                }

                    @Override
                    // Characteristic notification
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        Log.i(TAG, "Char changed.");
                    if (characteristic.getUuid().equals(UUID_WHEEL_COUNT_CHAR))
                    {
                        TextView textView = findViewById(R.id.counter_value);
                        String value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT32, 0).toString();
                        textView.setText(value);

                        updateCountDisplay(Integer.parseInt(value));
                    }
                    // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                }
            };
}
