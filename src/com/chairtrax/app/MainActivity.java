/******************************************************************************
 *
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
package com.chairtrax.app;

import com.broadcom.app.ledevicepicker.DevicePicker;
import com.broadcom.app.ledevicepicker.DevicePickerActivity;
//import com.broadcom.app.license.LicenseUtils;
//import com.broadcom.app.license.LicenseDialog.OnLicenseAcceptListener;
import com.broadcom.util.Settings.SettingChangeListener;
//import com.broadcom.app.wicedsmart.ota.OtaAppInfo;
//import com.broadcom.app.wicedsmart.ota.ui.OtaAppInfoFragment;
//import com.broadcom.app.wicedsmart.ota.ui.OtaResource;
//import com.broadcom.app.wicedsmart.ota.ui.OtaUiHelper;
//import com.broadcom.app.wicedsmart.ota.ui.OtaUiHelper.OtaUiCallback;
import com.broadcom.util.BluetoothEnabler;
import com.broadcom.util.SenseDeviceState;
import com.broadcom.util.SenseManager;
import com.broadcom.util.SensorDataParser;
import com.broadcom.util.Settings;
//import com.broadcom.ui.ExitConfirmUtils;
//import com.broadcom.ui.ExitConfirmFragment.ExitConfirmCallback;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * Manaages the main view and gauges for each sensor
 *
 */
public class MainActivity extends Activity implements /*OnLicenseAcceptListener,*/
        DevicePicker.Callback, android.os.Handler.Callback, OnClickListener, /*ExitConfirmCallback,*/
        /*OtaUiCallback,*/ SettingChangeListener {
    private static final String TAG = Settings.TAG_PREFIX + "MainActivity";
    private static final boolean DBG_LIFECYCLE = true;
    private static final boolean DBG = Settings.DBG;

    private static final int COMPLETE_INIT = 800;
    private static final int PROCESS_BATTERY_STATUS_UI = 801;
    private static final int PROCESS_EVENT_DEVICE_UNSUPPORTED = 802;
    private static final int PROCESS_CONNECTION_STATE_CHANGE_UI = 803;
    private static final int PROCESS_SENSOR_DATA_ON_UI_0 = 804;
    private static final int PROCESS_SENSOR_DATA_ON_UI_1 = 805;
    
    private static int NUM_DEVICES = 0;

//    private static int getBatteryStatusIcon(int batteryLevel) {
//        if (batteryLevel <= 0) {
//            return R.drawable.battery_charge_background;
//        } else if (batteryLevel < 25) {
//            return R.drawable.battery_charge_25;
//        } else if (batteryLevel < 50) {
//            return R.drawable.battery_charge_50;
//        } else if (batteryLevel < 75) {
//            return R.drawable.battery_charge_75;
//        } else {
//            return R.drawable.battery_charge_full;
//        }
//
//    }

    /**
     * Handles Bluetooth on/off events. If Bluetooth is turned off, exit this
     * app
     *
     * @author Fred Chen
     *
     */
    private class BluetoothStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            mSensorDataEventHandler.post(new Runnable() {
                @Override
                public void run() {
                    int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    switch (btState) {

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        exitApp();
                        break;
                    }
                }
            });
        }
    }

    private class UiHandlerCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {

            // These events run on the mUiHandler on the UI Main Thread
            case COMPLETE_INIT:
                initResourcesAndResume();
                break;
            case PROCESS_EVENT_DEVICE_UNSUPPORTED:
                Toast.makeText(getApplicationContext(), R.string.error_unsupported_device,
                        Toast.LENGTH_SHORT).show();
                break;
            case PROCESS_CONNECTION_STATE_CHANGE_UI:
            	updateConnectionStateWidgets(mSenseManager.indexForSenseDeviceStates((SenseDeviceState) msg.obj));
                break;
//            case PROCESS_BATTERY_STATUS_UI:
//                updateBatteryLevelWidget(msg.arg1);
//                break;
            case PROCESS_SENSOR_DATA_ON_UI_0:
                mWheelTrackingFragment.onSensorData(0, SensorDataParser.processSensorData((byte[]) msg.obj));
                break;
            case PROCESS_SENSOR_DATA_ON_UI_1:
            	mWheelTrackingFragment.onSensorData(1, SensorDataParser.processSensorData((byte[]) msg.obj));
                break;
            }
            return true;
        }
    };

    private Button mLeftConnectDisconnect;
    private Button mRightConnectDisconnect;
    private Button mLeftScan;
    private Button mRightScan;
    
    private WheelTrackingFragment mWheelTrackingFragment;
    
