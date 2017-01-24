package com.gaming.jeroen.rsrpechhulp.fragments;

import android.support.v4.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;
import android.widget.ImageButton;

import com.gaming.jeroen.rsrpechhulp.R;
import com.gaming.jeroen.rsrpechhulp.activities.RSRMainActivity;


public class InfoFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.info_screen, container, false);

        // toolbar instellen
        setToolBar();
        return view;
    }

    private void setToolBar(){
        // als apparaat telefoon heeft de infobutton op toolbar onzichtbaar maken
        if(RSRMainActivity.hasPhone){
            ImageButton infoButton = (ImageButton) getActivity().findViewById(R.id.info_button);
            infoButton.setImageAlpha(0);
        }

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.main_bar);
        toolbar.setNavigationIcon(R.drawable.menu_arrow);
        toolbar.setTitle(R.string.over_rsr_text);

        // back button op toolbar initialiseren
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }


}
