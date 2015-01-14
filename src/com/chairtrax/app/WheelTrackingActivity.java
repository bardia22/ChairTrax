package com.chairtrax.app;

import java.text.DecimalFormat;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class WheelTrackingActivity extends Activity {
	
	public static String EXTRAS_DEVICE_CONTROL = "DEVICE_CONTROL";
	
	private static DecimalFormat mFormatter = new DecimalFormat("0.00");
	private static String mDegreeSymbol = "\u00b0";
	
	private TextView mLeftWheelDistanceTraveledTextView;
	private double mLeftWheelDistanceTraveled;
	
	private TextView mRightWheelDistanceTraveledTextView;
	private double mRightWheelDistanceTraveled;
	
	private TextView mHeadingTextView;
	private float mHeading;
	
	private EditText mWheelRadiusEditText;
	private float mWheelRadius;
	
	private EditText mAxleLengthEditText;
	private float mAxleLength;
	
	private Button mResetButton;
	
	private Timer mTimer = new Timer();
	private static final int TIME_CONSTANT = 30;
	
	private ArrayList<DeviceControl> mDevices = new ArrayList<DeviceControl>();
	private WheelTracking mLeftWheel = null;
	private WheelTracking mRightWheel = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wheel_tracking);
		
        mDevices = DeviceControlActivity.mDevices; 
        mLeftWheel = mDevices.get(0).getWheel();
        if (mDevices.size() > 1) mRightWheel = mDevices.get(1).getWheel();
        		
		mLeftWheelDistanceTraveledTextView = (TextView) findViewById(R.id.left_wheel_distance_traveled);
		mLeftWheelDistanceTraveled = Float.parseFloat(mLeftWheelDistanceTraveledTextView.getText().toString());
		
		mRightWheelDistanceTraveledTextView = (TextView) findViewById(R.id.right_wheel_distance_traveled);
		mRightWheelDistanceTraveled = Float.parseFloat(mRightWheelDistanceTraveledTextView.getText().toString());
		
		mHeadingTextView = (TextView) findViewById(R.id.heading);
		mHeading = Float.parseFloat(mHeadingTextView.getText().toString());
		
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
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}
		});
		
		mAxleLengthEditText = (EditText) findViewById(R.id.axle_length);
		mAxleLength = Float.parseFloat(mAxleLengthEditText.getText().toString());
		mAxleLengthEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence currentDigits, int start, int before, int count) {
            	mAxleLength = Float.parseFloat(currentDigits.toString());
            }

			@Override
			public void afterTextChanged(Editable arg0) {
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}
		});
		
		mResetButton = (Button) findViewById(R.id.reset_button);
		mResetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (mLeftWheel != null) mLeftWheel.reset();
				if (mRightWheel != null) mRightWheel.reset();
			}
		});
		
        mTimer.scheduleAtFixedRate(new updateUI(), 1000, TIME_CONSTANT);
	}
	
    class updateUI extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	mLeftWheelDistanceTraveled = (-1) * mLeftWheel.getAbsoluteOrientationAngle() * mWheelRadius;
                	mLeftWheelDistanceTraveledTextView.setText(" " + mFormatter.format(mLeftWheelDistanceTraveled) + " m");
                	if (mRightWheel != null) {
                		mRightWheelDistanceTraveled = mRightWheel.getAbsoluteOrientationAngle() * mWheelRadius;
                		mRightWheelDistanceTraveledTextView.setText(" " + mFormatter.format(mRightWheelDistanceTraveled) + " m");
                		
                		mHeading = findHeading();
                		mHeadingTextView.setText(" " + mFormatter.format(mHeading) + mDegreeSymbol);
                	}
                }
            });
        }
    }
    
    private float findHeading() {
    	double theta = (double) ((mRightWheelDistanceTraveled - mLeftWheelDistanceTraveled) / mAxleLength);
    	int quotient = (int) (theta / (2*Math.PI));
    	
    	if (quotient < 0) quotient--;
    	quotient = (-1) * quotient;
    	
    	return (float) (((theta + 2*Math.PI*quotient) / (2*Math.PI)) * 360); 
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
