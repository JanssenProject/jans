//
//  FidoConfiguration.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 08.07.2024.
//

import Foundation
import RealmSwift

final class FidoConfiguration: Object, Codable, ObjectKeyIdentifiable {
    
    override static func primaryKey() -> String? {
        "sno"
    }
    
    private enum CodingKeys: String, CodingKey {
        case sno
        case issuer
        case attestation
    }
    
    @Persisted var sno: String?
    @Persisted var issuer: String?
    @Persisted var attestation: Attestation?
    
    @Persisted var isSuccessful: Bool?
    @Persisted var errorMessage: String?
}

//"{\"version\":\"1.1\",\"issuer\":\"https://admin-ui-test.gluu.org\",\"attestation\":{\"base_path\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/attestation\",\"options_endpoint\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/attestation/options\",\"result_endpoint\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/attestation/result\"},\"assertion\":{\"base_path\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/assertion\",\"options_endpoint\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/assertion/options\",\"options_generate_endpoint\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/assertion/options/generate\",\"result_endpoint\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/assertion/result\"},\"super_gluu_registration_endpoint\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/attestation/registration\",\"super_gluu_authentication_endpoint\":\"https://admin-ui-test.gluu.org/jans-fido2/restv1/assertion/authentication\"}"
