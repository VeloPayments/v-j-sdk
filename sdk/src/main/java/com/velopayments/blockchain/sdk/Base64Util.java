package com.velopayments.blockchain.sdk;

import java.nio.ByteBuffer;
import java.util.Base64;

public class Base64Util {

    public static String toBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String toBase64(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        return toBase64(byteBuffer.array());
    }

    public static byte[] fromBase64(String base64) {
        if (base64 == null) {
            return null;
        }
        return Base64.getDecoder().decode(base64);
    }

}
