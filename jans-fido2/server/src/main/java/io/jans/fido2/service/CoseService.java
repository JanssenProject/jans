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

package io.jans.fido2.service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import io.jans.fido2.ctap.CoseEC2Algorithm;
import io.jans.fido2.ctap.CoseKeyType;
import io.jans.fido2.ctap.CoseRSAAlgorithm;
import io.jans.fido2.exception.Fido2RuntimeException;
import io.jans.as.model.exception.SignatureException;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Utility classes for COSE key structure.
 *
 */
@ApplicationScoped
public class CoseService {

    private static final byte UNCOMPRESSED_POINT_INDICATOR = 0x04;

    @Inject
    private Logger log;

    @Inject
    private Base64Service base64Service;

    @Inject
    private DataMapperService dataMapperService;

    private static String convertCoseCurveToSunCurveName(int curve) {
        switch (curve) {
        case 1:
            return "secp256r1";
        default:
            throw new Fido2RuntimeException("Unsupported curve");
        }
    }

    public int getCodeCurve(JsonNode uncompressedECPointNode) {
        return uncompressedECPointNode.get("-1").asInt();
    }

    public PublicKey createUncompressedPointFromCOSEPublicKey(JsonNode uncompressedECPointNode) {
        int keyToUse = uncompressedECPointNode.get("1").asInt();
        int algorithmToUse = uncompressedECPointNode.get("3").asInt();
        CoseKeyType keyType = CoseKeyType.fromNumericValue(keyToUse);

        switch (keyType) {
        case RSA: {
            CoseRSAAlgorithm coseRSAAlgorithm = CoseRSAAlgorithm.fromNumericValue(algorithmToUse);
            switch (coseRSAAlgorithm) {
            case RS65535:
            case RS256: {
                byte[] rsaKey_n = base64Service.decode(uncompressedECPointNode.get("-1").asText());
                byte[] rsaKey_e = base64Service.decode(uncompressedECPointNode.get("-2").asText());
                return convertUncompressedPointToRSAKey(rsaKey_n, rsaKey_e);
            }
            default: {
                throw new Fido2RuntimeException("Don't know what to do with this key" + keyType);
            }
            }
        }
        case EC2: {
            CoseEC2Algorithm coseEC2Algorithm = CoseEC2Algorithm.fromNumericValue(algorithmToUse);
            switch (coseEC2Algorithm) {
            case ES256: {
                int curve = uncompressedECPointNode.get("-1").asInt();
                byte[] x = base64Service.decode(uncompressedECPointNode.get("-2").asText());
                byte[] y = base64Service.decode(uncompressedECPointNode.get("-3").asText());
                byte[] buffer = ByteBuffer.allocate(1 + x.length + y.length).put(UNCOMPRESSED_POINT_INDICATOR).put(x).put(y).array();
                return convertUncompressedPointToECKey(buffer, curve);
            }
            default: {
                throw new Fido2RuntimeException("Don't know what to do with this key" + keyType + " and algorithm " + coseEC2Algorithm);
            }
            }
        }
        case OKP: {
            throw new Fido2RuntimeException("Don't know what to do with this key" + keyType);
        }
        default:
            throw new Fido2RuntimeException("Don't know what to do with this key" + keyType);
        }
    }

    private PublicKey convertUncompressedPointToRSAKey(byte[] rsaKey_n, byte[] rsaKey_e) {
        try {
            BigInteger n = new BigInteger(1, rsaKey_n);
            BigInteger e = new BigInteger(1, rsaKey_e);
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(n, e);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Problem here ", e);
            throw new Fido2RuntimeException(e.getMessage());
        }
    }

