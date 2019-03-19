package com.felhr.sensors;

import com.felhr.madsessions.MadSession;

public class BMI160 {
    private static final byte BMI160_USER_CHIP_ID_ADDR=0x00;
    private static final byte BMI160_USER_ACC_CONF_ADDR=0x40;
    private static final byte BMI160_USER_ACC_RANGE_ADDR=0x41;
    private static final byte BMI160_USER_INT_EN_0_ADDR=0x50;
    private static final byte BMI160_CMD_COMMANDS_ADDR=0x7E;
    private static final byte BMI160_USER_ACC_DATA_ADDR=0x12;
    //acc configure
    private static final byte BMI160_ACCEL_ODR_RESERVED=0x00;
    private static final byte BMI160_ACCEL_ODR_0_78HZ=0x01;
    private static final byte BMI160_ACCEL_ODR_1_56HZ=0x02;
    private static final byte BMI160_ACCEL_ODR_3_12HZ=0x03;
    private static final byte BMI160_ACCEL_ODR_6_25HZ=0x04;
    private static final byte BMI160_ACCEL_ODR_12_5HZ=0x05;
    private static final byte BMI160_ACCEL_ODR_25HZ=0x06;
    private static final byte BMI160_ACCEL_ODR_50HZ=0x07;
    private static final byte BMI160_ACCEL_ODR_100HZ=0x08;
    private static final byte BMI160_ACCEL_ODR_200HZ=0x09;
    private static final byte BMI160_ACCEL_ODR_400HZ=0x0A;
    private static final byte BMI160_ACCEL_ODR_800HZ=0x0B;
    private static final byte BMI160_ACCEL_ODR_1600HZ=0x0C;

    //acc range
    private static final byte BMI160_ACCEL_RANGE_2G=0x03;
    private static final byte BMI160_ACCEL_RANGE_4G=0x05;
    private static final byte BMI160_ACCEL_RANGE_8G=0x08;
    private static final byte BMI160_ACCEL_RANGE_16G=0x0c;

    //power mode
    private static final byte CMD_PMU_ACC_SUSPEND=0x10;
    private static final byte CMD_PMU_ACC_NORMAL=0x11;

    private static final byte BMI160_USER_GYR_CONF_ADDR = 0x42;
    private static final byte BMI160_USER_GYR_RANGE_ADDR = 0x43;
    private static final byte BMI160_USER_GYR_DATA_ADDR = 0x0c;

    private static final byte BMI160_GYRO_ODR_RESERVED =0x00;
    private static final byte BMI160_GYRO_ODR_25HZ	= 0x06;
    private static final byte BMI160_GYRO_ODR_50HZ	= 0x07;
    private static final byte BMI160_GYRO_ODR_100HZ	= 0x08;
    private static final byte BMI160_GYRO_ODR_200HZ	= 0x09;
    private static final byte BMI160_GYRO_ODR_400HZ	= 0x0A;
    private static final byte BMI160_GYRO_ODR_800HZ	= 0x0B;
    private static final byte BMI160_GYRO_ODR_1600HZ	= 0x0C;
    private static final byte BMI160_GYRO_ODR_3200HZ	= 0x0D;

    private static final byte BMG_GYRO_RANGE_2000 = 0x0;	/* +/- 2000 degree/s */
    private static final byte BMG_GYRO_RANGE_1000 = 0x01;		/* +/- 1000 degree/s */
    private static final byte BMG_GYRO_RANGE_500 = 0x02;		/* +/- 500 degree/s */
    private static final byte BMG_GYRO_RANGE_250 = 0x03;		/* +/- 250 degree/s */
    private static final byte BMG_GYRO_RANGE_125 = 0x04;		/* +/- 125 degree/s */

    private static final byte CMD_PMU_GYRO_SUSPEND   = 0x14;
    private static final byte CMD_PMU_GYRO_NORMAL    = 0x15;
    private static final byte CMD_PMU_GYRO_FASTSTART = 0x17;

    public byte mChannel;
    public byte mSlaveAddr;
    public boolean mEnableAcc;
    public boolean mEnableGy;
    public boolean mInited;
    private static BMI160 mInstance = null;

    public byte mGyRange;
    public int mGySensitivity;
    public byte mGyODR;

    public byte mAccRange;
    public byte mAccODR;
    public int mAccSensitivity;

    private BMI160(){
        mChannel = 0;
        mSlaveAddr = 0x68;
        mEnableAcc = false;
        mEnableGy = false;
        mInited = false;
        mGyRange = BMG_GYRO_RANGE_2000;
        mGySensitivity = 16;
        mGyODR = BMI160_GYRO_ODR_100HZ;

        mAccRange = BMI160_ACCEL_RANGE_4G;
        mAccSensitivity = 8192;
        mAccODR = BMI160_ACCEL_ODR_200HZ;
    }

    public static BMI160 getInstance(){
        if(mInstance == null){
            mInstance = new BMI160();
        }
        return mInstance;
    }

    public boolean setAccConfig(MadSession sesson, byte rate){
        byte[] conf = new byte[1];
        byte acc_us = 0;
        byte acc_bwp = 0x2;
        int size;
        conf[0] = (byte)((acc_us & 0x80) | (acc_bwp & 0x70)| (rate & 0x0F));

        size = sesson.writeI2C(mChannel, mSlaveAddr, BMI160_USER_ACC_CONF_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, conf, MadSession.RESULT_TIME_OUT);
        return true;
    }

