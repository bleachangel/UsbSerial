package com.felhr.protocal;

import java.nio.charset.Charset;
import java.util.Arrays;

public class SerialRingBuffer{
    private int mReadPosStart;
    private int mReadSize;
    private int mWritePosStart;
    private int mWriteSize;
    private int mReadPosStartBak;
    private int mReadSizeBak;
    private int mWritePosStartBak;
    private int mWriteSizeBak;
    private int mCapacity;
    private long lRecvCount = 0;
    private long lAnalyzedCount = 0;
    private long lAnalyzedCountBak = 0;
    private long lErrCount = 0;
    private long lErrCountBak = 0;
    static final int MIN_BUFFER_SIZE = 256;
    private byte[] mRingBuffer;
    public static int MESSAGE_CMD_TAG_LEN = 4;

    public SerialRingBuffer(int capacity){
        if(capacity < MIN_BUFFER_SIZE){
            capacity = MIN_BUFFER_SIZE;
        }

        mCapacity = capacity;
        mRingBuffer = new byte[mCapacity];
        mReadPosStart = 0;
        mReadSize = 0;

        mWritePosStart = 0;
        mWriteSize = mCapacity;
    }

    private boolean saveStatus(){
        mReadPosStartBak = mReadPosStart;
        mReadSizeBak = mReadSize;

        mWritePosStartBak = mWritePosStart;
        mWriteSizeBak = mWriteSize;

        lErrCountBak = lErrCount;
        lAnalyzedCountBak = lAnalyzedCount;
        return  true;
    }

    private boolean restoreStatus(){
        if(lErrCount == lErrCountBak) {
            mReadPosStart = mReadPosStartBak;
            mReadSize = mReadSizeBak;

            mWritePosStart = mWritePosStartBak;
            mWriteSize = mWriteSizeBak;

            lAnalyzedCount = lAnalyzedCountBak;
        }

        return  true;
    }

