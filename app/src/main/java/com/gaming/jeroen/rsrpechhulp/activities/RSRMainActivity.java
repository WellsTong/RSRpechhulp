package com.gaming.jeroen.rsrpechhulp.activities;

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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import com.gaming.jeroen.rsrpechhulp.util.LocationHelper;
import com.gaming.jeroen.rsrpechhulp.R;
import com.gaming.jeroen.rsrpechhulp.fragments.RSRMapFragment;
import com.gaming.jeroen.rsrpechhulp.fragments.InfoFragment;

public class RSRMainActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener {

    private FragmentManager manager;
    private Fragment infoFragment;
    private Button alarmButton;
    private Button infoButton;
    private RSRMapFragment rsrMap;
    private LocationHelper locationHelper;
    public static boolean hasPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_screen);

        // verschillende onderdelen initialiseren
        initialize();
    }

    private void initialize() {
        // initialiseren hasPhone value (flase)
        RSRMainActivity.hasPhone = getResources().getBoolean(R.bool.intial_hasPhone_value);

        initializePackageManager();

        initializeFragmentsAndButtons();

        initGPSandNetwork();
    }

    private void initializeFragmentsAndButtons() {
        // initialiseren fragments
        infoFragment = new InfoFragment();
        rsrMap = new RSRMapFragment();

        // button main layout initialeren
        initButtons();

        // custom toolbar opzetten
        setToolBar();
    }

    private void initializePackageManager() {
        // controleren of apparaat telefoon heeft en vastleggen
        PackageManager packageManager = getPackageManager();
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            RSRMainActivity.hasPhone = true;

            // als apparaat telefoon heeft permissie controleren
            checkPermissionPhoneCall();
        } else {
            RSRMainActivity.hasPhone = false;
        }

        // initialiseren fargmentmanager en BackStackListeneer
        manager = this.getSupportFragmentManager();
        manager.addOnBackStackChangedListener(this);
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
                    manager.beginTransaction().replace(R.id.fragment_placeholder, rsrMap)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });

        // als apparaat geen telefoon magelijkheden heeft infobutton initiailiseren
        if (!RSRMainActivity.hasPhone) {
            infoButton = (Button) this.findViewById(R.id.info_button_large);

            infoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!infoFragment.isAdded()) {
                        manager.beginTransaction()
                                .replace(R.id.fragment_placeholder, infoFragment)
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        getSupportActionBar().setTitle(R.string.main_toolbar_text);

        // als apparaat telefoon heeft infobutton op toolbar inialiseren
        if (RSRMainActivity.hasPhone) {
            ImageButton infoButton = (ImageButton) findViewById(R.id.info_button);
            infoButton.setImageAlpha(getResources().getInteger(R.integer.image_alpha_max));
            infoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!infoFragment.isAdded()) {
                        manager.beginTransaction()
                                .replace(R.id.fragment_placeholder, infoFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
            });
        }
    }

    // gebruiker vragen internetverbinding te maken
    private void showNetworkDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, android.R.style.Theme_Black);
        alertDialogBuilder
                .setMessage(R.string.geen_internet)
                .setCancelable(false)
                .setPositiveButton(R.string.probeer_opnieuw_text,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                initGPSandNetwork();
                            }
                        });
        alertDialogBuilder.setNegativeButton(R.string.annuleren_text,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
        }
    }

    // gebruiker vragen of deze app gebruik mag maken van telefoon diensten
    private void checkPermissionPhoneCall() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CALL_PHONE}, getResources().getInteger(R.integer.request_call_phone));
        }
    }

    @Override
    public void onBackStackChanged() {
        // bij elke wissel van fragment controleren of de verbindingen nog actief zijn
        // zo niet dan opnieuw initialiseren
        if (!checkConnectivity()) {
            initGPSandNetwork();
        }

        // index backstack opvragen
        int index = manager.getBackStackEntryCount();

        // als index is root-value (0) alarmbutton zichtbaar maken en toolbar instellen
        if (index == getResources().getInteger(R.integer.root_value)) {
            if (alarmButton != null) {
                alarmButton.setVisibility(View.VISIBLE);
            }

            if (!hasPhone && infoButton != null){
                infoButton.setVisibility(View.VISIBLE);
            }

            setToolBar();
        } else {
            if (alarmButton != null) {
                alarmButton.setVisibility(View.GONE);
            }

            if (!hasPhone && infoButton != null){
                infoButton.setVisibility(View.GONE);
            }
        }

    }

    // als gebruiker instellingen heeft veranderd opnieuw GPS en internet initialiseren
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == getResources().getInteger(R.integer.request_fine_location_state)) {
            initGPSandNetwork();
        }
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
}
