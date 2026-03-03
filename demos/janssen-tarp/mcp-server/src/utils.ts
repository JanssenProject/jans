import crypto from "crypto";

export default class Utils {

    // helper: base64url encode
    static base64url(buffer: Buffer) {
        return buffer.toString('base64')
        .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    // helper: create sha256 base64url for PKCE code challenge
    static codeChallengeFromVerifier(verifier: string) {
        const hash = crypto.createHash('sha256').update(verifier).digest();
        return Utils.base64url(hash);
    }
}