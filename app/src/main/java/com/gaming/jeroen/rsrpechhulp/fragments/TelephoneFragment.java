package com.gaming.jeroen.rsrpechhulp.fragments;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.gaming.jeroen.rsrpechhulp.R;

public class TelephoneFragment extends Fragment{
    private final String TAG = "TelephoneFragment";

    Button makeCallButton;
    Button cancelButton;
    private String telephoneNnumber;


    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.telephone_screen, container, false);

        initializeComponents(v);

        return v;
    }

    private void initializeComponents(View v){
        //initialiseren buttons om te telefoneren of te cancellen
        initCallButton(v);

        //telefoonnummer string creëren
        telephoneNnumber = "tel:" + getResources().getString(R.string.telephone_number);
    }

    private void initCallButton(View v){
        makeCallButton = (Button)v.findViewById(R.id.make_call_button);
        makeCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

        // intent starten om te telefoneren
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse(telephoneNnumber));
                startActivity(callIntent);
            }
        });

        cancelButton = (Button)v.findViewById(R.id.cancel_telephone_call);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

    }
}
