//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation
import SwiftCBOR

struct AuthenticatorExtensionsInput {
    func processAuthenticatorExtensions() -> AuthenticatorExtensionsOutput? {
        return nil
    }
}

struct AuthenticatorExtensionsOutput: Codable {
    func toData() -> Data {
        return Data()
    }
}
