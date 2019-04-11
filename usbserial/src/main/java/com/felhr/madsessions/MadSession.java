package com.felhr.madsessions;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;

import com.felhr.protocal.ATRCmdResult;
import com.felhr.protocal.CFLCmdResult;
import com.felhr.protocal.CLCCmdResult;
import com.felhr.protocal.CmdResultFactory;
import com.felhr.protocal.GLBCmdResult;
import com.felhr.protocal.GMVCmdResult;
import com.felhr.protocal.GVLCmdResult;
import com.felhr.protocal.I2CConfigCmdResult;
import com.felhr.protocal.I2CReadCmdResult;
import com.felhr.protocal.I2CWriteCmdResult;
import com.felhr.protocal.IOReadCmdResult;
import com.felhr.protocal.IOWriteCmdResult;
import com.felhr.protocal.OFLCmdResult;
import com.felhr.protocal.OPCCmdResult;
import com.felhr.protocal.ProtocalCmd;
import com.felhr.protocal.ResetCmdResult;
import com.felhr.protocal.SLBCmdResult;
import com.felhr.protocal.SMVCmdResult;
import com.felhr.protocal.SVLCmdResult;
import com.felhr.protocal.SetupCmdResult;
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
    public static final int RESULT_TIME_OUT = 100;
    public static final int ENQUEUE_TIME_OUT = 1;
    public static final int SYNC_WRITE_TIME_OUT = 0;
    public static final int I2R_RESULT_QUEUE_SIZE = 5;
    public static final int AUTO_REPEAT_QUEUE_SIZE = 5;
    public static final int I2W_RESULT_QUEUE_SIZE = 5;
    public static final int DEFAULT_RESULT_QUEUE_SIZE = 5;
    private BlockingQueue<SetupCmdResult> STPQueue = new ArrayBlockingQueue<SetupCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<I2CReadCmdResult> I2RQueue = new ArrayBlockingQueue<I2CReadCmdResult>(I2R_RESULT_QUEUE_SIZE);
    private BlockingQueue<ATRCmdResult> ATRQueue = new ArrayBlockingQueue<ATRCmdResult>(AUTO_REPEAT_QUEUE_SIZE);
    private BlockingQueue<I2CWriteCmdResult> I2WQueue = new ArrayBlockingQueue<I2CWriteCmdResult>(I2W_RESULT_QUEUE_SIZE);
    private BlockingQueue<I2CConfigCmdResult> I2CQueue = new ArrayBlockingQueue<I2CConfigCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<IOWriteCmdResult> IOWQueue = new ArrayBlockingQueue<IOWriteCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<IOReadCmdResult> IORQueue = new ArrayBlockingQueue<IOReadCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<SVLCmdResult> SVLQueue = new ArrayBlockingQueue<SVLCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<GVLCmdResult> GVLQueue = new ArrayBlockingQueue<GVLCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<SLBCmdResult> SLBQueue = new ArrayBlockingQueue<SLBCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<GLBCmdResult> GLBQueue = new ArrayBlockingQueue<GLBCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<OPCCmdResult> OPCQueue = new ArrayBlockingQueue<OPCCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<CLCCmdResult> CLCQueue = new ArrayBlockingQueue<CLCCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<OFLCmdResult> OFLQueue = new ArrayBlockingQueue<OFLCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<CFLCmdResult> CFLQueue = new ArrayBlockingQueue<CFLCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<SMVCmdResult> SMVQueue = new ArrayBlockingQueue<SMVCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<GMVCmdResult> GMVQueue = new ArrayBlockingQueue<GMVCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);
    private BlockingQueue<ResetCmdResult> RSTQueue = new ArrayBlockingQueue<ResetCmdResult>(DEFAULT_RESULT_QUEUE_SIZE);

    public int mProtocalVersion = 0x0001;
    public static final int DEFAULT_RETRY_TIMES = 3;

    public MadSession(){
        mSessionID = MadSessionManager.getInstance().newID();
        MadSessionManager.getInstance().registerSession(mSessionID, this);
    }
    /*
    public boolean connect(UsbDevice device, UsbDeviceConnection connection){
        return MadSessionManager.getInstance().connect(device, connection);
    }
*/
    public boolean appendCmdResult(ProtocalCmd cmd){
        boolean ret = false;
        switch(cmd.getCmdValue()){
            case CmdResultFactory.CMD_SETUP_VALUE:
                try {
                    STPQueue.offer((SetupCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_RESET_VALUE:
                try {
                    RSTQueue.offer((ResetCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_I2C_WRITE_VALUE:
                try {
                    I2WQueue.offer((I2CWriteCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_I2C_READ_VALUE:
                try {
                    ret = I2RQueue.offer((I2CReadCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_AUTO_REPORT_VALUE:
                try {
                    ATRQueue.offer((ATRCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_GPIO_READ_VALUE:
                try {
                    IORQueue.offer((IOReadCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_GPIO_WRITE_VALUE:
                try {
                    IOWQueue.offer((IOWriteCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_I2C_CONFIG_VALUE:
                try {
                    I2CQueue.offer((I2CConfigCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_SET_VOL_VALUE:
                try {
                    SVLQueue.offer((SVLCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_GET_VOL_VALUE:
                try {
                    GVLQueue.offer((GVLCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_SET_LCD_BRIGHT_VALUE:
                try {
                    SLBQueue.offer((SLBCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_GET_LCD_BRIGHT_VALUE:
                try {
                    GLBQueue.offer((GLBCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_OPEN_CAMERA_VALUE:
                try {
                    OPCQueue.offer((OPCCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_CLOSE_CAMERA_VALUE:
                try {
                    CLCQueue.offer((CLCCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_OPEN_FLASH_LIGHT_VALUE:
                try {
                    OFLQueue.offer((OFLCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case CmdResultFactory.CMD_CLOSE_FLASH_LIGHT_VALUE:
                try {
                    CFLQueue.offer((CFLCmdResult)cmd, ENQUEUE_TIME_OUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        return ret;
    }

    public byte[] assembleCmd(String cmdStr, byte[] para){
        int cmdStrLen = cmdStr.length();
        int paraLen = para.length;
        //int endStrLen = CmdResultFactory.CMD_END_TAG.length();
        int size = cmdStrLen + paraLen;// + endStrLen;
        byte[] data = new byte[size];

        System.arraycopy(cmdStr.getBytes(), 0, data, 0, cmdStrLen);
        System.arraycopy(para, 0, data, cmdStrLen, paraLen);
        //System.arraycopy(CmdResultFactory.CMD_END_TAG.getBytes(), 0, data, cmdStrLen + paraLen, endStrLen);
        return data;
    }

    public int findStartTag(byte[] para, int len){
        int index = -1;

        for(int i = 1; i < len; i++){
            if(CmdResultFactory.CMD_START_TAG == para[i]){
                index = i;
                para[i] += 1;//接收时必须减1
                break;
            }
        }
        return index;
    }
    public int setup(long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[8];
        int status = 1;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>> 8) & 0xFF);

        para[3] = (byte)(mProtocalVersion &0xFF);
        para[4] = (byte)((mProtocalVersion &0xFF00)>>8);

        //para len
        para[0] = (byte)7;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 5));

        para[5] = (byte)(crc &0xFF);
        para[6] = (byte)((crc >>>8) & 0xFF);
        para[7] = (byte)(findStartTag(para, para[0]) & 0xFF);

        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_SETUP_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                SetupCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (SetupCmdResult) STPQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int reset(int deviceID, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[9];
        int status = 1;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>> 8) & 0xFF);

        para[3] = (byte)(deviceID &0xFF);
        para[4] = (byte)((deviceID >>> 8) & 0xFF);
        para[5] = (byte)((deviceID >>> 16) & 0xFF);

        //para len
        para[0] = (byte)8;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 6));

        para[6] = (byte)(crc & 0xFF);
        para[7] = (byte)((crc >>> 8) & 0xFF) ;
        para[8] = (byte)(findStartTag(para, para[0]) & 0xFF);

        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_RESET_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                ResetCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (ResetCmdResult)RSTQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            if(result.mNo == deviceID) {
                                find = true;
                                status = result.mStatus;
                            }
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int configI2C(byte channel, byte slaveAddr, int regAddr, int enAddr, int enValue, int delayms, byte regAddrMode, byte mode, byte size, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[18];
        int status = 1;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>> 8) & 0xFF);

        //channel addr
        para[3] = (byte)((channel<<4)&0xF0);
        para[3] = (byte)(regAddrMode&0x0F);

        //slave addr
        para[4] = slaveAddr;

        //register addr
        para[5] = (byte)(regAddr & 0xFF);
        para[7] = (byte)(enAddr & 0xFF);
        para[9] = (byte)(enValue & 0xFF);
        if(I2C_REGISTER_ADDR_MODE_16 == regAddrMode) {
            para[6] = (byte)((regAddr >>>8) & 0xFF);
            para[8] = (byte)((enAddr >>>8) & 0xFF);
            para[10] = (byte)((enValue >>>8)& 0xFF);
        } else {
            para[6] = 0;
            para[8] = 0;
            para[10] = 0;
        }

        //mode
        para[11] = (byte)mode;
        //data len
        para[12] = (byte)size;

        //read data after enable, in ms
        para[13] = (byte)(delayms & 0xFF);
        para[14] = (byte)((delayms >>>8) & 0xFF);

        //para len
        para[0] = (byte)17;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 15));

        para[15] = (byte)(crc &0xFF);
        para[16] = (byte)((crc >>> 8) & 0xFF);

        para[17] = (byte)(findStartTag(para, para[0]) & 0xFF);

        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_I2C_CONFIG_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do{
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                I2CConfigCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while(current < quit){
                    try {
                        result = (I2CConfigCmdResult)I2CQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if(result != null){
                            MadSessionManager.getInstance().statRecvCmd();
                            if (result.mChannel == channel
                                    && result.mSlaveAddr == slaveAddr
                                    && result.mRegAddr == regAddr){
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
            } while(retry > 0 && !find);
        }
        return  status;
    }

    public int writeI2C(byte channel, byte slaveAddr, int regAddr, byte regAddrMode, byte[] data, long timeOut){
        int wirteSize = 0;
        if(data.length > 0){
            //byte[] paraStr = null;
            int dataLen = data.length;
            byte[] para = new byte[dataLen+11];
            //System.arraycopy( CmdResultFactory.CMD_I2C_WRITE_TAG.getBytes(), 0, assemble, 0,4);

            //session id
            para[1] = (byte)(mSessionID & 0xFF);
            para[2] = (byte)((mSessionID >>>8) & 0xFF);

            //channel addr
            para[3] = (byte)((channel<<4)&0xF0);
            para[3] = (byte)(regAddrMode&0x0F);

            //slave addr
            para[4] = slaveAddr;

            //register addr
            para[5] = (byte)(regAddr & 0xFF);
            if(I2C_REGISTER_ADDR_MODE_16 == regAddrMode) {
                para[6] = (byte)((regAddr >>>8) & 0xFF);
            } else {
                para[6] = 0;
            }

            //data len
            para[7] = (byte)(dataLen & 0xFF);
            System.arraycopy(data, 0, para, 8,dataLen);

            //para len
            para[0] = (byte)(10 + dataLen);

            //crc
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 8 + dataLen));

            para[8+dataLen] = (byte)(crc &0xFF);
            para[9+dataLen] = (byte)((crc >>>8) & 0xFF);

            para[10+dataLen] = (byte)(findStartTag(para, para[0]) & 0xFF);

            //paraStr = ByteOps.byteArrayToHexStr(para);
            //String assemble = CmdResultFactory.CMD_I2C_WRITE_TAG+paraStr.toString()+CmdResultFactory.CMD_END_TAG;
            byte[] assemble = assembleCmd(CmdResultFactory.CMD_I2C_WRITE_TAG, para);
            if (MadSessionManager.getInstance().isConnected()) {
                int retry = DEFAULT_RETRY_TIMES;
                boolean find = false;
                do {
                    retry--;
                    MadSessionManager.getInstance().statSendCmd(assemble.length);
                    MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    I2CWriteCmdResult result = null;
                    long current = System.currentTimeMillis();
                    long quit = current + timeOut;
                    while (current < quit) {
                        try {
                            result = (I2CWriteCmdResult) I2WQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                            if (result != null) {
                                MadSessionManager.getInstance().statRecvCmd();
                                if (result.mChannel == channel
                                        && result.mSlaveAddr == slaveAddr
                                        && result.mRegAddr == regAddr) {
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
                } while(retry > 0 && !find);
            }
        }
        return  wirteSize;
    }
    //value : token(4)+len(1)+session_id(2)+channel(1)+slave(1)+reg(2)+len(1)+crc(2)
    public byte[] readI2C(byte channel, byte slaveAddr, int regAddr, byte regAddrMode, int size, long timeOut){
        byte[] data = null;
        if(size > 0){
            //byte[] paraStr = null;
            int dataLen = 11;
            byte[] para = new byte[dataLen];

            //session id
            para[1] = (byte)(mSessionID &0xFF);
            para[2] = (byte)((mSessionID >>>8) & 0xFF);

            //channel addr
            para[3] = (byte)((channel<<4)&0xF0);
            para[3] = (byte)(regAddrMode&0x0F);

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
            para[0] = 10;

            //crc
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 8));

            para[8] = (byte)(crc &0xFF);
            para[9] = (byte)((crc >>>8)&0xFF);
            para[10] = (byte)(findStartTag(para, para[0]) & 0xFF);
            //paraStr = ByteOps.byteArrayToHexStr(para);
            //String assemble = CmdResultFactory.CMD_I2C_READ_TAG+para.toString()+CmdResultFactory.CMD_END_TAG;
            byte[] assemble = assembleCmd(CmdResultFactory.CMD_I2C_READ_TAG, para);
            if (MadSessionManager.getInstance().isConnected()) {
                int retry = DEFAULT_RETRY_TIMES;
                boolean find = false;
                do {
                    retry--;
                    MadSessionManager.getInstance().statSendCmd(assemble.length);
                    MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                    I2CReadCmdResult result = null;
                    long current = System.currentTimeMillis();
                    long quit = current + timeOut;
                    while (current < quit) {
                        try {
                            result = (I2CReadCmdResult) I2RQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                            if (result != null) {
                                MadSessionManager.getInstance().statRecvCmd();

                                if (result.mChannel == channel
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
                } while(retry > 0 && !find);
            }
        }
        return  data;
    }

    public byte[] readI2CAsync(byte channel, byte slaveAddr, int regAddr, byte regAddrMode, int size, long timeOut){
        byte[] data = null;
        if (MadSessionManager.getInstance().isConnected()) {
            boolean find = false;
            ATRCmdResult result = null;
            long current = System.currentTimeMillis();
            long quit = current + timeOut;
            while(current < quit){
                try {
                    result = (ATRCmdResult)ATRQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                    if(result != null){
                        MadSessionManager.getInstance().statRecvCmd();

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
        return  data;
    }

    public int writeIO(int No, byte dir, byte level, long timeOut){
        //byte[] paraStr = null;
        int dataLen = 10;
        byte[] para = new byte[dataLen];
        int status = -1;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        para[3] = (byte)(No&0xFF);
        para[4] = (byte)((No>>>8)&0xFF);

        //register addr
        para[5] = (byte)dir;
        para[6] = (byte)level;

        //para len
        para[0] = (byte)9;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 7));

        para[7] = (byte)(crc &0xFF);
        para[8] = (byte)((crc >>>8)&0xFF);

        para[9] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_GPIO_WRITE_TAG, para);//CmdResultFactory.CMD_GPIO_WRITE_TAG+paraStr.toString()+CmdResultFactory.CMD_END_TAG;
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);

                IOWriteCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (IOWriteCmdResult) IOWQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
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
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int readIO(int No, byte[] dir, byte[] level, long timeOut){
        //byte[] paraStr = null;
        int dataLen = 8;
        byte[] para = new byte[dataLen];
        int status = -1;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        para[3] = (byte)(No&0xFF);
        para[4] = (byte)((No>>>8)&0xFF);

        //para len
        para[0] = (byte)7;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 5));

        para[5] = (byte)(crc &0xFF);
        para[6] = (byte)((crc >>>8)&0xFF);

        para[7] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        //String assemble = CmdResultFactory.CMD_GPIO_READ_TAG+paraStr.toString()+CmdResultFactory.CMD_END_TAG;
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_GPIO_READ_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                IOReadCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (IOReadCmdResult) IORQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
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
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public boolean close(){
        MadSessionManager.getInstance().unregisterSession(mSessionID);
        return MadSessionManager.getInstance().deleteID(mSessionID);
    }

    public int setVol(byte vol, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[7];
        int status = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        para[3] = (byte)vol;

        //para len
        para[0] = (byte)6;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 4));

        para[4] = (byte)(crc &0xFF);
        para[5] = (byte)((crc >>>8)&0xFF);

        para[6] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_SET_VOL_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                SVLCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (SVLCmdResult) SVLQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int getVol(long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[6];
        int vol = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        //para len
        para[0] = (byte)5;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 3));

        para[3] = (byte)(crc &0xFF);
        para[4] = (byte)((crc >>>8)&0xFF);

        para[5] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_GET_VOL_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                GVLCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (GVLCmdResult) GVLQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            vol = result.mVol;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  vol;
    }

    public int setLCDBrightness(byte brightness, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[7];
        int status = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        para[3] = (byte)brightness;

        //para len
        para[0] = (byte)6;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 4));

        para[4] = (byte)(crc &0xFF);
        para[5] = (byte)((crc >>>8)&0xFF);

        para[6] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_SET_LCD_BRIGHT_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                SLBCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (SLBCmdResult) SLBQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int getLCDBrightness(long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[6];
        int brightness = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        //para len
        para[0] = (byte)5;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 3));

        para[3] = (byte)(crc &0xFF);
        para[4] = (byte)((crc >>>8)&0xFF);

        para[5] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_GET_LCD_BRIGHT_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                GLBCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (GLBCmdResult) GLBQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            brightness = result.mBrightness;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  brightness;
    }

    public int openCamera(byte no, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[7];
        int status = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        para[3] = (byte)no;

        //para len
        para[0] = (byte)6;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 4));

        para[4] = (byte)(crc &0xFF);
        para[5] = (byte)((crc >>>8)&0xFF);

        para[6] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_OPEN_CAMERA_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                OPCCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (OPCCmdResult) OPCQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int closeCamera(byte no, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[7];
        int status = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);
        para[3] = (byte)no;

        //para len
        para[0] = (byte)6;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 4));

        para[4] = (byte)(crc &0xFF);
        para[5] = (byte)((crc >>>8)&0xFF);

        para[6] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_CLOSE_CAMERA_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                CLCCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (CLCCmdResult) CLCQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int openFlashLight(byte no, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[7];
        int status = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        para[3] = (byte)no;

        //para len
        para[0] = (byte)6;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 4));

        para[4] = (byte)(crc &0xFF);
        para[5] = (byte)((crc >>>8) &0xFF);

        para[6] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_OPEN_FLASH_LIGHT_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                OFLCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (OFLCmdResult) OFLQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int closeFlashLight(byte no, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[7];
        int status = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);
        para[3] = (byte)no;

        //para len
        para[0] = (byte)6;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 4));

        para[4] = (byte)(crc &0xFF);
        para[5] = (byte)((crc>>>8) & 0xFF);

        para[6] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_CLOSE_FLASH_LIGHT_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                CFLCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (CFLCmdResult) CFLQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int setMicVol(byte vol, long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[7];
        int status = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        para[3] = (byte)vol;

        //para len
        para[0] = (byte)6;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 4));

        para[4] = (byte)(crc &0xFF);
        para[5] = (byte)((crc>>>8)&0xFF);

        para[6] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_SET_MIC_VOL_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                SMVCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (SMVCmdResult) SMVQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            status = result.mStatus;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  status;
    }

    public int getMicVol(long timeOut){
        //byte[] paraStr = null;
        byte[] para = new byte[6];
        int vol = 0;

        //session id
        para[1] = (byte)(mSessionID &0xFF);
        para[2] = (byte)((mSessionID >>>8)&0xFF);

        //para len
        para[0] = (byte)5;

        //crc
        int crc = CRC16.calc(Arrays.copyOfRange(para, 1, 3));

        para[3] = (byte)(crc &0xFF);
        para[4] = (byte)((crc >>>8) & 0xFF);

        para[5] = (byte)(findStartTag(para, para[0]) & 0xFF);
        //paraStr = ByteOps.byteArrayToHexStr(para);
        byte[] assemble = assembleCmd(CmdResultFactory.CMD_GET_MIC_VOL_TAG, para);
        if (MadSessionManager.getInstance().isConnected()) {
            int retry = DEFAULT_RETRY_TIMES;
            boolean find = false;
            do {
                retry--;
                MadSessionManager.getInstance().statSendCmd(assemble.length);
                MadSessionManager.getInstance().getSerialDevice().syncWrite(assemble, SYNC_WRITE_TIME_OUT);
                GMVCmdResult result = null;
                long current = System.currentTimeMillis();
                long quit = current + timeOut;
                while (current < quit) {
                    try {
                        result = (GMVCmdResult) GMVQueue.poll(timeOut, TimeUnit.MILLISECONDS);
                        if (result != null) {
                            MadSessionManager.getInstance().statRecvCmd();
                            find = true;
                            vol = result.mVol;
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    current = System.currentTimeMillis();
                }
            } while (retry > 0 && !find);
        }
        return  vol;
    }
}
