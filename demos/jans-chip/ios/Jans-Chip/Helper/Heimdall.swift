//
//  Heimdall.swift
//
//  Heimdall - The gatekeeper of Bifrost, the road connecting the
//  world (Midgard) to Asgard, home of the Norse gods.
//
//  In iOS, Heimdall is the gatekeeper to the Keychain, offering
//  a nice wrapper for interacting with private-public RSA keys
//  and encrypting/decrypting/signing data.
//
//  Created by Henri Normak on 22/04/15.
//
//  The MIT License (MIT)
//
//  Copyright (c) 2015 Henri Normak
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in all
//  copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.
//

import Foundation
import Security
import CommonCrypto

open class Heimdall {
    fileprivate let publicTag: String
    fileprivate var privateTag: String?
    fileprivate var scope: ScopeOptions
    
    ///
    /// Create an instance with data for the public key,
    /// the keychain is updated with the tag given (call .destroy() to remove)
    ///
    /// - parameters:
    ///     - publicTag: Tag of the public key, keychain is checked for existing key (updated if data
    /// provided is non-nil and does not match)
    ///     - publicKeyData: Bits of the public key, can include the X509 header (will be stripped)
    ///
    /// - returns: Heimdall instance that can handle only public key operations
    ///
    public convenience init?(publicTag: String, publicKeyData: Data? = nil) {
        if let existingData = Heimdall.obtainKeyData(publicTag) {
            // Compare agains the new data (optional)
            if let newData = publicKeyData?.dataByStrippingX509Header() , (existingData != newData) {
                if !Heimdall.updateKey(publicTag, data: newData) {
                    // Failed to update the key, fail the initialisation
                    return nil
                }
            }
            
            self.init(scope: ScopeOptions.PublicKey, publicTag: publicTag, privateTag: nil)
        } else if let data = publicKeyData?.dataByStrippingX509Header(), let _ = Heimdall.insertPublicKey(publicTag, data: data) {
            // Successfully created the new key
            self.init(scope: ScopeOptions.PublicKey, publicTag: publicTag, privateTag: nil)
        } else {
            // Call the init, although returning nil
            self.init(scope: ScopeOptions.PublicKey, publicTag: publicTag, privateTag: nil)
            return nil
        }
    }
    
    ///
    /// Create an instance with the modulus and exponent of the public key
    /// the resulting key is added to the keychain (call .destroy() to remove)
    ///
    /// - parameters:
    ///     - publicTag: Tag of the public key, see data based initialiser for details
    ///     - publicKeyModulus: Modulus of the public key
    ///     - publicKeyExponent: Exponent of the public key
    ///
    /// - returns: Heimdall instance that can handle only public key operations
    ///
    public convenience init?(publicTag: String, publicKeyModulus: Data, publicKeyExponent: Data) {
        // Combine the data into one that we can use for initialisation
        let combinedData = Data(modulus: publicKeyModulus, exponent: publicKeyExponent)
        self.init(publicTag: publicTag, publicKeyData: combinedData)
    }
    
    ///
    /// Shorthand for creating an instance with both public and private key, where the tag
    /// for private key is automatically generated
    ///
    /// - parameters
    ///     - tagPrefix: Prefix to use for the private/public keys in Keychain
    ///     - keySize: Size of the RSA key pair (in bits)
    ///
    /// - returns: Heimdall instance that can handle both private and public key operations
    ///
    public convenience init?(tagPrefix: String, keySize: Int = 2048) {
        self.init(publicTag: tagPrefix, privateTag: tagPrefix + ".private", keySize: keySize)
    }
    
    ///
    /// Create an instane with public and private key tags, if the key pair does not exist
    /// the keys will be generated
    ///
    /// - parameters:
    ///     - publicTag: Tag to use for the public key
    ///     - privateTag: Tag to use for the private key
    ///     - keySize: Size of the RSA key pair (in bits)
    ///
    /// - returns: Heimdall instance ready for both public and private key operations
    ///
    public convenience init?(publicTag: String, privateTag: String, keySize: Int = 2048) {
        self.init(scope: ScopeOptions.All, publicTag: publicTag, privateTag: privateTag)

        if Heimdall.obtainKey(publicTag) == nil || Heimdall.obtainKey(privateTag) == nil {
            if Heimdall.generateKeyPair(publicTag, privateTag: privateTag, keySize: keySize) == nil {
                return nil
            }
        }
    }
    
