//
//  AuthenticatorMakeCredentialOptions.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 12.07.2024.
//

import Foundation

struct AuthenticatorMakeCredentialOptions: Codable {
    
    private enum CodingKeys: String, CodingKey {
        case clientDataHash
        case rpEntity = "rp"
        case userEntity = "user"
        case requireResidentKey
        case requireUserPresence
        case requireUserVerification
        case credTypesAndPubKeyAlgs
        case excludeCredentialDescriptorList = "excludeCredentials"
    }
    
    var clientDataHash: Data
    var rpEntity: RpEntity
    var userEntity: UserEntity?
    var requireResidentKey: Bool
    var requireUserPresence: Bool
    var requireUserVerification: Bool
    var credTypesAndPubKeyAlgs: [String: Int]
    var excludeCredentialDescriptorList: [PublicKeyCredentialDescriptor]
    // TODO: possibly support extensions in the future
    // @SerializedName("authenticatorExtensions") public byte[] extensions;
    
    init() {
        self.clientDataHash = Data()
        self.rpEntity = RpEntity()
        self.userEntity = nil
        self.requireResidentKey = false
        self.requireUserPresence = false
        self.requireUserVerification = false
        self.credTypesAndPubKeyAlgs = [:]
        self.excludeCredentialDescriptorList = []
    }
}
