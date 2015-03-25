package com.chairtrax.app;

public class WheelTracking {
	private boolean isDirectionForward = true;
	
	private Float mOrientationAngle;
	private Float mLastOrientationAngle;
	private float mAbsoluteOrientationAngle;
	
	private int mState = 0;
	private final int CALIBRATION = 0;
	private final int DATA_COLLECTION = 1;
	
	private int mInitCalibCounter;
	private float mAvgInitCalibAngle;
	private final int CALIBRATION_DATA_NUM = 3;
	
	private final float ALLOWED_PI_DEVIATION = 1.3f; 
	
	public void processRevs(float[] accelData) {
		switch (mState) {
		case CALIBRATION:
			mInitCalibCounter++;
			mAvgInitCalibAngle += (float) Math.atan2(accelData[1], accelData[0]);
			
			if (mInitCalibCounter == CALIBRATION_DATA_NUM) {
				mAvgInitCalibAngle /= CALIBRATION_DATA_NUM;
				mOrientationAngle = mAvgInitCalibAngle;
				
				mState++;
			} else {
				return;
			}
			break;
		case DATA_COLLECTION:
			mOrientationAngle = (float) Math.atan2(accelData[1], accelData[0]);
			break;
		}
		
		isDirectionForward = findDirection();
		updateAbsoluteOrientationAngle();
		
		mLastOrientationAngle = mOrientationAngle;
	}
	
	public float getAbsoluteOrientationAngle() {
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
		
		if (forwardWrapAround()) return true;
		else if (backwardWrapAround()) return false;
		else if (mOrientationAngle > mLastOrientationAngle) return true;
		else return false;
	}

	private boolean forwardWrapAround() {
		if (mLastOrientationAngle == null) return false;
		
		if ((mOrientationAngle < (-Math.PI + ALLOWED_PI_DEVIATION)) &&
			(mLastOrientationAngle > (Math.PI - ALLOWED_PI_DEVIATION))) {
			mAbsoluteOrientationAngle = (float) (mAbsoluteOrientationAngle + (2 * Math.PI));
			return true;
		}
		
		return false;
	}
	
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
