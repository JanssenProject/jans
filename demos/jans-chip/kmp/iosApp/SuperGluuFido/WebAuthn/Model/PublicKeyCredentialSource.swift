//  SuperGluuFido
//
//  Created by Nazar Yavornytskyi on 01.10.2024.
//  Copyright © 2024 orgName. All rights reserved.
//

public struct PublicKeyCredentialSource: Identifiable, Equatable {
    public let id: String // base64url encoding of the credential
    public let type: String
    public let aaguid: String
    public let userId: String? // base64url encoding of the user id (= user handle)
    public let rpId: String
    public let userName: String
    public let userDisplayName: String

    public init(id: String, type: String, aaguid: String, userId: String?, rpId: String, userName: String, userDisplayName: String) {
        self.id = id
        self.type = type
        self.aaguid = aaguid
        self.userId = userId
        self.rpId = rpId
        self.userName = userName
        self.userDisplayName = userDisplayName
    }
}
