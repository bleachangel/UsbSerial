package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class BMI160Gy extends MadSensor {
    /*
     *1 rad = PI*degree/180, about 3.1416*degree/180
     *1 degree = rad*180/PI, about rad*180/3.1416
     */
    public static final int DEGREE_TO_RAD=57;

    public BMI160Gy(){
        super();
        mInited = false;
        mChannel = BMI160.getInstance().mChannel;
        mSlaveAddr = BMI160.getInstance().mSlaveAddr;
        mSensorType = MadSensorManager.MAD_SENSOR_TYPE_GYROSCOPE;
    }

    public boolean init(){
        if(BMI160.getInstance().init(mSession)){
            mInited = true;
        } else {
            mInited = false;
        }
        return mInited;
    }

    public boolean enable(boolean enable){
        boolean ret = false;
        mEnable = enable;
        if(BMI160.getInstance().enableGy(mSession, enable)){
            ret = true;
        }
        return ret;
    }

    public MadSensorEvent read(){
        byte[] status = BMI160.getInstance().readGy(mSession);
        if(status == null || status.length != 6){
            return null;
        }

        MadSensorEvent event = new MadSensorEvent(3);
        event.sensor = this;

        int sensitivity = BMI160.getInstance().getGySensitivity();
        //x
        event.values[0] = (float)((short)((status[0]&0xFF)|((status[1]<<8)&0xFF00)))/sensitivity/DEGREE_TO_RAD;
        //y
        event.values[1] = (float)((short)((status[2]&0xFF)|((status[3]<<8)&0xFF00)))/sensitivity/DEGREE_TO_RAD;
        //z
        event.values[2] = (float)((short)((status[4]&0xFF)|((status[5]<<8)&0xFF00)))/sensitivity/DEGREE_TO_RAD;

        return event;
    }
}
