package com.felhr.madsessions;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class MadSessionManager {
    public static String TAG = "MadSessionManager";
    private static final MadSessionManager ourInstance = new MadSessionManager();
    private HashMap<Integer,MadSession> mSessions;
    private MadSession mControlSession;
    public static final int CONTROL_SESSION_ID = 0;
    public static final int MIN_SESSION_ID = 1;
    public static final int MAX_SESSION_ID = 0xFFFE;
    public static final int TEST_SESSION_ID = 0xFFFF;
    private int mSessionGenerator = MIN_SESSION_ID;
    private long mSendCmdCount;
    private long mSendByteCount;
    private long mRecvCmdCount;
    private long mRecvByteCount;

    public static MadSessionManager getInstance() {
        return ourInstance;
    }

    private MadSessionManager() {
        mSendCmdCount = 0;
        mSendByteCount = 0;
        mRecvCmdCount = 0;
        mRecvByteCount = 0;

        mSessions = new HashMap<Integer, MadSession>();
        mControlSession = new MadSession(CONTROL_SESSION_ID, MadDeviceConnection.CAPACITY_ALL_MASK);
    }

    public MadSession getControlSession(){
        return mControlSession;
    }

    public ArrayList<MadSession> getSessionList(){
        ArrayList<MadSession> sessionList = null;
        synchronized (mSessions) {
            sessionList = new ArrayList<MadSession>(mSessions.values());
        }
        return sessionList;
    }

    public int bind(MadDeviceConnection connection){
        int ret = 0;
        ArrayList<MadSession> sessionList = getSessionList();
        for(int i = 0; i < sessionList.size(); i++){
            MadSession session = sessionList.get(i);
            MadConnectionManager.getInstance().bind(session.mSessionID, connection);
        }

        return ret;
    }

    public MadSession createSession(long capacity){
        int sessionID = getID();
        MadSession session = new MadSession(sessionID, capacity);
        if(!registerSession(sessionID, session)){
            Log.e(TAG, "Register session failure!");
            session = null;
        } else {
            //遍历，搜索可用的connection
            ArrayList<MadDeviceConnection> connection_list = MadConnectionManager.getInstance().filter(sessionID);

            //如果不存在可用的连接，则有可能需要等待设备连接建立后，再与session进行绑定。
            if(connection_list != null){
                //默认使用第一个connenction
                MadDeviceConnection conn = connection_list.get(0);
                MadConnectionManager.getInstance().bind(sessionID, conn);
            }
        }
        return session;
    }

    public int releaseSession(int sessionID){
        int ret = -1;
        MadSession session = getSession(sessionID);
        if(session != null){
            //如果已经绑定了connection，则需要先解除绑定
            MadDeviceConnection conn = session.getConnection();
            if(conn != null){
                conn.unbind(sessionID);
            }
            session.setConnection(null);
            unregisterSession(sessionID);
        }
        return ret;
    }

    public int releaseAllSession(){
        int ret = -1;
        ArrayList<MadSession> sessionList = getSessionList();
        for(int i = 0; i < sessionList.size(); i++){
            MadSession session = sessionList.get(i);
            releaseSession(session.mSessionID);
        }
        return ret;
    }

    public MadSession getSession(int sessionID){
        MadSession session = null;
        synchronized (mSessions){
            session = mSessions.get(sessionID);
        }
        return session;
    }

    //返回可用的connection list
    private boolean registerSession(int sessionID, MadSession session){
        boolean ret = false;
        if(!mSessions.containsKey(sessionID) && session != null && !mSessions.containsValue(session)){
            mSessions.put(sessionID, session);
            ret = true;
        }
        return ret;
    }

    private boolean unregisterSession(int sessionID){
        synchronized (mSessions){
            if(mSessions.containsKey(sessionID)){
                mSessions.remove(sessionID);
            }
        }
        return true;
    }

    private int getID(){
        while (mSessions.containsKey(mSessionGenerator)) {
            mSessionGenerator++;
            if(mSessionGenerator > MAX_SESSION_ID){
                mSessionGenerator = MIN_SESSION_ID;
            }
        }
        return mSessionGenerator;
    }

    public void statSendCmd(int size){
        synchronized (this) {
            if (mSendCmdCount + 1 > Long.MAX_VALUE) {
                mSendCmdCount = 0;
            }
            mSendCmdCount++;

            if (mSendByteCount + size > Long.MAX_VALUE) {
                mSendByteCount = 0;
            }

            mSendByteCount += size;
        }
    }

    public void statRecvCmd(){
        synchronized (this) {
            if (mRecvCmdCount + 1 > Long.MAX_VALUE) {
                mRecvCmdCount = 0;
            }
            mRecvCmdCount++;
        }
/*
        if (mRecvByteCount + size > Long.MAX_VALUE) {
            mRecvByteCount = 0;
        }

        mSendByteCount += size;*/
    }

    public long getSendCmdCount(){
        return  mSendCmdCount;
    }

    public long getSendByteCount(){
        return mSendByteCount;
    }

    public long getRecvCmdCount(){
        return mRecvCmdCount;
    }

    public long getRecvByteCount(){
        return MadConnectionManager.getInstance().getRecvCount();
    }

    public long getRecvErrCount(){
        return MadConnectionManager.getInstance().getErrCount();
    }
}