    fileprivate init(scope: ScopeOptions, publicTag: String, privateTag: String?) {
        self.publicTag = publicTag
        self.privateTag = privateTag
        self.scope = scope
    }
    
    //
    //  MARK: Public functions
    //
    
    ///
    /// - returns: Public key in X.509 format
    ///
    open func publicKeyDataX509() -> Data? {
        if let keyData = obtainKeyData(.public) {
            return keyData.dataByPrependingX509Header()
        }
        
        return nil
    }
    
    ///
    /// - returns: Public key components (modulus and exponent)
    ///
    open func publicKeyComponents() -> (modulus: Data, exponent: Data)? {
        if let keyData = obtainKeyData(.public), let (modulus, exponent) = keyData.splitIntoComponents() {
            return (modulus, exponent)
        }
        
        return nil
    }
    
    ///
    /// - returns: Public key data
    ///
    open func publicKeyData() -> Data? {
        return obtainKeyData(.public)
    }
    
    ///
    /// - returns: Private key data
    ///
    open func privateKeyData() -> Data? {
        return obtainKeyData(.private)
    }
    
    ///
    /// Encrypt an arbitrary string using AES256, the key for which
    /// is generated for a particular process and then encrypted with the
    /// public key from the RSA pair and prepended to the resulting data
    ///
    /// - parameters:
    ///     - string: Input string to be encrypted
    ///     - urlEncode: If true, resulting Base64 string is URL encoded
    ///
    /// - returns: The encrypted data, as Base64 string
    ///
    open func encrypt(_ string: String, urlEncode: Bool = false) -> String? {
        if let data = string.data(using: String.Encoding.utf8, allowLossyConversion: false), let encrypted = self.encrypt(data) {
            
            // Convert to a string
            var resultString = encrypted.base64EncodedString(options: NSData.Base64EncodingOptions(rawValue: 0))
            
            if urlEncode {
                resultString = resultString.replacingOccurrences(of: "/", with: "_")
                resultString = resultString.replacingOccurrences(of: "+", with: "-")
            }
            
            return resultString
        }
        
        return nil
    }
    
    ///
    /// Encrypt an arbitrary message using AES256, the key for which
    /// is generated for a particular process and then encrypted with the
    /// public key from the RSA pair and prepended to the resulting data
    ///
    /// - parameters:
    ///     - data: Input data to be encrypted
    ///
    /// - returns: The encrypted data
    ///
    open func encrypt(_ data: Data) -> Data? {
        if let publicKey = obtainKey(.public) {
            let algorithm = CCAlgorithm(kCCAlgorithmAES128)
            let blockSize = SecKeyGetBlockSize(publicKey)
            let ivSize = Heimdall.blockSize(algorithm)
            let padding = SecPadding.OAEP
            
            let keySize: Int = {
                let adjustedBlockSize = blockSize - ivSize - 42 // Assumes SHA1-OAEP is used
                
                if adjustedBlockSize >= Int(kCCKeySizeAES256) {
                    return kCCKeySizeAES256
                } else if adjustedBlockSize >= Int(kCCKeySizeAES192) {
                    return kCCKeySizeAES192
                } else {
                    return kCCKeySizeAES128
                }
            }()
            
            if let aesKey = Heimdall.generateRandomBytes(keySize), let iv = Heimdall.generateRandomBytes(ivSize),
                let encrypted = Heimdall.encrypt(data, key: aesKey, iv: iv, algorithm: algorithm) {
                // Final resulting data
                let result = NSMutableData()
                
                // Encrypt the AES key and IV with our public key
                var encryptedLength = blockSize
                var encryptedData = [UInt8](repeating: 0, count: encryptedLength)
                
                var rawKeyIVData = NSData(data: aesKey) as Data
                rawKeyIVData.append(iv)
                
                
                let keyIvBytes = rawKeyIVData.withUnsafeBytes { (pointer: UnsafePointer<UInt8>) -> UnsafePointer<UInt8> in
                    return pointer
                }
                
                //let keyIvBytes = rawKeyIVData.bytes.bindMemory(to: UInt8.self, capacity: rawKeyIVData.count)
                
                let status = SecKeyEncrypt(publicKey, padding, keyIvBytes, rawKeyIVData.count, &encryptedData, &encryptedLength)
                if status != noErr {
                    return nil
                }
                
                // Order is simple, first block is metadata, then comes the message
                result.append(&encryptedData, length: encryptedLength)
                result.append(encrypted)
                
                return result as Data
            }
        }
        
        return nil
    }
    
