// IAidlCallback.aidl
package org.ecjtu.phonerecord;

// Declare any non-default types here with import statements

interface IAidlCallback {
    /**
     * Called when the service has a new value for you.
     */
    void onStateChanged(int state);
    void onError(int error);
    void onInfo(int info);
}
