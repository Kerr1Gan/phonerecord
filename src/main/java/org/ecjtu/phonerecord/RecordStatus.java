package org.ecjtu.phonerecord;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by xiang on 2018/5/19.
 */
public class RecordStatus implements Parcelable {

    private boolean isRecording;
    private boolean isPausing;
    private long recordTime;

    public RecordStatus() {
    }

    protected RecordStatus(Parcel in) {
        isRecording = in.readByte() != 0;
        isPausing = in.readByte() != 0;
        recordTime = in.readLong();
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setRecording(boolean recording) {
        isRecording = recording;
    }

    public boolean isPausing() {
        return isPausing;
    }

    public void setPausing(boolean pausing) {
        isPausing = pausing;
    }

    public long getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(long recordTime) {
        this.recordTime = recordTime;
    }

    public static final Creator<RecordStatus> CREATOR = new Creator<RecordStatus>() {
        @Override
        public RecordStatus createFromParcel(Parcel in) {
            return new RecordStatus(in);
        }

        @Override
        public RecordStatus[] newArray(int size) {
            return new RecordStatus[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isRecording ? 1 : 0));
        dest.writeByte((byte) (isPausing ? 1 : 0));
        dest.writeLong(recordTime);
    }
}
