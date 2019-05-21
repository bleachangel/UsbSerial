package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class I2CConfigCmdResult extends ProtocalCmd {
    public int mChannel;
    public int mAddrBit; //0:8, 1:16
    public int mSlaveAddr;
    public int mRegAddr;
    public int mStatus;

    public I2CConfigCmdResult(int cmd, byte[] para){
        super();
        mCmdValue = cmd;
        mParaLen = (int)para[0];

        if(Array.getLength(para) == mParaLen + 1 && mParaLen > 0) {
            int index = para[mParaLen];
            if(index > 0 && index < mParaLen ){
                para[index] -= 1;
            }
            mCRC = ((para[mParaLen-1]<<8) & 0xFF00)| (para[mParaLen-2] &0xFF);
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 2));
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                //ret cmd para: len(1)+session_id(2)+channel&addr bit(1)+slaveaddr(1)+regaddr(2)+status(1)+crc(2)
                mChannel = (int) ((para[3] & 0xF0) >>> 4);
                mAddrBit = (int) (para[3] & 0xF);

                mSlaveAddr = para[4];
                mRegAddr = (int) ((para[5] & 0xFF) | ((para[6] << 8) & 0xFF00));
                mStatus = (int) para[7];
            }
        } else {
            mValid = false;
        }
    }
}
