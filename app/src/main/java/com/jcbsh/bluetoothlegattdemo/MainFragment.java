package com.jcbsh.bluetoothlegattdemo;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by JCBSH on 23/02/2016.
 */
public class MainFragment extends Fragment{
    private Button mTestBLEButton;

    public static Fragment getInstance() {
        Fragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);

        mTestBLEButton = (Button) v.findViewById(R.id.test_ble_button);
        mTestBLEButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), TestBLEActivity.class);
                startActivity(intent);

            }
        });


        return v;
    }
}
