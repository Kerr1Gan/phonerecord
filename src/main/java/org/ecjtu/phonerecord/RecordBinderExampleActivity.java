package org.ecjtu.phonerecord;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.ecjtu.phonerecord.service.ScreenRecorderBinderService;

/**
 * Created by xiang on 2018/5/19.
 */
@SuppressLint("NewApi")
public class RecordBinderExampleActivity extends Activity {

    private static final boolean DEBUG = false;
    private static final String TAG = "RecordBinderActivity";
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1;

    private IRecordService mRecordService;

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
        bindService(new Intent(this, ScreenRecorderBinderService.class), mConnection, Context.BIND_AUTO_CREATE);

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
                if (mRecordService == null) return;
                try {
                    RecordStatus status = mRecordService.getRecordStatus();
                    if (!status.isPausing()) {
                        mRecordService.pauseScreenRecord();
                    } else {
                        mRecordService.resumeScreenRecord();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRecordService == null) return;
                try {
                    Toast.makeText(RecordBinderExampleActivity.this, "output path " + mRecordService.getOutputPath(), Toast.LENGTH_SHORT).show();
                    mRecordService.stopScreenRecord();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateRecording(final boolean isRecording, final boolean isPausing) {
        if (DEBUG)
            Log.v(TAG, "updateRecording:isRecording=" + isRecording + ",isPausing=" + isPausing);
        mStartButton.setEnabled(!isRecording);
        mPauseButton.setEnabled(isRecording);
        mPauseButton.setText(isPausing ? R.string.resume : R.string.pause);
        mStopButton.setEnabled(isRecording);
    }

    @Override
    protected void onDestroy() {
        if(mRecordService!=null){
            try {
                mRecordService.stopScreenRecord();
                mRecordService.releaseResource();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
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
            try {
                mRecordService.startScreenRecordV1(data, resultCode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mRecordService = IRecordService.Stub.asInterface(service);
            try {
                mRecordService.registerCallback(new IRecordCallback.Stub() {
                    @Override
                    public void onStateChanged(RecordStatus recordStatus) throws RemoteException {
                        Log.i(TAG, "status recording " + recordStatus.isRecording() + " pausing " + recordStatus.isPausing());
                        updateRecording(recordStatus.isRecording(), recordStatus.isPausing());
                    }

                    @Override
                    public void onRecordTimeChanged(long time) throws RemoteException {

                    }
                });
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mRecordService = null;
        }
    };
}
