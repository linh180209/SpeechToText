package com.visualthreat.data.canbus;

/**
 * Created by USER on 1/9/2017.
 */

public class CANLogEntry implements Comparable<CANLogEntry>{
    private static final String TAG = CANLogEntry.class.getSimpleName();
    public static final int MAX_DATA_LENGTH = 8;
    private static final String LOG_LINE_PREFIX = "{\"timestamp\":";

    public enum SortBy {
        TIMESTAMP,
        ID,
        SEQNUM
    }

    public enum FrameType {
        response,
        request,
        comment
    }

    public enum CategoryType {
        HFCDF,
        LFCDF,
        SDF
    }

    private long timeStamp = 0;
    private int seqNum = 0;
    private boolean is_extended_id = false;
    private int dlc = 8;
    private int id;
    private byte[] data = new byte[8];
    private FrameType type = FrameType.response;
    private boolean no_data = false;
    private CategoryType category = CategoryType.SDF;
    private String comment = "";

    // Default constructor used by ObjectMapper
    public CANLogEntry() {
        this.timeStamp = System.currentTimeMillis();
    }

    public CANLogEntry(long time, int id, int dlc, byte[] data) {
        this.timeStamp = time;
        setId(id);
        setDlc(dlc);
        setData(data);
    }

    public CANLogEntry(int seqNum, int id, int dlc, String type, byte[] data) {
        this(System.currentTimeMillis(), id, dlc, data);
        this.seqNum = seqNum;

        try {
            this.type = FrameType.valueOf(type);
        } catch (IllegalArgumentException e) {
            this.type = FrameType.response;
        }
    }

    public CANLogEntry(long time, int seqNum, int id, int dlc, String type, byte[] data) {
        this(time, id, dlc, data);
        this.seqNum = seqNum;

        try {
            this.type = FrameType.valueOf(type);
        } catch (IllegalArgumentException e) {
            this.type = FrameType.response;
        }
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public boolean is_extended_id() {
        return is_extended_id;
    }

    public int getDlc() {
        return dlc;
    }

    public int getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    public FrameType getType() {
        return type;
    }

    public boolean isNo_data() {
        return no_data;
    }

    public CategoryType getCategory() {
        return category;
    }

    public String getComment() {
        return comment;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setDlc(int dlc) {
        if (dlc >= 0 && dlc <= MAX_DATA_LENGTH) { //dlc must be between 0 and 8'
            this.dlc = dlc;
        } else {
            throw new IllegalArgumentException("dlc must be between 0 and 8");
        }
    }

    public void setData(byte[] newData) {
        if (newData == null || newData.length == 0) {
            return;
        }
        if (newData.length > MAX_DATA_LENGTH) { // CAN data cannot contain more than 8 bytes
            throw new IllegalArgumentException("CAN data cannot contain more than 8 bytes");
        }
        System.arraycopy(newData, 0, this.data, 0, newData.length);
    }

    public void setId(int newId) {
        // ensure standard id is in range
        if (newId >= 0 && newId <= 0x7FF) {
            this.id = newId;
            this.is_extended_id = false;
        } else if (newId > 0x7FF && newId <= 0x1FFFFFFF) {
            //otherwise, check if frame is extended
            this.id = newId;
            this.is_extended_id = true;
        } else {
            throw new IllegalArgumentException("CAN ID=" + newId + " out of range");
        }
    }

    @Override
    public String toString() {
        if (type == FrameType.comment) {
            return String.format("{comment:\"%s\"}", this.comment);
        }

        return String.format("{id:0x%X,dlc:%d,data:[0x%X,0x%X,0x%X,0x%X,0x%X,0x%X,0x%X,0x%X]}",
                this.id,
                this.dlc,
                this.data[0],
                this.data[1],
                this.data[2],
                this.data[3],
                this.data[4],
                this.data[5],
                this.data[6],
                this.data[7]);
    }

    /**
     * Used to store CANLogEntries in timestamp order
     * @param o2
     * @return 0 this = o2
     *         1 this > o2
     *         -1 this < o2
     */
    @Override
    public int compareTo(CANLogEntry o2) {
        if (o2 == null) {
            return 1;
        }

        if (this.equals(o2)) {
            return 0;
        }
        if (this.timeStamp < o2.timeStamp) {
            return -1;
        } else {
            return 1;
        }
    }

    public String toLogLine() {
        String dataArray = String.format("0x%X,0x%X,0x%X,0x%X,0x%X,0x%X,0x%X,0x%X",
                this.data[0],
                this.data[1],
                this.data[2],
                this.data[3],
                this.data[4],
                this.data[5],
                this.data[6],
                this.data[7]);

        if (this.getType() == FrameType.comment) {
            return String.format("{\"timestamp\":%d,\"type\":\"comment\",\"comment\":\"%s\"}",
                    this.getTimeStamp(), this.getComment());
        }
        if (this.getType() == FrameType.request) {
            return String.format("{\"timestamp\":%d,\"type\":\"request\",\"id\":0x%X,\"dlc\":%d,\"data\":[%s]}",
                    this.getTimeStamp(), this.getId(), this.getDlc(), dataArray);
        } else {
            return String.format("{\"timestamp\":%d,\"id\":0x%X,\"dlc\":%d,\"data\":[%s]}",
                    this.getTimeStamp(), this.getId(), this.getDlc(), dataArray);
        }
    }
}