    ///
    /// Decrypt a Base64 string representation of encrypted data
    ///
    /// - parameters:
    ///     - base64String: String containing Base64 data to decrypt
    ///     - urlEncoded: Whether the input Base64 data is URL encoded
    ///
    /// - returns: Decrypted string as plain text
    ///
    open func decrypt(_ base64String: String, urlEncoded: Bool = true) -> String? {
        var string = base64String
        
        if urlEncoded {
            string = string.replacingOccurrences(of: "_", with: "/")
            string = string.replacingOccurrences(of: "-", with: "+")
        }
        
        if let data = Data(base64Encoded: string, options: NSData.Base64DecodingOptions(rawValue: 0)), let decryptedData = self.decrypt(data) {
            return NSString(data: decryptedData, encoding: String.Encoding.utf8.rawValue) as String?
        }
        
        return nil
    }
    
    ///
    /// Decrypt the encrypted data
    ///
    /// - parameters:
    ///     - encryptedData: Data to decrypt
    ///
    /// - returns: The decrypted data, or nil if failed
    ///
    open func decrypt(_ encryptedData: Data) -> Data? {
        if let key = obtainKey(.private) {
            let algorithm = CCAlgorithm(kCCAlgorithmAES128)
            let blockSize = SecKeyGetBlockSize(key)
            let ivSize = Heimdall.blockSize(algorithm)
            let padding = SecPadding.OAEP
            
            guard encryptedData.count > blockSize else { return nil }
            
            let keySize: Int = {
                let adjustedBlockSize = blockSize - ivSize - 42 // Assumes SHA1-OAEP is used
                
                if adjustedBlockSize >= Int(kCCKeySizeAES256) {
                    return kCCKeySizeAES256
                } else if adjustedBlockSize >= Int(kCCKeySizeAES192) {
                    return kCCKeySizeAES192
                } else {
                    return kCCKeySizeAES128
                }
            }()
            
            let metadata = encryptedData.subdata(in: Range(uncheckedBounds: (0, blockSize)))
            let messageData = encryptedData.subdata(in: Range(uncheckedBounds: (blockSize, encryptedData.count)))
                        
            // Decrypt the key and the IV
            if let decryptedMetadata = NSMutableData(length: blockSize) {
                let encryptedMetadata = (metadata as NSData).bytes.bindMemory(to: UInt8.self, capacity: metadata.count)
                let decryptedMetadataBytes = decryptedMetadata.mutableBytes.assumingMemoryBound(to: UInt8.self)
                
                var decryptedMetadataLength = blockSize
                let decryptionStatus = SecKeyDecrypt(key, padding, encryptedMetadata, blockSize, decryptedMetadataBytes, &decryptedMetadataLength)
                
                if decryptionStatus == noErr {
                    decryptedMetadata.length = Int(decryptedMetadataLength)
                    
                    // We can now extract the key and the IV
                    let decryptedKey = decryptedMetadata.subdata(with: NSRange(location: 0, length: keySize))
                    let decryptedIv = decryptedMetadata.subdata(with: NSRange(location: keySize, length: ivSize))

                    if let message = Heimdall.decrypt(messageData, key: decryptedKey, iv: decryptedIv, algorithm: algorithm) {
                        return message
                    }
                }
            }
        }
        
        return nil
    }
    
    ///
    /// Generate a signature for an arbitrary message
    ///
    /// - parameters:
    ///     - string: Message to generate the signature for
    ///     - urlEncode: True if the resulting Base64 data should be URL encoded
    ///
    /// - returns: Signature as a Base64 string
    ///
    open func sign(_ string: String, urlEncode: Bool = false) -> String? {
        if let data = string.data(using: String.Encoding.utf8, allowLossyConversion: false), let signatureData = self.sign(data) {
            
            var signature = signatureData.base64EncodedString(options: NSData.Base64EncodingOptions(rawValue: 0))
            
            if urlEncode {
                signature = signature.replacingOccurrences(of: "/", with: "_")
                signature = signature.replacingOccurrences(of: "+", with: "-")
            }
            
            return signature
        }
        
        return nil
    }
    
