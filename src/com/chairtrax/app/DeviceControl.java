package com.chairtrax.app;

import com.broadcom.util.SignalProcessingUtils;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class DeviceControl {
	
	private final static String TAG = DeviceControl.class.getSimpleName();
	
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    
    private Context mContext;
    
    public static int mNumDevicesConnected = 0;
	
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean mConnected = false;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothLeService1 mBluetoothLeService1;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    
	private float[] mRawAccelData = new float[3];
	private float[] mSmoothedAccelData = null;
	
	private WheelTracking mWheel = new WheelTracking();

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    
    public DeviceControl(Context context, Intent intent) {
    	mContext = context;
    	
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        
        if (mNumDevicesConnected == 0) {
        	Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
        	mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        	mNumDevicesConnected++;
        } else if (mNumDevicesConnected == 1) {
        	Intent gattServiceIntent = new Intent(mContext, BluetoothLeService1.class);
        	mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        	mNumDevicesConnected++;
        }
    }
    
    public String getDeviceAddress() {
    	return mDeviceAddress;
    }
    
    public String getDeviceName() {
    	return mDeviceName;
    }
    
    public WheelTracking getWheel() {
    	return mWheel;
    }
    
    public void resumeConnection(Context context) {
        context.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if ((mNumDevicesConnected == 1) && (mBluetoothLeService != null)) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        if ((mNumDevicesConnected > 1) && (mBluetoothLeService1 != null)) {
            final boolean result = mBluetoothLeService1.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }
    
    public void pauseConnection(Context context) {
    	context.unregisterReceiver(mGattUpdateReceiver);
    }
    
    public void destroyConnection(Context context) {
        context.unbindService(mServiceConnection);
        if (mNumDevicesConnected == 1) {
        	mBluetoothLeService = null;
        }
        if (mNumDevicesConnected > 1) {
        	mBluetoothLeService1 = null;
        }
    }
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	Log.e(TAG, "onServiceConnected Called");
        	if (mNumDevicesConnected == 1) {
	            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
	            if (!mBluetoothLeService.initialize()) {
	                Log.e(TAG, "Unable to initialize Bluetooth");
	//                finish();
	            }
	            // Automatically connects to the device upon successful start-up initialization.
	            mBluetoothLeService.connect(mDeviceAddress);
        	} 
        	if (mNumDevicesConnected > 1) {
	            mBluetoothLeService1 = ((BluetoothLeService1.LocalBinder) service).getService();
	            if (!mBluetoothLeService1.initialize()) {
	                Log.e(TAG, "Unable to initialize Bluetooth");
	//                finish();
	            }
	            // Automatically connects to the device upon successful start-up initialization.
	            mBluetoothLeService1.connect(mDeviceAddress);
        	}         	
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        	Log.e(TAG, "onServiceDisconnected Called");
            if (mNumDevicesConnected == 1) {
            	mBluetoothLeService = null;
            }
            if (mNumDevicesConnected > 1) {
            	mBluetoothLeService1 = null;
            }
        }
    };
    
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            String deviceAddress = extras.getString(BluetoothLeService.DEVICE_ADDRESS);

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                // displayGattServices(mBluetoothLeService.getSupportedGattServices());
            	Log.i(TAG, "onReceive() Services Discovered");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	//float[] data = intent.getFloatArrayExtra(BluetoothLeService.EXTRA_DATA);
            	//displayData(data[0] + " " + data[1] + " " + data[2]);
            	float[] rawData = extras.getFloatArray(BluetoothLeService.EXTRA_DATA);
            	if (rawData == null) return;
            	if (deviceAddress.equalsIgnoreCase(mDeviceAddress)) {
	            	mRawAccelData = rawData;
	            	mSmoothedAccelData = SignalProcessingUtils.lowPass(mRawAccelData, mSmoothedAccelData);
	        		mWheel.processRevs(mSmoothedAccelData);
            	}
            }
        }
    };
}
