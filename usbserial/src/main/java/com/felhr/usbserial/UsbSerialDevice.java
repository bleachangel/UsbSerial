package com.felhr.usbserial;

import com.felhr.deviceids.CH34xIds;
import com.felhr.deviceids.CP210xIds;
import com.felhr.deviceids.FTDISioIds;
import com.felhr.deviceids.PL2303Ids;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;

import java.nio.ByteBuffer;
import java.util.Arrays;

public abstract class UsbSerialDevice implements UsbSerialInterface {
    public static final String CDC = "cdc";
    public static final String CH34x = "ch34x";
    public static final String CP210x = "cp210x";
    public static final String FTDI = "ftdi";
    public static final String PL2303 = "pl2303";

    protected static final String COM_PORT = "COM ";
    protected static final int USB_TIMEOUT = 0;
    private static final boolean mr1Version;

    public static final byte MESSAGE_TAG_ACELERATOR = 'L';
    public static final byte MESSAGE_TAG_GYROSCOPE = 'G';
    public static final byte MESSAGE_TAG_MAGNETIC = 'M';
    public static final byte MESSAGE_TAG_ALS = 'S';
    public static final byte MESSAGE_TAG_PS = 'P';

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
    //protected SerialBuffer readBuffer;
    protected ByteBuffer readBuffer;
    protected SerialRingBuffer analyzeBuffer;
    protected WorkerThread workerThread;
    protected WriteThread writeThread;
    protected ReadThread readThread;
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
    static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;

    public UsbSerialDevice(UsbDevice device, UsbDeviceConnection connection) {
        this.device = device;
        this.connection = connection;
        this.asyncMode = true;
        serialBuffer = new SerialBuffer(mr1Version);

        readBuffer = ByteBuffer.allocate(DEFAULT_READ_BUFFER_SIZE);
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

        if (FTDISioIds.isDeviceSupported(vid, pid))
            return new FTDISerialDevice(device, connection, iface);
        else if (CP210xIds.isDeviceSupported(vid, pid))
            return new CP2102SerialDevice(device, connection, iface);
        else if (PL2303Ids.isDeviceSupported(vid, pid))
            return new PL2303SerialDevice(device, connection, iface);
        else if (CH34xIds.isDeviceSupported(vid, pid))
            return new CH34xSerialDevice(device, connection, iface);
        else if (isCdcDevice(device))
            return new CDCSerialDevice(device, connection, iface);
        else
            return null;
    }

    public static UsbSerialDevice createUsbSerialDevice(String type, UsbDevice device, UsbDeviceConnection connection, int iface) {
        if (type.equals(FTDI)) {
            return new FTDISerialDevice(device, connection, iface);
        } else if (type.equals(CP210x)) {
            return new CP2102SerialDevice(device, connection, iface);
        } else if (type.equals(PL2303)) {
            return new PL2303SerialDevice(device, connection, iface);
        } else if (type.equals(CH34x)) {
            return new CH34xSerialDevice(device, connection, iface);
        } else if (type.equals(CDC)) {
            return new CDCSerialDevice(device, connection, iface);
        } else {
            throw new IllegalArgumentException("Invalid type argument. Must be:cdc, ch34x, cp210x, ftdi or pl2303");
        }
    }

    public static boolean isSupported(UsbDevice device) {
        int vid = device.getVendorId();
        int pid = device.getProductId();

        if (FTDISioIds.isDeviceSupported(vid, pid))
            return true;
        else if (CP210xIds.isDeviceSupported(vid, pid))
            return true;
        else if (PL2303Ids.isDeviceSupported(vid, pid))
            return true;
        else if (CH34xIds.isDeviceSupported(vid, pid))
            return true;
        else if (isCdcDevice(device))
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
        if (asyncMode)
            serialBuffer.putWriteBuffer(buffer);
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
        } else {
            readThread.setCallback(mCallback);
            //readThread.start();
        }
        return 0;
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
        if (!asyncMode) {
            if (buffer == null)
                return 0;

            return connection.bulkTransfer(outEndpoint, buffer, buffer.length, timeout);
        } else {
            return -1;
        }
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

    private boolean isFTDIDevice() {
        return (this instanceof FTDISerialDevice);
    }

    protected void setSyncParams(UsbEndpoint inEndpoint, UsbEndpoint outEndpoint) {
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
    }

    protected void setThreadsParams(UsbRequest request, UsbEndpoint endpoint) {
        if (mr1Version) {
            workerThread.setUsbRequest(request);
            writeThread.setUsbEndpoint(endpoint);
        } else {
            readThread.setUsbEndpoint(request.getEndpoint());
            writeThread.setUsbEndpoint(endpoint);
        }
    }

    /*
     * Kill workingThread; This must be called when closing a device
     */
    protected void killWorkingThread() {
        if (mr1Version && workerThread != null) {
            workerThread.stopThread();
            workerThread = null;
        } else if (!mr1Version && readThread != null) {
            readThread.stopThread();
            readThread = null;
        }
    }

