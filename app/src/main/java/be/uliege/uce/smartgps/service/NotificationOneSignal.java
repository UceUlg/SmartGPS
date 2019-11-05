package be.uliege.uce.smartgps.service;

import android.util.Log;

import com.onesignal.OSPermissionSubscriptionState;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationOneSignal {

    public void sendNotification(){

        String [] arrayPregunta = {"1", "¿El sensor está tapado?", "Si", "No"};

        OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
        String userID = status.getSubscriptionStatus().getUserId();
        boolean isSubscribed = status.getSubscriptionStatus().getSubscribed();

        if (!isSubscribed)
            return;

        try {
            OneSignal.postNotification(new JSONObject("{'contents': {'en':'"+arrayPregunta[1]+"'}, " +
                            "'include_player_ids': ['" + userID + "'], " +
                            "'headings': {'en': 'SmartGPS'}, " +
                            "'buttons':[{'id': 'id1', 'text': '"+arrayPregunta[2]+"'}, {'id':'id2', 'text': '"+arrayPregunta[3]+"'}]}"),
                    new OneSignal.PostNotificationResponseHandler() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.i("OneSignalSmartGPS", "postNotification Success: " + response);
                        }

                        @Override
                        public void onFailure(JSONObject response) {
                            Log.e("OneSignalSmartGPS", "postNotification Failure: " + response);
                            System.out.println("NO FUNCIONO LA NOTIFICACION");
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
