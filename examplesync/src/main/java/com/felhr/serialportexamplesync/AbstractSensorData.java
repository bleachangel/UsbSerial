package com.felhr.serialportexamplesync;

public abstract class AbstractSensorData {
    public int mX;
    public int mY;
    public int mZ;
    public AbstractSensorData(int x, int y, int z){
        mX = x;
        mY = y;
        mZ = z;
    }
}
