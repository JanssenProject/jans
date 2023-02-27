package io.jans.ca.plugin.adminui.utils;

import com.google.common.base.Joiner;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;

import javax.crypto.Cipher;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;

public class CommonUtils {
    @Inject
    Logger log;
    public static final DateTimeFormatter LS_DATE_FORMAT = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);


    public static String joinAndUrlEncode(Collection<String> list) throws UnsupportedEncodingException {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return encode(Joiner.on(" ").join(list));
    }

    public static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, "UTF-8");
    }

    public static String getFormattedDate() {
        ZonedDateTime currentDateTime = ZonedDateTime.now();
        ZonedDateTime gmtTime = currentDateTime.withZoneSameInstant(ZoneId.of("GMT"));
        DateTimeFormatter currentTimeFormatter = LS_DATE_FORMAT;
        return gmtTime.format(currentTimeFormatter);
    }

    public static RSAPrivateKey loadPrivateKey(String privateKeyPEM) throws Exception {

        byte[] encoded = Base64.decodeBase64(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    public static String decode(String toDecode, String privateKeyPEM) throws Exception {

        PrivateKey privateKey = loadPrivateKey(privateKeyPEM);

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] bytes = cipher.doFinal(Base64.decodeBase64(toDecode));
        return new String(bytes);

    }

    //openssl genrsa -out private.pem 2048
    //openssl rsa -in private.pem -pubout -outform PEM -out public_key.pem
    //private_key.pem‬‬ => name of private key with PCKS8 format! you can just read this format in java
    //openssl pkcs8 -topk8 -inform PEM -in private.pem -out private_key.pem -nocrypt
/*    public static void main(String[] args) {

        try {
            String s = CommonUtils.decode("ProKf/o7Ai3cPjP3Qui5vu0z4SWMuL5jR2rMj+M/uV74miwwixkMdEo7sTgdhtksGBlAqUskVq/5hWRFMgVYry1fCvO+I9TvUoxTY3X5KzqY6OrM3UYbxJkCx6P/OT5tRKQy5jt7PxKjPspsR0fEzaUpCMmH8spNkfYS7zW5ZGc=",
                    "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKMbpXvumDpm50NX2aqpujuJUsS4f6fuVDWi2dB4QLOhOfTOmje3EgxgtGEYySmgfVHReUb0uCYwmwzfOiYeBTCcbmllHXitYDzbkVN93IEdgVMk891UYc5Jr18SgdMejMvyNpZkFTZDzcJarOk+Vm5MH+W8TxLamsV61HhyzrjfAgMBAAECgYBxy4kB1O152W9BaZof2jhm7yDCxKGzbLBtl0d1jWA+sp43sKihdGTwI8vU7jDyjNjB6248VeHgKwsRyO/NxiYBrg4SeEn2ZgQ02Hw+bD117hN8uhbdj1Mn8lMLq2gR3aEMdG76Uc7iNKKL2W0ZbAuBt/53QMJL3zXLizrCv/kH0QJBAM4N4IqK7zStrICxdpXY4oPx0vcVjd4a07pw9Y48d9UGma5kdVve3uuaLk/v/I5ncNzpU3MKJ8cTxoP++BNS62kCQQDKpNxr4+nkDE3qBwiXkb9bL6R5TKxE1D5wNX48XYOztVx9R73ZIqNyq0UB7876frRnnUhP1WxWBDqJZUKAK+EHAkBUxI64UBnCMSSDOP6Q/M2K/GQOs/ZOBflOfQP4BNZDc9irTradGd3ZTO96gT8EEnfy2aYz8FyW4ILNcIn74SPpAkEAxUtzq7uP2c89AJBuhhuJ4j7ldI/43V6Dl/4M9LrbYlk+Pl9d8I0v85HaswMHUo2QGZ1hbRDO1IRzdZfYAjA2ewJBAL/R15Bvcm8NY5gSWAlAPqMi0b8kg84mrdOWCHp0sSO2hFOj2lZTZv6iUxjQIFn/NSmRcaEWoYNaz9ggetjNpHQ=");
            System.out.println(s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}