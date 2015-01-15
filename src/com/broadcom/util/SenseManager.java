/******************************************************************************
 *
 *  Copyright (C) 2014 Broadcom Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/
package com.broadcom.util;

import java.util.ArrayList;
//import com.broadcom.app.wicedsmart.ota.OtaAppInfo;
//import com.broadcom.app.wicedsmart.ota.OtaAppInfoReader;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * Manages WICED Sense devices. Currently, this application only manages one
 * WICED Sense device at a time. But this can be expanded to support more than
 * one device connection
 *
 * @author fredc
 *
 */
public class SenseManager extends Service implements SenseDeviceState.EventCallback {
    private static final String TAG = Settings.TAG_PREFIX + "SenseManager";
    private static final boolean DBG = Settings.DBG;

    public static final int EVENT_CONNECTED = 10;
    public static final int EVENT_DISCONNECTED = 11;
    public static final int EVENT_DEVICE_UNSUPPORTED = 12;
    public static final int EVENT_SENSOR_DATA_0 = 50;
    public static final int EVENT_SENSOR_DATA_1 = 51;
    public static final int EVENT_BATTERY_STATUS = 60;
    public static final int EVENT_APP_INFO = 70;

    private static SenseManager sService;
    private static boolean initialized = false; 

    public static synchronized SenseManager getInstance() {
        return sService;
    }

    public static synchronized void init(Context ctx) {
    	if (initialized) return;
    	
        Context appCtx = ctx.getApplicationContext();
        Settings.init(appCtx);
        Intent i = new Intent(appCtx, SenseManager.class);
        appCtx.startService(i);
        initialized = true;
    }

    public static synchronized void destroy() {
        SenseManager s = SenseManager.getInstance();
        if (s != null && s.mIsStarted) {
            for (int i = 0; i < mDeviceStates.length; i++) {
            	s.stop(i);
            }
        }
        Settings.finish();
    }

    private boolean mIsStarted;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private static SenseDeviceState[] mDeviceStates = new SenseDeviceState[2];
    private final ArrayList<Handler> mEventCallbackHandlers = new ArrayList<Handler>();
    private boolean mIsOtaUpdateMode;
    private HandlerThread mHandlerThread;