    public ECPublicKey convertUncompressedPointToECKey(final byte[] uncompressedPoint, int curve) {
        AlgorithmParameters parameters = null;
        try {
            parameters = AlgorithmParameters.getInstance("EC");

            parameters.init(new ECGenParameterSpec(convertCoseCurveToSunCurveName(curve)));
            ECParameterSpec params = parameters.getParameterSpec(ECParameterSpec.class);

            int offset = 0;
            if (uncompressedPoint[offset++] != UNCOMPRESSED_POINT_INDICATOR) {
                throw new IllegalArgumentException("Invalid uncompressedPoint encoding, no uncompressed point indicator");
            }

            int keySizeBytes = (params.getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE;

            if (uncompressedPoint.length != 1 + 2 * keySizeBytes) {
                throw new IllegalArgumentException("Invalid uncompressedPoint encoding, not the correct size");
            }

            final BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, offset, offset + keySizeBytes));
            offset += keySizeBytes;
            final BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, offset, offset + keySizeBytes));
            final ECPoint w = new ECPoint(x, y);
            final ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(w, params);
            final KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return (ECPublicKey) keyFactory.generatePublic(ecPublicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidParameterSpecException e) {
            throw new Fido2RuntimeException(e.getMessage());
        }
    }

    public PublicKey getPublicKeyFromUncompressedECPoint(byte[] uncompressedECPointCOSEPubKey) {
        JsonNode uncompressedECPointNode = null;
        try {
            uncompressedECPointNode = dataMapperService.cborReadTree(uncompressedECPointCOSEPubKey);
        } catch (IOException e) {
            throw new Fido2RuntimeException("Unable to parse the structure");
        }
        log.debug("Uncompressed ECpoint node {}", uncompressedECPointNode.toString());
        PublicKey publicKey = createUncompressedPointFromCOSEPublicKey(uncompressedECPointNode);
        log.debug("EC Public key hex {}", Hex.encodeHexString(publicKey.getEncoded()));
        return publicKey;
    }
    
	public JsonNode convertECKeyToUncompressedPoint(byte[] encodedPublicKey) {
		X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
		org.bouncycastle.math.ec.ECPoint point = curve.getCurve().decodePoint(encodedPublicKey);
		int keySizeBytes = (curve.getN().bitLength() + Byte.SIZE - 1) / Byte.SIZE;

		ObjectNode uncompressedECPointNode = dataMapperService.createObjectNode();
		uncompressedECPointNode.put("1", 2);
		uncompressedECPointNode.put("3", -7);
		uncompressedECPointNode.put("-1", 1);
		uncompressedECPointNode.put("-2", toUncompressedCoord(point.getAffineXCoord().toBigInteger().toByteArray(), keySizeBytes));
		uncompressedECPointNode.put("-3", toUncompressedCoord(point.getAffineYCoord().toBigInteger().toByteArray(), keySizeBytes));

		return uncompressedECPointNode;
	}

	public static byte[] toUncompressedCoord(final byte[] coord, int keySizeBytes) {
		final byte[] uncompressedPoint = new byte[keySizeBytes];

		if (coord.length <= keySizeBytes) {
			return coord;
		} else if ((coord.length == keySizeBytes + 1) && (coord[0] == 0)) {
			System.arraycopy(coord, 1, uncompressedPoint, 0, keySizeBytes);
			return uncompressedPoint;
		} else {
			throw new IllegalStateException("coord value is too large");
		}
	}

   	public PublicKey decodePublicKey(byte[] encodedPublicKey) throws SignatureException {
        X9ECParameters curve = SECNamedCurves.getByName("secp256r1");
        org.bouncycastle.math.ec.ECPoint point = curve.getCurve().decodePoint(encodedPublicKey);

        try {
			return KeyFactory.getInstance("ECDSA").generatePublic(
			        new org.bouncycastle.jce.spec.ECPublicKeySpec(point,
			                new org.bouncycastle.jce.spec.ECParameterSpec(
			                        curve.getCurve(),
			                        curve.getG(),
			                        curve.getN(),
			                        curve.getH()
			                )
			        )
			);
		} catch (GeneralSecurityException ex) {
			throw new SignatureException(ex);
		}
}

}
