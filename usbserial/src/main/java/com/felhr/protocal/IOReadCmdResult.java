package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class IOReadCmdResult extends ProtocalCmd {
    public int mNo;
    public int mDir; //0:out, 1:in
    public int mLevel;//1:high, 0:low

    public IOReadCmdResult(int cmd, byte[] para){
        super();
        mCmdValue = cmd;
        mParaLen = (int)para[0];
        if(Array.getLength(para) > mParaLen && mParaLen > 0) {
            mCRC = ((para[mParaLen]<<8) & 0xFF00)| (para[mParaLen-1] &0xFF);
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 1));
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                //io read ret cmd para: len(1)+session_id(2)+io(1)+dir(1)+level(1)+crc(2)
                mNo = (int) para[3];
                mDir = (int) para[4];
                mLevel = (int) para[5];
            }
        }
    }
}
