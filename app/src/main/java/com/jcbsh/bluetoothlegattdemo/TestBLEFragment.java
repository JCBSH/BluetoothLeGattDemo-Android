package com.jcbsh.bluetoothlegattdemo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
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
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by JCBSH on 23/02/2016.
 */
public class TestBLEFragment extends Fragment {

    protected static final String LIFE_TAG = "life_" + TestBLEFragment.class.getSimpleName();
    protected static final String TAG = TestBLEFragment.class.getSimpleName();
    private String getFragmentLifeTag () {return LIFE_TAG;}
    private String getFragmentTag () {return TAG;}

    public static Fragment getInstance() {

        Fragment fragment = new TestBLEFragment();
        return fragment;
    }

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int mConnectionState = STATE_DISCONNECTED;

    protected final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            Log.e(getFragmentTag(), "onServiceConnected");

            //Log.d(getFragmentLifeTag(), "onServiceConnected get permission");
            if (!mBluetoothLeService.initialize()) {
                //Log.d(getFragmentLifeTag(), "mBluetoothLeService.initialize() true");
                String title =  getActivity().getResources().getString(R.string.bluetooth_error_alert_title);
                String msg =  getActivity().getResources().getString(R.string.error_bluetooth_not_supported);
                showAlert(title, msg);
            } else {
                //Log.d(getFragmentLifeTag(), "mBluetoothLeService.initialize() false");
                if (!mBluetoothLeService.isAdapterEnable()) {
                    //Log.d(getFragmentLifeTag(), "mBluetoothLeService.initialize() isAdapterEnable");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    //Log.d(getFragmentLifeTag(), "mBluetoothLeService.initialize() not isAdapterEnable");
                    boolean check1 = checkACCESS_FINE_LOCATION();
                    boolean check2 = checkACCESS_COARSE_LOCATION();
                    if (check1 && check2) {
                        mBluetoothLeService.startScan();
                    }
                }
            }

        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };




    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(LIFE_TAG, "onAttach() ");

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(LIFE_TAG, "onActivityCreated() ");
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.test_ble_fragment, menu);

        if (mConnectionState == STATE_DISCONNECTED) {
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else if (mConnectionState == STATE_CONNECTED) {
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_connecting).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_disconnect:
                //todo disconnect();

                return true;
            case R.id.menu_connect:
                //todo startScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected BluetoothLeService mBluetoothLeService;
    private BroadcastReceiver mGattUpdateReceiver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(getFragmentLifeTag(), "onCreate() ");
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Intent gattServiceIntent = new Intent(getActivity(), BluetoothLeService.class);
        getActivity().bindService(gattServiceIntent, mServiceConnection, Activity.BIND_AUTO_CREATE);
        mGattUpdateReceiver = createGattUpdateReceiver();
        getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    private Switch mLaserSwitch;
    private Handler mHandler;
    private LaserGattAttributes mLaserAttributes;
    private BluetoothLeScanner mBluetoothLeScanner;
    private View mUIContainer;
    private TextView mCPTextView;
    private TextView mIPTextView;
    private Button mMotorIncButton;
    private Button mMotorDecButton;
    private Button m1StepButton;
    private Button m2StepButton;
    private Button m4StepButton;
    private Button m8StepButton;
    private Button m16StepButton;
    private Button m32StepButton;
    private TextView mCurrentStepTextView;
    private TextView mMoveStatusStringTextView;
    private TextView mMoveStatusIntTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LIFE_TAG, "onCreateView() ");

        View v = inflater.inflate(R.layout.fragment_test_ble, container, false);

        mUIContainer = (View) v.findViewById(R.id.control_container);
        mLaserSwitch = (Switch) v.findViewById(R.id.laser_switch);
        mCPTextView = (TextView) v.findViewById(R.id.motor_current_position);
        mIPTextView = (TextView) v.findViewById(R.id.motor_intended_position);
        mMotorIncButton = (Button) v.findViewById(R.id.increase_motor_position_button);
        mMotorDecButton = (Button) v.findViewById(R.id.decrease_motor_position_button);
        m1StepButton = (Button) v.findViewById(R.id.step_full_button);
        m2StepButton = (Button) v.findViewById(R.id.step_2_button);
        m4StepButton = (Button) v.findViewById(R.id.step_4_button);
        m8StepButton = (Button) v.findViewById(R.id.step_8_button);
        m16StepButton = (Button) v.findViewById(R.id.step_16_button);
        m32StepButton = (Button) v.findViewById(R.id.step_32_button);
        mCurrentStepTextView = (TextView) v.findViewById(R.id.motor_current_step_mode);
        mMoveStatusStringTextView = (TextView) v.findViewById(R.id.motor_MM_string);
        mMoveStatusIntTextView = (TextView) v.findViewById(R.id.motor_MM_Int);

        mMotorIncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mMotorDecButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        mLaserSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    byte[] data = {new Byte("1")};

                } else {
                    byte[] data = {new Byte("0")};
                }

                //todo mBluetoothGatt.writeCharacteristic(mLaserCharacteristic);

            }
        });

        mUIContainer.setVisibility(View.INVISIBLE);
        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            String title =  getActivity().getResources().getString(R.string.bluetooth_error_alert_title);
            String msg =  getActivity().getResources().getString(R.string.error_bluetooth_enable_fail);
            showAlert(title, msg);
            return;
        }

        if (resultCode == Activity.RESULT_CANCELED) return;

        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                mBluetoothLeService.startScan();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }

    }




    @Override
    public void onStart() {
        Log.d(LIFE_TAG, "onStart() ");
        super.onStart();
    }


    @Override
    public void onResume() {
        Log.d(getFragmentLifeTag(), "onResume() ");
        super.onResume();

        openBackgroundThread();

        if (mBluetoothLeService != null && mBluetoothLeService.isInitialized()) {
            mBluetoothLeService.reconnect();

        }
    }

    private void openBackgroundThread() {
        mHandler = new Handler();
    }

    private void closeBackgroundThread() {
        mHandler = null;
    }
    @Override
    public void onPause() {
        Log.d(getFragmentLifeTag(), "onPause() ");

        super.onPause();

        closeBackgroundThread();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        Log.d(getFragmentLifeTag(), "onDestroy() ");
        super.onDestroy();
        getActivity().unbindService(mServiceConnection);
        getActivity().unregisterReceiver(mGattUpdateReceiver);
        mBluetoothLeService = null;
    }

    @Override
    public void onStop() {
        Log.d(LIFE_TAG, "onStop() ");
        super.onStop();

    }

    @Override
    public void onDestroyView() {
        Log.d(LIFE_TAG, "onDestroyView() ");
        super.onDestroyView();
    }


    @Override
    public void onDetach() {
        Log.d(LIFE_TAG, "onDetach() ");
        super.onDetach();


    }

