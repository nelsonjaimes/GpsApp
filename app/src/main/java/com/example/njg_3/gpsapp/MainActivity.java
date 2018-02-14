package com.example.njg_3.gpsapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
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

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks
        {

    private static final String[] PERMISSION_LOCATION = {Manifest.permission.ACCESS_FINE_LOCATION,
                                                         Manifest.permission.ACCESS_COARSE_LOCATION};

    private TextView txtLon, txtLat;

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
    private ActivityDetectionBroadcastRevicer mBroadCastReciver;
    /* PlayServices*/
    private  GoogleApiAvailability mGoogleApiAvailability;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtLat = findViewById(R.id.txtLat);
        txtLon = findViewById(R.id.txtLon);
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

        registerActivityBroadCastReciver();
        updateValuesFromBundle(savedInstanceState);
        /*Verificar si el googlePLayServices es actualizado*/
        mGoogleApiAvailability= GoogleApiAvailability.getInstance();
         /*Para obtener la ultima posicion conocida*/
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


    }


    private void updateValuesFromBundle(Bundle savedInstanceState){
        if (savedInstanceState!=null){
            if (savedInstanceState.keySet().contains(CODE_KEY_LOCATION)){
                mLastLocation=savedInstanceState.getParcelable(CODE_KEY_LOCATION);
                updateUI();
            }
         }


    }
    /*=====================================================================
    * ACTIVITY RECOGNITIONS
    * =====================================================================*/

    private void unregisterActivityBroadCastReciver(){
        LocalBroadcastManager.getInstance(MainActivity.this).unregisterReceiver(mBroadCastReciver);
    }
    private void registerActivityBroadCastReciver(){
         mBroadCastReciver=new ActivityDetectionBroadcastRevicer();
         IntentFilter intentFilter= new IntentFilter();
         intentFilter.addAction(DetectedActivitiesIntentService.ACTION_LOCATION_UPDATE);
         LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(mBroadCastReciver,intentFilter);

     }
            private PendingIntent getPendingIntent() {
     Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
     intent.setAction(DetectedActivitiesIntentService.ACTION_LOCATION_UPDATE);
     return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
     }



    /*===================================================================
    * */

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

    private void updateUI(){
             if (mLastLocation!=null){
                 txtLon.append(String.valueOf(mLastLocation.getLongitude()+"\n"));
                 txtLat.append(String.valueOf(mLastLocation.getLatitude())+"\n");
             }
     }
    private void initKnowLocation(){
         mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
             @Override
             public void onSuccess(Location location) {
                 Location knowLocation=Utils.getLocationUpdatesResult(MainActivity.this);
                 if (knowLocation==null){
                     if (location!=null){
                         mLastLocation=location;
                     }else{
                        showMessage(R.string.msmNoLocation);
                     }
                 }else{
                     mLastLocation=knowLocation;
                 }
                 Utils.setLocationUpdatesResult(MainActivity.this,mLastLocation);
                 showMessage("Ultima Location Conocida: Lat:"+mLastLocation.getLatitude()+"/Log:"+mLastLocation.getLongitude());
                 updateUI();

                 }

         });
        }



    @Override
    protected void onStart() {
        super.onStart();
        int ret = mGoogleApiAvailability.isGooglePlayServicesAvailable(this);
        if (ret==ConnectionResult.SUCCESS){
            showMessage("correcta version de goooglePlay");
            if (mGoogleApiClient != null) {
                showMessage("iniciando conexion gps...");
                mGoogleApiClient.connect();
            }
        }else{
            showMessage("problemas en la  version de goooglePlay");
            if (mGoogleApiAvailability.isUserResolvableError(ret)){
               mGoogleApiAvailability.getErrorDialog(this,ret, REQUEST_RESOLVE_ERROR).show();
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
       if (mGoogleApiClient.isConnected()&& STATE_UPDATE_LOCATION){
           showMessage("onResumen...!!");
           initUpdateLocation();
           }
    }




    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient!=null && mGoogleApiClient.isConnected()){
           // unregisterActivityBroadCastReciver();
            }
    }

     @Override
     protected void onStop() {
     super.onStop();
     if (mGoogleApiClient!=null){
       // mGoogleApiClient.disconnect();
        }
     }




            @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(CODE_KEY_LOCATION,mLastLocation);
        super.onSaveInstanceState(outState);
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
            EasyPermissions.requestPermissions(this,
              getString(R.string.msmRequesPermissionLocation),
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


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == RC_LOCATION_PERMISSION) {
            STATE_UPDATE_LOCATION=true;
            showMessage(R.string.msmGrandendPermissionLocation);
           }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == RC_LOCATION_PERMISSION) {
            STATE_UPDATE_LOCATION=false;
            if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
                 new AppSettingsDialog.Builder(this).build().show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        showMessage("onResult");
        switch (requestCode){
            case  REQUEST_RESOLVE_ERROR:
                if (resultCode==RESULT_OK){
                    if (!mGoogleApiClient.isConnecting() &&!mGoogleApiClient.isConnected()) {
                    showMessage("onResult: estableciendo conexion de nuecvo GPS .!!");
                    mGoogleApiClient.connect();
                    STATE_UPDATE_LOCATION=true;
                     }
                 }
                break;
            case  REQUEST_CHECK_SETTINGS:
                   if (resultCode==RESULT_OK){
                    showMessage("El usuario permitió el cambio de ajustes de ubicación");
                    initPermissionLocation();
                }else{
                    showMessage("El usuario NO ! permitió el cambio de ajustes de ubicación");
                    showMessage("codigo:"+resultCode);
                 }
                break;
        }

    }

    /*Locaiton */
    private  boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        showMessage("Ocurrio un error al conectar el Proveedor GPS... !!:"+result);
       switch (result.getErrorCode()){
            case ConnectionResult.NETWORK_ERROR:
                if(mGoogleApiAvailability.isUserResolvableError(result.getErrorCode())){
                    mGoogleApiAvailability.getErrorDialog(this,result.getErrorCode(), REQUEST_RESOLVE_ERROR).show();
                    showMessage("revice su conexion a red");
                }
                break;
            case ConnectionResult.TIMEOUT:
                 if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting()){
                     mGoogleApiClient.connect();
                 }
                break;
        }

     }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        showMessage("Se establecio conexion con el Proveedor GPS...");
        initSettingLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        }

    private void showMessage(int idString){
      Log.d("nel_", getString(idString));
    }
    public static void showMessage(String idString){
        Log.d("nel_",":"+idString);
    }

    public class ActivityDetectionBroadcastRevicer extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();
            showMessage(action);
            if (action.equals(DetectedActivitiesIntentService.ACTION_LOCATION_UPDATE)){
                   Location location = (Location) intent.getParcelableExtra(DetectedActivitiesIntentService.EXTRA_LOCATION_UPDATE);
                   showMessage("Service: Nueva ubicacion: Lat="+location.getLatitude()+"/Log="+location.getLongitude());
                   mLastLocation=location;
                   updateUI();
                }else{
                    showMessage("location es nulll");
                }
            }
        }

}
