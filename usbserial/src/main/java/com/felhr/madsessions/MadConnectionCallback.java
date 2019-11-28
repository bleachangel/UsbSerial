package com.felhr.madsessions;

import com.felhr.protocal.ProtocalCmd;

public interface MadConnectionCallback {
    void onReceivedData(ProtocalCmd cmd);
    void onReceivedDataForTest(byte[] data);
}
