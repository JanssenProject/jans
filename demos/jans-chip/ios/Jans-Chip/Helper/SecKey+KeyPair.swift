//
//  SecKey+KeyPair.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 27.10.2023.
//

import Foundation
import Security

public typealias KeyPair = (privateKey:SecKey, publicKey:SecKey)

extension SecKey {

    /**
     * Generates an RSA private-public key pair. Wraps `SecKeyGeneratePair()`.
     *
     * - parameter ofSize: the size of the keys in bits
     * - returns: The generated key pair.
     * - throws: A `SecKeyError` when something went wrong.
     */
    public static func generateKeyPair(ofSize bits:UInt) throws -> KeyPair {
        let pubKeyAttrs = [ kSecAttrIsPermanent as String: true ]
        let privKeyAttrs = [ kSecAttrIsPermanent as String: true ]
        let params: NSDictionary = [ kSecAttrKeyType as String : kSecAttrKeyTypeRSA as String,
                       kSecAttrKeySizeInBits as String : bits,
                       kSecPublicKeyAttrs as String : pubKeyAttrs,
                       kSecPrivateKeyAttrs as String : privKeyAttrs ]
        var pubKey: SecKey?
        var privKey: SecKey?
        let status = SecKeyGeneratePair(params, &pubKey, &privKey)
        guard status == errSecSuccess else {
            throw SecKeyError.generateKeyPairFailed(osStatus: status)
        }
        guard let pub = pubKey, let priv = privKey else {
            throw SecKeyError.generateKeyPairFailed(osStatus: nil)
        }

        try changeKeyTag(priv)
        try changeKeyTag(pub)

        return (priv, pub)
    }

    static fileprivate func changeKeyTag(_ key: SecKey) throws {
//        let query = [kSecValueRef as String: key]
//        guard let keyTag = key.keychainTag else {
//            throw SecKeyError.generateKeyPairFailed(osStatus: nil)
//        }
//        let attrsToUpdate = [kSecAttrApplicationTag as String: keyTag]
//        let status = SecItemUpdate(query as CFDictionary, attrsToUpdate as CFDictionary)
//
//        guard status == errSecSuccess else {
//            throw SecKeyError.generateKeyPairFailed(osStatus: status)
//        }
    }

    /**
     * The block size of the key. Wraps `SecKeyGetBlockSize()`.
     */
    public var blockSize: Int {
        return SecKeyGetBlockSize(self)
    }
}

/**
 * Errors related to SecKey extensions.
 */
public enum SecKeyError: Error {
    /**
     * Indicates that generating a key pair has failed. The associated osStatus is the return value
     * of `SecKeyGeneratePair`.
     *
     * - parameter osStatus: The return value of SecKeyGeneratePair. If this is `errSecSuccess`
     *                       then something else failed.
     */
    case generateKeyPairFailed(osStatus: OSStatus?)
}
