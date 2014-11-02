package model.network;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import common.message.Data;
import data.information.FileInfo;
import data.information.GlobalSettingsAndInformation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gongpu on 2014/4/22.
 */
public class ActiveInputThread extends Thread {
    private static final String TAG = "ActiveInputThread";
    private static ActiveInputThread instance = new ActiveInputThread();
    private static boolean isRunning = false;
    List<ProcessingThread> subThreads = new ArrayList<ProcessingThread>();
    private Handler handler = null;
    private ServerSocket serverSocket = null;


    private ActiveInputThread() {
    }

    public static ActiveInputThread getInstance() {
        if (instance == null)
            instance = new ActiveInputThread();
        return instance;
    }


    public void setHandler(Handler handler) {
        this.handler = handler;
    }


    @Override
    public void interrupt() {
        super.interrupt();
        handler = null;
        isRunning = false;
        instance = null;
    }

    @Override
    public synchronized void start() {
        if (!isRunning)
            super.start();
    }

    @Override
    public void run() {
        super.run();
        isRunning = true;

        try {
            serverSocket = new ServerSocket(50323);

            while (isRunning) {
                Socket socket = serverSocket.accept();

                Log.e(TAG, "Accepted");

                ProcessingThread processingThread = new ProcessingThread(socket);
                processingThread.start();
                subThreads.add(processingThread);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (ProcessingThread thread : subThreads) {
            thread.interrupt();
        }
    }

    private class ProcessingThread extends Thread {

        private Socket socket = null;

        private FileOutputStream fileOutputStream = null;
        private long fileReceivedSize = 0;


        ProcessingThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();
            try {

                Log.e(TAG, "run");

//                boolean flag = false;//用于标记在context不是receivingFileActivity时是否发送过消息

                InputStream inputStream = socket.getInputStream();

                boolean uninitialized = true;
                String filePath = null;

                while (!this.isInterrupted() && !socket.isClosed()) {
                    Data.DataPacket dataPacket = Data.DataPacket.parseDelimitedFrom(inputStream);

                    if (dataPacket != null) {
                        Log.e(TAG, "收到 " + dataPacket.getDataPacketType());

//                        Log.e(TAG, "HasFile " + dataPacket.hasSharedFile());
                        if (dataPacket.hasSharedFile()) {
                            if (uninitialized) {

                                filePath = Environment.getExternalStorageDirectory().getPath()
                                        + "/" + new String(dataPacket.getSharedFile().getFileName().toByteArray());
                                fileOutputStream = new FileOutputStream(
                                        filePath
                                );

                                Log.e(TAG, "Location : " + filePath);

                                fileReceivedSize = 0;

                                uninitialized = false;
                            }
                            fileOutputStream.write(dataPacket.getSharedFile().getContent().toByteArray());
                            fileOutputStream.flush();
                            fileReceivedSize += dataPacket.getSharedFile().getContent().toByteArray().length;

                            Log.e(TAG, "Received Size = " + fileReceivedSize + "  FileSize = " + dataPacket.getSharedFile().getFileLength());

                            if (handler != null) {
                                FileInfo fileInfo = new FileInfo();
                                fileInfo.fileLength = dataPacket.getSharedFile().getFileLength();
                                fileInfo.totalReceivedSize = fileReceivedSize;
                                fileInfo.filePath = filePath;

                                Message message = handler.obtainMessage();
                                message.arg1 = GlobalSettingsAndInformation.MessageType_FileInfo;
                                message.obj = fileInfo;

                                Log.e(TAG, "Message send to target");

                                message.sendToTarget();

                            }

                            if (fileReceivedSize >= dataPacket.getSharedFile().getFileLength()) {
                                fileOutputStream.close();
                                fileOutputStream = null;
                                fileReceivedSize = 0;
                                break;
                            }
                        } else if (dataPacket.getDataPacketType().equals(Data.DataPacket.DataPacketType.FindMyPhone) &&
                                handler != null) {
                            // find my phone
                            Message message = handler.obtainMessage();
                            message.arg1 = GlobalSettingsAndInformation.MessageType_FindMyDevice;
                            message.sendToTarget();
                        } else if (dataPacket.hasSharedMessage() && handler != null) {
                            //send to main
                            Message message = handler.obtainMessage();
                            message.arg1 = GlobalSettingsAndInformation.MessageType_Message;
                            message.obj = new String(dataPacket.getSharedMessage().getContent().toByteArray());
                            message.sendToTarget();
                        }
                    } else break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            subThreads.remove(this);
        }
    }
}