    public int getCmdStartPos(){
        int pos = -1;
        if(mReadSize < CmdResultFactory.CMD_TAG_LEN){
            return pos;
        }

        //mReadPosStart = mReadPosStart % mCapacity;

        //首先读取出一部分数据，确定标签
        if(CmdResultFactory.CMD_START_TAG != mRingBuffer[mReadPosStart]){
            for(int i = 1; i < mReadSize-1; i++) {
                int curPos = (mReadPosStart + i) % mCapacity;
                if (CmdResultFactory.CMD_START_TAG == mRingBuffer[curPos]) {
                    mReadSize -= i;
                    mWriteSize += i;
                    mReadPosStart = (mReadPosStart + i) % mCapacity;

                    //统计错误率
                    addErrCount(i);
                    break;
                }
            }
        }

        if(mReadSize > CmdResultFactory.CMD_TAG_LEN) {
            int find = 0;
            for(int i = 0; i < mReadSize; i++) {
                int curPos = (mReadPosStart + i) % mCapacity;
                int next1 = (curPos + 1) % mCapacity;
                int next2 = (curPos + 2) % mCapacity;
                int next3 = (curPos + 3) % mCapacity;
                if (mRingBuffer[curPos] == CmdResultFactory.CMD_START_TAG
                        && ((mRingBuffer[next1] == 'S'&& mRingBuffer[next2] == 'T'&& mRingBuffer[next3] == 'P')
                        ||(mRingBuffer[next1] == 'T'&& mRingBuffer[next2] == 'S'&& mRingBuffer[next3] == 'T')
                        ||(mRingBuffer[next1] == 'R'&& mRingBuffer[next2] == 'S'&& mRingBuffer[next3] == 'T')
                        ||(mRingBuffer[next1] == 'I'&& mRingBuffer[next2] == '2'&& mRingBuffer[next3] == 'R')
                        ||(mRingBuffer[next1] == 'I'&& mRingBuffer[next2] == '2'&& mRingBuffer[next3] == 'W')
                        ||(mRingBuffer[next1] == 'I'&& mRingBuffer[next2] == 'O'&& mRingBuffer[next3] == 'R')
                        ||(mRingBuffer[next1] == 'I'&& mRingBuffer[next2] == 'O'&& mRingBuffer[next3] == 'W')
                        ||(mRingBuffer[next1] == 'S'&& mRingBuffer[next2] == 'V'&& mRingBuffer[next3] == 'L')
                        ||(mRingBuffer[next1] == 'G'&& mRingBuffer[next2] == 'V'&& mRingBuffer[next3] == 'L')
                        ||(mRingBuffer[next1] == 'S'&& mRingBuffer[next2] == 'L'&& mRingBuffer[next3] == 'B')
                        ||(mRingBuffer[next1] == 'G'&& mRingBuffer[next2] == 'L'&& mRingBuffer[next3] == 'B')
                        ||(mRingBuffer[next1] == 'O'&& mRingBuffer[next2] == 'P'&& mRingBuffer[next3] == 'C')
                        ||(mRingBuffer[next1] == 'C'&& mRingBuffer[next2] == 'L'&& mRingBuffer[next3] == 'C')
                        ||(mRingBuffer[next1] == 'O'&& mRingBuffer[next2] == 'F'&& mRingBuffer[next3] == 'L')
                        ||(mRingBuffer[next1] == 'C'&& mRingBuffer[next2] == 'F'&& mRingBuffer[next3] == 'L')
                        ||(mRingBuffer[next1] == 'I'&& mRingBuffer[next2] == '2'&& mRingBuffer[next3] == 'C')
                        ||(mRingBuffer[next1] == 'A'&& mRingBuffer[next2] == 'T'&& mRingBuffer[next3] == 'R')
                        ||(mRingBuffer[next1] == 'U'&& mRingBuffer[next2] == 'P'&& mRingBuffer[next3] == 'F')
                        ||(mRingBuffer[next1] == 'G'&& mRingBuffer[next2] == 'H'&& mRingBuffer[next3] == 'V')
                        ||(mRingBuffer[next1] == 'G'&& mRingBuffer[next2] == 'F'&& mRingBuffer[next3] == 'V')
                        ||(mRingBuffer[next1] == 'S'&& mRingBuffer[next2] == 'S'&& mRingBuffer[next3] == 'N')
                        ||(mRingBuffer[next1] == 'G'&& mRingBuffer[next2] == 'S'&& mRingBuffer[next3] == 'N')
                        ||(mRingBuffer[next1] == 'S'&& mRingBuffer[next2] == 'D'&& mRingBuffer[next3] == 'N')
                        ||(mRingBuffer[next1] == 'G'&& mRingBuffer[next2] == 'D'&& mRingBuffer[next3] == 'N')
                        ||(mRingBuffer[next1] == 'S'&& mRingBuffer[next2] == 'V'&& mRingBuffer[next3] == 'D')
                        ||(mRingBuffer[next1] == 'G'&& mRingBuffer[next2] == 'V'&& mRingBuffer[next3] == 'D')
                        ||(mRingBuffer[next1] == 'S'&& mRingBuffer[next2] == 'K'&& mRingBuffer[next3] == 'F')
                        ||(mRingBuffer[next1] == 'G'&& mRingBuffer[next2] == 'K'&& mRingBuffer[next3] == 'F')
                        ||(mRingBuffer[next1] == 'R'&& mRingBuffer[next2] == 'K'&& mRingBuffer[next3] == 'V'))) {
                    mReadSize -= i;
                    mWriteSize += i;
                    mReadPosStart = (mReadPosStart + i) % mCapacity;
                    pos = mReadPosStart;

                    //统计错误率
                    addErrCount(i);
                    break;
                }
            }

            if(pos == -1){
                mReadPosStart = (mReadPosStart + mReadSize) % mCapacity;
                mWriteSize += mReadSize;

                //统计错误率
                addErrCount(mReadSize);

                mReadSize = 0;
            }
        }
        return pos;
    }
    public byte[] getCmdTag(){
        byte[] cmd = null;

        if(mReadSize > CmdResultFactory.CMD_TAG_LEN){
            cmd = new byte[CmdResultFactory.CMD_TAG_LEN];
            int readPosEnd = mReadPosStart + CmdResultFactory.CMD_TAG_LEN - 1;

            if (readPosEnd <= mCapacity - 1) {
                System.arraycopy(mRingBuffer, mReadPosStart, cmd, 0, CmdResultFactory.CMD_TAG_LEN);
            } else {
                int len1 = mCapacity - mReadPosStart;
                System.arraycopy(mRingBuffer, mReadPosStart, cmd, 0, len1);
                System.arraycopy(mRingBuffer, 0, cmd, len1, CmdResultFactory.CMD_TAG_LEN - len1);
            }

            mReadSize -= CmdResultFactory.CMD_TAG_LEN;
            mWriteSize += CmdResultFactory.CMD_TAG_LEN;
            mReadPosStart = (mReadPosStart + CmdResultFactory.CMD_TAG_LEN) % mCapacity;

            //统计已分析正确的字符数量
            addAnalyzedCount(CmdResultFactory.CMD_TAG_LEN);
        }

        return cmd;
    }
    public byte[] getCmdPara(){
        byte[] data = null;
        //origin str : token(4)+len(1)+session_id(2)+crc(2)+end(4)
        if(mReadSize > 5) {
            int i;
            int curPos = mReadPosStart % mCapacity;
            int paraLen = mRingBuffer[curPos] + 1;
            boolean find = false;
            if(paraLen > mReadSize){
                return null;
            }

            for (i = 0; i < mReadSize; i++) {
                curPos = (mReadPosStart + i) % mCapacity;
                if (mRingBuffer[curPos] == CmdResultFactory.CMD_START_TAG) {
                    int dataLen = i;
                    int readPosEnd = mReadPosStart + dataLen - 1;
                    data = new byte[dataLen];

                    if (readPosEnd <= mCapacity - 1) {
                        System.arraycopy(mRingBuffer, mReadPosStart, data, 0, dataLen);
                    } else {
                        int len1 = mCapacity - mReadPosStart;
                        System.arraycopy(mRingBuffer, mReadPosStart, data, 0, len1);
                        System.arraycopy(mRingBuffer, 0, data, len1, dataLen - len1);
                    }

                    //跳过:END
                    mReadSize -= dataLen;
                    mWriteSize += dataLen;
                    mReadPosStart = (mReadPosStart + dataLen) % mCapacity;

                    //统计已分析正确的字符数量
                    addAnalyzedCount(dataLen);
                    find = true;
                    break;
                }
            }

            if(paraLen <= mReadSize && paraLen > 0 && !find){
                int readPosEnd = mReadPosStart + paraLen - 1;
                data = new byte[paraLen];

                if (readPosEnd <= mCapacity - 1) {
                    System.arraycopy(mRingBuffer, mReadPosStart, data, 0, paraLen);
                } else {
                    int len1 = mCapacity - mReadPosStart;
                    System.arraycopy(mRingBuffer, mReadPosStart, data, 0, len1);
                    System.arraycopy(mRingBuffer, 0, data, len1, paraLen - len1);
                }

                //跳过:END
                mReadSize -= paraLen;
                mWriteSize += paraLen;
                mReadPosStart = (mReadPosStart + paraLen) % mCapacity;

                //统计已分析正确的字符数量
                addAnalyzedCount(paraLen);
            }
        }

        return data;
    }

