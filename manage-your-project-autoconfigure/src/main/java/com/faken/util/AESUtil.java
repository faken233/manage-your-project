package com.faken.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AESUtil {
    
    //生成AES密钥，然后Base64编码
    public static String genKeyAES() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey key = keyGen.generateKey();
        return byte2Base64(key.getEncoded());
    }

    //将Base64编码后的AES密钥转换成SecretKey对象
    public static SecretKey loadKeyAES(String base64Key) throws UnsupportedEncodingException {
        return new SecretKeySpec(base64Key.getBytes("UTF-8"), "AES");
    }


    //字节数组换Base64编码
    public static String byte2Base64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    //Base64编码转字节数组
    public static byte[] base642Byte(String base64Key){
        return Base64.getDecoder().decode(base64Key);
    }

    //加密
    public static byte[] encryptAES(byte[] source, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(source);
    }

    //解密
    public static byte[] decryptAES(byte[] source,SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(source);
    }

}
