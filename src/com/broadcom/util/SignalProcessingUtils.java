package com.broadcom.util;

/**
 * Helper math class
 *
 *
 */
public class SignalProcessingUtils {

	/*
	 * time smoothing constant for low-pass filter
	 * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
	 * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
	 */
	private static final float ALPHA = 0.80f;
	 
	/**
	 * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
	 * @see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	 */
	public static double lowPass(double newData, Double oldData) {
		if (oldData == null) return newData;
		
	    oldData = oldData + ALPHA * (newData - oldData);
	    return oldData;
	}
}