package com.felhr.sensors;

public class MadSensorEvent {
    public final float[] values;
    public MadSensor sensor;
    public int accuracy;
    public long timestamp;

    MadSensorEvent(int valueSize) {
        values = new float[valueSize];
    }
}
