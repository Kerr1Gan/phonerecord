package org.ecjtu.phonerecord.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;

import org.ecjtu.phonerecord.IRecordCallback;
import org.ecjtu.phonerecord.IRecordService;
import org.ecjtu.phonerecord.RecordFormat;
import org.ecjtu.phonerecord.RecordStatus;
import org.ecjtu.phonerecord.media.MediaAudioEncoder;
import org.ecjtu.phonerecord.media.MediaEncoder;
import org.ecjtu.phonerecord.media.MediaMuxerWrapper;
import org.ecjtu.phonerecord.media.MediaScreenEncoder;

import java.io.IOException;


/**
 * Created by Ethan_Xiang on 2018/5/15.
 */

public class ScreenRecorderBinderService extends Service {
    private static final boolean DEBUG = false;
    private static final String TAG = "ScreenRecorderService";

    /**
     * int N = mCallbacks.beginBroadcast();
     * try {
     * for (int i = 0; i < N; i++) {
     * mCallbacks.getBroadcastItem(i).onStateChanged(state);
     * }
     * <p>
     * } catch (RemoteException e) {
     * e.printStackTrace();
     * }
     * mCallbacks.finishBroadcast();
     */
    private RemoteCallbackList<IRecordCallback> mCallbacks = new RemoteCallbackList<>();

    private RecordStatus mRecordStatus;

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Object mSync = new Object();
    private MediaMuxerWrapper mMuxer;

    private long mPausedTime;
    private long mTimeOffset;
    private long mStartTime;

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind " + intent.toString());
        return new ScreenRecorderBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind " + intent.toString());
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {
            Log.i(TAG, "onCreate");
        }
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public void registerCallback(IRecordCallback callback) {
        if (callback != null) {
            mCallbacks.register(callback);
        }
    }

