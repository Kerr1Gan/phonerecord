// IRecordCallback.aidl
package org.ecjtu.phonerecord;

// Declare any non-default types here with import statements
import org.ecjtu.phonerecord.RecordStatus;
interface IRecordCallback {
    /**
     * Called when the service has a new value for you.
     */
    void onStateChanged(in RecordStatus recordStatus);
    void onRecordTimeChanged(long time);
    void onError(String msg);
}
