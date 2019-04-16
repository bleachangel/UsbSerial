package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class BMI160A extends MadSensor {
    private static final float GRAVITY_EARTH=9.807f;
    private int mSensitivity;

    public BMI160A(){
        super();
        mInited = false;
        mChannel = BMI160.getInstance().mChannel;
        mSlaveAddr = BMI160.getInstance().mSlaveAddr;
        mSensorType = MadSensorManager.MAD_SENSOR_TYPE_ACCELERATOR;
        mSensitivity = BMI160.getInstance().getAccSensitivity();
    }

    public boolean init(){
        if(BMI160.getInstance().init(mSession)){
            mInited = true;
        } else {
            mInited = false;
        }
        return mInited;
    }

    public boolean getStatus(){
        boolean ret = false;
        int state = 0;
        if((state = BMI160.getInstance().getAccPower(mSession)) >= 0){
            if(state > 0) {
                ret = true;
            } else {
                ret = false;
            }
        }

        return  ret;
    }

    public boolean enable(boolean enable){
        boolean ret = false;

        if(BMI160.getInstance().enableAcc(mSession, enable)){
            ret = true;
            mEnable = enable;
        }
        return ret;
    }

    public MadSensorEvent read(){
        byte[] status = BMI160.getInstance().readAcc(mSession);
        if(status == null || status.length != 6){
            return null;
        }

        MadSensorEvent event = new MadSensorEvent(3);
        event.sensor = this;

        //x'= z, y' = y, z' = x
        //x
        event.values[0] = (float)(short)((status[4]&0xFF)|((status[5]<<8)&0xFF00));
        //y
        event.values[1] = (float)(short)((status[2]&0xFF)|((status[3]<<8)&0xFF00));
        //z
        event.values[2] = (float)(short)((status[0]&0xFF)|((status[1]<<8)&0xFF00));

        //x
        event.values[0] = (float)event.values[0]*GRAVITY_EARTH/mSensitivity;
        //y
        event.values[1] = (float)event.values[1]*GRAVITY_EARTH/mSensitivity;
        //z
        event.values[2] = (float)event.values[2]*GRAVITY_EARTH/mSensitivity;

        return event;
    }
}
