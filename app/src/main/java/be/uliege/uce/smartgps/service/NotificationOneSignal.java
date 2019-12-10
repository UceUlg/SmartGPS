package be.uliege.uce.smartgps.service;

import android.util.Log;

import com.onesignal.OSPermissionSubscriptionState;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class NotificationOneSignal {

    private String pregunta, nextQuestion, id1, id2, id3, resp1, resp2, resp3;

    public String sendFirstNotification(String response){
        try{
            JSONArray jsonArray =  new JSONArray(response);
            pregunta = jsonArray.getJSONObject(0).getString("descripcion_pregunta");
            id1 = jsonArray.getJSONObject(0).getString("respId");
            id2 = jsonArray.getJSONObject(1).getString("respId");
            resp1 = jsonArray.getJSONObject(0).getString("descripcion_respuesta");
            resp2 = jsonArray.getJSONObject(1).getString("descripcion_respuesta");
        }catch (JSONException e){
            e.printStackTrace();
        }

        OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
        String userID = status.getSubscriptionStatus().getUserId();
        boolean isSubscribed = status.getSubscriptionStatus().getSubscribed();

        if (!isSubscribed)
            System.out.println("No suscrito");

        try {
            OneSignal.postNotification(new JSONObject("{'contents': {'en':'"+pregunta+"'}, " +
                            "'include_player_ids': ['" + userID + "'], " +
                            "'headings': {'en': 'SmartGPS'}, " +
                            "'buttons':[{'id': '"+id1+"', 'text': '"+resp1+"'}, {'id':'"+id2+"', 'text': '"+resp2+"'}]}"),
                    new OneSignal.PostNotificationResponseHandler() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.i("OneSignalSmartGPS", "postNotification Success: " + response);
                        }

                        @Override
                        public void onFailure(JSONObject response) {
                            Log.e("OneSignalSmartGPS", "postNotification Failure: " + response);
//                            SensorService ma = new SensorService();
//                            ma.checkNotification(0);
//                            ma.timeNotification(1);
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return pregunta;
    }

    public String sendSecondNotification(String response){

        try{
            JSONArray jsonArray =  new JSONArray(response);
            nextQuestion = jsonArray.getJSONObject(0).getString("descripcion_pregunta");
            id1 = jsonArray.getJSONObject(0).getString("respId");
            id2 = jsonArray.getJSONObject(1).getString("respId");
            id3 = jsonArray.getJSONObject(2).getString("respId");
            resp1 = jsonArray.getJSONObject(0).getString("descripcion_respuesta");
            resp2 = jsonArray.getJSONObject(1).getString("descripcion_respuesta");
            resp3 = jsonArray.getJSONObject(2).getString("descripcion_respuesta");
        }catch (JSONException e){
            e.printStackTrace();
        }

        OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
        String userID = status.getSubscriptionStatus().getUserId();
        boolean isSubscribed = status.getSubscriptionStatus().getSubscribed();

        if (!isSubscribed)
            System.out.println("No suscrito");

        try {
            OneSignal.postNotification(new JSONObject("{'contents': {'en':'"+nextQuestion+"'}, " +
                            "'include_player_ids': ['" + userID + "'], " +
                            "'headings': {'en': 'SmartGPS'}, " +
                            "'buttons':[{'id': '"+id1+"', 'text': '"+resp1+"'}, {'id':'"+id2+"', 'text': '"+resp2+"'}, {'id':'"+id3+"', 'text': '"+resp3+"'}]}"),
                    new OneSignal.PostNotificationResponseHandler() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.i("OneSignalSmartGPS", "postNotification Success: " + response);
                        }

                        @Override
                        public void onFailure(JSONObject response) {
                            Log.e("OneSignalSmartGPS", "postNotification Failure: " + response);
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return nextQuestion;
    }
}
