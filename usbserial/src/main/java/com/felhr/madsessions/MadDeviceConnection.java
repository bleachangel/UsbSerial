package com.felhr.madsessions;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

import com.felhr.protocal.ProtocalCmd;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.ArrayList;
import java.util.HashMap;

public class MadDeviceConnection {
    public static final int CONNECT_TYPE_UNKNOWN = 0;
    public static final int CONNECT_TYPE_UART = 1;
    public static final int CONNECT_TYPE_USB = 2;
    public static final int CONNECT_TYPE_NET = 3;

    //设备类型枚举
    public static final int DEVICE_TYPE_UNKNOWN = 0;
    public static final int DEVICE_TYPE_DEVICE = 1;
    public static final int DEVICE_TYPE_DONGLE = 2;

    private boolean mConnected;
    private UsbSerialDevice mSerialDevice;
    private UsbDevice mUsbDevice;
    private UsbDeviceConnection mUsbConnection;
    private MadUartParameters mUartParams;
    private int mConnectType;
    private int mDeviceType;
    private String mDeviceName;

    //设备类型
    public static final long CAPACITY_DEVICE_MASK = 0x00000001L;
    public static final long CAPACITY_DONGLE_MASK = 0x00000002L;

    //sensor 能力
    public static final long CAPACITY_ACCELERATOR_MASK = 0x00000004L;
    public static final long CAPACITY_GYROSCOPE_MASK = 0x00000008L;
    public static final long CAPACITY_MAGNETIC_MASK = 0x00000010L;
    public static final long CAPACITY_AMBIENT_LIGHT_MASK = 0x00000020L;
    public static final long CAPACITY_PROXIMITY_MASK = 0x00000040L;

    //闪关灯能力
    public static final long CAPACITY_NORMAL_FLASH_LIGHT_MASK = 0x00000080L;
    public static final long CAPACITY_INFRARED_FLASH_LIGHT_MASK = 0x00000100L;

    //摄像头能力
    public static final long CAPACITY_NORMAL_CAMERA_MASK = 0x00000200L;
    public static final long CAPACITY_INFRARED_CAMERA_MASK = 0x00000400L;
    public static final long CAPACITY_TOF_CAMERA_MASK = 0x00000800L;

    //屏幕能力
    public static final long CAPACITY_LCD_ENABLE_MASK = 0x00001000L;
    public static final long CAPACITY_LCD_BRIGHTNESS_MASK = 0x00002000L;

    //显示输出能力
    public static final long CAPACITY_DISPLAY_ENABLE_MASK = 0x00004000L;

    //显示输入能力
    public static final long CAPACITY_SOURCES_IN_MASK = 0x00008000L;

    //音频能力
    public static final long CAPACITY_AUDIO_VOLUME_MASK = 0x00010000L;
    public static final long CAPACITY_AUDIO_CHANNEL_MASK = 0x00020000L;
    public static final long CAPACITY_MIC_VOLUME_MASK = 0x00040000L;

    //按键能力
    public static final long CAPACITY_KEY_MASK = 0x00080000L;

    //信息获取能力
    public static final long CAPACITY_DEVICE_INFO_MASK = 0x00100000L;

    //默认全功能掩码
    public static final long CAPACITY_ALL_MASK = 0xFFFFFFFFL;

    private long mCapacity;

    public static final int DEFAULT_CONNECTION_CALLBACK_COUNT = 64;
    public HashMap<Integer, MadConnectionCallback> mCallbackList;
    private UsbSerialInterface.UsbReadCallback mReadCallback;

    //用于标识设备连接
    private int mConnectionID;

