/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.comp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEDecrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jwt.EncryptedJWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.RSAKeyFactory;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwe.Jwe;
import io.jans.as.model.jwe.JweDecrypterImpl;
import io.jans.as.model.jwe.JweEncrypterImpl;
import io.jans.as.model.jwk.Algorithm;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.jws.RSASigner;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.util.Base64Util;
import io.jans.as.server.model.token.JwtSigner;
import org.apache.commons.codec.Charsets;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.PublicJsonWebKey;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * CrossEncryptionTest Unit Tests.
 */
public class CrossEncryptionTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private final String encryptedJweProducedByGluu = "eyJraWQiOiIxIiwidHlwIjoiSldUIiwiZW5jIjoiQTEyOEdDTSIsImFsZyI6IlJTQS1PQUVQIn0.bnWzspu4G40jEAkOjV-yRsXnHhgy7MgHxDKHc_ePWqCji-rNfeViybYV62jSCGRWsRB1sGiLuiE35z8aag4dr1gIbYARfNB7t9kaBcZCfZ-jwaGUYn-XfCDg98U4VVv1P77R8Gu-OcU53vBM_pPCzOm75IelWf_W8wFK4DB6i9P8CDFVlsDSWslMfqsMZLj9lE0KV_10c2ovELzcTu-GPC-rMUglFSHIt8Povi7bFf-kiWxFd1kT0NdrnHmKUVqIRNv5fsAtbY5B7jx5-EQ_IjhdaoK0QwfaqF0Vz4qVOO7y1PSXdDXyvrLwSY8rrTjzaLbXCnLc9oLeiIP-aR3HuA.YB2_esWvrHdJh1jt.P56SeJfBlBDm73YVQsEH_8ZtBgwQpnpX0hKY7v2ufFuqAlP2BeR2Ku-3rgIhFHPOAhqRuZ-YOROwIUVfC9ceG0tI63W_Xf0.FyuoL4LlnBvPEnmCJ5H8pw";

    private final String senderJwkJson = "{\"kty\":\"RSA\",\"d\":\"iSx-zxihgOITpEhz6WwGiiCZjxx597wqblhSYgFWa_bL9esLY3FT_Kq9sdvGPiI8QmObRxPZuTi4n3BVKYUWcfjVz3swq7VmESxnJJZE-vMI9NTaZ-CT2b4I-c3qwAsejhWagJf899I3MRtPOnyxMimyOw4_5YYvXjBkXkCMfCsbj5TBR3RbtMrUYzDMXsVT1EJ_7H76DPBFJx5JptsEAA17VMtqwvWhRutnPyQOftDGPxD-1aGgpteKOUCv7Lx-mFX-zV6nnPB8vmgTgaMqCbCFKSZI567p714gzWBkwnNdRHleX8wos8yZAGbdwGqqUz5x3iKKdn3c7U9TTU7DAQ\",\"e\":\"AQAB\",\"use\":\"sig\",\"kid\":\"1\",\"alg\":\"RS256\",\"n\":\"i6tdK2fREwykTUU-qkYkiSHgg9B31-8EjVCbH0iyrewY9s7_WYPT7I3argjcmiDkufnVfGGW0FadtO3br-Qgk_N2e9LqGMtjUoGMZKFS3fJhqjnLYDi_E5l2FYU_ilw4EXPsZJY0CaM7BxjwUBoCjopYrgvtdxA9G6gpGoAH4LopAkgX-gkawVLpB4NpLvA09FLF2OlYZL7aaybvM2Lz_IXEPa-LSOwLum80Et-_A1-YMx_Z767Iwl1pGTpgZ87jrDD1vEdMdiLcWFG3UIYAAIxtg6X23cvQVLMaXKpyV0USDCWRJrZYxEDgZngbDRj3Sd2-LnixPkMWAfo_D9lBVQ\"}";
    private final String recipientJwkJson = "{\"kty\":\"RSA\",\"d\":\"jAFM0c4oXxh5YcEujZRVY5LNUzkm0OZf8OUZ31DockQE07BwSAsi4_y6vursS4Z74EurjYlfPx7WoZZokTLyBReVvG8XQZ-AQ5smU9gXQrsiVdU2kOp17oYnOP3OKc0HtvlfTPKdz0DhoA--wAsPFCL2ei4Qly_J3IQTF9ffJJMEyzgabcV1xqrk8NEK5XfEHOdNHzzg-doRe4lCsDcEfIppCIxPHTozhYpwH0_OrssAX1OwX5Jx6-5pXc_BIBrymIkjfwlPYBC32f0iD6VTntJfIngMOdeu0t6krOaWlbfmf6RdoM5sugT-j3mYnd3w4c2eFW23Z9sPCrQvDNlTcQ\",\"e\":\"AQAB\",\"use\":\"enc\",\"kid\":\"2\",\"alg\":\"RS256\",\"n\":\"oaPsFKHgVnK0d04rjN5GgZFqCh9HwYkLMdDQDIgkM3x4sxTpctS5NJQK7iKWNxPTtULdzrY6NLqtrNWmIrJFC6f2h4q5p46Kmc8vdhm_Ph_jpYfsXWTdsHAoee6iJPMoie7rBGoscr3y2DdNlyxAO_jHLUkaaSAqDQrH_f4zVTO0XKisJu8DxKoh2U8myOow_kxx4PUxEdlH6XclpxYT5lIZijOZ8wehFad_BAJ2iZM40JDoqOgspUF1Jyq7FjOoMQabYYwDMyfs2rEALcTU1UsvLeWbl95T3mdAw64Ux3uFCZzHdXF4IDr7xH4NrEVT7SMAlwNoaRfmFbtL-WoISw\"}";

    public static final String PAYLOAD = "{\"iss\":\"https:devgluu.saminet.local\",\"sub\":\"testing\"}";

    private final String rsa1JwkJson = "{ \"kty\":\"RSA\", \"d\":\"BSOr_bbK0THvHyqE8CaPE-f26VBUFRqry47VW0MWtZyU7tGWoBNJi-hB4kxDskw7HitOpdx2zXDhQq6rg6Yv1Wn4WTHSFtQ1_vEJaCOunN1SejrhfEz1eFrADuCyOUXflrUduhymvWGltIgd000kib3QvwvsIft597wqfW5kDds_JgnTILnk-UkKdVx58SGhkgkcUI8uo9BSN5MMGgqEUHY_orqMa-oVWy5VsOAMU22m2ZuPhEZa4uOH4xCRkRtid7LlgzWCOncAAhM4trspKM60Zntq1m6D7ir_bDD1qxpEWYOvEH1UhvUcQw8UKYxGZ3VwscSBlTxQ15mNiDi-wQ\", \"e\":\"AQAB\", \"use\":\"enc\", \"use\":\"sig\", \"kid\":\"1\", \"alg\":\"RS256\", \"n\":\"ALPsdlfm6QYerQFRhL2-QOJUn-a70JsuElspspyBXRZUllhpxSaNWZlouT0DAf3NUmEm8wgVbJF1-exMVaaF8kUrdGYKS5N1uDnZwc1G8MIt29YQ6xoLLqzihJlD-syYL6tPYCr97a4AbA8EssrMjynk_WIBh18gRESq-I9vyvlYyjRfZ9ey185ERGpMfm0d4Mouttl2nc3VRfuuLIstQ4ylKvMjnWtuAvBdwF7jJSIalDUjxIA9kkg6dH2e0ZNyDlN8-14VkeAaC73f-va0kBrD9bPIUSuaaIkmgN0lKwR_xH1Y7vG2xs45R1d_cVTSo-yXhPgKLphPXflPkMMXBs0\" }";
    private final String rsa2JwkJson = "{ \"kty\":\"RSA\", \"d\":\"BCS_6xY0EZQ9jcusklPvP97Ydvo7I7kdb3na3b7HPzxRsfP2NJluz3noPydH7RtY2H1osEy4TJVHZRHtH00DjUAB-dk1KGkNNpGBl51uNZgQ9L0hbz_EvSdXQoNNaCOXhXjeOoM3P_keH4ztJMpLIvI5E4SDVA7zuze24HVNOijIR0NwulhtcdblmkbsKVU0lCvDK2eJnVF-rSEk4Vor4w-bA4hzEMof79W26VsdQVGs9Yc6p1zWiefe9dTo_hIploXTxgF4D_8na1vyHZu5xrtBqK87DDAq9RK70uC3OPVE2lGSjnlP-MeTsbW_fFKK_BV9-0dAloTM85-D_aOhgQ\", \"e\":\"AQAB\", \"use\":\"enc\", \"use\":\"sig\", \"kid\":\"1\", \"alg\":\"RS256\", \"n\":\"AM4kBzkoQcWAWYd-YxwVCQfr2siOZz8DVZ70iUAPevM4JgMsFFHO6eSsg1DAFIuEyMcsy7ucGaVb-sGNYanfY2tx25EzoPZS-KvMukIkXudPAXO1ii3WR_PWXR8So-hbI_a_bHJJ7fa-VaLLiSSvJMZ7mXjLb4cuFKHV6cAI4M1aX-SvR3Y-VvJBSLIwuW7AVtaVgiO24YIlbegfNnx2iHxu-ZWcl5fdbgrLfWRVD29udwCQqiCosMBL6Yfvax-_H4Q9-ir6sna5eJSLCPWuBiwdOqE6Y3eZQxVoY2CCVlVjWY8UXQra8RPMcwAvdOCbpNPuY4Wq7b1T18l-e79xtA8\" }";;

    private final String encryptedRsa2Jwe = "eyJraWQiOiIxIiwidHlwIjoiSldUIiwiZW5jIjoiQTEyOENCQytIUzI1NiIsImFsZyI6IlJTQTFfNSJ9.jzdQDZMZJEBb1v-N2DcSg1k0j8wPMGLhWRhIsFvEpS5A7JyKfEY2fkptWDStB_sEl4uZKODuN6WCmNO6ESetYJq0a2BIS_M5MurXPXLEXZey96PJK1h9EWl-Mi-HgEYGS_56EFag3n-87JEPbyG-v65sk7Z6sHm4ti0azf5WPUqskhBEe1YgdgPaZKfLq-hWJ11teFt3vD-xxYNXOmbrGV3RrV-BEtzh69O87Ik_kkhCsc_Jlul2AxXDBJAhJhy_2bVPuXS1WUoEJ6UuWEj-us20OS2H2BuTU8Xh7k9TtHbsx_XF7qe7Syey3A1ET_7T-r922OZJDmHoJlCrEqp3rQ.0UKKw6CuiHOFMHbcENGo4w.7RYTtNPmdCFcsu2yDzjMggMUBe1eUgPLmz84O6QACAJjT4wJ8vTHZwMSUvJoCEv9yQYoMSy5cHXO2JiLGQ3U0CTIAiuF_viMbQPudADJENQ.poaAuMG83LFk2oCREptmamh6uQvbiy2WCqB6WSKzdWk";

    private final String ec1PrvJwkJson = "{ \"kty\":\"EC\", \"crv\":\"P-256\", \"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\", \"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\", \"d\":\"870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE\", \"use\":\"enc\", \"kid\":\"3\" }";
    private final String ec1PubJwkJson = "{ \"kty\":\"EC\", \"crv\":\"P-256\", \"x\":\"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4\", \"y\":\"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM\", \"use\":\"enc\", \"kid\":\"3\" }";
    
    private final String ec2PrvJwkJson = "{ \"kty\":\"EC\", \"crv\":\"P-256\", \"x\":\"MBiqPPePV0UDsvPS6PC9tC6ZikJP3o4sRFnlIQTX5Mw\", \"y\":\"AJLmKWvp8GCEnuFtodk0feyeW2FS4T_Ok9zGc1xVVMrU\", \"d\":\"PwBwLX_3nQozA0t2DKDH0K28re9O2cvxOZkS212zfYk\", \"use\":\"enc\", \"kid\":\"3\" }";
    
    private final String encryptedEc2Jwe = "eyJlcGsiOnsia3R5IjoiRUMiLCJjcnYiOiJQLTI1NiIsIngiOiJRc0FEbjZtalBRWW0zRnJWaHZqbi1Gc0s5dlpOLTlMaWg3eFY1blVtQTRvIiwieSI6IlVsWnBIT3dfbUl6TWdhUllXQmNlaVQ4Yl9NM2VnS0s4ODdtX2xXcE42OW8ifSwia2lkIjoiMSIsInR5cCI6IkpXVCIsImVuYyI6IkExMjhDQkMtSFMyNTYiLCJhbGciOiJFQ0RILUVTIn0..4y_NFicmz3pAjzpKo0RFGw._BRu1vhk5WiAGQUZ51v2ykC6nDpBGCG2NWwfJNePt_krLcYJ3Paqa67nuRN8f8Yfzify1q5v3oTBsaAJRu9zx5oocCI6oiWQewgFlz-CThc.mc-8GKaH105ZY2Syi8gLoQ";

    private final String aes128_1JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A128KW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhg\" }";
    private final String aes192_1JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A192KW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhsgXfmbScFAt\" }";
    private final String aes256_1JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A256KW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhsgXfmbScFAtw3Kpqkrudq0\" }";
    @SuppressWarnings("unused")
    private final String aes384_1JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A384KW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhsgXfmbScFAtw3Kpqkrudq2M3T9YqsQdtoZrl1Yfn8JK\" }";
    @SuppressWarnings("unused")
    private final String aes512_1JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A512KW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhsgXfmbScFAtw3Kpqkrudq2M3T9YqsQdtoZrl1Yfn8JKzk8EeKHaHZY9Qj49CFIf8g\" }";

    private final String aes128_2JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A128KW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uoQ\" }";
    private final String aes192_2JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A192KW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uocqDQ-DcvFYC\" }";
    private final String aes256_2JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A256KW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uocqDQ-DcvFYCjls8w5D0fq8\" }";
    @SuppressWarnings("unused")
    private final String aes384_2JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A384KW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uocqDQ-DcvFYCjls8w5D0fq-x0G75b9rN0EZVuJ0vcZs8\" }";
    @SuppressWarnings("unused")
    private final String aes512_2JwkJson = "{ \"kty\":\"oct\", \"alg\":\"A512KW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uocqDQ-DcvFYCjls8w5D0fq-x0G75b9rN0EZVuJ0vcZs84r7y9hP67Ip0wg1i-dv-WA\" }";

    private final String aes128_1GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A128GCMKW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhg\" }";
    private final String aes192_1GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A192GCMKW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhsgXfmbScFAt\" }";
    private final String aes256_1GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A256GCMKW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhsgXfmbScFAtw3Kpqkrudq0\" }";
    @SuppressWarnings("unused")
    private final String aes384_1GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A384GCMKW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhsgXfmbScFAtw3Kpqkrudq2M3T9YqsQdtoZrl1Yfn8JK\" }";
    @SuppressWarnings("unused")
    private final String aes512_1GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A512GCMKW\", \"k\":\"bcDF5_XQSpDPnGXR6RyDhsgXfmbScFAtw3Kpqkrudq2M3T9YqsQdtoZrl1Yfn8JKzk8EeKHaHZY9Qj49CFIf8g\" }";

    private final String aes128_2GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A128GCMKW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uoQ\" }";
    private final String aes192_2GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A192GCMKW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uocqDQ-DcvFYC\" }";
    private final String aes256_2GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A256GCMKW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uocqDQ-DcvFYCjls8w5D0fq8\" }";
    @SuppressWarnings("unused")
    private final String aes384_2GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A384GCMKW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uocqDQ-DcvFYCjls8w5D0fq-x0G75b9rN0EZVuJ0vcZs8\" }";
    @SuppressWarnings("unused")
    private final String aes512_2GCMKJwkJson = "{ \"kty\":\"oct\", \"alg\":\"A512GCMKW\", \"k\":\"kGZk_EIKv-WmGgQ2nR0uocqDQ-DcvFYCjls8w5D0fq-x0G75b9rN0EZVuJ0vcZs84r7y9hP67Ip0wg1i-dv-WA\" }";

    private final String encryptedAes2Jwe = "eyJraWQiOiIxIiwidHlwIjoiSldUIiwiZW5jIjoiQTEyOENCQytIUzI1NiIsImFsZyI6IkExMjhLVyJ9.modY7qzot_7wA7uG8Q24vGOZspwruc9SJ3ZXSkhuispncHyZdN86qg.Z7tq_hvbgZWqw90iYmypIQ.lCqKHaplNalcoZZJx9W0pf9ktXvll3-ik4C8pyZ2fOiuZb7Fq5my6LT_UeckEP-VPFhEfivZPZHGwgKTbN-3hqydbjLbIDzqcV311XdpR5A.JDaZ7gUnhiQ0KO5JPgwjqUF7dVsB0dQfFCd7FuB-PfA";

    private final String passwordValue1 = "password_1";
    private final String passwordValue2 = "password_2";

    private final String encryptedPassword2Jwe = "eyJwMnMiOiJhblZhd015eXVYUC1JOGRHdE5HbUxRIiwicDJjIjo4MTkyLCJraWQiOiIxIiwidHlwIjoiSldUIiwiZW5jIjoiQTEyOENCQytIUzI1NiIsImFsZyI6IlBCRVMyLUhTMjU2K0ExMjhLVyJ9.QC_VQ1yTQa4W-23_O2-rYeB8UrpXKiN4QWtB1eecWMhRnJijtgPhrQ.3sZIvXHjhQzxKELNIEU0tw.qCw5ukxt0G-x62XPomVZvLfRm2x9uwTZmDKDGUjb2zPBiQW8hLwRO2sPnepzplc2YVKaRFaGM-V83YqAU_F52OHVfPT0-BuuXH6TI01ukgE.o8eLTWkUfFtGo3fY-aRwsbrq3Io1gjDqW3Xiewc1eqI";

    /**
     * Key and EncryptionAlgorithm Suite.
     * Simple storage, that contains join/relation:
     * KeyEncryptionAlgorithm <---> JwkJson (correct keys (public/private))
     * Correct Asymmetric(Private/Public) or Symmetric Key
     *      RSA - Private/Public Key
     *      ECDSA - only Private Key
     *      AES - Symmetric Key
     * .
     * KeyEncryptionAlgorithm <---> JwkJson (wrong keys (public/private))
     * Wrong Asymmetric(Private/Public) or Symmetric Key
     *      Wrong Key is used for testing, that
     *      decrypting works correctly - exception should be generated
     * .
     * KeyEncryptionAlgorithm <---> JwkJson (wrong encrypt Data)
     *      Wrong Encrypt Data are used for testing,
     *      decrypting works correctly - exception should be generated
     * . 
     * KeyEncryptionAlgorithm <---> BlockEncryptionAlgorithm[] (array of BlockEncryptionAlgorithm,
     * that can be used with this KeyEncryptionAlgorithm)
     * .
     *
     * @author Sergey Manoylo
     * @version September 13, 2021
     */
    private static class KeyEncryptionAlgorithmSuite {

        public KeyEncryptionAlgorithm keyEncrAlg;   // Key Encryption Algorithm
        public String keyData1;     // Correct Asymmetric(Private/Public) or Symmetric Key
                                    // RSA - Private/Public Key
                                    // ECDSA - only Private Key
                                    // AES - Symmetric Key
        public String keyData2;     // Wrong Asymmetric(Private/Public) or Symmetric Key
                                    // Wrong Key is used for testing, that
                                    // decrypting works correctly - exception should be generated
        public String keyData3;     // Correct Public Key
                                    // ECDSA - only Public Key
        public String encData2;     // Wrong Encrypt Data are used for testing,
                                    // decrypting works correctly - exception should be generated
                                    
        public BlockEncryptionAlgorithm[] blockEncryptionAlgorithms;    // Block Encryption Algorithms,
                                                                        // that can be used with defined KeyEncryptionAlgorithm   

        /**
         * Constructor.
         * 
         * @param keyEncrAlg
         * @param keyData1
         * @param keyData2
         * @param encData2
         * @param blockEncryptionAlgorithms
         */
        public KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm keyEncrAlg, String keyData1, String keyData2,
                String keyData3, String encData2, BlockEncryptionAlgorithm[] blockEncryptionAlgorithms) {
            this.keyEncrAlg = keyEncrAlg;
            this.keyData1 = keyData1;
            this.keyData2 = keyData2;
            this.keyData3 = keyData3;
            this.encData2 = encData2;
            this.blockEncryptionAlgorithms = blockEncryptionAlgorithms;
        }
    }

    /**
     * Used BlockEncryptionAlgorithm(s).
     */
    BlockEncryptionAlgorithm[] blockEncryptionAlgorithms = { BlockEncryptionAlgorithm.A128CBC_PLUS_HS256,
            BlockEncryptionAlgorithm.A256CBC_PLUS_HS512, BlockEncryptionAlgorithm.A128CBC_HS256,
            BlockEncryptionAlgorithm.A192CBC_HS384, BlockEncryptionAlgorithm.A256CBC_HS512,
            BlockEncryptionAlgorithm.A128GCM, BlockEncryptionAlgorithm.A192GCM, BlockEncryptionAlgorithm.A256GCM, };

    /**
     * Used BlockEncryptionAlgorithm(s)
     * for KeyEncryptionAlgorithm.ECDH_ES only. 
     */
    BlockEncryptionAlgorithm[] blockEncryptionAlgorithms_ECDH_ES = { BlockEncryptionAlgorithm.A128CBC_HS256,
            BlockEncryptionAlgorithm.A192CBC_HS384, BlockEncryptionAlgorithm.A256CBC_HS512,
            BlockEncryptionAlgorithm.A128GCM, BlockEncryptionAlgorithm.A192GCM, BlockEncryptionAlgorithm.A256GCM, };

    /**
     * Definition KeyEncryptionAlgorithmSuite
     * for AlgorithmFamily.RSA family.
     */
    KeyEncryptionAlgorithmSuite[] keyEncrAlgorithmsRSA = {
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.RSA1_5, rsa1JwkJson, rsa2JwkJson, null, encryptedRsa2Jwe,
                    null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.RSA_OAEP, rsa1JwkJson, rsa2JwkJson, null, encryptedRsa2Jwe,
                    null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.RSA_OAEP_256, rsa1JwkJson, rsa2JwkJson, null,
                    encryptedRsa2Jwe, null), };

    /**
     * Definition KeyEncryptionAlgorithmSuite
     * for AlgorithmFamily.EC.
     */
    KeyEncryptionAlgorithmSuite[] keyEncrAlgorithmsECDH = {
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.ECDH_ES, ec1PrvJwkJson, ec2PrvJwkJson, ec1PubJwkJson,
                    encryptedEc2Jwe, blockEncryptionAlgorithms_ECDH_ES),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.ECDH_ES_PLUS_A128KW, ec1PrvJwkJson, ec2PrvJwkJson, ec1PubJwkJson,
                    encryptedEc2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.ECDH_ES_PLUS_A192KW, ec1PrvJwkJson, ec2PrvJwkJson, ec1PubJwkJson,
                    encryptedEc2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.ECDH_ES_PLUS_A256KW, ec1PrvJwkJson, ec2PrvJwkJson, ec1PubJwkJson,
                    encryptedEc2Jwe, null), };

    /**
     * Definition KeyEncryptionAlgorithmSuite
     * for AlgorithmFamily.AES.
     */
    KeyEncryptionAlgorithmSuite[] keyEncrAlgorithmsAES = {
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.A128KW, aes128_1JwkJson, aes128_2JwkJson, null,
                    encryptedAes2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.A192KW, aes192_1JwkJson, aes192_2JwkJson, null,
                    encryptedAes2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.A256KW, aes256_1JwkJson, aes256_2JwkJson, null,
                    encryptedAes2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.A128GCMKW, aes128_1GCMKJwkJson, aes128_2GCMKJwkJson, null,
                    encryptedAes2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.A192GCMKW, aes192_1GCMKJwkJson, aes192_2GCMKJwkJson, null,
                    encryptedAes2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.A256GCMKW, aes256_1GCMKJwkJson, aes256_2GCMKJwkJson, null,
                    encryptedAes2Jwe, null), };

    /**
     * Definition KeyEncryptionAlgorithmSuite
     * for AlgorithmFamily.PASSW.
     */
    KeyEncryptionAlgorithmSuite[] keyEncrAlgorithmsPassw = {
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.PBES2_HS256_PLUS_A128KW, passwordValue1,
                    passwordValue2,  null, encryptedPassword2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.PBES2_HS384_PLUS_A192KW, passwordValue1,
                    passwordValue2,  null, encryptedPassword2Jwe, null),
            new KeyEncryptionAlgorithmSuite(KeyEncryptionAlgorithm.PBES2_HS512_PLUS_A256KW, passwordValue1,
                    passwordValue2,  null, encryptedPassword2Jwe, null), };

    @Test
    public void encryptWithNimbus_decryptByAll() {
        final String jwt = encryptWithNimbusJoseJwt();
        System.out.println("Gluu encrypted All : " + jwt);

        assertTrue(testDecryptNimbusJoseJwt(jwt));
        assertTrue(testDecryptWithJose4J(jwt));
        assertTrue(testDecryptWithGluuDecrypter_RSA_OAEP(jwt));
    }

    @Test
    public void encryptWithGluu_RSA_OAEP_decryptByAll() {
        final String jwt = encryptWithGluuJweEncrypter_RSA_OAEP();
        System.out.println("Gluu encrypted (RSA_OAEP) : " + jwt);

        assertTrue(testDecryptNimbusJoseJwt(jwt));
        assertTrue(testDecryptWithJose4J(jwt));
        assertTrue(testDecryptWithGluuDecrypter_RSA_OAEP(jwt));
    }

    @Test
    public void testNimbusJoseJwt_first() {

        //jwe produced by gluu 3.1.2 in development environment
        assertTrue(testDecryptNimbusJoseJwt(encryptedJweProducedByGluu));
    }

    @Test
    public void testNimbusJoseJwt_second() {

        //jwe produced by Gluu JweEncrypter
        assertTrue(testDecryptNimbusJoseJwt(encryptWithGluuJweEncrypter_RSA_OAEP()));
    }

    @Test
    public void testNimbusJoseJwt_third() {

        //jwe produced by Nimbus Jose+JWT
        assertTrue(testDecryptNimbusJoseJwt(encryptWithNimbusJoseJwt()));
    }

    @Test
    public void testNimbusJose4J_first() {

        //jwe produced by gluu 3.1.2 in development environment
        assertTrue(testDecryptWithJose4J(encryptedJweProducedByGluu));
    }

    @Test
    public void testNimbusJose4J_second() {

        //jwe produced by Gluu JweEncrypter
        assertTrue(testDecryptWithJose4J(encryptWithGluuJweEncrypter_RSA_OAEP()));
    }

    @Test
    public void testNimbusJose4J_third() {

        //jwe produced by Nimbus Jose+JWT
        assertTrue(testDecryptWithJose4J(encryptWithNimbusJoseJwt()));
    }

    @Test
    public void testGluuJweDecrypter_first() {
        String str = encryptWithNimbusJoseJwt();
        System.out.println(str);
        System.out.println(encryptedJweProducedByGluu);

        //jwe produced by gluu 3.1.2 in development environment
        assertTrue(testDecryptWithGluuDecrypter_RSA_OAEP(encryptedJweProducedByGluu));
    }

    @Test
    public void testGluuJweDecrypter_second() {

        //jwe produced by Gluu JweEncrypter
        assertTrue(testDecryptWithGluuDecrypter_RSA_OAEP(encryptWithGluuJweEncrypter_RSA_OAEP()));
    }

    @Test
    public void testGluuJweDecrypter_third() {

        //jwe produced by Nimbus Jose+JWT
        assertTrue(testDecryptWithGluuDecrypter_RSA_OAEP(encryptWithNimbusJoseJwt()));
    }

    @Test
    public void encryptWithGluu_RSA_decryptByAll() {
        for (KeyEncryptionAlgorithmSuite keyEncrAlgorithmRSA : keyEncrAlgorithmsRSA) {
            for (BlockEncryptionAlgorithm blckEncrAlgorithm : blockEncryptionAlgorithms) {
                System.out.println("-----------------");
                System.out.println(
                        "Gluu encrypted (encryptWithGluu_RSA_decryptByAll):  blckEncrAlgorithm = " + blckEncrAlgorithm);
                System.out
                        .println("Gluu encrypted (encryptWithGluu_RSA_decryptByAll):  keyEncrAlgorithmRSA.keyEncrAlg = "
                                + keyEncrAlgorithmRSA.keyEncrAlg);
                System.out.println("Gluu encrypted (encryptWithGluu_RSA_decryptByAll):  keyEncrAlgorithmRSA.keyData1 = "
                        + keyEncrAlgorithmRSA.keyData1);
                System.out.println("Gluu encrypted (encryptWithGluu_RSA_decryptByAll):  keyEncrAlgorithmRSA.keyData2 = "
                        + keyEncrAlgorithmRSA.keyData2);
                System.out.println("Gluu encrypted (encryptWithGluu_RSA_decryptByAll):  keyEncrAlgorithmRSA.encData2 = "
                        + keyEncrAlgorithmRSA.encData2);
                try {
                    RSAKey rsaPublicKey = (RSAKey) (JWK.parse(keyEncrAlgorithmRSA.keyData1));

                    BlockEncryptionAlgorithm blockEncryptionAlgorithm = blckEncrAlgorithm;
                    KeyEncryptionAlgorithm keyEncryptionAlgorithm = keyEncrAlgorithmRSA.keyEncrAlg;
                    Jwe jwe = new Jwe();
                    jwe.getHeader().setType(JwtType.JWT);
                    jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
                    jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);
                    jwe.getClaims().setIssuer("https:devgluu.saminet.local");
                    jwe.getClaims().setSubjectIdentifier("testing");
                    jwe.getHeader().setKeyId("1");

                    JweEncrypterImpl encrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm,
                            rsaPublicKey.toPublicKey());
                    jwe = encrypter.encrypt(jwe);
                    System.out.println("EncodedHeader: " + jwe.getEncodedHeader());
                    System.out.println("EncodedEncryptedKey: " + jwe.getEncodedEncryptedKey());
                    System.out.println("EncodedInitializationVector: " + jwe.getEncodedInitializationVector());
                    System.out.println("EncodedCiphertext: " + jwe.getEncodedCiphertext());
                    System.out.println("EncodedIntegrityValue: " + jwe.getEncodedIntegrityValue());
                    final String jweStr = jwe.toString();

                    RSAPrivateKey rsaPrivateKey = ((RSAKey) JWK.parse(keyEncrAlgorithmRSA.keyData1)).toRSAPrivateKey();

                    JweDecrypterImpl decrypter = new JweDecrypterImpl(rsaPrivateKey);

                    decrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    decrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);
                    final String decryptedPayload = decrypter.decrypt(jweStr).getClaims().toJsonString().toString();
                    System.out.println(String.format("Gluu decrypt %s succeed: %s ", keyEncrAlgorithmRSA.keyEncrAlg,
                            decryptedPayload));

                    assertTrue(isJsonEqual(decryptedPayload, PAYLOAD));

                    try {
                        final String decryptedPayloadWrong = decrypter.decrypt(keyEncrAlgorithmRSA.encData2).getClaims()
                                .toJsonString().toString();
                        assertFalse(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                        assertTrue(false);
                    } catch (io.jans.as.model.exception.InvalidJweException e) {
                        assertTrue(true);
                    }

                    RSAPrivateKey rsaPrivateKeyWrong = ((RSAKey) JWK.parse(keyEncrAlgorithmRSA.keyData2))
                            .toRSAPrivateKey();

                    decrypter = new JweDecrypterImpl(rsaPrivateKeyWrong);

                    decrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    decrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);

                    try {
                        String decryptedPayloadWrong = decrypter.decrypt(jweStr).getClaims().toJsonString().toString();
                        assertFalse(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                        assertTrue(false);
                    } catch (io.jans.as.model.exception.InvalidJweException e) {
                        assertTrue(true);
                    }

                    String decryptedPayloadWrong = decrypter.decrypt(keyEncrAlgorithmRSA.encData2).getClaims()
                            .toJsonString().toString();
                    assertTrue(isJsonEqual(decryptedPayloadWrong, PAYLOAD));

                } catch (Exception e) {
                    System.out.println("Error (encryptWithGluu_RSA_decryptByAll) : " + " blckEncrAlgorithm = "
                            + blckEncrAlgorithm + " keyEncrAlgorithmRSA.keyEncrAlg = " + keyEncrAlgorithmRSA.keyEncrAlg
                            + " keyEncrAlgorithmRSA.keyData1 = " + keyEncrAlgorithmRSA.keyData1 + " message: "
                            + e.getMessage());
                    assertTrue(false);
                }
                System.out.println("-----------------");
            }
        }
    }

    @Test
    public void encryptWithGluu_ECDH_decryptByAll() {
        for (KeyEncryptionAlgorithmSuite keyEncrAlgorithmECDH : keyEncrAlgorithmsECDH) {
            BlockEncryptionAlgorithm[] blckEncrAlgorithms;
            if (keyEncrAlgorithmECDH.blockEncryptionAlgorithms != null) {
                blckEncrAlgorithms = keyEncrAlgorithmECDH.blockEncryptionAlgorithms;
            } else {
                blckEncrAlgorithms = blockEncryptionAlgorithms;
            }
            for (BlockEncryptionAlgorithm blckEncrAlgorithm : blckEncrAlgorithms) {
                System.out.println("-----------------");
                System.out.println("Gluu encrypted (encryptWithGluu_ECDH_decryptByAll):  blckEncrAlgorithm = "
                        + blckEncrAlgorithm);
                System.out.println(
                        "Gluu encrypted (encryptWithGluu_ECDH_decryptByAll):  keyEncrAlgorithmECDH.keyEncrAlg = "
                                + keyEncrAlgorithmECDH.keyEncrAlg);
                System.out
                        .println("Gluu encrypted (encryptWithGluu_ECDH_decryptByAll):  keyEncrAlgorithmECDH.keyData1 = "
                                + keyEncrAlgorithmECDH.keyData1);
                System.out
                        .println("Gluu encrypted (encryptWithGluu_ECDH_decryptByAll):  keyEncrAlgorithmECDH.keyData2 = "
                                + keyEncrAlgorithmECDH.keyData2);
                System.out
                        .println("Gluu encrypted (encryptWithGluu_ECDH_decryptByAll):  keyEncrAlgorithmECDH.keyData3 = "
                                + keyEncrAlgorithmECDH.keyData3);
                try {
                    ECKey ecPublicKey = (ECKey) (JWK.parse(keyEncrAlgorithmECDH.keyData3));

                    BlockEncryptionAlgorithm blockEncryptionAlgorithm = blckEncrAlgorithm;
                    KeyEncryptionAlgorithm keyEncryptionAlgorithm = keyEncrAlgorithmECDH.keyEncrAlg;

                    Jwe jwe = new Jwe();
                    jwe.getHeader().setType(JwtType.JWT);
                    jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
                    jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);
                    jwe.getClaims().setIssuer("https:devgluu.saminet.local");
                    jwe.getClaims().setSubjectIdentifier("testing");
                    jwe.getHeader().setKeyId("1");

                    JweEncrypterImpl encrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm,
                            ecPublicKey);
                    jwe = encrypter.encrypt(jwe);
                    System.out.println("EncodedHeader: " + jwe.getEncodedHeader());
                    System.out.println("EncodedEncryptedKey: " + jwe.getEncodedEncryptedKey());
                    System.out.println("EncodedInitializationVector: " + jwe.getEncodedInitializationVector());
                    System.out.println("EncodedCiphertext: " + jwe.getEncodedCiphertext());
                    System.out.println("EncodedIntegrityValue: " + jwe.getEncodedIntegrityValue());
                    final String jweStr = jwe.toString();

                    ECPrivateKey ecPrivateKey = ((ECKey) JWK.parse(keyEncrAlgorithmECDH.keyData1)).toECPrivateKey();

                    JweDecrypterImpl decrypter = new JweDecrypterImpl(ecPrivateKey);

                    decrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    decrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);
                    final String decryptedPayload = decrypter.decrypt(jweStr).getClaims().toJsonString().toString();
                    System.out.println(String.format("Gluu decrypt %s succeed: %s ", keyEncrAlgorithmECDH.keyEncrAlg,
                            decryptedPayload));

                    assertTrue(isJsonEqual(decryptedPayload, PAYLOAD));

                    try {
                        final String decryptedPayloadWrong = decrypter.decrypt(keyEncrAlgorithmECDH.encData2)
                                .getClaims().toJsonString().toString();
                        assertFalse(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                        assertTrue(false);
                    } catch (io.jans.as.model.exception.InvalidJweException e) {
                        assertTrue(true);
                    }

                    ECPrivateKey ecPrivateKeyWrong = ((ECKey) JWK.parse(keyEncrAlgorithmECDH.keyData2))
                            .toECPrivateKey();

                    decrypter = new JweDecrypterImpl(ecPrivateKeyWrong);

                    decrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    decrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);

                    try {
                        String decryptedPayloadWrong = decrypter.decrypt(jweStr).getClaims().toJsonString().toString();
                        assertFalse(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                        assertTrue(false);
                    } catch (io.jans.as.model.exception.InvalidJweException e) {
                        assertTrue(true);
                    }

                    String decryptedPayloadWrong = decrypter.decrypt(keyEncrAlgorithmECDH.encData2).getClaims()
                            .toJsonString().toString();
                    assertTrue(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                } catch (Exception e) {
                    String message = "Error (encryptWithGluu_ECDH_decryptByAll) : " + " blckEncrAlgorithm = "
                            + blckEncrAlgorithm + " keyEncrAlgorithmECDH.keyEncrAlg = "
                            + keyEncrAlgorithmECDH.keyEncrAlg + " keyEncrAlgorithmECDH.keyData1 = "
                            + keyEncrAlgorithmECDH.keyData1 + " message: " + e.getMessage();
                    System.out.println(message);
                    assertTrue(false, message);
                }
                System.out.println("-----------------");
            }
        }
    }

    @Test
    public void encryptWithGluu_AES_decryptByAll()
            throws ParseException, JOSEException, InvalidJweException, InvalidJwtException, IOException {
        for (KeyEncryptionAlgorithmSuite keyEncrAlgorithmAES : keyEncrAlgorithmsAES) {
            for (BlockEncryptionAlgorithm blckEncrAlgorithm : blockEncryptionAlgorithms) {
                System.out.println("-----------------");
                System.out.println(
                        "Gluu encrypted (encryptWithGluu_AES_decryptByAll):  blckEncrAlgorithm = " + blckEncrAlgorithm);
                System.out
                        .println("Gluu encrypted (encryptWithGluu_AES_decryptByAll):  keyEncrAlgorithmAES.keyEncrAlg = "
                                + keyEncrAlgorithmAES.keyEncrAlg);
                System.out.println("Gluu encrypted (encryptWithGluu_AES_decryptByAll):  keyEncrAlgorithmAES.keyData1 = "
                        + keyEncrAlgorithmAES.keyData1);
                System.out.println("Gluu encrypted (encryptWithGluu_AES_decryptByAll):  keyEncrAlgorithmAES.keyData2 = "
                        + keyEncrAlgorithmAES.keyData2);
                try {
                    OctetSequenceKey aesKey = (OctetSequenceKey) (JWK.parse(keyEncrAlgorithmAES.keyData1));

                    BlockEncryptionAlgorithm blockEncryptionAlgorithm = blckEncrAlgorithm;
                    KeyEncryptionAlgorithm keyEncryptionAlgorithm = keyEncrAlgorithmAES.keyEncrAlg;
                    Jwe jwe = new Jwe();
                    jwe.getHeader().setType(JwtType.JWT);
                    jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
                    jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);
                    jwe.getClaims().setIssuer("https:devgluu.saminet.local");
                    jwe.getClaims().setSubjectIdentifier("testing");
                    jwe.getHeader().setKeyId("1");

                    JweEncrypterImpl encrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm,
                            aesKey.toByteArray());
                    jwe = encrypter.encrypt(jwe);
                    System.out.println("EncodedHeader: " + jwe.getEncodedHeader());
                    System.out.println("EncodedEncryptedKey: " + jwe.getEncodedEncryptedKey());
                    System.out.println("EncodedInitializationVector: " + jwe.getEncodedInitializationVector());
                    System.out.println("EncodedCiphertext: " + jwe.getEncodedCiphertext());
                    System.out.println("EncodedIntegrityValue: " + jwe.getEncodedIntegrityValue());
                    final String jweStr = jwe.toString();
                    System.out.println("jweStr = " + jweStr);

                    aesKey = (OctetSequenceKey) (JWK.parse(keyEncrAlgorithmAES.keyData1));

                    JweDecrypterImpl decrypter = new JweDecrypterImpl(aesKey.toByteArray());

                    decrypter.setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.A128KW);
                    decrypter.setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.A128GCM);
                    final String decryptedPayload = decrypter.decrypt(jweStr).getClaims().toJsonString().toString();
                    System.out.println(String.format("Gluu decrypt %s succeed: %s ", keyEncrAlgorithmAES.keyEncrAlg,
                            decryptedPayload));

                    assertTrue(isJsonEqual(decryptedPayload, PAYLOAD));

                    try {
                        final String decryptedPayloadWrong = decrypter.decrypt(keyEncrAlgorithmAES.encData2).getClaims()
                                .toJsonString().toString();
                        assertFalse(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                        assertTrue(false);
                    } catch (io.jans.as.model.exception.InvalidJweException e) {
                        assertTrue(true);
                    }

                    OctetSequenceKey aesKeyWrong = (OctetSequenceKey) (JWK.parse(keyEncrAlgorithmAES.keyData2));

                    decrypter = new JweDecrypterImpl(aesKeyWrong.toByteArray());

                    decrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    decrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);

                    try {
                        String decryptedPayloadWrong = decrypter.decrypt(jweStr).getClaims().toJsonString().toString();
                        assertFalse(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                        assertTrue(false);
                    } catch (io.jans.as.model.exception.InvalidJweException e) {
                        assertTrue(true);
                    }

                } catch (Exception e) {
                    String message = "Error (encryptWithGluu_AES_decryptByAll) : " + " blckEncrAlgorithm = "
                            + blckEncrAlgorithm + " keyEncrAlgorithmAES.keyEncrAlg = " + keyEncrAlgorithmAES.keyEncrAlg
                            + " keyEncrAlgorithmAES.keyData1 = " + keyEncrAlgorithmAES.keyData1 + " message: "
                            + e.getMessage();
                    System.out.println(message);
                    assertTrue(false, message);
                }
                System.out.println("-----------------");
            }
        }
    }

    @Test
    public void encryptWithGluu_Password_decryptByAll() {
        for (KeyEncryptionAlgorithmSuite keyEncrAlgorithmPassw : keyEncrAlgorithmsPassw) {
            for (BlockEncryptionAlgorithm blckEncrAlgorithm : blockEncryptionAlgorithms) {
                System.out.println("-----------------");
                System.out.println("Gluu encrypted (encryptWithGluu_Password_decryptByAll):  blckEncrAlgorithm = "
                        + blckEncrAlgorithm);
                System.out.println(
                        "Gluu encrypted (encryptWithGluu_Password_decryptByAll):  keyEncrAlgorithmAES.keyEncrAlg = "
                                + keyEncrAlgorithmPassw.keyEncrAlg);
                System.out.println(
                        "Gluu encrypted (encryptWithGluu_Password_decryptByAll):  keyEncrAlgorithmAES.keyData1 = "
                                + keyEncrAlgorithmPassw.keyData1);
                System.out.println(
                        "Gluu encrypted (encryptWithGluu_Password_decryptByAll):  keyEncrAlgorithmAES.keyData2 = "
                                + keyEncrAlgorithmPassw.keyData2);
                try {

                    BlockEncryptionAlgorithm blockEncryptionAlgorithm = blckEncrAlgorithm;
                    KeyEncryptionAlgorithm keyEncryptionAlgorithm = keyEncrAlgorithmPassw.keyEncrAlg;

                    Jwe jwe = new Jwe();
                    jwe.getHeader().setType(JwtType.JWT);
                    jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
                    jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);
                    jwe.getClaims().setIssuer("https:devgluu.saminet.local");
                    jwe.getClaims().setSubjectIdentifier("testing");
                    jwe.getHeader().setKeyId("1");

                    JweEncrypterImpl encrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm,
                            keyEncrAlgorithmPassw.keyData1);
                    jwe = encrypter.encrypt(jwe);
                    System.out.println("EncodedHeader: " + jwe.getEncodedHeader());
                    System.out.println("EncodedEncryptedKey: " + jwe.getEncodedEncryptedKey());
                    System.out.println("EncodedInitializationVector: " + jwe.getEncodedInitializationVector());
                    System.out.println("EncodedCiphertext: " + jwe.getEncodedCiphertext());
                    System.out.println("EncodedIntegrityValue: " + jwe.getEncodedIntegrityValue());
                    final String jweStr = jwe.toString();

                    JweDecrypterImpl decrypter = new JweDecrypterImpl(keyEncrAlgorithmPassw.keyData1);

                    decrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    decrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);
                    final String decryptedPayload = decrypter.decrypt(jweStr).getClaims().toJsonString().toString();

                    assertTrue(isJsonEqual(decryptedPayload, PAYLOAD));

                    try {
                        final String decryptedPayloadWrong = decrypter.decrypt(keyEncrAlgorithmPassw.encData2)
                                .getClaims().toJsonString().toString();
                        assertFalse(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                        assertTrue(false);
                    } catch (io.jans.as.model.exception.InvalidJweException e) {
                        assertTrue(true);
                    }

                    decrypter = new JweDecrypterImpl(keyEncrAlgorithmPassw.keyData2);

                    decrypter.setKeyEncryptionAlgorithm(keyEncryptionAlgorithm);
                    decrypter.setBlockEncryptionAlgorithm(blockEncryptionAlgorithm);

                    try {
                        String decryptedPayloadWrong = decrypter.decrypt(jweStr).getClaims().toJsonString().toString();
                        assertFalse(isJsonEqual(decryptedPayloadWrong, PAYLOAD));
                        assertTrue(false);
                    } catch (io.jans.as.model.exception.InvalidJweException e) {
                        assertTrue(true);
                    }

                } catch (Exception e) {
                    String message = "Error (encryptWithGluu_Password_decryptByAll) : " + " blckEncrAlgorithm = "
                            + blckEncrAlgorithm + " keyEncrAlgorithmPassw.keyEncrAlg = "
                            + keyEncrAlgorithmPassw.keyEncrAlg + " keyEncrAlgorithmPassw.keyData1 = "
                            + keyEncrAlgorithmPassw.keyData1 + " message: " + e.getMessage();
                    System.out.println(message);
                    assertTrue(false, message);
                }
                System.out.println("-----------------");
            }
        }
    }

    private String encryptWithGluuJweEncrypter_RSA_OAEP() {
        try {
            RSAKey recipientPublicJWK = (RSAKey) (JWK.parse(recipientJwkJson));

            BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.A128GCM;
            KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.RSA_OAEP;
            Jwe jwe = new Jwe();
            jwe.getHeader().setType(JwtType.JWT);
            jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
            jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);
            jwe.getClaims().setIssuer("https:devgluu.saminet.local");
            jwe.getClaims().setSubjectIdentifier("testing");
            jwe.getHeader().setKeyId("1");

            JweEncrypterImpl encrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, recipientPublicJWK.toPublicKey());
            jwe = encrypter.encrypt(jwe);
            System.out.println("EncodedHeader: " + jwe.getEncodedHeader());
            System.out.println("EncodedEncryptedKey: " + jwe.getEncodedEncryptedKey());
            System.out.println("EncodedInitializationVector: " + jwe.getEncodedInitializationVector());
            System.out.println("EncodedCiphertext: " + jwe.getEncodedCiphertext());
            System.out.println("EncodedIntegrityValue: " + jwe.getEncodedIntegrityValue());
            return jwe.toString();
        } catch (Exception e) {
            System.out.println("Error encryption with Gluu JweEncrypter: " + e.getMessage());
            return null;
        }
    }

    private String encryptWithNimbusJoseJwt() {
        try {
            RSAKey senderJWK = (RSAKey) JWK.parse(senderJwkJson);

            RSAKey recipientPublicJWK = (RSAKey) (JWK.parse(recipientJwkJson));

            // Create JWT
//          SignedJWT signedJWT = new SignedJWT(
//              new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(senderJWK.getKeyID()).build(),
//              new JWTClaimsSet.Builder()
//                  .subject("testi")
//                  .issuer("https:devgluu.saminet.local")
//                  .build());

            // Sign the JWT
            // signedJWT.sign(new RSASSASigner(senderJWK));

            // Create JWE object with signed JWT as payload
            // JWEObject jweObject = new JWEObject(
            // new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A128GCM)
            // .contentType("JWT") // required to indicate nested JWT
            // .build(),
            // new Payload(signedJWT));

            @SuppressWarnings("deprecation")
            JWEObject jweObject = new JWEObject(
                    new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A128GCM)
                            .type(JOSEObjectType.JWT)
                            .keyID(senderJWK.getKeyID())
                            .build(),
                    new Payload(Base64Util.base64urlencode(PAYLOAD.getBytes(Charsets.UTF_8))));

            // Encrypt with the recipient's public key
            RSAEncrypter encrypter = new RSAEncrypter(recipientPublicJWK);
            jweObject.encrypt(encrypter);

            // System.out.println("Header: " + jweObject.getHeader());
            // System.out.println("Encrypted Key: " + jweObject.getEncryptedKey());
            // System.out.println("Cipher Text: " + jweObject.getCipherText());
            // System.out.println("IV: " + jweObject.getIV());
            // System.out.println("Auth Tag: " + jweObject.getAuthTag());

            // Serialise to JWE compact form
            return jweObject.serialize();
        } catch (Exception e) {
            System.out.println("Error encryption with Nimbus: " + e.getMessage());
            return null;
        }
    }

    private boolean testDecryptNimbusJoseJwt(String jwe) {
        try {
            EncryptedJWT encryptedJwt = EncryptedJWT.parse(jwe);
            // EncryptedJWT encryptedJwt = EncryptedJWT.parse(encryptWithGluu());
            // EncryptedJWT encryptedJwt = EncryptedJWT.parse(encryptWithNimbus());

            JWK jwk = JWK.parse(recipientJwkJson);
            RSAPrivateKey rsaPrivateKey = ((RSAKey) jwk).toRSAPrivateKey();

            JWEDecrypter decrypter = new RSADecrypter(rsaPrivateKey);
            decrypter.getJCAContext().setProvider(BouncyCastleProviderSingleton.getInstance());

            encryptedJwt.decrypt(decrypter);
            final String decryptedPayload = new String(
                    Base64Util.base64urldecode(encryptedJwt.getPayload().toString()));
            System.out.println("Nimbusds decrypt JoseJwt succeed: " + decryptedPayload);
            return isJsonEqual(decryptedPayload, PAYLOAD);
        } catch (Exception e) {
            System.out.println("Nimbusds decrypt JoseJwt failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isJsonEqual(String json1, String json2) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tree1 = mapper.readTree(json1);
        JsonNode tree2 = mapper.readTree(json2);
        return tree1.equals(tree2);
    }

    private boolean testDecryptWithJose4J(String jwe) {
        try {

            PublicJsonWebKey jwk = PublicJsonWebKey.Factory.newPublicJwk(recipientJwkJson);

            JsonWebEncryption receiverJwe = new JsonWebEncryption();

            AlgorithmConstraints algConstraints = new AlgorithmConstraints(ConstraintType.WHITELIST,
                    KeyManagementAlgorithmIdentifiers.RSA_OAEP);
            receiverJwe.setAlgorithmConstraints(algConstraints);
            AlgorithmConstraints encConstraints = new AlgorithmConstraints(ConstraintType.WHITELIST,
                    ContentEncryptionAlgorithmIdentifiers.AES_128_GCM);
            receiverJwe.setContentEncryptionAlgorithmConstraints(encConstraints);

            receiverJwe.setKey(jwk.getPrivateKey());

            receiverJwe.setCompactSerialization(jwe);
            final String decryptedPayload = new String(Base64Util.base64urldecode(receiverJwe.getPlaintextString()));
            System.out.println("Jose4j decrypt Jose4J succeed: " + decryptedPayload);
            return isJsonEqual(decryptedPayload, PAYLOAD);
        } catch (Exception e) {
            System.out.println("Jose4j decrypt Jose4J failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean testDecryptWithGluuDecrypter_RSA_OAEP(String jwe) {
        try {
            JWK jwk = JWK.parse(recipientJwkJson);
            RSAPrivateKey rsaPrivateKey = ((RSAKey) jwk).toRSAPrivateKey();

            JweDecrypterImpl decrypter = new JweDecrypterImpl(rsaPrivateKey);

            decrypter.setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.RSA_OAEP);
            decrypter.setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.A128GCM);
            final String decryptedPayload = decrypter.decrypt(jwe).getClaims().toJsonString().toString();
            System.out.println("Gluu decrypt RSA_OAEP succeed: " + decryptedPayload);
            return isJsonEqual(decryptedPayload, PAYLOAD);
        } catch (Exception e) {
            System.out.println("Gluu decrypt RSA_OAEP failed: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Test
    public void nestedJWT() throws Exception {

        RSAKey senderJWK = (RSAKey) JWK.parse(senderJwkJson);

        RSAKey recipientPublicJWK = (RSAKey) (JWK.parse(recipientJwkJson));

        // Create JWT
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(senderJWK.getKeyID()).build(),
                new JWTClaimsSet.Builder()
                        .subject("testing")
                        .issuer("https:devgluu.saminet.local")
                        .build());

        signedJWT.sign(new RSASSASigner(senderJWK));

        JWEObject jweObject = new JWEObject(
                new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP, EncryptionMethod.A128GCM)
                        .contentType("JWT") // required to indicate nested JWT
                        .build(),
                new Payload(signedJWT));

        // Encrypt with the recipient's public key
        RSAEncrypter encrypter = new RSAEncrypter(recipientPublicJWK);
        jweObject.encrypt(encrypter);

        final String jweString = jweObject.serialize();

        decryptAndValidateSignatureWithNimbus(jweString);
        decryptAndValidateSignatureWithGluu(jweString);
    }

    @Test
    public void nestedJWTProducedByGluu() throws Exception {
        AppConfiguration appConfiguration = new AppConfiguration();

        List<JSONWebKey> keyArrayList = new ArrayList<JSONWebKey>();
        keyArrayList.add(getSenderWebKey());

        JSONWebKeySet keySet = new JSONWebKeySet();
        keySet.setKeys(keyArrayList);

        final JwtSigner jwtSigner = new JwtSigner(appConfiguration, keySet, SignatureAlgorithm.RS256, "audience", null, new AbstractCryptoProvider() {
            @Override
            public JSONObject generateKey(Algorithm algorithm, Long expirationTime) throws CryptoProviderException {
                return null;
            }

            @Override
            public JSONObject generateKey(Algorithm algorithm, Long expirationTime, int keyLength) throws CryptoProviderException {
                return null;
            }

            @Override
            public boolean containsKey(String keyId) {
                return false;
            }

            @Override
            public String sign(String signingInput, String keyId, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws CryptoProviderException {
                try {
                    RSAPrivateKey privateKey = ((RSAKey) JWK.parse(senderJwkJson)).toRSAPrivateKey();
                    Signature signature = Signature.getInstance(signatureAlgorithm.getAlgorithm(), "BC");
                    signature.initSign(privateKey);
                    signature.update(signingInput.getBytes());

                    return Base64Util.base64urlencode(signature.sign());
                } catch (JOSEException | ParseException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e) {
                    throw new CryptoProviderException(e);
                }
            }

            @Override
            public boolean verifySignature(String signingInput, String encodedSignature, String keyId, JSONObject jwks, String sharedSecret, SignatureAlgorithm signatureAlgorithm) throws CryptoProviderException {
                return false;
            }

            @Override
            public boolean deleteKey(String keyId) throws CryptoProviderException {
                return false;
            }

            @Override
            public PrivateKey getPrivateKey(String keyId) throws CryptoProviderException {
                throw new UnsupportedOperationException("Method not implemented.");
            }

            @Override
            public PublicKey getPublicKey(String keyId) {
                throw new UnsupportedOperationException("Method not implemented.");
            }
        });
        Jwt jwt = jwtSigner.newJwt();
        jwt.getClaims().setSubjectIdentifier("testing");
        jwt.getClaims().setIssuer("https:devgluu.saminet.local");
        jwt = jwtSigner.sign();

        RSAKey recipientPublicJWK = (RSAKey) (JWK.parse(recipientJwkJson));

        BlockEncryptionAlgorithm blockEncryptionAlgorithm = BlockEncryptionAlgorithm.A128GCM;
        KeyEncryptionAlgorithm keyEncryptionAlgorithm = KeyEncryptionAlgorithm.RSA_OAEP;
        Jwe jwe = new Jwe();
        jwe.getHeader().setType(JwtType.JWT);
        jwe.getHeader().setAlgorithm(keyEncryptionAlgorithm);
        jwe.getHeader().setEncryptionMethod(blockEncryptionAlgorithm);
        jwe.getHeader().setKeyId("1");
        jwe.setSignedJWTPayload(jwt);

        JweEncrypterImpl encrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, recipientPublicJWK.toPublicKey());
        String jweString = encrypter.encrypt(jwe).toString();

        decryptAndValidateSignatureWithGluu(jweString);
        decryptAndValidateSignatureWithNimbus(jweString);
    }

    private JSONWebKey getSenderWebKey() throws JSONException {
        return JSONWebKey.fromJSONObject(new JSONObject(senderJwkJson));
    }

    public RSAPublicKey getSenderPublicKey() {
        return RSAKeyFactory.valueOf(getSenderWebKey()).getPublicKey();
    }

    private void decryptAndValidateSignatureWithGluu(String jweString) throws ParseException, JOSEException, InvalidJweException, JSONException, InvalidJwtException {
        JWK jwk = JWK.parse(recipientJwkJson);
        RSAPrivateKey rsaPrivateKey = ((RSAKey) jwk).toRSAPrivateKey();

        JweDecrypterImpl decrypter = new JweDecrypterImpl(rsaPrivateKey);

        decrypter.setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm.RSA_OAEP);
        decrypter.setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm.A128GCM);

        final Jwe jwe = decrypter.decrypt(jweString);
        assertEquals(jwe.getHeader().getContentType(), JwtType.JWT);

        final Jwt jwt = jwe.getSignedJWTPayload();

        final RSAPublicKey senderPublicKey = RSAKeyFactory.valueOf(getSenderWebKey()).getPublicKey();
        Assert.assertTrue(new RSASigner(SignatureAlgorithm.RS256, senderPublicKey).validate(jwt));

        System.out.println("Gluu decrypt and nested jwt signature verification succeed: " + jwt.getClaims().toJsonString());
    }

    private void decryptAndValidateSignatureWithNimbus(String jweString) throws ParseException, JOSEException {
        JWK jwk = JWK.parse(recipientJwkJson);
        RSAPrivateKey rsaPrivateKey = ((RSAKey) jwk).toRSAPrivateKey();

        JWEObject jweObject = JWEObject.parse(jweString);

        jweObject.decrypt(new RSADecrypter(rsaPrivateKey));
        SignedJWT signedJWT = jweObject.getPayload().toSignedJWT();

        assertNotNull("Payload not a signed JWT", signedJWT);

        RSAKey senderJWK = (RSAKey) JWK.parse(senderJwkJson);
        assertTrue(signedJWT.verify(new RSASSAVerifier(senderJWK)));

        assertEquals("testing", signedJWT.getJWTClaimsSet().getSubject());
        System.out.println("Nimbus decrypt and nested jwt signature verification succeed: " + signedJWT.getJWTClaimsSet().toJSONObject());
    }
}