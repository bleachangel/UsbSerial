package com.felhr.madsessions;

public class MadPlatformDevice {
    public static final int DEFAULT_TIME_OUT = 100;
    public MadSession mSession = null;
    public MadPlatformDevice(){
        mSession = new MadSession();
    }

    public int setup(){
        return mSession.setup(DEFAULT_TIME_OUT);
    }

    public int reset(int deviceID){
        return mSession.reset(deviceID, DEFAULT_TIME_OUT);
    }

    public int setVol(byte vol){
        return mSession.setVol(vol, DEFAULT_TIME_OUT);
    }

    public int getVol(){
        return mSession.getVol(DEFAULT_TIME_OUT);
    }

    public int setLcdBrightness(byte brightness){
        return mSession.setLCDBrightness(brightness, DEFAULT_TIME_OUT);
    }

    public int getLcdBrightness(){
        return mSession.getLCDBrightness(DEFAULT_TIME_OUT);
    }

    public int openCamera(byte no){
        return mSession.openCamera(no, DEFAULT_TIME_OUT);
    }

    public int closeCamera(byte no){
        return mSession.closeCamera(no, DEFAULT_TIME_OUT);
    }

    public int openFlashLight(byte no){
        return mSession.openFlashLight(no, DEFAULT_TIME_OUT);
    }

    public int closeFlashLight(byte no){
        return mSession.closeFlashLight(no, DEFAULT_TIME_OUT);
    }

    public int setMicVol(byte vol){
        return mSession.setMicVol(vol, DEFAULT_TIME_OUT);
    }

    public int getMicVol(){
        return mSession.getMicVol(DEFAULT_TIME_OUT);
    }
}