    public MadDeviceConnection(int connectionID, UsbDevice usbDevice, UsbDeviceConnection usbConnection){
        mConnectionID = connectionID;

        mConnected = false;
        mSerialDevice = null;
        mUsbDevice = usbDevice;
        mUsbConnection = usbConnection;
        mUartParams = null;
        mConnectType = CONNECT_TYPE_UNKNOWN;
        mDeviceType = DEVICE_TYPE_UNKNOWN;
        mCapacity = 0;
        mDeviceName = new String("NULL");

        mCallbackList = new HashMap<Integer, MadConnectionCallback>(DEFAULT_CONNECTION_CALLBACK_COUNT);
        mReadCallback = new UsbSerialInterface.UsbReadCallback(){
            public void onReceivedData(ProtocalCmd cmd) {
                if(cmd != null) {
                    MadConnectionCallback cb = mCallbackList.get(cmd.getSessionID());
                    if(cb != null){
                        cb.onReceivedData(cmd);
                    }
                }
            }
            public void onReceivedDataForTest(byte[] data) {
            }
        };
    }

    public int setCapacity(long capacity){
        int ret = 0;
        mCapacity = capacity;
        if((mCapacity & CAPACITY_DEVICE_MASK) != 0){
            mDeviceType = DEVICE_TYPE_DEVICE;
        } else if ((mCapacity & CAPACITY_DONGLE_MASK) != 0){
            mDeviceType = DEVICE_TYPE_DONGLE;
        } else {
            mDeviceType = DEVICE_TYPE_UNKNOWN;
        }
        return ret;
    }

    public int setDeviceName(String deviceName){
        int ret = 0;
        if(deviceName == null){
            String nullStr = "NULL";
            mDeviceName = nullStr;
        } else {
            mDeviceName = deviceName;
        }
        return ret;
    }

    public String getDeviceName(){
        return mDeviceName;
    }

    public boolean isSupported(long capacity){
        boolean ret = false;
        if((capacity & mCapacity) != 0){
            ret = true;
        }

        return ret;
    }

    public boolean isSupportSensor(){
        boolean ret = false;
        long sensor_mask = (CAPACITY_ACCELERATOR_MASK | CAPACITY_GYROSCOPE_MASK | CAPACITY_MAGNETIC_MASK
                            | CAPACITY_AMBIENT_LIGHT_MASK | CAPACITY_PROXIMITY_MASK);

        if((mCapacity & sensor_mask) != 0) {
            ret = true;
        }
        return ret;
    }

    public boolean isSupportFlashLight(){
        boolean ret = false;
        long mask = (CAPACITY_NORMAL_FLASH_LIGHT_MASK | CAPACITY_INFRARED_FLASH_LIGHT_MASK );
        if((mCapacity & mask) != 0) {
            ret = true;
        }
        return ret;
    }

    public boolean isSupportCamera(){
        boolean ret = false;
        long mask = (CAPACITY_NORMAL_CAMERA_MASK | CAPACITY_INFRARED_CAMERA_MASK | CAPACITY_TOF_CAMERA_MASK);
        if((mCapacity & mask) != 0) {
            ret = true;
        }
        return ret;
    }

    public boolean isSupportLCD(){
        boolean ret = false;
        long mask = (CAPACITY_LCD_ENABLE_MASK | CAPACITY_LCD_BRIGHTNESS_MASK);
        if((mCapacity & mask) != 0) {
            ret = true;
        }
        return ret;
    }

    public boolean isSupportDisplayControl(){
        boolean ret = false;
        long mask = CAPACITY_DISPLAY_ENABLE_MASK;
        if((mCapacity & mask) != 0) {
            ret = true;
        }
        return ret;
    }

    public boolean isSupportSourcesIn(){
        boolean ret = false;
        long mask = CAPACITY_SOURCES_IN_MASK;
        if((mCapacity & mask) != 0) {
            ret = true;
        }
        return ret;
    }

    public boolean isSupportAudio(){
        boolean ret = false;
        long mask = CAPACITY_AUDIO_VOLUME_MASK | CAPACITY_AUDIO_CHANNEL_MASK;
        if((mCapacity & mask) != 0) {
            ret = true;
        }
        return ret;
    }

    public boolean isSupportMicrophone(){
        boolean ret = false;
        long mask = CAPACITY_MIC_VOLUME_MASK;
        if((mCapacity & mask) != 0) {
            ret = true;
        }
        return ret;
    }

    public long getCapacity(){
        return mCapacity;
    }

    public UsbSerialDevice getSerialDevice(){
        return mSerialDevice;
    }

    public int getConnectType(){
        return mConnectType;
    }

