/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chairtrax.app;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.broadcom.util.SensorDataParser;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    private TextView mConnectionState;
    private TextView mDataField;
    private Button mWheelTrackingIntentButton;
    private Button mRightWheelScanningIntentButton;
    
    public static ArrayList<DeviceControl> mDevices = new ArrayList<DeviceControl>(); 

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDevices.add(new DeviceControl(this, intent));

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDevices.get(0).getDeviceAddress());
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mWheelTrackingIntentButton = (Button) findViewById(R.id.wheel_tracking_intent_button);
        mRightWheelScanningIntentButton = (Button) findViewById(R.id.right_wheel_scanning_intent_button);
        
        mWheelTrackingIntentButton.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			Intent intent = new Intent(getApplicationContext(), WheelTrackingActivity.class);
    			startActivity(intent);
    		};
	    });
        
        mRightWheelScanningIntentButton.setOnClickListener(new View.OnClickListener() {
    		public void onClick(View v) {
    			Intent intent = new Intent(getApplicationContext(), DeviceScanActivity.class);
    			startActivity(intent);
    		};
	    });

        getActionBar().setTitle(mDevices.get(0).getDeviceName());
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (DeviceControl device: mDevices) {
        	device.resumeConnection(this);
        }
        if (DeviceControl.mNumDevicesConnected >= 2) {
        	mRightWheelScanningIntentButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (DeviceControl device: mDevices) {
        	device.pauseConnection(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        for (DeviceControl device: mDevices) {
//        	device.destroyConnection(this);
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
 //       getMenuInflater().inflate(R.menu.gatt_services, menu);
 //       if (mConnected) {
 //           menu.findItem(R.id.menu_connect).setVisible(false);
 //           menu.findItem(R.id.menu_disconnect).setVisible(true);
 //       } else {
 //           menu.findItem(R.id.menu_connect).setVisible(true);
 //           menu.findItem(R.id.menu_disconnect).setVisible(false);
 //       }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch(item.getItemId()) {
//            case R.id.menu_connect:
//                mBluetoothLeService.connect(mDeviceAddress);
//                return true;
//            case R.id.menu_disconnect:
//                mBluetoothLeService.disconnect();
//                return true;
//            case android.R.id.home:
//                onBackPressed();
//                return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }
}
