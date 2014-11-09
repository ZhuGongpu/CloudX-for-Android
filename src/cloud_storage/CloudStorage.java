package cloud_storage;

import android.content.Context;
import android.util.Log;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;

import java.io.File;

/**
 * Created by zhugongpu on 14/11/9.
 */
public class CloudStorage {

    private static final String TAG = "CloudStorage";

    private static String TOKEN = null;

    private static UploadManager uploadManager = new UploadManager();

    /**
     * 设置新的token
     *
     * @param newToken
     */
    public static void setToken(String newToken) {
        TOKEN = newToken;
    }

    /**
     * 上传文件
     *
     * @param filePath 文件的完整路径
     */
    public static void uploadFile(String filePath, UpCompletionHandler completed) {

        File file = new File(filePath);
        if (file.exists()) {
            Log.e(TAG, "Start");
            if (TOKEN != null)
                uploadManager.put(file, filePath, TOKEN, completed, null);
            else {
                //TODO 请求新的Token
                Log.e(TAG, "TOKEN is null");
            }
        } else
            Log.e(TAG, "File not exist");
    }


    /**
     * 从指定url中下载文件
     *
     * @param context
     * @param sourceURL       文件的源地址
     * @param destinationPath 保存文件的本机路径
     * @param callback
     * @param progressCallback
     */
    public static void downloadFile(Context context, String sourceURL, String destinationPath, FutureCallback<File> callback, ProgressCallback progressCallback) {
        Ion.with(context)
                .load(sourceURL)
                .progress(progressCallback)
//                        // have a ProgressBar get updated automatically with the percent
//                .progressBar(progressBar)
//                        // and a ProgressDialog
//                .progressDialog(progressDialog)
                        // can also use a custom callback
                .progress(new ProgressCallback() {
                    @Override
                    public void onProgress(long downloaded, long total) {
                        System.out.println("" + downloaded + " / " + total);
                    }
                })
                .write(new File(destinationPath))
                .setCallback(callback);
    }
}
