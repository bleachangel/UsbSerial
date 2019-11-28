package com.felhr.protocal;

public abstract class ProtocalCmd {
    int mCmdValue;
    int mParaLen;
    int mSessionID;
    int mCRC;
    boolean mValid;
    public ProtocalCmd(){
        mCmdValue = 0;
        mParaLen = 0;
        mSessionID = -1;
        mCRC = -1;
        mValid = true;//用于确定是否位有效的命令，通常参数出错该值设置为false;
    }

    public int getCmdValue(){
        return mCmdValue;
    }

    public int getSessionID(){
        return  mSessionID;
    }

    public boolean isCalcCRC(){
        return mCRC == 0 ? false:true;
    }

    public int getCRC(){
        return mCRC;
    }
    public boolean isValid(){
        return  mValid;
    }
}
