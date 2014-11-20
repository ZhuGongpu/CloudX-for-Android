package cloudx.receive_file;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import cloudx.main.R;
import cloudx.view.CircleProgressBar.CircleProgressBar;
import data.information.FileInfo;
import data.information.Constants;
import cloudx.network.ListeningThread;

/**
 * Created by Gongpu on 2014/4/22.
 */
public class ReceivingFileActivity extends Activity {
    private static final String TAG = "ReceivingFileActivity";
    private static final int MaxProgress = 100;
    private CircleProgressBar progressBar = null;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.e(TAG, "get a message");
            if (msg.arg1 == Constants.MessageType_FileInfo) {

                Log.e(TAG, "File in transmission");

                FileInfo info = (FileInfo) msg.obj;
                if (progressBar != null) {
                    if (info != null) {
                        if (info.totalReceivedSize >= info.fileLength) {
                            Toast.makeText(ReceivingFileActivity.this,
                                    getText(R.string.FileTransmissionDone), Toast.LENGTH_LONG).show();

                            Log.e(TAG, "File transmission done");

                        } else
                            progressBar.setProgress((int)
                                    (((double) info.totalReceivedSize / (double) info.fileLength) * 100));
                    }
                }
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receive_file_layout);
        progressBar = (CircleProgressBar) findViewById(R.id.progressBar);
        progressBar.setMaxProgress(MaxProgress);

        ListeningThread.getInstance().setHandler(handler);

        Log.e(TAG, "handler set");
    }
}
