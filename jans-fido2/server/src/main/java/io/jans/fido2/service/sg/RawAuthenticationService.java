/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

/*
 * Copyright (c) 2018 Mastercard
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package io.jans.fido2.service.sg;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import io.jans.as.model.fido.u2f.exception.BadInputException;
import io.jans.as.model.fido.u2f.message.RawAuthenticateResponse;
import io.jans.as.model.util.Base64Util;
import io.jans.util.io.ByteDataInputStream;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
/**
 * Provides operations with U2F RAW registration response
 *
 * @author Yuriy Movchan Date: 05/20/2015
 */
@ApplicationScoped
public class RawAuthenticationService {

	public static final String AUTHENTICATE_GET_TYPE = "navigator.id.getAssertion";
	public static final String AUTHENTICATE_CANCEL_TYPE = "navigator.id.cancelAssertion";
	public static final String[] SUPPORTED_AUTHENTICATE_TYPES = new String[] { AUTHENTICATE_GET_TYPE, AUTHENTICATE_CANCEL_TYPE };

	@Inject
	private Logger log;

	public RawAuthenticateResponse parseRawAuthenticateResponse(String rawDataBase64) {
		ByteDataInputStream bis = new ByteDataInputStream(Base64Util.base64urldecode(rawDataBase64));
		try {
			return new RawAuthenticateResponse(bis.readSigned(), bis.readInt(), bis.readAll());
		} catch (IOException ex) {
			throw new BadInputException("Failed to parse RAW authenticate response", ex);
		} finally {
			IOUtils.closeQuietly(bis);
		}
	}

	private byte[] packBytesToSign(byte[] appIdHash, byte userPresence, long counter, byte[] challengeHash) {
		ByteArrayDataOutput encoded = ByteStreams.newDataOutput();
		encoded.write(appIdHash);
		encoded.write(userPresence);
		encoded.writeInt((int) counter);
		encoded.write(challengeHash);

		return encoded.toByteArray();
	}

}
