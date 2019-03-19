package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class LTR579 {
    public byte mChannel;
    public byte mSlaveAddr;
    public boolean mEnableAls;
    public boolean mEnablePs;
    public boolean mInited;
    private static LTR579 mInstance = null;

    private LTR579(){
        mChannel = 0;
        mSlaveAddr = 0x53;
        mEnableAls = false;
        mEnablePs = false;
        mInited = false;
    }

    public static LTR579 getInstance(){
        if(mInstance == null){
            mInstance = new LTR579();
        }
        return mInstance;
    }

    public boolean enableAls(MadSession sesson, boolean enable){
        boolean ret = false;
        int len = 1;
        mEnableAls = enable;
        byte[] status = sesson.readI2C(mChannel, mSlaveAddr, 0x0, MadSession.I2C_REGISTER_ADDR_MODE_8, len,MadSession.RESULT_TIME_OUT);
        if(status == null) {
            return false;
        }


        if(status.length != len){
            return false;
        }

        if(enable){
            status[0] |= 0x2;
        } else {
            status[0] &= ~0x2;
        }

        int size = sesson.writeI2C(mChannel, mSlaveAddr, 0x0, MadSession.I2C_REGISTER_ADDR_MODE_8, status, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = true;
        }
        return ret;
    }

    public boolean enablePs(MadSession sesson, boolean enable){
        boolean ret = false;

        mEnablePs = enable;
        byte[] status = sesson.readI2C(mChannel, mSlaveAddr, 0x0, MadSession.I2C_REGISTER_ADDR_MODE_8, 1,MadSession.RESULT_TIME_OUT);
        if(status == null) {
            return false;
        }

        if(status.length == 1){
            return false;
        }

        if(enable){
            status[0] |= 0x1;
        } else {
            status[0] &= ~0x1;
        }

        int size = sesson.writeI2C(mChannel, mSlaveAddr, 0x0, MadSession.I2C_REGISTER_ADDR_MODE_8, status, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = true;
        }
        return ret;
    }

    public boolean init(MadSession sesson){
        if(mInited){
            return true;
        }

        //read ic id
        byte[] icID = sesson.readI2C(mChannel, mSlaveAddr, 0x06, MadSession.I2C_REGISTER_ADDR_MODE_8, 1,MadSession.RESULT_TIME_OUT);
        if(icID == null){
            return  false;
        } else if(icID.length == 1 && icID[0] == (byte)0xB1) {
            byte[] ps_pulse={32};
            int size = sesson.writeI2C(mChannel, mSlaveAddr, 0x2, MadSession.I2C_REGISTER_ADDR_MODE_8, ps_pulse, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }

            byte[] ps_led={0x36};
            size = sesson.writeI2C(mChannel, mSlaveAddr, 0x1, MadSession.I2C_REGISTER_ADDR_MODE_8, ps_led, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }

            byte[] ps_mesrate={0x5c};
            size = sesson.writeI2C(mChannel, mSlaveAddr, 0x3, MadSession.I2C_REGISTER_ADDR_MODE_8, ps_mesrate, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }
            enablePs(sesson, false);

            byte[] als_gain={0x1};
            size = sesson.writeI2C(mChannel, mSlaveAddr, 0x5, MadSession.I2C_REGISTER_ADDR_MODE_8, als_gain, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }

            byte[] als_mesrate={0x22};
            size = sesson.writeI2C(mChannel, mSlaveAddr, 0x4, MadSession.I2C_REGISTER_ADDR_MODE_8, als_mesrate, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }
            enableAls(sesson, false);
            mInited = true;
        }
        return true;
    }

    public boolean deinit(){
        mInited = false;
        return  true;
    }

    public byte[] readPs(MadSession sesson){
        byte[] status = null;
        if(enablePs(sesson, true)) {
            int size = 2;
            status = sesson.readI2C(mChannel, mSlaveAddr, 0x8, MadSession.I2C_REGISTER_ADDR_MODE_8, size, MadSession.RESULT_TIME_OUT);
            if (status == null || status.length != size) {
                return null;
            }
        }

        return status;
    }

    public byte[] readAls(MadSession sesson){
        byte[] status = null;
        if(enableAls(sesson, true)) {
            int size = 3;
            status = sesson.readI2C(mChannel, mSlaveAddr, 0xD, MadSession.I2C_REGISTER_ADDR_MODE_8, size, MadSession.RESULT_TIME_OUT);
            if (status == null || status.length != size) {
                return null;
            }
        }

        return status;
    }
}