    ///
    /// Generate a signature for an arbitrary payload
    ///
    /// - parameters:
    ///     - data: Data to sign
    ///
    /// - returns: Signature as NSData
    ///
    open func sign(_ data: Data) -> Data? {
        if let key = obtainKey(.private), let hash = NSMutableData(length: Int(CC_SHA256_DIGEST_LENGTH)) {
            
            // Create SHA256 hash of the message
            CC_SHA256((data as NSData).bytes, CC_LONG(data.count), hash.mutableBytes.assumingMemoryBound(to: UInt8.self))
            
            // Sign the hash with the private key
            let blockSize = SecKeyGetBlockSize(key)
            
            let hashDataLength = Int(hash.length)
            let hashData = hash.bytes.bindMemory(to: UInt8.self, capacity: hash.length)
            
            if let result = NSMutableData(length: Int(blockSize)) {
                let encryptedData = result.mutableBytes.assumingMemoryBound(to: UInt8.self)
                var encryptedDataLength = blockSize
                
                let status = SecKeyRawSign(key, .PKCS1SHA256, hashData, hashDataLength, encryptedData, &encryptedDataLength)
                
                if status == noErr {
                    // Create Base64 string of the result
                    result.length = encryptedDataLength
                    return result as Data
                }
            }
        }
        
        return nil
    }
    
    
    ///
    /// Verify the message with the given signature
    ///
    /// - parameters:
    ///     - message: Message that was used to generate the signature
    ///     - signatureBase64: Base64 of the signature data, signature is made of the SHA256 hash of message
    ///     - urlEncoded: True, if the signature is URL encoded and has to be reversed before manipulating
    ///
    /// - returns: true if the signature is valid (and can be validated)
    ///
    open func verify(_ message: String, signatureBase64: String, urlEncoded: Bool = true) -> Bool {
        var string = signatureBase64
        
        if urlEncoded {
            string = string.replacingOccurrences(of: "_", with: "/")
            string = string.replacingOccurrences(of: "-", with: "+")
        }

        if let data = message.data(using: String.Encoding.utf8, allowLossyConversion: false), let signature = Data(base64Encoded: string, options: NSData.Base64DecodingOptions(rawValue: 0)) {
            return self.verify(data, signatureData: signature)
        }
        
        return false
    }
    
    ///
    /// Verify a data payload with the given signature
    ///
    /// - parameters:
    ///     - data: Data the signature should be verified against
    ///     - signatureData: Data of the signature
    ///
    /// - returns: True if the signature is valid
    ///
    open func verify(_ data: Data, signatureData: Data) -> Bool {
        if let key = obtainKey(.public), let hashData = NSMutableData(length: Int(CC_SHA256_DIGEST_LENGTH)) {
            CC_SHA256((data as NSData).bytes, CC_LONG(data.count), hashData.mutableBytes.assumingMemoryBound(to: UInt8.self))
            
            let signedData = hashData.bytes.bindMemory(to: UInt8.self, capacity: hashData.length)
            let signatureLength = Int(signatureData.count)
            let signatureData = (signatureData as NSData).bytes.bindMemory(to: UInt8.self, capacity: signatureData.count)
            
            let result = SecKeyRawVerify(key, .PKCS1SHA256, signedData, Int(CC_SHA256_DIGEST_LENGTH), signatureData, signatureLength)
            
            switch result {
            case noErr:
                return true
            default:
                return false
            }
        }
        
        return false
    }
    
    // MARK: Managing the key pair
    
    ///
    /// Remove the key pair this Heimdall represents
    /// Does not regenerate the keys, thus the Heimdall
    /// instance becomes useless after this call
    ///
    /// - returns: True if remove successfully
    ///
    @discardableResult open func destroy() -> Bool {
        if Heimdall.deleteKey(self.publicTag) {
            self.scope = self.scope & ~(ScopeOptions.PublicKey)
            
            if let privateTag = self.privateTag , Heimdall.deleteKey(privateTag) {
                self.scope = self.scope & ~(ScopeOptions.PrivateKey)
                return true
            }
            
            return true
        }
        
        return false
    }
    
    ///
    /// Delete existing key pair and regenerate new one
    /// This will always fail for instances that don't have
    /// a private key, including those that have been explicitly
    /// destroyed beforehand
    ///
    /// - parameters:
    ///     - keySize: Size of keys in the new pair
    ///
    /// - returns: True if reset successfully
    ///
    @discardableResult open func regenerate(_ keySize: Int = 2048) -> Bool {
        // Only if we currently have a private key in our control (or we think we have one)
        if self.scope & ScopeOptions.PrivateKey != ScopeOptions.PrivateKey {
            return false
        }
        
        if let privateTag = self.privateTag , self.destroy() {
            if Heimdall.generateKeyPair(self.publicTag, privateTag: privateTag, keySize: keySize) != nil {
                // Restore our scope back to .All
                self.scope = .All
                return true
            }
        }
        
        return false
    }
    