//    private ImageView mBatteryStatusIcon;
//    private TextView mBatteryStatusText;
//    private View mBatteryStatusView;
    private DevicePicker mDevicePicker;
    private String mDevicePickerTitle;
//    private int mLastBatteryStatus = -1;
//    private boolean mConnectDisconnectPending;
    private SenseManager mSenseManager;
    private Handler mUiHandler;
    private final BluetoothStateReceiver mBtStateReceiver = new BluetoothStateReceiver();
//    private LicenseUtils mLicense;
//    private ExitConfirmUtils mExitConfirm;
    private int mInitState;
    private Handler mSensorDataEventHandler;
    private HandlerThread mSensorDataEventThread;
//    private final OtaUiHelper mOtaUiHelper = new OtaUiHelper();
//    private boolean mShowAppInfoDialog;
//    private boolean mFirmwareUpdateCheckPending;
//    private boolean mCanAskForFirmwareUpdate;
//    private boolean mIsTempScaleF = false;

    /**
     * Initialize async resources in series
     *
     * @return
     */
    private boolean initResourcesAndResume() {
        switch (mInitState) {
        case 0:
//            // Check if license accepted. If not, prompt user
//            if (!mLicense.checkLicenseAccepted(getFragmentManager())) {
//                return false;
//            }
//            mInitState++;
//        case 1:
            // Check if BT is on, If not, prompt user
            if (!BluetoothEnabler.checkBluetoothOn(this)) {
                return false;
            }
            mInitState++;
            SenseManager.init(this);
//        case 2:
        case 1:
            // Check if sense manager initialized. If not, keep waiting
            if (waitForSenseManager()) {
                return false;
            }
            mInitState = -1;
            //checkDevicePicked(0);
        }
        mSenseManager.registerEventCallbackHandler(mSensorDataEventHandler);

        if (mSenseManager.isConnectedAndAvailable(0)) {
            mSenseManager.enableNotifications(0, true);
        }
        
        if (mSenseManager.isConnectedAndAvailable(1)) {
            mSenseManager.enableNotifications(1, true);
        }

        updateConnectionStateWidgets(0);
        updateConnectionStateWidgets(1);
//        updateTemperatureScaleType();
        Settings.addChangeListener(this);
        return true;
    }

    /**
     * Acquire reference to the SenseManager serivce....This is asynchronous
     *
     * @return
     */
    private boolean waitForSenseManager() {
        // Check if the SenseManager is available. If not, keep retrying
        mSenseManager = SenseManager.getInstance();
        if (mSenseManager == null) {
            mUiHandler.sendEmptyMessageDelayed(COMPLETE_INIT, Settings.SERVICE_INIT_TIMEOUT_MS);
            return true;
        }
        return false;
    }

    /**
     * Exit the application and cleanup resources
     */
    protected void exitApp() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "exitApp");
        }
        SenseManager.destroy();
        finish();
    }

    /**
     * Update the battery level UI widgets
     *
     * @param batteryLevel
     */
//    private void updateBatteryLevelWidget(int batteryLevel) {
//        mLastBatteryStatus = batteryLevel;
//        invalidateOptionsMenu();
//    }

    /**
     * Update all UI components related to the connection state
     */
    private void updateConnectionStateWidgets(int index) {
//        mConnectDisconnectPending = false;
    	Button button;
    	if (index == 0)
    		button = mLeftConnectDisconnect;
    	else
    		button = mRightConnectDisconnect;
    	
        if (button != null) {
            if (mSenseManager.getDevice(index) == null) {
            	button.setEnabled(false);
            	button.setText(R.string.no_device);
                return;
            }
            if (!button.isEnabled()) {
            	button.setEnabled(true);
            }
            if (mSenseManager.isConnectedAndAvailable(index)) {
            	button.setText(R.string.disconnect);
            } else {
            	button.setText(R.string.connect);
            }
            button.setEnabled(true);
        }
        invalidateOptionsMenu();
    }

    /**
     * Initialize the license agreement dialog
     */
