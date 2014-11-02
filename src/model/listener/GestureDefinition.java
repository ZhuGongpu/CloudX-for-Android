package model.listener;

import android.graphics.PointF;
import android.util.FloatMath;
import android.view.MotionEvent;

/**
 * Created by Me on 14-2-26.
 */
public class GestureDefinition {
    private static int MajorDistanceThreshold = 60;
    private static int SecondaryDistanceThreshold = 80;
    private static int ClickTimeThreshold = 100;

    public static boolean isPinchWithFiveFingers(MotionEvent event, PointF firstPoint) {
        for (int i = 1; i < 5; i++) {
            if (getDistance(new PointF(event.getX(i), event.getY(i)), firstPoint)
                    < MajorDistanceThreshold / 4) {
                return false;
            }
        }
        return true;
    }

    public static boolean isClick(MotionEvent event, PointF point) {
        return event.getActionMasked() == MotionEvent.ACTION_UP
                && event.getEventTime() - event.getDownTime() < ClickTimeThreshold
                && getDistance(point, new PointF(event.getX(), event.getY())) < MajorDistanceThreshold;
    }


    public static boolean isScroll(MotionEvent event, PointF startPoints[]) {
        if (event.getPointerCount() == 2) {
            if (Math.abs(
                    (event.getX(0) - event.getX(1))
                            - (startPoints[0].x - startPoints[1].x)
            )
                    > MajorDistanceThreshold / 2)
                return false;
        } else
            return false;
        return true;
    }

    public static boolean isSwipeLeftward(MotionEvent event, PointF points[]) {
        if (event.getPointerCount() >= 3) {
            for (int i = 0; i < 3; i++) {
                if (points[i].x - event.getX(i) < MajorDistanceThreshold ||
                        Math.abs(event.getY(i) - points[i].y) > SecondaryDistanceThreshold)
                    return false;
            }
            return true;
        } else
            return false;
    }

    public static boolean isSwipeRightward(MotionEvent event, PointF points[]) {
        if (event.getPointerCount() >= 3) {
            for (int i = 0; i < 3; i++) {
                if (event.getX(i) - points[i].x < MajorDistanceThreshold ||
                        Math.abs(event.getY(i) - points[i].y) > SecondaryDistanceThreshold)
                    return false;
            }
            return true;
        } else
            return false;
    }

    public static boolean isSlideDown(MotionEvent event, PointF points[]) {
        if (event.getPointerCount() >= 3) {
            for (int i = 0; i < 3; i++) {
                if (event.getY(i) - points[i].y < MajorDistanceThreshold ||
                        Math.abs(event.getX(i) - points[i].x) > SecondaryDistanceThreshold)
                    return false;
            }
            return true;
        } else
            return false;
    }

    public static boolean isSlideUp(MotionEvent event, PointF points[]) {
        if (event.getPointerCount() >= 3) {
            for (int i = 0; i < 3; i++) {
                if (points[i].y - event.getY(i) < MajorDistanceThreshold ||
                        Math.abs(event.getX(i) - points[i].x) > SecondaryDistanceThreshold) {
                    return false;
                }
            }
            return true;
        } else
            return false;
    }

    private static final float getDistance(PointF p1, PointF p2) {
        return FloatMath.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
    }


}
