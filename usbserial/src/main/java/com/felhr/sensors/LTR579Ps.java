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

    @Override
    public boolean getStatus() {
        boolean ret = false;
        int state = 0;
        if((state = LTR579.getInstance().getPsEnable(mSession)) >= 0){
            if(state > 0) {
                ret = true;
            } else {
                ret = false;
            }
        }

        return  ret;
    }

    public MadSensorEvent read(){
        byte[] status = LTR579.getInstance().readPs(mSession);
        if(status == null || status.length != 2){
            return null;
        }

        MadSensorEvent event = new MadSensorEvent(1);
        event.sensor = this;
        int ps = (int)((status[0]&0xFF)|((status[1]<<8)&0x0700));
        int[] ps_level={1800, 1600, 1400, 1200, 1000, 800, 700, 600, 500, 400, 300, 200, 150, 100, 50, 0};
        float[] ps_value={0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f};
        int level_length = ps_level.length;
        float value = ps_value[0];
        for(int i = 0; i < level_length; i++){
            if(ps >= ps_level[i]){
                value = ps_value[i];
                break;
            }
        }
        event.values[0] = value;
        return event;
    }
}
