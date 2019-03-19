package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class I2CWriteCmdResult extends ProtocalCmd {
    public int mChannel;
    public int mAddrBit; //0:8, 1:16
    public int mSlaveAddr;
    public int mRegAddr;
    public int mStatus;

    public I2CWriteCmdResult(int cmd, byte[] para){
        super();
        mCmdValue = cmd;
        mParaLen = (int)para[0];

        if(Array.getLength(para) > mParaLen) {
            mCRC = ((para[mParaLen]<<8) & 0xFF00)| (para[mParaLen-1] &0xFF);
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 1));
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                //I2C write ret cmd para: len(1)+session_id(2)+channel&addr bit(1)+slaveaddr(1)+regaddr(2)+status(1)+crc(2)
                mChannel = (int) ((para[3] & 0xF0) >>> 4);
                mAddrBit = (int) (para[3] & 0xF);

                mSlaveAddr = para[4];
                mRegAddr = (int) ((para[5] & 0xFF) | ((para[6] << 8) & 0xFF00));
                mStatus = (int) para[7];
            }
        }
    }
}