    public void unregisterCallback(IRecordCallback callback) {
        if (callback != null) {
            mCallbacks.unregister(callback);
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        mCallbacks.kill();
        super.onDestroy();
    }

    /**
     * start screen recording as .mp4 file
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenRecord(Intent intent, int resultCode, String outputPath, RecordFormat recordFormat) {
        if (DEBUG) Log.v(TAG, "startScreenRecord:mMuxer=" + mMuxer);
        synchronized (mSync) {
            if (mMuxer == null) {
                // get MediaProjection
                final MediaProjection projection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
                if (projection != null) {
                    final DisplayMetrics metrics = getResources().getDisplayMetrics();
                    final int density = metrics.densityDpi;

                    mMediaProjection = projection;

                    if (DEBUG) Log.v(TAG, "startRecording:");
                    try {
                        mMuxer = new MediaMuxerWrapper(".mp4"); // if you record audio only, ".m4a" is also OK.

                        //change output path add in 2016.6.30 by KerriGan.
                        if (outputPath != null) {
                            mMuxer.setOutputPath(outputPath, true);
                        }

                        RecordFormat localRecorderFormat = null;
                        if (recordFormat != null) {
                            localRecorderFormat = recordFormat;
                        }

                        if (localRecorderFormat == null || localRecorderFormat.isRecordVideo()) {
                            int width = metrics.widthPixels;
                            int height = metrics.heightPixels;
                            if (localRecorderFormat != null) {
                                if (localRecorderFormat.getVideoWidth() != -1) {
                                    width = localRecorderFormat.getVideoWidth();
                                }
                                if (localRecorderFormat.getVideoHeight() != -1) {
                                    height = localRecorderFormat.getVideoHeight();
                                }
                            }
                            // for screen capturing
                            new MediaScreenEncoder(mMuxer, mMediaEncoderListener,
                                    projection, width, height, density);
                        }
                        if (localRecorderFormat == null || localRecorderFormat.isRecordAudio()) {
                            // for audio capturing
                            new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
                        }
                        mMuxer.prepare();
                        mMuxer.startRecording();
                        mStartTime = System.nanoTime() / 1000;

                        callRecordStatusChanged(getRecordStatus());
                    } catch (final IOException e) {
                        Log.e(TAG, "startScreenRecord:", e);
                        callErrorMsg(e.toString());
                    }
                }
            }
        }
    }

    /**
     * stop screen recording
     */
    private void stopScreenRecord() {
        if (DEBUG) Log.v(TAG, "stopScreenRecord:mMuxer=" + mMuxer);
        synchronized (mSync) {
            if (mMuxer != null) {
                mMuxer.stopRecording();
                mMuxer = null;
                // you should not wait here
            }
        }
        callRecordStatusChanged(getRecordStatus());
    }

    private void pauseScreenRecord() {
        synchronized (mSync) {
            if (mMuxer != null) {
                mMuxer.pauseRecording();
                mPausedTime = System.nanoTime() / 1000;
            }
        }
        callRecordStatusChanged(getRecordStatus());
    }

    private void resumeScreenRecord() {
        synchronized (mSync) {
            if (!getRecordStatus().isPausing()) {
                return;
            }
            if (mMuxer != null) {
                mMuxer.resumeRecording();
                mTimeOffset += System.nanoTime() / 1000 - mPausedTime;
                mPausedTime = 0;
            }
        }
        callRecordStatusChanged(getRecordStatus());
    }

    public void callRecordStatusChanged(RecordStatus status) {
        int N = mCallbacks.beginBroadcast();
        try {
            for (int i = 0; i < N; i++) {
                mCallbacks.getBroadcastItem(i).onStateChanged(status);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mCallbacks.finishBroadcast();
    }

    public void callRecordTimeChanged(RecordStatus status) {
        int N = mCallbacks.beginBroadcast();
        try {
            for (int i = 0; i < N; i++) {
                mCallbacks.getBroadcastItem(i).onRecordTimeChanged(status.getRecordTime());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mCallbacks.finishBroadcast();
    }

    public void callErrorMsg(String msg) {
        int N = mCallbacks.beginBroadcast();
        try {
            for (int i = 0; i < N; i++) {
                mCallbacks.getBroadcastItem(i).onError(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mCallbacks.finishBroadcast();
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
        }
    };

    private String getOutputPath() {
        if (mMuxer == null) {
            return null;
        }
        return mMuxer.getOutputPath();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void releaseResource() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    public RecordStatus getRecordStatus() {
        if (mRecordStatus == null) {
            mRecordStatus = new RecordStatus();
        }
        final boolean isRecording, isPausing;
        synchronized (mSync) {
            isRecording = (mMuxer != null);
            isPausing = isRecording ? mMuxer.isPaused() : false;
        }
        mRecordStatus.setPausing(isPausing);
        mRecordStatus.setRecording(isRecording);
        mRecordStatus.setRecordTime(System.nanoTime() / 1000 - mStartTime - mTimeOffset);
        return mRecordStatus;
    }

    private class ScreenRecorderBinder extends IRecordService.Stub {


        @Override
        public void startScreenRecordV2(Intent intent, int requestCode, String outputPath, RecordFormat recordFormat) throws RemoteException {
            startScreenRecord(intent, requestCode, outputPath, recordFormat);
        }

        @Override
        public void startScreenRecordV1(Intent intent, int requestCode) throws RemoteException {
            startScreenRecordV2(intent, requestCode, null, null);
        }


        @Override
        public RecordStatus getRecordStatus() throws RemoteException {
            return ScreenRecorderBinderService.this.getRecordStatus();
        }

        @Override
        public void stopScreenRecord() throws RemoteException {
            ScreenRecorderBinderService.this.stopScreenRecord();
        }

        @Override
        public void pauseScreenRecord() throws RemoteException {
            ScreenRecorderBinderService.this.pauseScreenRecord();
        }

        @Override
        public void resumeScreenRecord() throws RemoteException {
            ScreenRecorderBinderService.this.resumeScreenRecord();
        }

        @Override
        public void releaseResource() throws RemoteException {
            ScreenRecorderBinderService.this.releaseResource();
        }

        @Override
        public int getPid() throws RemoteException {
            return android.os.Process.myPid();
        }

        @Override
        public String getOutputPath() throws RemoteException {
            return ScreenRecorderBinderService.this.getOutputPath();
        }

        @Override
        public void registerCallback(IRecordCallback callback) throws RemoteException {
            ScreenRecorderBinderService.this.registerCallback(callback);
        }

        @Override
        public void unregisterCallback(IRecordCallback callback) throws RemoteException {
            ScreenRecorderBinderService.this.unregisterCallback(callback);
        }

    }
}
