package com.komodo.mygdxgame;

import android.os.Bundle;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;



public class MainActivity extends AndroidApplication {
	public static SensorFusionListener sensorFused;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
        cfg.useGL20 = true;
      //  cfg.useCompass = true;
        
        sensorFused = new SensorFusionListener(this); 
        initialize(new GhostEscape(), cfg);
    }
}