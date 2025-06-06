/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2025, Janssen Project
 */

package io.jans.orm.operation.auth;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bouncycastle.crypto.params.Argon2Parameters;

/**
 * Base interface for persistence script
 *
 * @author Yuriy Movchan Date: 05/22/2025
 */
final class Argon2EncodingUtils {

	private static final Base64.Encoder b64encoder = Base64.getEncoder().withoutPadding();
	private static final Base64.Decoder b64decoder = Base64.getDecoder();

	private static Map<Integer, String> TYPE_MAP = new HashMap<Integer, String>();

    static {
    	TYPE_MAP.put(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_d, "argon2d");
    	TYPE_MAP.put(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_i, "argon2i");
    	TYPE_MAP.put(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_id, "argon2id");
    }

	private Argon2EncodingUtils() {
	}

	public static String encode(byte[] hash, Argon2Parameters parameters) throws IllegalArgumentException {
		StringBuilder stringBuilder = new StringBuilder();
		String type = TYPE_MAP.get(parameters.getType());
		if (type == null) {
			throw new IllegalArgumentException("Invalid algorithm type: " + parameters.getType());
		}
	
		stringBuilder.append("$");
		stringBuilder.append(type);
		stringBuilder.append("$v=")
			.append(parameters.getVersion())
			.append("$m=")
			.append(parameters.getMemory())
			.append(",t=")
			.append(parameters.getIterations())
			.append(",p=")
			.append(parameters.getLanes());

		if (parameters.getSalt() != null) {
			stringBuilder.append("$").append(b64encoder.encodeToString(parameters.getSalt()));
		}
		stringBuilder.append("$").append(b64encoder.encodeToString(hash));

		return stringBuilder.toString();
	}

	public static PasswordDetails decode(PasswordEncryptionMethod encryptionMethod, String encodedHash) throws IllegalArgumentException {
		Argon2Parameters.Builder paramsBuilder;
		String[] parts = encodedHash.split("\\$");
		int currPart = 1;
		if (parts.length < 4) {
			throw new IllegalArgumentException("Invalid encoded Argon2-hash");
		}

		int type = -1;
		for (Entry<Integer, String> itemEntry : TYPE_MAP.entrySet()) {
			if (itemEntry.getValue().equals(parts[currPart])) {
				type = itemEntry.getKey();
				break;
			}
		}
		if (type == -1) {
			throw new IllegalArgumentException("Invalid algorithm type: " + parts[0]);
		}

		paramsBuilder = new Argon2Parameters.Builder(type);

		if (parts[++currPart].startsWith("v=")) {
			paramsBuilder.withVersion(Integer.parseInt(parts[currPart].substring(2)));
			currPart++;
		}

		String[] performanceParams = parts[currPart++].split(",");
		if (performanceParams.length != 3) {
			throw new IllegalArgumentException("Not all performance parameters specified");
		}
		if (!performanceParams[0].startsWith("m=")) {
			throw new IllegalArgumentException("Invalid memory parameter");
		}
		paramsBuilder.withMemoryAsKB(Integer.parseInt(performanceParams[0].substring(2)));
		if (!performanceParams[1].startsWith("t=")) {
			throw new IllegalArgumentException("Invalid iterations parameter");
		}
		paramsBuilder.withIterations(Integer.parseInt(performanceParams[1].substring(2)));
		if (!performanceParams[2].startsWith("p=")) {
			throw new IllegalArgumentException("Invalid parallelity parameter");
		}

		paramsBuilder.withParallelism(Integer.parseInt(performanceParams[2].substring(2)));
		byte[] salt = b64decoder.decode(parts[currPart++]);
		paramsBuilder.withSalt(salt);

		return new PasswordDetails(encryptionMethod, salt, b64decoder.decode(parts[currPart]), paramsBuilder.build());
	}

}