    //
    //  MARK: Private types
    //
    fileprivate enum KeyType {
        case `public`
        case `private`
    }
    
    fileprivate struct ScopeOptions: OptionSet {
        fileprivate var value: UInt
        
        init(_ rawValue: UInt) { self.value = rawValue }
        init(rawValue: UInt) { self.value = rawValue }
        init(nilLiteral: ()) { self.value = 0}
        
        var rawValue: UInt { return self.value }
        var boolValue: Bool { return self.value != 0 }
        
        static var allZeros: ScopeOptions { return self.init(0) }
        
        static var PublicKey: ScopeOptions { return self.init(1 << 0) }
        static var PrivateKey: ScopeOptions { return self.init(1 << 1) }
        static var All: ScopeOptions           { return self.init(0b11) }
    }
    
    
    //
    //  MARK: Private helpers
    //
    fileprivate func obtainKey(_ key: KeyType) -> SecKey? {
        if key == .public && self.scope & ScopeOptions.PublicKey == ScopeOptions.PublicKey {
            return Heimdall.obtainKey(self.publicTag)
        } else if let tag = self.privateTag , key == .private && self.scope & ScopeOptions.PrivateKey == ScopeOptions.PrivateKey {
            return Heimdall.obtainKey(tag)
        }
        
        return nil
    }
    
    fileprivate func obtainKeyData(_ key: KeyType) -> Data? {
        if key == .public && self.scope & ScopeOptions.PublicKey == ScopeOptions.PublicKey {
            return Heimdall.obtainKeyData(self.publicTag)
        } else if let tag = self.privateTag , key == .private && self.scope & ScopeOptions.PrivateKey == ScopeOptions.PrivateKey {
            return Heimdall.obtainKeyData(tag)
        }
        
        return nil
    }
    
    //
    //  MARK: Private class functions
    //
    
    fileprivate class func obtainKey(_ tag: String) -> SecKey? {
        var keyRef: AnyObject?
        let query: Dictionary<String, AnyObject> = [
            String(kSecAttrKeyType): kSecAttrKeyTypeRSA,
            String(kSecReturnRef): kCFBooleanTrue as CFBoolean,
            String(kSecClass): kSecClassKey as CFString,
            String(kSecAttrApplicationTag): tag as CFString,
        ]
        
        let status = SecItemCopyMatching(query as CFDictionary, &keyRef)
        
        switch status {
        case noErr:
            if let ref = keyRef {
                return (ref as! SecKey)
            }
        default:
            break
        }
        
        return nil
    }
    
    fileprivate class func obtainKeyData(_ tag: String) -> Data? {
        var keyRef: AnyObject?
        let query: Dictionary<String, AnyObject> = [
            String(kSecAttrKeyType): kSecAttrKeyTypeRSA,
            String(kSecReturnData): kCFBooleanTrue as CFBoolean,
            String(kSecClass): kSecClassKey as CFString,
            String(kSecAttrApplicationTag): tag as CFString,
        ]
        
        let result: Data?
        
        switch SecItemCopyMatching(query as CFDictionary, &keyRef) {
        case noErr:
            result = keyRef as? Data
        default:
            result = nil
        }
        
        return result
    }
    
    fileprivate class func updateKey(_ tag: String, data: Data) -> Bool {
        let query: Dictionary<String, AnyObject> = [
            String(kSecAttrKeyType): kSecAttrKeyTypeRSA,
            String(kSecClass): kSecClassKey as CFString,
            String(kSecAttrApplicationTag): tag as CFString]
        
        
        return SecItemUpdate(query as CFDictionary, [String(kSecValueData): data] as CFDictionary) == noErr
    }
    
    fileprivate class func deleteKey(_ tag: String) -> Bool {
        let query: Dictionary<String, AnyObject> = [
            String(kSecAttrKeyType): kSecAttrKeyTypeRSA,
            String(kSecClass): kSecClassKey as CFString,
            String(kSecAttrApplicationTag): tag as CFString]
        
        return SecItemDelete(query as CFDictionary) == noErr
    }
    
