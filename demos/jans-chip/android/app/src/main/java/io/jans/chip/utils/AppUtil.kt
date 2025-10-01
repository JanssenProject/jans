package io.jans.chip.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class AppUtil {
    companion object {
        @Throws(
            IOException::class,
            NoSuchAlgorithmException::class,
            PackageManager.NameNotFoundException::class
        )
        fun getChecksum(context: Context): String? {
            // Get the Android package file path.
            val apkPath =
                context.packageManager.getPackageInfo(
                    context.packageName,
                    0
                ).applicationInfo.sourceDir
            Log.d("apkPath :::::::", apkPath)
            // Get the checksum of the Android package file.
            return getChecksum(File(apkPath))
        }

        @Throws(IOException::class, NoSuchAlgorithmException::class)
        fun getChecksum(file: File?): String? {
            val md = MessageDigest.getInstance("MD5")
            val fis = FileInputStream(file)
            val data = ByteArray(1024)
            var nRead: Int
            while (fis.read(data).also { nRead = it } != -1) {
                md.update(data, 0, nRead)
            }
            val mdbytes = md.digest()

            // Convert the byte array to a hexadecimal string.
            val sb = StringBuilder()
            for (i in mdbytes.indices) {
                sb.append(Integer.toString((mdbytes[i].toInt() and 0xff) + 0x100, 16).substring(1))
            }
            return sb.toString()
        }
    }
}