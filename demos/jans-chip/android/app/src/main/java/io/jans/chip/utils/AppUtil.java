package io.jans.chip.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AppUtil {
    public static String getChecksum(Context context) throws IOException, NoSuchAlgorithmException, PackageManager.NameNotFoundException {
        // Get the Android package file path.
        String apkPath = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).applicationInfo.sourceDir;
        Log.d("apkPath :::::::", apkPath);
        // Get the checksum of the Android package file.
        return getChecksum(new File(apkPath));
    }

    public static String getChecksum(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = fis.read(data)) != -1) {
            md.update(data, 0, nRead);
        }
        byte[] mdbytes = md.digest();

        // Convert the byte array to a hexadecimal string.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}
