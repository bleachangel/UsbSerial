package com.felhr.sensors;

public interface MadSensorEventListener {
    public void onMadSensorChanged(MadSensorEvent event);
    public void onMadAccuracyChanged(MadSensor sensor, int accuracy);
}
