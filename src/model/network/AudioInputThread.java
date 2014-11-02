package model.network;

import android.util.Log;
import common.message.Data;
import data.information.GlobalSettingsAndInformation;
import utils.AudioPlayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.zip.GZIPInputStream;

/**
 * Created by Gongpu on 14-3-16.
 */
public class AudioInputThread extends Thread {
    private String TAG = "AudioInputThread";

    @Override
    public void run() {

        Socket socket = null;
        try {
            AudioPlayer audioAudioPlayer = new AudioPlayer();

            Log.e(TAG, GlobalSettingsAndInformation.ServerIP);

            socket = new Socket(GlobalSettingsAndInformation.ServerIP,
                    GlobalSettingsAndInformation.ServerPortAvailable);

            Log.e(TAG, "Audio Socket Connected");

            InputStream inputStream = socket.getInputStream();

            while (!this.isInterrupted()) {
                try {
                    Data.DataPacket dataPacket = Data.DataPacket.parseDelimitedFrom(inputStream);

                    if (dataPacket != null && dataPacket.getDataPacketType() == Data.DataPacket.DataPacketType.Audio
                            && dataPacket
                            .hasAudio()) {
                        byte[] encodedAudio = GZipDecompress(dataPacket.getAudio().getSound().toByteArray());
                        audioAudioPlayer.play(encodedAudio);
                    }

                } catch (IOException e) {
                    e.printStackTrace();

                    audioAudioPlayer.stopPlaying();

                    inputStream.close();
                    socket.close();

                    socket = new Socket(GlobalSettingsAndInformation.ServerIP,
                            GlobalSettingsAndInformation.ServerPortAvailable);

                    inputStream = socket.getInputStream();

                    audioAudioPlayer = new AudioPlayer();

                    Log.e(TAG, "EXCEPTION");
                }
            }
            if (audioAudioPlayer != null)
                audioAudioPlayer.stopPlaying();


            if (inputStream != null)
                inputStream.close();
            if (socket != null)
                socket.close();


            Log.e(TAG, "AudioInputThread STOPPED");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null)
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private byte[] GZipDecompress(byte[] compressedData) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);

        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            gzipInputStream.close();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
