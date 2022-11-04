package com.example.footballfinder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;

import com.example.footballfinder.classes.Field;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.example.footballfinder.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final int MY_REQUEST_INT = 177;
    public static boolean active = false;
    private Map<Marker, Field> markerMap = new HashMap<>();
    private GoogleMap event_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        active = true;
        super.onStart();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onResume() {
        reloadEvents();
        super.onResume();
    }

    private CameraUpdate getCurrentLocation(){
        if(ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            CameraUpdate point = CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 12.0f);
            return point;
        }
        return CameraUpdateFactory.newLatLngZoom(new LatLng(55.4038, 10.4024), 12.0f);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        map.setOnMarkerClickListener(this);
        // Here We want to check the permission of Location - GPS
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, MY_REQUEST_INT);
            }
        } else {
            // Here the code of Grand Permission
            map.setMyLocationEnabled(true);
        }
        map.moveCamera(getCurrentLocation());
        event_map = map;
        loadFieldsIntoMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.show_all){
            markerMap.forEach( (marker, field) -> {
                marker.setVisible(true);
            });
        }
        if(item.getItemId() == R.id.show_events){
            markerMap.forEach( (marker, field) -> {
                if(!field.event_today){
                    marker.setVisible(false);
                }
            });
        }
        if(item.getItemId() == R.id.reload_events){
            reloadEvents();
        }

        if(item.getItemId() == R.id.my_joined_events){
            Intent intent = new Intent(this, MyJoinedEventsActivity.class);
            startActivity(intent);
        }

        if(item.getItemId() == R.id.logout){
            active = false;
            finish();
        }


        return true;
    }

    private void loadFieldsIntoMap(){
        Field.getAllFields(this, fields -> {
            for(int i = 0; i < fields.size(); i++){
                Field field = fields.get(i);
                float color = BitmapDescriptorFactory.HUE_RED;
                if(field.event_today){
                    color = BitmapDescriptorFactory.HUE_GREEN;
                }
                MarkerOptions marker = new MarkerOptions()
                        .position(new LatLng(field.lat, field.lon))
                        .title(String.valueOf(field.id))
                        .icon(BitmapDescriptorFactory.defaultMarker(color));
                Marker m = event_map.addMarker(marker);
                markerMap.put(m, field);
            }
        }, error -> {
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void reloadEvents(){
        markerMap.forEach( (marker, field) -> {
            marker.remove();
        });
        markerMap.clear();
        loadFieldsIntoMap();
    }
    /*
     * Open MakerEvents activity. Contains a recyclerview with the events for this field
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        Intent intent = new Intent(this, MarkerEvents.class);
        intent.putExtra("fieldID", Integer.parseInt(marker.getTitle()));
        startActivity(intent);
        return true;
    }


    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}