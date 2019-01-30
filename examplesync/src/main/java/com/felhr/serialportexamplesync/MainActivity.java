package com.felhr.serialportexamplesync;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private TextView display;
    private TextView txtview_acc_value;
    private TextView txtview_gyro_value;
    private TextView txtview_err_rate;
    private EditText editText;
    private CheckBox box115200, box921600;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    /*
            * ��ʱ���ת��Ϊʱ��
    */
    public String stampToDate(long timeMillis){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(timeMillis);
        return simpleDateFormat.format(date);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);

        txtview_acc_value = (TextView) findViewById(R.id.txtview_acc_value);
        txtview_gyro_value = (TextView) findViewById(R.id.txtview_gyro_value);
        txtview_err_rate = (TextView) findViewById(R.id.txtview_err_rate);
        /*
        display.setMovementMethod(new ScrollingMovementMethod());

        editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.buttonSend);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    String data = editText.getText().toString();
                    if (usbService != null) { // if UsbService was correctly binded, Send data
                        usbService.write(data.getBytes());
                        synchronized (display){
                            long timeStamp = System.currentTimeMillis();
                            String time = stampToDate(timeStamp);
                            //display.setTextColor(Color.BLUE);
                            display.append("("+time+"):"+data+"\n");

                        }
                    }
                }
            }
        });

        box115200 = (CheckBox) findViewById(R.id.checkBox);
        box115200.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(box115200.isChecked())
                    box921600.setChecked(false);
                else
                    box921600.setChecked(true);
            }
        });

        box921600 = (CheckBox) findViewById(R.id.checkBox2);
        box921600.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(box921600.isChecked())
                    box115200.setChecked(false);
                else
                    box115200.setChecked(true);
            }
        });

        Button baudrateButton = (Button) findViewById(R.id.buttonBaudrate);
        baudrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(box115200.isChecked())
                    usbService.changeBaudRate(115200);
                else
                    usbService.changeBaudRate(921600);
            }
        });*/
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            long timeStamp = System.currentTimeMillis();
            String time = mActivity.get().stampToDate(timeStamp);
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    //mActivity.get().display.append("("+time+"):"+data+"\n");
                    String displayString = "("+time+"):"+data;
                    SpannableString spannableString = new SpannableString(displayString);
                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.GREEN);
                    spannableString.setSpan(colorSpan, 0, displayString.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

                    mActivity.get().display.append(spannableString);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.SYNC_READ:
                    String buffer = (String) msg.obj;

                    //mActivity.get().display.append("("+time+"):"+buffer+"\n");

                    String disString = "("+time+"):"+buffer;
                    SpannableString spanString = new SpannableString(disString);
                    ForegroundColorSpan colSpan = new ForegroundColorSpan(Color.GREEN);
                    spanString.setSpan(colSpan, 0, disString.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

                    mActivity.get().display.append(spanString);
                    break;

                case UsbService.MESSAGE_ACCELERATOR:
                    UsbService.AcceleratorData acc = (UsbService.AcceleratorData) msg.obj;
                    String accStr = "( X: "+acc.mX+", Y: "+ acc.mY + ", Z: " + acc.mZ + " )";
                    mActivity.get().txtview_acc_value.setText(accStr);
                    //mActivity.get().txtview_acc_value.invalidate();
                    break;
                case UsbService.MESSAGE_GYROSCOPE:
                    UsbService.GyroscopeData gyro = (UsbService.GyroscopeData) msg.obj;
                    String gyroStr = "( X: "+gyro.mX+", Y: "+ gyro.mY + ", Z: " + gyro.mZ + " )";
                    mActivity.get().txtview_gyro_value.setText(gyroStr);
                    //mActivity.get().txtview_gyro_value.invalidate();
                    break;
                case UsbService.MESSAGE_ERR_RATE:
                    String errRate = (String) msg.obj;

                    mActivity.get().txtview_err_rate.setText(errRate);
                    //mActivity.get().txtview_gyro_value.invalidate();
                    break;
            }
        }
    }
}
