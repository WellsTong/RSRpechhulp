package com.gaming.jeroen.rsrpechhulp.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.support.v4.app.ActivityCompat.*;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gaming.jeroen.rsrpechhulp.R;
import com.gaming.jeroen.rsrpechhulp.activities.RSRMainActivity;
import com.gaming.jeroen.rsrpechhulp.service.FetchAddressService;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


public class RSRMapFragment extends Fragment implements
        OnMapReadyCallback,
        OnRequestPermissionsResultCallback,
        GoogleMap.OnCameraMoveListener {

    private GoogleMap mMap;
    private MapView mapView;
    private Marker mapMarker, markerText;
    private LatLng position = new LatLng(52, 4);
    private Fragment telephoneFragment;
    private View customMarkerView;
    private TextView adresTextView;
    private float zoom;
    private float bearing;
    private FragmentManager manager;
    private String addressString;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.map_layout, container, false);
        mapView = (MapView) v.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();

        initializeComponents(v, container);
        return v;
    }

    private void initializeComponents(View v, ViewGroup container) {
        initMap();
        initTelephoneButton(v);
        initValuesZoomAndBearing();
        initAdresPopUpMarker(container);
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
    private void initAdresPopUpMarker(ViewGroup parent) {
        getActivity();
        this.customMarkerView = ((LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.popup_layout, parent, false);

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

        if (addressString == null) {
            this.addressString = getString(R.string.adres_text_map_popup);
        }
    }


    // initialiseren telefoon button
    private void initTelephoneButton(View v) {
        if (!RSRMainActivity.hasPhone) return;

        Button telefoonButton = (Button) v.findViewById(R.id.call_button);

        telefoonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!telephoneFragment.isAdded()) {
                    manager.beginTransaction().add(R.id.fragment_placeholder, telephoneFragment)
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
            infoButton.setImageAlpha(
                    getResources().getInteger(R.integer.image_alpha_zero)
            );
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
        if (mMap == null || getActivity() == null) return;
        mapMarker.setPosition(position);

        markerText.setPosition(position);
        markerText.setIcon(BitmapDescriptorFactory.fromBitmap(getMarkerBitmapFromView()));

        CameraPosition cameraPosition = new CameraPosition(mapMarker.getPosition(),
                zoom, getActivity().getResources().getInteger(R.integer.zero), bearing);
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    // set custom marker
    private void setCustomMarker() {
        markerText = mMap.addMarker(new MarkerOptions()
                .position(position)
                .icon(BitmapDescriptorFactory
                        .fromBitmap(getMarkerBitmapFromView())));
    }

    // creëer bitmap voor adres popup
    private Bitmap getMarkerBitmapFromView() {
        customMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        customMarkerView.layout(
                getActivity().getResources().getInteger(R.integer.zero),
                getActivity().getResources().getInteger(R.integer.zero),
                customMarkerView.getMeasuredWidth(),
                customMarkerView.getMeasuredHeight());

        customMarkerView.buildDrawingCache();
        Bitmap returnedBitmap = Bitmap.createBitmap(customMarkerView.getMeasuredWidth(),
                customMarkerView.getMeasuredHeight() + getActivity().getResources().getInteger(R.integer.height_custom_marker),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        canvas.drawColor(Color.WHITE, PorterDuff.Mode.SRC_IN);
        Drawable drawable = customMarkerView.getBackground();

        if (drawable != null) drawable.draw(canvas);

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
    private void startIntentService() {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), FetchAddressService.class);
        AddressResultReceiver mResultReceiver = new AddressResultReceiver(new Handler(), this, getActivity());
        intent.putExtra(getResources().getString(R.string.receiver), mResultReceiver);
        intent.putExtra(getResources().getString(R.string.location_data_extra), position);
        getActivity().startService(intent);
    }


    private void setAddress(String s) {
        this.addressString = s;
        adresTextView.setText(s);
    }

    // result receiver class om adres te ontvanger
    class AddressResultReceiver extends ResultReceiver {
        private final RSRMapFragment rsrMap;
        private final Activity activity;

        // initialiseren met handler en view om direct adres string in adres view te zetten
        AddressResultReceiver(Handler handler, RSRMapFragment rsrMap, Activity activity) {
            super(handler);
            this.rsrMap = rsrMap;
            this.activity = activity;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            String mAddressOutput = resultData.getString(activity.getResources().getString(R.string.result_data_key));

            if (resultCode == activity.getResources().getInteger(R.integer.succes_result)) {
                rsrMap.setAddress(mAddressOutput);
            }
        }
    }
}