    public boolean setAccRange(MadSession sesson, byte para){
        byte[] range = new byte[1];
        int size;
        range[0] = (byte)((para & 0x0F));

        size = sesson.writeI2C(mChannel, mSlaveAddr, BMI160_USER_ACC_RANGE_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, range, MadSession.RESULT_TIME_OUT);
        return true;
    }

    public boolean setIntEnable(MadSession sesson, boolean enable){
        boolean ret = false;
        byte enableValue[] = new byte[3];
        if(enable){
            enableValue[0] = 0x1;
            enableValue[1] = 0x1;
            enableValue[2] = 0x1;
        } else {
            enableValue[0] = 0x0;
            enableValue[1] = 0x0;
            enableValue[2] = 0x0;
        }
        int size = sesson.writeI2C(mChannel, mSlaveAddr, BMI160_USER_INT_EN_0_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, enableValue, MadSession.RESULT_TIME_OUT);
        if(size == 3){
            ret = true;
        }

        return ret;
    }

    public boolean setAccPower(MadSession sesson, byte mode){
        byte[] state = new byte[1];
        int size;

        state[0] = (byte)mode;
        size = sesson.writeI2C(mChannel, mSlaveAddr, BMI160_CMD_COMMANDS_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, state, MadSession.RESULT_TIME_OUT);
        return true;
    }

    public boolean enableAcc(MadSession sesson, boolean enable){
        byte mode = CMD_PMU_ACC_SUSPEND;
        boolean ret = false;
        mEnableAcc = enable;
        if(mEnableAcc){
            mode = CMD_PMU_ACC_NORMAL;
        }

        if(setAccPower(sesson, mode)){
            ret = true;
        }
        return ret;
    }

    public boolean setGyConfig(MadSession sesson, byte rate){
        byte[] conf = new byte[1];
        byte gy_bwp = 0x2;
        int size;
        conf[0] = (byte)((gy_bwp & 0x30)| (rate & 0x0F));

        size = sesson.writeI2C(mChannel, mSlaveAddr, BMI160_USER_GYR_CONF_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, conf, MadSession.RESULT_TIME_OUT);
        return true;
    }

    public boolean setGyRange(MadSession sesson, byte para){
        byte[] range = new byte[1];
        int size;
        boolean ret = false;
        range[0] = (byte)((para & 0x03));

        size = sesson.writeI2C(mChannel, mSlaveAddr, BMI160_USER_GYR_RANGE_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, range, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            mGyRange = range[0];
            ret = true;
        }
        return ret;
    }

    public boolean setGyPower(MadSession sesson, byte mode){
        byte[] state = new byte[1];
        int size;
        boolean ret = false;

        state[0] = (byte)mode;
        size = sesson.writeI2C(mChannel, mSlaveAddr, BMI160_CMD_COMMANDS_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, state, MadSession.RESULT_TIME_OUT);
        if(size == 1){
            ret = true;
        }
        return ret;
    }

    public boolean enableGy(MadSession sesson, boolean enable){
        byte mode = CMD_PMU_GYRO_SUSPEND;
        boolean ret = false;

        mEnableGy = enable;
        if(mEnableGy){
            mode = CMD_PMU_GYRO_NORMAL;
        }

        if(setGyPower(sesson, mode)){
            ret = true;
        }

        return ret;
    }

    public boolean init(MadSession sesson){
        if(mInited){
            return true;
        }

        //read ic id
        byte[] icID = sesson.readI2C(mChannel, mSlaveAddr, BMI160_USER_CHIP_ID_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, 1,MadSession.RESULT_TIME_OUT);
        if(icID == null){
            mInited = false;
            return  false;
        } else if(icID.length == 1 && (icID[0] == (byte)0xD0 || icID[0] == (byte)0xD1 || icID[0] == (byte)0xD3)) {
            mInited = true;
            setAccConfig(sesson, mAccODR);
            setAccRange(sesson, mAccRange);
            setIntEnable(sesson, false);
            enableAcc(sesson, false);

            setGyConfig(sesson, mGyODR);
            setGyRange(sesson, mGyRange);
            enableGy(sesson, false);
        }
        return true;
    }

    public boolean deinit(){
        mInited = false;
        return  true;
    }

    public byte[] readAcc(MadSession sesson){
        byte[] status = null;
        if(enableAcc(sesson, true)) {
            int size = 6;
            status = sesson.readI2C(mChannel, mSlaveAddr, BMI160_USER_ACC_DATA_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, size, MadSession.RESULT_TIME_OUT);
            if (status == null || status.length != size) {
                return null;
            }
        }

        return status;
    }

    public byte[] readGy(MadSession sesson){
        byte[] status = null;
        if(enableGy(sesson, true)) {
            int size = 6;
            status = sesson.readI2C(mChannel, mSlaveAddr, BMI160_USER_GYR_DATA_ADDR, MadSession.I2C_REGISTER_ADDR_MODE_8, size, MadSession.RESULT_TIME_OUT);
            if (status == null || status.length != size) {
                return null;
            }
        }

        return status;
    }

    public int getGySensitivity(){
        /* bitnum: 16bit */
        switch (mGyRange) {
            case BMG_GYRO_RANGE_2000:
                mGySensitivity = 16;
                break;
            case BMG_GYRO_RANGE_1000:
                mGySensitivity = 33;
                break;
            case BMG_GYRO_RANGE_500:
                mGySensitivity = 66;
                break;
            case BMG_GYRO_RANGE_250:
                mGySensitivity = 131;
                break;
            case BMG_GYRO_RANGE_125:
                mGySensitivity = 262;
                break;
            default:
                mGySensitivity = 16;
                break;
        }
        return  mGySensitivity;
    }

    public int getAccSensitivity(){
        return mAccSensitivity;
    }
}
