/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jcbsh.bluetoothlegattdemo;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class LaserGattAttributes {
    private HashMap<String, Characteristics> attributes;


    public static final String DEVICE_NAME =        "nRF51-DK        ";
    public static final String SERVICE_LED =        "LED SERVICE     ";
    public static final String SERVICE_LASER =      "LASER SERVICE   ";
    public static final String SERVICE_MOTOR =      "MOTOR SERVICE   ";

    public static final String CHAR_LED_TYPE =      "LED TYPE        ";
    public static final String CHAR_LED_LEVEL =     "LED LEVEL       ";
    public static final String CHAR_LASER_STATUS =  "LASER STATUS    ";
    public static final String CHAR_MOTOR_CPOS =    "MOTOR CPOS      ";
    public static final String CHAR_MOTOR_IPOS =    "MOTOR IPOS      ";
    public static final String CHAR_MOTOR_MMODE =   "MOTOR MMODE     ";
    public static final String CHAR_MOTOR_SMODE =   "MOTOR SMODE     ";


    public static final String DEVICE_NAME_KEY = "device name";
    public static final String SERVICE_LED_KEY = "led service";
    public static final String SERVICE_LASER_KEY = "laser service";
    public static final String SERVICE_MOTOR_KEY =  "motor service";



    public static final String CHAR_LED_TYPE_KEY =      "led type";
    public static final String CHAR_LED_LEVEL_KEY =     "led level";
    public static final String CHAR_LASER_STATUS_KEY =  "laser status";
    public static final String CHAR_MOTOR_CPOS_KEY =    "motor Current Position";
    public static final String CHAR_MOTOR_IPOS_KEY =    "motor Intended Position";
    public static final String CHAR_MOTOR_MMODE_KEY =   "motor MMode";
    public static final String CHAR_MOTOR_SMODE_KEY =   "motor SMode";


    private final String[] ID_STRINGS = {CHAR_LED_TYPE, CHAR_LED_LEVEL, CHAR_LASER_STATUS,
            CHAR_MOTOR_CPOS, CHAR_MOTOR_IPOS, CHAR_MOTOR_MMODE, CHAR_MOTOR_SMODE};

    private final String[] KEYS = {CHAR_LED_TYPE_KEY, CHAR_LED_LEVEL_KEY,
            CHAR_LASER_STATUS_KEY, CHAR_MOTOR_CPOS_KEY,
            CHAR_MOTOR_IPOS_KEY, CHAR_MOTOR_MMODE_KEY,
            CHAR_MOTOR_SMODE_KEY};


    public LaserGattAttributes() {
        init();
    }

    private void init() {
        attributes  = new HashMap();

        for (int i = 0; i < KEYS.length; i++) {
            String uuid = getUUIDInString(ID_STRINGS[i]);
            Characteristics characteristics = new Characteristics(KEYS[i], uuid);
            attributes.put(KEYS[i], characteristics);
        }


        //String uuid = getUUIDInString(DEVICE_NAME);
    }



    public Characteristics getCharacteristicsByName(String name) {
       return attributes.get(name);
    }

    public Characteristics getCharacteristicsByID(String ID) {
        for (Characteristics characteristics: attributes.values()) {
            if (characteristics.getId().equals(ID)) return characteristics;
        }

        return null;
    }

    public static String getUUIDInString (String parseString) {
        byte[] b = parseString.getBytes(StandardCharsets.UTF_8);
        UUID id = UUID.nameUUIDFromBytes(b);
        //Log.d("static", id.toString());

        final StringBuilder stringBuilder = new StringBuilder(b.length);
        for (byte byteChar : b)
            stringBuilder.append(String.format("%02x", byteChar));
        //Log.d("static",  stringBuilder.toString());
        String s = stringBuilder.toString();
        s = s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16)
                + "-" + s.substring(16, 20) + "-" + s.substring(20, 32);
        //Log.d("static 3",  s);
        UUID id2 = UUID.fromString(s);
        //Log.d("static 2", id2.toString());


        return id2.toString();
    }
}
