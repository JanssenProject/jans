/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.core.cedarling;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import org.json.JSONObject;

/**
 * Base test class for tests
 * 
 * @author Yuriy Movchan Date: 12/05/2026
 */
public abstract class BaseCedarlingTest {

	/**
	 * Patches the {@code exp} claim of a JWT so it expires one hour from now.
	 *
	 * <p>The method only re-encodes the payload; the original header and signature
	 * parts are preserved unchanged.  Because the Cedarling adapter is configured
	 * with {@code jwtSigValidation(false)} the modified payload will still pass
	 * validation inside the policy engine.
	 *
	 * <p>Algorithm:
	 * <ol>
	 *   <li>Split the JWT on {@code '.'} to obtain header, payload, signature.</li>
	 *   <li>Base64url-decode the payload (without padding).</li>
	 *   <li>Parse as JSON and replace {@code exp} with {@code now + 3600} seconds.</li>
	 *   <li>Base64url-encode (without padding) and reassemble.</li>
	 * </ol>
	 *
	 * @param jwt the original JWT string
	 * @return the same JWT with a refreshed {@code exp} claim
	 */
	protected static String withFutureExp(String jwt) {
	    String[] parts = jwt.split("\\.");
	    if (parts.length != 3) {
	        throw new IllegalArgumentException("Not a three-part JWT: " + jwt);
	    }
	
	    // Decode payload – add padding if the decoder requires it
	    Base64.Decoder decoder = Base64.getUrlDecoder();
	    String payloadJson = new String(decoder.decode(addPadding(parts[1])), StandardCharsets.UTF_8);
	
	    // Update the exp claim to 1 minute in the future
	    JSONObject payload = new JSONObject(payloadJson);
	    payload.put("exp", (Instant.now().toEpochMilli() + 1 * 60L * 1000L) / 1000L);

	    // Re-encode without padding (standard JWT convention)
	    String newPayload = Base64.getUrlEncoder()
	            .withoutPadding()
	            .encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
	
	    return parts[0] + "." + newPayload + "." + parts[2];
	}

	/**
	 * Reads the entire content of a classpath resource into a {@link String}.
	 *
	 * @param resourceName the resource file name relative to the classpath root
	 *                     (e.g. {@code "lock_policy_store.json"})
	 * @return the file content, or {@code null} if the resource cannot be found
	 */
	protected static String loadResourceAsString(String resourceName) throws Exception {
	    ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    try (InputStream is = cl.getResourceAsStream(resourceName)) {
	        if (is == null) {
	            return null;
	        }
	        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
	    }
	}

	/**
	 * Injects a value into a private field of an object using reflection.
	 * Used to wire CDI {@code @Inject} fields without a running container.
	 *
	 * @param target    the object whose field should be set
	 * @param fieldName the name of the private field
	 * @param value     the value to assign
	 */
	protected static void injectField(Object target, String fieldName, Object value) throws Exception {
		Class<?> cls = target.getClass();
		while (cls != null) {
			try {
				Field field = cls.getDeclaredField(fieldName);
				field.setAccessible(true);
				field.set(target, value);
				return;
			} catch (NoSuchFieldException ignored) {
				cls = cls.getSuperclass();
			}
		}
		throw new NoSuchFieldException("Field '" + fieldName + "' not found in " + target.getClass().getName());
	}

	/**
	 * Appends Base64 padding characters ({@code =}) so that the URL-decoder accepts
	 * base64url strings whose length is not a multiple of 4.
	 *
	 * @param base64url the unpadded base64url string
	 * @return the same string with trailing {@code =} padding if needed
	 */
	private static String addPadding(String base64url) {
	    int mod = base64url.length() % 4;
	    if (mod == 0) {
	        return base64url;
	    }
	    return base64url + "=".repeat(4 - mod);
	}

	public BaseCedarlingTest() {
		super();
	}

}