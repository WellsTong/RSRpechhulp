package com.gaming.jeroen.rsrpechhulp;

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

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;


class LocationHelper implements LocationListener {

    private String TAG = "LocationHelper";
    private final Context context;
    private final Activity activity;
    private final RSRMap rsrMap;
    private LocationManager locationManager;
    private Location location = null;
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private int updateTime = 0;
    private int minDistanceUpdate = 0;
    private int maxSizeArraylist = 10;
    private ArrayList<LatLng> positions = new ArrayList<>();


    LocationHelper(Context context, Activity activity, RSRMap rsrMap) {
        this.context = context;
        this.activity = activity;
        this.rsrMap = rsrMap;
        initLocationMananger();
    }


    void initLocationMananger() {
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
                        updateTime, minDistanceUpdate, this);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            }

            // als er GPS toegang is deze initialiseren
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        updateTime, minDistanceUpdate, this);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

        // als GPS uitstaat gebruiker naar GPS instellingen brengen
        // alertdialog venster beetje gecustomized
    void showSettingGPS() {
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
    void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

        // controleren of netwerk beschikbaar is
    boolean isNetworAvailable() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

        // controleren of GPS beschikbaar is
    boolean isGPSAvailable() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


        // nieuw binnen gekomen locatie aan lijst toevoegen en daar het gemiddelde van nemen
        // en met dit gemiddelde de map location updaten
        // als lijst langer is dan 10 locaties eerste entry wissen en nieuwe locatie toevoegen
    @Override
    public void onLocationChanged(Location location) {
        LatLng newPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (positions.size() >= maxSizeArraylist) {
            positions.remove(0);
        }
        positions.add(newPosition);

        double lat = 0;
        double lon = 0;
        for (LatLng pos : positions) {
            lat += pos.latitude;
            lon += pos.longitude;
        }

        lat /= positions.size();
        lon /= positions.size();
        newPosition = new LatLng(lat, lon);

        if (positions.size() == 10){
            positions.clear();
            positions.add(newPosition);
            maxSizeArraylist = 3;
            updateTime = 30_000;
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

    public boolean isNetworkEnabled() {
        return isNetworkEnabled;
    }

    boolean isGPSEnabled() {
        return isGPSEnabled;
    }
}
