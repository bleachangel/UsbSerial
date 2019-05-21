package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class LTR579Als extends MadSensor {

    public LTR579Als(){
        super();
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

        int[] als_level = {0, 11, 26, 41, 61, 81, 101, 133, 211, 305, 411, 503, 637, 751, 881, 1027, 2297, 5588, 6730, 11301, 20449, 25768, 31122, 57937, 57937, 57937, 57937, 57937, 57937, 57937, 57937};
        int[] als_value = {0, 10, 25, 40, 60, 80, 100, 132, 210, 304, 410, 502, 636, 750, 880, 1004, 2003, 3006, 5006, 8004, 10000, 12000, 16000, 20000, 20000, 20000, 20000, 20000, 20000, 20000, 20000};

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
