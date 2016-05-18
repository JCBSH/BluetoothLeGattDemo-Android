package com.jcbsh.bluetoothlegattdemo;

import android.app.Fragment;
import android.os.Bundle;


public class MainActivity extends SingleFragmentActivity {

    protected static final String LIFE_TAG = "life_" + MainActivity.class.getSimpleName();


    @Override
    protected String getLifeTag() {
        return LIFE_TAG;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Fragment createFragment() {
        return MainFragment.getInstance();
    }




}
