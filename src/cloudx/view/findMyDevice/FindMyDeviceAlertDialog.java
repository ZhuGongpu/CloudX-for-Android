package cloudx.view.findMyDevice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.os.Vibrator;
import cloudx.main.R;
import utils.RingtoneUtils;

/**
 * Created by Gongpu on 2014/4/24.
 */
public class FindMyDeviceAlertDialog {

    private static final String TAG = "FindMyDevice";

    public FindMyDeviceAlertDialog(Context context) {

        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        final Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        vibrator.vibrate(999999999900000L);

        final int audioType = AudioManager.STREAM_RING;

        final int currentVolume = audioManager.getStreamVolume(audioType);
        audioManager.setStreamVolume(audioType, 100, AudioManager.FLAG_ALLOW_RINGER_MODES);
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);

        RingtoneUtils.playRingtone(context, RingtoneManager.TYPE_RINGTONE);

        new AlertDialog.Builder(context).setTitle(R.string.FindMyDevice)
                .setNegativeButton(R.string.FindMyDevice_GotIt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RingtoneUtils.stopRingtone();
                        vibrator.cancel();
                        audioManager.setStreamVolume(audioType, currentVolume, AudioManager.FLAG_ALLOW_RINGER_MODES);
                    }
                })
                .create()
                .show();
    }

}
