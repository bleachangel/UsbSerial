package com.felhr.madsessions;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;


import com.felhr.protocal.ProtocalCmd;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;

public class MadSessionManager {
    private static final MadSessionManager ourInstance = new MadSessionManager();
    private Map<Integer,MadSession> mSessions = new HashMap<Integer, MadSession>();
    public static final int MIN_SESSION_ID = 1;
    public static final int MAX_SESSION_ID = 0xFFFF;
    public static final int BAUD_RATE = 921600;
    private int mSessionGenerator = MIN_SESSION_ID;

    private UsbSerialDevice mSerialDevice = null;
    private boolean mSerialPortConnected = false;

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(ProtocalCmd cmd) {
            MadSession session = mSessions.get(cmd.getSessionID());
            if(session != null){
                session.appendCmdResult(cmd);
            }
        }
    };

    public static MadSessionManager getInstance() {
        return ourInstance;
    }

    private MadSessionManager() {
    }

    public boolean registerSession(int sessionID, MadSession session){
        boolean ret = false;
        if(!mSessions.containsKey(sessionID) && session != null && !mSessions.containsValue(session)){
            mSessions.put(sessionID, session);
            ret = true;
        }

        return ret;
    }

    public boolean unregisterSession(int sessionID){
        boolean ret = false;
        if(mSessions.containsKey(sessionID) ){
            mSessions.remove(sessionID);
        }

        return ret;
    }

    public boolean connect(UsbDevice device, UsbDeviceConnection connection){
        boolean ret = false;
        //if(mSerialDevice != null){
        //    return true;
        //}

        mSerialDevice = UsbSerialDevice.createUsbSerialDevice(device, connection);
        if (mSerialDevice != null) {
            if (mSerialDevice.open()) {
                mSerialPortConnected = true;
                mSerialDevice.setBaudRate(BAUD_RATE);
                mSerialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
                mSerialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);
                mSerialDevice.setParity(UsbSerialInterface.PARITY_NONE);

                mSerialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                mSerialDevice.setReadCallback(mCallback);
                //mSerialDevice.getCTS(ctsCallback);
                //mSerialDevice.getDSR(dsrCallback);

                ret = true;
            } else {
                mSerialDevice = null;
            }
        }

        return  ret;
    }

    public boolean isConnected(){
        return  mSerialPortConnected;
    }

    public UsbSerialDevice getSerialDevice(){
        return mSerialDevice;
    }

    public boolean disconnect(){
        boolean ret = false;

        if(mSerialDevice != null){
            mSerialDevice.close();
            mSerialPortConnected = false;
        }
        return ret;
    }

    public int newID(){
        int sessionID;
        sessionID = mSessionGenerator++;
        while (mSessions.containsKey(sessionID)) {
            sessionID ++;
            if(sessionID > MAX_SESSION_ID){
                sessionID = MIN_SESSION_ID;
            }
        }

        mSessionGenerator = sessionID;
        return sessionID;
    }

    public boolean deleteID(int sessionID){
        if(mSessions.containsKey(sessionID)){
            mSessions.remove(sessionID);
        }
        return  true;
    }
}
