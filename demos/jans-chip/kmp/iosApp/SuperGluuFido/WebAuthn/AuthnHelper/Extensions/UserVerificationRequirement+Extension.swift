//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

extension UserVerificationRequirement {
    static func ofString(_ str: String) -> UserVerificationRequirement {
        switch str {
        case "required":
            return .required
        case "preferred":
            return .preferred
        default:
            return .preferred // default value
        }
    }
}
