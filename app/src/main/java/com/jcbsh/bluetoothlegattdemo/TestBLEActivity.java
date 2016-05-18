package com.jcbsh.bluetoothlegattdemo;

import android.app.Fragment;
import android.os.Bundle;
import android.view.MenuItem;


public class TestBLEActivity extends SingleFragmentActivity {


    protected static final String LIFE_TAG = "life_DMainActivity";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    protected String getLifeTag() {
        return LIFE_TAG;
    }

    @Override
    protected Fragment createFragment() {
        return TestBLEFragment.getInstance();
    }


}
