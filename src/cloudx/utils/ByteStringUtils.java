package cloudx.utils;

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

    public static ByteString stringToByteString(String string) {
        return ByteString.copyFromUtf8(string);
    }

    public static ByteString bytesToByteString(byte[] bytes) {
        return ByteString.copyFrom(bytes);
    }

    public static byte[] byteStringToBytes(ByteString byteString) {
        return byteString.toByteArray();
    }

    public static String byteStringToString(ByteString byteString)
    {
        return new String(byteString.toByteArray());
    }
}
