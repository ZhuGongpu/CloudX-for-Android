package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Created by Gongpu on 14-3-23.
 */
public class CompressionAndDecompressionUtils {
    public static byte[] GZipDecompress(byte[] compressedData) {
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
