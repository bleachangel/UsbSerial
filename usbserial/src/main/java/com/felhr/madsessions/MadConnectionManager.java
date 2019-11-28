package com.felhr.madsessions;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MadConnectionManager {
    public static String TAG = "MadConnectionManager";
    private static MadConnectionManager mInstance = new MadConnectionManager();
    public static final int MIN_CONNECTION_ID = 0;
    public static final int MAX_CONNECTION_ID = 0xFFFF;
    private HashMap<Integer, MadDeviceConnection> mConnections;
    private int mConnectionGenerator;

    private int getID(){
        while (mConnections.containsKey(mConnectionGenerator)) {
            mConnectionGenerator++;
            if(mConnectionGenerator > MAX_CONNECTION_ID){
                mConnectionGenerator = MIN_CONNECTION_ID;
            }
        }
        return mConnectionGenerator;
    }

    private MadConnectionManager(){
        mConnectionGenerator = MIN_CONNECTION_ID;
        mConnections = new HashMap<Integer, MadDeviceConnection>();
    }

    public static MadConnectionManager getInstance() {
        return mInstance;
    }

    //根据usb设备对象确定是否已经建立了connection对象。
    public MadDeviceConnection findConnection(UsbDevice device){
        MadDeviceConnection connection = null;
        synchronized (mConnections) {
            Iterator<HashMap.Entry<Integer, MadDeviceConnection>> iterator = mConnections.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<Integer, MadDeviceConnection> entry = iterator.next();
                MadDeviceConnection conn = entry.getValue();
                if (conn != null && device.equals(conn.getUsbDevice())) {
                    //已经有建立连接
                    connection = conn;
                    break;
                }
            }
        }
        return connection;
    }

    /*设置connection提供的能力。
     * 之所以由sessionmanager来设置，主要考虑到session是与外围设备通信指令解析的唯一途径，
     * 只有通过session，各种指令才会被下发，然后执行结果才会被接收到。
     * 获取设备能力指令GCP只在设备建立连接后，才会被调用，而且指挥在该函数中被调用。
     * **/
    public boolean setConnectionCapacity(MadDeviceConnection connection){
        boolean ret = false;
        MadSession controlSession = MadSessionManager.getInstance().getControlSession();

        //没有设置过capacity的情况下，才需要再次
        if(connection != null && connection.getCapacity() == 0) {
            //绑定控制session
            connection.bind(controlSession.mSessionID, controlSession.getCallback());
            controlSession.setConnection(connection);
            connection.start();
            int retry = 5;
            do {
                //获取设备提供的能力
                long capacity = controlSession.getCapacity(MadSession.RESULT_TIME_OUT);
                if (capacity != 0) {
                    //保存到connection实例
                    connection.setCapacity(capacity);
                    ret = true;
                    break;
                }
                retry--;
            }while(retry > 0);

            byte[] deviceName;
            deviceName = controlSession.getDeviceName(MadSession.RESULT_TIME_OUT);
            if(deviceName != null){
                connection.setDeviceName(new String(deviceName));
            }

            connection.stop();
            //解除控制session的绑定
            connection.unbind(controlSession.mSessionID);
            controlSession.setConnection(null);
        }

        return ret;
    }

    public MadDeviceConnection connectUartDevice(UsbDevice device, UsbDeviceConnection connection, MadUartParameters params){
        MadDeviceConnection mad_connection = null;
        mad_connection = findConnection(device);
        if(mad_connection != null){
            return mad_connection;
        }

        int connectionID = getID();
        mad_connection = new MadDeviceConnection(connectionID, device, connection);
        if(mad_connection.open(MadDeviceConnection.CONNECT_TYPE_UART, params) == 0) {
            synchronized (mConnections) {
                mConnections.put(connectionID, mad_connection);
            }

            //一旦有连接，读取连接设备的能力，为后续的自动绑定服务
            setConnectionCapacity(mad_connection);

            //搜索是否有可用的session,然后自动将session与connection绑定，如果session已经绑定了connection，则不做修改。
            ArrayList<MadSession> sessionList = MadSessionManager.getInstance().getSessionList();
            if(sessionList != null){
                for(int i = 0; i < sessionList.size(); i++){
                    MadSession session = sessionList.get(i);
                    //功能匹配才绑定
                    if(isMatch(session, mad_connection)) {
                        bind(session.mSessionID, mad_connection);
                    }
                }
            }
        } else {
            mad_connection = null;
        }

        return  mad_connection;
    }

    public boolean isMatch(MadSession session, MadDeviceConnection connection){
        boolean ret = false;
        long sessionCapacity = session.getSessionCapacity();
        long connectionCapacity = connection.getCapacity();

        //设备类型一致
        if(session.getDeviceType() == connection.getDeviceType()){
            //过滤掉设备类型
            sessionCapacity = sessionCapacity & ~(MadDeviceConnection.CAPACITY_DEVICE_MASK | MadDeviceConnection.CAPACITY_DONGLE_MASK | MadDeviceConnection.CAPACITY_DEVICE_INFO_MASK);
            connectionCapacity = connectionCapacity & ~(MadDeviceConnection.CAPACITY_DEVICE_MASK | MadDeviceConnection.CAPACITY_DONGLE_MASK | MadDeviceConnection.CAPACITY_DEVICE_INFO_MASK);
            //提供的能力一致
            if(((sessionCapacity & connectionCapacity) != 0) || (sessionCapacity == connectionCapacity)){
                ret = true;
            }
        }

        return ret;
    }

    //找到可以与sessionID绑定的connection
    public ArrayList<MadDeviceConnection> filter(int sessionID){
        MadSession session = MadSessionManager.getInstance().getSession(sessionID);
        if(session == null){
            Log.d(TAG, "sessionID:"+sessionID+" not register yet!");
            return null;
        }

        ArrayList<MadDeviceConnection> filtered_list = new ArrayList<MadDeviceConnection>();
        ArrayList<MadDeviceConnection> connection_list = getConnectionList();
        for (int i = 0; i < connection_list.size(); i++) {
            MadDeviceConnection conn = connection_list.get(i);
            if(isMatch(session, conn)) {
                filtered_list.add(conn);
            }
        }
        return filtered_list.size() > 0 ? filtered_list : null;
    }

    /*遍历所有的设备连接，确定sessionID是否与设备已经绑定。如果已绑定，则sessionID不能再次绑定。*/
    public int bind(int sessionID, MadDeviceConnection connection){
        int ret = -1;

        //如果session已经绑定当前连接，则不需要再做绑定
        if(connection != null && connection.isBind(sessionID)){
            ret = 0;
        } else {
            boolean bBindFlag = false;
            //没有绑定当前连接，则查找其他已经的连接，确定没有在其他连接上绑定。
            synchronized (mConnections) {
                Iterator<HashMap.Entry<Integer, MadDeviceConnection>> iterator = mConnections.entrySet().iterator();
                while (iterator.hasNext()) {
                    HashMap.Entry<Integer, MadDeviceConnection> entry = iterator.next();
                    int connectionID = entry.getKey();
                    MadDeviceConnection conn = entry.getValue();

                    //排除当前连接
                    if(connectionID != connection.getConnectionID()){
                        if(conn.isBind(sessionID)){
                            //已经绑定，则不能与当前的connection进行连接
                            bBindFlag = true;
                            break;
                        }
                    }
                }
            }

            //没有绑定过，则与当前的connection进行绑定
            if(!bBindFlag){
                MadSession session = MadSessionManager.getInstance().getSession(sessionID);
                ret = connection.bind(sessionID, session.getCallback());
                if(ret == 0) {
                    session.setConnection(connection);
                }
            }
        }
        return ret;
    }

    public int unbind(int sessionID){
        int ret = -1;
        MadSession session = MadSessionManager.getInstance().getSession(sessionID);
        if(session != null){
            MadDeviceConnection conn = session.getConnection();
            if(conn != null){
                ret = conn.unbind(sessionID);
                session.setConnection(null);
            }
        }
        return ret;
    }

    public int disconnect(MadDeviceConnection conn){
        int ret = 0;
        if(conn != null){
            conn.close();
            synchronized (mConnections){
                mConnections.remove(conn.getConnectionID());
            }
            //将session绑定的connection设置为可用的连接。
            ArrayList<Integer> sessionList = conn.getBindedSessionList();
            if(sessionList != null){
                for(int i = 0; i < sessionList.size(); i++){
                    int sessionID = sessionList.get(i);
                    MadSession session = MadSessionManager.getInstance().getSession(sessionID);
                    if(session != null){
                        session.setConnection(null);
                    }
                }
            }

            //移除connection绑定的所有session
            conn.unbindAll();
        }
        return ret;
    }

    public int disconnectDevice(UsbDevice device){
        MadDeviceConnection conn = findConnection(device);
        return disconnect(conn);
    }

    public int disconnectAll(){
        int ret = -1;
        MadDeviceConnection connection = null;
        synchronized (mConnections) {
            Iterator<HashMap.Entry<Integer, MadDeviceConnection>> iterator = mConnections.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry<Integer, MadDeviceConnection> entry = iterator.next();
                MadDeviceConnection conn = entry.getValue();
                disconnect(conn);
            }
        }
        return ret;
    }

    public ArrayList<MadDeviceConnection> getConnectionList(){
        ArrayList<MadDeviceConnection> connectionList = null;
        synchronized (mConnections) {
            connectionList = new ArrayList<MadDeviceConnection>(mConnections.values());
        }
        return connectionList;
    }

    public static long getDeviceCapacity(){
        long deviceCapacity = MadDeviceConnection.CAPACITY_DEVICE_MASK;

        return  deviceCapacity;
    }

    public static long getDongleCapacity(){
        long dongleCapacity = MadDeviceConnection.CAPACITY_DONGLE_MASK;

        return  dongleCapacity;
    }

    public static long getSensorCapacity(){
        long sensorCapacity = (MadDeviceConnection.CAPACITY_ACCELERATOR_MASK
                                | MadDeviceConnection.CAPACITY_GYROSCOPE_MASK
                                | MadDeviceConnection.CAPACITY_MAGNETIC_MASK
                                | MadDeviceConnection.CAPACITY_AMBIENT_LIGHT_MASK
                                | MadDeviceConnection.CAPACITY_PROXIMITY_MASK);

        return  sensorCapacity;
    }

    public static long getFlashlightCapacity(){
        long flashlightCapacity = (MadDeviceConnection.CAPACITY_NORMAL_FLASH_LIGHT_MASK
                                | MadDeviceConnection.CAPACITY_INFRARED_FLASH_LIGHT_MASK);

        return  flashlightCapacity;
    }

    public static long getCameraCapacity(){
        long cameraCapacity = (MadDeviceConnection.CAPACITY_NORMAL_CAMERA_MASK
                                | MadDeviceConnection.CAPACITY_INFRARED_CAMERA_MASK
                                | MadDeviceConnection.CAPACITY_TOF_CAMERA_MASK);
        return cameraCapacity;
    }

    public static long getLCDCapacity(){
        long cameraCapacity = (MadDeviceConnection.CAPACITY_LCD_ENABLE_MASK
                                | MadDeviceConnection.CAPACITY_LCD_BRIGHTNESS_MASK);
        return cameraCapacity;
    }

    public static long getDisplayCapacity(){
        long displayCapacity = MadDeviceConnection.CAPACITY_DISPLAY_ENABLE_MASK;
        return displayCapacity;
    }

    public static long getSourcesInCapacity(){
        //该功能仅在dongle设备上使用
        long sourceCapacity = MadDeviceConnection.CAPACITY_SOURCES_IN_MASK;
        return sourceCapacity;
    }

    public static long getAudioCapacity(){
        long audioCapacity = (MadDeviceConnection.CAPACITY_AUDIO_VOLUME_MASK
                            | MadDeviceConnection.CAPACITY_AUDIO_CHANNEL_MASK);
        return audioCapacity;
    }

    public static long getMicrophoneCapacity(){
        long microphoneCapacity = MadDeviceConnection.CAPACITY_MIC_VOLUME_MASK;
        return microphoneCapacity;
    }

    public static long getKeyCapacity(){
        //该功能仅在dongle设备上使用
        long keyCapacity = MadDeviceConnection.CAPACITY_KEY_MASK;
        return keyCapacity;
    }

    public static long getInfoCapacity(){
        //该功能仅在dongle设备上使用
        long infoCapacity = MadDeviceConnection.CAPACITY_DEVICE_INFO_MASK;
        return infoCapacity;
    }

    public long getRecvCount(){
        long ret = 0;
        //遍历所有的连接通道，获取接收数据长度
        ArrayList<MadDeviceConnection> connectionList = getConnectionList();
        if(connectionList != null){
            for(int i  = 0; i < connectionList.size(); i++) {
                MadDeviceConnection conn = connectionList.get(i);
                ret += conn.getRecvCount();
            }
        }
        return ret;
    }

    public long getErrCount(){
        long ret = 0;
        //遍历所有的连接通道，获取接收出错数据长度
        ArrayList<MadDeviceConnection> connectionList = getConnectionList();
        if(connectionList != null){
            for(int i  = 0; i < connectionList.size(); i++) {
                MadDeviceConnection conn = connectionList.get(i);
                ret += conn.getErrCount();
            }
        }
        return ret;
    }

    public boolean startAll(){
        boolean ret = true;
        ArrayList<MadDeviceConnection> connectionList = getConnectionList();
        if(connectionList != null){
            for(int i  = 0; i < connectionList.size(); i++) {
                MadDeviceConnection conn = connectionList.get(i);
                conn.start();
            }
        }
        return ret;
    }

    public boolean stopAll(){
        boolean ret = true;
        ArrayList<MadDeviceConnection> connectionList = getConnectionList();
        if(connectionList != null){
            for(int i  = 0; i < connectionList.size(); i++) {
                MadDeviceConnection conn = connectionList.get(i);
                conn.stop();
            }
        }
        return ret;
    }

    public boolean start(UsbDevice usbDevice){
        boolean ret = false;
        MadDeviceConnection conn = findConnection(usbDevice);
        if(conn != null){
            conn.start();
            ret = true;
        }
        return ret;
    }
    public boolean stop(UsbDevice usbDevice){
        boolean ret = false;
        MadDeviceConnection conn = findConnection(usbDevice);
        if(conn != null){
            conn.stop();
            ret = true;
        }
        return ret;
    }
}
