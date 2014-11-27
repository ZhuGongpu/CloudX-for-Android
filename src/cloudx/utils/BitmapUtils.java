package cloudx.utils;

import android.graphics.Bitmap;

/**
 * Created by zhugongpu on 14/11/4.
 */
public class BitmapUtils {
    /**
     * 提取指定矩形内的Bitmap
     *
     * @param sourceBitmap
     * @param x            矩形的left
     * @param y            矩形的right
     * @param width
     * @param height
     * @return
     */
    public static Bitmap ExtractRect(Bitmap sourceBitmap, int x, int y, int width, int height) {
        int[] pixels = new int[width * height];
        sourceBitmap.getPixels(pixels, 0, width, x, y, width, height);
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565);//TODO 格式不确定
    }
}
