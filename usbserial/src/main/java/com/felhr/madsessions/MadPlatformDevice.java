package com.felhr.madsessions;

public class MadPlatformDevice {
    public static final int DEFAULT_TIME_OUT = 100;
    public static final int READ_KEY_TIME_OUT = 5;
    public MadSession mSession = null;
    private static MadPlatformDevice mInstance = null;
    private MadPlatformDevice(){
        mSession = new MadSession();
    }

    public static MadPlatformDevice getInstance(){
        if(mInstance == null){
            mInstance = new MadPlatformDevice();
        }

        return mInstance;
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
    public int enterBootloader(){
        return mSession.enterBootloader(DEFAULT_TIME_OUT);
    }

    public byte[] getHardwareVersion(){
        return mSession.getHardwareVersion(DEFAULT_TIME_OUT);
    }

    public byte[] getFirmwareVersion(){
        return mSession.getFirmwareVersion(DEFAULT_TIME_OUT);
    }

    public int setSN(byte[] sn){
        return mSession.setSN(sn, DEFAULT_TIME_OUT);
    }

    public byte[] getSN(){
        return mSession.getSN(DEFAULT_TIME_OUT);
    }

    public int setDeviceName(byte[] deviceName){
        return mSession.setDeviceName(deviceName, DEFAULT_TIME_OUT);
    }

    public byte[] getDeviceName(){
        return mSession.getDeviceName(DEFAULT_TIME_OUT);
    }

    public int setVendor(byte[] vendor){
        return mSession.setVendor(vendor, DEFAULT_TIME_OUT);
    }

    public byte[] getVendor(){
        return mSession.getVendor(DEFAULT_TIME_OUT);
    }

    public int setKeyFunction(int key, int function){
        return mSession.setKeyFunction(key, function, DEFAULT_TIME_OUT);
    }

    public int getKeyFunction(int key){
        return mSession.getKeyFunction(key, DEFAULT_TIME_OUT);
    }

    public MadKeyEvent readKey(){
        return mSession.readKey(READ_KEY_TIME_OUT);
    }

    public int openLCD(){
        return mSession.openLCD(DEFAULT_TIME_OUT);
    }

    public int closeLCD(){
        return mSession.closeLCD(DEFAULT_TIME_OUT);
    }
}
