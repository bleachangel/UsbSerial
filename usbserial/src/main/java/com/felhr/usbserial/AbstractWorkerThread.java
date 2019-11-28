package com.felhr.usbserial;

abstract class AbstractWorkerThread extends Thread {
    boolean firstTime = true;
    boolean started = false;
    private volatile boolean keep = true;

    AbstractWorkerThread(){
        super();
        started = false;
    }

    AbstractWorkerThread(String name){
        super(name);
        started = false;
    }

    void stopThread() {
        keep = false;
    }

    int pauseThread(){
        started = false;
        return 0;
    }

    int resumeThread(){
        started = true;
        return 0;
    }

    public final void run() {
        while (keep) {
            doRun();
        }
    }

    abstract void doRun();
}
