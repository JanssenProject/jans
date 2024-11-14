//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

public enum KeyStorageError: Error, Equatable {
    case accessFailed
    case invalidItemFound
    case loadFailed(status: OSStatus)
    case storeFailed(status: OSStatus)
    case updateFailed(status: OSStatus)
    case deleteFailed(status: OSStatus)
}
