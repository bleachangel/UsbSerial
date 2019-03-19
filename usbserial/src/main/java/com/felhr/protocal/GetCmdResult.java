package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class GetCmdResult extends ProtocalCmd {
    public static final int SUB_GET_CMD_CONNECT_STATUS = 1;
    public static final int SUB_GET_CMD_DEVICE_LIST = 2;
    int mSubCmd;

    public GetCmdResult(int cmd, byte[] para){
        super();
        mCmdValue = cmd;
        mParaLen = (int)para[0];
        if(Array.getLength(para) > mParaLen) {
            mCRC = ((para[mParaLen]<<8) & 0xFF00)| (para[mParaLen-1] &0xFF);
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 1));
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                //setup ret cmd para: len(1)+session_id(2)+status(1)+crc(2)
                mSubCmd = para[3];
            }
        }
    }
}
