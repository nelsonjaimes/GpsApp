package com.example.njg_3.gpsapp;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.SharedPreferencesCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationResult;

import java.util.List;


public class DetectedActivitiesIntentService extends IntentService {
    private   static  final  String TAG=DetectedActivitiesIntentService.class.getName();
    public static  final  String ACTION_LOCATION_UPDATE="com.example.njg_3.gpsapp.action.LocationUpdate";
    public static final String EXTRA_LOCATION_UPDATE = "com.example.njg_3.gpsapp.extra.LocationUpdate";


    public DetectedActivitiesIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
       }


    @Override
    protected void onHandleIntent(Intent intent) {
          if (intent!=null){
               switch (intent.getAction()){
                   case ACTION_LOCATION_UPDATE:
                        try{
                            LocationResult locationResult= LocationResult.extractResult(intent);
                            Location location= getLocation(locationResult.getLocations());
                            if (location!=null){
                                Intent intenLocation=new Intent(ACTION_LOCATION_UPDATE);
                                intenLocation.putExtra(EXTRA_LOCATION_UPDATE,location);
                                LocalBroadcastManager.getInstance(this).sendBroadcast(intenLocation);
                            }

                        }catch (NullPointerException e){
                            e.printStackTrace();
                        }
                       break;
               }
            }
    }


    private  Location getLocation(List<Location> lsLocation){
       Location knowLocation=Utils.getLocationUpdatesResult(DetectedActivitiesIntentService.this);
        if (!lsLocation.isEmpty()){
            for (Location location:lsLocation){
                if (isBetterLocation(location,knowLocation)){
                     knowLocation=location;
                 }
            }
        }
       Utils.setLocationUpdatesResult(DetectedActivitiesIntentService.this,knowLocation);
      return knowLocation;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        final int TWO_MINUTES = 1000*60*2;
        if (currentBestLocation == null) {
           return true;
        }
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;
        if (isSignificantlyNewer) {
            return true;
            } else if (isSignificantlyOlder) {
            return false;
        }
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }
  }
