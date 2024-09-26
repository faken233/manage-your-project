package com.faken.util;



import javax.crypto.SecretKey;
import java.security.PublicKey;

public class ClientEncryptUtils {

    public static final String publicKeyStr = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgTh8XPOs5hCMlpHFiJ4D3XvMFjGVlPE7fMC4IHjhVqWfcyJDtDRrfy1Uey7Qk+uzYfKfMRy/pkT1ARrz3UQc5IolD4nl3CH2Ag2a4MO7TG8dMqRL3vvwXVVH8OrwdXXIUEemo2dSApWxjVKxtsTGIGNt9i9UEjVBx9NpNu4w0yizeDAnkqalFD+lNZJnMw0v7wP2p8cHZ8iqxvbQ9W2i/WyL0E06vOtm/LUgVSMuz6r0H2FOiBvzSjaa6bHYJlx16AR2vLO7gJSOLDkCOye6oyA7b1ZOd510LH1VBsYPGbT9LOZU1lhFi9cGHFdC3IFBnPFloLlvwJvrzjNxCKUzwwIDAQAB";
    public static final String aesKeyStr = "Ky01tCGl8Zvh3oVlQFapMw==";

    /**
     * 加密操作
     * @param content
     * @return
     * @throws Exception
     */
    public static String encryptText(String content) throws Exception {
        //客户端
//        String message = "Hello GQ";
        //将Base64编码后的公钥转换成PublicKey对象
        PublicKey publicKey = RSAUtil.string2PublicKey(publicKeyStr);
        //生成AES秘钥，并Base64编码
//        String aesKeyStr = AESUtil.genKeyAES();

        //用公钥加密AES秘钥
        //将AES加密
        byte[] publicEncrypt = RSAUtil.publicEncrypt(aesKeyStr.getBytes(), publicKey);
        //公钥加密AES秘钥后的内容Base64编码
        //转Base64
        String publicEncryptStr = RSAUtil.byte2Base64(publicEncrypt);

        //将Base64编码后的AES秘钥转换成SecretKey对象
        SecretKey aesKey = AESUtil.loadKeyAES(aesKeyStr);
        //用AES秘钥加密实际的内容
        byte[] encryptAES = AESUtil.encryptAES(content.getBytes(), aesKey);
        //AES秘钥加密后的内容Base64编码
        String encryptAESStr = AESUtil.byte2Base64(encryptAES);
//        System.out.println("AES秘钥加密实际的内容并Base64编码的结果：" + encryptAESStr);
        return encryptAESStr;
    }

}
