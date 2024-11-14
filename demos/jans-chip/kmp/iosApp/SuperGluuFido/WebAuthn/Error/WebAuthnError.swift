//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

public enum WebAuthnError: Error {
    case coreError(CoreError, cause: String? = nil)
    case keyStorageError(KeyStorageError, credId: String, deleteTrigger: Error? = nil)
    case credSrcStorageError(Error, credId: String? = nil, deleteTrigger: Error? = nil) // credId can be nil when loadAll()
    case rpError(Error)
    case encodingError(Error)
    case laError(Error)
    case secKeyError(cause: String)
    case keyNotFoundError
    case utilityError(cause: String)
    case unknownError(Error)
}
