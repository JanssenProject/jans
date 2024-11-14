//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

extension AttestationConveyancePreference {
    static func ofString(_ str: String) -> AttestationConveyancePreference {
        switch str {
        case "none":
            return .none
        case "direct":
            return .direct
        case "indirect":
            return .indirect
        default:
            return .none // default value
        }
    }
}
