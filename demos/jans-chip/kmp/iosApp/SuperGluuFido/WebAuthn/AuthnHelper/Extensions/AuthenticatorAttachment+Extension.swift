//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

extension AuthenticatorAttachment {
    static func ofString(_ str: String) -> AuthenticatorAttachment {
        switch str {
        case "platform":
            return .platform
        case "cross-platform":
            return .crossPlatform
        default:
            return .platform // default value
        }
    }
}
