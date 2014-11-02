package utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

/**
 * Created by Gongpu on 2014/4/24.
 */
public class RingtoneUtils {

    private static MediaPlayer mediaPlayer = null;


    private static Ringtone ringtone = null;

    /**
     * 获取铃声的Uri
     *
     * @param context
     * @param type    通知声音: RingtoneManager.TYPE_NOTIFICATION; 警告:RingtoneManager.TYPE_ALARM; 铃声:RingtoneManager.TYPE_RINGTONE
     * @return
     */
    private static Uri getDefaultRingtoneUri(Context context, int type) {
        return RingtoneManager.getActualDefaultRingtoneUri(context, type);
    }

    public static void playRingtone(Context context, int type) {
//        mediaPlayer = MediaPlayer.create(context, getDefaultRingtoneUri(context, type));
//        mediaPlayer.setLooping(true);
//        mediaPlayer.setVolume(100, 100);
//        mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
//
//        mediaPlayer.start();

        ringtone = getDefaultRingtone(context, type);
        ringtone.setStreamType(AudioManager.STREAM_RING);
        ringtone.play();

    }

    public static void stopRingtone() {
//        if (mediaPlayer != null) {
//            if (mediaPlayer.isPlaying())
//                mediaPlayer.stop();
//            mediaPlayer.release();
//
//        }

        if (ringtone != null && ringtone.isPlaying())
            ringtone.stop();
    }

    private static Ringtone getDefaultRingtone(Context context, int type) {
        return RingtoneManager.getRingtone(context, getDefaultRingtoneUri(context, type));
    }
}
