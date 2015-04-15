package com.chairtrax.app;

import android.util.Log;

public class WheelTracking {
	private boolean isDirectionForward = true;
	
	// current orientation angle and previous iteration's orientation angle
	private Double mOrientationAngle;
	private Double mLastOrientationAngle;
	
	// cumulative orientation angle
	private double mAbsoluteOrientationAngle;
	
	// flags to track the state
	private int mState = 0;
	private final int CALIBRATION = 0;
	private final int DATA_COLLECTION = 1;
	
	// calibration variables
	private int mInitCalibCounter;
	private double mAvgInitCalibAngle;
	private final int CALIBRATION_DATA_NUM = 3;
	
	private final double ALLOWED_PI_DEVIATION = 1.3f; 
	
	// starts off calibrating the stationary accelerometer values,
	// then switches to data collection state and stays there until
	// reset is hit
	public void processRevs(float[] accelData) {
		switch (mState) {
		case CALIBRATION:
			mInitCalibCounter++;
			mAvgInitCalibAngle += Math.atan2(accelData[1], accelData[0]);
			
			if (mInitCalibCounter == CALIBRATION_DATA_NUM) {
				mAvgInitCalibAngle /= CALIBRATION_DATA_NUM;
				mOrientationAngle = mAvgInitCalibAngle;
				
				mState++;
			} else {
				return;
			}
			break;
		case DATA_COLLECTION:
			mOrientationAngle = Math.atan2(accelData[1], accelData[0]);
			break;
		}
		
		isDirectionForward = findDirection();
		updateAbsoluteOrientationAngle();
		
		mLastOrientationAngle = mOrientationAngle;
	}
	
	public double getAbsoluteOrientationAngle() {
		return mAbsoluteOrientationAngle;
	}
	
	public void reset() {
		mState = 0;
		mInitCalibCounter = 0;
		mAvgInitCalibAngle = 0;
		
		mOrientationAngle = null;
		mLastOrientationAngle = null;
		mAbsoluteOrientationAngle = 0.f;
	}
	
	private void updateAbsoluteOrientationAngle() {
		if (mLastOrientationAngle != null)
			mAbsoluteOrientationAngle += mOrientationAngle - mLastOrientationAngle;
	}

	private boolean findDirection() {
		if (mLastOrientationAngle == null) return true;
		
		// look for wrap-arounds (going from 180 to -180 or -180 to 180)
		if (forwardWrapAround()) return true;
		else if (backwardWrapAround()) return false;
		else if (mOrientationAngle > mLastOrientationAngle) return true;
		else return false;
	}

	// from 180 to -180
	private boolean forwardWrapAround() {
		if (mLastOrientationAngle == null) return false;
		
		if ((mOrientationAngle < (-Math.PI + ALLOWED_PI_DEVIATION)) &&
			(mLastOrientationAngle > (Math.PI - ALLOWED_PI_DEVIATION))) {
			mAbsoluteOrientationAngle = (float) (mAbsoluteOrientationAngle + (2 * Math.PI));
			return true;
		}
		
		return false;
	}
	
	// from -180 to 180
	private boolean backwardWrapAround() {
		if (mLastOrientationAngle == null) return false;
		
		if ((mOrientationAngle > (Math.PI - ALLOWED_PI_DEVIATION)) &&
			(mLastOrientationAngle < (-Math.PI + ALLOWED_PI_DEVIATION))) {
			mAbsoluteOrientationAngle = (float) (mAbsoluteOrientationAngle - (2 * Math.PI));
			return true;
		}
		
		return false;
	}
}