//    private void initLicenseUtils() {
//        mLicense = new LicenseUtils(this, this);
//    }

    /**
     * Initialize the exit confirmation dialog
     */
//    private void initExitConfirm() {
//        mExitConfirm = new ExitConfirmUtils(this);
//    }

    /*
     * Initialize the device picker
     *
     * @return
     */
    private void initDevicePicker() {
        mDevicePickerTitle = getString(R.string.title_devicepicker);
        mDevicePicker = new DevicePicker(this, Settings.PACKAGE_NAME,
                DevicePickerActivity.class.getName(), this,
                Uri.parse("content://com.brodcom.app.wicedsense/device/pick"));

        mDevicePicker.init();
    }

    /**
     * Launch the device picker
     */
    private void launchDevicePicker() {
        mDevicePicker.launch(mDevicePickerTitle, null, null);
    }

    /**
     * Cleanup the device picker
     */
    private void cleanupDevicePicker() {
        if (mDevicePicker != null) {
            mDevicePicker.cleanup();
            mDevicePicker = null;
        }
    }

    /**
     * Check if a device has been picked, and launch the device picker if not...
     *
     * @return
     */
    private boolean checkDevicePicked(int index) {
        if (mSenseManager != null && mSenseManager.getDevice(index) != null) {
            return true;
        }
        // Launch device picker
        launchDevicePicker();
        return false;
    }

    /**
     * Start the connect or disconnect, based on the current state of the device
     */
    private void doConnectDisconnect(int index) {
        if (!mSenseManager.isConnectedAndAvailable(index)) {
            if (!mSenseManager.connect(index)) {
                updateConnectionStateWidgets(index);
            }
        } else {
            if (!mSenseManager.disconnect(index)) {
                updateConnectionStateWidgets(index);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onCreate " + this);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        FragmentManager fragmentManager = getFragmentManager();
        mWheelTrackingFragment = (WheelTrackingFragment) fragmentManager.findFragmentById(R.id.wheel_tracking_fragment);
        
        mLeftConnectDisconnect = (Button) findViewById(R.id.connection_state_left);
        if (mLeftConnectDisconnect != null) {
            mLeftConnectDisconnect.setOnClickListener(this);
            mLeftConnectDisconnect.setEnabled(false);
        } else {
            // large screen sizes do not have button in the main layout. Instead
            // it's an action button in the menu button
        }
        
        mRightConnectDisconnect = (Button) findViewById(R.id.connection_state_right);
        if (mRightConnectDisconnect != null) {
            mRightConnectDisconnect.setOnClickListener(this);
            mRightConnectDisconnect.setEnabled(false);
        } else {
            // large screen sizes do not have button in the main layout. Instead
            // it's an action button in the menu button
        }
        
        mLeftScan = (Button) findViewById(R.id.scan_state_left);
        mLeftScan.setOnClickListener(this);
        
        mRightScan = (Button) findViewById(R.id.scan_state_right);
        mRightScan.setOnClickListener(this);

        // Initialize dialogs
        initDevicePicker();
//        initLicenseUtils();
//        initExitConfirm();

        mInitState = 0;

        // Register bluetooth state receiver
        registerReceiver(mBtStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Start event handler thread
        mSensorDataEventThread = new HandlerThread("WicedSenseEventHandlerThread");
        mSensorDataEventThread.start();
        mSensorDataEventHandler = new Handler(mSensorDataEventThread.getLooper(), this);

        // Start ui handler
        mUiHandler = new Handler(new UiHandlerCallback());
    }

    @Override
    protected void onResume() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onResume " + this);
        }
        super.onResume();
        initResourcesAndResume();
    }

    @Override
    protected void onPause() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onPause " + this);
        }
//        mLicense.dismiss();
//        mExitConfirm.dismiss();

        Settings.removeChangeListener(this);

        // Disable notifications if the application is backgrounded, but don't
        // disconnect from the device
