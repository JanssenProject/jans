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

package io.jans.fido2.androind;

import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.X509Certificate;

import jakarta.enterprise.context.ApplicationScoped;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;

/**
 * Taken from
 * https://github.com/googlesamples/android-key-attestation/blob/master/server/src/main/java/com/android/example/KeyAttestationExample.java
 **/

@ApplicationScoped
public class AndroidKeyUtils {

    public static final String KEY_DESCRIPTION_OID = "1.3.6.1.4.1.11129.2.1.17";

    public static final int ATTESTATION_VERSION_INDEX = 0;
    public static final int ATTESTATION_SECURITY_LEVEL_INDEX = 1;
    public static final int KEYMASTER_SECURITY_LEVEL_INDEX = 3;
    public static final int ATTESTATION_CHALLENGE_INDEX = 4;
    public static final int SW_ENFORCED_INDEX = 6;
    public static final int TEE_ENFORCED_INDEX = 7;

    // Some authorization list tags. The complete list is in this AOSP file:
    // hardware/libhardware/include/hardware/keymaster_defs.h
    public static final int KM_TAG_PURPOSE = 1;
    public static final int KM_TAG_ALGORITHM = 2;
    public static final int KM_TAG_KEY_SIZE = 3;
    public static final int KM_TAG_USER_AUTH_TYPE = 504;
    public static final int KM_TAG_AUTH_TIMEOUT = 505;
    public static final int KM_TAG_ORIGIN = 702;
    public static final int KM_TAG_ROLLBACK_RESISTANT = 703;

    // The complete list of purpose values is in this AOSP file:
    // hardware/libhardware/include/hardware/keymaster_defs.h
    public static final int KM_PURPOSE_SIGN = 2;

    // The complete list of algorithm values is in this AOSP file:
    // hardware/libhardware/include/hardware/keymaster_defs.h
    public static final int KM_ALGORITHM_EC = 3;

    // Some authentication type values. The complete list is in this AOSP file:
    // hardware/libhardware/include/hardware/hw_auth_token.h
    public static final int HW_AUTH_PASSWORD = 1 << 0;
    public static final int HW_AUTH_FINGERPRINT = 1 << 1;

    // The complete list of origin values is in this AOSP file:
    // hardware/libhardware/include/hardware/keymaster_defs.h
    public static final int KM_ORIGIN_GENERATED = 0;

    // Some security values. The complete list is in this AOSP file:
    // hardware/libhardware/include/hardware/keymaster_defs.h
    public static final int KM_SECURITY_LEVEL_SOFTWARE = 0;
    public static final int KM_SECURITY_LEVEL_TRUSTED_ENVIRONMENT = 1;

    public static final int EXPECTED_ATTESTATION_VERSION = 1;

    public static int getIntegerFromAsn1(ASN1Encodable asn1Value) throws Exception {
        if (asn1Value instanceof ASN1Integer) {
            return AndroidKeyUtils.bigIntegerToInt(((ASN1Integer) asn1Value).getValue());
        } else if (asn1Value instanceof ASN1Enumerated) {
            return AndroidKeyUtils.bigIntegerToInt(((ASN1Enumerated) asn1Value).getValue());
        } else {
            throw new Exception("Integer value expected; found " + asn1Value.getClass().getName() + " instead.");
        }
    }

    public static int bigIntegerToInt(BigInteger bigInt) throws Exception {
        if (bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 || bigInt.compareTo(BigInteger.ZERO) < 0) {
            throw new Exception("INTEGER out of bounds");
        }
        return bigInt.intValue();
    }

    public ASN1Sequence extractAttestationSequence(X509Certificate attestationCert) throws Exception, IOException {
        byte[] attestationExtensionBytes = attestationCert.getExtensionValue(KEY_DESCRIPTION_OID);
        if (attestationExtensionBytes == null || attestationExtensionBytes.length == 0) {
            throw new Exception("Couldn't find the keystore attestation " + "extension data.");
        }

        ASN1Sequence decodedSequence;
        try (ASN1InputStream asn1InputStream = new ASN1InputStream(attestationExtensionBytes)) {
            // The extension contains one object, a sequence, in the
            // Distinguished Encoding Rules (DER)-encoded form. Get the DER
            // bytes.
            byte[] derSequenceBytes = ((ASN1OctetString) asn1InputStream.readObject()).getOctets();
            // Decode the bytes as an ASN1 sequence object.
            try (ASN1InputStream seqInputStream = new ASN1InputStream(derSequenceBytes)) {
                decodedSequence = (ASN1Sequence) seqInputStream.readObject();
            }
        }
        return decodedSequence;
    }
}
