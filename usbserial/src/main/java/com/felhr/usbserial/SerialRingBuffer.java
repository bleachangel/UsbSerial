package com.felhr.usbserial;

import java.util.Arrays;

public class SerialRingBuffer {
    private int mReadPosStart;
    private int mReadSize;
    private int mWritePosStart;
    private int mWriteSize;
    private int mCapacity;
    private long lRecvCount = 0;
    private long lAnalyzedCount = 0;
    private long lErrCount = 0;
    static final int MIN_BUFFER_SIZE = 1024;
    private byte[] mRingBuffer;

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

    public int getReadStartPos(){
        synchronized (this) {
            //首先读取出一部分数据，确定标签
            if (UsbSerialDevice.MESSAGE_TAG_ACELERATOR != mRingBuffer[mReadPosStart]
                    && UsbSerialDevice.MESSAGE_TAG_GYROSCOPE != mRingBuffer[mReadPosStart]
                    && UsbSerialDevice.MESSAGE_TAG_MAGNETIC != mRingBuffer[mReadPosStart]
                    && UsbSerialDevice.MESSAGE_TAG_ALS != mRingBuffer[mReadPosStart]
                    && UsbSerialDevice.MESSAGE_TAG_PS != mRingBuffer[mReadPosStart]) {
                //不是标签的情况下，必须跳过这部分数据
                for(int i = 1; i < mReadSize; i++){
                    mReadPosStart = (mReadPosStart + 1) % mCapacity;
                    if (UsbSerialDevice.MESSAGE_TAG_ACELERATOR == mRingBuffer[mReadPosStart]
                            || UsbSerialDevice.MESSAGE_TAG_GYROSCOPE == mRingBuffer[mReadPosStart]
                            || UsbSerialDevice.MESSAGE_TAG_MAGNETIC == mRingBuffer[mReadPosStart]
                            || UsbSerialDevice.MESSAGE_TAG_ALS == mRingBuffer[mReadPosStart]
                            || UsbSerialDevice.MESSAGE_TAG_PS == mRingBuffer[mReadPosStart]) {
                        mReadSize -= i;
                        mWriteSize += i;

                        //统计错误率
                        addErrCount(i);
                        break;
                    }
                }
            }
        }
        return mReadPosStart;
    }

    public boolean isValidData(byte[] data, int start, int len){
        boolean valid = true;
        for(int i = start; i < len; i++){
            if(!((data[i] >= 0x30 && data[i] <= 0x39)
                    || (data[i] >= 0x41 && data[i] <= 0x46))){
                //非数字或者16进制字符
                valid = false;
            }
        }
        return valid;
    }

    //每次仅读取一个标签的数据，如果不够一个标签，则不读取
    //tagLen 为标签的长度
    public byte[] get(int tagLen){
        byte[] data = null;
        synchronized (this) {
            //缓冲区中有数据可读
            getReadStartPos();

            if(mReadSize >= tagLen){
                data = new byte[tagLen];
                int readPosEnd = mReadPosStart + tagLen - 1;

                if(readPosEnd <= mCapacity - 1){
                    System.arraycopy(mRingBuffer, mReadPosStart, data, 0, tagLen);
                } else {
                    int len1 = mCapacity - mReadPosStart;
                    System.arraycopy(mRingBuffer, mReadPosStart, data, 0, len1);
                    System.arraycopy(mRingBuffer, 0, data, len1, tagLen - len1);
                }

                if(isValidData(data, 1, tagLen -1)){
                    //释放已读取的缓冲区
                    mReadPosStart = (mReadPosStart + tagLen) % mCapacity;
                    //改变可写缓冲区的大小
                    mWriteSize = mWriteSize + tagLen;
                    mReadSize -= tagLen;

                    //统计已分析正确的字符数量
                    addAnalyzedCount(tagLen);
                } else {
                    //跳过当前的TAG头部
                    mReadPosStart = (mReadPosStart + 1) % mCapacity;
                    //改变可写缓冲区的大小
                    mWriteSize = mWriteSize + 1;
                    mReadSize -= 1;

                    //统计错误率
                    addErrCount(1);

                    //继续下一次的数据
                    data = null;
                }
            }
        }

        return data;
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
