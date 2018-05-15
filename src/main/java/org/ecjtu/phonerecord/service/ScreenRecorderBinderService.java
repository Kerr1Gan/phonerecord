package org.ecjtu.phonerecord.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import org.ecjtu.phonerecord.IAidlCallback;

/**
 * Created by Ethan_Xiang on 2018/5/15.
 */

public class ScreenRecorderBinderService extends Service {

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
    private RemoteCallbackList<IAidlCallback> mCallbacks = new RemoteCallbackList<>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void registerCallback(IAidlCallback callback) {
        if (callback != null) {
            mCallbacks.register(callback);
        }
    }

    public void unregisterCallback(IAidlCallback callback) {
        if (callback != null) {
            mCallbacks.unregister(callback);
        }
    }

    @Override
    public void onDestroy() {
        mCallbacks.kill();
        super.onDestroy();
    }
}
