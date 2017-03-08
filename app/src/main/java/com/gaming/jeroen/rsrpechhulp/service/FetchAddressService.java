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
import com.google.android.gms.maps.model.LatLng;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FetchAddressService extends IntentService {
    private ResultReceiver mReceiver;

    public FetchAddressService() {
        super("FetchAddressService");
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";

        /* geocoder om adres te verkrijgen initialiseren */
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        /* latitude en longitude verkrijgen uit intent */
        mReceiver = intent.getParcelableExtra(getApplicationContext().getResources().getString(R.string.receiver));
        LatLng location = intent.getParcelableExtra(getApplicationContext().getResources().getString(R.string.location_data_extra));

        List<Address> addresses = null;

        String TAG = "FetchAddressService";
        try {

            /*  mogelijke adres(sen) binenhalen, in dit geval maar 1 */

            addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1);

        } catch (IOException e) {
            errorMessage = getString(R.string.service_not_available);
            Log.e(TAG, errorMessage, e);
        } catch (IllegalArgumentException illegalArgument) {
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.latitude + ", longitude = "
                    + location.longitude, illegalArgument);
        }


        if (addresses == null || addresses.size() == R.integer.zero) {

            /* Als er geen adres gevonden is error bericht maken en loggen,
            resultaat opsturen */
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(getResources().getInteger(R.integer.faillure_result), errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

           /* Adres verkrijgen en aan string arraylist toevoegen
            en vervolgens opsturen naar ontvanger  */
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            addressFragments.add(address.getCountryName());

            deliverResultToReceiver(getResources().getInteger(R.integer.succes_result),
                    TextUtils.join(System.getProperty("line.separator"),
                            addressFragments));
        }
    }

     /* resultaat geocoder en eventuelen adressen opsturen */
    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.result_data_key), message);
        mReceiver.send(resultCode, bundle);
    }
}
