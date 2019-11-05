package be.uliege.uce.smartgps.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import be.uliege.uce.smartgps.utilities.Constants;
import be.uliege.uce.smartgps.utilities.DetectNoise;


public class SensorService extends Service implements SensorEventListener {

    private static final String TAG = SensorService.class.getSimpleName();
    private SensorManager mSensorManager;
    private Sensor mSensorAcc;
    private Sensor mSensorGyr;

    //Sensores Nuevos
    private Sensor mSensorProximity;
    private Sensor mSensorLuminosity;
    private Sensor mSensorStepCount;
    private BatteryManager mBattery;

    private static final int POLL_INTERVAL = 300;
    private Handler mHandler = new Handler();
    private DetectNoise mSensor;
    private Double amp;
    private Runnable mPollTask = new Runnable() {
        public void run() {
            amp = mSensor.getAmplitude();
            mHandler.postDelayed(mPollTask, POLL_INTERVAL);
        }
    };

    private RequestQueue queue;

    IBinder mBinder = new SensorService.LocalBinder();

    public class LocalBinder extends Binder {
        public SensorService getServerInstance() {
            return SensorService.this;
        }
    }

    public SensorService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        mSensor = new DetectNoise();
        start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorGyr = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //
        mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorLuminosity = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorStepCount = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        queue = Volley.newRequestQueue(this);
        //
        mSensorManager.registerListener(this, mSensorAcc, SensorManager.SENSOR_DELAY_UI, new Handler());
        mSensorManager.registerListener(this, mSensorGyr, SensorManager.SENSOR_DELAY_UI, new Handler());
        //
        mSensorManager.registerListener(this, mSensorProximity, SensorManager.SENSOR_DELAY_UI, new Handler());
        mSensorManager.registerListener(this, mSensorLuminosity, SensorManager.SENSOR_DELAY_UI, new Handler());
        mSensorManager.registerListener(this, mSensorStepCount, SensorManager.SENSOR_DELAY_UI, new Handler());
        //
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        final be.uliege.uce.smartgps.entities.Sensor sensor = new be.uliege.uce.smartgps.entities.Sensor();

        /*
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {

            /*if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                sensor.setAclX(null);
                sensor.setAclY(null);
                sensor.setAclZ(null);

            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                sensor.setGrsX(null);
                sensor.setGrsY(null);
                sensor.setGrsZ(null);

            return;
        }

        }*/

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensor.setAclX(event.values[0]);
            sensor.setAclY(event.values[1]);
            sensor.setAclZ(event.values[2]);
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensor.setGrsX(event.values[0]);
            sensor.setGrsY(event.values[1]);
            sensor.setGrsZ(event.values[2]);
        }

        //
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY){
            sensor.setProximity(event.values[0]);
        }

        if (event.sensor.getType() == Sensor.TYPE_LIGHT){
            sensor.setLuminosity(event.values[0]);
        }

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            sensor.setStepCounter(event.values[0]);
        }

        mBattery = (BatteryManager)getSystemService(BATTERY_SERVICE);
        sensor.setBattery(mBattery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));


        sensor.setSound(amp);
        //

        broadcastSensor(sensor);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void broadcastSensor(be.uliege.uce.smartgps.entities.Sensor sensor) {
        Intent intent = new Intent(Constants.SENSOR_ACTIVITY);
        intent.putExtra(Constants.SENSOR_ACTIVITY, sensor);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void start() {

        mSensor.start(this);
        mHandler.postDelayed(mPollTask, POLL_INTERVAL);
    }
}