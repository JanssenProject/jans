/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @see org.apache.commons.lang.StringUtils
 */
public final class StringHelper {

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern QUOTE_PATTERN = Pattern.compile("\\\"");
    private static final String CHARSET = "!0123456789abcdefghijklmnopqrstuvwxyz";
    public static final byte[] EMPTY_BYTES = new byte[0];
    private static Random RAND;

    private StringHelper() {
    }

    public static String getRandomString(final int length) {
        if (RAND == null) {
            RAND = new Random(System.currentTimeMillis());
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int pos = RAND.nextInt(CHARSET.length());
            sb.append(CHARSET.charAt(pos));
        }
        return sb.toString();
    }

    public static String[] add(final String[] x, String sep, String[] y) {
        String[] result = new String[x.length];
        for (int i = 0; i < x.length; i++) {
            result[i] = x[i] + sep + y[i];
        }
        return result;
    }

    public static String repeat(final String string, int times) {
        StringBuffer buf = new StringBuffer(string.length() * times);
        for (int i = 0; i < times; i++) {
            buf.append(string);
        }

        return buf.toString();
    }

    public static String repeat(char character, int times) {
        char[] buffer = new char[times];
        Arrays.fill(buffer, character);
        return new String(buffer);
    }

    public static String[] split(final String str, String delim) {
        return split(str, delim, true, false);
    }