    /*
     * Restart workingThread if it has been killed before
     */
    protected void restartWorkingThread() {
        if (mr1Version && workerThread == null) {
            workerThread = new WorkerThread(this);
            workerThread.start();
            while (!workerThread.isAlive()) {
            } // Busy waiting
        } else if (!mr1Version && readThread == null) {
            readThread = new ReadThread(this);
            readThread.start();
            while (!readThread.isAlive()) {
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
            bufferThread = new BufferThread(this);
            bufferThread.start();
            while (!bufferThread.isAlive()) {
            } // Busy waiting
        }
    }

    /*
     * WorkerThread waits for request notifications from IN endpoint
     */
    protected class WorkerThread extends AbstractWorkerThread {
        private final UsbSerialDevice mUsbSerialDevice;
        private UsbRequest requestIN;

        public WorkerThread(UsbSerialDevice usbSerialDevice) {
            this.mUsbSerialDevice = usbSerialDevice;
        }

        @Override
        public void doRun() {
            UsbRequest request = connection.requestWait();
            if (request != null && request.getEndpoint().getType() == UsbConstants.USB_ENDPOINT_XFER_BULK
                    && request.getEndpoint().getDirection() == UsbConstants.USB_DIR_IN) {
                byte[] data = serialBuffer.getDataReceived();

                // Clear buffer, execute the callback
                serialBuffer.clearReadBuffer();
                //onReceivedData(data);

                //加入到分析缓冲区
                analyzeBuffer.put(data);

                // Queue a new request
                requestIN.queue(serialBuffer.getReadBuffer(), SerialBuffer.DEFAULT_READ_BUFFER_SIZE);
            }
        }

        public UsbRequest getUsbRequest() {
            return requestIN;
        }

        public void setUsbRequest(UsbRequest request) {
            this.requestIN = request;
        }

        private void onReceivedData(byte[] data) {
        }
    }

    private class WriteThread extends AbstractWorkerThread {
        private UsbEndpoint outEndpoint;

        @Override
        public void doRun() {
            byte[] data = serialBuffer.getWriteBuffer();
            if (data.length > 0)
                connection.bulkTransfer(outEndpoint, data, data.length, USB_TIMEOUT);
        }

        public void setUsbEndpoint(UsbEndpoint outEndpoint) {
            this.outEndpoint = outEndpoint;
        }
    }

    protected class ReadThread extends AbstractWorkerThread {
        private final UsbSerialDevice mUsbSerialDevice;

        private UsbReadCallback callback;
        private UsbEndpoint inEndpoint;

        public ReadThread(UsbSerialDevice usbSerialDevice) {
            this.mUsbSerialDevice = usbSerialDevice;
        }

        public void setCallback(UsbReadCallback callback) {
            this.callback = callback;
        }

        @Override
        public void doRun() {
            byte[] dataReceived = null;
            int numberBytes;

            if (inEndpoint != null)
                numberBytes = connection.bulkTransfer(inEndpoint, serialBuffer.getBufferCompatible(),
                        SerialBuffer.DEFAULT_READ_BUFFER_SIZE, 0);
            else
                numberBytes = 0;

            if (numberBytes > 0) {
                dataReceived = serialBuffer.getDataReceivedCompatible(numberBytes);

                // FTDI devices reserve two first bytes of an IN endpoint with info about
                // modem and Line.
                if (isFTDIDevice()) {
                    ((FTDISerialDevice) mUsbSerialDevice).ftdiUtilities.checkModemStatus(dataReceived);

                    if (dataReceived.length > 2) {
                        dataReceived = ((FTDISerialDevice) mUsbSerialDevice).ftdiUtilities.adaptArray(dataReceived);
                        onReceivedData(dataReceived);
                    }
                } else {
                    onReceivedData(dataReceived);
                }
            }
        }

        public void setUsbEndpoint(UsbEndpoint inEndpoint) {
            this.inEndpoint = inEndpoint;
        }

        private void onReceivedData(byte[] data) {
            if (callback != null)
                callback.onReceivedData(data);
        }
    }

    /*
     * BufferThread waits for request notifications from  WorkerThread
     */
    protected class BufferThread extends AbstractWorkerThread {
        private final UsbSerialDevice mUsbSerialDevice;
        private UsbReadCallback callback;
        static final int MIN_TAG_LEN = 13;
        public BufferThread(UsbSerialDevice usbSerialDevice) {
            mUsbSerialDevice = usbSerialDevice;
        }

        @Override
        public void doRun() {
            onReceivedData(analyzeBuffer.get(MIN_TAG_LEN));
        }

        public void setCallback(UsbReadCallback callback) {
            this.callback = callback;
        }

        private void onReceivedData(byte[] data) {
            if(data != null) {
                if (callback != null) {
                    //解析消息头
                    //String temp = new String(data);
                    //String[] message = temp.split("GY|AL|MG|PS|AS");
                    //for(int i = 0; i < Arrays.)
                    callback.onReceivedData(data);
                }
            }
        }
    }
}
