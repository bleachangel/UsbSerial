package com.felhr.usbserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import com.felhr.protocal.CmdResultFactory;
import com.felhr.protocal.ProtocalCmd;
import com.felhr.protocal.SerialRingBuffer;

public abstract class UsbSerialDevice implements UsbSerialInterface {
    public static final String TAG="UsbSerialDevice";
    public static final String CDC = "cdc";
    protected static final String COM_PORT = "COM ";
    protected static final int USB_TIMEOUT = 0;
    private static final boolean mr1Version;
    private boolean mDebug = false;

    // Get Android version if version < 4.3 It is not going to be asynchronous read operations
    static {
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.JELLY_BEAN_MR1)
            mr1Version = true;
        else
            mr1Version = false;
    }

    protected final UsbDevice device;
    protected final UsbDeviceConnection connection;
    protected SerialBuffer serialBuffer;
    protected SerialRingBuffer analyzeBuffer;
    protected WorkerThread workerThread;
    protected WriteThread writeThread;
    protected BufferThread bufferThread;
    // InputStream and OutputStream (only for sync api)
    protected SerialInputStream inputStream;
    protected SerialOutputStream outputStream;
    protected boolean asyncMode;
    protected boolean isOpen;
    // Endpoints for synchronous read and write operations
    private UsbEndpoint inEndpoint;
    private UsbEndpoint outEndpoint;
    private String portName = "";
    static final int DEFAULT_READ_BUFFER_SIZE = 512;

    public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection) {
        this.device = device;
        this.connection = connection;
        this.asyncMode = true;
        serialBuffer = new SerialBuffer(mr1Version);

        analyzeBuffer = new SerialRingBuffer(DEFAULT_READ_BUFFER_SIZE);
    }

    public static UsbSerialDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection) {
        return createUsbSerialDevice(device, connection, -1);
    }

    public static UsbSerialDevice createUsbSerialDevice(UsbDevice device, UsbDeviceConnection connection, int iface) {
        /*
         * It checks given vid and pid and will return a custom driver or a CDC serial driver.
         * When CDC is returned open() method is even more important, its response will inform about if it can be really
         * opened as a serial device with a generic CDC serial driver
         */
        int vid = device.getVendorId();
        int pid = device.getProductId();

        if (isCdcDevice(device))
            return new CDCSerialDevice(device, connection, iface);
        else
            return null;
    }

    public static UsbSerialDevice createUsbSerialDevice(String type, UsbDevice device, UsbDeviceConnection connection, int iface) {
        if (type.equals(CDC)) {
            return new CDCSerialDevice(device, connection, iface);
        } else {
            throw new IllegalArgumentException("Invalid type argument. Must be:cdc, ch34x, cp210x, ftdi or pl2303");
        }
    }

    public static boolean isSupported(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();

        if (isCdcDevice(device))
            return true;
        else
            return false;
    }

    public static boolean isCdcDevice(UsbDevice device) {
        int iIndex = device.getInterfaceCount();
        for (int i = 0; i <= iIndex - 1; i++) {
            UsbInterface iface = device.getInterface(i);
            if (iface.getInterfaceClass() == UsbConstants.USB_CLASS_CDC_DATA)
                return true;
        }
        return false;
    }

    // Common Usb Serial Operations (I/O Asynchronous)
    @Override
    public abstract boolean open();

    @Override
    public void write(byte[] buffer) {
        if (asyncMode) {
            serialBuffer.putWriteBuffer(buffer);
        }
    }

    /**
     * Classes that do not implement {@link #setInitialBaudRate(int)} should always return -1
     *
     * @return initial baud rate used when initializing the serial connection
     */
    public int getInitialBaudRate() {
        return -1;
    }

    /**
     * <p>
     * Use this setter <strong>before</strong> calling {@link #open()} to override the default baud rate defined in this particular class.
     * </p>
     *
     * <p>
     * This is a workaround for devices where calling {@link #setBaudRate(int)} has no effect once {@link #open()} has been called.
     * </p>
     *
     * @param initialBaudRate baud rate to be used when initializing the serial connection
     */
    public void setInitialBaudRate(int initialBaudRate) {
        // this class does not implement initialBaudRate
    }

    @Override
    public int setReadCallback(UsbReadCallback mCallback) {
        if (!asyncMode)
            return -1;

        if (mr1Version) {
            if (workerThread != null) {
                workerThread.getUsbRequest().queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
            }

            if (bufferThread != null) {
                bufferThread.setCallback(mCallback);
            }
        }

        return 0;
    }

    public int removeReadCallback(int session) {
        int ret = -1;
        if(bufferThread != null){
            ret = bufferThread.removeCallback();
        }
        return ret;
    }

    @Override
    public abstract void close();

    // Common Usb Serial Operations (I/O Synchronous)
    @Override
    public abstract boolean syncOpen();

    @Override
    public abstract void syncClose();

    @Override
    public int syncWrite(byte[] buffer, int timeout) {
        if (buffer == null)
            return 0;

        return connection.bulkTransfer(outEndpoint, buffer, buffer.length, timeout);
    }

    @Override
    public int syncRead(byte[] buffer, int timeout) {
        if (asyncMode) {
            return -1;
        }

        if (buffer == null)
            return 0;

        return connection.bulkTransfer(inEndpoint, buffer, buffer.length, timeout);
    }

    // Serial port configuration
    @Override
    public abstract void setBaudRate(int baudRate);

    @Override
    public abstract void setDataBits(int dataBits);

    @Override
    public abstract void setStopBits(int stopBits);

    @Override
    public abstract void setParity(int parity);

    @Override
    public abstract void setFlowControl(int flowControl);

    public SerialInputStream getInputStream() {
        if (asyncMode)
            throw new IllegalStateException("InputStream only available in Sync mode. \n" +
                    "Open the port with syncOpen()");
        return inputStream;
    }

    public SerialOutputStream getOutputStream() {
        if (asyncMode)
            throw new IllegalStateException("OutputStream only available in Sync mode. \n" +
                    "Open the port with syncOpen()");
        return outputStream;
    }

    public int getVid() {
        return device.getVendorId();
    }

    public int getPid() {
        return device.getProductId();
    }

    public int getDeviceId() {
        return device.getDeviceId();
    }

    //Debug options
    public void debug(boolean value) {
        if (serialBuffer != null)
            serialBuffer.debug(value);
    }

    public String getPortName() {
        return this.portName;
    }

    public long getErrCount(){
        return  analyzeBuffer.getErrCount();
    }

    public long getRecvCount(){
        return  analyzeBuffer.getRecvCount();
    }

    public long getAnalyzedCount(){
        return  analyzeBuffer.getAnalyzedCount();
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public boolean isOpen() {
        return isOpen;
    }

    protected void setSyncParams(UsbEndpoint inEndpoint, UsbEndpoint outEndpoint) {
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
    }

    protected void setThreadsParams(UsbRequest request, UsbEndpoint endpoint) {
        if (mr1Version) {
            workerThread.setUsbRequest(request);
            //writeThread.setUsbEndpoint(endpoint);
        }
    }

    /*
     * Kill workingThread; This must be called when closing a device
     */
    protected void killWorkingThread() {
        if (mr1Version && workerThread != null) {
            workerThread.stopThread();
            workerThread = null;
        }
    }

    /*
     * Restart workingThread if it has been killed before
     */
    protected void restartWorkingThread() {
        if (mr1Version && workerThread == null) {
            workerThread = new WorkerThread();
            workerThread.start();
            while (!workerThread.isAlive()) {
            } // Busy waiting
        }
    }

    protected void killWriteThread() {
        if (writeThread != null) {
            writeThread.stopThread();
            writeThread = null;
        }
    }

    protected void restartWriteThread() {
        if (writeThread == null) {
            writeThread = new WriteThread();
            writeThread.start();
            while (!writeThread.isAlive()) {
            } // Busy waiting
        }
    }

    protected void killBufferThread() {
        if (bufferThread != null) {
            bufferThread.stopThread();
            bufferThread = null;
        }
    }

    protected void restartBufferThread() {
        if (bufferThread == null) {
            bufferThread = new BufferThread();
            bufferThread.start();
            while (!bufferThread.isAlive()) {
            } // Busy waiting
        }
    }

    /*
     * WorkerThread waits for request notifications from IN endpoint
     */
    protected class WorkerThread extends AbstractWorkerThread {
        private UsbRequest requestIN = null;

        public WorkerThread() {
            super("WorkerThread");
        }

        @Override
        public void doRun() {
            // Queue a new request
            if (requestIN != null && started) {
                requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);

                UsbRequest resonse = null;
                try {
                    resonse = connection.requestWait();

                    if (resonse != null && resonse.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                            && resonse.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN) {
                        byte[] data = serialBuffer.getDataReceived();

                        // Clear buffer, execute the callback
                        serialBuffer.clearReadBuffer();

                        //加入到分析缓冲区
                        analyzeBuffer.put(data);
                        if(mDebug) {
                            Log.d(TAG, new String(data));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(1,0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public UsbRequest getUsbRequest() {
            return requestIN;
        }

        public void setUsbRequest(UsbRequest request) {
            this.requestIN = request;
        }
    }

    private class WriteThread extends AbstractWorkerThread {
        private UsbEndpoint outEndpoint;

        public WriteThread() {
            super("WriteThread");
        }

        @Override
        public void doRun() {
            byte[] data = serialBuffer.getWriteBuffer();
            if (data.length > 0)
                connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void setUsbEndpoint(UsbEndpoint outEndpoint) {
            this.outEndpoint = outEndpoint;
        }
    }

    /*
     * BufferThread waits for request notifications from  WorkerThread
     */
    protected class BufferThread extends AbstractWorkerThread {
        private boolean mTestMode;
        private UsbReadCallback mReadCallback;
        public BufferThread() {
            super("BufferThread");
            mTestMode = false;
            mReadCallback = null;
        }

        @Override
        public void doRun() {
            if(started) {
                if(mTestMode) {
                    byte[] data;
                    data = analyzeBuffer.getBuffer();
                    if (data != null && mReadCallback != null) {
                        mReadCallback.onReceivedDataForTest(data);
                    }
                } else {
                    ProtocalCmd result = analyzeBuffer.get();
                    if(result != null && mReadCallback != null){
                        int cmdValue = result.getCmdValue();
                        if(!(cmdValue == CmdResultFactory.CMD_KEEP_LIVE_VALUE || cmdValue == CmdResultFactory.CMD_ERR_MESSAGE_VALUE)) {
                            mReadCallback.onReceivedData(result);
                        }
                    }
                }
            }
            try {
                Thread.sleep(1,0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public int setCallback(UsbReadCallback mCallback) {
            int ret = 0;
            mReadCallback = mCallback;
            return ret;
        }

        public int removeCallback(){
            int ret = 0;
            mReadCallback = null;
            return ret;
        }

        public boolean setTestMode(boolean test_mode){
            mTestMode = test_mode;
            return true;
        }
    }

    public int resume(){
        int ret = -1;
        if(workerThread != null && bufferThread != null) {
            workerThread.resumeThread();
            bufferThread.resumeThread();
            ret = 0;
        }
        return ret;
    }

    public int pause(){
        int ret = -1;
        if(workerThread != null && bufferThread != null) {
            workerThread.pauseThread();
            bufferThread.pauseThread();
            ret = 0;
        }
        return ret;
    }

    public boolean setTestMode(boolean test_mode) {
        return bufferThread.setTestMode(test_mode);
    }
}
