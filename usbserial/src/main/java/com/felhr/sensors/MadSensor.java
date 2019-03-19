package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public abstract class MadSensor {
    public int mSensorType;
    public byte mChannel;
    public byte mSlaveAddr;
    public boolean mEnable;
    public boolean mInited;
    public MadSession mSession = new MadSession();

    public MadSensor(){
        mChannel = 0;
        mSlaveAddr = 0;
        mEnable = false;
    }

    public boolean isEnabled(){
        return mEnable;
    }

    public boolean isInited(){
        return mInited;
    }

    public abstract boolean init();
    public abstract boolean enable(boolean enable);
    public abstract MadSensorEvent read();
}
