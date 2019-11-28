package com.felhr.sensors;

import com.felhr.madsessions.MadConnectionManager;
import com.felhr.madsessions.MadDeviceConnection;

public class LTR579Als extends MadSensor {
    static long mCapacity = MadConnectionManager.getDeviceCapacity()| MadDeviceConnection.CAPACITY_AMBIENT_LIGHT_MASK;
    public LTR579Als(){
        super(mCapacity);
        mChannel = LTR579.getInstance().mChannel;
        mSlaveAddr = LTR579.getInstance().mSlaveAddr;
        mInited = false;
        mSensorType = MadSensorManager.MAD_SENSOR_TYPE_AMBIENT_LIGHT;
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

        if(LTR579.getInstance().enableAls(mSession, enable)){
            ret = true;
            mEnable = enable;
            byte mode = 0;
            if(enable){
                mode = 1;
            }
            LTR579.getInstance().configureAls(mSession, mode);
        }
        return ret;
    }

    @Override
    public boolean getStatus() {
        boolean ret = false;
        int state = 0;
        if((state = LTR579.getInstance().getAlsEnable(mSession)) >= 0){
            if(state > 0) {
                ret = true;
            } else {
                ret = false;
            }
        }

        return  ret;
    }

    public MadSensorEvent read(){
        long curtime[] = new long[1];
        byte[] status = LTR579.getInstance().readAls(mSession, curtime);
        if(status == null || status.length != LTR579.ALS_DATA_SIZE){
            return null;
        }

        MadSensorEvent event = new MadSensorEvent(1);
        event.sensor = this;

        int[] als_level = {0, 1, 2, 3, 4, 5, 6, 8, 10, 13, 17, 22, 28, 36, 46, 58, 72, 88, 106, 126, 148, 172, 200, 250, 300, 400, 500, 600, 800, 1000, 1500};
        int[] als_value = {0, 10, 25, 40, 60, 80, 100, 130, 180, 230, 280, 350, 450, 550, 650, 780, 880, 980, 1100, 1200, 1300, 1500, 1600, 1650, 1700, 1800, 1900, 2000, 2500, 3000, 4000};

        int als = 0;
        als = (int)((status[0]&0xFF)|((status[1]<<8)&0xFF00) | ((status[2]<<16)&0xFF0000));

        int level_length = als_level.length;
        int value = als_value[0];
        for(int i = 0; i < level_length - 1; i++){
            if(als >= als_level[i] && als < als_level[i+1]){
                value = als_value[i];
                break;
            }
        }

        event.values[0] = value;
        event.timestamp = curtime[0];
        return event;
    }
}
