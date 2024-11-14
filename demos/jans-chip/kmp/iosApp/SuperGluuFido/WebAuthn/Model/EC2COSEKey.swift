//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import SwiftCBOR

struct EC2COSEKey {
    let kty: Int // EC2 key type
    let alg: Int // ES256 signature algorithm
    let crv: Int // P-256 curve
    let x: Data
    let y: Data

    static func create(pubKey: Data) -> Self {
        // public key: 04 [32 bytes x] [32 bytes y] -> uncompressed public key (65 bytes)
        assert(pubKey[0] == 4, "Given public key must be uncompressed key.")
        assert(pubKey.count == 65, "Given public key's length must be 65.")
        let x = pubKey[1..<33]
        let y = pubKey[33..<pubKey.count]
        return EC2COSEKey(kty: 2,
                          alg: COSEAlgorithmIdentifier.ES256.rawValue,
                          crv: 1,
                          x: x,
                          y: y)
    }

    func toCBOR() -> Result<Data, WebAuthnError> {
        let ec2COSEKey: [Int: Any] = [
            1: self.kty,  // kty
            3: self.alg,  // alg
            -1: self.crv, // crv
            -2: self.x,   // x
            -3: self.y    // y
        ]
        do {
            let cbor = try CBOR.encodeMap(ec2COSEKey)
            return .success(Data(cbor))
        } catch {
            return .failure(.encodingError(error))
        }
    }
}
