package org.xdi.oxd.server.license;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/11/2016
 */

public class MacAddressProvider {

    private static final Logger LOG = LoggerFactory.getLogger(MacAddressProvider.class);

    private MacAddressProvider() {
    }

    public static String macAddress() {
           String macAddressFromFile = LicenseFile.MacAddress.getMacAddress();
           if (!Strings.isNullOrEmpty(macAddressFromFile)) {
               LOG.trace("Mac address fetched from file: " + macAddressFromFile);
               return macAddressFromFile;
           }
           try {
               InetAddress ip = InetAddress.getLocalHost();
               LOG.trace("Generating new mac address ... ip: " + ip);
               NetworkInterface network = NetworkInterface.getByInetAddress(ip);
               if (network != null) {
                   byte[] mac = network.getHardwareAddress();
                   if (mac != null && mac.length > 0) {
                       return macAsString(mac);
                   }
               }

               for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                   byte[] mac = networkInterface.getHardwareAddress();

                   if (mac != null && mac.length > 0) {
                       return macAsString(mac);
                   }
               }
           } catch (Exception e) {
               LOG.error(e.getMessage(), e);
           }

           String uuid = UUID.randomUUID().toString();
           LOG.debug("Generated fallback UUID instead of mac address:" + uuid);
           return uuid;
       }

       private static String macAsString(byte[] mac) {
           StringBuilder sb = new StringBuilder();
           for (int i = 0; i < mac.length; i++) {
               sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
           }
           return sb.toString();
       }
}
