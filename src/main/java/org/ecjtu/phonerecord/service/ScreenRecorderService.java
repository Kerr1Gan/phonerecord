package org.ecjtu.phonerecord.service;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import org.ecjtu.phonerecord.RecorderFormat;
import org.ecjtu.phonerecord.media.MediaAudioEncoder;
import org.ecjtu.phonerecord.media.MediaEncoder;
import org.ecjtu.phonerecord.media.MediaMuxerWrapper;
import org.ecjtu.phonerecord.media.MediaScreenEncoder;
import java.io.IOException;


/**
 * Created by KerriGan on 2016/5/27.
 */
public class ScreenRecorderService extends IntentService {
    private static final boolean DEBUG = false;
    private static final String TAG = "ScreenRecorderService";

    private static final String BASE = "org.ecjtu.share.ScreenRecorderService.";
    public static final String ACTION_START = BASE + "ACTION_START";
    public static final String ACTION_STOP = BASE + "ACTION_STOP";
    public static final String ACTION_PAUSE = BASE + "ACTION_PAUSE";
    public static final String ACTION_RESUME = BASE + "ACTION_RESUME";
    public static final String ACTION_QUERY_STATUS = BASE + "ACTION_QUERY_STATUS";
    public static final String ACTION_QUERY_STATUS_RESULT = BASE + "ACTION_QUERY_STATUS_RESULT";
    public static final String EXTRA_RESULT_CODE = BASE + "EXTRA_RESULT_CODE";
    public static final String EXTRA_QUERY_RESULT_RECORDING = BASE + "EXTRA_QUERY_RESULT_RECORDING";
    public static final String EXTRA_QUERY_RESULT_PAUSING = BASE + "EXTRA_QUERY_RESULT_PAUSING";

    public static final String ACTION_QUERY_OUTPUT_PATH = BASE + "ACTION_QUERY_OUTPUT_PATH";
    public static final String ACTION_QUERY_OUTPUT_PATH_RESULT = BASE + "ACTION_QUERY_OUTPUT_PATH_RESULT";

    public static final String ACTION_RESTART_WITH_NEW_FILE = BASE + "ACTION_RESTART_WITH_NEW_FILE";

    public static final String EXTRA_QUERY_OUTPUT_PATH = BASE + "EXTRA_QUERY_OUTPUT_PATH";

    public static final String EXTRA_SET_OUTPUT_PATH = BASE + "EXTRA_SET_OUTPUT_PATH";
    public static final String EXTRA_RESTART_WITH_NEW_FILE = BASE + "EXTRA_RESTART_WITH_NEW_FILE";
    public static final String EXTRA_SET_RECORDER_FORMAT = BASE + "EXTRA_SET_RECORDER_FORMAT";

    public static final String ACTION_RELEASE_RESOURCE = BASE + "ACTION_RELEASE_RESOURCE";

    private MediaProjectionManager mMediaProjectionManager;
    private static MediaProjection sMediaProjection;
    private static Object sSync = new Object();
    private static MediaMuxerWrapper sMuxer;