    fileprivate class func insertPublicKey(_ publicTag: String, data: Data) -> SecKey? {
        var publicAttributes = Dictionary<String, AnyObject>()
        publicAttributes[String(kSecAttrKeyType)] = kSecAttrKeyTypeRSA
        publicAttributes[String(kSecClass)] = kSecClassKey as CFString
        publicAttributes[String(kSecAttrApplicationTag)] = publicTag as CFString
        publicAttributes[String(kSecValueData)] = data as CFData
        publicAttributes[String(kSecReturnPersistentRef)] = true as CFBoolean
        
        var persistentRef: AnyObject?
        let status = SecItemAdd(publicAttributes as CFDictionary, &persistentRef)
        
        if status != noErr && status != errSecDuplicateItem {
            return nil
        }
        
        return Heimdall.obtainKey(publicTag)
    }
    
    
    fileprivate class func generateKeyPair(_ publicTag: String, privateTag: String, keySize: Int) -> (publicKey: SecKey, privateKey: SecKey)? {
        let privateAttributes = [String(kSecAttrIsPermanent): true,
                                 String(kSecAttrApplicationTag): privateTag] as [String : Any]
        let publicAttributes = [String(kSecAttrIsPermanent): true,
                                String(kSecAttrApplicationTag): publicTag] as [String : Any]
        
        let pairAttributes = [String(kSecAttrKeyType): kSecAttrKeyTypeRSA,
                              String(kSecAttrKeySizeInBits): keySize,
                              String(kSecPublicKeyAttrs): publicAttributes,
                              String(kSecPrivateKeyAttrs): privateAttributes] as [String : Any]
        
        var publicRef: SecKey?
        var privateRef: SecKey?
        switch SecKeyGeneratePair(pairAttributes as CFDictionary, &publicRef, &privateRef) {
            case noErr:
                if let publicKey = publicRef, let privateKey = privateRef {
                    return (publicKey, privateKey)
                }
                
                return nil
            default:
                return nil
        }
    }
    
    fileprivate class func generateRandomBytes(_ count: Int) -> Data? {
        var result = [UInt8](repeating: 0, count: count)
        if SecRandomCopyBytes(kSecRandomDefault, count, &result) != 0 {
            // Failed to get random bits
            return nil
        }
        
        return Data(bytes: UnsafePointer<UInt8>(result), count: count)
    }
    
    fileprivate class func blockSize(_ algorithm: CCAlgorithm) -> Int {
        switch Int(algorithm) {
        case kCCAlgorithmAES128, kCCAlgorithmAES:
            return kCCBlockSizeAES128
        case kCCAlgorithmDES:
            return kCCBlockSizeDES
        case kCCAlgorithm3DES:
            return kCCBlockSize3DES
        case kCCAlgorithmCAST:
            return kCCBlockSizeCAST
        case kCCAlgorithmRC2:
            return kCCBlockSizeRC2
        case kCCAlgorithmBlowfish:
            return kCCBlockSizeBlowfish
        default:
            return 0
        }
    }
    
    
    fileprivate class func encrypt(_ data: Data, key: Data, iv: Data, algorithm: CCAlgorithm) -> Data? {
        let dataBytes = (data as NSData).bytes.bindMemory(to: UInt8.self, capacity: data.count)
        let dataLength = data.count
        
        if let result = NSMutableData(length: dataLength + key.count + iv.count) {
            let keyData = (key as NSData).bytes.bindMemory(to: UInt8.self, capacity: key.count)
            let keyLength = size_t(key.count)
            let ivData = (iv as NSData).bytes.bindMemory(to: UInt8.self, capacity: iv.count)
            
            let encryptedData = UnsafeMutablePointer<UInt8>(result.mutableBytes.assumingMemoryBound(to: UInt8.self))
            let encryptedDataLength = size_t(result.length)
            
            var encryptedLength: size_t = 0
            
            let status = CCCrypt(CCOperation(kCCEncrypt), algorithm, CCOptions(kCCOptionPKCS7Padding), keyData, keyLength, ivData, dataBytes, dataLength, encryptedData, encryptedDataLength, &encryptedLength)
            
            if status == Int32(kCCSuccess) {
                result.length = Int(encryptedLength)
                return result as Data
            }
        }
        
        return nil
    }
    
