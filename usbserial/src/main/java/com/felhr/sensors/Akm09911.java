package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class Akm09911 extends MadSensor {
    public static final byte WIA_ADDR = 0x0;
    public static final byte ST1_ADDR = 0x10;
    public static final byte MAG_DATA_START_ADDR = 0x11;
    public static final byte CNTL2_ADDR = 0x31;
    public static final byte CNTL3_ADDR = 0x32;
    public static final byte ASAX_ADDR = 0x60;

    public static final byte COMPANY_ID_VALUE = 0x48;
    public static final byte DEVICE_ID_VALUE = 0x05;

    public static final byte POWER_DOWN_MODE_VALUE = 0x0;
    public static final byte SINGLE_MEAS_MODE_VALUE = 0x1;
    public static final byte CONTINUOUS_MEAS_MODE_1_VALUE = 0x2;
    public static final byte CONTINUOUS_MEAS_MODE_2_VALUE = 0x4;
    public static final byte CONTINUOUS_MEAS_MODE_3_VALUE = 0x6;
    public static final byte CONTINUOUS_MEAS_MODE_4_VALUE = 0x8;
    public static final byte FUSE_ROM_MODE_VALUE = 0x1F;

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
        int size = mSession.writeI2C(mChannel, mSlaveAddr, CNTL3_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, reset, MadSession.RESULT_TIME_OUT);

        //read ic id
        byte[] icID = mSession.readI2C(mChannel, mSlaveAddr, WIA_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, 2,MadSession.RESULT_TIME_OUT);
        if(icID == null || !(icID.length == 2 && icID[0] == COMPANY_ID_VALUE && icID[1] == DEVICE_ID_VALUE)){
            mInited = false;
            return  false;
        }

        //set fuse rom mode
        byte fuse[] = {FUSE_ROM_MODE_VALUE};
        size = mSession.writeI2C(mChannel, mSlaveAddr, CNTL2_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, fuse, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            byte senseinfo[] = {0, 0, 0};
            size = mSession.writeI2C(mChannel, mSlaveAddr, ASAX_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, senseinfo, MadSession.RESULT_TIME_OUT);
            mInited = true;
        }

        enable(false);
        return true;
    }

    public boolean enable(boolean enable){
        boolean ret = false;
        byte mode = 0;
        int repeatReadSize = 9;
        //set single measurement mode
        byte enableValue[] = new byte[1];

        if(enable){
            enableValue[0] = SINGLE_MEAS_MODE_VALUE;
            mode = 1;
        } else {
            enableValue[0] = POWER_DOWN_MODE_VALUE;
        }

        int size = mSession.writeI2C(mChannel, mSlaveAddr, CNTL2_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, enableValue, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = true;
            mEnable = enable;

            mSession.configI2C(mChannel, mSlaveAddr, ST1_ADDR,
                    CNTL2_ADDR, SINGLE_MEAS_MODE_VALUE, (int)10,MadSession.I2C_REGISTER_ADDR_MODE_8,
                    (byte)mode, (byte)repeatReadSize, MadSession.RESULT_TIME_OUT);
        }

        return ret;
    }

    @Override
    public boolean getStatus() {
        boolean ret = false;
        byte[] state;
        state = mSession.readI2C(mChannel, mSlaveAddr, CNTL2_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, 1, MadSession.RESULT_TIME_OUT);
        if(state != null && state.length == 1){
            if(state[0] > 0) {
                ret = true;
            } else {
                ret = false;
            }
        }

        return  ret;
    }

    public MadSensorEvent read(){
        int size = 9;
        byte[] status = mSession.readI2CAsync(mChannel, mSlaveAddr, ST1_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, size, mTimeOut/1000);
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
        event.values[0] = (float)(short)((status[5]&0xFF)|((status[6]<<8)&0xFF00));
        //y
        event.values[1] = (float)(short)((status[3]&0xFF)|((status[4]<<8)&0xFF00));
        //z
        event.values[2] = (float)(short)((status[1]&0xFF)|((status[2]<<8)&0xFF00));

        return event;
    }
}
