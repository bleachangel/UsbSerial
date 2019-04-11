package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class LTR579 {
    public static final byte MAIN_CTRL_ADDR = (byte)0x0;
    public static final byte PS_LED_ADDR = (byte)0x1;
    public static final byte PS_PULSES_ADDR = (byte)0x2;
    public static final byte PS_MEAS_RATE_ADDR = (byte)0x3;
    public static final byte ALS_MEAS_RATE_ADDR = (byte)0x4;
    public static final byte ALS_GAIN_ADDR = (byte)0x5;
    public static final byte PART_ID_ADDR = (byte)0x6;
    public static final byte PS_DATA_START_ADDR = (byte)0x8;
    public static final byte ALS_DATA_START_ADDR = (byte)0xD;

    public static final int PS_DATA_SIZE = 2;
    public static final int ALS_DATA_SIZE = 2;

    public static final byte PART_NUMBER_ID = (byte)0xB1;
    public static final byte PS_LED_VALUE = (byte)0x36;// 60khz ,100ma
    public static final byte PS_PULSES_VALUE = (byte)0x20;//32 pulse
    public static final byte PS_MEAS_RATE_VALUE = (byte)0x5C;//11 bit 50 ms
    public static final byte ALS_MEAS_RATE_VALUE = (byte)0x40;//100ms
    public static final byte ALS_GAIN_VALUE = (byte)0x0;//3
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

    public boolean enableAls(MadSession session, boolean enable){
        boolean ret = false;
        int len = 1;
        byte pmu = 0x2;
        byte mode = 0;

        byte[] status = session.readI2C(mChannel, mSlaveAddr, MAIN_CTRL_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, len,MadSession.RESULT_TIME_OUT);
        if(status == null || status.length != len) {
            return ret;
        }

        if(enable){
            status[0] |= 0x2;
            mode = 1;
        } else {
            status[0] &= ~0x2;
        }

        int size = session.writeI2C(mChannel, mSlaveAddr, MAIN_CTRL_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, status, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = true;
            mEnableAls = enable;
            session.configI2C(mChannel, mSlaveAddr, ALS_DATA_START_ADDR,
                    MAIN_CTRL_ADDR, pmu, 25, MadSession.I2C_REGISTER_ADDR_MODE_8,
                    (byte)mode, (byte)ALS_DATA_SIZE, MadSession.RESULT_TIME_OUT);
        }

        return ret;
    }

    public int getAlsEnable(MadSession session){
        byte[] state;
        int size = 1;
        int ret = -1;
        state = session.readI2C(mChannel, mSlaveAddr, MAIN_CTRL_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, size, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = (byte)((state[0] >>> 1)&0x1);
        }
        return ret;
    }

    public boolean enablePs(MadSession session, boolean enable){
        boolean ret = false;
        byte pmu = 0x1;
        byte mode = 0;

        byte[] status = session.readI2C(mChannel, mSlaveAddr, MAIN_CTRL_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, 1,MadSession.RESULT_TIME_OUT);
        if(status == null || status.length != 1) {
            return ret;
        }

        if(enable){
            status[0] |= 0x1;
            mode = 1;
        } else {
            status[0] &= ~0x1;
        }

        int size = session.writeI2C(mChannel, mSlaveAddr, MAIN_CTRL_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, status, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = true;
            mEnablePs = enable;
            session.configI2C(mChannel, mSlaveAddr, PS_DATA_START_ADDR,
                    MAIN_CTRL_ADDR, pmu, 2, MadSession.I2C_REGISTER_ADDR_MODE_8,
                    (byte)mode, (byte)PS_DATA_SIZE, MadSession.RESULT_TIME_OUT);
        }

        return ret;
    }

    public int getPsEnable(MadSession session){
        byte[] state;
        int size = 1;
        int ret = -1;
        state = session.readI2C(mChannel, mSlaveAddr, MAIN_CTRL_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, size, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = (byte)(state[0]&0x1);
        }
        return ret;
    }

    public boolean init(MadSession session){
        if(mInited){
            return true;
        }

        //read ic id
        byte[] icID = session.readI2C(mChannel, mSlaveAddr, PART_ID_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, 1,MadSession.RESULT_TIME_OUT);
        if(icID == null){
            return  false;
        } else if(icID.length == 1 && icID[0] == (byte)PART_NUMBER_ID) {
            byte[] ps_pulse={PS_PULSES_VALUE};
            int size = session.writeI2C(mChannel, mSlaveAddr, PS_PULSES_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, ps_pulse, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }

            byte[] ps_led={PS_LED_VALUE};
            size = session.writeI2C(mChannel, mSlaveAddr, PS_LED_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, ps_led, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }

            byte[] ps_mesrate={PS_MEAS_RATE_VALUE};
            size = session.writeI2C(mChannel, mSlaveAddr, PS_MEAS_RATE_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, ps_mesrate, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }
            enablePs(session, false);

            byte[] als_gain={ALS_GAIN_VALUE};
            size = session.writeI2C(mChannel, mSlaveAddr, ALS_GAIN_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, als_gain, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }

            byte[] als_mesrate={ALS_MEAS_RATE_VALUE};
            size = session.writeI2C(mChannel, mSlaveAddr, ALS_MEAS_RATE_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, als_mesrate, MadSession.RESULT_TIME_OUT);
            if(size != 1){
                return  false;
            }
            enableAls(session, false);
            mInited = true;
        }
        return true;
    }

    public boolean deinit(){
        mInited = false;
        return  true;
    }

    public byte[] readPs(MadSession session){
        byte[] status = null;
        int size = PS_DATA_SIZE;
        status = session.readI2CAsync(mChannel, mSlaveAddr, PS_DATA_START_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, size, MadSession.RESULT_TIME_OUT);
        if (status == null || status.length != size) {
            return null;
        }

        return status;
    }

    public byte[] readAls(MadSession session){
        byte[] status = null;

        int size = ALS_DATA_SIZE;
        status = session.readI2CAsync(mChannel, mSlaveAddr, ALS_DATA_START_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, size, MadSession.RESULT_TIME_OUT);
        if (status == null || status.length != size) {
            return null;
        }

        return status;
    }
}
