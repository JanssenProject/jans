package org.xdi.oxauth.model.crypto.binding;

import org.apache.commons.codec.digest.DigestUtils;
import org.xdi.oxauth.model.util.Base64Util;

/**
 * <pre>
 * struct {
 *    TokenBindingKeyParameters key_parameters;
 *    uint16 key_length;       Length (in bytes) of the following TokenBindingID.TokenBindingPublicKey
 *    select (key_parameters) {
 *       case rsa2048_pkcs1.5:
 *       case rsa2048_pss:
 *          RSAPublicKey rsapubkey;
 *       case ecdsap256:
 *          TB_ECPoint point;
 *    } TokenBindingPublicKey;
 * } TokenBindingID;
 * </pre>
 *
 * @author Yuriy Zabrovarnyy
 */
public class TokenBindingID {

    private TokenBindingKeyParameters keyParameters;
    private byte[] publicKey;
    private byte[] raw;

    public TokenBindingID(TokenBindingKeyParameters keyParameters, byte[] publicKey, byte[] raw) {
        this.keyParameters = keyParameters;
        this.publicKey = publicKey;
        this.raw = raw;
    }

    public TokenBindingKeyParameters getKeyParameters() {
        return keyParameters;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getRaw() {
        return raw;
    }

    public byte[] sha256() {
        return DigestUtils.sha256(raw);
    }

    public String sha256base64url() {
        return Base64Util.base64urlencode(sha256());
    }
}
