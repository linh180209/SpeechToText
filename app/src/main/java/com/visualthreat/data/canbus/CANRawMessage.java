package com.visualthreat.data.canbus;

import java.util.Arrays;

/**
 * Created by USER on 1/9/2017.
 */

/**
 * Instances of the class is to store RAW message read from serial port
 */
public class CANRawMessage {
    private long timeStamp;
    private byte[] data;

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public CANRawMessage(byte[] in, int length) {
        timeStamp = System.currentTimeMillis();
        data = Arrays.copyOf(in, length);
    }
}
