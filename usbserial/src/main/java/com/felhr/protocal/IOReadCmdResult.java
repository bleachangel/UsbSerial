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
        if(Array.getLength(para) == mParaLen + 1 && mParaLen > 0) {
            int index = para[mParaLen];
            if(index > 0 && index < mParaLen ){
                para[index] -= 1;
            }
            mCRC = ((para[mParaLen-1]<<8) & 0xFF00)| (para[mParaLen-2] &0xFF);
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 2));
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                //io read ret cmd para: len(1)+session_id(2)+gpio no(2)+dir(1)+level(1)+crc(2)+index(1)
                mNo = (int) para[3];
                mDir = (int) para[5];
                mLevel = (int) para[6];
            }
        } else {
            mValid = false;
        }
    }
}
