package be.uliege.uce.smartgps.utilities;

import android.app.Application;
import android.os.CountDownTimer;
import android.util.Log;

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
import com.google.gson.Gson;
import com.onesignal.OSNotification;
import com.onesignal.OSNotificationAction;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import be.uliege.uce.smartgps.entities.User;
import be.uliege.uce.smartgps.service.NotificationOneSignal;
import be.uliege.uce.smartgps.service.SensorService;

public class NotificationAction extends Application {

    private NotificationOneSignal nos = new NotificationOneSignal();
    SensorService ss = new SensorService();

    private String notificationID = "";
    private long milisNotif;
    private String pregunta;
    private String QUESTION_DEFAULT = "";

    static final int DEFAULT_CHECK = 0;
    static int check = 0;


    static CountDownTimer withoutCounter;

    @Override
    public void onCreate(){
        super.onCreate();

        OneSignal.startInit(this)
                .setNotificationReceivedHandler(new NotificationReceivedHandler())
                .setNotificationOpenedHandler(new NotificationOpenedHandler())
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

    }

    private class NotificationReceivedHandler implements OneSignal.NotificationReceivedHandler{
        @Override
        public void notificationReceived(OSNotification notification){
            notificationID = notification.payload.notificationID;
            String a = notification.payload.rawPayload;
            try {
                JSONObject as = new JSONObject(a);
                milisNotif = as.getLong("google.sent_time");
            }catch (JSONException e){
                e.printStackTrace();
            }

        }
    }

    private class NotificationOpenedHandler implements OneSignal.NotificationOpenedHandler{
        @Override
        public void notificationOpened(OSNotificationOpenResult result){
            OSNotificationAction.ActionType actionType = result.action.type;
            JSONObject object = result.notification.payload.toJSONObject();
            JSONArray array = object.optJSONArray("actionButtons");
            pregunta = result.notification.payload.body;

            int cn = checkNotification(1);

            if(cn < 2){
                if(actionType == OSNotificationAction.ActionType.ActionTaken){
                    for (int i = 0; i < array.length(); i++) {
                        try{
                            JSONObject objectButton = array.getJSONObject(i);
                            if(result.action.actionID.equals(objectButton.getString("id"))){
                                Timestamp time = new Timestamp(System.currentTimeMillis());
                                Timestamp timeCrea = new Timestamp(milisNotif);
                                String respuesta = objectButton.getString("id");
                                String txtRespuesta = objectButton.getString("text");
                                OneSignal.clearOneSignalNotifications();
                                User user = new Gson().fromJson(DataSession.returnDataSession(getApplicationContext(), Constants.INFO_SESSION_KEY), User.class);
                                Map<String, String> params = new HashMap<>();
                                params.put("type", "setNotificationAnswer");
                                params.put("dspId", String.valueOf(user.getDspId()));
                                params.put("rpPregunta", pregunta);
                                params.put("rpRespuesta", respuesta);
                                params.put("rpFecha", time.toString());
                                params.put("rpFechaCreacion", timeCrea.toString());
                                saveNotificationAnswer(params);
                                if(txtRespuesta.equals("Si")){
                                    ss.timeWithoutAnswer(0);
                                    Map<String, String> paramsS = new HashMap<>();
                                    paramsS.put("type", "getNotificationQuestion");
                                    paramsS.put("pregId", "2");
                                    getQuestionNotification(paramsS);
                                    timeWithoutAnswer(1);
                                }else if(txtRespuesta.equals("No")){
                                    ss.timeWithoutAnswer(0);
                                    ss.timeNewNotification();
                                }

                                if(pregunta.equals("¿Medio de transporte?")){
                                    timeWithoutAnswer(0);
                                    Map<String, String> paramsS = new HashMap<>();
                                    paramsS.put("type", "getNotificationQuestion");
                                    paramsS.put("pregId", "3");
                                    timeWithoutAnswer(1);
                                    getQuestionNotification(paramsS);

                                }else if(pregunta.equals("¿Proposito del viaje?")){
                                    timeWithoutAnswer(0);
                                    ss.timeNewNotification();
                                }
                                checkNotification(0);
                            }
                        }catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public void saveNotificationAnswer (final Map<String, String> params){
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
                            pregunta = nos.sendSecondNotification(response);
                            response = null;
                        } else {
                        }
                    }
                },
                new com.android.volley.Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        timeWithoutAnswer(0);
                        ss.checkNotification(0);
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

    public void withoutAnswer(String preg){
        Timestamp time = new Timestamp(System.currentTimeMillis());
        Timestamp timeCrea = new Timestamp(milisNotif);
        User user = new Gson().fromJson(DataSession.returnDataSession(getApplicationContext(), Constants.INFO_SESSION_KEY), User.class);
        Map<String, String> params = new HashMap<>();
        params.put("type", "setNotificationAnswer");
        params.put("dspId", String.valueOf(user.getDspId()));
        params.put("rpPregunta", preg);
        params.put("rpRespuesta", "-1");
        params.put("rpFecha", time.toString());
        params.put("rpFechaCreacion", timeCrea.toString());
        saveNotificationAnswer(params);
        ss.checkNotification(0);
        pregunta = QUESTION_DEFAULT;
        OneSignal.clearOneSignalNotifications();
    }

    public int checkNotification (int valor){
        if(valor == 1){
            check = check + valor;
        }else if(valor == 0) {
            check = DEFAULT_CHECK;
        }
        return check;
    }
}
