package com.felhr.sensors;

import com.felhr.madsessions.MadConnectionManager;
import com.felhr.madsessions.MadKeyEvent;
import com.felhr.madsessions.MadKeyEventListener;
import com.felhr.madsessions.MadPlatformDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MadSensorManager {
    public static final int MAD_SENSOR_TYPE_ACCELERATOR = 1;
    public static final int MAD_SENSOR_TYPE_GYROSCOPE = 2;
    public static final int MAD_SENSOR_TYPE_MAGNETIC = 3;
    public static final int MAD_SENSOR_TYPE_PROXIMITY = 4;
    public static final int MAD_SENSOR_TYPE_AMBIENT_LIGHT = 5;
    public static ArrayList<MadSensor> mSensorList = new ArrayList<MadSensor>();

    public MadSensorManager(){
        new SensorThread().start();
    }

    public MadSensor CreateSensor(int sensorType){
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

        synchronized (mSensorList) {
            mSensorList.add(sensor);
        }
        return  sensor;
    }

    public boolean init(){
        boolean ret = true;
        synchronized (mSensorList) {
            int size = mSensorList.size();
            for (int i = 0; i < size; i++) {
                MadSensor sensor = mSensorList.get(i);
                sensor.init();
            }
        }
        return ret;
    }

    public boolean deinit(){
        boolean ret = true;
        synchronized (mSensorList) {
            int size = mSensorList.size();
            for (int i = 0; i < size; i++) {
                MadSensor sensor = mSensorList.get(i);
                sensor.enable(false);
            }
        }
        return ret;
    }

    public boolean destorySensor(MadSensor sensor){
        boolean ret = false;
        if(sensor != null) {
            synchronized (mSensorList) {
                ret = mSensorList.remove(sensor);
                sensor.release();
            }
        }
        return ret;
    }

    public boolean destoryAllSensor(){
        boolean ret = true;
        synchronized (mSensorList){
            for(int i = 0; i < mSensorList.size();i++){
                MadSensor sensor = mSensorList.get(i);
                if(sensor != null){
                    sensor.release();
                }
            }
            mSensorList.clear();
        }
        return ret;
    }

    protected class SensorThread extends Thread {
        private volatile boolean keep = true;
        private MadPlatformDevice mDeviceCtrl;
        private MadPlatformDevice mDongleCtrl;
        public SensorThread() {
            super("SensorThread");

            long capacity = MadConnectionManager.getDeviceCapacity();
            capacity |= MadConnectionManager.getKeyCapacity();

            mDeviceCtrl = new MadPlatformDevice(capacity);

            long dongleCapacity = MadConnectionManager.getDongleCapacity();
            dongleCapacity |= MadConnectionManager.getKeyCapacity();
            mDongleCtrl = new MadPlatformDevice(dongleCapacity);
        }

        void stopThread() {
            keep = false;
        }

        @Override
        public void run() {
            long starttime = 0;
            long endtime = 0;
            long cost = 0;
            while (keep) {
                //starttime = System.currentTimeMillis();
                synchronized (MadSensorManager.mSensorList) {
                    int sensorSize = MadSensorManager.mSensorList.size();

                    for (int i = 0; i < sensorSize; i++) {
                        MadSensor sensor = MadSensorManager.mSensorList.get(i);
                        if (sensor.isEnabled()) {
                            if (!sensor.isInited()) {
                                sensor.init();
                            }

                            dispatchSensorEvent(sensor, sensor.read());
                        }
                    }
                }

                dispatchKeyEvent(mDeviceCtrl.readKey());
                dispatchKeyEvent(mDongleCtrl.readKey());
            }
        }
    }

    private HashMap<MadSensor, MadSensorEventListener> mSensorListeners = new HashMap<MadSensor, MadSensorEventListener>();
    public boolean registerListener(MadSensorEventListener listener, MadSensor sensor, int delayUs){
        synchronized (mSensorListeners) {
            sensor.mTimeOut = delayUs;
            mSensorListeners.put(sensor, listener);
        }
        return true;
    }

    public boolean unregisterListener(MadSensorEventListener listener){
        synchronized (mSensorListeners) {
            Iterator<HashMap.Entry<MadSensor, MadSensorEventListener>> keys = mSensorListeners.entrySet().iterator();
            while (keys.hasNext()) {
                HashMap.Entry<MadSensor, MadSensorEventListener> key = keys.next();
                if (key.getValue().equals(listener)) {
                    keys.remove();
                }
            }
        }
        return true;
    }

    public boolean unregisterAllListener(){
        synchronized (mSensorListeners) {
            mSensorListeners.clear();
        }
        return true;
    }

    public void dispatchSensorEvent(MadSensor sensor, MadSensorEvent event){
        if (mSensorListeners == null || sensor == null || event == null) {
            return;
        }

        synchronized (mSensorListeners) {
            MadSensorEventListener listener = mSensorListeners.get(sensor);
            listener.onMadSensorChanged(event);
        }
    }

    private HashMap<MadKeyEventListener, Integer> mKeyEventListeners = new HashMap<MadKeyEventListener, Integer>();
    public boolean registerKeyListener(MadKeyEventListener listener, int key){
        synchronized (mKeyEventListeners) {
            mKeyEventListeners.put(listener, key);
        }
        return true;
    }

    public boolean unregisterKeyListener(MadKeyEventListener listener){
        synchronized (mKeyEventListeners) {
            Iterator<HashMap.Entry<MadKeyEventListener, Integer>> keys = mKeyEventListeners.entrySet().iterator();
            while (keys.hasNext()) {
                HashMap.Entry<MadKeyEventListener, Integer> key = keys.next();
                if (key.getKey().equals(listener)) {
                    keys.remove();
                }
            }
        }
        return true;
    }

    public void dispatchKeyEvent(MadKeyEvent event){
        if (mKeyEventListeners == null || event == null) {
            return;
        }
        synchronized (mKeyEventListeners) {
            Iterator<HashMap.Entry<MadKeyEventListener, Integer>> keys = mKeyEventListeners.entrySet().iterator();
            while (keys.hasNext()) {
                HashMap.Entry<MadKeyEventListener, Integer> key = keys.next();
                if (key.getValue() == event.mKeyValue) {
                    MadKeyEventListener listener = key.getKey();
                    if(event.mKeyState == event.MAD_KEY_UP){
                        listener.onKeyUp(event.mKeyValue);
                    }else if(event.mKeyState == event.MAD_KEY_DOWN){
                        listener.onKeyDown(event.mKeyValue);
                    }
                }
            }
        }
    }

    public void enableAllSensor(boolean flag){
        synchronized (mSensorList) {
            for (int i = 0; i < mSensorList.size(); i++) {
                MadSensor sensor = mSensorList.get(i);
                sensor.enable(flag);
            }
        }
    }
}
