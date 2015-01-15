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
package com.chairtrax.app;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

import com.broadcom.util.SignalProcessingUtils;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class WheelTrackingFragment extends Fragment {
	
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
	
	private WheelTracking mLeftWheel = null;
	private WheelTracking mRightWheel = null;
	
	private float[] mLeftSmoothedAccelData = null;
	private float[] mRightSmoothedAccelData = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wheel_tracking_fragment, null);
        return v;
	}

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        		
		mLeftWheelDistanceTraveledTextView = (TextView) view.findViewById(R.id.left_wheel_distance_traveled);
		mLeftWheelDistanceTraveled = Float.parseFloat(mLeftWheelDistanceTraveledTextView.getText().toString());
		
		mRightWheelDistanceTraveledTextView = (TextView) view.findViewById(R.id.right_wheel_distance_traveled);
		mRightWheelDistanceTraveled = Float.parseFloat(mRightWheelDistanceTraveledTextView.getText().toString());
		
		mHeadingTextView = (TextView) view.findViewById(R.id.heading);
		mHeading = Float.parseFloat(mHeadingTextView.getText().toString());
		
		mWheelRadiusEditText = (EditText) view.findViewById(R.id.wheel_radius);
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
		
		mAxleLengthEditText = (EditText) view.findViewById(R.id.axle_length);
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
		
		mResetButton = (Button) view.findViewById(R.id.reset_button);
		mResetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (mLeftWheel != null) mLeftWheel.reset();
				if (mRightWheel != null) mRightWheel.reset();
			}
		});
    }
    
    public void createWheelTracking(int index) {
    	switch(index) {
    	case 0:
    		mLeftWheel = new WheelTracking();
    		break;
    	case 1:
    		mRightWheel = new WheelTracking();
    		break;
    	}
    		
    	if ((mLeftWheel != null) && (mRightWheel != null)) 
    		mTimer.scheduleAtFixedRate(new updateUI(), 1000, TIME_CONSTANT);
    }
    
    public void onSensorData(int index, float[] sensorData) {
    	
    	switch(index) {
    	case 0:
    		if (mLeftWheel == null) return;
    		
        	mLeftSmoothedAccelData = SignalProcessingUtils.lowPass(sensorData, mLeftSmoothedAccelData);
    		mLeftWheel.processRevs(mLeftSmoothedAccelData);
    		break;
    	case 1:
    		if (mRightWheel == null) return;
    		
        	mRightSmoothedAccelData = SignalProcessingUtils.lowPass(sensorData, mRightSmoothedAccelData);
    		mRightWheel.processRevs(mRightSmoothedAccelData);
    		break;
    	}
    }
    
    class updateUI extends TimerTask {
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
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

}
