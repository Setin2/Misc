package com.example.hoply;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

/*
 * Class used for getting the geolocation
 */
class MyLocationListener{
    private FusedLocationProviderClient mFusedLocationClient;
    String latitude, longitude;
    private Context context;

    /*
     * Constructor
     */
    MyLocationListener(Context context){
        this.context = context;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
        getLastLocation();
    }

    /*
     * Get last known location and set the values of latitude and longitude
     */
    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        task -> {
                            Location location = task.getResult();
                            if (location == null) {
                                requestNewLocationData();
                            } else {
                                latitude = Double.toString(location.getLatitude());
                                longitude = Double.toString(location.getLongitude());
                            }
                        }
                );
            }
        } else {
            requestPermissions();
        }
    }

    /*
     * Request new location from the user
     */
    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );
    }


    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            latitude = Double.toString(mLastLocation.getLatitude());
            longitude = Double.toString(mLastLocation.getLongitude());
        }
    };

    /*
     * Return true if we have permission to read location
     */
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /*
     *  Request permission to read location
     */
    private void requestPermissions() {
        int PERMISSION_ID = 44;
        ActivityCompat.requestPermissions(
                (Activity) this.context,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    /*
     * Return true if location is enabled
     */
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }
}