//        if (mSenseManager != null) {
//            if (mSenseManager.isConnectedAndAvailable(0)) {
//                mSenseManager.enableNotifications(0, false);
//            }
//            if (mSenseManager.isConnectedAndAvailable(1)) {
//                mSenseManager.enableNotifications(1, false);
//            }
//            mSenseManager.unregisterEventCallbackHandler(mSensorDataEventHandler);
//        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onDestroy " + this);
        }

        mSensorDataEventThread.quit();
        cleanupDevicePicker();
        unregisterReceiver(mBtStateReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
//        MenuItem item = menu.findItem(R.id.action_battery_status);
//        if (item == null) {
//            return true;
//        }

        // ActionViews must have an explicit onClickListener registered
//        mBatteryStatusView = item.getActionView();
//        mBatteryStatusIcon = (ImageView) mBatteryStatusView.findViewById(R.id.battery_status_icon);
//        mBatteryStatusText = (TextView) mBatteryStatusView.findViewById(R.id.battery_status);
//        mBatteryStatusView.setOnClickListener(this);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        boolean isDeviceSelected = (mSenseManager != null && mSenseManager.getDevice(0) != null);
        boolean isDeviceConnected = isDeviceSelected && mSenseManager.isConnectedAndAvailable(0);

        // Check if we are in landscape mode. If so, update the state of the
        // connect/disconnect action
//        if (mButtonConnectDisconnect == null) {
//            // Get Connect/disconnect button
//            MenuItem menuConnectDisconnect = menu.findItem(R.id.action_connectdisconnect);
//            if (menuConnectDisconnect != null) {
//                // Landscape mode
//                if (!isDeviceSelected) {
//                    // No device selected: hide connect/disconnect button
//                    // menuConnectDisconnect.setVisible(false);
//                    menuConnectDisconnect.setEnabled(false);
//
//                } else {
//                    menuConnectDisconnect.setEnabled(!mConnectDisconnectPending);
//                    if (isDeviceConnected) {
//                        menuConnectDisconnect.setTitle(R.string.disconnect);
//                    } else {
//                        menuConnectDisconnect.setTitle(R.string.connect);
//                    }
//                }
//            }
//        }
        // Update the battery icon
//        if (mBatteryStatusView != null && mBatteryStatusIcon != null && mBatteryStatusText != null) {
//            int batteryStatus = mLastBatteryStatus;
//            if (!isDeviceConnected) {
//                mBatteryStatusIcon.setImageResource(getBatteryStatusIcon(-1));
//                mBatteryStatusText.setText(getString(R.string.battery_status, "??"));
//            } else {
//                mBatteryStatusView.setEnabled(true);
//                mBatteryStatusIcon.setImageResource(getBatteryStatusIcon(batteryStatus));
//                mBatteryStatusText.setText(getString(R.string.battery_status, batteryStatus < 0 ? 0
//                        : batteryStatus));
//            }
//        }

        // Update the update firmware button
//        MenuItem updateFw = menu.findItem(R.id.update_fw);
//        if (updateFw != null) {
//            updateFw.setEnabled(isDeviceSelected);
//        }

        // Update the get firmware info button
//        MenuItem getFwInfo = menu.findItem(R.id.get_fw_info);
//        if (getFwInfo != null) {
//            getFwInfo.setEnabled(isDeviceConnected);
//        }

        // Update the pick device menu: only allow a pick device from the
        // disconnected state
//        MenuItem pick = menu.findItem(R.id.action_pick);
//        if (pick != null) {
//            pick.setEnabled(!isDeviceConnected);
//        }

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Invoked when a menu option is picked
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//        case R.id.action_connectdisconnect:
//            mConnectDisconnectPending = true;
//            invalidateOptionsMenu();
//            doConnectDisconnect();
//            return true;
//        case R.id.action_pick:
//            launchDevicePicker();
//            return true;
//        case R.id.update_fw:
//            checkForFirmwareUpdate();
//            return true;
//        case R.id.get_fw_info:
//            getFirmwareInfo();
//            return true;
//        case R.id.action_settings:
//            // Launch setttings menu
//            Intent i = new Intent(this, SettingsActivity.class);
//            startActivity(i);
//            return true;
        }
        return false;
    }

    /**
     * Callback invoked when the user finishes with the license agreement dialog
     */
