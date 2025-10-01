package io.jans.casa.misc;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.model.SimpleCustomProperty;
import io.jans.util.properties.FileConfiguration;
import io.jans.util.security.StringEncrypter;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.enterprise.inject.spi.CDI;

import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.mime.MimeTypes;
import io.jans.casa.core.model.CustomScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides miscellaneous utilities.
 * @author jgomer
 */
public final class Utils {

    private static Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static ObjectMapper MAPPER = new ObjectMapper();
    private static Tika tika = new Tika();    
    private static final String SALT_FILE_LOCATION = "/etc/jans/conf/salt";

    private Utils() { }

    /**
     * Whether this call is being performed on Windows operating system or not.
     * @return A boolean value
     */
    public static boolean onWindows() {
        return System.getProperty("os.name").toLowerCase().matches(".*win.*");
    }

    public static boolean isEmpty(String string) {
        return !isNotEmpty(string);
    }

    public static boolean isNotEmpty(String string) {
        return Optional.ofNullable(string).map(String::length)
                .flatMap(i -> i > 0 ? Optional.of(i) : Optional.empty()).isPresent();
    }

    public static <T> boolean isEmpty(T[] array) {
        return !isNotEmpty(array);
    }

    public static <T> boolean isNotEmpty(T[] array) {
        return Optional.ofNullable(array).map(arr -> arr.length)
                .flatMap(i -> i > 0 ? Optional.of(i) : Optional.empty()).isPresent();
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return !isNotEmpty(collection);
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return Optional.ofNullable(collection).map(Collection::size)
                .flatMap(i -> i > 0 ? Optional.of(i) : Optional.empty()).isPresent();
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return !isNotEmpty(map);
    }

    public static <K, V> boolean isNotEmpty(Map<K, V> map) {
        return Optional.ofNullable(map).map(Map::size)
                .flatMap(i -> i > 0 ? Optional.of(i) : Optional.empty()).isPresent();
    }

    /**
     * Checks whether the {@link Path} instance passed seems to correspond to a jar file.
     * @param path Path of file
     * @return A boolean value. A shallow check is performed: if the file exists and has .jar extension
     */
    public static boolean isJarFile(Path path) {
        return path.toString().toLowerCase().endsWith(".jar") && Files.isRegularFile(path);
    }

    /**
     * Checks whether the {@link Path} instance passed seems to correspond to a java class file.
     * @param path Path of file
     * @return A boolean value. A shallow check is performed: if the file exists and has .class extension
     */
    public static boolean isClassFile(Path path) {
        return path.toString().endsWith(".class") && Files.isRegularFile(path);
    }

    /**
     * Given a type obtains a reference to a CDI managed bean living in Gluu Casa webapp. Use interfaces belonging to
     * package <code>io.jans.casa.service</code> when calling this method.
     * @param clazz A {@link Class} representing the required type
     * @param <T> The required type
     * @return An instance object
     */
    public static <T> T managedBean(Class<T> clazz) {
        return CDI.current().select(clazz).get();
    }

    /**
     * Returns a json representation of an arbitrary object using a default <code>com.fasterxml</code> Jackson mapper object.
     * @param obj Object to convert to json representation
     * @return A string with result of the operation. The empty object {} is returned in case of failure when serializing
     */
    public static String jsonFromObject(Object obj) {

        String json;
        try {
            json = MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            json = "{}";
        }
        return json;

    }

    /**
     * Computes the <code>data-uri</code> representation of an image whose bytes are given a parameter. This is useful
     * to represent images inlined in CSS.
     * @param bytes Array of bytes
     * @param hintName A file name this image may have if it were physical stored in disk. This is used as a hint for the
     *                mime content of the bytes if it cannot be inferred automatically. Pass null if you cannot provide
     *                a hint about which could be a suitable file name (note that file extension is the useful portion of
     *                this String)
     * @return The data-uri representation (uses Base64 encoding)
     */
    public static String getImageDataUriEncoding(byte[] bytes, String hintName) {

        String mime = null;
        String encodedImg = Base64.getEncoder().encodeToString(bytes);

        try {
            try (TikaInputStream tis = TikaInputStream.get(bytes)) {
                mime = tika.detect(tis, hintName);
                
                if (mime.equals(MimeTypes.OCTET_STREAM) && hintName != null) {
                    //detection failed
                    LOG.trace("Cannot infer mime type of image using Tika");
                    mime = URLConnection.guessContentTypeFromName(hintName);
                }
            }
        } catch (Exception e) {
            if (hintName != null) {
                mime = URLConnection.guessContentTypeFromName(hintName);
            }
        }
        if (mime == null) {
            mime = "";
            LOG.trace("Cannot infer mime type of image");
        } else {
            LOG.trace("Using mime {}", mime);
        }
        return String.format("data:%s;base64,%s", mime, encodedImg);

    }

