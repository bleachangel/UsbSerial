package com.felhr.protocal;

import com.felhr.utils.CRC16;

import java.lang.reflect.Array;
import java.util.Arrays;

public class ATRCmdResult extends ProtocalCmd {
    public int mChannel;
    public int mAddrBit; //0:8, 1:16
    public int mSlaveAddr;
    public int mRegAddr;
    public int mDataSize;
    public byte[] mReadData=null;

    //result str : token(4)+len(2)+session_id(4)+channel(2)+slave(2)+reg(4)+datalen(2)+data(<=55*2)+crc(4)+end(4)
    public ATRCmdResult(int cmd, byte[] para){
        super();
        mCmdValue = cmd;
        mParaLen = (int)para[0];
        mDataSize = 0;
        if(Array.getLength(para) == mParaLen + 1 && mParaLen > 0) {
            int index = para[mParaLen];
            if(index > 0 && index < mParaLen ){
                para[index] -= 1;
            }
            mCRC = ((para[mParaLen-1]<<8) & 0xFF00)| (para[mParaLen-2] &0xFF);
            int crc = CRC16.calc(Arrays.copyOfRange(para, 1, mParaLen - 2));
            if(mCRC == crc) {
                mSessionID = (int) (((para[2] << 8) & 0xFF00) | (para[1] & 0xFF));
                //I2C read ret cmd para: len(1)+session_id(2)+channel&addr bit(1)+slaveaddr(1)+regaddr(2)+datasize(1)+data(<=6)+crc(2)
                mChannel = (int) ((para[3] & 0xF0) >>> 4);
                mAddrBit = (int) (para[3] & 0xF);

                mSlaveAddr = para[4];
                mRegAddr = (int) ((para[5] & 0xFF) | ((para[6] << 8)&0xFF00));
                mDataSize = (int) para[7];
                if(mDataSize > 0) {
                    mReadData = new byte[mDataSize];
                    System.arraycopy(para, 8, mReadData, 0, mDataSize);
                }
            }
        } else {
            mValid = false;
        }
    }
}
