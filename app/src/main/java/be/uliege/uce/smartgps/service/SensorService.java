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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.onesignal.OneSignal;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import be.uliege.uce.smartgps.activities.MainActivity;
import be.uliege.uce.smartgps.entities.User;
import be.uliege.uce.smartgps.utilities.Constants;
import be.uliege.uce.smartgps.utilities.DataSession;


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

    static final int DEFAULT_CHECK = 0;
    static int check = 0;
    private String QUESTION_DEFAULT = "";
    private String pregunta;
    private NotificationOneSignal nos = new NotificationOneSignal();
    MainActivity ma = new MainActivity();
    private static CountDownTimer withoutCounter;
    private Context contexto;

    private float proxi;
    private int count;

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
        contexto = ma.consContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        contexto = ma.consContext();
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
            proxi = event.values[0];
            sensor.setProximity(event.values[0]);
        }
        sensor.setProximity(proxi);

        if (event.sensor.getType() == Sensor.TYPE_LIGHT){
            sensor.setLuminosity(event.values[0]);
        }

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            count = (int) event.values[0];
            sensor.setStepCounter((int) event.values[0]);
        }

        sensor.setStepCounter(count);
        mBattery = (BatteryManager)getSystemService(BATTERY_SERVICE);
        sensor.setBattery(mBattery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));

        //
        if(valueHour()){
            int cn = checkNotification(1);
            if(cn < 2){
                timeFirstNotification();
            }
        }

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

    public int checkNotification (int valor){
        if(valor == 1){
            check = check + valor;
        }else if(valor == 0) {
            check = DEFAULT_CHECK;
        }
        return check;
    }

    public void timeFirstNotification(){
        new CountDownTimer(Constants.TIME_FIRST_NOTIFICATION*60000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                Map<String, String> params = new HashMap<>();
                params.put("type", "getNotificationQuestion");
                params.put("pregId", "1");
                getQuestionNotification(params);
                timeWithoutAnswer(1);
            }
        }.start();
    }

    public void timeWithoutAnswer(int valor){
        if(valor == 1){
            withoutCounter = new CountDownTimer(Constants.TIME_WITHOUT_ANSWER_NOTIFICATION*60000, 1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    withoutAnswer(pregunta);
                }
            }.start();
        }else if(valor == 0){
            withoutCounter.cancel();
        }
    }

    public void timeNewNotification(){
        new CountDownTimer(Constants.TIME_NEW_NOTIFICATION*60000, 1000) {
            public void onTick(long millisUntilFinished) {
            }
            public void onFinish() {
                checkNotification(0);
            }
        }.start();
    }

    public void withoutAnswer(String preg){
        Timestamp time = new Timestamp(System.currentTimeMillis());
        User user = new Gson().fromJson(DataSession.returnDataSession(getApplicationContext(), Constants.INFO_SESSION_KEY), User.class);
        Map<String, String> params = new HashMap<>();
        params.put("type", "setNotificationAnswer");
        params.put("dspId", String.valueOf(user.getDspId()));
        params.put("rpPregunta", preg);
        params.put("rpRespuesta", "-1");
        params.put("rpFecha", time.toString());
        saveNoNotificationAnswer(params);
        checkNotification(0);
        pregunta = QUESTION_DEFAULT;
        OneSignal.clearOneSignalNotifications();
    }

    public void saveNoNotificationAnswer (final Map<String, String> params){
        RequestQueue queue;
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        queue = new RequestQueue(cache, network);
        // Start the queue
        queue.start();
        //RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                Constants.URL_CONSUMMER,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("GuardarNotificacion", response);
                        String palabra = "OK";
                        boolean resultado = response.contains(palabra);
                        if (response != null && resultado) {
                            response = null;
                        } else {
                        }
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(5*1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(postRequest);
    }

    public void getQuestionNotification (final Map<String, String> params){
        RequestQueue queue;
        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        queue = new RequestQueue(cache, network);
        // Start the queue
        queue.start();
        //RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        StringRequest postRequest = new StringRequest(
                Request.Method.POST,
                Constants.URL_CONSUMMER,
                new com.android.volley.Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("PreguntaNotificacion", response);
                        if (response != null) {
                            pregunta = nos.sendFirstNotification(response);
                            response = null;
                        } else {
                            Toast.makeText(contexto, "Error al recuperar pregunta", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        timeWithoutAnswer(0);
                        checkNotification(0);
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        postRequest.setRetryPolicy(new DefaultRetryPolicy(5*1000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(postRequest);
    }

    private Boolean valueHour(){
        Timestamp objCalendar = new Timestamp(System.currentTimeMillis());

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        String strHora = sdf.format(objCalendar);

        try{
            Date date1, date2, dateNew;
            date1 = sdf.parse(Constants.START_HOUR);
            date2 = sdf.parse(Constants.END_HOUR);
            dateNew = sdf.parse(strHora);
            if ((date1.compareTo(dateNew) <= 0) && (date2.compareTo(dateNew) >= 0)){
                return false;
            }else{
                return true;
            }
        }catch (ParseException e){
            e.printStackTrace();
            return false;
        }
    }
}