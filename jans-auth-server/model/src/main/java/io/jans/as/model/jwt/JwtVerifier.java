/**
 * 
 */
package io.jans.as.model.jwt;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

import org.bouncycastle.jcajce.provider.asymmetric.edec.BCEdDSAPublicKey;
import org.json.JSONObject;

import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.AlgorithmFamily;
import io.jans.as.model.crypto.signature.ECDSAPublicKey;
import io.jans.as.model.crypto.signature.EDDSAPublicKey;
import io.jans.as.model.crypto.signature.RSAPublicKey;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jws.ECDSASigner;
import io.jans.as.model.jws.EDDSASigner;
import io.jans.as.model.jws.HMACSigner;
import io.jans.as.model.jws.JwsSigner;
import io.jans.as.model.jws.PlainTextSignature;
import io.jans.as.model.jws.RSASigner;

/**
 * Provides signature verification, using signature from JWT.
 *
 * @author Sergey Manoylo
 * @version September 6, 2021
 */
public class JwtVerifier {

    private AbstractCryptoProvider cryptoProvider;

    private JSONObject jwks;

    /**
     * Constructor
     */
    public JwtVerifier() {
    }    

    /**
     * Constructor.
     *  
     * @param cryptoProvider crypto provider.
     * @param jwks JSON Web Keys.
     */
    public JwtVerifier(final AbstractCryptoProvider cryptoProvider, final JSONObject jwks) {
        this.cryptoProvider = cryptoProvider;
        this.jwks = jwks;
    }

    /**
     * Verifying the input JWT (token).
     *
     * @param jwt input JWT (token).
     * @param clientSecret client secret (is used by HMAC-family algorithms).  
     * @return Boolean result of the verification.
     * @throws Exception 
     */
    public boolean verifyJwt(final Jwt jwt, final String clientSecret) throws Exception {

        if (jwt == null) {
            throw new InvalidJwtException("JwtVerifyer: jwt == null (jwt isn't defined)");
        }

        String signKeyId = jwt.getHeader().getKeyId();

        SignatureAlgorithm signatureAlgorithm = jwt.getHeader().getSignatureAlgorithm();
        if (signatureAlgorithm == null) {
            throw new InvalidJwtException(
                    "JwtVerifyer: signatureAlgorithm == null (signatureAlgorithm  isn't defined)");
        }

        final AlgorithmFamily algorithmFamily = signatureAlgorithm.getFamily();

        PublicKey publicKey = getPublicKey(algorithmFamily, signKeyId);

        JwsSigner signer = null;
        switch (signatureAlgorithm.getFamily()) {
        case NONE: {
            signer = new PlainTextSignature();
            break;
        }
        case HMAC: {
            if (clientSecret == null) {
                throw new InvalidJwtException("JwtVerifyer: clientSecret == null (clientSecret isn't  defined)");
            }
            signer = new HMACSigner(signatureAlgorithm, clientSecret);
            break;
        }
        case RSA: {
            if(publicKey != null) {
                java.security.interfaces.RSAPublicKey jrsaPublicKey = (java.security.interfaces.RSAPublicKey) publicKey;
                RSAPublicKey rsaPublicKey = new RSAPublicKey(jrsaPublicKey.getModulus(), jrsaPublicKey.getPublicExponent());
                signer = new RSASigner(signatureAlgorithm, rsaPublicKey);
            }
            break;
        }
        case EC: {
            if(publicKey != null) {
                ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
                ECDSAPublicKey ecdsaPublicKey = new ECDSAPublicKey(signatureAlgorithm, ecPublicKey.getW().getAffineX(), ecPublicKey.getW().getAffineY());
                signer = new ECDSASigner(signatureAlgorithm, ecdsaPublicKey);
            }
            break;
        }
        case ED: {
            if(publicKey != null) {
                BCEdDSAPublicKey bceddsaPublicKey = (BCEdDSAPublicKey) publicKey;
                EDDSAPublicKey eddsaPublicKey = new EDDSAPublicKey(signatureAlgorithm, bceddsaPublicKey.getEncoded());
                signer = new EDDSASigner(signatureAlgorithm, eddsaPublicKey);
            }
            break;
        }
        default: {
            break;
        }
        }

        if (signer == null) {
            throw new InvalidJwtException("JwtVerifyer: signer == null (signer isn't  defined)");
        }

        return signer.validate(jwt);
    }

    /**
     * Verifying the input JWT (token).
     * 
     * @param jwt input JWT (token).
     * @return Boolean result of the verification.
     * @throws Exception
     */
    public boolean verifyJwt(final Jwt jwt) throws Exception {
        return verifyJwt(jwt, null);
    }

    /**
     * Returns public key, only if AlgorithmFamily == RSA, EC, ED
     * from jwks (JSON Web Keys).      
     * 
     * @param algorithmFamily input Algorithm Family.
     * @param signKeyId Key ID.
     * @return Public key.
     * @throws Exception
     */
    private PublicKey getPublicKey(final AlgorithmFamily algorithmFamily, final String signKeyId) throws Exception {
        PublicKey publicKey = null;
        if (AlgorithmFamily.RSA.equals(algorithmFamily) || AlgorithmFamily.EC.equals(algorithmFamily) || AlgorithmFamily.ED.equals(algorithmFamily)) {
            if (signKeyId == null) {
                throw new InvalidJwtException("JwtVerifyer: signKeyId == null (signKeyId  isn't defined)");
            }
            publicKey = cryptoProvider.getPublicKey(signKeyId, jwks, null);
            if (publicKey == null) {
                throw new InvalidJwtException("JwtVerifyer: publicKey == null (publicKey isn't  defined)");
            }
        }        
        return publicKey;
    }
}