    /**
     * Returns a clone of the object passsed.
     * @param obj Object to be cloned
     * @return A deep clone (using Jackson objectmapper conversion: object -> json string -> object)
     * (null if the cloning process thew an exception)
     */
    public static Object cloneObject(Object obj) {

        Object result = null;
        try {
            result = MAPPER.readValue(MAPPER.writeValueAsString(obj), obj.getClass());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return result;

    }

    /**
     * Builds a {@link List} of elements using the contents of the array supplied.
     * @param array Array of elements
     * @param <T> Type of elements
     * @return A list (empty if provided array has length zero or is null)
     */
    public static <T> List<T> listfromArray(T[] array) {
        if (isEmpty(array)) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(array);
        }
    }

    /**
     * Returns an array of length zero or one of the specified type
     * @param cls Class to which the elements of the array belong
     * @param value Single value the resulting array will contain. If null, the resulting array will be empty
     * @param <T> Type of elements
     * @return An array of T instances
     */
    public static <T> T[] arrayFromValue(Class<T> cls, T value) {

        T[] arr = (T[]) java.lang.reflect.Array.newInstance(cls, value == null ? 0 : 1);
        if (arr.length == 1) {
            arr[0] = value;
        }
        return arr;

    }

    public static <T> List<T> nonNullList(List<T> list) {
        return Optional.ofNullable(list).orElse(Collections.emptyList());
    }

    /**
     * Checks if a socket connection can be established.
     * @param address A {@link SocketAddress} to establish the connection.
     * @param timeout A timeout for connection in ms
     * @return Whether the connection was established
     */
    public static boolean hostAvailabilityCheck(SocketAddress address, int timeout) {

        boolean available = false;
        try (Socket socket = new Socket()) {
            socket.connect(address, timeout);
            available = true;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return available;
    }

    /**
     * Checks if the string passed as parameter resembles a valid URL.
     * @param strUrl String to check
     * @return Whether it has the form a valid URL or not
     */
    public static boolean isValidUrl(String strUrl) {

        boolean valid = false;
        try {
            URL url = new URL(strUrl);
            valid = isNotEmpty(url.getHost());
        } catch (Exception e) {
            //Intentionally left empty
        }
        if (!valid) {
            LOG.warn("Error validating url: {}", strUrl);
        }
        return valid;

    }

    /**
     * This method takes a {@link List} as input, applies a map turning elements into booleans, and returns the index of
     * first true occurrence.
     * @param list Input list
     * @param map mapping function (converts every T into a Boolean)
     * @param <T> Type of the list elements
     * @return Index of the first occurrence of True (-1 if all were False)
     */
    public static <T> int firstTrue(List<T> list, Function<? super T, ? extends Boolean> map){
        return list.stream().map(map).collect(Collectors.toList()).indexOf(true);
    }

    /**
     * Generates a sequence of random bytes (using {@link SecureRandom} class).
     * @param keyLen Number of bytes to return
     * @return Array of random bytes
     */
    public static byte[] randomBytes(int keyLen) {

        byte[] bytes = new byte[keyLen];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        return bytes;

    }

    /**
     * Builds a map of name/value entries based on the configuration properties associated to an object of class
     * {@link CustomScript}.
     * @param script A {@link CustomScript} instance
     * @return A Mapping of property name / property value for the script
     */
    public static Map<String, String> scriptConfigPropertiesAsMap(CustomScript script) {
        return nonNullList(script.getConfigurationProperties()).stream()
                .collect(Collectors.toMap(SimpleCustomProperty::getValue1, SimpleCustomProperty::getValue2));
    }

    /**
     * Analog method to {@link #scriptConfigPropertiesAsMap(CustomScript)}, this time for obtaining module properties
     * @param script A {@link CustomScript} instance
     * @return A Mapping of property name / property value for the script
     */
    public static Map<String, String> scriptModulePropertiesAsMap(CustomScript script) {
        return nonNullList(script.getModuleProperties()).stream()
                .collect(Collectors.toMap(SimpleCustomProperty::getValue1, SimpleCustomProperty::getValue2));
    }

    public static StringEncrypter stringEncrypter(String saltFile) throws StringEncrypter.EncryptionException {
        String salt = new FileConfiguration(saltFile).getProperties().getProperty("encodeSalt");
        return StringEncrypter.instance(salt);
    }

    /**
     * Returns a default instance of an encryption/decryption utility object. This is compatible with the
     * <code>EncryptionService</code> accessible in Gluu custom scripts
     * @return Utility object
     * @throws StringEncrypter.EncryptionException
     */
    public static StringEncrypter stringEncrypter() throws StringEncrypter.EncryptionException {
        return stringEncrypter(SALT_FILE_LOCATION);
    }

    public static boolean urlAvailabilityCheck(URL siteURL) throws Exception {

        HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(3000);
        connection.connect();

        return connection.getResponseCode() == 200;

    }

}
