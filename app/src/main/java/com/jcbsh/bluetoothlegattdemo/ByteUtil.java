package com.jcbsh.bluetoothlegattdemo;

import java.nio.ByteBuffer;

/**
 * Created by JCBSH on 1/04/2016.
 */
public class ByteUtil {


    public static String getBinaryString(byte[] bytes) {
        String result = "";
        for (byte b: bytes) {
            result = result + String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
        }
        return  result;
    }

    public static String getIntString(byte[] bytes) {
        return  "" + ByteBuffer.wrap(bytes).getInt();
    }


    public static int getIntDate(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] reverseBytes(byte[] data) {
        byte[] reverseData = new byte[data.length];
        for (int i = 0, j = data.length - 1; i  < data.length; ++i, --j) {
            reverseData[i] = data[j];
        }

        return reverseData;
    }

    public static byte[] getIntToByteArray(int num) {
        return ByteBuffer.allocate(4).putInt(num).array();
    }

}
