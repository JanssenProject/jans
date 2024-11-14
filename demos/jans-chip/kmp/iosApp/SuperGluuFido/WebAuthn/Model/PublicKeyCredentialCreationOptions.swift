//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

struct PublicKeyCredentialCreationOptions {
    let rp: PublicKeyCredentialRpEntity
    let user: PublicKeyCredentialUserEntity
    let challenge: String
    let publicKeyCredentialParams: [PublicKeyCredentialParameters]
    let excludeCredentials: [PublicKeyCredentialDescriptor]?
    let authenticatorSelection: AuthenticatorSelectionCriteria
    let attestation: AttestationConveyancePreference
    let extensions: ClientExtensionsInput?
}
