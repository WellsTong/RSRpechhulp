package com.gaming.jeroen.rsrpechhulp.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.gaming.jeroen.rsrpechhulp.R;
import com.gaming.jeroen.rsrpechhulp.util.Constants;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class FetchAddressService extends IntentService {
    private final String TAG = "FetchAddressService";
    protected ResultReceiver mReceiver;
    private final int NUMBER_OF_ADDRESSES = 1;

   public FetchAddressService(){
       super("FetchAddressService");
   }



    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";

        // geocoder om adres te verkrijgen initialiseren
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        // latitude en longitude verkrijgen uit intent
        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        LatLng location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);

        List<Address> addresses = null;

        try{
            // mogelijke adressen binnenhalen, in dit geval maar
            addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    NUMBER_OF_ADDRESSES );

        }catch (IOException e){
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, e);
        }catch (IllegalArgumentException illegalArgument){
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
            "Latitude = " + location.latitude + ", longitude = "
                    + location.longitude, illegalArgument);
        }


        if (addresses == null || addresses.size()  == 0) {

            // Als er geen adres gevonden is error bericht maken en loggen en resultaat opsturen
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            // Adressen verkrijgen en aan string arraylist toevoegen
            // en vervolgens opsturen naar ontvanger
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"),
                            addressFragments));
        }
    }

          // resultaat geocoder en eventuelen adressen opsturen
    private void deliverResultToReceiver(int resultCode, String message){
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }
}
