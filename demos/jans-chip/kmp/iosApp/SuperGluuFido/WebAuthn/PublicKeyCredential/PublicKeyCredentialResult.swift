//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

public struct PublicKeyCredentialCreateResult {
    public let id: Data
    public let clientDataJson: Data
    public let attestation: Data
    public let clientExtensionsOutput: ClientExtensionsOutput?
}

public struct PublicKeyCredentialGetResult {
    public let id: Data
    public let clientDataJson: Data
    public let userHandle: String?
    public let authenticatorData: Data
    public let signature: Data
    public let clientExtensionsOutput: ClientExtensionsOutput?
}
