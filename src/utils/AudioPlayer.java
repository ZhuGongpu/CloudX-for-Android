package utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

public class AudioPlayer {

    private int sampleRateInHz = 44100;
    private int inChannelConfig = AudioFormat.CHANNEL_IN_STEREO;
    private int outChannelConfig = AudioFormat.CHANNEL_OUT_STEREO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize;
    private short[] buffer;

    private AudioTrack audioTrack = null;

    public AudioPlayer() {
        int audioRecordBufferSize = AudioRecord.getMinBufferSize(
                sampleRateInHz, inChannelConfig, audioFormat);
        int audioTrackBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz,
                outChannelConfig, audioFormat);

        bufferSize = Math.max(audioRecordBufferSize, audioTrackBufferSize);
        System.out.println("Player : audioRecordBufferSize " + audioRecordBufferSize);
        System.out.println("Player : audioTrackBufferSize " + audioTrackBufferSize);
        System.out.println("Player : bufferSize " + bufferSize);

        buffer = new short[bufferSize];

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRateInHz, outChannelConfig, audioFormat, bufferSize,
                AudioTrack.MODE_STREAM);

        if (audioTrack != null)
            System.out.println("audioTrack inited");

        try {
            audioTrack.play();
            System.out.println("audioTrack.play()");
        } catch (Exception e) {
            System.out.println("failed to audioTrack.play()");
            e.printStackTrace();
        }
    }

    public void play(byte[] data) {
        audioTrack.write(data, 0, data.length);
    }

    public void stopPlaying() {
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;

    }

}
