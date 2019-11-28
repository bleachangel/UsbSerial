package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ResetCmdResult extends ProtocalCmd {
    public int mDeviceID;//000000 for all,
    public int mStatus;

    public ResetCmdResult(int cmd, byte[] para){
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
                //ret cmd para: len(1)+session_id(2)+device id(3)+result(1)+crc(2)+index(1)
                mDeviceID = (int) ((para[5] << 16) | (para[4] << 8) | para[3]);
                mStatus = para[6];
            }
        } else {
            mValid = false;
        }
    }
}
