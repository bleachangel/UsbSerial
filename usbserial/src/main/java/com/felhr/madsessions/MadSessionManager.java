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
    public static final int BROADCAST_SESSION_ID = 0;
    public static final int MIN_SESSION_ID = 1;
    public static final int MAX_SESSION_ID = 0xFFFF;
    public static final int BAUD_RATE = 921600;
    private int mSessionGenerator = MIN_SESSION_ID;

    private UsbSerialDevice mSerialDevice = null;
    private boolean mSerialPortConnected = false;

    private long mSendCmdCount;
    private long mSendByteCount;

    private long mRecvCmdCount;
    private long mRecvByteCount;

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
        mSendCmdCount = 0;
        mSendByteCount = 0;
        mRecvCmdCount = 0;
        mRecvByteCount = 0;
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
        synchronized (this) {
            return mSerialPortConnected;
        }
    }

    public UsbSerialDevice getSerialDevice(){
        synchronized (this) {
            return mSerialDevice;
        }
    }

    public boolean disconnect(){
        boolean ret = false;

        synchronized (this) {
            if (mSerialDevice != null) {
                mSerialDevice.close();
                mSerialPortConnected = false;
            }
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

    public void statSendCmd(int size){
        synchronized (this) {
            if (mSendCmdCount + 1 > Long.MAX_VALUE) {
                mSendCmdCount = 0;
            }
            mSendCmdCount++;

            if (mSendByteCount + size > Long.MAX_VALUE) {
                mSendByteCount = 0;
            }

            mSendByteCount += size;
        }
    }

    public void statRecvCmd(){
        synchronized (this) {
            if (mRecvCmdCount + 1 > Long.MAX_VALUE) {
                mRecvCmdCount = 0;
            }
            mRecvCmdCount++;
        }
/*
        if (mRecvByteCount + size > Long.MAX_VALUE) {
            mRecvByteCount = 0;
        }

        mSendByteCount += size;*/
    }

    public long getSendCmdCount(){
        return  mSendCmdCount;
    }

    public long getSendByteCount(){
        return mSendByteCount;
    }

    public long getRecvCmdCount(){
        return mRecvCmdCount;
    }

    public long getRecvByteCount(){
        if(mSerialDevice == null){
            return 0;
        }

        return mSerialDevice.getRecvCount();
    }

    public long getRecvErrCount(){
        if(mSerialDevice == null){
            return 0;
        }

        return mSerialDevice.getErrCount();
    }
}
