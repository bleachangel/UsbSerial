package com.felhr.serialportexamplesync;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.madsessions.MadConnectionManager;
import com.felhr.madsessions.MadDeviceConnection;
import com.felhr.madsessions.MadKeyEvent;
import com.felhr.madsessions.MadKeyEventListener;
import com.felhr.madsessions.MadPlatformDevice;
import com.felhr.madsessions.MadSessionManager;
import com.felhr.madsessions.MadUartParameters;
import com.felhr.sensors.MadSensor;
import com.felhr.sensors.MadSensorEvent;
import com.felhr.sensors.MadSensorEventListener;
import com.felhr.sensors.MadSensorManager;
import com.felhr.usbserial.UsbSerialDevice;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements MadSensorEventListener,MadKeyEventListener {
    public static String TAG = "MainActivity";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;
    public static final int SYNC_READ = 3;
    public static final int MESSAGE_GYROSCOPE = 4;
    public static final int MESSAGE_ACCELERATOR = 5;
    public static final int MESSAGE_MAGNETIC = 6;
    public static final int MESSAGE_ALS = 7;
    public static final int MESSAGE_PS = 8;
    public static final int MESSAGE_ERR_RATE = 9;

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
    private boolean mLoopTest;
    private UsbManager mUsbManager;
    private MadSensorManager mSensorManager;

    private void requestUserPermission(UsbDevice usbDevice) {
        Log.d(TAG, String.format("requestUserPermission(%X:%X)", usbDevice.getVendorId(), usbDevice.getProductId() ) );
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        mUsbManager.requestPermission(usbDevice, mPendingIntent);
    }

    public boolean isUartConnection(UsbDevice usbDevice){
        boolean ret = false;
        int iIndex = usbDevice.getInterfaceCount();
        for (int i = 0; i <= iIndex - 1; i++) {
            UsbInterface iface = usbDevice.getInterface(i);
            if (iface.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
                case ACTION_USB_PERMISSION:
                    {
                        boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                        if (granted) {
                            // User accepted our USB connection. Try to open the device as a serial port
                            Intent permissionIntent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                            context.sendBroadcast(permissionIntent);

                            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                            UsbDeviceConnection connection = mUsbManager.openDevice(device);

                            Log.d(TAG, "Current usb device name:"+ device.getDeviceName());
                            MadUartParameters para = new MadUartParameters();
                            MadDeviceConnection conn = MadConnectionManager.getInstance().connectUartDevice(device, connection, para);
                            if(conn != null) {
                                Log.d(TAG, "Current connect device:"+conn.getDeviceName());
                            }
                        } else {
                            Intent permissionIntent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                            context.sendBroadcast(permissionIntent);
                        }
                    }
                    break;
                case ACTION_USB_ATTACHED:
                    {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (isUartConnection(device)) {
                            requestUserPermission(device);
                        }
                    }
                    break;
                case ACTION_USB_DETACHED:
                    {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        Log.d(TAG, "detach usb device name:"+ device.getDeviceName());
                        MadConnectionManager.getInstance().disconnectDevice(device);
                        //MadDeviceConnection conn = MadConnectionManager.getInstance().findConnection(device);
                        //if(conn != null) {
                        //    Log.d(TAG, "detach device:"+conn.getDeviceName());
                        //}

                        Intent detachIntent = new Intent(ACTION_USB_DISCONNECTED);
                        context.sendBroadcast(detachIntent);
                    }
                    break;
            }
        }
    };

    private void findSerialPortDevice() {
        boolean bFindFlag = false;

        HashMap<String, UsbDevice> usbDevices = mUsbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            ArrayList<UsbDevice> deviceList = new ArrayList<UsbDevice>();
            // first, dump the map for diagnostic purposes
            for (HashMap.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                UsbDevice device = entry.getValue();
                Log.d(TAG, String.format("USBDevice.HashMap (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                        device.getVendorId(), device.getProductId(),
                        UsbSerialDevice.isSupported(device),
                        device.getDeviceClass(), device.getDeviceSubclass(),
                        device.getDeviceName()));
                if(!deviceList.contains(device)){
                    deviceList.add(device);
                }
            }

            for (int i = 0; i < deviceList.size(); i++) {
                UsbDevice device = deviceList.get(i);
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

//                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                if (UsbSerialDevice.isSupported(device)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    bFindFlag = true;
                    requestUserPermission(device);
                }
            }
            if (!bFindFlag) {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            Log.d(TAG, "findSerialPortDevice() usbManager returned empty device list." );
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

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
        mHandler = new MyHandler(this);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mSensorManager = new MadSensorManager();

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

        mLoopTest = false;
        mPlatformDevice = new MadPlatformDevice(MadConnectionManager.getDeviceCapacity()|MadConnectionManager.getInfoCapacity());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mLoopTest){
                    mPlatformDevice.getFirmwareVersion();
                }
            }
        }).start();

        Button upgrade = (Button) findViewById(R.id.upgrade);
        upgrade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] firmwareVersion = mPlatformDevice.getFirmwareVersion();
                if(firmwareVersion != null) {
                    System.out.print("vendor: " + firmwareVersion.toString());
                }
            }
        });

        Button startBtn = (Button) findViewById(R.id.btnStart);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //开启所有设备连接的数据传输通道
                MadConnectionManager.getInstance().startAll();

                //sensor设备初始出
                mSensorManager.init();
            }
        });

        Button stopBtn = (Button) findViewById(R.id.btnStop);
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //关闭所有设备连接的数据传输通道
                MadConnectionManager.getInstance().stopAll();

                //sensor设备去初始化
                mSensorManager.deinit();
            }
        });
    }

    public void onStart() {
        super.onStart();
        setFilters();

        //外围设备已经通过USB连接成功，则需要为这些设备重新授权
        findSerialPortDevice();

        mEnable = false;
        mPrevTime = 0;
        mPrevCount = 0;
        mMinRate = Long.MAX_VALUE;
        mMaxRate = 0;

        //创建各种sensor对象
        mMagSensor = mSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_MAGNETIC);
        mAccSensor = mSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_ACCELERATOR);
        mGyroSensor = mSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_GYROSCOPE);
        mAlsSensor = mSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_AMBIENT_LIGHT);
        mPsSensor = mSensorManager.CreateSensor(MadSensorManager.MAD_SENSOR_TYPE_PROXIMITY);

        //监听sensor的事件
        mSensorManager.registerListener(this, mMagSensor, 5000);
        mSensorManager.registerListener(this, mAccSensor, 5000);
        mSensorManager.registerListener(this, mGyroSensor, 5000);
        mSensorManager.registerListener(this, mAlsSensor, 5000);
        mSensorManager.registerListener(this, mPsSensor, 5000);
        mSensorManager.registerKeyListener(this, MadKeyEvent.MAD_KEY_0);

        //默认不要打开sensor
        mEnable = false;
        mSensorManager.enableAllSensor(mEnable);
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

        //关闭所有sensor
        mEnable = false;
        mSensorManager.enableAllSensor(mEnable);

        //取消所有sensor的监听
        mSensorManager.unregisterAllListener();

        //销毁所有sensor的对象
        mSensorManager.destoryAllSensor();

        mMagSensor = null;
        mAccSensor = null;
        mGyroSensor = null;
        mAlsSensor = null;
        mPsSensor = null;
        mPlatformDevice = null;

        //断开所有的连接
        MadConnectionManager.getInstance().disconnectAll();
        unregisterReceiver(mUsbReceiver);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        filter.addAction(ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(ACTION_NO_USB);
        filter.addAction(ACTION_USB_DISCONNECTED);
        filter.addAction(ACTION_USB_NOT_SUPPORTED);
        filter.addAction(ACTION_USB_PERMISSION_NOT_GRANTED);

        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void onMadSensorChanged(MadSensorEvent event) {
        Message msg = mHandler.obtainMessage();
        switch (event.sensor.mSensorType){
            case MadSensorManager.MAD_SENSOR_TYPE_MAGNETIC:
                msg.what = MESSAGE_MAGNETIC;
                msg.obj = event;
                msg.sendToTarget();
                break;
            case MadSensorManager.MAD_SENSOR_TYPE_ACCELERATOR:
                msg.what = MESSAGE_ACCELERATOR;
                msg.obj = event;
                msg.sendToTarget();
                break;
            case MadSensorManager.MAD_SENSOR_TYPE_GYROSCOPE:
                msg.what = MESSAGE_GYROSCOPE;
                msg.obj = event;
                msg.sendToTarget();
                break;
            case MadSensorManager.MAD_SENSOR_TYPE_AMBIENT_LIGHT:
                msg.what = MESSAGE_ALS;
                msg.obj = event;
                msg.sendToTarget();
                break;
            case MadSensorManager.MAD_SENSOR_TYPE_PROXIMITY:
                msg.what = MESSAGE_PS;
                msg.obj = event;
                msg.sendToTarget();
                break;
            default:
                break;
        }
        {
            Message ratemsg = mHandler.obtainMessage();
            ratemsg.what = MESSAGE_ERR_RATE;

            long current = System.currentTimeMillis();
            if(mPrevTime == 0){
                mPrevTime = current;
            }

            long interval = current - mPrevTime;
            long curCount = MadSessionManager.getInstance().getRecvCmdCount();
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

            String rateString = "min rate : "+mMinRate+", max rate : "+ mMaxRate + ", send cmd: " + MadSessionManager.getInstance().getSendCmdCount() + ",recv cmd: " + curCount + " , recv byte count / err count: "+ MadSessionManager.getInstance().getRecvByteCount() + " / " + MadSessionManager.getInstance().getRecvErrCount();
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
                case MESSAGE_FROM_SERIAL_PORT:
                    break;
                case CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case SYNC_READ:
                    break;

                case MESSAGE_ACCELERATOR:
                    MadSensorEvent acc = (MadSensorEvent)msg.obj;
                    String accStr = "( X: "+acc.values[0]+", Y: "+ acc.values[1] + ", Z: " + acc.values[2] + " )";
                    mActivity.get().txtview_acc_value.setText(accStr);
                    break;
                case MESSAGE_GYROSCOPE:
                    MadSensorEvent gyro = (MadSensorEvent) msg.obj;
                    String gyroStr = "( X: "+gyro.values[0]+", Y: "+ gyro.values[1] + ", Z: " + gyro.values[2] + " )";
                    mActivity.get().txtview_gyro_value.setText(gyroStr);
                    break;
                case MESSAGE_MAGNETIC:
                    MadSensorEvent mag = (MadSensorEvent)msg.obj;
                    String magStr = "( X: "+mag.values[0]+", Y: "+ mag.values[1] + ", Z: " + mag.values[2] + " )";
                    long cost = timeStamp - mag.timestamp;
                    Log.d(TAG, "#### mag time cost : " + cost + " ####");
                    mActivity.get().txtview_mag_value.setText(magStr);
                    break;
                case MESSAGE_ALS:
                    MadSensorEvent als = (MadSensorEvent)msg.obj;
                    String alsStr = "( value: "+ als.values[0] + " )";
                    mActivity.get().txtview_als_value.setText(alsStr);
                    break;
                case MESSAGE_PS:
                    MadSensorEvent ps = (MadSensorEvent)msg.obj;
                    String psStr = "( value: "+ ps.values[0] + " )";
                    mActivity.get().txtview_ps_value.setText(psStr);
                    break;
                case MESSAGE_ERR_RATE:
                    String errRate = (String) msg.obj;
                    mActivity.get().txtview_err_rate.setText(errRate);
                    break;
            }
        }
    }
}
