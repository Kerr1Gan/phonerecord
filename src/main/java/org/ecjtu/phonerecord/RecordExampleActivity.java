package org.ecjtu.phonerecord;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import org.ecjtu.phonerecord.service.ScreenRecorderService;
import java.lang.ref.WeakReference;

/**
 * Created by Ethan_Xiang on 2018/5/15.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RecordExampleActivity extends Activity {

    private static final boolean DEBUG = false;
    private static final String TAG = "RecordExampleActivity";
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

    private RecordBroadcastReceiver mReceiver;

    private Button mStartButton;
    private Button mPauseButton;
    private Button mStopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_example);
        mStartButton = (Button) findViewById(R.id.btn_start);
        mPauseButton = (Button) findViewById(R.id.btn_pause);
        mStopButton = (Button) findViewById(R.id.btn_stop);
        updateRecording(false, false);
        if (mReceiver == null) {
            mReceiver = new RecordBroadcastReceiver(this);
        }
        registerButtonListener();
    }

    private void registerButtonListener() {
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final MediaProjectionManager manager
                        = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                final Intent permissionIntent = manager.createScreenCaptureIntent();
                startActivityForResult(permissionIntent, REQUEST_CODE_SCREEN_CAPTURE);
            }
        });

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Boolean isPause = false;
                if (v.getTag() != null) {
                    isPause = (Boolean) v.getTag();
                }
                if (!isPause) {
                    final Intent intent = new Intent(RecordExampleActivity.this, ScreenRecorderService.class);
                    intent.setAction(ScreenRecorderService.ACTION_PAUSE);
                    startService(intent);
                    v.setTag(true);
                } else {
                    final Intent intent = new Intent(RecordExampleActivity.this, ScreenRecorderService.class);
                    intent.setAction(ScreenRecorderService.ACTION_RESUME);
                    startService(intent);
                    v.setTag(false);
                }
                queryRecordingStatus();
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecordExampleActivity.this, ScreenRecorderService.class);
                intent.setAction(ScreenRecorderService.ACTION_QUERY_OUTPUT_PATH);
                startService(intent);

                intent = new Intent(RecordExampleActivity.this, ScreenRecorderService.class);
                intent.setAction(ScreenRecorderService.ACTION_STOP);
                startService(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) Log.v(TAG, "onResume:");
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_STATUS_RESULT);
        intentFilter.addAction(ScreenRecorderService.ACTION_QUERY_OUTPUT_PATH_RESULT);
        registerReceiver(mReceiver, intentFilter);
        queryRecordingStatus();
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.v(TAG, "onPause:");
        unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (DEBUG) Log.v(TAG, "onActivityResult:resultCode=" + resultCode + ",data=" + data);
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_CODE_SCREEN_CAPTURE == requestCode) {
            if (resultCode != Activity.RESULT_OK) {
                // when no permission
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                return;
            }
            mStartButton.setEnabled(false);
            startScreenRecorder(resultCode, data);
        }
    }

    private void queryRecordingStatus() {
        if (DEBUG) Log.v(TAG, "queryRecording:");
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_QUERY_STATUS);
        startService(intent);
    }

    private void startScreenRecorder(final int resultCode, final Intent data) {
        final Intent intent = new Intent(this, ScreenRecorderService.class);
        intent.setAction(ScreenRecorderService.ACTION_START);
        intent.putExtra(ScreenRecorderService.EXTRA_RESULT_CODE, resultCode);
        intent.putExtras(data);
        startService(intent);
    }

    private void updateRecording(final boolean isRecording, final boolean isPausing) {
        if (DEBUG)
            Log.v(TAG, "updateRecording:isRecording=" + isRecording + ",isPausing=" + isPausing);
        mStartButton.setEnabled(!isRecording);
        mPauseButton.setEnabled(isRecording);
        mPauseButton.setText(isPausing ? R.string.resume : R.string.pause);
        mStopButton.setEnabled(isRecording);
    }

    private static final class RecordBroadcastReceiver extends BroadcastReceiver {
        private final WeakReference<RecordExampleActivity> mWeakParent;

        public RecordBroadcastReceiver(final RecordExampleActivity parent) {
            mWeakParent = new WeakReference<>(parent);
        }

        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (DEBUG) Log.v(TAG, "onReceive:" + intent);
            final String action = intent.getAction();
            if (ScreenRecorderService.ACTION_QUERY_STATUS_RESULT.equals(action)) {
                final boolean isRecording = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_RECORDING, false);
                final boolean isPausing = intent.getBooleanExtra(ScreenRecorderService.EXTRA_QUERY_RESULT_PAUSING, false);
                final RecordExampleActivity parent = mWeakParent.get();
                if (parent != null) {
                    parent.updateRecording(isRecording, isPausing);
                }
            } else if (ScreenRecorderService.ACTION_QUERY_OUTPUT_PATH_RESULT.equals(action)) {
                String path = intent.getStringExtra(ScreenRecorderService.EXTRA_QUERY_OUTPUT_PATH);
                Toast.makeText(context, context.getString(R.string.video_path) + path, Toast.LENGTH_LONG).show();
            }
        }
    }
}
