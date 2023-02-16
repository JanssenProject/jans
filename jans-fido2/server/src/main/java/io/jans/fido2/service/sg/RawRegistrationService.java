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
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import io.jans.as.model.fido.u2f.exception.BadInputException;
import io.jans.as.model.fido.u2f.message.RawRegisterResponse;
import io.jans.as.model.util.Base64Util;
import io.jans.fido2.service.Base64Service;
import io.jans.util.io.ByteDataInputStream;
import io.jans.util.security.SecurityProviderUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
/**
 * Provides operations with U2F RAW registration response
 *
 * @author Yuriy Movchan Date: 05/20/2015
 */
@ApplicationScoped
public class RawRegistrationService {

	@Inject
	private Logger log;

	@Inject
    private Base64Service base64Service;

	public static final byte REGISTRATION_RESERVED_BYTE_VALUE = (byte) 0x05;
	public static final byte REGISTRATION_SIGNED_RESERVED_BYTE_VALUE = (byte) 0x00;
	public static final long INITIAL_DEVICE_COUNTER_VALUE = -1;

	public static final String REGISTER_FINISH_TYPE = "navigator.id.finishEnrollment";
	public static final String REGISTER_CANCEL_TYPE = "navigator.id.cancelEnrollment";
	public static final String[] SUPPORTED_REGISTER_TYPES = new String[] { REGISTER_FINISH_TYPE, REGISTER_CANCEL_TYPE };


	public RawRegisterResponse parseRawRegisterResponse(String rawDataBase64) throws BadInputException {
		ByteDataInputStream bis = new ByteDataInputStream(Base64Util.base64urldecode(rawDataBase64));
		try {
			try {
				byte reservedByte = bis.readSigned();
				if (reservedByte != REGISTRATION_RESERVED_BYTE_VALUE) {
					throw new BadInputException("Incorrect value of reserved byte. Expected: " + REGISTRATION_RESERVED_BYTE_VALUE + ". Was: " + reservedByte);
				}
				return new RawRegisterResponse(bis.read(65), bis.read(bis.readUnsigned()), parseDer(bis), bis.readAll());
			} catch (IOException ex) {
				throw new BadInputException("Failed to parse RAW register response", ex);
			} catch (CertificateException e) {
				throw new BadInputException("Malformed attestation certificate", e);
			} catch (NoSuchProviderException e) {
				throw new BadInputException("Failed to parse attestation certificate", e);
			}
		} finally {
			IOUtils.closeQuietly(bis);
		}
	}

    public X509Certificate parseDer(InputStream is) throws CertificateException, NoSuchProviderException {
            return (X509Certificate) CertificateFactory.getInstance("X.509", SecurityProviderUtility.getBCProvider()).generateCertificate(is);
    }


}
