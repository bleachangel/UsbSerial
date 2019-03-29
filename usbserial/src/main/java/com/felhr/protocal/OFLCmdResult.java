package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class OFLCmdResult extends ProtocalCmd {
    public int mStatus;

    public OFLCmdResult(int cmd, byte[] para){
        super();
        mCmdValue = cmd;
        mParaLen = (int)para[0];
        if(Array.getLength(para) > mParaLen && mParaLen > 0) {
            mCRC = ((para[mParaLen]<<8) & 0xFF00)| (para[mParaLen-1] &0xFF);
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 1));
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                mStatus = para[3];
            }
        }
    }
}
