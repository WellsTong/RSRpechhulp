package com.gaming.jeroen.rsrpechhulp.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gaming.jeroen.rsrpechhulp.R;
import com.gaming.jeroen.rsrpechhulp.activities.RSRMainActivity;
import com.gaming.jeroen.rsrpechhulp.service.FetchAddressService;
import com.gaming.jeroen.rsrpechhulp.util.Constants;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;

import android.support.v4.app.ActivityCompat.*;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


public class RSRMapFragment extends Fragment implements
        OnMapReadyCallback,
        OnRequestPermissionsResultCallback,
        GoogleMap.OnCameraMoveListener {

    private String TAG = "RSRMapFragment";
    private GoogleMap mMap;
    private MapView mapView;
    private Marker mapMarker, markerText;
    private LatLng position = new LatLng(52, 4);
    private Button telefoonButton;
    private Fragment telephoneFragment;
    private View view;
    private View customMarkerView;
    private TextView adresTextView;
    private float zoom;
    private float bearing;
    private FragmentManager manager;
    private FragmentTransaction transaction;
    private String addressString;

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.map_layout, container, false);
        mapView = (MapView) v.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        view = v;

        initializeComponents(v);

        return v;
    }

    private void initializeComponents(View v) {
        initMap();
        initTelephoneButton(v);
        initValuesZoomAndBearing();
        initAdresPopUpMarker();
        setAddress(addressString);

        // customize toolbar
        setToolBar();

        // initialiseren telefoon fragment en fragmentmanager
        telephoneFragment = new TelephoneFragment();

        manager = getActivity().getSupportFragmentManager();
    }

    private void initMap() {
        // Mapinitializer initialiseren
        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // creëer googlemao asynchroon (buiten main thread)
        mapView.getMapAsync(this);
    }

        // initialiseren adres popup
    private void initAdresPopUpMarker() {
        this.customMarkerView = ((LayoutInflater) getActivity().getSystemService(
                getActivity().LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.popup_layout, null);

        this.adresTextView = (TextView) customMarkerView.findViewById(R.id.adres_text_map);
    }

    // waardes for zoom and bearing ophalen uit resources en instellen
    private void initValuesZoomAndBearing() {
        TypedValue zoomValue = new TypedValue();
        getResources().getValue(R.dimen.zoom_value, zoomValue, true);
        this.zoom = zoomValue.getFloat();

        TypedValue bearingValue = new TypedValue();
        getResources().getValue(R.dimen.bearing_value, bearingValue, true);
        this.bearing = bearingValue.getFloat();

        if(addressString == null){
            this.addressString = getString(R.string.adres_text_map_popup);
        }
    }


    // initialiseren telefoon button
    private void initTelephoneButton(View v) {
        if (!RSRMainActivity.hasPhone) return;

        telefoonButton = (Button) v.findViewById(R.id.call_button);

        telefoonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!telephoneFragment.isAdded()) {
                    transaction = manager.beginTransaction();
                    transaction.add(R.id.fragment_placeholder, telephoneFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
    }

    // toolbar instellen
    private void setToolBar() {
        if (RSRMainActivity.hasPhone) {
            ImageButton infoButton = (ImageButton) getActivity().findViewById(R.id.info_button);
            infoButton.setImageAlpha(0);
        }

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.main_bar);
        toolbar.setNavigationIcon(R.drawable.menu_arrow);
        toolbar.setTitle(R.string.app_name);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }

    // googlemap en mapMarker instellen
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraMoveListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);

        mapMarker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker)));

        setCustomMarker();
        updateLocation(position);
    }

    // locatie uodaten en aan map toevoegen en intent service starten
    public void updateLocation(LatLng newPosition) {
        position = newPosition;
        startIntentService();
        if (mMap == null) return;
        mapMarker.setPosition(position);

        markerText.setPosition(position);
        markerText.setIcon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView()));

        CameraPosition cameraPosition = new CameraPosition(mapMarker.getPosition(), zoom, 0, bearing);
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
    // set custom marker
    public void setCustomMarker() {
        markerText = mMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory
                        .fromBitmap(getMarkerBitmapFromView())));
    }

    // creëer bitmap voor adres popup
    private Bitmap getMarkerBitmapFromView() {
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(0, 0, customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight());
        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(), customMarkerView.getMeasuredHeight() + 110,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable = customMarkerView.getBackground();
        if (drawable != null)
            drawable.draw(canvas);
        customMarkerView.draw(canvas);
        return returnedBitmap;
    }

    // als op map handmatig wordt ingezoomd of gedraaid, zoom factor en hoek (bearing) opslaan en gebruiken
    @Override
    public void onCameraMove() {
        this.zoom = mMap.getCameraPosition().zoom;
        this.bearing = mMap.getCameraPosition().bearing;
    }

    // intentservice starten om adres te verkrijgen
    public void startIntentService() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), FetchAddressService.class);
        AddressResultReceiver mResultReceiver = new AddressResultReceiver(new Handler(), this);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, position);
        getActivity().startService(intent);
    }


    public void setAddress(String s) {
        this.addressString = s;
        adresTextView.setText(s);
    }

    // result receiver class om adres te ontvanger
    class AddressResultReceiver extends ResultReceiver {
        private RSRMapFragment rsrMap;

        // initialiseren met handler en view om direct adres string in adres view te zetten
        AddressResultReceiver(Handler handler, RSRMapFragment rsrMap) {
            super(handler);
            this.rsrMap = rsrMap;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            String mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);

            if (resultCode == Constants.SUCCESS_RESULT) {
                rsrMap.setAddress(mAddressOutput);
            }
        }
    }
}
