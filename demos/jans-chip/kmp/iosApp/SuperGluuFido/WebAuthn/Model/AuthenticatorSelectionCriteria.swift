//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

public struct AuthenticatorSelectionCriteria: Codable {
    let authenticatorAttachment: AuthenticatorAttachment
    let userVerification: UserVerificationRequirement
}
