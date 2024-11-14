//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

struct PublicKeyCredentialRequestOptions {
    let rpId: String
    let challenge: String
    let allowCredentials: [PublicKeyCredentialDescriptor]?
    let userVerification: UserVerificationRequirement
    let extensions: ClientExtensionsInput?
}
