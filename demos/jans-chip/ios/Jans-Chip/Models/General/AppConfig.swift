//
//  AppConfig.swift
//  Jans-Chip
//
//  Created by Nazar Yavornytskyi on 11.10.2023.
//

import Foundation

struct AppConfig {
    
    static let APP_NAME = "jans-chip"
    static let DEFAULT_S_NO = "1"
    static let FIDO_CONFIG_URL = "/jans-fido2/restv1/configuration"
    static let OP_CONFIG_URL = "/.well-known/openid-configuration"
    static let SSA = "eyJraWQiOiJjb25uZWN0X2UyYjdhZDBjLTRjY2ItNDY4MC1hNzA1LWU3YjRjOWRkZDg2ZV9zaWdfcnMyNTYiLCJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJzb2Z0d2FyZV9pZCI6ImphbnMtY2hpcC1zc2EiLCJncmFudF90eXBlcyI6WyJhdXRob3JpemF0aW9uX2NvZGUiXSwib3JnX2lkIjoiR2x1dSIsImlzcyI6Imh0dHBzOi8vYWRtaW4tdWktdGVzdC5nbHV1Lm9yZyIsInNvZnR3YXJlX3JvbGVzIjpbImFkbWluIl0sImV4cCI6MTcyMjQ5OTkzNSwiaWF0IjoxNzE5OTA3OTM1LCJqdGkiOiJiMjM5M2NhNC1kOWIzLTRkYzgtOGVkNS0wYjQ5OTg4ZDJiNDUifQ.jnjy2Yam6xaf9lqAAubnTeiN5Ly2muy8wqkJ4AXzaOEMR_Md0WYHRA9bz2LvtVrno9cf0wUw27qDqGA99Y5J-guJjSc7ttnUceDSOGypoEdHsmCxN1-nsHgNcNcKN01FxAq4SPfkOeDj7un5GeKm82bBlO8Reyih5uNg3GYb8sNWVzHH3msSZuR2ExGvoJZlCbon1I5AdiZ-CnpSVywhHHwlJUQ0AN-zyns0JGIf8anLr13TPdp1vJiBLa_YOVJgxA-xT76Rlv0NB57SFoDMuyiSaaUySUyZo9sSIZIV0-U8XNYZ1HdOyJhdc5Kn3tpaiHbHOAc5NbLIodfMMZ2afg"
    static let ALLOWED_REGISTRATION_SCOPES = "openid authorization_challenge profile"
}