    public ScreenRecorderService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.v(TAG, "onCreate:");
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (DEBUG) Log.v(TAG, "onHandleIntent:intent=" + intent);
        final String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startScreenRecord(intent);
            updateStatus();
        } else if (ACTION_STOP.equals(action)) {
            stopScreenRecord();
            updateStatus();
        } else if (ACTION_QUERY_STATUS.equals(action)) {
            updateStatus();
        } else if (ACTION_PAUSE.equals(action)) {
            pauseScreenRecord();
            updateStatus();
        } else if (ACTION_RESUME.equals(action)) {
            resumeScreenRecord();
            updateStatus();
        } else if (ACTION_QUERY_OUTPUT_PATH.equals(action)) {
            getOutputPath();
        } else if (ACTION_RESTART_WITH_NEW_FILE.equals(action)) {
            String path = intent.getStringExtra(EXTRA_RESTART_WITH_NEW_FILE);
            restartWithNewFile(path, intent);
        } else if (ACTION_RELEASE_RESOURCE.equals(action)) {
            releaseResource();
        }
    }

    private void updateStatus() {
        final boolean isRecording, isPausing;
        synchronized (sSync) {
            isRecording = (sMuxer != null);
            isPausing = isRecording ? sMuxer.isPaused() : false;
        }
        final Intent result = new Intent();
        result.setAction(ACTION_QUERY_STATUS_RESULT);
        result.putExtra(EXTRA_QUERY_RESULT_RECORDING, isRecording);
        result.putExtra(EXTRA_QUERY_RESULT_PAUSING, isPausing);
        if (DEBUG)
            Log.v(TAG, "sendBroadcast:isRecording=" + isRecording + ",isPausing=" + isPausing);
        sendBroadcast(result);
    }

    /**
     * start screen recording as .mp4 file
     *
     * @param intent
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScreenRecord(final Intent intent) {
        if (DEBUG) Log.v(TAG, "startScreenRecord:sMuxer=" + sMuxer);
        synchronized (sSync) {
            if (sMuxer == null) {
                final int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
                // get MediaProjection
                final MediaProjection projection = mMediaProjectionManager.getMediaProjection(resultCode, intent);
                if (projection != null) {
                    final DisplayMetrics metrics = getResources().getDisplayMetrics();
                    final int density = metrics.densityDpi;

                    sMediaProjection = projection;

                    if (DEBUG) Log.v(TAG, "startRecording:");
                    try {
                        sMuxer = new MediaMuxerWrapper(".mp4"); // if you record audio only, ".m4a" is also OK.

                        //change output path add in 2016.6.30 by KerriGan.
                        if (intent.getStringExtra(EXTRA_SET_OUTPUT_PATH) != null) {
                            sMuxer.setOutputPath(intent.getStringExtra(EXTRA_SET_OUTPUT_PATH), true);
                        }

                        RecorderFormat recorderFormat = null;
                        Parcelable parcelable = intent.getParcelableExtra(EXTRA_SET_RECORDER_FORMAT);
                        if (parcelable != null && parcelable instanceof RecorderFormat) {
                            recorderFormat = (RecorderFormat) parcelable;
                        }

                        if (recorderFormat == null || recorderFormat.isRecordVideo()) {
                            int width = metrics.widthPixels;
                            int height = metrics.heightPixels;
                            if (recorderFormat != null) {
                                if (recorderFormat.getVideoWidth() != -1) {
                                    width = recorderFormat.getVideoWidth();
                                }
                                if (recorderFormat.getVideoHeight() != -1) {
                                    height = recorderFormat.getVideoHeight();
                                }
                            }
                            // for screen capturing
                            new MediaScreenEncoder(sMuxer, mMediaEncoderListener,
                                    projection, width, height, density);
                        }
                        if (recorderFormat == null || recorderFormat.isRecordAudio()) {
                            // for audio capturing
                            new MediaAudioEncoder(sMuxer, mMediaEncoderListener);
                        }
                        sMuxer.prepare();
                        sMuxer.startRecording();
                    } catch (final IOException e) {
                        Log.e(TAG, "startScreenRecord:", e);
                    }
                }
            }
        }
    }

    /**
     * stop screen recording
     */
    private void stopScreenRecord() {
        if (DEBUG) Log.v(TAG, "stopScreenRecord:sMuxer=" + sMuxer);
        synchronized (sSync) {
            if (sMuxer != null) {
                sMuxer.stopRecording();
                sMuxer = null;
                // you should not wait here
            }
        }
    }

    private void pauseScreenRecord() {
        synchronized (sSync) {
            if (sMuxer != null) {
                sMuxer.pauseRecording();
            }
        }
    }

    private void resumeScreenRecord() {
        synchronized (sSync) {
            if (sMuxer != null) {
                sMuxer.resumeRecording();
            }
        }
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

    private void getOutputPath() {
        String path = sMuxer.getOutputPath();
        Intent i = new Intent(ACTION_QUERY_OUTPUT_PATH_RESULT);
        i.putExtra(EXTRA_QUERY_OUTPUT_PATH, path);
        sendBroadcast(i);
    }

    private void restartWithNewFile(String path, Intent intent) {
        try {
            sMuxer = new MediaMuxerWrapper(".mp4");
            sMuxer.setOutputPath(path, true);
            final DisplayMetrics metrics = getResources().getDisplayMetrics();
            final int density = metrics.densityDpi;

            RecorderFormat recorderFormat = null;
            Parcelable parcelable = intent.getParcelableExtra(EXTRA_SET_RECORDER_FORMAT);
            if (parcelable != null && parcelable instanceof RecorderFormat) {
                recorderFormat = (RecorderFormat) parcelable;
            }

            if (recorderFormat == null || recorderFormat.isRecordVideo()) {
                // for screen capturing
                new MediaScreenEncoder(sMuxer, mMediaEncoderListener,
                        sMediaProjection, metrics.widthPixels, metrics.heightPixels, density);
            }
            if (recorderFormat == null || recorderFormat.isRecordAudio()) {
                // for audio capturing
                new MediaAudioEncoder(sMuxer, mMediaEncoderListener);
            }
            sMuxer.prepare();
            sMuxer.startRecording();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void releaseResource() {
        if (sMediaProjection != null) {
            sMediaProjection.stop();
            sMediaProjection = null;
        }
    }
}
