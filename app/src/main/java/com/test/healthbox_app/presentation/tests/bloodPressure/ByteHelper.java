package com.test.healthbox_app.presentation.tests.bloodPressure;

/**
 * Created by Terry on 2016/7/20.
 */
public class ByteHelper {


    /**
     * 将byte 转成对应的 int 值
     * @param b
     * @return
     */
    public static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }


    /**
     * 将一个byte[] 数组中
     * @param arrayOfByte
     * @param start
     * @param len
     * @return
     */
    public static String bytesToHexString(byte[] arrayOfByte,int start,int len){
        if ((arrayOfByte == null) || (arrayOfByte.length <= 0))
            return null;
        if(start >= len) return null;
        StringBuilder localStringBuilder = new StringBuilder();
        for (int i = start; i < len; i++) {
            String str = Integer.toHexString(0xFF & arrayOfByte[i]);
            if (str.length() < 2)
                localStringBuilder.append(0);
            localStringBuilder.append(str.toUpperCase());
        }
        return localStringBuilder.toString();
    }



    public static String bytesToHexString(byte[] arrayOfByte){
        if(arrayOfByte == null) return null;
        return bytesToHexString(arrayOfByte,0,arrayOfByte.length);
    }

    public static String bytesToHexString1(byte[] paramArrayOfByte) {
        StringBuilder localStringBuilder = new StringBuilder();
        if ((paramArrayOfByte == null) || (paramArrayOfByte.length <= 0)) {
            return null;
        }
        for (int i = 0; i < paramArrayOfByte.length; i++) {
            String str = Integer.toHexString(0xFF & paramArrayOfByte[i]);
            if (str.length() < 2) {
                localStringBuilder.append(0);
            }
            localStringBuilder.append(str).append("");
        }
        return localStringBuilder.toString();
    }



    /**
     * 根据一个byte[] ，和它的长度，生成个Byte长度的校验码。
     *
     * @param Data
     *            byte数组
     * @param Len
     *            这个数组的长度
     * @return 校验码 byte
     */
    public static final byte checkSumOneByte(byte[] Data,int start, int Len) {
        byte Sum = 0;
        short CheckSum = 0;
        int end = start + Len;
        for (int i = start; i < end; i++) {
            CheckSum += (Data[i] & 0xFF) ;
        }
        Sum = (byte) ((CheckSum) & 0xFF);
        return Sum;
    }

}
