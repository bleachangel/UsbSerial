package com.felhr.madsessions;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

import com.felhr.protocal.CmdResultFactory;
import com.felhr.protocal.I2CReadCmdResult;
import com.felhr.protocal.I2CWriteCmdResult;
import com.felhr.protocal.IOReadCmdResult;
import com.felhr.protocal.IOWriteCmdResult;
import com.felhr.protocal.ProtocalCmd;
import com.felhr.utils.ByteOps;
import com.felhr.utils.CRC16;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class MadSession {
    public int mSessionID;
    public static final byte I2C_REGISTER_ADDR_MODE_8 = 1;
    public static final byte I2C_REGISTER_ADDR_MODE_16 = 2;
    public static final int RESULT_TIME_OUT = 50;
    public static final int I2R_RESULT_QUEUE_SIZE = 50;
    public static final int I2W_RESULT_QUEUE_SIZE = 10;
    public static final int DEFAULT_RESULT_QUEUE_SIZE = 5;
    private BlockingQueue<I2CReadCmdResult> I2RQueue = new ArrayBlockingQueue<I2CReadCmdResult>(I2R_RESULT_QUEUE_SIZE);
    private BlockingQueue<I2CWriteCmdResult> I2WQueue = new ArrayBlockingQueue<I2CWriteCmdResult>(I2W_RESULT_QUEUE_SIZE);
    private BlockingQueue<IOWriteCmdResult> IOWQueue = new ArrayBlockingQueue<IOWriteCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<IOReadCmdResult> IORQueue = new ArrayBlockingQueue<IOReadCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private long mSendCmdCount;
    private long mSendByteCount;

    private long mRecvCmdCount;
    private long mRecvByteCount;

    public MadSession(){
        mSessionID = MadSessionManager.getInstance().newID();
        MadSessionManager.getInstance().registerSession(mSessionID, this);
        mSendCmdCount = 0;
        mSendByteCount = 0;
        mRecvCmdCount = 0;
        mRecvByteCount = 0;
    }
    /*
    public boolean connect(UsbDevice device, UsbDeviceConnection connection){
        return MadSessionManager.getInstance().connect(device, connection);
    }
*/
    public boolean appendCmdResult(ProtocalCmd cmd){
        boolean ret = false;
        switch(cmd.getCmdValue()){
            case CmdResultFactory.CMD_I2C_WRITE_VALUE:
                I2WQueue.add((I2CWriteCmdResult)cmd);
                break;
            case CmdResultFactory.CMD_I2C_READ_VALUE:
                I2RQueue.add((I2CReadCmdResult)cmd);
                break;
            case CmdResultFactory.CMD_GPIO_READ_VALUE:
                IORQueue.add((IOReadCmdResult)cmd);
                break;
            case CmdResultFactory.CMD_GPIO_WRITE_VALUE:
                IOWQueue.add((IOWriteCmdResult)cmd);
                break;
            default:
                break;
        }
        return ret;
    }

    public byte[] assembleCmd(String cmdStr, byte[] para){
        int cmdStrLen = cmdStr.length();
        int paraLen = para.length;
        int endStrLen = CmdResultFactory.CMD_END_TAG.length();
        int size = cmdStrLen + paraLen + endStrLen;
        byte[] data = new byte[size];

        System.arraycopy(cmdStr.getBytes(), 0, data, 0, cmdStrLen);
        System.arraycopy(para, 0, data, cmdStrLen, paraLen);
        System.arraycopy(CmdResultFactory.CMD_END_TAG.getBytes(), 0, data, cmdStrLen + paraLen, endStrLen);
        return data;
    }

    public void statSendCmd(int size){
        synchronized (this) {
            if (mSendCmdCount + 1 > Long.MAX_VALUE) {
                mSendCmdCount = 0;
            }
            mSendCmdCount++;

            if (mSendByteCount + size > Long.MAX_VALUE) {
                mSendByteCount = 0;
            }

            mSendByteCount += size;
        }
    }

    public void statRecvCmd(){
        synchronized (this) {
            if (mRecvCmdCount + 1 > Long.MAX_VALUE) {
                mRecvCmdCount = 0;
            }
            mRecvCmdCount++;
        }
/*
        if (mRecvByteCount + size > Long.MAX_VALUE) {
            mRecvByteCount = 0;
        }

        mSendByteCount += size;*/
    }

    public long getSendCmdCount(){
        return  mSendCmdCount;
    }

    public long getSendByteCount(){
        return mSendByteCount;
    }

    public long getRecvCmdCount(){
        return mRecvCmdCount;
    }

    public int writeI2C(byte channel, byte slaveAddr, int regAddr, byte regAddrMode, byte[] data, long timeOut){
        int wirteSize = 0;
        if(data.length > 0){
            byte[] paraStr = null;
            int dataLen = data.length;
            byte[] para = new byte[dataLen+10];
            //System.arraycopy( CmdResultFactory.CMD_I2C_WRITE_TAG.getBytes(), 0, assemble, 0,4);

            //session id
            para[1] = (byte)(mSessionID &0xFF);
            para[2] = (byte)((mSessionID &0xFF00)>>8);

            //channel addr
            para[3] = (byte)((channel&0x0F)<<4);

            //slave addr
            para[4] = slaveAddr;

            //register addr
            para[5] = (byte)(regAddr & 0xFF);
            if(I2C_REGISTER_ADDR_MODE_16 == regAddrMode) {
                para[6] = (byte)((regAddr & 0xFF00)>>8);
            } else {
                para[6] = 0;
            }

            //data len
            para[7] = (byte)(dataLen & 0xFF);
            System.arraycopy(data, 0, para, 8,dataLen);

            //para len
            para[0] = (byte)(7 + dataLen + 2);

            //crc
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 8 + dataLen));

            para[8+dataLen] = (byte)(crc &0xFF);
            para[9+dataLen] = (byte)((crc &0xFF00) >>>8);

            paraStr = ByteOps.byteArrayToHexStr(para);
            //String assemble = CmdResultFactory.CMD_I2C_WRITE_TAG+paraStr.toString()+CmdResultFactory.CMD_END_TAG;
            byte[] assemble = assembleCmd(CmdResultFactory.CMD_I2C_WRITE_TAG, paraStr);
            if (MadSessionManager.getInstance().isConnected()) {
                statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().write(assemble);
                boolean find = false;
                I2CWriteCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while(current < quit){
                    try {
                        result = (I2CWriteCmdResult)I2WQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if(result != null){
                            statRecvCmd();
                            if (result.mChannel == channel
                            && result.mSlaveAddr == slaveAddr
                            && result.mRegAddr == regAddr){
                                find = true;
                                wirteSize = dataLen;
                                break;
                                }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            }
        }
        return  wirteSize;
    }
    //value : token(4)+len(1)+session_id(2)+channel(1)+slave(1)+reg(2)+len(1)+crc(2)
    public byte[] readI2C(byte channel, byte slaveAddr, int regAddr, byte regAddrMode, int size, long timeOut){
        byte[] data = null;
        if(size > 0){
            byte[] paraStr = null;
            int dataLen = 10;
            byte[] para = new byte[dataLen];

            //session id
            para[1] = (byte)(mSessionID &0xFF);
            para[2] = (byte)((mSessionID &0xFF00)>>8);

            //channel addr
            para[3] = (byte)((channel&0x0F)<<4);

            //slave addr
            para[4] = slaveAddr;

            //register addr
            para[5] = (byte)(regAddr & 0xFF);
            if(I2C_REGISTER_ADDR_MODE_16 == regAddrMode) {
                para[6] = (byte)((regAddr & 0xFF00)>>8);
            } else {
                para[6] = 0;
            }

            //data len
            para[7] = (byte)(size & 0xFF);

            //para len
            para[0] = 9;

            //crc
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 8));

            para[8] = (byte)(crc &0xFF);
            para[9] = (byte)((crc &0xFF00) >>>8);

            paraStr = ByteOps.byteArrayToHexStr(para);
            //String assemble = CmdResultFactory.CMD_I2C_READ_TAG+para.toString()+CmdResultFactory.CMD_END_TAG;
            byte[] assemble = assembleCmd(CmdResultFactory.CMD_I2C_READ_TAG, paraStr);
            if (MadSessionManager.getInstance().isConnected()) {
                statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().write(assemble);
                boolean find = false;
                I2CReadCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut*10;
                while(current < quit){
                    try {
                        result = (I2CReadCmdResult)I2RQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if(result != null){
                            statRecvCmd();

                            if(result.mChannel == channel
                            && result.mSlaveAddr == slaveAddr
                            && result.mRegAddr == regAddr) {
                                find = true;
                                data = result.mReadData;
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            }
        }
        return  data;
    }

    public int writeIO(int No, byte dir, byte level, long timeOut){
        byte[] paraStr = null;
        int dataLen = 9;
        byte[] para = new byte[dataLen];
        int status = -1;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID &0xFF00)>>8);

        para[3] = (byte)(No&0xFF);
        para[4] = (byte)((No&0xFF00)>>>8);

        //register addr
        para[5] = (byte)dir;
        para[6] = (byte)level;

        //para len
        para[0] = (byte)8;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 7));

        para[7] = (byte)(crc &0xFF);
        para[8] = (byte)((crc &0xFF00) >>>8);

        paraStr = ByteOps.byteArrayToHexStr(para);
        String assemble = CmdResultFactory.CMD_GPIO_WRITE_TAG+paraStr.toString()+CmdResultFactory.CMD_END_TAG;
        if (MadSessionManager.getInstance().isConnected()) {
            MadSessionManager.getInstance().getSerialDevice().write(assemble.getBytes());
            boolean find = false;
            IOWriteCmdResult result = null;
            long current = System.currentTimeMillis();
            long quit = current + timeOut;
            while(current < quit){
                try {
                    result = (IOWriteCmdResult)IOWQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                    if(result != null) {
                        statRecvCmd();
                        if (result.mNo == No) {
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                current = System.currentTimeMillis();
            }
        }
        return  status;
    }

    public int readIO(int No, byte[] dir, byte[] level, long timeOut){
        byte[] paraStr = null;
        int dataLen = 7;
        byte[] para = new byte[dataLen];
        int status = -1;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID &0xFF00)>>8);

        para[3] = (byte)(No&0xFF);
        para[4] = (byte)((No&0xFF00)>>>8);

        //para len
        para[0] = (byte)6;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 5));

        para[5] = (byte)(crc &0xFF);
        para[6] = (byte)((crc &0xFF00) >>>8);

        paraStr = ByteOps.byteArrayToHexStr(para);
        String assemble = CmdResultFactory.CMD_GPIO_READ_TAG+paraStr.toString()+CmdResultFactory.CMD_END_TAG;
        if (MadSessionManager.getInstance().isConnected()) {
            MadSessionManager.getInstance().getSerialDevice().write(assemble.getBytes());
            boolean find = false;
            IOReadCmdResult result = null;
            long current = System.currentTimeMillis();
            long quit = current + timeOut;
            while(current < quit){
                try {
                    result = (IOReadCmdResult)IORQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                    if(result != null) {
                        statRecvCmd();
                        if (result.mNo == No) {
                            find = true;
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                current = System.currentTimeMillis();
            }
        }
        return  status;
    }

    public boolean close(){
        MadSessionManager.getInstance().unregisterSession(mSessionID);
        return MadSessionManager.getInstance().deleteID(mSessionID);
    }
}