    public static String[] split(final String str, String delim, boolean trim, boolean include) {
        StringTokenizer tokens = new StringTokenizer(str, delim, include);
        String[] result = new String[tokens.countTokens()];
        int i = 0;
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (trim) {
                token = token.trim();
            }
            result[i++] = token;
        }
        return result;
    }

    public static String unqualify(final String qualifiedName) {
        int loc = qualifiedName.lastIndexOf(".");
        return (loc < 0) ? qualifiedName : qualifiedName.substring(loc + 1);
    }

    public static String qualifier(final String qualifiedName) {
        int loc = qualifiedName.lastIndexOf(".");
        return (loc < 0) ? "" : qualifiedName.substring(0, loc);
    }

    public static String suffix(final String name, String suffix) {
        return (suffix == null) ? name : name + suffix;
    }

    public static boolean booleanValue(final String tfString) {
        String trimmed = tfString.trim().toLowerCase();
        return trimmed.equals("true") || trimmed.equals("t");
    }

    public static String toString(Object[] array) {
        int len = array.length;
        if (len == 0) {
            return "";
        }
        StringBuffer buf = new StringBuffer(len * 12);
        for (int i = 0; i < len - 1; i++) {
            buf.append(array[i]).append(", ");
        }
        return buf.append(array[len - 1]).toString();
    }

    public static boolean isNotEmpty(final String str) {
        return !StringHelper.isEmpty(str);
    }

    public static boolean isEmpty(final String str) {
        if (str == null || (str.length() == 0)) {
            return true;
        }

        int strLen = str.length();

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static String truncate(final String string, int length) {
        if (string.length() <= length) {
            return string;
        } else {
            return string.substring(0, length);
        }
    }

    public static String toUpperCase(final String str) {
        return str == null ? null : str.toUpperCase();
    }

    public static String toLowerCase(final String str) {
        return str == null ? null : str.toLowerCase();
    }

    public static String removePunctuation(final String punctuatedString) {
        return punctuatedString == null ? null : punctuatedString.replaceAll("[\\.@!,:/]", "");
    }

    public static String formatZeroLeadingLong(long n, int digits) {
        /*
         * we create a format : %% : % the first % is to escape the second % 0 : 0 zero
         * character %d : how many '0' we want (specified by digits) d : d the number to
         * format
         */
        String format = String.format("%%0%dd", digits);
        return String.format(format, n);
    }

    public static boolean compare(final String str1, String str2) {
        if (str1 == null) {
            if (str2 != null) {
                return false;
            }
        } else {
            return str1.equals(str2);
        }

        return true;
    }

    public static boolean equalsIgnoreCase(final String str1, String str2) {
        if (str1 == null) {
            if (str2 != null) {
                return false;
            }
        } else if (!str1.equalsIgnoreCase(str2)) {
            return false;
        }

        return true;
    }

    public static boolean equals(final String str1, String str2) {
        if (str1 == null) {
            if (str2 != null) {
                return false;
            }
        } else if (!str1.equals(str2)) {
            return false;
        }

        return true;
    }

    public static String encodeString(final String str) {
        if ((str == null) || (str.length() == 0)) {
            return str;
        }
        try {
            return (new URI(null, str, null)).toString();
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    public static String getEmtpyStringIfNull(final String str) {
        return (str == null) ? "" : str;
    }

    public static String getNullIfEmtpyString(final String str) {
        return (str == null) || (str.trim().length() == 0) ? null : str;
    }

    public static String getValueFromDelimitedString(final String delimitedString, int attrIndex) {
        if (isEmpty(delimitedString)) {
            return "";
        }

        String[] parts = delimitedString.split("\\|");
        if (attrIndex < parts.length) {
            String[] paramPararts = parts[attrIndex].split("\\:");
            if (paramPararts.length == 2) {
                return paramPararts[1].trim();
            }
        }

        return "";
    }

    public static Map<String, String> getValueMapForDelimitedString(final String delimitedString) {
        Map<String, String> result = new HashMap<String, String>();
        if (isEmpty(delimitedString)) {
            return result;
        }

        String[] parts = delimitedString.split("\\|");
        for (final String part : parts) {
            String[] paramPararts = part.split("\\:");
            if (paramPararts.length == 2) {
                result.put(paramPararts[0].trim().toLowerCase(), paramPararts[1].trim());
            }
        }

        return result;
    }

    public static String[] getValuesFromColonDelimitedString(final String delimitedString) {
        if (isEmpty(delimitedString)) {
            return new String[0];
        }

        String[] result = delimitedString.split("\\:");
        for (int i = 0; i < result.length; i++) {
            if (StringHelper.isNotEmpty(result[i])) {
                result[i] = result[i].trim();
            }

        }

        return result;
    }

    public static String buildDelimitedString(final String[] attributes, String... values) {
        if ((attributes == null) || (values == null) || (attributes.length != values.length)) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < attributes.length; i++) {
            String value = getEmtpyStringIfNull(values[i]);
            result.append(attributes[i]).append(": ").append(value);
            if (i < attributes.length - 1) {
                if (value.length() > 0) {
                    result.append(" ");
                }
                result.append("| ");
            }
        }

        return result.toString();
    }

    public static String buildColonDelimitedString(final String... values) {
        return buildDelimitedString(" : ", values);
    }

    public static String buildDelimitedString(final String delimiter, String... values) {
        if (values == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            result.append(getEmtpyStringIfNull(values[i]));
            if (i < values.length - 1) {
                result.append(delimiter);
            }
        }

        return result.toString();
    }

    public static float toFloat(final String string) {
        if (isEmpty(string)) {
            return 0.0f;
        }

        try {
            return Float.parseFloat(string);
        } catch (NumberFormatException ex) {
            return 0.0f;
        }
    }

    public static int toInteger(final String string) {
        if (isEmpty(string)) {
            return 0;
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static int toInteger(final String string, int defaultValue) {
        if (isEmpty(string)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static Integer toInteger(final String string, Integer defaultValue) {
        if (isEmpty(string)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static int toInt(final String string, int defaultValue) {
        if (isEmpty(string)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static long toLong(String string, int defaultValue) {
        if (isEmpty(string)) {
            return defaultValue;
        }

        try {
            return Long.parseLong(string);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
    public static boolean toBoolean(final String string, boolean defaultValue) {
        if (isEmpty(string)) {
            return defaultValue;
        }

        try {
            return Boolean.parseBoolean(string);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public static boolean toBoolean(Boolean value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    public static String getFirstPositiveNumber(final String string) {
        if (isEmpty(string)) {
            return "";
        }

        String[] numbers = string.split("([^\\d]+)");

        if (numbers.length > 0) {
            return numbers[0];
        }

        return "";
    }

    public static String removeMultipleSpaces(final String string) {
        if (isEmpty(string)) {
            return string;
        }
        Matcher matcher = MULTI_SPACE_PATTERN.matcher(string);
        if (matcher.find()) {
            return matcher.replaceAll(" ");
        }

        return string;
    }

    // Deprecated. Use IOUtils.toString(input)
    @Deprecated
    public static String convertStreamToString(InputStream is) throws IOException {
        /*
         * To convert the InputStream to String we use the Reader.read(char[] buffer)
         * method. We iterate until the Reader return -1 which means there's no more
         * data to read. We use the StringWriter class to produce the string.
         */
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    public static String replaceLast(final String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos) + replacement + string.substring(pos + toReplace.length(), string.length());
        } else {
            return string;
        }
    }

    public static String doubleQuotes(final String string) {
        if (isEmpty(string)) {
            return string;
        }
        Matcher matcher = QUOTE_PATTERN.matcher(string);
        if (matcher.find()) {
            return matcher.replaceAll("\"\"");
        }

        return string;
    }

    public static String addQuote(final String string) {
        if (isEmpty(string)) {
            return "";
        }

        return "\"" + string + "\"";
    }

    public static boolean isEmptyString(Object string) {
        return !(string instanceof String) || isEmpty((String) string);
    }

    public static boolean isNotEmptyString(Object string) {
        return !(string instanceof String) || isNotEmpty((String) string);
    }

    public static String toString(Object object) {
        return (object == null) ? null : object.toString();
    }

    public static String qualify(final String prefix, String name) {
        if (name == null || prefix == null) {
            throw new NullPointerException();
        }
        return new StringBuffer(prefix.length() + name.length() + 1).append(prefix).append('.').append(name).toString();
    }

    public static String trimAll(final String string) {
        if (isEmpty(string)) {
            return string;
        }

        return string.trim();
    }

    public static String utf8ToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String utf8ToString(byte[] bytes, int start, int length) {
        if (bytes == null) {
            return "";
        }

        return new String(bytes, start, length, StandardCharsets.UTF_8);
    }

    public static byte[] getBytesUtf8(String string) {
        if (string == null) {
            return EMPTY_BYTES;
        }

        return string.getBytes(StandardCharsets.UTF_8);
    }

}