    public int getDeviceType(){
        //默认为device类型
        return mDeviceType;
    }

    public UsbDevice getUsbDevice(){
        return mUsbDevice;
    }

    public int getConnectionID(){
        return mConnectionID;
    }

    public ArrayList<Integer> getBindedSessionList(){
        ArrayList<Integer> sessionList = null;
        synchronized (mCallbackList) {
            sessionList = new ArrayList<Integer>(mCallbackList.keySet());
        }
        return sessionList;
    }

    //当前的session是否已经与该设备连接绑定
    public boolean isBind(int sessionID){
        return mCallbackList.containsKey(sessionID);
    }

    public int bind(int sessionID, MadConnectionCallback call_back){
        int ret = 0;
        synchronized (mCallbackList) {
            if (mCallbackList != null) {
                mCallbackList.put(sessionID, call_back);
            }
        }
        return ret;
    }

    public int unbind(int sessionID){
        int ret = 0;
        synchronized (mCallbackList) {
            if (mCallbackList != null) {
                mCallbackList.remove(sessionID);
            }
        }
        return ret;
    }

    public int unbindAll(){
        int ret = 0;
        synchronized (mCallbackList) {
            if (mCallbackList != null) {
                mCallbackList.clear();
            }
        }
        return ret;
    }

    //开启数据的接收
    public int start(){
        int ret = -1;
        if(mConnectType == CONNECT_TYPE_UART) {
            if(mSerialDevice != null) {
                ret = mSerialDevice.resume();
            }
        }
        return ret;
    }

    //暂停数据的接收
    public int stop(){
        int ret = -1;
        if(mConnectType == CONNECT_TYPE_UART) {
            if(mSerialDevice != null) {
                ret = mSerialDevice.pause();
            }
        }
        return ret;
    }

    //打开连接，数据传输默认关闭
    public int open(int connectType, MadConnectionParameters params){
        int ret = -1;
        mConnectType = connectType;
        if(mConnectType == CONNECT_TYPE_UART) {
            mUartParams = (MadUartParameters) params;
            if(mUsbDevice != null && mUsbConnection != null && mUartParams != null) {
                UsbSerialDevice serial = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, mUsbConnection);
                if (serial != null && serial.open()) {
                    serial.setBaudRate(mUartParams.mBandRate);
                    serial.setDataBits(mUartParams.mDataBits);
                    serial.setStopBits(mUartParams.mStopBits);
                    serial.setParity(mUartParams.mParity);
                    serial.setFlowControl(mUartParams.mFlowCtrl);

                    //注册获取数据回调
                    serial.setReadCallback(mReadCallback);
                    mSerialDevice = serial;
                    ret = 0;
                }
            }
        }
        return ret;
    }

    public int close(){
        int ret = -1;
        if(mConnectType == CONNECT_TYPE_UART) {
            if(mSerialDevice != null) {
                mSerialDevice.close();
                mUartParams = null;
                ret = 0;
            }
        }
        return ret;
    }

    public int syncWrite(byte[] buffer, int timeout) {
        int ret = -1;
        if (buffer == null)
            return ret;

        if(mConnectType == CONNECT_TYPE_UART){
            if(mSerialDevice != null) {
                ret = mSerialDevice.syncWrite(buffer, timeout);
            }
        }
        return ret;
    }

    public int syncRead(byte[] buffer, int timeout) {
        int ret = -1;
        if (buffer == null)
            return ret;

        if(mConnectType == CONNECT_TYPE_UART){
            if(mSerialDevice != null) {
                ret = mSerialDevice.syncRead(buffer, timeout);
            }
        }
        return ret;
    }

    public long getRecvCount(){
        long ret = 0;
        if(mConnectType == CONNECT_TYPE_UART){
            if(mSerialDevice != null) {
                ret = mSerialDevice.getRecvCount();
            }
        }
        return ret;
    }

    public long getErrCount(){
        long ret = 0;
        if(mConnectType == CONNECT_TYPE_UART) {
            if (mSerialDevice != null) {
                ret = mSerialDevice.getErrCount();
            }
        }
        return ret;
    }
}
