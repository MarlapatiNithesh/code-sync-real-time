package com.codesync.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressionUtils {

    public static byte[] compress(String str) {
        if (str == null || str.isEmpty()) {
            return new byte[0];
        }
        try (ByteArrayOutputStream obj = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(obj)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
            gzip.flush();
            gzip.close();
            return obj.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress string", e);
        }
    }

    public static String decompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to decompress string", e);
        }
    }
}
