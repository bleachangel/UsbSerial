package com.felhr.madsessions;

public class MadPlatformDevice {
    public MadSession mSession = new MadSession();
    public MadPlatformDevice(){
    }
    public int setVol(byte vol){
        long timeOut = 50;
        return mSession.setVol(vol, timeOut);
    }

    public int getVol(){
        long timeOut = 50;
        return mSession.getVol(timeOut);
    }

    public int setLcdBrightness(byte brightness){
        long timeOut = 50;
        return mSession.setLCDBrightness(brightness, timeOut);
    }

    public int getLcdBrightness(){
        long timeOut = 50;
        return mSession.getLCDBrightness(timeOut);
    }

    public int openCamera(byte no){
        long timeOut = 50;
        return mSession.openCamera(no, timeOut);
    }

    public int closeCamera(byte no){
        long timeOut = 50;
        return mSession.closeCamera(no, timeOut);
    }

    public int openFlashLight(byte no){
        long timeOut = 50;
        return mSession.openFlashLight(no, timeOut);
    }

    public int closeFlashLight(byte no){
        long timeOut = 50;
        return mSession.closeFlashLight(no, timeOut);
    }
}
