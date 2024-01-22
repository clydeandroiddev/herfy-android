package com.herfy.location;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class FusedLocationProvider implements LocationListener {

    private Context context;
    
    private FusedLocationProviderClient fusedLocationProviderClient;
    
    private LocationRequest locationRequest;
    
    private LocationCallback locationCallback;
    
    private LocationResultCallback locationResultCallback;
    
    public abstract interface LocationResultCallback {

        public void getNewFusedLocation(Location location);

    }

    public FusedLocationProvider(Context context) {

        this.context = context;
    
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient( this.context);

    }

    public void setLocationResultCallback(LocationResultCallback locationResultCallback) {
        this.locationResultCallback = locationResultCallback;
    }

    public void connect() {
        
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        
            locationRequest = new LocationRequest();
            locationRequest.setInterval(60000);
            locationRequest.setFastestInterval(60000);
            locationRequest.setSmallestDisplacement(0);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    
            locationCallback = new LocationCallback() {
    
                @Override
                public void onLocationResult(LocationResult locationResult) {
        
                    for(Location location : locationResult.getLocations()) {
            
                        System.out.println(location.toString());
                        
                        locationResultCallback.getNewFusedLocation(location);
                        
                    }
        
                }
    
            };
            
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback,null);
            
        }
  
    }

    public void disconnect() {
    
        if(fusedLocationProviderClient != null) {
    
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    
        }

    }
    
    @Override
    public void onLocationChanged(Location location) {

        if(locationResultCallback != null) {
            locationResultCallback.getNewFusedLocation(location);
        }
        
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {



    }

    @Override
    public void onProviderEnabled(String provider) {



    }

    @Override
    public void onProviderDisabled(String provider) {



    }

}
