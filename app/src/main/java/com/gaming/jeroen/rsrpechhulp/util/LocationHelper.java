package com.gaming.jeroen.rsrpechhulp.util;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.gaming.jeroen.rsrpechhulp.R;
import com.gaming.jeroen.rsrpechhulp.fragments.RSRMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;


public class LocationHelper implements LocationListener {

    private String TAG = "LocationHelper";
    private final Context context;
    private final Activity activity;
    private final RSRMapFragment rsrMap;
    private LocationManager locationManager;
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private int updateTime;
    private int updateDistance;
    private int maxSizeArraylist;
    private int arrayListSize;
    private ArrayList<LatLng> positions = new ArrayList<>();


    public LocationHelper(Context context, Activity activity, RSRMapFragment rsrMap) {
        this.context = context;
        this.activity = activity;
        this.rsrMap = rsrMap;

        setUpdateValues();
        initLocationMananger();
    }

    private void setUpdateValues(){
        this.updateTime = context.getResources().getInteger(R.integer.update_time_start);
        this.updateDistance = context.getResources().getInteger(R.integer.update_distance_start);
        this.maxSizeArraylist = context.getResources().getInteger(R.integer.array_list_size_to_start);
        this.arrayListSize = maxSizeArraylist;
    }


    public void initLocationMananger() {
        // controleren of app toestemming heeft voor GPS en netwerk locatie
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // als er geen toestemming is die aan gebruiker vragen
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, Constants.REQUEST_FINE_LOCATION_STATE);
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, Constants.REQUEST_COARSE_LOCATION_STATE);
            return;
        }


        try {
            // locationonmanager initialiseren
            locationManager = (LocationManager) context
                    .getSystemService(Context.LOCATION_SERVICE);

            // checken of er network en GPS toegang is
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            // als er netwerk toegang is deze initialiseren
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        updateTime, updateDistance, this);
            }

            // als er GPS toegang is deze initialiseren
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        updateTime, updateDistance, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // als GPS uitstaat gebruiker naar GPS instellingen brengen
    // alertdialog venster beetje gecustomized
    public void showSettingGPS() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity, android.R.style.Theme_Black);

        alertDialogBuilder
                .setMessage(R.string.geen_gps)
                .setCancelable(false)
                .setPositiveButton("Ga naar instellingen",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSsettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                activity.startActivityForResult(callGPSsettings, Constants.REQUEST_FINE_LOCATION_STATE);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Annuleren",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        activity.finish();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialog.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
    }

    // locatie updates uitzetten als app stopt
    public void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

    // controleren of netwerk beschikbaar is
    public boolean isNetworAvailable() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    // controleren of GPS beschikbaar is
    public boolean isGPSAvailable() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isGPSEnabled() {
        return isGPSEnabled;
    }

    // nieuw binnen gekomen locatie aan lijst toevoegen en daar het gemiddelde van nemen
    // en met dit gemiddelde de map location updaten
    // als arraylist met positions een size van 10 heeft bereikt
    // locatiamanager opnieuw initialiseren met nieuwe waardes
    // en maximale size van arraylist verlagen naar 3

    @Override
    public void onLocationChanged(Location location) {
        LatLng newPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (positions.size() >= arrayListSize) {
            positions.remove(0);
        }
        positions.add(newPosition);

        int arraySize = positions.size();

        double lat = 0;
        double lon = 0;
        for (LatLng pos : positions) {
            lat += pos.latitude;
            lon += pos.longitude;
        }

        lat /= arraySize;
        lon /= arraySize;
        newPosition = new LatLng(lat, lon);

        if (arraySize == maxSizeArraylist) {
            positions.clear();
            positions.add(newPosition);
            arrayListSize = context.getResources().getInteger(R.integer.array_list_size_running);
            updateTime = context.getResources().getInteger(R.integer.update_time_running);
            updateDistance = context.getResources().getInteger(R.integer.update_distance_running);
            initLocationMananger();
        }

        rsrMap.updateLocation(newPosition);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "onProviderEnabled");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "onProviderDisabled");
    }


}
