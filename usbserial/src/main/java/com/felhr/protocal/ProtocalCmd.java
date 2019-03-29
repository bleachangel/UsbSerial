package com.felhr.protocal;

public abstract class ProtocalCmd {
    int mCmdValue;
    int mParaLen;
    int mSessionID;
    int mCRC;
    public ProtocalCmd(){
        mCmdValue = 0;
        mParaLen = 0;
        mSessionID = -1;
        mCRC = -1;
    }

    public int getCmdValue(){
        return mCmdValue;
    }

    public int getSessionID(){
        return  mSessionID;
    }

    public int getCRC(){
        return mCRC;
    }
}