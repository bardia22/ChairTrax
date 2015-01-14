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
package com.broadcom.util;

import android.util.Log;


/**
 * Helper class to parse the sensor data packets
 *
 * @author fredc
 *
 */
public class SensorDataParser {
    public static final String TAG = "SensorDataParser";

    public static final int SENSOR_FLAG_ACCEL = (0x1 << 0);
    public static final int SENSOR_FLAG_GYRO = (0x1 << 1);
    public static final int SENSOR_FLAG_HUMIDITY = (0x1 << 2);
    public static final int SENSOR_FLAG_MAGNO = (0x1 << 3);
    public static final int SENSOR_FLAG_PRESSURE = (0x1 << 4);
    public static final int SENSOR_FLAG_TEMP = (0x1 << 5);

    // not currently in use - but should be set so that the match what the
    // sensor produces - currently wrong
    public static final int SENSOR_ACCEL_MIN = -90;
    public static final int SENSOR_ACCEL_MAX = 90;

    // not currently in use - but should be set so that the match what the
    // sensor produces - currently wrong
    public static final int SENSOR_GYRO_MIN = -327;
    public static final int SENSOR_GYRO_MAX = 327;

    public static final int SENSOR_HUMIDITY_MIN = 0;
    public static final int SENSOR_HUMIDITY_MAX = 100;

    // not currently in use - but should be set so that the match what the
    // sensor produces - currently wrong
    public static final int SENSOR_MAGNO_MIN = -180;
    public static final int SENSOR_MAGNO_MAX = 180;

    public static final int SENSOR_PRESSURE_MIN = 800;
    public static final int SENSOR_PRESSURE_MAX = 1200;

    public static final float SENSOR_TEMP_MIN_F = -40;
    public static final float SENSOR_TEMP_MAX_F = 120;

    public static final float SENSOR_TEMP_MIN_C = -40;
    public static final float SENSOR_TEMP_MAX_C = 60;

    public static final int SENSOR_TEMP_DATA_SIZE = 2;
    public static final int SENSOR_PRES_DATA_SIZE = 2;
    public static final int SENSOR_HUMD_DATA_SIZE = 2;

    public static final int SENSOR_ACCEL_DATA_SIZE = 6;
    public static final int SENSOR_MAGNO_DATA_SIZE = 6;
    public static final int SENSOR_GYRO_DATA_SIZE = 6;

    private static int getTwoByteValue(byte[] bytes, int offset) {
        return (bytes[offset + 1] << 8) + (bytes[offset] & 0xFF);
    }

    public static boolean accelerometerHasChanged(int mask) {
        return (SENSOR_FLAG_ACCEL & mask) > 0;
    }

    public static boolean gyroHasChanged(int mask) {
        return (SENSOR_FLAG_GYRO & mask) > 0;
    }

    public static boolean magnetometerHasChanged(int mask) {
        return (SENSOR_FLAG_MAGNO & mask) > 0;
    }

    public static boolean humidityHasChanged(int mask) {
        return (SENSOR_FLAG_HUMIDITY & mask) > 0;
    }

    public static boolean temperatureHasChanged(int mask) {
        return (SENSOR_FLAG_TEMP & mask) > 0;
    }

    public static boolean pressureHasChanged(int mask) {
        return (SENSOR_FLAG_PRESSURE & mask) > 0;
    }

    public static float getHumidityPercent(byte[] sensorData, int offset) {
        return ((float) getTwoByteValue(sensorData, offset)) / 10;
    }

    public static float getPressureMBar(byte[] sensorData, int offset) {
        return ((float) getTwoByteValue(sensorData, offset)) / 10;
    }

    public static float getTemperatureC(byte[] sensorData, int offset) {
        return ((float) getTwoByteValue(sensorData, offset)) / 10;
    }

    public static float getTemperatureF(byte[] sensorData, int offset) {
        return getTemperatureC(sensorData, offset) * 9 / 5 + 32;
    }

    public static void getAccelorometerData(byte[] sensorData, int offset, int[] values) {
        values[0] = getTwoByteValue(sensorData, offset);
        values[1] = getTwoByteValue(sensorData, offset + 2);
        values[2] = getTwoByteValue(sensorData, offset + 4);
    }

