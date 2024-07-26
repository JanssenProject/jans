//
//  WebAuthnCryptography.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.07.2024.
//

import Foundation
import CommonCrypto

final class WebAuthnCryptography {
    
    private let tag = "WebAuthnCryptographyTag"
    private let algorithm: SecKeyAlgorithm = .rsaEncryptionOAEPSHA512
    
    func generateSignatureObject(alias: String) {
        let attributes: CFDictionary =
        [kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
         kSecAttrKeySizeInBits as String: 2048,
         kSecPrivateKeyAttrs as String:
            [kSecAttrIsPermanent as String: true,
             kSecAttrApplicationTag as String: tag]
        ] as CFDictionary

        var error: Unmanaged<CFError>?

        do {
            guard SecKeyCreateRandomKey(attributes, &error) != nil else {
                throw error!.takeRetainedValue() as Error
            }
            print("key created")
        } catch {
            print(error.localizedDescription)
        }

        let query: CFDictionary = [kSecClass as String: kSecClassKey,
                                   kSecAttrApplicationTag as String: tag,
                                   kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                                   kSecReturnRef as String: true] as CFDictionary

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query, &item)
        guard status == errSecSuccess else {
            print("keychain don't have private key")
            return
        }

        let privateKey = item as! SecKey

        guard let publicKey = SecKeyCopyPublicKey(privateKey),
              let publicKeyExportable = SecKeyCopyExternalRepresentation(publicKey, nil) else {
            return
        }

        //check if the public key encrypt data
        guard SecKeyIsAlgorithmSupported(publicKey, .encrypt, algorithm)
        else {
            print("not supported cryptography")
            return
        }

        let publicKeyBase64Encoded = (publicKeyExportable as Data).base64EncodedString()
        print("Crypto - public key export = \(publicKeyBase64Encoded)")

        // the keys created we can now perform a example cryptograph operation

        let textToEncryptData = alias.data(using: .utf8)!

        guard let cipherText = SecKeyCreateEncryptedData(publicKey,
                                                         algorithm,
                                                         textToEncryptData as CFData,
                                                         &error) as Data? else {
            return
        }

        print("Crypto - encrypted text \(cipherText.base64EncodedString())")

        // check if the private key can decrypt
        guard SecKeyIsAlgorithmSupported(privateKey, .decrypt, algorithm) else {
            return
        }

        //check if the text size is compatible with the key size
        guard cipherText.count == SecKeyGetBlockSize(privateKey) else {
            return
        }

        //perform decrypt, the return is Data
        guard let clearTextData = SecKeyCreateDecryptedData(privateKey,
                                                            algorithm,
                                                            cipherText as CFData,
                                                            &error) as Data? else {
            return
        }

        guard let clearText = String(data: clearTextData, encoding: .utf8) else { return }
        print("Crypto - decrypted text [\(clearText)]")

    }
    
    func isUserAuthenticationRequired(alias: String) -> Bool {
        var error: Unmanaged<CFError>?
        let textToEncryptData = alias.data(using: .utf8)!
        
        let query: CFDictionary = [kSecClass as String: kSecClassKey,
                                   kSecAttrApplicationTag as String: tag,
                                   kSecAttrKeyType as String: kSecAttrKeyTypeRSA,
                                   kSecReturnRef as String: true] as CFDictionary
        
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query, &item)
        guard status == errSecSuccess else {
            print("keychain don't have private key")
            return false
        }
        
        let privateKey = item as! SecKey

        guard let publicKey = SecKeyCopyPublicKey(privateKey),
              let publicKeyExportable = SecKeyCopyExternalRepresentation(publicKey, nil) else {
            return false
        }

        guard let cipherText = SecKeyCreateEncryptedData(publicKey,
                                                         algorithm,
                                                         textToEncryptData as CFData,
                                                         &error) as Data? else {
            return false
        }

        print("Crypto - encrypted text \(cipherText.base64EncodedString())")

        // check if the private key can decrypt
        guard SecKeyIsAlgorithmSupported(privateKey, .decrypt, algorithm) else {
            return false
        }

        //check if the text size is compatible with the key size
        guard cipherText.count == SecKeyGetBlockSize(privateKey) else {
            return false
        }

        //perform decrypt, the return is Data
        guard let clearTextData = SecKeyCreateDecryptedData(privateKey,
                                                            algorithm,
                                                            cipherText as CFData,
                                                            &error) as Data? else {
            return false
        }

        guard let clearText = String(data: clearTextData, encoding: .utf8) else { return false }
        
        return alias == clearText
    }
    
    func sha256(string: String) -> Data {
        var hash = [UInt8](repeating: 0,  count: Int(CC_SHA256_DIGEST_LENGTH))
        let data = string.data(using: .utf8) ?? Data()
        data.withUnsafeBytes {
            _ = CC_SHA256($0.baseAddress, CC_LONG(data.count), &hash)
        }
        return Data(hash)
    }
}
