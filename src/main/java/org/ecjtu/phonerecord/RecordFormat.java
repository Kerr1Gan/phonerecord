package org.ecjtu.phonerecord;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Ethan_Xiang on 2018/5/15.
 */
public class RecordFormat implements Parcelable {

    private boolean recordAudio = true;
    private boolean recordVideo = true;
    private int videoWidth = -1;
    private int videoHeight = -1;

    public RecordFormat() {
    }

    protected RecordFormat(Parcel in) {
        recordAudio = in.readByte() != 0;
        recordVideo = in.readByte() != 0;
        videoWidth = in.readInt();
        videoHeight = in.readInt();
    }

    public boolean isRecordAudio() {
        return recordAudio;
    }

    public void setRecordAudio(boolean recordAudio) {
        this.recordAudio = recordAudio;
    }

    public boolean isRecordVideo() {
        return recordVideo;
    }

    public void setRecordVideo(boolean recordVideo) {
        this.recordVideo = recordVideo;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public static final Creator<RecordFormat> CREATOR = new Creator<RecordFormat>() {
        @Override
        public RecordFormat createFromParcel(Parcel in) {
            return new RecordFormat(in);
        }

        @Override
        public RecordFormat[] newArray(int size) {
            return new RecordFormat[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isRecordAudio() ? 1 : 0));
        dest.writeByte((byte) (isRecordVideo() ? 1 : 0));
        dest.writeInt(getVideoWidth());
        dest.writeInt(getVideoHeight());
    }
}
