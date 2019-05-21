package com.felhr.serialportexamplesync;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

import com.felhr.madsessions.MadKeyEvent;
import com.felhr.madsessions.MadKeyEventListener;
import com.felhr.madsessions.MadPlatformDevice;
import com.felhr.madsessions.MadSession;
import com.felhr.sensors.MadSensor;
import com.felhr.sensors.MadSensorEvent;
import com.felhr.sensors.MadSensorEventListener;
import com.felhr.sensors.MadSensorManager;
import com.felhr.utils.CRC16;

public class MainActivity extends AppCompatActivity implements MadSensorEventListener,MadKeyEventListener {
    public static String TAG = "MainActivity";
    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MadSensorService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case MadSensorService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case MadSensorService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case MadSensorService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case MadSensorService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    public static MainActivity instance = null;
    private MadSensorService mSensorService;
    private TextView txtview_mag_value;
    private TextView txtview_acc_value;
    private TextView txtview_gyro_value;
    private TextView txtview_als_value;
    private TextView txtview_ps_value;
    private TextView txtview_err_rate;
    private MyHandler mHandler;
    private MadSensor mMagSensor;
    private MadSensor mAccSensor;
    private MadSensor mGyroSensor;
    private MadSensor mAlsSensor;
    private MadSensor mPsSensor;
    private MadPlatformDevice mPlatformDevice;
    private boolean mEnable;
    private long mPrevTime;
    private long mPrevCount;
    private long mMinRate;
    private long mMaxRate;
    private long mSeconds;

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            mSensorService = ((MadSensorService.UsbBinder) arg1).getService();
            mSensorService.setHandler(mHandler);
            mSensorService.registerListener(instance, mMagSensor, 5000);
            mSensorService.registerListener(instance, mAccSensor, 5000);
            mSensorService.registerListener(instance, mGyroSensor, 5000);
            mSensorService.registerListener(instance, mAlsSensor, 5000);
            mSensorService.registerListener(instance, mPsSensor, 5000);
            mSensorService.registerKeyListener(instance, MadKeyEvent.MAD_KEY_0);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mSensorService.unregisterListener(instance);
            mSensorService = null;
        }
    };

    public String stampToDate(long timeMillis){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(timeMillis);
        return simpleDateFormat.format(date);
    }

    public int calcSum(byte[] data, int size){
        int sum = 0;
        while (size > 0)
        {
            sum = sum + (data[size - 1] & 0xFF);
            size--;
        }

        return 1 + (~sum);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        mHandler = new MyHandler(this);

        txtview_mag_value = (TextView) findViewById(R.id.txtview_mag_value);
        txtview_acc_value = (TextView) findViewById(R.id.txtview_acc_value);
        txtview_gyro_value = (TextView) findViewById(R.id.txtview_gyro_value);
        txtview_als_value = (TextView) findViewById(R.id.txtview_als_value);
        txtview_ps_value = (TextView) findViewById(R.id.txtview_ps_value);
        txtview_err_rate = (TextView) findViewById(R.id.txtview_err_rate);

        Button magEnableBtn = (Button) findViewById(R.id.magEnable);
        magEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean status;

                //当前感应器状态已经打开
                if(mMagSensor.isEnabled()){
                    status = true;
                } else {
                    status = false;
                }

                status = !status;

                mMagSensor.enable(status);

                //当前感应器状态已经打开
                if(mMagSensor.isEnabled()){
                    magEnableBtn.setText(R.string.enable_mag);
                } else {
                    magEnableBtn.setText(R.string.disable_mag);
                }
            }
        });

        Button accEnableBtn = (Button) findViewById(R.id.accEnable);
        accEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean status;

                //当前感应器状态已经打开
                if(mAccSensor.isEnabled()){
                    status = true;
                } else {
                    status = false;
                }

                status = !status;

                mAccSensor.enable(status);

                //当前感应器状态已经打开
                if(mAccSensor.isEnabled()){
                    accEnableBtn.setText(R.string.enable_acc);
                } else {
                    accEnableBtn.setText(R.string.disable_acc);
                }
            }
        });

        Button gyroEnableBtn = (Button) findViewById(R.id.gyroEnable);
        gyroEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean status;

                //当前感应器状态已经打开
                if(mGyroSensor.isEnabled()){
                    status = true;
                } else {
                    status = false;
                }

                status = !status;

                mGyroSensor.enable(status);

                //当前感应器状态已经打开
                if(mGyroSensor.isEnabled()){
                    gyroEnableBtn.setText(R.string.enable_gyro);
                } else {
                    gyroEnableBtn.setText(R.string.disable_gyro);
                }
            }
        });

        Button alsEnableBtn = (Button) findViewById(R.id.alsEnable);
        alsEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean status;

                //当前感应器状态已经打开
                if(mAlsSensor.isEnabled()){
                    status = true;
                } else {
                    status = false;
                }

                status = !status;

                mAlsSensor.enable(status);

                //当前感应器状态已经打开
                if(mAlsSensor.isEnabled()){
                    alsEnableBtn.setText(R.string.enable_als);
                } else {
                    alsEnableBtn.setText(R.string.disable_als);
                }
            }
        });

        Button psEnableBtn = (Button) findViewById(R.id.psEnable);
        psEnableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean status;

                //当前感应器状态已经打开
                if(mPsSensor.isEnabled()){
                    status = true;
                } else {
                    status = false;
                }

                status = !status;

                mPsSensor.enable(status);

                //当前感应器状态已经打开
                if(mPsSensor.isEnabled()){
                    psEnableBtn.setText(R.string.enable_ps);
                } else {
                    psEnableBtn.setText(R.string.disable_ps);
                }
            }
        });

        Button upgrade = (Button) findViewById(R.id.upgrade);
        upgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if(mPlatformDevice == null){
                    mPlatformDevice = MadPlatformDevice.getInstance();
                    mPlatformDevice.setup();
                    mPlatformDevice.reset(0);
                }*/
                mPlatformDevice = MadPlatformDevice.getInstance();
                /*byte[] vendor = mPlatformDevice.getVendor();
                if(vendor != null) {
                    Log.d(TAG, "vendor: " + new String(vendor));
                    String test = "TEST";
                    mPlatformDevice.setVendor(test.getBytes());
                }*/

                byte[] firmwareVersion = mPlatformDevice.getFirmwareVersion();
                if(firmwareVersion != null) {
                    System.out.print("vendor: " + firmwareVersion.toString());
                }

                /*int ret = mPlatformDevice.openLCD();
                if(ret == 0) {
                    System.out.print("open lcd success!");
                    ret = mPlatformDevice.closeLCD();
                }*/

                //byte[] data = { (byte)0x01,   (byte)0x37,   (byte)0x05,   (byte)0x00,   (byte)0x20,   (byte)0x10,  (byte)0xb5,   (byte)0x06,   (byte)0x4c};
                //int crc = calcSum(data, data.length);
                //Log.d(TAG, "crc: " + crc);
                //mPlatformDevice.setKeyFunction(MadKeyEvent.MAD_KEY_0, 1);
                //mPlatformDevice.enterBootloader();
                /*byte[] sn = mPlatformDevice.getSN();
                if(sn != null) {
                    System.out.print("sn: " + sn.toString());
                    mPlatformDevice.setSN(sn);
                }
                byte[] device = mPlatformDevice.getDeviceName();
                if(device != null) {
                    System.out.print("device: " + device.toString());
                    mPlatformDevice.setDeviceName(device);
                }

                byte[] vendor = mPlatformDevice.getVendor();
                if(vendor != null) {
                    System.out.print("vendor: " + vendor.toString());
                    mPlatformDevice.setVendor(vendor);
                }*/
            }
        });
    }

    public void onStart() {
        super.onStart();

        setFilters();  // Start listening notifications from MadSensorService
        startService(MadSensorService.class, usbConnection, null); // Start MadSensorService(if it was not started before) and Bind it
        mEnable = false;
        mPrevTime = 0;
        mPrevCount = 0;
        mMinRate = Long.MAX_VALUE;
        mMaxRate = 0;
        mMagSensor = MadSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_MAGNETIC);
        mAccSensor = MadSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_ACCELERATOR);
        mGyroSensor = MadSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_GYROSCOPE);
        mAlsSensor = MadSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_AMBIENT_LIGHT);
        mPsSensor = MadSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_PROXIMITY);

        mEnable = false;
        mMagSensor.enable(mEnable);
        mAccSensor.enable(mEnable);
        mGyroSensor.enable(mEnable);
        mAlsSensor.enable(mEnable);
        mPsSensor.enable(mEnable);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void onStop(){
        super.onStop();

        mEnable = false;
        mMagSensor.enable(mEnable);
        mAccSensor.enable(mEnable);
        mGyroSensor.enable(mEnable);
        mAlsSensor.enable(mEnable);
        mPsSensor.enable(mEnable);

        MadSensorManager.DestorySensor(mMagSensor);
        MadSensorManager.DestorySensor(mAccSensor);
        MadSensorManager.DestorySensor(mGyroSensor);
        MadSensorManager.DestorySensor(mAlsSensor);
        MadSensorManager.DestorySensor(mPsSensor);

        mMagSensor = null;
        mAccSensor = null;
        mGyroSensor = null;
        mAlsSensor = null;
        mPsSensor = null;
        mPlatformDevice = null;
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }
    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!MadSensorService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MadSensorService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(MadSensorService.ACTION_NO_USB);
        filter.addAction(MadSensorService.ACTION_USB_DISCONNECTED);
        filter.addAction(MadSensorService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(MadSensorService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void onMadSensorChanged(MadSensorEvent event) {
        Message msg = mHandler.obtainMessage();
        switch (event.sensor.mSensorType){
            case MadSensorManager.MAD_SENSOR_TYPE_MAGNETIC:
                msg.what = MadSensorService.MESSAGE_MAGNETIC;
                msg.obj = event;
                msg.sendToTarget();
                break;
            case MadSensorManager.MAD_SENSOR_TYPE_ACCELERATOR:
                msg.what = MadSensorService.MESSAGE_ACCELERATOR;
                msg.obj = event;
                msg.sendToTarget();
                break;
            case MadSensorManager.MAD_SENSOR_TYPE_GYROSCOPE:
                msg.what = MadSensorService.MESSAGE_GYROSCOPE;
                msg.obj = event;
                msg.sendToTarget();
                break;
            case MadSensorManager.MAD_SENSOR_TYPE_AMBIENT_LIGHT:
                msg.what = MadSensorService.MESSAGE_ALS;
                msg.obj = event;
                msg.sendToTarget();
                break;
            case MadSensorManager.MAD_SENSOR_TYPE_PROXIMITY:
                msg.what = MadSensorService.MESSAGE_PS;
                msg.obj = event;
                msg.sendToTarget();
                break;
            default:
                break;
        }
        if(mSensorService!= null) {
            Message ratemsg = mHandler.obtainMessage();
            ratemsg.what = MadSensorService.MESSAGE_ERR_RATE;

            long current = System.currentTimeMillis();
            if(mPrevTime == 0){
                mPrevTime = current;
            }

            long interval = current - mPrevTime;
            long curCount = mSensorService.getRecvCmdCount();
            long interCount = curCount - mPrevCount;
            if(interval >= 1000){
                mSeconds ++;
                if(mSeconds >= 10){
                    mSeconds = 0;

                    //每30秒重新计算最大最小值
                    mMinRate = Long.MAX_VALUE;
                    mMaxRate = 0;
                }

                interCount = interCount * 1000 / interval;
                if(mMinRate > interCount){
                    mMinRate = interCount;
                }

                if(mMaxRate < interCount){
                    mMaxRate = interCount;
                }

                mPrevCount = curCount;
                mPrevTime = current;
            }

            String rateString = "min rate : "+mMinRate+", max rate : "+ mMaxRate + ", send cmd: " + mSensorService.getSendCmdCount() + ",recv cmd: " + curCount + " , recv byte count / err count: "+ mSensorService.getRecvByteCount() + " / " + mSensorService.getRecvErrCount();
            ratemsg.obj = rateString;
            ratemsg.sendToTarget();
        }
    }

    @Override
    public void onMadAccuracyChanged(MadSensor sensor, int accuracy) {
    }

    @Override
    public void onKeyDown(int event) {
        System.out.println("key down : "+event);
    }

    @Override
    public void onKeyUp(int event) {
        System.out.println("key up : "+event);
    }

    /*
     * This handler will be passed to MadSensorService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            long timeStamp = System.currentTimeMillis();
            String time = mActivity.get().stampToDate(timeStamp);
            switch (msg.what) {
                case MadSensorService.MESSAGE_FROM_SERIAL_PORT:
                    break;
                case MadSensorService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case MadSensorService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case MadSensorService.SYNC_READ:
                    break;

                case MadSensorService.MESSAGE_ACCELERATOR:
                    MadSensorEvent acc = (MadSensorEvent)msg.obj;
                    String accStr = "( X: "+acc.values[0]+", Y: "+ acc.values[1] + ", Z: " + acc.values[2] + " )";
                    mActivity.get().txtview_acc_value.setText(accStr);
                    break;
                case MadSensorService.MESSAGE_GYROSCOPE:
                    MadSensorEvent gyro = (MadSensorEvent) msg.obj;
                    String gyroStr = "( X: "+gyro.values[0]+", Y: "+ gyro.values[1] + ", Z: " + gyro.values[2] + " )";
                    mActivity.get().txtview_gyro_value.setText(gyroStr);
                    break;
                case MadSensorService.MESSAGE_MAGNETIC:
                    MadSensorEvent mag = (MadSensorEvent)msg.obj;
                    String magStr = "( X: "+mag.values[0]+", Y: "+ mag.values[1] + ", Z: " + mag.values[2] + " )";
                    long cost = timeStamp - mag.timestamp;
                    Log.d(TAG, "#### mag time cost : " + cost + " ####");
                    mActivity.get().txtview_mag_value.setText(magStr);
                    break;
                case MadSensorService.MESSAGE_ALS:
                    MadSensorEvent als = (MadSensorEvent)msg.obj;
                    String alsStr = "( value: "+ als.values[0] + " )";
                    mActivity.get().txtview_als_value.setText(alsStr);
                    break;
                case MadSensorService.MESSAGE_PS:
                    MadSensorEvent ps = (MadSensorEvent)msg.obj;
                    String psStr = "( value: "+ ps.values[0] + " )";
                    mActivity.get().txtview_ps_value.setText(psStr);
                    break;
                case MadSensorService.MESSAGE_ERR_RATE:
                    String errRate = (String) msg.obj;

                    mActivity.get().txtview_err_rate.setText(errRate);
                    break;
            }
        }
    }
}
