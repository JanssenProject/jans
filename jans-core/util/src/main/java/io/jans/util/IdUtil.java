package io.jans.util;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * @author Yuriy Z
 */
public class IdUtil {

    // we are ok to have not secured random
    private static final Random RANDOM = new Random();

    private IdUtil() {
    }

    public static String randomShortUUID() {
        return toShortUUID(UUID.randomUUID());
    }

    public static String toShortUUID(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().withoutPadding().encodeToString(byteBuffer.array())
                .replace("/", randomChar()).replace("+", randomChar());
    }

    private static String randomChar() {
        return (char) (RANDOM.nextInt(26) + 'a') + "";
    }
}
