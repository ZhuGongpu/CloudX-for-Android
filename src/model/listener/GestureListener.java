package model.listener;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Message;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import common.message.Data;
import common.message.LocalCommand;
import data.information.Constants;

/**
 * Created by Me on 14-2-19.
 */
public class GestureListener implements View.OnTouchListener {

    private static final String TAG = "GestureListener";

    private boolean isGestureListenerEnabled = true;
    private boolean isScreenLocked = false;

    private Matrix previousMatrix = new Matrix();
    private Matrix currentMatrix = new Matrix();
    private PointF[] startPoints = new PointF[10];
    private PointF[] lastPointsForScroll = new PointF[10];
    private int gestureType;//根据手指数量定义手势类型
    private float[] values = new float[9];//用于存放currentMatrix的值
    private boolean handled = false;

    private boolean isLongClickFlag = false;//在判定longClick过程中用来做标记,
    private boolean isLongClick = false;//最终判定是否为longClick

    private OnLongClickGestureListener onLongClickGestureListener = null;
    private CommandSender commandSender = null;

    private int peerScreen_Width, peerScreen_Height;

    public GestureListener(int peerScreen_Width, int peerScreen_Height) {
        this.peerScreen_Height = peerScreen_Height;
        this.peerScreen_Width = peerScreen_Width;
    }

    private static float getDistance(PointF p1, PointF p2) {
        return FloatMath.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }

    private static PointF getMidPoint(MotionEvent event, PointF startPoints[]) {
        int counter = event.getPointerCount();
        float x = 0;
        float y = 0;

        for (int i = 0; i < counter; i++) {
            x += event.getX(i) + startPoints[i].x;
            y += event.getY(i) + startPoints[i].y;
        }

        counter *= 2;
        return new PointF(x / counter, y / counter);
    }

    private static PointF getMidPoint(MotionEvent event) {
        float x = 0;
        float y = 0;
        int counter = event.getPointerCount();

        for (int i = 0; i < counter; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }

        return new PointF(x / counter, y / counter);
    }

    public void setScreenLocked(boolean screenLocked) {
        this.isScreenLocked = screenLocked;
    }

    public void setOnLongClickGestureListener(OnLongClickGestureListener onLongClickGestureListener) {
        this.onLongClickGestureListener = onLongClickGestureListener;
    }

