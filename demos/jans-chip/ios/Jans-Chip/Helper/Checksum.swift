//
//  Checksum.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 22.11.2023.
//

import Foundation
import CryptoKit
import CommonCrypto

final class Checksum {
    
    public static func md5File(data: Data) -> Data? {
        let bufferSize = 1024 * 1024
        // Create and initialize MD5 context:
        var context = CC_MD5_CTX()
        CC_MD5_Init(&context)
        
        // Read up to `bufferSize` bytes, until EOF is reached, and update MD5 context:
        while autoreleasepool(invoking: {
            if data.count > 0 {
                data.withUnsafeBytes {
                    _ = CC_MD5_Update(&context, $0.baseAddress, numericCast(data.count))
                }
                return true // Continue
            } else {
                return false // End of file
            }
        }) { }
        
        // Compute the MD5 digest:
        var digest: [UInt8] = Array(repeating: 0, count: Int(CC_MD5_DIGEST_LENGTH))
        _ = CC_MD5_Final(&digest, &context)
        
        return Data(digest)
    }
    
    public static func md5File(url: URL) -> Data? {
        
        let bufferSize = 1024 * 1024
        
        do {
            // Open file for reading:
            let file = try FileHandle(forReadingFrom: url)
            defer {
                file.closeFile()
            }
            
            // Create and initialize MD5 context:
            var context = CC_MD5_CTX()
            CC_MD5_Init(&context)
            
            // Read up to `bufferSize` bytes, until EOF is reached, and update MD5 context:
            while autoreleasepool(invoking: {
                let data = file.readData(ofLength: bufferSize)
                if data.count > 0 {
                    data.withUnsafeBytes {
                        _ = CC_MD5_Update(&context, $0.baseAddress, numericCast(data.count))
                    }
                    return true // Continue
                } else {
                    return false // End of file
                }
            }) { }
            
            // Compute the MD5 digest:
            var digest: [UInt8] = Array(repeating: 0, count: Int(CC_MD5_DIGEST_LENGTH))
            _ = CC_MD5_Final(&digest, &context)
            
            return Data(digest)
            
        } catch {
            print("Cannot open file:", error.localizedDescription)
            return nil
        }
    }
}

extension Data {
    public func checksum() -> UInt32 {
        let table: [UInt32] = {
            (0...255).map { i -> UInt32 in
                (0..<8).reduce(UInt32(i), { c, _ in
                    (c % 2 == 0) ? (c >> 1) : (0xEDB88320 ^ (c >> 1))
                })
            }
        }()
        
        return ~(self.withUnsafeBytes({ pointers in pointers.reduce(~UInt32(0), { crc, byte in (crc >> 8) ^ table[(Int(crc) ^ Int(byte)) & 0xFF] }) }) )
    }
}

extension URL {

    func checksumInBase64() -> String? {
        let bufferSize = 16*1024

        do {
            // Open file for reading:
            let file = try FileHandle(forReadingFrom: self)
            defer {
                file.closeFile()
            }

            // Create and initialize MD5 context:
            var md5 = CryptoKit.Insecure.MD5()
            
            // Read up to `bufferSize` bytes, until EOF is reached, and update MD5 context:
            while autoreleasepool(invoking: {
                let data = file.readData(ofLength: bufferSize)
                if data.count > 0 {
                    md5.update(data: data)
                    return true // Continue
                } else {
                    return false // End of file
                }
            }) { }

            // Compute the MD5 digest:
            let data = Data(md5.finalize())
            
            return data.base64EncodedString()
        } catch {
            print(error)
            
            return nil
        }
    }
}
