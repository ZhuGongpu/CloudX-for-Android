package model.network;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import common.message.Data;
import data.information.GlobalSettingsAndInformation;
import utils.BitmapUtils;
import utils.ByteStringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.List;

/**
 * 用于处理PC发来的Video、Info数据
 * Created by Gongpu on 2014/3/30.
 */
public class MainInputThread extends Thread {
    private static final String TAG = "MainInputThread";
    public Handler handler = null;
    Bitmap bitmap = null;
    private boolean running = true;
    private Socket socket = null;
    private InputStream inputStream = null;

    public MainInputThread(Socket socket, Handler handler) {
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

                    if (dataPacket.hasVideo() && this.handler != null) {

                        //根据新的proto重写

                        Log.e(TAG, "video get");

                        //先解析是否包含完整的frame
                        if (dataPacket.getVideo().hasFrame()) {
                            //将frame中数据写入bitmap

                            bitmap = ByteStringUtils.ByteStringToBitmap(dataPacket.getVideo().getFrame());

                            Message message = this.handler.obtainMessage();
                            message.arg1 = GlobalSettingsAndInformation.MessageType_Bitmap;
                            message.obj = bitmap;
                            message.sendToTarget();

                            Log.e(TAG, "Frame get  " + (bitmap.getWidth()) + " " + (bitmap.getHeight()));

                            System.gc();
                        } else if (bitmap == null) {
                            //TODO 应该重新发起请求，向对方请求完整的frame

                            Log.e(TAG, "bitmap = null!!!!!!");
                        } else {

                            Canvas canvas = new Canvas(bitmap);
                            Paint paint = new Paint();

                            Log.e(TAG, "MoveRects : ");

                            //处理move rect
                            if (dataPacket.getVideo().getMoveRectsCount() > 0) {

                                List<Data.Video.MoveRectangle> moveRects = dataPacket.getVideo().getMoveRectsList();
                                for (Data.Video.MoveRectangle moveRect : moveRects) {

                                    //move rect 的 destination rectangle 中不包含image
                                    Data.Video.Rectangle destinationRect = moveRect.getDestinationRectangle();
                                    Data.Video.Point sourcePoint = moveRect.getSourcePoint();


                                    //将sourcePoint处destination rect大小的图像平移到destination rect
                                    if (destinationRect.getWidth() > 0 && destinationRect.getHeight() > 0) {
                                        canvas.drawBitmap(BitmapUtils.ExtractRect(bitmap,
                                                        sourcePoint.getX(), sourcePoint.getY(),
                                                        destinationRect.getWidth(), destinationRect.getHeight()),
                                                destinationRect.getX(), destinationRect.getY(),
                                                paint);

                                        Log.e(TAG, "DestRect : " + destinationRect.getX() + " " + destinationRect.getY() + " " + destinationRect.getWidth() + " " + destinationRect.getHeight());
                                        Log.e(TAG, "SrcPoint : " + sourcePoint.getY() + " " + sourcePoint.getY());
                                    }
                                }
                            }

                            //处理dirty rect
                            if (dataPacket.getVideo().getDirtyRectsCount() > 0) {
                                List<Data.Video.Rectangle> dirtyRects = dataPacket.getVideo().getDirtyRectsList();

                                //更新dirty rect
                                for (Data.Video.Rectangle dirtyRect : dirtyRects) {
                                    if (dirtyRect.getWidth() > 0 && dirtyRect.getHeight() > 0) {
                                        canvas.drawBitmap(ByteStringUtils.ByteStringToBitmap(dirtyRect.getImage()),
                                                dirtyRect.getX(), dirtyRect.getY(), paint);

                                        Log.e(TAG, "DirtyRect:" + (dirtyRect.getX()) + " " + (dirtyRect.getY()) + " " + (dirtyRect.getWidth()) + " " + dirtyRect.getHeight()
                                        );
                                    }
                                }
                            }

                            Message message = this.handler.obtainMessage();
                            message.arg1 = GlobalSettingsAndInformation.MessageType_Bitmap;
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
                }
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