    @Override
    public void onCreate() {
        if (DBG) {
            Log.d(TAG, "onCreate()");
        }
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothAdapter == null || mBluetoothManager == null) {
            Log.w(TAG, "Bluetooth not available!!!!");
        }
        synchronized (SenseManager.class) {
            sService = this;
        }
        mHandlerThread = new HandlerThread("SenseManagerHandlerThread");
        mHandlerThread.start();
    }

    @Override
    public synchronized void onDestroy() {
        if (DBG) {
            Log.d(TAG, "onDestroy()");
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        mIsStarted = true;
        return super.onStartCommand(intent, flags, startId);
    }

    public void onApplicationMinimized(boolean minimized) {

    }

    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    public synchronized void stop(int index) {
        if (DBG) {
            Log.d(TAG, "stop()");
        }
        mIsStarted = false;
        GattRequestManager gattManager = (getSenseDeviceState(index) == null) ? null : mDeviceStates[index]
                .getGattManager();
        if (gattManager != null) {
            gattManager.disconnect(true);
        }
        mEventCallbackHandlers.clear();
        super.stopSelf();
        synchronized (SenseManager.class) {
            sService = null;
        }
    }

    public void registerEventCallbackHandler(Handler callback) {
        if (mEventCallbackHandlers.contains(callback)) {
            Log.w(TAG, "registerEventCallbackHandler: callback already registered");
            return;
        }
        mEventCallbackHandlers.add(callback);
    }

    public void unregisterEventCallbackHandler(Handler callback) {
        mEventCallbackHandlers.remove(callback);
    }

    public synchronized void setDevice(int index, BluetoothDevice device) {
        GattRequestManager gattManager = (getSenseDeviceState(index) == null) ? null : mDeviceStates
                [index].getGattManager();
        if (gattManager != null) {
            gattManager.disconnect(true);
        }

        SenseDeviceState deviceState = new SenseDeviceState(this, device, mHandlerThread.getLooper(), this, index);
        mDeviceStates[index] = deviceState;
        
        if (Settings.CONNECT_AFTER_DEVICE_PICK) {
            connect(index);
        }
    }

    public synchronized SenseDeviceState getDeviceState(int index) {
        return (getSenseDeviceState(index) == null) ? null : mDeviceStates[index];
    }

    public BluetoothDevice getDevice(int index) {
        return (getSenseDeviceState(index) == null) ? null : mDeviceStates[index].getDevice();
    }

    public GattRequestManager getGattManager(int index) {
        return (getSenseDeviceState(index) == null) ? null : mDeviceStates[index].getGattManager();
    }

    public boolean connect(int index) {
        if (DBG) {
            Log.d(TAG, "connect(): mGattState=" + mDeviceStates);
        }
        if (getSenseDeviceState(index) == null) {
            return false;
        }
        if (!mDeviceStates[index].pairIfNeeded()) {
            mDeviceStates[index].getGattManager().connect();
        }
        return true;
    }

    public boolean disconnect(int index) {
        GattRequestManager gattManager = (getSenseDeviceState(index) == null) ? null : mDeviceStates
                [index].getGattManager();
        if (gattManager != null) {
            return gattManager.disconnect(true);
        } else {
            return false;
        }
    }

    public boolean isConnectedAndAvailable(int index) {
        return (getSenseDeviceState(index) != null) && mDeviceStates[index].isConnectedAndAvailable();
    }

//    public boolean getAppInfo() {
//        if (mDeviceState == null) {
//            return false;
//        }
//        GattRequestManager gattManager = null;
//        OtaAppInfoReader reader = null;
//        if (mDeviceState != null) {
//            gattManager = mDeviceState.getGattManager();
//            reader = mDeviceState.getAppInfoReader();
//        }
//        if (gattManager == null || reader == null) {
//            return false;
//        }
//        gattManager.addCallback(reader);
//        boolean success = reader.read();
//        if (!success) {
//            gattManager.removeCallback(reader);
//        }
//        return success;
//    }

//    public void getBatteryStatus() {
//        if (isConnectedAndAvailable()) {
//            mDeviceStates.getBatteryStatus(false);
//        }
//    }

    public void enableNotifications(int index, boolean enable) {
        if (DBG) {
            Log.d(TAG, "enableNotifications: enable= " + enable + ", mGattState= " + mDeviceStates
                    + ", mIsOtaUpdateMode=" + mIsOtaUpdateMode);
        }
        if (!isConnectedAndAvailable(index)) {
            Log.w(TAG, "enableNotifications: not connected or available...");
            return;
        }

        if (enable && mIsOtaUpdateMode) {
            Log.w(TAG,
                    "enableNotifications: is currently in OtaUpdateMode Ignoring enable request...");
            return;
        }

        mDeviceStates[index].enableNotifications(enable);
    }

    /**
     * Unregister for events and unregister for callbacks
     *
     * @param isOtaUpdateMode
     */
//    public void setOtaUpdateMode(boolean isOtaUpdateMode) {
//        mIsOtaUpdateMode = isOtaUpdateMode;
//        if (isOtaUpdateMode) {
//            enableNotifications(false);
//        } else {
//            GattRequestManager gattManager = mDeviceStates == null ? null : mDeviceStates
//                    .getGattManager();
//            if (gattManager != null) {
//                gattManager.addCallback(mDeviceStates);
//            }
//            enableNotifications(true);
//        }
//    }

    private void sendEvent(int eventType, SenseDeviceState state) {
        @SuppressWarnings("unchecked")
        ArrayList<Handler> eventCallbacks = (ArrayList<Handler>) mEventCallbackHandlers.clone();
        int sz = eventCallbacks == null ? 0 : eventCallbacks.size();
        for (int i = 0; i < sz; i++) {
            Handler cb = eventCallbacks.get(i);
            if (cb != null) {
                try {
                    Message event = cb.obtainMessage(eventType, state);
                    cb.sendMessage(event);
                } catch (Throwable t) {
                    Log.w(TAG, "sendEvent error, callback #" + i, t);
                }
            }
        }
    }

    @Override
    public void onConnected(SenseDeviceState deviceState) {
        sendEvent(EVENT_CONNECTED, deviceState);
    }

    @Override
    public void onDisconnected(SenseDeviceState deviceState) {
        sendEvent(EVENT_DISCONNECTED, deviceState);
    }

    @Override
    public void onUnsupportedDevice(SenseDeviceState deviceState) {
        sendEvent(EVENT_DEVICE_UNSUPPORTED, deviceState);
    }

    @Override
    public void onBatteryStatus(SenseDeviceState deviceState, int batteryLevel) {
        @SuppressWarnings("unchecked")
        ArrayList<Handler> eventCallbacks = (ArrayList<Handler>) mEventCallbackHandlers.clone();
        int sz = eventCallbacks == null ? 0 : eventCallbacks.size();
        for (int i = 0; i < sz; i++) {
            Handler cb = eventCallbacks.get(i);
            if (cb != null) {
                try {
                    Message event = cb.obtainMessage(EVENT_BATTERY_STATUS, batteryLevel,
                            batteryLevel, deviceState);
                    cb.sendMessage(event);
                } catch (Throwable t) {
                    Log.w(TAG, "onBatteryStatus error, callback #" + i, t);
                }
            }
        }
    }

    @Override
    public void onSensorData(SenseDeviceState deviceState, int index, byte[] sensorData) {
        @SuppressWarnings("unchecked")
        ArrayList<Handler> eventCallbacks = (ArrayList<Handler>) mEventCallbackHandlers.clone();
        int sz = eventCallbacks == null ? 0 : eventCallbacks.size();
        for (int i = 0; i < sz; i++) {
            Handler cb = eventCallbacks.get(i);
            if (cb != null) {
                try {
                	if (index == 0) {
                		Message event = cb.obtainMessage(EVENT_SENSOR_DATA_0, sensorData);
                		cb.sendMessage(event);
                	} else if (index == 1) {
                		Message event = cb.obtainMessage(EVENT_SENSOR_DATA_1, sensorData);
                		cb.sendMessage(event);
                	}
                } catch (Throwable t) {
                    Log.w(TAG, "onSensorData error, callback #" + i, t);
                }
            }
        }
    }

//    @Override
//    public void onAppInfoRead(SenseDeviceState deviceState, boolean success, OtaAppInfo info) {
//        @SuppressWarnings("unchecked")
//        ArrayList<Handler> eventCallbacks = (ArrayList<Handler>) mEventCallbackHandlers.clone();
//        int sz = eventCallbacks == null ? 0 : eventCallbacks.size();
//        for (int i = 0; i < sz; i++) {
//            Handler cb = eventCallbacks.get(i);
//            if (cb != null) {
//                try {
//                    Message event = cb.obtainMessage(EVENT_APP_INFO, success ? 1 : 0, 0, info);
//                    cb.sendMessage(event);
//                } catch (Throwable t) {
//                    Log.w(TAG, "onAppInfoRead error, callback #" + i, t);
//                }
//            }
//        }
//    }
    
    private SenseDeviceState getSenseDeviceState(int index) {
    	if ((mDeviceStates == null) ||
    		(mDeviceStates[index] == null))
    		return null;
    	return mDeviceStates[index];
    }
    
    public int indexForSenseDeviceStates(SenseDeviceState deviceState) {
    	return deviceState.index;
    }

    public int senseDeviceStatesSize() {
    	return (mDeviceStates == null) ? 0 : mDeviceStates.length; 
    }
}
