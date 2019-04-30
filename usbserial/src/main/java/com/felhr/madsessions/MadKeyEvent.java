package com.felhr.madsessions;

public class MadKeyEvent {
    public static final int MAD_KEY_AUTO = 0;
    public static final int MAD_KEY_DOWN = 1;
    public static final int MAD_KEY_UP = 2;

    public static final int MAD_KEY_0 = 0;
    public static final int MAD_KEY_1 = 1;
    public static final int MAD_KEY_2 = 2;
    public static final int MAD_KEY_3 = 3;
    public static final int MAD_KEY_4 = 4;
    public static final int MAD_MAX_KEY_NUM = 1;

    public int mKeyState;
    public int mKeyValue;
    public long mTimeOut;

    public MadKeyEvent(int keyState, int keyValue){
        mKeyState = keyState;
        mKeyValue = keyValue;
        mTimeOut = 0;
    }
    public MadKeyEvent(){
        mKeyState = MAD_KEY_AUTO;
        mKeyValue = MAD_KEY_0;
        mTimeOut = 0;
    }

}