////////////////////////////////////////////////////////////////////////////////
///////////////////////////////BLE RELATED CODES////////////////////////////////
///////////////////////////////BLE RELATED CODES////////////////////////////////
///////////////////////////////BLE RELATED CODES////////////////////////////////
////////////////////////////////////////////////////////////////////////////////
    protected static final HashMap<Integer, Integer> ORIENTATIONS_SCREEN = new HashMap<Integer, Integer>();
    static {
        ORIENTATIONS_SCREEN.put(Surface.ROTATION_0, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ORIENTATIONS_SCREEN.put(Surface.ROTATION_90, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ORIENTATIONS_SCREEN.put(Surface.ROTATION_180, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
        ORIENTATIONS_SCREEN.put(Surface.ROTATION_270, ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    protected IntentFilter makeGattUpdateIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_ZEROING_START);
        intentFilter.addAction(BluetoothLeService.ACTION_ZEROING_END);
        intentFilter.addAction(BluetoothLeService.ACTION_SCANNING_IN_PROGRESS);
        intentFilter.addAction(BluetoothLeService.ACTION_SCANNING_FAIL);
        return intentFilter;
    }

    protected BroadcastReceiver createGattUpdateReceiver() {
        return new BLEBroadcastReceiver();
    }

    protected class BLEBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                //todo
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //todo
            } else if (BluetoothLeService.ACTION_ZEROING_START.equals(action)) {
                int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                getActivity().setRequestedOrientation(ORIENTATIONS_SCREEN.get(rotation));
            } else if (BluetoothLeService.ACTION_ZEROING_END.equals(action)) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

            } else if (BluetoothLeService.ACTION_SCANNING_FAIL.equals(action)) {
                if (getActivity() != null) {
                    showAlert(getActivity().getResources().getString(R.string.bluetooth_error_alert_title)
                            , "fail to detect bluetooth");
                }
            }
        }
    };


////---------------------------------------------////
////---------------------------------------------////
////            UTILITY RELATED CODE             ////
////---------------------------------------------////
////---------------------------------------------////
    protected void showAlert(String title, String msg) {
        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .show();
    }


////---------------------------------------------////
////---------------------------------------------////
////            PERMISSION RELATED CODE          ////
////---------------------------------------------////
////---------------------------------------------////
    private static final int REQUEST_ACCESS_COARSE_LOCATION_PERMISSION_RESULT = 1;
    private static final int REQUEST_ACCESS_FINE_LOCATION_PERMISSION_RESULT = 2;

    private static final int REQUEST_ENABLE_BT = 1;

    private boolean checkACCESS_FINE_LOCATION() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(getActivity(),
                            "Video app required access to camera", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION_PERMISSION_RESULT);
                return false;
            }
        }
        return true;
    }

    private boolean checkACCESS_COARSE_LOCATION() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(getActivity(),
                            "Video app required access to camera", Toast.LENGTH_SHORT).show();
                }
                requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_COARSE_LOCATION_PERMISSION_RESULT);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_ACCESS_COARSE_LOCATION_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(),
                        "Application will not run without coarse location access", Toast.LENGTH_SHORT).show();
            } else if((grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                mBluetoothLeService.startScan();
            }
        }

        if(requestCode == REQUEST_ACCESS_FINE_LOCATION_PERMISSION_RESULT) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getActivity(),
                        "Application will not run without fine location access", Toast.LENGTH_SHORT).show();
            } else if((grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                mBluetoothLeService.startScan();
            }
        }
    }
}
