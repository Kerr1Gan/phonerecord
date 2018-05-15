package org.ecjtu.phonerecord.media;


import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import org.ecjtu.phonerecord.glutils.WindowSurface;

import java.io.IOException;

import org.ecjtu.phonerecord.glutils.EglTask;
import org.ecjtu.phonerecord.glutils.FullFrameRect;
import org.ecjtu.phonerecord.glutils.Texture2dProgram;


/**
 * Created by KerriGan on 2016/5/27.
 */
public class MediaScreenEncoder extends MediaVideoEncoderBase {
    private static final boolean DEBUG = false; // TODO set false on release
    private static final String TAG = "MediaScreenEncoder";

    private static final String MIME_TYPE = "video/avc";
    // parameters for recording
    private static final int FRAME_RATE = 25;

    private MediaProjection mMediaProjection;
    private final int mDensity;
    private Surface mSurface;
    private final Handler mHandler;

    public MediaScreenEncoder(final MediaMuxerWrapper muxer, final MediaEncoder.MediaEncoderListener listener,
                              final MediaProjection projection, final int width, final int height, final int density) {

        super(muxer, listener, width, height);
        mMediaProjection = projection;
        mDensity = density;
        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new Handler(thread.getLooper());
    }

    @Override
    protected void release() {
        mHandler.getLooper().quit();
        super.release();
    }

    @Override
    void prepare() throws IOException {
        if (DEBUG) Log.i(TAG, "prepare: ");
        mSurface = prepare_surface_encoder(MIME_TYPE, FRAME_RATE);
        mMediaCodec.start();
        mIsCapturing = true;
        new Thread(mScreenCaptureTask, "ScreenCaptureThread").start();
        if (DEBUG) Log.i(TAG, "prepare finishing");
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (final Exception e) {
                Log.e(TAG, "prepare:", e);
            }
        }
    }

    @Override
    void stopRecording() {
        if (DEBUG) Log.v(TAG,  "stopRecording:");
        synchronized (mSync) {
            mIsCapturing = false;
            mSync.notifyAll();
        }
        super.stopRecording();
    }

    private boolean requestDraw;
    private final DrawTask mScreenCaptureTask = new DrawTask(null, 0);

    private final class DrawTask extends EglTask {
        private VirtualDisplay display;
        private long intervals;
        private int mTexId;
        private SurfaceTexture mSourceTexture;
        private Surface mSourceSurface;
        private WindowSurface mEncoderSurface;
        private FullFrameRect mDrawer;
        private final float[] mTexMatrix = new float[16];

        public DrawTask(final EGLContext shared_context, final int flags) {
            super(shared_context, flags);
        }

        @Override
        protected void onStart() {
            if (DEBUG) Log.d(TAG,"mScreenCaptureTask#onStart:");
            mDrawer = new FullFrameRect(new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT));
            mTexId = mDrawer.createTextureObject();
            mSourceTexture = new SurfaceTexture(mTexId);
            mSourceTexture.setDefaultBufferSize(mWidth, mHeight);
            mSourceSurface = new Surface(mSourceTexture);
            mSourceTexture.setOnFrameAvailableListener(mOnFrameAvailableListener, mHandler);
            mEncoderSurface = new WindowSurface(getEglCore(), mSurface);

            if (DEBUG) Log.d(TAG,"setup VirtualDisplay");
            intervals = (long)(1000f / FRAME_RATE);
            display = mMediaProjection.createVirtualDisplay(
                    "Capturing Display",
                    mWidth, mHeight, mDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mSourceSurface, null, null);
            if (DEBUG) Log.v(TAG,  "screen capture loop:display=" + display);
            queueEvent(mDrawTask);
        }

        @Override
        protected void onStop() {
            if (mDrawer != null) {
                mDrawer.release();
                mDrawer = null;
            }
            if (mSourceSurface != null) {
                mSourceSurface.release();
                mSourceSurface = null;
            }
            if (mSourceTexture != null) {
                mSourceTexture.release();
                mSourceTexture = null;
            }
            if (mEncoderSurface != null) {
                mEncoderSurface.release();
                mEncoderSurface = null;
            }
            makeCurrent();
            if (DEBUG) Log.v(TAG, "mScreenCaptureTask#onStop:");
            if (display != null) {
                if (DEBUG) Log.v(TAG,  "release VirtualDisplay");
                display.release();
            }
            if (DEBUG) Log.v(TAG,  "tear down MediaProjection");
            if (mMediaProjection != null) {
                //We don't release mMediaProjection in there.
                //mMediaProjection.stop();
                //mMediaProjection = null;
            }
        }

        @Override
        protected boolean onError(final Exception e) {
            if (DEBUG) Log.w(TAG, "mScreenCaptureTask:", e);
            return false;
        }

        @Override
        protected boolean processRequest(final int request, final int arg1, final Object arg2) {
            return false;
        }

        private final SurfaceTexture.OnFrameAvailableListener mOnFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(final SurfaceTexture surfaceTexture) {
                if (mIsCapturing) {
                    synchronized (mSync) {
                        requestDraw = true;
                        mSync.notifyAll();
                    }
                }
            }
        };

        private final Runnable mDrawTask = new Runnable() {
            @Override
            public void run() {
                boolean localRequestPause;
                boolean localRequestDraw;

                synchronized (mSync) {
                    localRequestPause = mRequestPause;
                    localRequestDraw = requestDraw;

                    if (!requestDraw) {
                        try {
                            mSync.wait(intervals);
                            localRequestPause = mRequestPause;
                            localRequestDraw = requestDraw;
                            requestDraw = false;
                        } catch (final InterruptedException e) {
                            return;
                        }
                    }
                }
                if (mIsCapturing) {
                    if (localRequestDraw) {
                        mSourceTexture.updateTexImage();
                        mSourceTexture.getTransformMatrix(mTexMatrix);
                    }
                    if (!localRequestPause) {
                        mEncoderSurface.makeCurrent();
                        mDrawer.drawFrame(mTexId, mTexMatrix);
                        mEncoderSurface.swapBuffers();
                    }
                    makeCurrent();
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                    GLES20.glFlush();
                    frameAvailableSoon();
                    queueEvent(this);
                } else {
                    releaseSelf();
                }
            }
        };
    }
}
