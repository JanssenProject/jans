//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

enum COSEAlgorithmIdentifier: Int, Codable, CaseIterable {
    case RS1   = -65535 // RSASSA-PKCS1-v1_5 with SHA-1
    case RS256 = -257   // RSASSA-PKCS1-v1_5 with SHA-256
    case RS384 = -258   // RSASSA-PKCS1-v1_5 with SHA-384
    case RS512 = -259   // RSASSA-PKCS1-v1_5 with SHA-512
    case PS256 = -37    // RSASSA-PSS with SHA-256
    case PS384 = -38    // RSASSA-PSS with SHA-384
    case PS512 = -39    // RSASSA-PSS with SHA-512
    case EDDSA = -8     // EdDSA
    case ES256 = -7     // ECDSA with SHA-256
    case ES384 = -35    // ECDSA with SHA-384
    case ES512 = -36    // ECDSA with SHA-512
    case ES256K = -43   // ECDSA using P-256K and SHA-256

    var keyType: String {
        switch self {
        case .RS1, .RS256, .RS384, .RS512, .PS256, .PS384, .PS512:
            return kSecAttrKeyTypeRSA as String
        case .EDDSA, .ES256, .ES384, .ES512, .ES256K:
            return kSecAttrKeyTypeECSECPrimeRandom as String
        }
    }

    var keyLen: Int {
        switch self {
        case .RS1:
            return 160
        case .RS256, .PS256, .EDDSA, .ES256, .ES256K:
            return 256
        case .RS384, .PS384, .ES384:
            return 384
        case .RS512, .PS512, .ES512:
            return 512
        }
    }
}
