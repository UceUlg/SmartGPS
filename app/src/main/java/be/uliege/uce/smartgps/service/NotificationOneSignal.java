package be.uliege.uce.smartgps.service;

import android.util.Log;
import android.widget.Toast;

import com.onesignal.OSPermissionSubscriptionState;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import be.uliege.uce.smartgps.activities.MainActivity;


public class NotificationOneSignal {

    private Date horaInicio = null;
    private Date horaFin;
    private int ch = 0;

    public void sendFirstNotification(int valor){

        String [][] arrayPregunta = {{"1", "¿El sensor está tapado?", "Si", "No"},
                                    {"2", "¿Está en movimiento?", "Si", "No"},
                                    {"3", "¿Está detenido?", "Si", "No"}};

        OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
        String userID = status.getSubscriptionStatus().getUserId();
        boolean isSubscribed = status.getSubscriptionStatus().getSubscribed();

        valor = valor - 1;

        if (!isSubscribed)
            return;

        try {
            OneSignal.postNotification(new JSONObject("{'contents': {'en':'"+arrayPregunta[valor][1]+"'}, " +
                            "'include_player_ids': ['" + userID + "'], " +
                            "'headings': {'en': 'SmartGPS'}, " +
                            "'buttons':[{'id': 'id1', 'text': '"+arrayPregunta[valor][2]+"'}, {'id':'id2', 'text': '"+arrayPregunta[valor][3]+"'}]}"),
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
    }

    public void sendSecondNotification(int valor){

        String [][] arrayPregunta = {{"1", "¿Qué hacía?", "Lamada", "Guardado"},
                {"2", "¿Ya se detuvo?", "Si", "No"},
                {"3", "¿Está en...?", "Clases", "Descansando"}};

        OSPermissionSubscriptionState status = OneSignal.getPermissionSubscriptionState();
        String userID = status.getSubscriptionStatus().getUserId();
        boolean isSubscribed = status.getSubscriptionStatus().getSubscribed();

        valor = valor - 1;

        if (!isSubscribed)
            return;

        try {
            OneSignal.postNotification(new JSONObject("{'contents': {'en':'"+arrayPregunta[valor][1]+"'}, " +
                            "'include_player_ids': ['" + userID + "'], " +
                            "'headings': {'en': 'SmartGPS'}, " +
                            "'buttons':[{'id': 'id1', 'text': '"+arrayPregunta[valor][2]+"'}, {'id':'id2', 'text': '"+arrayPregunta[valor][3]+"'}]}"),
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
    }

    public void notificationActivity(int valor){
        if(valor == 3){
            if(horaInicio == null){
                horaInicio = new Date();
            }
            horaFin = new Date();
            long tiempoInicial = horaInicio.getTime();
            long tiempoFinal = horaFin.getTime();
            long min = tiempoFinal - tiempoInicial;
            min = min/(1000*60);
            System.out.println("////////////////////************"+min);
            if(min>=2){
                horaFin=null;
                horaInicio=null;
                MainActivity ma = new MainActivity();
                ch = ma.checkNotification(1);
                if(ch<2){
                    sendFirstNotification(valor);
                }
            }
        }else{

        }
    }
}
