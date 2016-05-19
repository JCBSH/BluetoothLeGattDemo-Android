package com.jcbsh.bluetoothlegattdemo;

/**
 * Created by JCBSH on 31/03/2016.
 */

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private LaserGattAttributes mLaserAttributes;
    private BluetoothLeScanner mBluetoothLeScanner;



    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private boolean mIsExpectedDisconnection = false;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private int mScanningState = STATE_NOT_SCANNING
            ;
    private static final int STATE_NOT_SCANNING = 0;
    private static final int STATE_SCANNING = 1;

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "ACTION_DATA_AVAILABLE";

    public final static String ACTION_DATA_CHANGED ="ACTION_DATA_CHANGED";
    public final static String ACTION_DATA_CHANGED_MOTOR_CPOS ="ACTION_DATA_CHANGED_MOTOR_CPOS";
    public final static String ACTION_DATA_CHANGED_MOTOR_MMODE ="ACTION_DATA_CHANGED_MOTOR_MMODE";

    public final static String ACTION_DATA_WRITE_FAIL = "ACTION_DATA_WRITE_FAIL";
    public final static String ACTION_DATA_WRITE_SUCCESSFUL = "ACTION_DATA_WRITE_SUCCESSFUL";
    public final static String ACTION_DATA_WRITE_SUCCESSFUL_LASER_STATUS = "ACTION_DATA_WRITE_SUCCESSFUL_LASER_STATUS";

    public final static String ACTION_ZEROING_START = "ACTION_ZEROING_START";
    public final static String ACTION_ZEROING_END = "ACTION_ZEROING_END";

    public final static String ACTION_SCANNING_IN_PROGRESS = "ACTION_SCANNING_IN_PROGRESS";
    public final static String ACTION_SCANNING_FAIL = "ACTION_SCANNING_FAIL";
    public final static String ACTION_LOOP_SCAN = "ACTION_LOOP_SCAN";

    public final static String EXTRA_DATA = "EXTRA_DATA";

    private static final String TARGET_DEVICE_NAME = "nRF51-DK board";
    public static final long SCAN_PERIOD = 10000;

    public static final int LASER_OFF = 0;
    public static final int LASER_ON = 1;

    private static final int STEP_MODIFIER = 32;
    private static final int MAX_STEP = 4000;
    private static final int MIN_STEP = 0;

    public int getIntendedPosition() {
        return mIntendedPosition;
    }

    public void setIntendedPosition(int intendedPosition) {
        mIntendedPosition = intendedPosition;
    }

    private int mIntendedPosition = 0;
    private int mCurrentPosition = 0;

    private int mInitializingIndex = 0;
    private boolean mZeroingFlag = true;
    private int mZeroingIndex = 0;


    private static final int MOVE_STATE_UNDEFINED = 0;
    private static final int MOVE_STATE_STOPPED = 1;
    private static final int MOVE_STATE_MOVING = 2;
    private int mBLEMovingState = MOVE_STATE_STOPPED;


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            if (mScanningState == STATE_SCANNING) return;
            Log.d(TAG, "scanLeDevice true");
            // Stops scanning after a pre-defined scan period.
            mBackgroundHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanningState = STATE_NOT_SCANNING;
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    if (mBluetoothGatt == null) {
                        mConnectionState = STATE_DISCONNECTED;
                        broadcastUpdate(ACTION_SCANNING_FAIL);

                    }
                }
            }, SCAN_PERIOD);

            mScanningState = STATE_SCANNING;
            mConnectionState = STATE_CONNECTING;
            ScanFilter.Builder builder = new ScanFilter.Builder();
            List<ScanFilter> filterList = new ArrayList<ScanFilter>();
            filterList.add(builder.build());
            ScanSettings scanSettings = new ScanSettings.Builder().build();
            mBluetoothLeScanner.startScan(filterList, scanSettings, mScanCallback);
            //mBluetoothLeScanner.startScan(mScanCallback);
        } else {
            Log.d(TAG, "scanLeDevice false");
            if (mBluetoothGatt == null) return;
            mScanningState = STATE_NOT_SCANNING;
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String name = result.getDevice().getName();
            String address = result.getDevice().getAddress();
            Log.d(TAG, "onScanResult");
            Log.d(TAG, name + "\n" + address);
            //Log.d(TAG, "1");
            if (name == null) return;
            //Log.d(TAG, "2");
            if (!isStateConnecting()) return;
            //Log.d(TAG, "3");
            if (name.equals(TARGET_DEVICE_NAME)) {
                if (mBluetoothGatt == null) {
                    Log.d(TAG, name + " address: " + address);
                    connect(result.getDevice());
                    scanLeDevice(false);
                }
            }

            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults");
            Log.d(TAG, results.get(0).getDevice().getName() + "\n" + results.get(0).getDevice().getAddress());
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "onScanFailed: " + errorCode);
            super.onScanFailed(errorCode);
        }
    };



    private BluetoothGattCharacteristic mLaserCharacteristic;
    private BluetoothGattCharacteristic mCPCharacteristic;
    private BluetoothGattCharacteristic mIPCharacteristic;
    private BluetoothGattCharacteristic mSMCharacteristic;
    private BluetoothGattCharacteristic mMMCharacteristic;


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                initialCharacteristic();
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                if (!mIsExpectedDisconnection) {
                    broadcastUpdate(intentAction);
                }
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                List<BluetoothGattService> services = mBluetoothGatt.getServices();
                if (services != null) {

                    Log.i(TAG, "service size: " + services.size());

                    for (BluetoothGattService service: services) {
                        //Log.i(TAG, "service: " + service.getUuid().toString());
                        List < BluetoothGattCharacteristic > gattCharacteristics = service.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                            String uuid = gattCharacteristic.getUuid().toString();
                            if (mLaserAttributes.getCharacteristicsByID(uuid) != null) {
                                mLaserAttributes.getCharacteristicsByID(uuid).setCharacteristic(gattCharacteristic);
                            }
                        }
                    }

                    mLaserCharacteristic = mLaserAttributes.
                            getCharacteristicsByName(LaserGattAttributes.CHAR_LASER_STATUS_KEY).getCharacteristic();
                    mCPCharacteristic = mLaserAttributes.
                            getCharacteristicsByName(LaserGattAttributes.CHAR_MOTOR_CPOS_KEY).getCharacteristic();
                    mIPCharacteristic = mLaserAttributes.
                            getCharacteristicsByName(LaserGattAttributes.CHAR_MOTOR_IPOS_KEY).getCharacteristic();
                    mSMCharacteristic = mLaserAttributes.
                            getCharacteristicsByName(LaserGattAttributes.CHAR_MOTOR_SMODE_KEY).getCharacteristic();
                    mMMCharacteristic = mLaserAttributes.
                            getCharacteristicsByName(LaserGattAttributes.CHAR_MOTOR_MMODE_KEY).getCharacteristic();

                    initialCharacteristic();

                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.d(TAG, "onCharacteristicRead received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "onCharacteristicRead success: " + status);
                String uuid = characteristic.getUuid().toString();
                byte[] preReversedData = characteristic.getValue();
                byte[] data = ByteUtil.reverseBytes(preReversedData);
                byte[] adjustedData = new byte[4];

                for(int i = 0; i < (4-data.length) ; ++i) {
                    adjustedData[i] = new Byte("0");
                }


                for(int i = (4-data.length), j = 0; i < 4 ; ++i, ++j) {
                    adjustedData[i] = data[j];
                }

                String binaryDataString = ByteUtil.getBinaryString(adjustedData);
                final String dataString = ByteUtil.getIntString(adjustedData);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //Log.w(TAG, "onCharacteristicChanged called");
            String uuid = characteristic.getUuid().toString();
            byte[] preReversedData = characteristic.getValue();
            byte[] data = ByteUtil.reverseBytes(preReversedData);
            byte[] adjustedData = new byte[4];

            for(int i = 0; i < (4-data.length) ; ++i) {
                adjustedData[i] = new Byte("0");
            }
            for(int i = (4-data.length), j = 0; i < 4 ; ++i, ++j) {
                adjustedData[i] = data[j];
            }
            String binaryDataString = ByteUtil.getBinaryString(adjustedData);
            final int dataInt = ByteUtil.getIntDate(adjustedData);

            //Log.d(TAG, binaryDataString);

            if (uuid.equals(LaserGattAttributes.getUUIDInString(LaserGattAttributes.CHAR_MOTOR_CPOS))) {
                //Log.d("detail3DScan" , " change CP: " + mCurrentPosition);
                //Log.d("detail3DScan" , " change IP: " + mIntendedPosition);
                mCurrentPosition = dataInt/STEP_MODIFIER;
                if (isMotorInPosition()) broadcastUpdate(ACTION_LOOP_SCAN);

            } else if (uuid.equals(LaserGattAttributes.getUUIDInString(LaserGattAttributes.CHAR_MOTOR_MMODE))) {
                //Log.d("detail3DScan" ," change MM: " + dataInt);
                //Log.d("detail3DScan" ," target MM: " + MOVE_STATE_STOPPED);
                mBLEMovingState = dataInt;
                if (isMotorInPosition()) broadcastUpdate(ACTION_LOOP_SCAN);
            }


            zeroingDevice();
        }



        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_FAILURE) {
                //Log.d(TAG ," ACTION_DATA_WRITE_FAIL");
                broadcastUpdate(ACTION_DATA_WRITE_FAIL);
            } else {
                //Log.d(TAG ," ACTION_DATA_WRITE_SUCCESS");
                String uuid = characteristic.getUuid().toString();
                if (uuid.equals(LaserGattAttributes.getUUIDInString(LaserGattAttributes.CHAR_LASER_STATUS))) {
                    //Log.d(TAG ," ACTION_DATA_WRITE_SUCCESS_laser");
                    broadcastUpdate(ACTION_LOOP_SCAN);
                    zeroingDevice();
                }
            }
        }

    };


    private void zeroingDevice() {
        if (mZeroingFlag) {
            switch (mZeroingIndex) {
                case 0:

                    broadcastUpdate(ACTION_ZEROING_START);

                    mIntendedPosition = MIN_STEP;
                    int modifiedIP = mIntendedPosition * STEP_MODIFIER;
                    byte[] IPData = ByteUtil.reverseBytes(ByteUtil.getIntToByteArray(modifiedIP));

                    Log.d(TAG, "zeroing motor position " + ByteUtil.getBinaryString(IPData));
                    mIPCharacteristic.setValue(IPData);
                    //mIPCharacteristic.set
                    mBluetoothGatt.writeCharacteristic(mIPCharacteristic);
                    break;
                case 1:
                    switchLaser(LASER_OFF);
                    break;

                default:

                    broadcastUpdate(ACTION_ZEROING_END);

                    mZeroingFlag = false;

            }

            mZeroingIndex++;
        }
    }


    private void initialCharacteristic() {
        switch (mInitializingIndex) {
            case 0:
                mBluetoothGatt.setCharacteristicNotification(mMMCharacteristic,true);
                List<BluetoothGattDescriptor> descriptors = mMMCharacteristic.getDescriptors();
                BluetoothGattDescriptor descriptor = descriptors.get(0);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (mBluetoothGatt.writeDescriptor(descriptor)) {
                    Log.d("T! mm writeDescriptor: ", "success");
                } else {
                    Log.d("T! mm writeDescriptor: ", "not success");
                }
                break;
            case 1:
                mBluetoothGatt.setCharacteristicNotification(mCPCharacteristic,true);
                descriptors = mCPCharacteristic.getDescriptors();
                descriptor = descriptors.get(0);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (mBluetoothGatt.writeDescriptor(descriptor)) {
                    Log.d("T! cp writeDescriptor: ", "success");
                } else {
                    Log.d("T! cp writeDescriptor: ", "not success");
                }
                break;
            case 2:
                mZeroingFlag = true;
                mZeroingIndex = 0;
                zeroingDevice();

                break;
        }
        mInitializingIndex++;
    }

    public void switchLaser(int laserState) {

        String laserStateString = String.valueOf(laserState);
        byte[] LaserData = {new Byte(laserStateString)};
        //Log.d(detail3DScanTag, "" + getBinaryString(LaserData));
        mLaserCharacteristic.setValue(LaserData);
        mBluetoothGatt.writeCharacteristic(mLaserCharacteristic);

    }

    public void movingMotor() {
        if (mIntendedPosition >= MIN_STEP && mIntendedPosition <= MAX_STEP) {
            int modifiedIP = mIntendedPosition * STEP_MODIFIER;
            byte[] IPData = ByteUtil.reverseBytes(ByteUtil.getIntToByteArray(modifiedIP));
            mIPCharacteristic.setValue(IPData);
            mBluetoothGatt.writeCharacteristic(mIPCharacteristic);

        }
    }

    public void startScan() {
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (getBluetoothGatt() == null) scanLeDevice(true);
    }

    public void zeroDevice() {
        mZeroingFlag = true;
        mZeroingIndex = 0;
        zeroingDevice();
    }
    public boolean isMotorInPosition() {
        return (mIntendedPosition == mCurrentPosition && mBLEMovingState == MOVE_STATE_STOPPED);
    }



    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final boolean flag) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, flag);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        sendBroadcast(intent);
    }

    public boolean isAdapterEnable() {
        return mBluetoothAdapter.isEnabled();
    }



    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        openBackgroundThread();
        return mBinder;

    }


    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private void openBackgroundThread() {
        mBackgroundThread =  new HandlerThread("background thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void closeBackgroundThread() {

        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    @Override
    public boolean onUnbind(Intent intent) {
        closeBackgroundThread();
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mConnectionState = STATE_DISCONNECTED;
        mLaserAttributes = new LaserGattAttributes();

        return true;
    }

    private boolean connect(final BluetoothDevice device) {
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    public boolean reconnect() {
        // Previously connected device.  Try to reconnect.
        mInitializingIndex = 0;
        mIsExpectedDisconnection = false;
        if (mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                Log.d(TAG, "existing mBluetoothGatt still exist");
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                Log.d(TAG, "existing mBluetoothGatt no longer exist");
                return false;
            }
        }
        return false;
    }



    public void disconnect() {
        mConnectionState = STATE_DISCONNECTED;
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mIsExpectedDisconnection = true;
        mBluetoothGatt.disconnect();

    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {

        mConnectionState = STATE_DISCONNECTED;
        if (mBluetoothGatt == null) {
            Log.d(TAG, "mBluetoothGatt equal null");
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;

    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public boolean isStateConnecting() {

        if (mConnectionState != STATE_CONNECTING) return false;
        return true;
    }

    public boolean isInitialized() {
        return mInitializingIndex != 0;
    }


    public int getConnectionState() {
        return mConnectionState;
    }
}
