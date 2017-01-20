package com.gaming.jeroen.rsrpechhulp;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.support.v7.widget.Toolbar;

public class RSRMainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private final String TAG = "RSRMainActivity";
    FragmentManager manager;
    FragmentTransaction transaction;
    Fragment infoFragment;
    Button alarmButton;
    Button infoButtonLarge;
    RSRMap rsrMap;
    LocationHelper locationHelper;
    PackageManager packageManager;
    public static boolean hasPhone = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        // controleren of apparaat telefoon heeft en vastleggen
        packageManager = getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            RSRMainActivity.hasPhone = true;

        // als apparaat telefoon heeft permissie controleren
            checkPermissionPhoneCall();
        } else {
            RSRMainActivity.hasPhone = false;
        }

        // initialiseren fragments
        infoFragment = new InfoFragment();
        rsrMap = new RSRMap();

        // initialiseren fargmentmanager en BackStackListeneer
        manager = this.getSupportFragmentManager();
        manager.addOnBackStackChangedListener(this);

        // custom toolbar opzetten
        setToolBar();

        // initialiseren Locatie en Internet
        initGPSandNetwork();

        // button main layout initialeren
        initButtons();
    }

    private boolean checkInternetAccess() {

        // controleren of apparaat internettoegang heeft
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    private void initGPSandNetwork() {

        // internet toegang controleren
        boolean access = checkInternetAccess();
        if (!access) {

            // waarneer geen internet gebruiker vragen internet beschikbaar te maken
            showNetworkDialog();
        }

        // locatie class initialiseren
        locationHelper = new LocationHelper(getApplicationContext(), this, rsrMap);

        // als GSP uit staat gebruiker vragen GPS aan te zetten
        if (!locationHelper.isGPSEnabled()) {
            locationHelper.showSettingGPS();
        }
    }

    // internet toegang en GPS controleren
    private boolean checkConnectivity() {
        return checkInternetAccess() && locationHelper.isGPSAvailable() && locationHelper.isNetworAvailable();
    }

    // alarmbutton initialiseren en functie vastleggen
    private void initButtons() {
        alarmButton = (Button) this.findViewById(R.id.alarm_button);

        alarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (!rsrMap.isAdded()) {
                    transaction = manager.beginTransaction();
                    transaction.replace(R.id.fragment_placeholder, rsrMap)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        // als apparaat geen telefoon magelijkheden heeft infobutton initiailiseren
        if (!RSRMainActivity.hasPhone) {
            infoButtonLarge = (Button) this.findViewById(R.id.info_button_large);

            infoButtonLarge.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!infoFragment.isAdded()) {
                        transaction = manager.beginTransaction();
                        transaction.replace(R.id.fragment_placeholder, infoFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            });
        }
    }

    // toolbar instellen
    private void setToolBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_bar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(R.string.main_toolbar_text);

        // als apparaat telefoon heeft infobutton op toolbar inialiseren
        if (RSRMainActivity.hasPhone) {
            ImageButton infoButton = (ImageButton) findViewById(R.id.info_button);
            infoButton.setImageAlpha(255);
            infoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!infoFragment.isAdded()) {
                        transaction = manager.beginTransaction();
                        transaction.replace(R.id.fragment_placeholder, infoFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            });
        }
    }

        // gebruiker vragen internetverbinding te maken
    public void showNetworkDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, android.R.style.Theme_Black);
        alertDialogBuilder
                .setMessage(R.string.geen_internet)
                .setCancelable(false)
                .setPositiveButton("Probeer opnieuw",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                initGPSandNetwork();
                            }
                        });
        alertDialogBuilder.setNegativeButton("Annuleren",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        alertDialog.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
    }

        // gebruiker vragen of deze app gebruik mag maken van telefoon diensten
    private void checkPermissionPhoneCall() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CALL_PHONE}, Constants.REQUEST_CALL_PHONE);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onBackStackChanged() {
        // bij elke wissel van fragment controleren of de verbindingen nog actief zijn
        // zo niet dan opnieuw initialiseren
        if (!checkConnectivity()) {
            Log.i(TAG, "init again");
            initGPSandNetwork();
        }

        // index backstack opvragen
        int index = manager.getBackStackEntryCount();

        // als index 0 is (root) alarmbutton zichtbaar maken en toolbar instellen
        if (index == 0) {
            if (alarmButton != null) {
                alarmButton.setVisibility(View.VISIBLE);
            }
            setToolBar();
        } else {
            if (alarmButton != null) {
                alarmButton.setVisibility(View.GONE);
            }
        }

    }

     // als gebruiker instellingen heeft veranderd opnieuw GPS en internet initialiseren
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_FINE_LOCATION_STATE) {
            initGPSandNetwork();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        locationHelper.initLocationMananger();
        super.onPostResume();
    }

    // als app wordt gestop locatie updates uitzetten
    @Override
    protected void onPause() {
        locationHelper.stopLocationUpdates();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
