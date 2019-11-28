package com.felhr.madsessions;

import com.felhr.usbserial.UsbSerialInterface;

public class MadUartParameters extends MadConnectionParameters {
    public static final int DEF_BAUD_RATE = 921600;

    public int mBandRate;
    public int mDataBits;
    public int mStopBits;
    public int mParity;
    public int mFlowCtrl;

    public MadUartParameters(){
        super();
        mBandRate = DEF_BAUD_RATE;
        mDataBits = UsbSerialInterface.DATA_BITS_8;
        mStopBits = UsbSerialInterface.STOP_BITS_1;
        mParity = UsbSerialInterface.PARITY_NONE;
        mFlowCtrl = UsbSerialInterface.FLOW_CONTROL_OFF;
    }
    public MadUartParameters(int band_rate, int data_bits, int stop_bits, int parity, int flow_ctrl){
        super();
        mBandRate = band_rate;
        mDataBits = data_bits;
        mStopBits = stop_bits;
        mParity = parity;
        mFlowCtrl = flow_ctrl;
    }
}
