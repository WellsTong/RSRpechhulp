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
import com.gaming.jeroen.rsrpechhulp.R;
import com.gaming.jeroen.rsrpechhulp.fragments.RSRMapFragment;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;


public class LocationHelper implements LocationListener {

    private final Context context;
    private final Activity activity;
    private final RSRMapFragment rsrMap;
    private LocationManager locationManager;
    private boolean isGPSEnabled = false;
    private int updateTime;
    private int updateDistance;
    private int maxSizeArraylist;
    private int arrayListSize;
    private final ArrayList<LatLng> positionsArrayList = new ArrayList<>();


    public LocationHelper(Context context, Activity activity, RSRMapFragment rsrMap) {
        this.context = context;
        this.activity = activity;
        this.rsrMap = rsrMap;

        setUpdateValues();
        initLocationMananger();
    }

    /* initialiseren van diverse start waardes */
    private void setUpdateValues(){
        this.updateTime = context.getResources().getInteger(R.integer.update_time_start);
        this.updateDistance = context.getResources().getInteger(R.integer.update_distance_start);
        this.maxSizeArraylist = context.getResources().getInteger(R.integer.intial_location_array_list_size);
        this.arrayListSize = maxSizeArraylist;
    }


    public void initLocationMananger() {

        /* controleren of app toestemming heeft voor GPS en netwerk locatie */
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            /* als er geen toestemming is die aan gebruiker vragen */
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, context.getResources().getInteger(R.integer.request_fine_location_state));
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, context.getResources().getInteger(R.integer.request_coarse_location_state));
            return;
        }


        try {
            /* locationonmanager initialiseren */
            locationManager = (LocationManager) context
                    .getSystemService(Context.LOCATION_SERVICE);

            /* checken of er network en GPS toegang is */
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            /* als er netwerk toegang is deze initialiseren */
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        updateTime, updateDistance, this);
            }

            /* als er GPS toegang is deze initialiseren */
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        updateTime, updateDistance, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     /* als GPS uitstaat gebruiker naar GPS instellingen brengen.
        alertdialog venster gecustomized */
    public void showSettingGPS() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity, android.R.style.Theme_Black);

        alertDialogBuilder
                .setMessage(R.string.geen_gps)
                .setCancelable(false)
                .setPositiveButton(context.getResources().getText(R.string.gps_alert_positve_button_text),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSsettings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                activity.startActivityForResult(callGPSsettings,
                                        context.getResources().getInteger(R.integer.request_fine_location_state) );
                            }
                        });

        alertDialogBuilder.setNegativeButton(context.getResources().getText(R.string.annuleren_text),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        activity.finish();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        if(alertDialog.getWindow() != null){
            alertDialog.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
        }
    }

    /* locatie updates uitzetten als app stopt */
    public void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.removeUpdates(this);
    }

    /* controleren of netwerk beschikbaar is */
    public boolean isNetworAvailable() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /* controleren of GPS beschikbaar is */
    public boolean isGPSAvailable() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public boolean isGPSEnabled() {
        return isGPSEnabled;
    }

    /* Nieuw binnen gekomen locatie aan lijst toevoegen en daar het gemiddelde van nemen
     en met dit gemiddelde de map location updaten.
     Als arraylist met positionsArrayList een size van 10 heeft bereikt
     locatiamanager opnieuw initialiseren met nieuwe waardes:
     - update time verhogen van 0 naar 3000 milliseconds
     - distance verhogen van 0 meter naar 20 meter
     en maximale size van arraylist verlagen van 10 naar 3. */

    @Override
    public void onLocationChanged(Location location) {

        LatLng newPosition = new LatLng(location.getLatitude(), location.getLongitude());

        if (positionsArrayList.size() >= arrayListSize) {
            positionsArrayList.remove(0);
        }
        positionsArrayList.add(newPosition);

        int arraySize = positionsArrayList.size();

        double lat = context.getResources().getInteger(R.integer.zero);
        double lon = context.getResources().getInteger(R.integer.zero);
        for (LatLng pos : positionsArrayList) {
            lat += pos.latitude;
            lon += pos.longitude;
        }
        lat /= arraySize;
        lon /= arraySize;
        newPosition = new LatLng(lat, lon);
        rsrMap.updateLocation(newPosition);

        if (arraySize == maxSizeArraylist) {
            positionsArrayList.clear();
            positionsArrayList.add(newPosition);
            arrayListSize = context.getResources().getInteger(R.integer.array_list_size_running);
            updateTime = context.getResources().getInteger(R.integer.update_time_running_in_milliseconds);
            updateDistance = context.getResources().getInteger(R.integer.update_distance_running);
            initLocationMananger();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider){
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

}
