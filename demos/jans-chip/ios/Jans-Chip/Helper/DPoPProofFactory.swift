//
//  DPoPProofFactory.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 27.10.2023.
//

import Foundation
import SwiftJWT
import JOSESwift

struct JansIntegrityClaims: Claims {
    var appName: String = ""
    var seq: String = ""
    var app_id: String = ""
    var app_integrity_result: String = ""
    var app_checksum: String = ""
    var jti: String = ""
    var htm: String = ""
    var htu: String = ""
    var iat: Date = Date()
}

struct JansClaims: Claims {
    let jti: String
    let htm: String
    let htu: String
    let iat: Date
}

final class DPoPProofFactory {
    
    static let shared = DPoPProofFactory()
    
    private init() {}
    
    /**
         * The function `issueDPoPJWTToken` generates a DPoP (Distributed Proof of Possession) JWT (JSON Web Token) with the
         * specified HTTP method and request URL.
         *
         * @param httpMethod The `httpMethod` parameter represents the HTTP method used in the request, such as "GET", "POST",
         *                   "PUT", etc.
         * @param requestUrl The `requestUrl` parameter is the URL of the HTTP request that you want to issue a DPoP JWT token
         *                   for. It represents the target resource or endpoint that you want to access.
         * @return The method is returning a JWT (JSON Web Token) token.
     */
    public func issueDPoPJWTToken(httpMethod: String, requestUrl: String) -> String {
        var tokenJWT = ""
        
        do {
            let (privateKey, publicKey) = try SecKey.generateKeyPair(ofSize: 3072)
            let jwk = try RSAPublicKey(publicKey: publicKey)
            
            let header = Header(typ: "dpop+jwt", jwk: jwk.jsonString() ?? "")
            let claims = JansClaims(
                jti: UUID().uuidString,
                htm: httpMethod,
                htu: requestUrl,
                iat: Date()
            )
            
            var objectJWT = JWT(header: header, claims: claims)
            
            guard let privateKeyData = privateKey.keyData else {
                return tokenJWT
            }
            
            let jwtSigner = JWTSigner.rs256(privateKey: Data(privateKeyData))
            
            tokenJWT = try objectJWT.sign(using: jwtSigner)
        } catch(let error) {
            print("Error generating JWT, reason: \(error.localizedDescription)")
        }
        
        return tokenJWT
    }
    
    /**
         * The function generates a JWT token with specified claims and signs it using an RSA256 algorithm.
         *
         * @param claims A map containing the claims to be included in the JWT token. Claims are key-value pairs that provide
         *               information about the token, such as the issuer, subject, expiration time, etc.
         * @return The method returns a JWT (JSON Web Token) token as a String.
         */
    public func issueJWTToken(claims: JansIntegrityClaims) -> String {
        var tokenJWT = ""
        var claims = claims
        
        do {
            let (privateKey, publicKey) = try SecKey.generateKeyPair(ofSize: 3072)
            let jwk = try RSAPublicKey(publicKey: publicKey)
            
            let header = Header(typ: "dpop+jwt")
            claims.jti = UUID().uuidString
            claims.iat = Date()
            
            var objectJWT = JWT(header: header, claims: claims)
            
            guard let privateKeyData = privateKey.keyData else {
                return tokenJWT
            }
            
            let jwtSigner = JWTSigner.rs256(privateKey: Data(privateKeyData))
            
            tokenJWT = try objectJWT.sign(using: jwtSigner)
        } catch(let error) {
            print("Error generating JWT, reason: \(error.localizedDescription)")
        }
        
        return tokenJWT
    }
}
