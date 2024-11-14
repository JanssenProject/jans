//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

public struct ClientExtensionsInput: Codable {
    func processAuthenticatorExtensionsInput() -> AuthenticatorExtensionsInput? {
        return nil
    }

    func processClientExtensionsOutput() -> ClientExtensionsOutput {
        return ClientExtensionsOutput()
    }
}

public struct ClientExtensionsOutput {
}