//    @Override
//    public void onLicenseAccepted(boolean accepted) {
//        if (!accepted) {
//            exitApp();
//            return;
//        }
//        mLicense.setAccepted(true);
//        initResourcesAndResume();
//    }

    /**
     * Callback invoked when a device is selected from the device picker
     */
    @Override
    public void onDevicePicked(BluetoothDevice device) {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onDevicePicked");
        }
//        if (Settings.CHECK_FOR_UPDATES_ON_CONNECT) {
//            mCanAskForFirmwareUpdate = true;
//        } else {
//            mCanAskForFirmwareUpdate = false;
//        }
        
        mSenseManager.setDevice(NUM_DEVICES, device);
        updateConnectionStateWidgets(NUM_DEVICES);
    }

    /**
     * Callback invoked when the user aborts picking a device from the device
     * picker
     */
    @Override
    public void onDevicePickCancelled() {
        if (DBG_LIFECYCLE) {
            Log.d(TAG, "onDevicePickCancelled");
        }
        updateConnectionStateWidgets(0);
    }

    /**
     * Handler callback used for two purposes
     *
     * 1. This callback is invoked by the event handler loop when the
     * SenseManager service sends a event from the sensor tag using the
     * mEventHandler object. The event handler loop runs in a child thread, so
     * that it can queue up events and allow the SenseManager (and Bluetooth
     * callbacks) to return asynchronously before the UI processes the event.
     * The event handler loop reposts the event to the main UI handler loop via
     * the mUiHandler Handler
     *
     * 2. This callback is invoked by the mEventHandler object to run a UI
     * operation in the main event loop of the application
     *
     *
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case SenseManager.EVENT_DEVICE_UNSUPPORTED:
            mUiHandler.sendEmptyMessage(PROCESS_EVENT_DEVICE_UNSUPPORTED);
            break;
        case SenseManager.EVENT_CONNECTED:
            mUiHandler.sendMessage(mUiHandler.obtainMessage(PROCESS_CONNECTION_STATE_CHANGE_UI, msg.obj));
            onConnected(mSenseManager.indexForSenseDeviceStates((SenseDeviceState) msg.obj));
            break;
        case SenseManager.EVENT_DISCONNECTED:
        	mUiHandler.sendMessage(mUiHandler.obtainMessage(PROCESS_CONNECTION_STATE_CHANGE_UI, msg.obj));
            break;
        case SenseManager.EVENT_BATTERY_STATUS:
            mUiHandler.sendMessage(mUiHandler.obtainMessage(PROCESS_BATTERY_STATUS_UI, msg.arg1,
                    msg.arg1));
            break;
        case SenseManager.EVENT_SENSOR_DATA_0:
            mUiHandler.sendMessage(mUiHandler.obtainMessage(PROCESS_SENSOR_DATA_ON_UI_0, msg.obj));
            break;
        case SenseManager.EVENT_SENSOR_DATA_1:
            mUiHandler.sendMessage(mUiHandler.obtainMessage(PROCESS_SENSOR_DATA_ON_UI_1, msg.obj));
            break;
//        case SenseManager.EVENT_APP_INFO:
//            boolean success = msg.arg1 == 1;
//            OtaAppInfo appInfo = (OtaAppInfo) msg.obj;
//            if (DBG) {
//                Log.d(TAG, "EVENT_APP_INFO: success=" + success + ",otaAppInfo=" + appInfo);
//            }
//            if (mFirmwareUpdateCheckPending) {
//                mFirmwareUpdateCheckPending = false;
//                checkForFirmwareUpdate(appInfo);
//                break;
//            }
//
//            if (mShowAppInfoDialog) {
//                mShowAppInfoDialog = false;
//                if (success) {
//                    OtaAppInfoFragment mOtaAppInfoFragment = OtaAppInfoFragment.createDialog(
//                            mSenseManager.getDevice(), appInfo);
//                    mOtaAppInfoFragment.show(getFragmentManager(), null);
//                }
//            }
//            break;
        }
        return true;
    }

    /**
     * Callback invoked when the connect/disconnect button is clicked or the
     * battery status button is clicked
     */
    @Override
    public void onClick(View v) {

        // Process connect/disconnect request
        if (v == mLeftConnectDisconnect) {
            // Temporary disable the button while a connect/disconnect is
            // pending
//            mConnectDisconnectPending = true;
            mLeftConnectDisconnect.setEnabled(false);
            doConnectDisconnect(0);
        }
        
        // Process connect/disconnect request
        if (v == mRightConnectDisconnect) {
            // Temporary disable the button while a connect/disconnect is
            // pending
//            mConnectDisconnectPending = true;
            mRightConnectDisconnect.setEnabled(false);
            doConnectDisconnect(1);
        }
        
        if (v == mLeftScan) {
        	NUM_DEVICES = 0;
        	checkDevicePicked(0);
        }
        
        if (v == mRightScan) {
        	NUM_DEVICES = 1;
        	checkDevicePicked(1);
        }

        // Process battery status request
//        else if (v == mBatteryStatusView) {
//            if (mSenseManager != null) {
//                mSenseManager.getBatteryStatus();
//            }
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothEnabler.REQUEST_ENABLE_BT) {
            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                exitApp();
                return;
            }
            initResourcesAndResume();
        }
    }

    /**
     * Show exit confirmation dialog if user presses the button
     */
    @Override
    public void onBackPressed() {
//        mExitConfirm.show(getFragmentManager());
    }

    /**
     * Callback invoked when the user selects "ok" from the exit confirmation
     * dialog
     */
