package model.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.google.protobuf.ByteString;
import common.message.Data;
import data.information.GlobalSettingsAndInformation;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Calendar;

/**
 * Created by Gongpu on 2014/3/30.
 */
public class MainPassiveInputThread extends Thread {
    private static final String TAG = "MainPassiveInputThread";
    public Handler handler = null;
    Bitmap bitmap = null;
    private boolean running = true;
    private Socket socket = null;
    private InputStream inputStream = null;
    private long fileReceivedSize = 0;
    private FileOutputStream fileOutputStream = null;

    public MainPassiveInputThread(Socket socket, Handler handler) {
        this.handler = handler;
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            //socket = new Socket(GlobalSettingsAndInformation.ServerIP, GlobalSettingsAndInformation.ServerPort);
            inputStream = socket.getInputStream();
            while (running && inputStream != null) {
                Data.DataPacket dataPacket = Data.DataPacket.parseDelimitedFrom(inputStream);

                if (dataPacket != null) {
//                    Log.e(TAG, "Receive the packet from " + (new String(dataPacket.getTimeStamp().toByteArray())));

                    if (dataPacket.hasVideo() && this.handler != null) {
                        if (dataPacket.getVideo().hasImage()) {

                            long startTime = Calendar.getInstance().getTimeInMillis();

                            ByteString compressedImage = dataPacket.getVideo().getImage();

                            Message message = this.handler.obtainMessage();
                            message.arg1 = GlobalSettingsAndInformation.MessageType_Bitmap;
                            //decompress
                            //todo changed
                            // byte[] decompressedImage = CompressionAndDecompressionUtils.GZipDecompress(compressedImage.toByteArray());
                            byte[] decompressedImage =
                                    compressedImage.toByteArray();
                            //CompressionAndDecompressionUtils.SnappyDecompress(compressedImage.toByteArray());


//                        if (bitmap != null && !bitmap.isRecycled())
//                            bitmap.recycle();

                            // Log.e(TAG, "  Array Length = " + decompressedImage.length);

                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decompressedImage);
                            bitmap = BitmapFactory.decodeStream(byteArrayInputStream, null, null);

                            long endTime = Calendar.getInstance().getTimeInMillis();

//                            Log.e(TAG, "DecodeBitmap : " + (endTime - startTime) + " ms");

//                        bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scaleRate),
//                                (int) (bitmap.getHeight() * scaleRate), true);
//                        bitmap = BitmapFactory.decodeByteArray(
//                                decompressedImage, 0, decompressedImage.length,
//                                options
//                        );

                            message.obj = bitmap;
                            message.sendToTarget();
                            System.gc();
                        }
                    } else if (dataPacket.hasInfo() && handler != null) {
                        Log.e(TAG, "receive info");
                        GlobalSettingsAndInformation.ServerResolutionWidth = dataPacket.getInfo().getWidth();
                        GlobalSettingsAndInformation.ServerResolutionHeight = dataPacket.getInfo().getHeight();
                        GlobalSettingsAndInformation.ServerPortAvailable = dataPacket.getInfo().getPortAvailable();
                    }

                    //todo move to active input thread
//                    else if (dataPacket.hasSharedMessage() && handler != null) {
//                        //send to main
//                        Message message = handler.obtainMessage();
//                        message.arg1 = GlobalSettingsAndInformation.MessageType_Message;
//                        message.obj = new String(dataPacket.getSharedMessage().getContent().toByteArray());
//                        message.sendToTarget();
//                    }
                }
//todo move to specified thread
//                else if (dataPacket.hasSharedFile() && handler != null) {
//                    //todo write to file
//                    if (fileOutputStream
//                            == null) {
//                        fileOutputStream = new FileOutputStream(
//                                Environment.getExternalStorageDirectory().getPath()
//                                        + "/" + new String(dataPacket.getSharedFile().getFileName().toByteArray())
//                        );
//                        fileReceivedSize = 0;
//                    }
//                    fileOutputStream.write(dataPacket.getSharedFile().getContent().toByteArray());
//                    fileOutputStream.flush();
//                    fileReceivedSize += dataPacket.getSharedFile().getContent().toByteArray().length;
//
//                    if (fileReceivedSize >= dataPacket.getSharedFile().getFileLength()) {
//                        fileOutputStream.close();
//                        fileOutputStream = null;
//                        fileReceivedSize = 0;
//                    }
//
//                    Message message = handler.obtainMessage();
//                    message.arg1 = GlobalSettingsAndInformation.MessageType_FileInfo;
//
//                    FileInfo fileInfo = new FileInfo();
//                    fileInfo.fileLength = dataPacket.getSharedFile().getFileLength();
//                    fileInfo.totalReceivedSize = fileReceivedSize;
//                    message.obj = fileInfo;
//                    message.sendToTarget();
//                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            running = false;

            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            if (socket != null)
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
        }
    }


    @Override
    public void interrupt() {
        super.interrupt();
        running = false;

        handler = null;

        if (inputStream != null)
            try {
                inputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        if (socket != null)
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
    }
}
