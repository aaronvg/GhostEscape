package com.komodo.mygdxgame;

/*
Sensor Fusion code borrowed from Paul Lawitzki under the MIT license.

----------------------------------------------------------------------
Copyright 2012 Paul Lawitzki

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-------------------------------------------------------------------------


*/


import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;



import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;
import android.widget.RadioGroup;
import android.widget.TextView;

public class SensorFusionListener implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mRotationVectorSensor;
    Context context;

    // Used for initial calibration to 0 degrees
    private Timer calibrateAngle = new Timer();
    private double angleOffset;
    private boolean firstRunCalibration;

    private float[] R = new float[9];
    // With one sensor matrix is only 9
    private final float[] mRotationMatrix = new float[16];

    // angular speeds from gyro
    private float[] gyro = new float[3];
    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];
    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];
    // magnetic field vector
    private float[] magnet = new float[3];
    // accelerometer vector
    private float[] accel = new float[3];
    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];
    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];

    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    private double azimuth;
    private float g_azimuth;

    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;
    //private Timer fuseTimer = new Timer();

    // The following members are only for displaying the sensor output.
    public Handler mHandler;
    private RadioGroup mRadioGroup;
    private TextView mAzimuthView;
    private TextView mPitchView;
    private TextView mRollView;
    private int radioSelection;
    DecimalFormat d = new DecimalFormat("#.##");

    Paint thePaint = new Paint();
    Rect r = new Rect(0, 0, 0, 0);
    int angle;
    long start;

    private Timer fuseTimer;
    private Handler handler;

    private boolean oneSensor;
    float valuesCopy[];
    float values2[];
    public SensorFusionListener(Context contxt) {
        oneSensor = true;
        // Initialize to identity matrix
        mRotationMatrix[0] = 1;
        mRotationMatrix[4] = 1;
        mRotationMatrix[8] = 1;
        mRotationMatrix[12] = 1;

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f;
        gyroMatrix[1] = 0.0f;
        gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f;
        gyroMatrix[4] = 1.0f;
        gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f;
        gyroMatrix[7] = 0.0f;
        gyroMatrix[8] = 1.0f;

        d.setRoundingMode(RoundingMode.HALF_UP);
        d.setMaximumFractionDigits(3);
        d.setMinimumFractionDigits(3);

        thePaint.setTextSize(50);
        start = System.currentTimeMillis();
        angle = 0;
        valuesCopy = new float[3];
        values2 = new float[3];
       // fuseTimer = new Timer();
        // instead of TIME_CONSTANT just re-orient every 100ms
        //fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(),
        //        1000, 50);
        handler = new Handler();
           
        context = contxt;
        mSensorManager = (SensorManager) context
                .getSystemService(Context.SENSOR_SERVICE);
     //   handler.postDelayed(runnable, 100);
        /** With one sensor **/
        if(oneSensor)
          mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public void startSensor() {
        // Used to calibrate the angle to 0 at the beginning.
    //    calibrateAngle.scheduleAtFixedRate(new calibrateAngleTask(), 4000, 100);
        firstRunCalibration = true;
        angleOffset = 0;
        
        Log.d("sensor", "startfused");
        /** With one sensor **/
        if(oneSensor){
          //  mSensorManager.registerListener(this, mRotationVectorSensor,
          //  		SensorManager.SENSOR_DELAY_GAME);
        	 mSensorManager.registerListener(this, mRotationVectorSensor,
        			 (int)(1.0F/60) * 1000 * 1000); //poll at 60hz
        	
        }
        /** With many sensors **/
        else {
         //   handler.postDelayed(runnable, 100);
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void stopSensor() {
   //     handler.removeCallbacks(runnable);
        firstRunCalibration = false;
        Log.d("sensor", "end");
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
    	
        // TODO Auto-generated method stub
        // Log.d("sensor", "changed");
        /** WITH ONE SENSOR **/
        if(oneSensor) {
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            	// convert the rotation-vector to a 4x4 matrix.
            	// System.arraycopy(event.values, 0, valuesCopy, 0, 3);
            	SensorManager.getRotationMatrixFromVector(mRotationMatrix,
            			event.values);
            	
            	// Saves the orientation values in the values array
            	SensorManager.getOrientation(mRotationMatrix, event.values);
            	System.arraycopy(event.values, 0, values2, 0, 1);
            	//values2 = valuesCopy;
            }
        }
        /** With Sensor Fusion **/
        else {
            switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                // copy new accelerometer data into accel array and calculate
                // orientation
                System.arraycopy(event.values, 0, accel, 0, 3);
                calculateAccMagOrientation();
                break;
            case Sensor.TYPE_GYROSCOPE:
                // process gyro data
                //Log.d("gyro", "triggered");
                gyroFunction(event);
                break;
    
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, magnet, 0, 3);
                break;
            }
        }

    }

    public final float getAzimuth() {
        /** With one sensor **/
        if(oneSensor) {
         // return g_azimuth -90; // the -90 dictates the direction we start in.
        	g_azimuth = (float) (values2[0] * 180 / Math.PI);
        	if (firstRunCalibration) {
        		angleOffset = g_azimuth;
        		firstRunCalibration = false;
        	}
        	g_azimuth += -angleOffset;
        	return g_azimuth - 90;
        }

        /** With many **/
        else {
            //azimuth = fusedOrientation[0] * 180/Math.PI;
            //  azimuth = gyroOrientation[0] * 180/Math.PI;
            //azimuth = accMagOrientation[0] * 180/Math.PI;
            // calculateAngle();
            
            azimuth = gyroOrientation[0] * 180 / Math.PI - angleOffset;
            return (float) azimuth;
        }
    }

    public void calculateAngle() {
        //calculateFusedOrientationTask task = new calculateFusedOrientationTask();
        //azimuth = task.getAzimuth();
        float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;
        /*
         * Fix for 179� <--> -179� transition problem:
         * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
         * If so, add 360� (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360� from the result
         * if it is greater than 180�. This stabilizes the output in positive-to-negative-transition cases.
         */
        // azimuth
        if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (FILTER_COEFFICIENT
                    * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff
                    * accMagOrientation[0]);
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else if (accMagOrientation[0] < -0.5 * Math.PI
                && gyroOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (FILTER_COEFFICIENT
                    * gyroOrientation[0] + oneMinusCoeff
                    * (accMagOrientation[0] + 2.0 * Math.PI));
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else {
            fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0]
                    + oneMinusCoeff * accMagOrientation[0];
        }

        // pitch
        if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (FILTER_COEFFICIENT
                    * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff
                    * accMagOrientation[1]);
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else if (accMagOrientation[1] < -0.5 * Math.PI
                && gyroOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (FILTER_COEFFICIENT
                    * gyroOrientation[1] + oneMinusCoeff
                    * (accMagOrientation[1] + 2.0 * Math.PI));
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else {
            fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1]
                    + oneMinusCoeff * accMagOrientation[1];
        }

        // roll
        if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (FILTER_COEFFICIENT
                    * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff
                    * accMagOrientation[2]);
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else if (accMagOrientation[2] < -0.5 * Math.PI
                && gyroOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (FILTER_COEFFICIENT
                    * gyroOrientation[2] + oneMinusCoeff
                    * (accMagOrientation[2] + 2.0 * Math.PI));
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else {
            fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2]
                    + oneMinusCoeff * accMagOrientation[2];
        }

        // overwrite gyro matrix and orientation with fused orientation
        // to comenpensate gyro drift
        gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
        System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);

        //azimuth = fusedOrientation[0] * 180/Math.PI;
        azimuth = gyroOrientation[0] * 180 / Math.PI;
        //azimuth = accMagOrientation[0] * 180/Math.PI;
        // Log.d("")
        /*
        case 0:
        mAzimuthView.setText(d.format(accMagOrientation[0] * 180/Math.PI) + '�');
        mPitchView.setText(d.format(accMagOrientation[1] * 180/Math.PI) + '�');
        mRollView.setText(d.format(accMagOrientation[2] * 180/Math.PI) + '�');
        break;
        case 1:
        mAzimuthView.setText(d.format(gyroOrientation[0] * 180/Math.PI) + '�');
        mPitchView.setText(d.format(gyroOrientation[1] * 180/Math.PI) + '�');
        mRollView.setText(d.format(gyroOrientation[2] * 180/Math.PI) + '�');
        break;
        case 2:
        mAzimuthView.setText(d.format(fusedOrientation[0] * 180/Math.PI) + '�');
        */
    }

    // calculates orientation angles from accelerometer and magnetometer output
    public void calculateAccMagOrientation() {
        if (SensorManager
                .getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    // This function is borrowed from the Android reference
    // at http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    // It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues,
            float[] deltaRotationVector, float timeFactor) {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude = (float) Math
                .sqrt(gyroValues[0] * gyroValues[0] + gyroValues[1]
                        * gyroValues[1] + gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }
        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has been acquired
        if (accMagOrientation == null)
            return;

        // initialization of the gyroscope based rotation matrix
        if (initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);
        // apply the new rotation interval on the gyroscope based rotation matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float) Math.sin(o[1]);
        float cosX = (float) Math.cos(o[1]);
        float sinY = (float) Math.sin(o[2]);
        float cosY = (float) Math.cos(o[2]);
        float sinZ = (float) Math.sin(o[0]);
        float cosZ = (float) Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f;
        xM[1] = 0.0f;
        xM[2] = 0.0f;
        xM[3] = 0.0f;
        xM[4] = cosX;
        xM[5] = sinX;
        xM[6] = 0.0f;
        xM[7] = -sinX;
        xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY;
        yM[1] = 0.0f;
        yM[2] = sinY;
        yM[3] = 0.0f;
        yM[4] = 1.0f;
        yM[5] = 0.0f;
        yM[6] = -sinY;
        yM[7] = 0.0f;
        yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ;
        zM[1] = sinZ;
        zM[2] = 0.0f;
        zM[3] = -sinZ;
        zM[4] = cosZ;
        zM[5] = 0.0f;
        zM[6] = 0.0f;
        zM[7] = 0.0f;
        zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];
        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    class calibrateAngleTask extends TimerTask {
        public void run() {
            angleOffset = azimuth;
            Log.d("timer", "calibrating");
            this.cancel();
        }
    }
    
    
    
   /* private Runnable runnable = new Runnable() {
        public void run() {
            {
                float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

                // azimuth
                if (gyroOrientation[0] < -0.5 * Math.PI
                        && accMagOrientation[0] > 0.0) {
                    fusedOrientation[0] = (float) (FILTER_COEFFICIENT
                            * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff
                            * accMagOrientation[0]);
                    fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI
                            : 0;
                } else if (accMagOrientation[0] < -0.5 * Math.PI
                        && gyroOrientation[0] > 0.0) {
                    fusedOrientation[0] = (float) (FILTER_COEFFICIENT
                            * gyroOrientation[0] + oneMinusCoeff
                            * (accMagOrientation[0] + 2.0 * Math.PI));
                    fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI
                            : 0;
                } else {
                    fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0]
                            + oneMinusCoeff * accMagOrientation[0];
                }

                // pitch
                if (gyroOrientation[1] < -0.5 * Math.PI
                        && accMagOrientation[1] > 0.0) {
                    fusedOrientation[1] = (float) (FILTER_COEFFICIENT
                            * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff
                            * accMagOrientation[1]);
                    fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI
                            : 0;
                } else if (accMagOrientation[1] < -0.5 * Math.PI
                        && gyroOrientation[1] > 0.0) {
                    fusedOrientation[1] = (float) (FILTER_COEFFICIENT
                            * gyroOrientation[1] + oneMinusCoeff
                            * (accMagOrientation[1] + 2.0 * Math.PI));
                    fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI
                            : 0;
                } else {
                    fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1]
                            + oneMinusCoeff * accMagOrientation[1];
                }

                // roll
                if (gyroOrientation[2] < -0.5 * Math.PI
                        && accMagOrientation[2] > 0.0) {
                    fusedOrientation[2] = (float) (FILTER_COEFFICIENT
                            * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff
                            * accMagOrientation[2]);
                    fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI
                            : 0;
                } else if (accMagOrientation[2] < -0.5 * Math.PI
                        && gyroOrientation[2] > 0.0) {
                    fusedOrientation[2] = (float) (FILTER_COEFFICIENT
                            * gyroOrientation[2] + oneMinusCoeff
                            * (accMagOrientation[2] + 2.0 * Math.PI));
                    fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI
                            : 0;
                } else {
                    fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2]
                            + oneMinusCoeff * accMagOrientation[2];
                }

                // overwrite gyro matrix and orientation with fused orientation
                // to compensate for gyro drift
                gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
                System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
            
                handler.postDelayed(this, 100);
            }
        }
    };
    
    */
    
    

    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            /*
             * Fix for 179� <--> -179� transition problem:
             * Check whether one of the two orientation angles (gyro or accMag) is negative while the other one is positive.
             * If so, add 360� (2 * math.PI) to the negative value, perform the sensor fusion, and remove the 360� from the result
             * if it is greater than 180�. This stabilizes the output in positive-to-negative-transition cases.
             */

            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI
                    && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT
                        * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff
                        * accMagOrientation[0]);
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI
                        : 0;
            } else if (accMagOrientation[0] < -0.5 * Math.PI
                    && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT
                        * gyroOrientation[0] + oneMinusCoeff
                        * (accMagOrientation[0] + 2.0 * Math.PI));
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI
                        : 0;
            } else {
                fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0]
                        + oneMinusCoeff * accMagOrientation[0];
            }

            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI
                    && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT
                        * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff
                        * accMagOrientation[1]);
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI
                        : 0;
            } else if (accMagOrientation[1] < -0.5 * Math.PI
                    && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT
                        * gyroOrientation[1] + oneMinusCoeff
                        * (accMagOrientation[1] + 2.0 * Math.PI));
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI
                        : 0;
            } else {
                fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1]
                        + oneMinusCoeff * accMagOrientation[1];
            }

            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI
                    && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT
                        * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff
                        * accMagOrientation[2]);
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI
                        : 0;
            } else if (accMagOrientation[2] < -0.5 * Math.PI
                    && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT
                        * gyroOrientation[2] + oneMinusCoeff
                        * (accMagOrientation[2] + 2.0 * Math.PI));
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI
                        : 0;
            } else {
                fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2]
                        + oneMinusCoeff * accMagOrientation[2];
            }

            // overwrite gyro matrix and orientation with fused orientation
            // to compensate for gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }

}