    fileprivate class func decrypt(_ data: Data, key: Data, iv: Data, algorithm: CCAlgorithm) -> Data? {
        let encryptedData = (data as NSData).bytes.bindMemory(to: UInt8.self, capacity: data.count)
        let encryptedDataLength = data.count
        
        if let result = NSMutableData(length: encryptedDataLength) {
            let keyData = (key as NSData).bytes.bindMemory(to: UInt8.self, capacity: key.count)
            let keyLength = size_t(key.count)
            let ivData = (iv as NSData).bytes.bindMemory(to: UInt8.self, capacity: iv.count)
            
            let decryptedData = UnsafeMutablePointer<UInt8>(result.mutableBytes.assumingMemoryBound(to: UInt8.self))
            let decryptedDataLength = size_t(result.length)
            
            var decryptedLength: size_t = 0
            
            let status = CCCrypt(CCOperation(kCCDecrypt), algorithm, CCOptions(kCCOptionPKCS7Padding), keyData, keyLength, ivData, encryptedData, encryptedDataLength, decryptedData, decryptedDataLength, &decryptedLength)
            
            if UInt32(status) == UInt32(kCCSuccess) {
                result.length = Int(decryptedLength)
                return result as Data
            }
        }

        return nil
    }
}

///
/// Arithmetic
///

private func ==(lhs: Heimdall.ScopeOptions, rhs: Heimdall.ScopeOptions) -> Bool {
    return lhs.rawValue == rhs.rawValue
}

private prefix func ~(op: Heimdall.ScopeOptions) -> Heimdall.ScopeOptions {
    return Heimdall.ScopeOptions(~op.rawValue)
}

private func &(lhs: Heimdall.ScopeOptions, rhs: Heimdall.ScopeOptions) -> Heimdall.ScopeOptions {
    return Heimdall.ScopeOptions(lhs.rawValue & rhs.rawValue)
}

///
/// Encoding/Decoding lengths as octets
///
private extension NSInteger {
    func encodedOctets() -> [CUnsignedChar] {
        // Short form
        if self < 128 {
            return [CUnsignedChar(self)];
        }
        
        // Long form
        let i = Int(log2(Double(self)) / 8 + 1)
        var len = self
        var result: [CUnsignedChar] = [CUnsignedChar(i + 0x80)]
        
        for _ in 0..<i {
            result.insert(CUnsignedChar(len & 0xFF), at: 1)
            len = len >> 8
        }
        
        return result
    }
    
    init?(octetBytes: [CUnsignedChar], startIdx: inout NSInteger) {
        if octetBytes[startIdx] < 128 {
            // Short form
            self.init(octetBytes[startIdx])
            startIdx += 1
        } else {
            // Long form
            let octets = NSInteger(octetBytes[startIdx] as UInt8 - 128)
            
            if octets > octetBytes.count - startIdx {
                self.init(0)
                return nil
            }
            
            var result = UInt64(0)
            
            for j in 1...octets {
                result = (result << 8)
                result = result + UInt64(octetBytes[startIdx + j])
            }
            
            startIdx += 1 + octets
            self.init(result)
        }
    }
}


///
/// Manipulating data
///
private extension Data {
    init(modulus: Data, exponent: Data) {
        // Make sure neither the modulus nor the exponent start with a null byte
        var modulusBytes = [CUnsignedChar](UnsafeBufferPointer<CUnsignedChar>(start: (modulus as NSData).bytes.bindMemory(to: CUnsignedChar.self, capacity: modulus.count), count: modulus.count / MemoryLayout<CUnsignedChar>.size))
        let exponentBytes = [CUnsignedChar](UnsafeBufferPointer<CUnsignedChar>(start: (exponent as NSData).bytes.bindMemory(to: CUnsignedChar.self, capacity: exponent.count), count: exponent.count / MemoryLayout<CUnsignedChar>.size))
        
        // Make sure modulus starts with a 0x00
        if let prefix = modulusBytes.first , prefix != 0x00 {
            modulusBytes.insert(0x00, at: 0)
        }
        
        // Lengths
        let modulusLengthOctets = modulusBytes.count.encodedOctets()
        let exponentLengthOctets = exponentBytes.count.encodedOctets()
        
        // Total length is the sum of components + types
        let totalLengthOctets = (modulusLengthOctets.count + modulusBytes.count + exponentLengthOctets.count + exponentBytes.count + 2).encodedOctets()
        
        // Combine the two sets of data into a single container
        var builder: [CUnsignedChar] = []
        let data = NSMutableData()
        
        // Container type and size
        builder.append(0x30)
        builder.append(contentsOf: totalLengthOctets)
        data.append(builder, length: builder.count)
        builder.removeAll(keepingCapacity: false)
        
        // Modulus
        builder.append(0x02)
        builder.append(contentsOf: modulusLengthOctets)
        data.append(builder, length: builder.count)
        builder.removeAll(keepingCapacity: false)
        data.append(modulusBytes, length: modulusBytes.count)
        
        // Exponent
        builder.append(0x02)
        builder.append(contentsOf: exponentLengthOctets)
        data.append(builder, length: builder.count)
        data.append(exponentBytes, length: exponentBytes.count)
        
        self.init(bytes: data.bytes, count: data.length)
        
        //self.init(data: data)
    }
    
