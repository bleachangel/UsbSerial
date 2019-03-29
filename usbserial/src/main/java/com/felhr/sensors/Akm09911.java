package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class Akm09911 extends MadSensor {
    public Akm09911(){
        super();
        mInited = false;
        mChannel = 0;
        mSlaveAddr = 0x0c;
        mSensorType = MadSensorManager.MAD_SENSOR_TYPE_MAGNETIC;
    }

    public boolean init(){
        byte reset[] = {1};
        //reset
        int size = mSession.writeI2C(mChannel, mSlaveAddr, 0x32, MadSession.I2C_REGISTER_ADDR_MODE_8, reset, MadSession.RESULT_TIME_OUT);

        //read ic id
        byte[] icID = mSession.readI2C(mChannel, mSlaveAddr, 0, MadSession.I2C_REGISTER_ADDR_MODE_8, 2,MadSession.RESULT_TIME_OUT);
        if(icID == null || !(icID.length == 2 && icID[0] == 0x48 && icID[1] == 0x05)){
            mInited = false;
            return  false;
        }

        //set fuse rom mode
        byte fuse[] = {0x1F};
        size = mSession.writeI2C(mChannel, mSlaveAddr, 0x31, MadSession.I2C_REGISTER_ADDR_MODE_8, fuse, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            byte senseinfo[] = {0, 0, 0};
            size = mSession.writeI2C(mChannel, mSlaveAddr, 0x60, MadSession.I2C_REGISTER_ADDR_MODE_8, senseinfo, MadSession.RESULT_TIME_OUT);
            mInited = true;
        }

        enable(false);
        return true;
    }

    public boolean enable(boolean enable){
        boolean ret = false;
        //set single measurement mode
        byte enableValue[] = new byte[1];
        mEnable = enable;
        if(mEnable){
            enableValue[0] = 0x1;
        } else {
            enableValue[0] = 0x0;
        }

        int size = mSession.writeI2C(mChannel, mSlaveAddr, 0x31, MadSession.I2C_REGISTER_ADDR_MODE_8, enableValue, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = true;
        }
        return ret;
    }

    public MadSensorEvent read(){
        enable(true);

        int size = 9;
        //byte[] status = mSession.readI2C(mChannel, mSlaveAddr, 0x10, MadSession.I2C_REGISTER_ADDR_MODE_8, size,MadSession.RESULT_TIME_OUT);
        byte[] status = mSession.readI2CAsync(mChannel, mSlaveAddr, 0x10, MadSession.I2C_REGISTER_ADDR_MODE_8, size,MadSession.RESULT_TIME_OUT);
        if(status == null || status.length != size){
            return null;
        }

        //check data is ready
        if((status[0] & 0x1) != 1){
            return null;
        }

        MadSensorEvent event = new MadSensorEvent(3);
        event.sensor = this;

        //x
        event.values[0] = (float)(short)((status[1]&0xFF)|((status[2]<<8)&0xFF00));
        //y
        event.values[1] = (float)(short)((status[3]&0xFF)|((status[4]<<8)&0xFF00));
        //z
        event.values[2] = (float)(short)((status[5]&0xFF)|((status[6]<<8)&0xFF00));

        return event;
    }
}
