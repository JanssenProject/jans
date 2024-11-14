//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct AuthenticatorMakeCredentialResult {
    let credentialId: Data
    let attestationObject: Data
}

struct AuthenticatorGetAssertionResult {
    let userHandle: String?
    let credentialId: Data
    let authenticatorData: Data
    let signature: Data
}
