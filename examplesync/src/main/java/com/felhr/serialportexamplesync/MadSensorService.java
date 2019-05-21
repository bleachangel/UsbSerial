package com.felhr.serialportexamplesync;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.felhr.madsessions.MadKeyEvent;
import com.felhr.madsessions.MadKeyEventListener;
import com.felhr.madsessions.MadPlatformDevice;
import com.felhr.madsessions.MadSession;
import com.felhr.madsessions.MadSessionManager;
import com.felhr.sensors.MadSensor;
import com.felhr.sensors.MadSensorEvent;
import com.felhr.sensors.MadSensorEventListener;
import com.felhr.sensors.MadSensorManager;
import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MadSensorService extends Service {

    public static final String TAG = "MadSensorService";

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
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    public static boolean SERVICE_CONNECTED = false;

    private IBinder binder = new UsbBinder();

    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;
    private MadSensorManager mSensorManager;
    //private MadSession mSession = null;
    private boolean serialPortConnected;

    private HashMap<MadSensor, MadSensorEventListener> mSensorListeners = new HashMap<MadSensor, MadSensorEventListener>();
    public boolean registerListener(MadSensorEventListener listener, MadSensor sensor, int delayUs){
        synchronized (mSensorListeners) {
            sensor.mTimeOut = delayUs;
            mSensorListeners.put(sensor, listener);
        }
        return true;
    }

    public boolean unregisterListener(MadSensorEventListener listener){
        synchronized (mSensorListeners) {
            Iterator<Map.Entry<MadSensor, MadSensorEventListener>> keys = mSensorListeners.entrySet().iterator();
            while (keys.hasNext()) {
                Map.Entry<MadSensor, MadSensorEventListener> key = keys.next();
                if (key.getValue().equals(listener)) {
                    keys.remove();
                }
            }
        }
        return true;
    }

    public void dispatchSensorEvent(MadSensor sensor, MadSensorEvent event){
        if (mSensorListeners == null || sensor == null || event == null) {
            return;
        }

        synchronized (mSensorListeners) {
            MadSensorEventListener listener = mSensorListeners.get(sensor);
            listener.onMadSensorChanged(event);
        }
    }

    private HashMap< MadKeyEventListener, Integer> mKeyEventListeners = new HashMap<MadKeyEventListener, Integer>();
    public boolean registerKeyListener(MadKeyEventListener listener, int key){
        synchronized (mKeyEventListeners) {
            mKeyEventListeners.put(listener, key);
        }
        return true;
    }

    public boolean unregisterKeyListener(MadKeyEventListener listener){
        synchronized (mKeyEventListeners) {
            Iterator<Map.Entry<MadKeyEventListener, Integer>> keys = mKeyEventListeners.entrySet().iterator();
            while (keys.hasNext()) {
                Map.Entry<MadKeyEventListener, Integer> key = keys.next();
                if (key.getKey().equals(listener)) {
                    keys.remove();
                }
            }
        }
        return true;
    }

    public void dispatchKeyEvent(MadKeyEvent event){
        if (mKeyEventListeners == null || event == null) {
            return;
        }

        synchronized (mKeyEventListeners) {
            Iterator<Map.Entry<MadKeyEventListener, Integer>> keys = mKeyEventListeners.entrySet().iterator();
            while (keys.hasNext()) {
                Map.Entry<MadKeyEventListener, Integer> key = keys.next();
                if (key.getValue() == event.mKeyValue) {
                    MadKeyEventListener listener = key.getKey();
                    if(event.mKeyState == event.MAD_KEY_UP){
                        listener.onKeyUp(event.mKeyValue);
                    }else if(event.mKeyState == event.MAD_KEY_DOWN){
                        listener.onKeyDown(event.mKeyValue);
                    }
                }
            }
        }
    }
    /*
     * State changes in the CTS line will be received here
     */
    private UsbSerialInterface.UsbCTSCallback ctsCallback = new UsbSerialInterface.UsbCTSCallback() {
        @Override
        public void onCTSChanged(boolean state) {
            if(mHandler != null)
                mHandler.obtainMessage(CTS_CHANGE).sendToTarget();
        }
    };

    /*
     * State changes in the DSR line will be received here
     */
    private UsbSerialInterface.UsbDSRCallback dsrCallback = new UsbSerialInterface.UsbDSRCallback() {
        @Override
        public void onDSRChanged(boolean state) {
            if(mHandler != null)
                mHandler.obtainMessage(DSR_CHANGE).sendToTarget();
        }
    };
    /*
     * Different notifications from OS will be received here (USB attached, detached, permission responses...)
     * About BroadcastReceiver: http://developer.android.com/reference/android/content/BroadcastReceiver.html
     */
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    connection = usbManager.openDevice(device);
                    //new ConnectionThread().start();
                    //mSession = new MadSession();
                    //mSession.connect(device, connection);
                    MadSessionManager.getInstance().connect(device, connection);
                    MadSensorManager.init();
                    //mSensorManager = new MadSensorManager();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);
                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                //if (!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                //if (serialPortConnected) {
                    //serialPort.syncClose();
                    //serialPort.close();
                //}
                //serialPortConnected = false;
                //mSession.close();
                //mSession = null;
                //mSensorManager = null;
                MadSensorManager.deinit();
                MadSessionManager.getInstance().disconnect();
            }
        }
    };

    protected class SensorServiceThread extends Thread {
        private volatile boolean keep = true;

        public SensorServiceThread() {
            super("SensorServiceThread");
        }

        void stopThread() {
            keep = false;
        }

        @Override
        public void run() {
            long starttime = 0;
            long endtime = 0;
            long cost = 0;
            while (keep) {
                starttime = System.currentTimeMillis();
                synchronized (MadSensorManager.mSensorList) {
                    int sensorSize = MadSensorManager.mSensorList.size();

                    for (int i = 0; i < sensorSize; i++) {
                        MadSensor sensor = MadSensorManager.mSensorList.get(i);
                        if (sensor.isEnabled()) {
                            if (!sensor.isInited()) {
                                sensor.init();
                            }

                            dispatchSensorEvent(sensor, sensor.read());
                        }
                    }
                }
                endtime = System.currentTimeMillis();
                cost = endtime - starttime;
                Log.d(TAG, "#### read & dispatch event time cost : "+ cost + " ####");

                dispatchKeyEvent(MadPlatformDevice.getInstance().readKey());
            }
        }
    }

    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    @Override
    public void onCreate() {
        this.context = this;
        //serialPortConnected = false;
        MadSensorService.SERVICE_CONNECTED = true;
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();

        new SensorServiceThread().start();
    }

    /* MUST READ about services
     * http://developer.android.com/guide/components/services.html
     * http://developer.android.com/guide/components/bound-services.html
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //serialPort.close();
        //mSession.close();
        //mSession = null;
        unregisterReceiver(usbReceiver);
        MadSensorService.SERVICE_CONNECTED = false;
    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            
            // first, dump the map for diagnostic purposes
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                Log.d(TAG, String.format("USBDevice.HashMap (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                        device.getVendorId(), device.getProductId(),
                        UsbSerialDevice.isSupported(device),
                        device.getDeviceClass(), device.getDeviceSubclass(),
                        device.getDeviceName()));
            }

            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

//                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003)) {
                if (UsbSerialDevice.isSupported(device)) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    break;
                } else {
                    connection = null;
                    device = null;
                }
            }
            if (device==null) {
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

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReceiver, filter);
    }

    /*
     * Request user permission. The response will be received in the BroadcastReceiver
     */
    private void requestUserPermission() {
        Log.d(TAG, String.format("requestUserPermission(%X:%X)", device.getVendorId(), device.getProductId() ) );
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    public class UsbBinder extends Binder {
        public MadSensorService getService() {
            return MadSensorService.this;
        }
    }

    public long getSendCmdCount(){
        return  MadSessionManager.getInstance().getSendCmdCount();
    }

    public long getSendByteCount(){
        return MadSessionManager.getInstance().getSendByteCount();
    }

    public long getRecvCmdCount(){
        return MadSessionManager.getInstance().getRecvCmdCount();
    }

    public long getRecvByteCount(){
        return MadSessionManager.getInstance().getRecvByteCount();
    }

    public long getRecvErrCount(){
        return MadSessionManager.getInstance().getRecvErrCount();
    }
}

