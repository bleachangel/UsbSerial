package com.felhr.sensors;

import java.util.ArrayList;

public class MadSensorManager {
    public static final int MAD_SENSOR_TYPE_ACCELERATOR = 1;
    public static final int MAD_SENSOR_TYPE_GYROSCOPE = 2;
    public static final int MAD_SENSOR_TYPE_MAGNETIC = 3;
    public static final int MAD_SENSOR_TYPE_PROXIMITY = 4;
    public static final int MAD_SENSOR_TYPE_AMBIENT_LIGHT = 5;
    public static ArrayList<MadSensor> mSensorList = new ArrayList<MadSensor>();

    public MadSensorManager(){}
    public static MadSensor CreateSensor(int sensorType){
        MadSensor sensor = null;
        switch (sensorType){
            case MAD_SENSOR_TYPE_ACCELERATOR:
                sensor = new BMI160A();
                break;
            case MAD_SENSOR_TYPE_GYROSCOPE:
                sensor = new BMI160Gy();
                break;
            case MAD_SENSOR_TYPE_MAGNETIC:
                sensor = new Akm09911();
                break;
            case MAD_SENSOR_TYPE_PROXIMITY:
                sensor = new LTR579Ps();
                break;
            case MAD_SENSOR_TYPE_AMBIENT_LIGHT:
                sensor = new LTR579Als();
                break;
            default:
                break;
        }

        mSensorList.add(sensor);
        return  sensor;
    }

    public static boolean init(){
        boolean ret = true;
        int size = mSensorList.size();
        for(int i = 0; i < size; i++){
            MadSensor sensor = mSensorList.get(i);
            sensor.init();
        }
        return ret;
    }

    public static boolean deinit(){
        boolean ret = true;
        int size = mSensorList.size();
        for(int i = 0; i < size; i++){
            MadSensor sensor = mSensorList.get(i);
            sensor.enable(false);
        }
        return ret;
    }

    public static boolean DestorySensor(MadSensor sensor){
        return mSensorList.remove(sensor);
    }
}
