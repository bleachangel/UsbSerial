package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class LTR579Ps extends MadSensor {
    public LTR579Ps(){
        super();
        mChannel = LTR579.getInstance().mChannel;
        mSlaveAddr = LTR579.getInstance().mSlaveAddr;
        mInited = false;
        mSensorType = MadSensorManager.MAD_SENSOR_TYPE_PROXIMITY;
    }

    public boolean init(){
        if(LTR579.getInstance().init(mSession)){
            mInited = true;
        } else {
            mInited = false;
        }
        return mInited;
    }

    public boolean enable(boolean enable){
        boolean ret = false;

        mEnable = enable;
        if(LTR579.getInstance().enablePs(mSession, enable)){
            ret = true;
        }
        return ret;
    }

    public MadSensorEvent read(){
        byte[] status = LTR579.getInstance().readPs(mSession);
        if(status == null || status.length != 2){
            return null;
        }

        MadSensorEvent event = new MadSensorEvent(1);
        event.sensor = this;
        int ps = (int)((status[0]&0xFF)|((status[1]<<8)&0x0700));
        event.values[0] = ps;
        return event;
    }
}