    public static void getMagnometerData(byte[] sensorData, int offset, int[] values) {
        values[0] = getTwoByteValue(sensorData, offset);
        values[1] = getTwoByteValue(sensorData, offset + 2);
        values[2] = getTwoByteValue(sensorData, offset + 4);

    }

    public static float getCompassAngleDegrees(int[] magnometerValues) {
        double x = magnometerValues[0];
        double y = magnometerValues[1];
        return (float) MathUtils.getDegrees(y, x);
    }

    public static void getGyroData(byte[] sensorData, int offset, int[] values) {
        values[0] = getTwoByteValue(sensorData, offset) / 100;
        values[1] = getTwoByteValue(sensorData, offset + 2) / 100;
        values[2] = getTwoByteValue(sensorData, offset + 4) / 100;
    }

    public static float tempCtoF(float c) {
        return c * 9 / 5 + 32;
    }

    public static float tempFtoC(float f) {
        return (f - 32) * 5 / 9;
    }
    
    /**
     * Parses the sensor data bytes and updates the corresponding sensor(s) UI
     * component
     *
     * @param sensorData
     */
    public static float[] processSensorData(byte[] sensorData) {
    	
    	float[] acclGyroCompass = new float[9];
        int maskField = sensorData[0];
        int offset = 0;
        int[] values = new int[3];
//        switch (sensorData.length) {
//        case 19:

            // packet type specifying accelerometer, gyro, magno
            offset = 1;
            if (SensorDataParser.accelerometerHasChanged(maskField)) {
                SensorDataParser.getAccelorometerData(sensorData, offset, values);
                offset += SensorDataParser.SENSOR_ACCEL_DATA_SIZE;
            	acclGyroCompass[0] = values[0]; acclGyroCompass[1] = values[1]; acclGyroCompass[2] = values[2];
            	return acclGyroCompass;
        	}

            
/*            if (SensorDataParser.gyroHasChanged(maskField)) {
                SensorDataParser.getGyroData(sensorData, offset, values);
                offset += SensorDataParser.SENSOR_GYRO_DATA_SIZE;
                acclGyroCompass[3] = (int) ((values[0] - 5.0f) / 10); acclGyroCompass[4] = (int) ((values[1]) / 10); acclGyroCompass[5] = (int) ((values[2] + 0.5f) / 10);
            }

            if (SensorDataParser.magnetometerHasChanged(maskField)) {
                SensorDataParser.getMagnometerData(sensorData, offset, values);
                offset += SensorDataParser.SENSOR_MAGNO_DATA_SIZE;
                acclGyroCompass[6] = values[0]; acclGyroCompass[7] = values[1]; acclGyroCompass[8] = values[2];
            }
*/
  //          break;
  //      case 7:

            // packet type specifying temp, humid, press
//            offset = 1;
//            float value = 0;
//            if (mHumidityFrag.isVisible() && SensorDataParser.humidityHasChanged(maskField)) {
//                value = SensorDataParser.getHumidityPercent(sensorData, offset);
//                offset += SensorDataParser.SENSOR_HUMD_DATA_SIZE;
//                mHumidityFrag.setValue(mAnimationSlower, value);
//                updateView = true;
//            }
//            if (mPressureFrag.isVisible() && SensorDataParser.pressureHasChanged(maskField)) {
//                value = SensorDataParser.getPressureMBar(sensorData, offset);
//                offset += SensorDataParser.SENSOR_PRES_DATA_SIZE;
//                mPressureFrag.setValue(mAnimationSlower, value);
//                updateView = true;
//            }
//
//            if (mTemperatureFrag.isVisible() && SensorDataParser.temperatureHasChanged(maskField)) {
//                if (mIsTempScaleF) {
//                    value = SensorDataParser.getTemperatureF(sensorData, offset);
//                } else {
//                    value = SensorDataParser.getTemperatureC(sensorData, offset);
//                }
//                offset += SensorDataParser.SENSOR_TEMP_DATA_SIZE;
//                mTemperatureFrag.setValue(mAnimationSlower, value);
//                updateView = true;
//            }
//            if (updateView && mAnimationSlower != null) {
//                mAnimationSlower.animate();
//            }
//            break;
//        }

        return null;
    }

}