    //每次仅读取一个标签的数据，如果不够一个标签，则不读取
    public ProtocalCmd get(){
        byte[] cmd = null;
        byte[] para = null;
        synchronized (this) {
            //缓冲区中有数据可读
            int startPos = getCmdStartPos();
            if(startPos < 0){
                return null;
            }

            saveStatus();
            cmd = getCmdTag();
            if(cmd == null){
                restoreStatus();
                return  null;
            }

            para = getCmdPara();
            if(para == null){
                restoreStatus();
                return null;
            }
        }
        String cmdStr = new String(cmd, Charset.forName("UTF-8"));
        ProtocalCmd cmdResult = CmdResultFactory.getCmdResult(cmdStr, para);
        if(cmdResult != null && cmdResult.isValid()){
            return  cmdResult;
        }

        addErrCount(para.length+cmd.length);
        return null;
    }

    public int put(byte[] data){
        int size = 0;
        int len = data.length;

        synchronized (this){
            if(mWriteSize > len){
                if(mCapacity >= mWritePosStart + len){
                    System.arraycopy(data, 0, mRingBuffer, mWritePosStart, len);
                } else {
                    int len1 = mCapacity - mWritePosStart;
                    System.arraycopy(data, 0, mRingBuffer, mWritePosStart, len1);
                    System.arraycopy(data, len1, mRingBuffer, 0,len - len1);
                }

                mWritePosStart = (mWritePosStart + len) % mCapacity;
                mWriteSize -= len;
                mReadSize += len;
                size = len;
            }
        }

        //统计接收到的数据总数
        addRecvCount(size);
        return size;
    }

    private long addErrCount(int err){
        long errCount = 0;
        synchronized (this){
            if(lErrCount + err >=  Long.MAX_VALUE){
                //复位
                lErrCount = err;
            } else {
                lErrCount = lErrCount + err;
            }

            errCount = lErrCount;
        }
        return errCount;
    }

    private long addRecvCount(int recv){
        long recvCount = 0;
        synchronized (this) {
            if (lRecvCount + recv >= Long.MAX_VALUE) {
                //复位
                lRecvCount = recv;
            } else {
                lRecvCount = lRecvCount + recv;
            }

            recvCount = lRecvCount;
        }
        return recvCount;
    }

    private long addAnalyzedCount(int recv){
        long analyzeCount = 0;
        synchronized (this) {
            if (lAnalyzedCount + recv >= Long.MAX_VALUE) {
                //复位
                lAnalyzedCount = recv;
            } else {
                lAnalyzedCount = lAnalyzedCount + recv;
            }

            analyzeCount = lAnalyzedCount;
        }
        return analyzeCount;
    }

    public long getErrCount(){
        long errCount = 0;
        synchronized (this){
            errCount = lErrCount;
        }
        return errCount;
    }

    public long getAnalyzedCount(){
        long analyzedCount = 0;
        synchronized (this){
            analyzedCount = lAnalyzedCount + lErrCount;
        }
        return analyzedCount;
    }

    public long getRecvCount(){
        long recv = 0;
        synchronized (this){
            recv = lRecvCount;
        }
        return recv;
    }
}
