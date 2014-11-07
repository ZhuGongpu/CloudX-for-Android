package utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.protobuf.ByteString;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by zhugongpu on 14/11/4.
 */
public class ByteStringUtils {
    public static Bitmap ByteStringToBitmap(ByteString bytes) throws IOException {
        Bitmap bitmap;

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes.toByteArray());

        bitmap = BitmapFactory.decodeStream(inputStream).copy(Bitmap.Config.ARGB_8888, true);
        inputStream.close();
        return bitmap;
    }
}
