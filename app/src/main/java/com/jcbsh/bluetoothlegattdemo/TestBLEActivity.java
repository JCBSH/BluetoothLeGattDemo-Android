package com.jcbsh.bluetoothlegattdemo;

import android.app.Fragment;


public class TestBLEActivity extends SingleFragmentActivity {


    protected static final String LIFE_TAG = "life_DMainActivity";


    @Override
    protected String getLifeTag() {
        return LIFE_TAG;
    }

    @Override
    protected Fragment createFragment() {
        return TestBLEFragment.getInstance();
    }


}
