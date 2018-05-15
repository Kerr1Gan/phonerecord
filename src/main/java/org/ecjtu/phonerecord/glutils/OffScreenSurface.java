package org.ecjtu.phonerecord.glutils;

/**
 * Created by KerriGan on 2016/5/27.
 */
public class OffScreenSurface extends EglSurfaceBase {

    public OffScreenSurface(final EglCore eglBase, final int width, final int height) {
        super(eglBase);
        createOffscreenSurface(width, height);
        makeCurrent();
    }

    public void release() {
        releaseEglSurface();
    }

}
