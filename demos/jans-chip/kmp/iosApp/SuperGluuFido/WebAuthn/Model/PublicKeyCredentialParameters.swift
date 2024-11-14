//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

public struct PublicKeyCredentialParameters: Codable {
    let type: String
    let alg: COSEAlgorithmIdentifier
}
