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

import com.broadcom.util.MathUtils;
import com.broadcom.util.SignalProcessingUtils;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
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
	
	private TextView mXTextView;	
	private TextView mYTextView;
	private TextView mVelocityTextView;
	
	private double mOldUIShownTotalDistanceTraveled;
	private double mTotalDistanceTraveled;
	private DataPoint mOldPoint;
	private DataPoint mNewPoint;
	
	private TextView mHeadingTextView;
	private float mHeading;
	
	private EditText mWheelRadiusEditText;
	private float mWheelRadius;
	
	private EditText mAxleLengthEditText;
	private float mAxleLength;
	
	private Button mResetButton;
	
	private GraphView mGraph;
	private PointsGraphSeries<DataPoint> mSeries;
	private static final int initialXAxis = 5;
	private static final int initialYAxis = 5;
	private int xAxis = initialXAxis;
	private int yAxis = initialYAxis;
	private int minDataX;
	private int maxDataX;
	private int minDataY;
	private int maxDataY;
	private LimitedSizeQueue queue = new LimitedSizeQueue(20);
	
	private Timer mUIUpdateTimer = new Timer();
	private static final int UI_UPDATE_TIME_CONSTANT = 750;
	
	private Timer mPositionUpdateTimer = new Timer();
	private static final int POSITION_UPDATE_TIME_CONSTANT = 100;
	
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
		
		mXTextView = (TextView) view.findViewById(R.id.x);
		mYTextView = (TextView) view.findViewById(R.id.y);
		mVelocityTextView = (TextView) view.findViewById(R.id.velocity);
		
		mHeadingTextView = (TextView) view.findViewById(R.id.heading);
		mHeading = Float.parseFloat(mHeadingTextView.getText().toString());
		
		mWheelRadiusEditText = (EditText) view.findViewById(R.id.wheel_radius);
		mWheelRadiusEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
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
		mAxleLengthEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
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
				mTotalDistanceTraveled = 0;
				mHeading = 0;
				mOldPoint = null;
				queue.flush();
				resetGraph();
			}
		});
		
		mGraph = (GraphView) view.findViewById(R.id.graph);
		resetGraph();
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
    		
    	if ((mLeftWheel != null) && (mRightWheel != null)) { 
    		mUIUpdateTimer.scheduleAtFixedRate(new updateUI(), 1000, UI_UPDATE_TIME_CONSTANT);
    		mPositionUpdateTimer.scheduleAtFixedRate(new updatePosition(), 50, POSITION_UPDATE_TIME_CONSTANT);
    	}
    }
    
    public void onSensorData(int index, float[] sensorData) {
    	
    	switch(index) {
    	case 0:
    		if (mLeftWheel == null) return;
    		
        	//mLeftSmoothedAccelData = SignalProcessingUtils.lowPass(sensorData, mLeftSmoothedAccelData);
    		mLeftWheel.processRevs(sensorData);
    		mHeading = findHeading();
    		break;
    	case 1:
    		if (mRightWheel == null) return;
    		
        	//mRightSmoothedAccelData = SignalProcessingUtils.lowPass(sensorData, mRightSmoothedAccelData);
    		mRightWheel.processRevs(sensorData);
    		mHeading = findHeading();
    		break;
    	}
    }
    
    class updatePosition extends TimerTask {
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	if ((mLeftWheel == null) || (mRightWheel == null))
                		return;
                	
                	if (mOldPoint == null) mOldPoint = new DataPoint(0, 0);
                	
                	mLeftWheelDistanceTraveled = (-1) * mLeftWheel.getAbsoluteOrientationAngle() * mWheelRadius;
                	mRightWheelDistanceTraveled = mRightWheel.getAbsoluteOrientationAngle() * mWheelRadius;
                	double newDistance = (mLeftWheelDistanceTraveled + mRightWheelDistanceTraveled) / 2;
                	
                	mNewPoint = moveChair(newDistance, mHeading, mTotalDistanceTraveled, mOldPoint);
                	mTotalDistanceTraveled = newDistance;
                	mOldPoint = mNewPoint;
                }
            });
        }
    }
    
    class updateUI extends TimerTask {
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	if (mLeftWheel != null) {
                		mLeftWheelDistanceTraveledTextView.setText(" " + mFormatter.format(mLeftWheelDistanceTraveled) + " m");
                	
                		if (mRightWheel != null) {
                			mRightWheelDistanceTraveledTextView.setText(" " + mFormatter.format(mRightWheelDistanceTraveled) + " m");
                			
                			mHeadingTextView.setText(" " + mFormatter.format(mHeading) + mDegreeSymbol);
                			
                			mVelocityTextView.setText(mFormatter.format(100 * 1000 * (mTotalDistanceTraveled - mOldUIShownTotalDistanceTraveled)/UI_UPDATE_TIME_CONSTANT) + " cm/s");
                			mOldUIShownTotalDistanceTraveled = mTotalDistanceTraveled;
                			queue.add(mNewPoint);
                			
                			mXTextView.setText(mFormatter.format(mNewPoint.getX()) + " m");
                			mYTextView.setText(mFormatter.format(mNewPoint.getY()) + " m");
                			
                			mGraph.removeAllSeries();
                			mSeries = new PointsGraphSeries<DataPoint>(queue.getAllData());
                			mGraph.addSeries(mSeries);
                			autoScale(mNewPoint.getX(), mNewPoint.getY());
                		}
                	}
                }
            });
        }
    }
    
    private DataPoint moveChair(double newDistance, float heading, double lastDistance, DataPoint lastPoint) {
    	// associate logical X-Y frame with rotated one on the graph
    	double lastX = lastPoint.getY(); double lastY = -lastPoint.getX();
    	
    	double deltaL = newDistance - lastDistance;
    	//Log.e("dfdfd", newDistance + " " + lastDistance + " " + heading + " " + lastX + " " + lastY);
    	double newX = lastX + deltaL * Math.cos(MathUtils.degreesToRadians(heading));
    	double newY = lastY + deltaL * Math.sin(MathUtils.degreesToRadians(heading));
    	
    	// rotate 90deg CCW
    	return new DataPoint(-newY, newX);
    }
    
    private float findHeading() {
    	double theta = (double) ((mRightWheelDistanceTraveled - mLeftWheelDistanceTraveled) / mAxleLength);
    	int quotient = (int) (theta / (2*Math.PI));
    	
    	if (theta < 0) quotient--;
    	quotient = (-1) * quotient;
    	
    	return (float) (((theta + 2*Math.PI*quotient) / (2*Math.PI)) * 360); 
    }
    
    private void resetGraph() {
    	queue.flush();
    	mGraph.removeAllSeries();
		mGraph.getViewport().setScalable(true);
		mGraph.getViewport().setXAxisBoundsManual(true);
		mGraph.getViewport().setYAxisBoundsManual(true);
		maxDataX = 0;
		minDataX = 0;
		maxDataY = 0;
		minDataY = 0;
		setAxesMinMax(initialXAxis, initialYAxis);
    }
    
    private void autoScale(double x, double y) {
    	if (x > maxDataX) maxDataX = (int) (x + 1);
    	if (x < minDataX) minDataX = (int) (x - 1);
    	if (y > maxDataY) maxDataY = (int) (y + 1);
    	if (y < minDataY) minDataY = (int) (y - 1);
    	
    	//Log.e("fjsfjs", "maxdataX " + maxDataX + " minDataX " + minDataX + " maxDataY " + maxDataY + " minDataY " + minDataY);
    	//Log.e("fdfd", "xAxis " + xAxis + " yAxis " + yAxis);
    	
    	if (maxDataX > xAxis) setAxesMinMax(2*xAxis, 2*yAxis);
    	if (minDataX < ((-1) * xAxis)) setAxesMinMax(2*xAxis, 2*yAxis);
    	if (maxDataY > yAxis) setAxesMinMax(2*xAxis, 2*yAxis);
    	if (minDataY < ((-1) * yAxis)) setAxesMinMax(2*xAxis, 2*yAxis);
    	
    	//if ((maxDataX < (xAxis/2)) && (xAxis >= (2*initialXAxis))) setAxesMinMax(xAxis/2, yAxis/2);
    	//if ((minDataX > ((-1) * xAxis/2)) && (xAxis >= (2*initialXAxis))) setAxesMinMax(xAxis/2, yAxis/2);
    	//if ((maxDataY < (yAxis/2)) && (yAxis >= (2*initialYAxis))) setAxesMinMax(xAxis/2, yAxis/2);
    	//if ((minDataY > ((-1) * yAxis/2)) && (yAxis >= (2*initialYAxis))) setAxesMinMax(xAxis/2, yAxis/2);
    }
    
    private void setAxesMinMax(int xMax, int yMax) {
		mGraph.getViewport().setMinX((-1) * xMax);
		mGraph.getViewport().setMaxX(xMax);
		xAxis = xMax;
		
		mGraph.getViewport().setMinY((-1) * yMax);
		mGraph.getViewport().setMaxY(yMax);
		yAxis = yMax;
    }
    
    private class LimitedSizeQueue {

        private int maxSize;
        private int currentIndex = 0;
        private DataPoint[] array;

        public LimitedSizeQueue(int size) {
            this.maxSize = size;
            array = new DataPoint[maxSize];
        }

        public void add(DataPoint data) {
        	array[currentIndex % maxSize] = data;
        	currentIndex++;
        }
        
        public DataPoint getOldest() {
        	return array[currentIndex % maxSize];
        }
        
        public DataPoint getNewest() {
        	return array[(currentIndex + maxSize - 1) % maxSize];
        }
        
        public DataPoint[] getAllData() {
        	if (array[currentIndex % maxSize] == null) {
        		DataPoint[] cpyArray = new DataPoint[currentIndex % maxSize];
        		System.arraycopy(array, 0, cpyArray, 0, currentIndex);
        		return cpyArray;
        	}
        	return array;
        }
        
        public void flush() {
        	currentIndex = 0;
        	array = new DataPoint[maxSize];
        }
    }

}