//    @Override
//    public void onExit() {
//        exitApp();
//    }

    /**
     * Callback invoked when the user cancels exitting the application.
     */
//    @Override
//    public void onExitCancelled() {
//    }

    /**
     * Callback invoked when the OTA firmware update has completed
     *
     * @param completed
     *            : if true, OTA upgrade was successful, false otherwise.
     */
//    @Override
//    public void onOtaFinished(boolean completed) {
//        if (DBG_LIFECYCLE) {
//            Log.d(TAG, "onOtaFinished");
//        }
//        if (mSenseManager != null) {
//            mSenseManager.setOtaUpdateMode(false);
//        }
//    }

    /**
     * Start a request to read application id, major version,minor version of a
     * connected WICED Sense tag...
     */
//    private void getFirmwareInfo() {
//        if (mSenseManager != null) {
//            mShowAppInfoDialog = true;
//            mSenseManager.getAppInfo();
//        }
//    }

    /**
     * Check for firmware update, if the user selects the option from the menu
     */
//    private void checkForFirmwareUpdate() {
//        if (mSenseManager == null) {
//            return;
//        }
//        // Check if we are connected
//        if (!mSenseManager.isConnectedAndAvailable()) {
//            mCanAskForFirmwareUpdate = true;
//            boolean success = mSenseManager.connect();
//            if (!success) {
//                mCanAskForFirmwareUpdate = false;
//            }
//        } else {
//            mCanAskForFirmwareUpdate = true;
//            checkForFirmwareUpdateIfAllowed();
//        }
//    }

    /**
     * Check for firmware update, if the user allows it. The connection is
     * assumed to be up
     *
     * @return
     */
//    private boolean checkForFirmwareUpdateIfAllowed() {
//        if (!mCanAskForFirmwareUpdate) {
//            if (DBG) {
//                Log.d(TAG, "firmwareUpdateCheck(): user opted out...skipping..");
//            }
//            return false;
//        }
//        mFirmwareUpdateCheckPending = true;
//        if (!mSenseManager.getAppInfo()) {
//            mFirmwareUpdateCheckPending = false;
//            if (DBG) {
//                Log.d(TAG, "checkForFirmwareUpdates(): unable to get app info");
//            }
//            return false;
//        }
//        if (DBG) {
//            Log.d(TAG, "firmwareUpdateCheck(): getting app info");
//        }
//        return true;
//    }

    private void onConnected(int index) {
//        if (checkForFirmwareUpdateIfAllowed()) {
//            // Wait for firmware check...
//            if (DBG) {
//                Log.d(TAG, "onConnected:Checking for firmware updates..");
//            }
//        } else {
            if (DBG) {
                Log.d(TAG, "onConnected: enabling notifications");
            }
            if (mSenseManager != null) {
                mSenseManager.enableNotifications(index, true);
                mWheelTrackingFragment.createWheelTracking(index);
            }
//        }
    }