    func splitIntoComponents() -> (modulus: Data, exponent: Data)? {
        // Get the bytes from the keyData
        let pointer = (self as NSData).bytes.bindMemory(to: CUnsignedChar.self, capacity: self.count)
        let keyBytes = [CUnsignedChar](UnsafeBufferPointer<CUnsignedChar>(start:pointer, count:self.count / MemoryLayout<CUnsignedChar>.size))

        // Assumption is that the data is in DER encoding
        // If we can parse it, then return successfully
        var i: NSInteger = 0
        
        // First there should be an ASN.1 SEQUENCE
        if keyBytes[0] != 0x30 {
            return nil
        } else {
            i += 1
        }
        // Total length of the container
        if let _ = NSInteger(octetBytes: keyBytes, startIdx: &i) {
            // First component is the modulus
            if keyBytes[i] == 0x02 {
                i += 1
                if let modulusLength = NSInteger(octetBytes: keyBytes, startIdx: &i) {
                    let modulus = self.subdata(in: Range.init(NSRange(location: i, length: modulusLength))!)
                    i += modulusLength
                    
                    // Second should be the exponent
                    if keyBytes[i] == 0x02 {
                        i += 1
                        if let exponentLength = NSInteger(octetBytes: keyBytes, startIdx: &i) {
                            let exponent = self.subdata(in: Range.init(NSRange(location: i, length: exponentLength))!)
                            i += exponentLength
                            
                            return (modulus, exponent)
                        }
                    }
                }
            }
        }
        
        return nil
    }
    
    func dataByPrependingX509Header() -> Data {
        let result = NSMutableData()
        
        let encodingLength: Int = (self.count + 1).encodedOctets().count
        let OID: [CUnsignedChar] = [0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86,
            0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00]
        
        var builder: [CUnsignedChar] = []
        
        // ASN.1 SEQUENCE
        builder.append(0x30)
        
        // Overall size, made of OID + bitstring encoding + actual key
        let size = OID.count + 2 + encodingLength + self.count
        let encodedSize = size.encodedOctets()
        builder.append(contentsOf: encodedSize)
        result.append(builder, length: builder.count)
        result.append(OID, length: OID.count)
        builder.removeAll(keepingCapacity: false)
        
        builder.append(0x03)
        builder.append(contentsOf: (self.count + 1).encodedOctets())
        builder.append(0x00)
        result.append(builder, length: builder.count)
        
        // Actual key bytes
        result.append(self)
        
        return result as Data
    }
    
    func dataByStrippingX509Header() -> Data {
        var bytes = [CUnsignedChar](repeating: 0, count: self.count)
        (self as NSData).getBytes(&bytes, length:self.count)
        
        var range = NSRange(location: 0, length: self.count)
        var offset = 0
        
        // ASN.1 Sequence
        if bytes[offset] == 0x30 {
            offset += 1
            
            // Skip over length
            let _ = NSInteger(octetBytes: bytes, startIdx: &offset)
            
            let OID: [CUnsignedChar] = [0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86,
                0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00]
            let slice: [CUnsignedChar] = Array(bytes[offset..<(offset + OID.count)])
            
            if slice == OID {
                offset += OID.count
                
                // Type
                if bytes[offset] != 0x03 {
                    return self
                }
                
                offset += 1
                
                // Skip over the contents length field
                let _ = NSInteger(octetBytes: bytes, startIdx: &offset)
                
                // Contents should be separated by a null from the header
                if bytes[offset] != 0x00 {
                    return self
                }
                
                offset += 1
                range.location += offset
                range.length -= offset
            } else {
                return self
            }
        }
        
        return self.subdata(in: Range.init(range)!)
    }
}
