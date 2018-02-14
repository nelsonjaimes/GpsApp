package com.example.njg_3.gpsapp;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.concurrent.Executor;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by NJG_3 on 3/12/2017.
 */

public class LocationService extends Service implements EasyPermissions.PermissionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks{

    private static final String[] PERMISSION_LOCATION = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation=null;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest.Builder mLocationRequestBuild;
    private static final int REQUEST_RESOLVE_ERROR = 102;
    private static final int REQUEST_CHECK_SETTINGS= 103;
    private static final int RC_LOCATION_PERMISSION =101;
    private static final  String CODE_KEY_LOCATION="code_key_location";
    public boolean STATE_UPDATE_LOCATION=false;
    public static final long UPDATE_INTERVAL =1*60*1000;
    public static final long UPDATE_FASTEST_INTERVAL = UPDATE_INTERVAL / 2;
    /*BroadCastRevicer*/
    public   MainActivity.ActivityDetectionBroadcastRevicer mBroadCastReciver;
    /* PlayServices*/
    private GoogleApiAvailability mGoogleApiAvailability;
    private Context context;
    public LocationService(Context context) {
        super();
        this.context=context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("nel_","OnCreate");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mLocationRequest = new LocationRequest()
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(UPDATE_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        /*Obejeto para el settingLocation*/
        mLocationRequestBuild = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

         //updateValuesFromBundle(savedInstanceState);
        /*Verificar si el googlePLayServices es actualizado*/
        mGoogleApiAvailability= GoogleApiAvailability.getInstance();
         /*Para obtener la ultima posicion conocida*/
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

    }

    /*GOOGLE LOCATION*/
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        showMessage("Se establecio conexion con el Proveedor GPS...");
        initSettingLocation();
    }

    private boolean isPermisionLocation() {
        return EasyPermissions.hasPermissions(this, PERMISSION_LOCATION);
    }
    @AfterPermissionGranted(RC_LOCATION_PERMISSION)
    void initPermissionLocation() {
        if (isPermisionLocation()) {
            showMessage(R.string.msmHasPermissionLocation);
            initKnowLocation();
            initUpdateLocation();

        } else {
            EasyPermissions.requestPermissions(LocationService.this,
                    context.getString(R.string.msmRequesPermissionLocation),
                    RC_LOCATION_PERMISSION, PERMISSION_LOCATION);
        }
    }

    private void initUpdateLocation(){
        try{
            showMessage("iniciando upate Location...");
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,getPendingIntent());
        }catch (SecurityException e){
            showMessage("exceptionSecurity:"+e.toString());
            e.printStackTrace();
        }

    }

    private void initKnowLocation(){
        mFusedLocationClient.getLastLocation().addOnSuccessListener((Executor) this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                Location knowLocation=Utils.getLocationUpdatesResult(LocationService.this);
                if (knowLocation==null){
                    if (location!=null){
                        mLastLocation=location;
                    }else{
                        showMessage(R.string.msmNoLocation);
                    }
                }else{
                    mLastLocation=knowLocation;
                }
                Utils.setLocationUpdatesResult(LocationService.this,mLastLocation);
                showMessage("Ultima Location Conocida: Lat:"+mLastLocation.getLatitude()+"/Log:"+mLastLocation.getLongitude());

            }

        });
    }


    private void initSettingLocation(){
        showMessage("consultando la configuracion de localizacion");
        // Verificar ajustes de ubicación actuales
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(mLocationRequestBuild.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                STATE_UPDATE_LOCATION=true;
                showMessage("Los ajustes de ubicación satisfacen la configuración.");
                initPermissionLocation();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showMessage("Los ajustes de ubicación no satisfacen la configuración. Se mostrará un diálogo de ayuda..");
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MainActivity.this,REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        STATE_UPDATE_LOCATION=false;
                        break;
                }
            }
        });

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void showMessage(int idString){
        Log.d("nel_", getString(idString));
    }
    private void showMessage(String idString){
        Log.d("nel_",":"+idString);
    }
}
