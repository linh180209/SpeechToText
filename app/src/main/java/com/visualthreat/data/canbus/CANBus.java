package com.visualthreat.data.canbus;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.visualthreat.data.log.WriteLog;
import com.visualthreat.data.serial.UsbService;
import com.visualthreat.data.unit.Command;
import com.visualthreat.data.unit.UnitFunction;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by USER on 1/9/2017.
 */

public class CANBus {
    private static String TAG = CANBus.class.getSimpleName();
    public static String    READ            = "readCanBus";
    public static final int MAX_LISTENERS = 8;
    private Thread consumer, publicEvent;
    CANEventListener canListener;
    private WriteLog writeLog;
    private Handler USBHandler;
    /**
     * Array of listeners registered
     */
    AtomicReference<CANEventListener>[] canEventListeners = new AtomicReference[MAX_LISTENERS];
    private BlockingQueue<CANRawMessage> canRAWMessageQueue = new LinkedBlockingQueue<>();

    /**
     * The status of the connection.
     */
    //private volatile boolean connected = false;

    /**
     * <b>int</b> identifying the specific instance of the CANBus. While having only a single
     * instance, 'id' is irrelevant. However, having more than one open connection (using more than
     * one instance of {@link CANBus} ), 'id' helps identifying which Serial connection a message or a
     * log entry came from.
     */
    private int id;

    /**
     * Link to the instance of the class implementing {@link CANClient}.
     */
    private CANClient client;
    private UsbService usbService;

    /**
     * @param id     <b>int</b> identifying the specific instance of the Network-class. While having
     *               only a single instance, {@link #id} is irrelevant. However, having more than one
     *               open connection (using more than one instance of Network), {@link #id} helps
     *               identifying which Serial connection a message or a log entry came from.
     * @param client Link to the instance of the class implementing {@link CANClient}.
     */
    public CANBus(int id, CANClient client) {
        this.id = id;
        this.client = client;
        for(int i = 0; i < MAX_LISTENERS; i++) {
            canEventListeners[i] = new AtomicReference<CANEventListener>();
            canEventListeners[i].set(null);
        }
    }

    /**
     * Just as {@link #CANBus(CANClient)}, but with a default {@link #id} of 0. This constructor
     * may mainly be used if only one Serial connection is needed at any time.
     *
     * @see #CANBus(CANClient)
     */
    public CANBus(CANClient client) {
        this(0, client);
        USBHandler = new MyHandler();
    }

    public void SetUSBSerial(UsbService usbService) {
        this.usbService = usbService;
    }

    public Handler GetHandler(){
        return USBHandler;
    }
    public void start(WriteLog writeLog) {
        this.writeLog = writeLog;
        Log.d(TAG,"CanBus Start");
        usbService.write(Command.START_COMMAND.getBytes());
        usbService.RECEIVE_DATA = true;
        if(consumer == null){
            consumer = new Thread(new CANRawMessageConsumer());
            consumer.start();
        }
    }

    public void stop() {
        Log.d(TAG,"CanBus Stop");
        //connected = false;
        usbService.write(Command.STOP_COMMAND.getBytes());
        consumer = null;
        usbService.RECEIVE_DATA = false;
    }

    public void messageDisconnect() {
        //connected = false;
        consumer = null;
        publicEvent = null;
    }

    public synchronized void registerListener(CANEventListener newListener) {
        canListener = newListener;
        for(AtomicReference<CANEventListener> listenerAtomicReference : canEventListeners){
            CANEventListener listener = listenerAtomicReference.get();
            if(listener == null || listener == newListener) {
                listenerAtomicReference.set(newListener);
                return;
            }
        }
        client.writeLog(id, "Can't listen to CAN events because active listeners are reached to MAX");
    }

    public synchronized void unRegisterListener(CANEventListener newListener) {
        for(AtomicReference<CANEventListener> listenerAtomicReference : canEventListeners){
            CANEventListener listener = listenerAtomicReference.get();
            if(listener == newListener) {
                listenerAtomicReference.set(null);
                return;
            }
        }
    }

    public void publishCANEvent(String event) {
        //Log.d(TAG,"publishCANEvent");
        if(canListener != null) {
            canListener.onEvent(event);
        }

    }


