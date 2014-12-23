package com.chairtrax.app;

public class WheelTracking {
	private boolean isDirectionForward = true;
	
	private Float mOrientationAngle;
	private Float mLastOrientationAngle;
	private float mAbsoluteOrientationAngle;
	
	private final float ALLOWED_PI_DEVIATION = 1.3f; 
	
	public float processRevs(float[] accelData) {
		mOrientationAngle = (float) Math.atan2(accelData[1], accelData[0]);
		
		isDirectionForward = findDirection();
		updateAbsoluteOrientationAngle();
		
		mLastOrientationAngle = mOrientationAngle;
		
		return mAbsoluteOrientationAngle;
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
