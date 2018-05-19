// IRecordService.aidl
package org.ecjtu.phonerecord;
// Declare any non-default types here with import statements
import org.ecjtu.phonerecord.RecordStatus;
import org.ecjtu.phonerecord.RecordFormat;
import android.content.Intent;
import org.ecjtu.phonerecord.IRecordCallback;
/**
* in 表示客户端向服务端传递数据，当服务端的数据发生改变，不会影响到客户端
* out表示服务端对数据修改，客户端会同步变动
* inout表示客户端和服务端的数据总是同步的
*/
interface IRecordService {
    void startScreenRecordV2(in Intent intent,int resultCode,String outputPath,in RecordFormat recordFormat);
    void startScreenRecordV1(in Intent intent,int resultCode);
    RecordStatus getRecordStatus();
    void stopScreenRecord();
    void pauseScreenRecord();
    void resumeScreenRecord();
    void releaseResource();
    int getPid();
    String getOutputPath();
    void registerCallback(IRecordCallback callback);
    void unregisterCallback(IRecordCallback callback);
}
