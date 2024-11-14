//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

import Foundation

struct CollectedClientData: Codable {
    let type: String
    let challenge: String
    let origin: String

    func encodeJSON() -> Result<Data, WebAuthnError> {
        do {
            let json = try JSONEncoder().encode(self)
            return .success(json)
        } catch {
            return .failure(.encodingError(error))
        }
    }
}
