/*
 * Copyright [2026] [Janssen Project]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jans.core.cedarling.service.policy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CjarArchive {

	public static final String METADATA_ENTRY = "metadata.json";

	public static final int MAX_METADATA_BYTES = 1024 * 1024;

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private CjarArchive() {
	}

	public static JsonNode readPolicyStoreMetadata(byte[] archive, int maxEntries, ObjectMapper objectMapper) throws IOException {
		JsonNode policyStore = null;
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(archive))) {
			ZipEntry ze;
			int entriesCount = 0;
			while ((ze = zis.getNextEntry()) != null) {
				entriesCount++;
				if (maxEntries > 0 && entriesCount > maxEntries) {
					throw new IllegalStateException("Archive contains too many files.");
				}

				if (policyStore != null || ze.isDirectory() || !METADATA_ENTRY.equals(ze.getName())) {
					continue;
				}

				byte[] metadataBytes = zis.readNBytes(MAX_METADATA_BYTES + 1);
				if (metadataBytes.length > MAX_METADATA_BYTES) {
					throw new IllegalStateException("Archive metadata.json is too large.");
				}

				policyStore = parseMetadata(metadataBytes, objectMapper).path("policy_store");
			}
		}

		return policyStore;
	}

	private static JsonNode parseMetadata(byte[] metadataBytes, ObjectMapper objectMapper) {
		JsonNode metadata;
		try {
			metadata = objectMapper.readTree(metadataBytes);
		} catch (JsonProcessingException ex) {
			throw new IllegalStateException("Archive metadata.json is not valid JSON.", ex);
		} catch (IOException ex) {
			throw new IllegalStateException("Archive metadata.json could not be read.", ex);
		}

		if (metadata == null || !metadata.isObject()) {
			throw new IllegalStateException("Archive metadata.json is empty or not a JSON object.");
		}

		return metadata;
	}

	public static String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isTextual() ? value.asText() : null;
	}

	public static String bundleHash(byte[] archive) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(archive);
			char[] hex = new char[digest.length * 2];
			for (int i = 0; i < digest.length; i++) {
				hex[i * 2] = HEX[(digest[i] >> 4) & 0x0F];
				hex[i * 2 + 1] = HEX[digest[i] & 0x0F];
			}

			return "sha256:" + new String(hex);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

}
