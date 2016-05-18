package com.jcbsh.bluetoothlegattdemo;

import android.app.Fragment;


public class MainActivity extends SingleFragmentActivity {

    protected static final String LIFE_TAG = "life_" + MainActivity.class.getSimpleName();


    @Override
    protected String getLifeTag() {
        return LIFE_TAG;
    }

    @Override
    protected Fragment createFragment() {
        return MainFragment.getInstance();
    }




}
