package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class GCPCmdResult extends ProtocalCmd {
    public long mCapacity;

    //len(1)+session_id(2)+ capacity(4)+ crc(2)+index(1)
    public GCPCmdResult(int cmd, byte[] para){
        super();
        mCmdValue = cmd;
        mParaLen = (int)para[0];
        if(Array.getLength(para) == mParaLen + 1 && mParaLen > 0) {
            int index = para[mParaLen];
            if(index > 0 && index < mParaLen ){
                para[index] -= 1;
            }
            mCRC = ((para[mParaLen-1]<<8) & 0xFF00)| (para[mParaLen-2] &0xFF);
            int crc = 0;
            if(isCalcCRC()) {
                crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 2));
            }
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                mCapacity = para[6] | ((para[5] << 8) & 0xFF00) | ((para[4] << 16) & 0xFF0000) | ((para[3] << 24) & 0xFF000000);
            }
        } else {
            mValid = false;
        }
    }
}
