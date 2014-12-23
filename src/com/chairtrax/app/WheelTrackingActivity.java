package com.chairtrax.app;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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
	
	public static String EXTRAS_DEVICE_CONTROL = "DEVICE_CONTROL";
	
	private TextView mDistanceTraveledTextView;
	private float mDistanceTraveled;
	
	private EditText mWheelRadiusEditText;
	private float mWheelRadius;
	
	private Timer mTimer = new Timer();
	private static final int TIME_CONSTANT = 30;
	
	private ArrayList<DeviceControl> mDevices = new ArrayList<DeviceControl>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wheel_tracking);
		
        //final Intent intent = getIntent();
        mDevices = DeviceControlActivity.mDevices; 
        		
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
		
        mTimer.scheduleAtFixedRate(new updateUI(), 1000, TIME_CONSTANT);
	}
	
    class updateUI extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	mDistanceTraveledTextView.setText(mDevices.get(1).mAbsoluteOrientationAngle * mWheelRadius + "");
                }
            });
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
    public void onResume() {
    	super.onResume();
        for (DeviceControl device: mDevices) {
        	device.resumeConnection(this);
        }
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
