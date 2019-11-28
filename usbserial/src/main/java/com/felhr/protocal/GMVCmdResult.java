package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class GMVCmdResult extends ProtocalCmd {
    public int mVol;
    public int mStatus;

    public GMVCmdResult(int cmd, byte[] para){
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
                //ret cmd para: len(1)+session_id(2)+ result(1) + vol(1) + crc(2) + index(1)
                mStatus = (int) para[3];
                mVol = (int) para[4];
            }
        } else {
            mValid = false;
        }
    }
}
