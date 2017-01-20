package com.gaming.jeroen.rsrpechhulp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends Activity {

    private final int SPLASH_TIME = 800;

    //splash screen tonen en 800 milliseconden laten zien

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, RSRMainActivity.class));
                SplashActivity.this.finish();
            }
        }, SPLASH_TIME);
    }
}
