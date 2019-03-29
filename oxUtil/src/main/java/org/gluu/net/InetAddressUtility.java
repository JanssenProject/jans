package org.gluu.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yuriy Movchan Date: 05/20/2015
 */
public final class InetAddressUtility {

    private static Pattern VALID_IPV4_PATTERN = null;
    private static Pattern VALID_IPV6_PATTERN = null;
    private static final String IPV4_PATTERN = "(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])";
    private static final String IPV6_PATTERN = "([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}";

    static {
        VALID_IPV4_PATTERN = Pattern.compile(IPV4_PATTERN, Pattern.CASE_INSENSITIVE);
        VALID_IPV6_PATTERN = Pattern.compile(IPV6_PATTERN, Pattern.CASE_INSENSITIVE);
    }

    private static String MAC_ADDRESS;
    private static boolean MAC_ADDRESS_SET = false;

    private InetAddressUtility() { }

    /**
     * Determine if the given string is a valid IPv4 or IPv6 address
     */
    public static boolean isIpAddress(String ipAddress) {

        Matcher m1 = VALID_IPV4_PATTERN.matcher(ipAddress);
        if (m1.matches()) {
            return true;
        }

        Matcher m2 = VALID_IPV6_PATTERN.matcher(ipAddress);

        return m2.matches();
    }

    public static String getMACAddressOrNull() {
        if (!MAC_ADDRESS_SET) {
            MAC_ADDRESS = getMACAddressOrNullImpl();
            MAC_ADDRESS_SET = true;
        }

        return MAC_ADDRESS;
    }

    private static synchronized String getMACAddressOrNullImpl() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                return null;
            }

            byte[] mac = network.getHardwareAddress();
            if (mac == null) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (UnknownHostException e) {
            return null;
        } catch (SocketException e) {
            return null;
        }
    }

}
