package com.felhr.sensors;

import com.felhr.madsessions.MadSession;
import com.felhr.madsessions.MadSessionManager;

public abstract class MadSensor {
    public int mSensorType;
    public byte mChannel;
    public byte mSlaveAddr;
    public boolean mEnable;
    public boolean mInited;
    public long mTimeOut; //unit is us.
    public MadSession mSession;

    public MadSensor(long capacity){
        mChannel = 0;
        mSlaveAddr = 0;
        mEnable = false;
        mTimeOut = 0;

        mSession = MadSessionManager.getInstance().createSession(capacity);
    }

    public boolean isEnabled(){
        return mEnable;
    }

    public boolean isInited(){
        return mInited;
    }

    public int release(){
        return MadSessionManager.getInstance().releaseSession(mSession.mSessionID);
    }

    public abstract boolean init();
    public abstract boolean enable(boolean enable);
    public abstract boolean getStatus();
    public abstract MadSensorEvent read();
}
