package be.uliege.uce.smartgps.utilities;

import android.app.Application;
import android.widget.Toast;

import com.onesignal.OSNotification;
import com.onesignal.OSNotificationAction;
import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Timestamp;

import be.uliege.uce.smartgps.activities.MainActivity;
import be.uliege.uce.smartgps.entities.User;
import be.uliege.uce.smartgps.service.NotificationOneSignal;

public class NotificationAction extends Application {

    private String notificationID = "";
    private String pregunta;
    private User user = new User();

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
        }
    }

    private class NotificationOpenedHandler implements OneSignal.NotificationOpenedHandler{
        @Override
        public void notificationOpened(OSNotificationOpenResult result){
            OSNotificationAction.ActionType actionType = result.action.type;

            JSONObject object = result.notification.payload.toJSONObject();
            JSONArray array = object.optJSONArray("actionButtons");
            pregunta = result.notification.payload.body;

            if(actionType == OSNotificationAction.ActionType.ActionTaken){
                for (int i = 0; i < array.length(); i++) {
                    try{
                        JSONObject objectButton = array.getJSONObject(i);
                        int time = (int) (System.currentTimeMillis());
                        Timestamp tsTemp = new Timestamp(time);
                        if(result.action.actionID.equals(objectButton.getString("id"))){
                            String respuesta = objectButton.getString("text");
                            NotificationOneSignal nos = new NotificationOneSignal();
                            if(respuesta.equals("Si")){
                                if(pregunta.equals("¿El sensor está tapado?")){
                                    nos.sendSecondNotification(1);
                                }else if(pregunta.equals("¿Está en movimiento?")){
                                    nos.sendSecondNotification(2);
                                }else if(pregunta.equals("¿Está detenido?")){
                                    nos.sendSecondNotification(3);
                                }
                            }

                            Toast.makeText(getApplicationContext(),
                                    "IdUser: "+user.getDspId()+" Pregunta: "+pregunta+" FechaResp: "+tsTemp+
                                            "IdNoti: "+notificationID+" texto: "+respuesta, Toast.LENGTH_SHORT).show();
                        }
                        MainActivity ma = new MainActivity();
                        ma.checkNotification(0);
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }else{
                MainActivity ma = new MainActivity();
                ma.checkNotification(0);
            }
        }
    }
}
