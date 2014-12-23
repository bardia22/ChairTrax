package com.chairtrax.app;

import com.broadcom.util.SignalProcessingUtils;
import com.chairtrax.app.BluetoothLeService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

public class WheelTrackingActivity extends Activity {
	
	private TextView mDistanceTraveledTextView;
	private float mDistanceTraveled;
	
	private EditText mWheelRadiusEditText;
	private float mWheelRadius;
	
	private float[] mRawAccelData = new float[3];
	private float[] mSmoothedAccelData = null;
	
	private WheelTracking mWheel = new WheelTracking();

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
            	float[] rawData = intent.getFloatArrayExtra(BluetoothLeService.EXTRA_DATA);
            	if (rawData == null) return;
            	mRawAccelData = rawData;
            	mSmoothedAccelData = SignalProcessingUtils.lowPass(mRawAccelData, mSmoothedAccelData);
        		mDistanceTraveledTextView.setText(String.valueOf(mWheel.processRevs(mSmoothedAccelData) * mWheelRadius));
            }
        }
    };
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wheel_tracking);
		
		mDistanceTraveledTextView = (TextView) findViewById(R.id.distance_traveled);
		mDistanceTraveled = Float.parseFloat(mDistanceTraveledTextView.getText().toString());
		
		mWheelRadiusEditText = (EditText) findViewById(R.id.wheel_radius);
		mWheelRadius = Float.parseFloat(mWheelRadiusEditText.getText().toString());
		mWheelRadiusEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence currentDigits, int start,
                    int before, int count) {
            	mWheelRadius = Float.parseFloat(currentDigits.toString());
            }

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				// TODO Auto-generated method stub
			}
		});
	}
	
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }
    
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.wheel_tracking, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
