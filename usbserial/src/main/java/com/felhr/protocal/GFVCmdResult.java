package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class GFVCmdResult extends ProtocalCmd {
    public int mLen;
    public byte[] mFirmwareVersion;

    //value : len(1)+session_id(2)+ version size(1) +firmware version(<=32)+crc(2)+index(1)
    public GFVCmdResult(int cmd, byte[] para){
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
                mLen = para[3];
                if(mLen > 0) {
                    mFirmwareVersion = new byte[mLen];
                    System.arraycopy(para, 4, mFirmwareVersion, 0, mLen);
                }
            }
        } else {
            mValid = false;
        }
    }
}
