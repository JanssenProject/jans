//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import CryptoKit
import Foundation

extension Data {
    public func toBase64Url() -> String {
        return self.base64EncodedString().replacingOccurrences(of: "+", with: "-")
                                         .replacingOccurrences(of: "/", with: "_")
                                         .replacingOccurrences(of: "=", with: "")
    }

    func toUInt8() -> UInt8 {
        assert(self.count == 1)
        return self[0]
    }

    func toUInt16BigEndian() -> UInt16 {
        assert(self.count == 2)
        return self.withUnsafeBytes {
            $0.load(as: UInt16.self).bigEndian
        }
    }

    func toUInt32BigEndian() -> UInt32 {
        assert(self.count == 4)
        return self.withUnsafeBytes {
            $0.load(as: UInt32.self).bigEndian
        }
    }

    func toSHA256() -> Data {
        return Data(SHA256.hash(data: self))
    }
}
