package com.example.njg_3.gpsapp;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;
import com.google.gson.Gson;
/**
 * Created by NJG_3 on 3/12/2017.
 */

class Utils {
    final static String KEY_LOCATION_UPDATES_RESULT = "location-update-result";

    static void setLocationUpdatesResult(Context context,Location location) {
        try{
            Gson gson= new Gson();
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(KEY_LOCATION_UPDATES_RESULT,gson.toJson(location))
                    .apply();
          }catch (Exception e){
         e.printStackTrace();
        }
    }

    static Location getLocationUpdatesResult(Context context) {
      try{
          String locationString=PreferenceManager.getDefaultSharedPreferences(context)
                  .getString(KEY_LOCATION_UPDATES_RESULT, "");
            Gson gson= new Gson();
            return gson.fromJson(locationString,Location.class);
            }catch (Exception e){
            e.printStackTrace();
        }
       return null;
    }
}