    private class CANRawMessageConsumer implements Runnable {
        public static final int MAX_CAN_MESSAGE_LEN = 21;
        public static final int MIN_CAN_MESSAGE_LEN = 5;

        @Override
        public void run() {
            Log.d(TAG,"CANRawMessageConsumer is starting... ");
            client.writeLog(id, "CANRawMessageConsumer is starting...");
            StringBuilder sb = new StringBuilder(20480);
            int temp = 0;
            try {
                while (UsbService.RECEIVE_DATA) {
                    try {
                        CANRawMessage msg = canRAWMessageQueue.poll(300, TimeUnit.MILLISECONDS);
                        if(msg == null){
                            continue;
                        }
                        if (msg != null) {
                            //Log.d(TAG,"data: " + msg.getData().toString());
                            byte[] buffer = msg.getData();
                            //Log.d(TAG,"size: " + buffer.length);
                            for (int i = 0; i < buffer.length; i++) {
                                byte tmpByte = buffer[i];
                                // adjust from C-Byte to Java-Byte
                                if (tmpByte < 0) {
                                    tmpByte += 256;
                                }
                                char tmpChar = (char) tmpByte;
                                if (tmpChar == '\r' || tmpChar == '\n') {
                                    //parse frame
                                    try {
                                        // CAN Message Validation
                                        if (sb.length() < MIN_CAN_MESSAGE_LEN || sb.length() > MAX_CAN_MESSAGE_LEN) {
                                            sb.setLength(0);
                                            continue;
                                        }
                                        String firstChar = sb.substring(0, 1);
                                        if (firstChar.equals("I")) {
                                            sb.setLength(0);
                                            continue;
                                        } else if (!firstChar.equals("t")) {
                                            sb.setLength(0);
                                            continue;
                                        }
                                        // parse the id, create a frame
                                        CANLogEntry logEntry = new CANLogEntry();
                                        logEntry.setTimeStamp(msg.getTimeStamp());
                                        logEntry.setId(Integer.valueOf(sb.substring(1, 4), 16));
                                        // parse the DLC
                                        logEntry.setDlc(Integer.valueOf(sb.substring(4, 5), 10));
                                        //parse the data bytes
                                        if (logEntry.getDlc() > 8 || sb.length() < logEntry.getDlc() * 2 + 5) {
                                            sb.setLength(0);
                                            continue;
                                        }
                                        logEntry.setData(UnitFunction.hexStringToByteArray(
                                                sb.substring(5, 5 + logEntry.getDlc() * 2)));

                                        //reset sb
                                        sb.setLength(0);
                                        String log = logEntry.toLogLine();
                                        writeLog.addLog(log);
                                        publishCANEvent(log);
                                    } catch (StringIndexOutOfBoundsException ex) {
                                        client.writeLog(id, "Found invalid CAN Message=" + sb.toString());
                                        sb.setLength(0);
                                        continue;
                                    } catch (IllegalArgumentException ile) {
                                        client.writeLog(id, "Got wrong frame data=" + sb.toString());
                                        sb.setLength(0);
                                        continue;
                                    }
                                    // The following line is for debugging only
                                    // client.writeLog(id, logEntry.toString());
                                } else {
                                    sb.append(tmpChar);
                                }
                            }

                        }
                    }
                    catch (Exception e){
                        Log.e(TAG,"error: " + e);
                    }
                }
                Log.d(TAG,"size queue temp: " + temp);
            } finally {
                client.writeLog(id, "CANRawMessageConsumer exiting...");
            }
        }
    }

    public class MyHandler extends Handler {

        public MyHandler() {
            Log.d(TAG,"create Handler");
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    //mActivity.get().display.append(data);
                    break;
                case UsbService.CTS_CHANGE:
                    //Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    //Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.SYNC_READ:
                    String buffer = (String) msg.obj;
                    buffer = buffer + "\r\n";
                    //Log.d(TAG,"size buffer: " + canRAWMessageQueue.size());
                    canRAWMessageQueue.offer(new CANRawMessage(buffer.getBytes(), buffer.getBytes().length));
                    break;
            }
        }
    }
}
