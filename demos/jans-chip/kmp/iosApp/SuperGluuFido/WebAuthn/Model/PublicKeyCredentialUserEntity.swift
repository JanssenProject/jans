//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

public struct PublicKeyCredentialUserEntity: Codable {
    let id: String // same as user handle
    let name: String
    let displayName: String
}
