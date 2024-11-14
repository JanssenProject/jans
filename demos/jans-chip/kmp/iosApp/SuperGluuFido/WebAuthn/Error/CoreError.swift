//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

public enum CoreError: Error, Equatable {
    /// Error when an authenticator is not supported by a device
    case constraintError
    /// Error when a user is already registered
    case invalidStateError
    /// Error when a user fails to authenticate or is not registered
    case notAllowedError
    /// Error when something is not yet supported
    case notSupportedError
    /// Error related in types
    case typeError

}
