package com.felhr.protocal;
import java.lang.reflect.Array;
public class KPLCmdResult extends ProtocalCmd {
    //value : len(1)+data(2)
    public KPLCmdResult(int cmd, byte[] para) {
        super();
        mCmdValue = cmd;
        mParaLen = (int) para[0];
        if (!(Array.getLength(para) == mParaLen + 1 && mParaLen > 0)) {
            mValid = false;
        }
    }
}