    public void setGestureListenerEnabled(boolean isGestureListenerEnabled) {
        this.isGestureListenerEnabled = isGestureListenerEnabled;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startPoints[0] = new PointF(event.getX(), event.getY());
                lastPointsForScroll[0] = new PointF(event.getX(), event.getY());
                previousMatrix.set(currentMatrix);
                gestureType = 1;
                handled = false;

                isLongClickFlag = true;
                isLongClick = false;

                Log.d(TAG, "Action_Down");
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                //todo WARNING: some exceptions may occur
                if (gestureType < 5 && event.getEventTime() - event.getDownTime() < 80) {
                    gestureType++;
                    startPoints[gestureType - 1] = new PointF(event.getX(gestureType - 1), event.getY(gestureType - 1));
                    lastPointsForScroll[gestureType - 1] = new PointF(event.getX(gestureType - 1), event.getY(gestureType - 1));

                    Log.d(TAG, "ACTION_POINTER_DOWN");

                }
                isLongClickFlag = false;
                isLongClick = false;
                break;
            case MotionEvent.ACTION_MOVE:
                movementHandler(view, event);
                break;
            case MotionEvent.ACTION_UP:
                if (GestureDefinition.isClick(event, startPoints[0])) {
                    if (gestureType == 1)
                        leftClickHandler(event, (ImageView) view);
                    else if (gestureType == 2) {
                        rightClickHandler(event, (ImageView) view);
                    }
                }
                handled = true;
                gestureType = 0;
                Log.d(TAG, "Action_Up");
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * 核心
     */
    private void movementHandler(View view, MotionEvent event) {
        switch (gestureType) {
            case 5:
                if (!handled && isGestureListenerEnabled) {
                    pinchWithFiveFingersHandler(event);
                }
                break;
            case 4:
            case 3:
                if (!handled) {
                    handled = true;
                    if (GestureDefinition.isSlideUp(event, startPoints) && isGestureListenerEnabled) {
                        slideUpHandler(event);
                    } else if (GestureDefinition.isSlideDown(event, startPoints) && isGestureListenerEnabled) {
                        slideDownHandler(event);
                    } else if (GestureDefinition.isSwipeRightward(event, startPoints)) {
                        slideRightHandler();
                    } else if (GestureDefinition.isSwipeLeftward(event, startPoints)) {
                        slideLeftHandler();
                    } else
                        handled = false;
                }
                break;
            case 2:
                if (isGestureListenerEnabled)
                    if (GestureDefinition.isScroll(event, startPoints)) { //todo isScroll or isScroll
                        scrollGestureHandler(event);
                    } else if (!isScreenLocked)
                        scaleGestureHandler(view, event);
                break;
            case 1:
                //对longClick的判定
                isLongClickFlag = isLongClickFlag && getDistance(new PointF(event.getX(), event.getY()),
                        lastPointsForScroll[0]) < 30;

                isLongClick = isLongClickFlag && (event.getEventTime() - event.getDownTime() > 800);

                if (isLongClick && !handled) {
                    Log.d(TAG, "Long Click");
                    onLongClickGestureListener.onLongClick(event);
                    handled = true;
                } else if (isGestureListenerEnabled && !isScreenLocked)
                    dragGestureHandler(view, event);
                break;
            default:
                break;
        }
    }

    protected void slideUpHandler(MotionEvent event) {
        PointF midPoint = getMidPoint(event, startPoints);
        commandSender.sendCommand(Data.Command.CommandType.ShutDownApp, midPoint.x, midPoint.y);

        Log.d(TAG, "UP");
    }

    protected void slideDownHandler(MotionEvent event) {
        PointF midPoint = getMidPoint(event, startPoints);
        commandSender.sendCommand(Data.Command.CommandType.Minimize, midPoint.x, midPoint.y);

        Log.d(TAG, "DOWN");
    }

    protected void slideLeftHandler() {
//        Message message = handler.obtainMessage();
//        message.arg1 = LocalCommand.Hide_OptionMenu;
//        message.sendToTarget();

        Log.d(TAG, "Swipe LEFT");
    }

    protected void slideRightHandler() {
//        Message message = handler.obtainMessage();
//        message.arg1 = LocalCommand.Show_OptionMenu;
//        message.sendToTarget();

        Log.d(TAG, "Swipe RIGHT");
    }

    protected void pinchWithFiveFingersHandler(MotionEvent event) {
        if (event.getPointerCount() >= 5) {//防止有手指抬起
            if (GestureDefinition.isPinchWithFiveFingers(event, startPoints[0])) {
                handled = true;

                PointF midPoint = getMidPoint(event);
                commandSender.sendCommand(Data.Command.CommandType.ShowDesktop, midPoint.x, midPoint.y);

                Log.d(TAG, "PINCH_IN_5");
            }
        }
    }

    protected void dragGestureHandler(View view, MotionEvent event) {
        float dx = event.getX() - startPoints[0].x;
        float dy = event.getY() - startPoints[0].y;
        currentMatrix.set(previousMatrix);
        currentMatrix.postTranslate(dx, dy);

        Log.d(TAG, "drag");

        ((ImageView) (view)).setImageMatrix(currentMatrix);
    }

    protected void leftClickHandler(MotionEvent event, ImageView view) {

        float[] coordinate = new float[]{event.getX(), event.getY()};

        Matrix matrix = new Matrix();

        if (view.getImageMatrix() != null && view.getDrawable() != null) {
            view.getImageMatrix().invert(matrix);

            matrix.mapPoints(coordinate);

            int width = view.getDrawable().getBounds().width();
            int height = view.getDrawable().getBounds().height();


            float x = coordinate[0] / width * peerScreen_Width;
            float y = coordinate[1] / height * peerScreen_Height;

            commandSender.sendCommand(Data.Command.CommandType.LeftClick, x, y);

            Log.d(TAG, "Left Click : " + x + "," + y);
        }
    }

    protected void rightClickHandler(MotionEvent event, ImageView view) {

        float[] coordinate = new float[]{event.getX(), event.getY()};

        Matrix matrix = new Matrix();
        view.getImageMatrix().invert(matrix);

        if (matrix != null) {
            matrix.mapPoints(coordinate);

            int width = view.getDrawable().getBounds().width();
            int height = view.getDrawable().getBounds().height();


            float x = coordinate[0] / width * peerScreen_Width;
            float y = coordinate[1] / height * peerScreen_Height;

            commandSender.sendCommand(Data.Command.CommandType.RightClick, x, y);

            Log.d(TAG, "Right Click : " + x + "," + y);
        }
    }

    protected void scrollGestureHandler(MotionEvent event) {

        float distanceY = ((event.getY() - lastPointsForScroll[0].y) + (event.getY(1) - lastPointsForScroll[1].y)) / 2;
        lastPointsForScroll[0].x = event.getX();
        lastPointsForScroll[0].y = event.getY();

        lastPointsForScroll[1].x = event.getX(1);
        lastPointsForScroll[1].y = event.getY(1);

        commandSender.sendCommand(Data.Command.CommandType.Scroll, 0, distanceY);
    }

    protected void scaleGestureHandler(View view, MotionEvent event) {

        if (event.getPointerCount() >= 2) {
            float currentDistance = getDistance(
                    new PointF(event.getX(1), event.getY(1)),
                    new PointF(event.getX(0), event.getY(0))
            );
            float primitiveDistance = getDistance(startPoints[0], startPoints[1]);
            float scaleRate = currentDistance / primitiveDistance;

            currentMatrix.getValues(values);

            if ((scaleRate < 0.95f && scaleRate > 0.15f && values[0] > 0.25f) || (scaleRate > 1.05f && values[0] < 3f)) {
                currentMatrix.set(previousMatrix);
                currentMatrix.postScale(scaleRate, scaleRate,
                        (startPoints[1].x + startPoints[0].x) / 2,
                        (startPoints[1].y + startPoints[0].y) / 2);

                Log.d(TAG, "scale  " + "\t" + scaleRate + "\t" + "\n" + currentMatrix);

                ((ImageView) (view)).setImageMatrix(currentMatrix);
            }
        }
    }

    public void setCommandSender(CommandSender commandSender) {
        this.commandSender = commandSender;
    }


}
