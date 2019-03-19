package com.felhr.utils;

public class ByteOps {
    public ByteOps(){}

    public static byte[] byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null){
            return null;
        }
        byte[] hexArray = "0123456789ABCDEF".getBytes();
        byte[] hexChars = new byte[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return hexChars;
    }
}
