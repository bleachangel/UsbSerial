package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ResetCmdResult extends ProtocalCmd {
    int mNo;//000000 for all,
    int mStatus;

    public ResetCmdResult(int cmd, byte[] para){
        super();
        mCmdValue = cmd;
        mParaLen = (int)para[0];
        if(Array.getLength(para) > mParaLen) {
            mCRC = ((para[mParaLen]<<8) & 0xFF00)| (para[mParaLen-1] &0xFF);
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 1));
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                //reset ret cmd para: len(1)+session_id(2)+bus(1)+device(1)+no(1)+status(1)+crc(2)
                mNo = (int) ((para[3] << 16) | (para[4] << 8) | para[5]);
                mStatus = para[6];
            }
        }
    }
}
