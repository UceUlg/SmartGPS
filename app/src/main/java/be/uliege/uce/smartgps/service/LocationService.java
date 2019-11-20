package be.uliege.uce.smartgps.service;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import be.uliege.uce.smartgps.R;
import be.uliege.uce.smartgps.activities.MainActivity;
import be.uliege.uce.smartgps.entities.Sensor;
import be.uliege.uce.smartgps.utilities.Constants;
import be.uliege.uce.smartgps.utilities.NotificationUtils;

public class LocationService extends Service implements GpsStatus.Listener, LocationListener {

    private static final String TAG = LocationService.class.getSimpleName();

    private GpsStatus gpsStatus;
    private LocationManager lm;

    private int nsat, msat;
    private String providerSelect;
    private RequestQueue queue;

    private Double temperature;
    private String locality;
    private String description;
    private String [] temporal = new String[3];

    IBinder mBinder = new LocationService.LocalBinder();

    public class LocalBinder extends Binder {
        public LocationService getServerInstance() {
            return LocationService.this;
        }
    }

    public LocationService() {
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        /*if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            providerSelect = LocationManager.GPS_PROVIDER;
            Log.e("asasasasasa","sasasas");

        }else if(locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            providerSelect = LocationManager.NETWORK_PROVIDER;
        }else{
            providerSelect = LocationManager.NETWORK_PROVIDER;
        }
        */
        providerSelect = LocationManager.GPS_PROVIDER;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }else {
            //lm.requestLocationUpdates(providerSelect, 0, 2 , (LocationListener) this);
            lm.requestLocationUpdates(providerSelect, Constants.FREQUENCY_SECOND * 1000,2 , (LocationListener) this);
            lm.addGpsStatusListener(this);
        }

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        queue = Volley.newRequestQueue(this);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public void onGpsStatusChanged(int event) {
        nsat = 0;
        msat = 0;
        Sensor sensor = new Sensor();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

        }
        gpsStatus = lm.getGpsStatus(gpsStatus);
        for (GpsSatellite satellitesItem : gpsStatus.getSatellites()) {
            if (satellitesItem.usedInFix()) {
                nsat++;
            }
            msat++;
        }
        //sensor.setnSatellites(msat);
        sensor.setnSatellites(nsat);

        List<String> listProviders = lm.getAllProviders();
        double avgAccuracy = 0.0;
        for (String item : listProviders) {
            LocationProvider provider = lm.getProvider(item);
            avgAccuracy = avgAccuracy + provider.getAccuracy();
        }
        avgAccuracy = avgAccuracy / listProviders.size();
        sensor.setAccuracy(avgAccuracy);
        broadcastActivity(sensor);
    }

    @Override
    public void onLocationChanged(Location location) {
        Sensor sensor = new Sensor();
        if(location != null) {
            sensor.setLatitude(location.getLatitude());
            sensor.setLongitude(location.getLongitude());
            String [] valor = apiWeather(location.getLatitude(), location.getLongitude());
            if (valor[0] != null && !valor[0].equals("null")
                    && valor[1] != null && !valor[1].equals("null")
                    && valor[2] != null && !valor[2].equals("null")){
                long i = Math.round(Double.parseDouble(valor[0]));
                int temp = (int)i;
                sensor.setTemperature(temp);
                sensor.setWeather(valor[1]);
                sensor.setCity(valor[2]);
            }
            sensor.setVelocity(location.getSpeed());
            sensor.setAltitude(location.getAltitude());
            broadcastActivityLocation(sensor);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i(TAG, "onProviderDisabled: " + provider);
        NotificationUtils mNotificationUtils = new NotificationUtils(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder nb = mNotificationUtils. getAndroidChannelNotification(getString(R.string.msgTittleNotification) + provider + ".!", getString(R.string.msgBodyNotification1) + provider + getString(R.string.msgBodyNotification2), MainActivity.class);
            mNotificationUtils.getManager().notify(101, nb.build());
        }else{
            mNotificationUtils.notificationAndroid(getString(R.string.msgTittleNotification) + provider + ".!", getString(R.string.msgBodyNotification1) + provider + getString(R.string.msgBodyNotification2), MainActivity.class);
        }

    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG, "onProviderEnabled: " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle bundle) {
        //Log.i(TAG, "onStatusChanged: " + provider);
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    private void broadcastActivityLocation(Sensor sensor) {
        Intent intent = new Intent(Constants.LOCATION_ACTIVITY);
        intent.putExtra(Constants.LOCATION_ACTIVITY, sensor);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastActivity(Sensor sensor) {
        Intent intent = new Intent(Constants.GPS_ACTIVITY);
        intent.putExtra(Constants.GPS_ACTIVITY, sensor);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public String [] apiWeather(Double lat, Double lon){

        String cityNow = cityService(lat, lon);
        if(cityNow != null && !cityNow.equals("null")){
            String url = "https://api.openweathermap.org/data/2.5/weather?q="+cityNow+"&appid=3f4998721d5fd526ab894777bf0e63ff";

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    //final double temp;
                    try{
                        JSONObject arrayTemperatura = response.getJSONObject("main");
                        JSONArray arrayClima = response.getJSONArray("weather");
                        JSONObject object = arrayClima.getJSONObject(0);
                        temperature = arrayTemperatura.getDouble("temp");
                        description = object.getString("main");
                        temperature = temperature - 273.15;
                        String city = response.getString("name");
                        String piv = String.valueOf(temperature);
                        temporal[0] = piv;
                        temporal[1] = description;
                        temporal[2] = city;
                    }catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                }
            });
            queue.add(request);
        }

        return temporal;
    }

    public String cityService(Double lat, Double lon){
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        final List<Address> addresses;

        try{
            if (lat != null && lon != null){
                addresses = geocoder.getFromLocation(lat,lon,10);
                if (addresses.size() > 0){
                    for (Address adr : addresses){
                        if (adr.getLocality() != null && adr.getLocality().length() >0){
                            locality = adr.getLocality();
                            break;
                        }
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return locality;
    }

}