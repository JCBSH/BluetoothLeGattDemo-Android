package com.jcbsh.bluetoothlegattdemo;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by JCBSH on 24/02/2016.
 */
public class Characteristics {
    private String mName;
    private String mId;
    private BluetoothGattCharacteristic mCharacteristic = null;

    public Characteristics(String name, String Id) {
        mName = name;
        mId = Id;
    }
    public BluetoothGattCharacteristic getCharacteristic() {
        return mCharacteristic;

    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        mCharacteristic = characteristic;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }


}