//    private boolean canUpdateToFirmware(OtaAppInfo appInfo, OtaResource otaResource) {
//        if (otaResource == null || appInfo == null) {
//            return false;
//        }
//        if (otaResource.getMajor() <= 0) {
//            return true;
//        }
//        if (appInfo.mMajorVersion < otaResource.getMajor()) {
//            return true;
//        } else if (appInfo.mMajorVersion == otaResource.getMajor()
//                && appInfo.mMinorVersion < otaResource.getMinor()) {
//            return true;
//        }
//        return false;
//    }
//
//    private void checkForFirmwareUpdate(OtaAppInfo appInfo) {
//        mCanAskForFirmwareUpdate = false;
//
//        ArrayList<OtaResource> otaResources = new ArrayList<OtaResource>();
//        OtaResource defaultResource = Settings.getDefaultOtaResource();
//        if (defaultResource != null) {
//            otaResources.add(defaultResource);
//        }
//        OtaUiHelper.createOtaResources(Settings.getOtaDirectory(), Settings.getOtaFileFilter(),
//                otaResources);
//        Iterator<OtaResource> i = otaResources.iterator();
//        while (i.hasNext()) {
//            OtaResource otaResource = i.next();
//            if (!canUpdateToFirmware(appInfo, otaResource)) {
//                if (DBG) {
//                    Log.d(TAG, "Skipping OTA firmware " + otaResource.getName());
//                }
//                i.remove();
//            }
//        }
//        if (otaResources.size() > 0) {
//            mSenseManager.setOtaUpdateMode(true);
//            mOtaUiHelper.startUpdate(getApplicationContext(), mSenseManager.getDevice(),
//                    mSenseManager.getGattManager(), getFragmentManager(), otaResources, this, true);
//        } else {
//            mSenseManager.setOtaUpdateMode(false);
//            mSenseManager.enableNotifications(true);
//        }
//    }
//
    @Override
    public void onSettingsChanged(String settingName) {
//        if (Settings.SETTINGS_KEY_TEMPERATURE_SCALE_TYPE.equals(settingName)) {
//            updateTemperatureScaleType();
//        }
//
    }
//
//    /**
//     * Update the temperature gauge by dynamically replacing the gauge with the
//     * correct temperature type scale
//     */
//    private void updateTemperatureScaleType() {
//        // Get the old temperatureType
//        String tempScaleType = Settings.getTemperatureeScaleType();
//        mIsTempScaleF = Settings.TEMPERATURE_SCALE_TYPE_F.equals(tempScaleType);
//
//        // Check if this is a new temperature fragment
//        if (mTemperatureFrag == null) {
//            TemperatureFragment f = new TemperatureFragment();
//            f.setScaleType(mIsTempScaleF ? TemperatureFragment.SCALE_F
//                    : TemperatureFragment.SCALE_C);
//            FragmentTransaction ft = getFragmentManager().beginTransaction();
//            ft.replace(R.id.fragment_temp, f, FRAGMENT_TEMP);
//            ft.commit();
//            mTemperatureFrag = f;
//            mAnimationSlower.addAnimated(f);
//            return;
//        }
//
//        // This is a refresh. Check if the temp scale has changed
//        boolean isLastScaleF = TemperatureFragment.SCALE_F == mTemperatureFrag.getScaleType();
//        if (mIsTempScaleF == isLastScaleF) {
//            // No change. exit
//            return;
//        }
//
//        float lastTempValue = mTemperatureFrag.getLastValue();
//        TemperatureFragment f = new TemperatureFragment();
//        f.setScaleType(mIsTempScaleF ? TemperatureFragment.SCALE_F : TemperatureFragment.SCALE_C);
//        FragmentTransaction ft = getFragmentManager().beginTransaction();
//        ft.replace(R.id.fragment_temp, f, FRAGMENT_TEMP);
//        if (mIsTempScaleF) {
//            // Convert last temp C to F
//            f.setInitialValue(SensorDataParser.tempCtoF(lastTempValue));
//        } else {
//            // Convert last temp F to C
//            f.setInitialValue(SensorDataParser.tempFtoC(lastTempValue));
//        }
//        ft.commit();
//        mAnimationSlower.removeAnimated(mTemperatureFrag);
//        mAnimationSlower.addAnimated(f);
//
//        mTemperatureFrag = f;
//    }

}
