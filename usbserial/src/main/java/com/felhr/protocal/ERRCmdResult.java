package com.felhr.protocal;

import java.lang.reflect.Array;

public class ERRCmdResult extends ProtocalCmd {
    int mErrorNo;
    //value : len(1)+error no(1) + data(1)
    public ERRCmdResult(int cmd, byte[] para) {
        super();
        mCmdValue = cmd;
        mParaLen = (int) para[0];
        if (Array.getLength(para) == mParaLen + 1 && mParaLen > 0) {
            mErrorNo = para[1];
        } else {
            mValid = false;
        }
    }
}
